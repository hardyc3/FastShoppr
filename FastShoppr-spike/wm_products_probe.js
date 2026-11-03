// Throwaway Phase 0 helper: select Walmart store 3081, then read the three product pages.
const fs = require('fs');
const path = require('path');
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const products = [
  ['milk', 'https://www.walmart.com/ip/Great-Value-2-Reduced-Fat-Milk-1-Gallon-128-fl-oz/10450115'],
  ['eggs', 'https://www.walmart.com/ip/Great-Value-Large-White-Eggs-12-Count/145051970'],
  ['basil', 'https://www.walmart.com/ip/Fresh-Basil-0-5-oz-Clamshell/3757188318'],
];

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
  await sleep(9000);
  const clicked = await send('Runtime.evaluate', {
    expression: `(function(){var b=Array.from(document.querySelectorAll('button')).find(function(e){return /make this my store/i.test(e.innerText||'')});if(!b)return 'not-found';b.click();return 'clicked';})()`,
    returnByValue: true,
  });
  console.log('store click:', clicked.result.value);
  await sleep(5000);
  const cookie = await send('Runtime.evaluate', { expression: 'document.cookie', returnByValue: true });
  console.log('assortment cookie:', (cookie.result.value.match(/assortmentStoreId=[^;]+/) || ['none'])[0]);

  for (const [name, url] of products) {
    await send('Page.navigate', { url });
    await sleep(12000);
    const r = await send('Runtime.evaluate', {
      expression: `JSON.stringify({url:location.href,title:document.title,text:document.body?document.body.innerText.slice(0,8000):''})`,
      returnByValue: true,
    });
    const obj = JSON.parse(r.result.value || '{}');
    fs.writeFileSync(path.join(__dirname, 'wm_' + name + '_chrome.txt'), obj.text || '');
    console.log('---', name, obj.url, obj.title, 'chars', (obj.text || '').length);
    const lines = (obj.text || '').split('\n');
    console.log(lines.filter((l) => /aisle|in stock|pickup|delivery|store/i.test(l)).slice(0, 16).join(' | '));
  }
  ws.close();
}

main().catch((e) => { console.error(e); process.exit(1); });
