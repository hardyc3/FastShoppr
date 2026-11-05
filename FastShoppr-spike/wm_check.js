// Throwaway Phase 0 helper: read one page in the already-store-selected Chrome tab.
const fs = require('fs');
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function main() {
  const res = await fetch('http://127.0.0.1:9222/json/list');
  const list = await res.json();
  const page = list.find((t) => t.type === 'page');
  const ws = new WebSocket(page.webSocketDebuggerUrl);
  let id = 0;
  const pending = new Map();
  ws.onmessage = (ev) => {
    const msg = JSON.parse(ev.data);
    if (msg.id && pending.has(msg.id)) {
      const p = pending.get(msg.id);
      pending.delete(msg.id);
      msg.error ? p.reject(new Error(JSON.stringify(msg.error))) : p.resolve(msg.result);
    }
  };
  await new Promise((resolve, reject) => { ws.onopen = resolve; ws.onerror = reject; });
  const send = (method, params = {}) => new Promise((resolve, reject) => {
    const msgId = ++id;
    pending.set(msgId, { resolve, reject });
    ws.send(JSON.stringify({ id: msgId, method, params }));
  });
  await send('Page.enable');
  await send('Runtime.enable');
  const url = process.argv[2];
  await send('Page.navigate', { url });
  await sleep(12000);
  const r = await send('Runtime.evaluate', {
    expression: `JSON.stringify({url:location.href,title:document.title,text:document.body?document.body.innerText.slice(0,8000):'',links:Array.from(document.querySelectorAll('a[href*="/ip/"]')).slice(0,10).map(a=>a.href)})`,
    returnByValue: true,
  });
  const obj = JSON.parse(r.result.value || '{}');
  fs.writeFileSync(__dirname + '/wm_last_probe.json', r.result.value || '');
  console.log(obj.url);
  console.log(obj.title);
  const lines = (obj.text || '').split('\n');
  console.log(lines.filter((l) => /aisle|in stock|out of stock|pickup|delivery/i.test(l)).slice(0, 20).join('\n'));
  console.log('LINKS', obj.links);
  ws.close();
}

main().catch((e) => { console.error(e); process.exit(1); });
