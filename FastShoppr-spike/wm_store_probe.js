// Throwaway Phase 0 helper: inspect the Walmart store page through Windows Chrome CDP.
const fs = require('fs');
const path = require('path');
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function main() {
  const res = await fetch('http://127.0.0.1:9222/json/list');
  const list = await res.json();
  const page = list.find((t) => t.type === 'page');
  if (!page) throw new Error('no page target');
  const ws = new WebSocket(page.webSocketDebuggerUrl);
  let id = 0;
  const pending = new Map();
  ws.onmessage = (ev) => {
    const msg = JSON.parse(ev.data);
    if (msg.id && pending.has(msg.id)) {
      const { resolve, reject } = pending.get(msg.id);
      pending.delete(msg.id);
      if (msg.error) reject(new Error(JSON.stringify(msg.error)));
      else resolve(msg.result);
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
  await send('Page.navigate', { url: 'https://www.walmart.com/store/3081' });
  await sleep(12000);
  const expression = `JSON.stringify({
    title: document.title,
    url: location.href,
    text: document.body ? document.body.innerText.slice(0, 3000) : '',
    buttons: Array.from(document.querySelectorAll('button,a')).map(e => ({tag:e.tagName, text:(e.innerText||'').trim().slice(0,80), aria:e.getAttribute('aria-label')||'', test:e.getAttribute('data-testid')||''})).filter(x => x.text || x.aria).slice(0,80),
    cookies: document.cookie
  })`;
  const r = await send('Runtime.evaluate', { expression, returnByValue: true });
  fs.writeFileSync(path.join(__dirname, 'wm_store_probe.json'), r.result.value || '');
  console.log(r.result.value);
  ws.close();
}

main().catch((e) => { console.error(e); process.exit(1); });
