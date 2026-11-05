// Throwaway Phase 0 helper: drive the locally-running Windows Chrome over CDP.
// Reads targets.json: [{ "name": "x", "url": "...", "waitMs": 8000 }, ...]
const fs = require('fs');
const path = require('path');

const base = __dirname;
const targets = JSON.parse(fs.readFileSync(path.join(base, 'targets.json'), 'utf8'));
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function getPage() {
  const res = await fetch('http://127.0.0.1:9222/json/list');
  const list = await res.json();
  const page = list.find((t) => t.type === 'page');
  if (!page) throw new Error('no page target');
  return page;
}

async function main() {
  const page = await getPage();
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
  await new Promise((resolve, reject) => {
    ws.onopen = resolve;
    ws.onerror = reject;
  });
  const send = (method, params = {}) =>
    new Promise((resolve, reject) => {
      const msgId = ++id;
      pending.set(msgId, { resolve, reject });
      ws.send(JSON.stringify({ id: msgId, method, params }));
    });

  await send('Page.enable');
  await send('Runtime.enable');

  for (const t of targets) {
    console.log('NAV', t.name, t.url);
    await send('Page.navigate', { url: t.url });
    const start = Date.now();
    while (Date.now() - start < 45000) {
      await sleep(1500);
      try {
        const r = await send('Runtime.evaluate', {
          expression: 'document.body ? document.body.innerText.slice(0,400) : ""',
          returnByValue: true,
        });
        const last = r.result && r.result.value ? r.result.value : '';
        if (Date.now() - start > 6000 && !/Access Denied|sec-if-cpt|Checking your browser|Just a moment/i.test(last)) {
          break;
        }
      } catch (e) {
        // navigation in progress
      }
    }
    await sleep(t.waitMs || 8000);
    const html = await send('Runtime.evaluate', {
      expression: 'document.documentElement ? document.documentElement.outerHTML : ""',
      returnByValue: true,
    });
    const text = await send('Runtime.evaluate', {
      expression: 'document.body ? document.body.innerText : ""',
      returnByValue: true,
    });
    const title = await send('Runtime.evaluate', { expression: 'document.title', returnByValue: true });
    fs.writeFileSync(path.join(base, t.name + '.html'), html.result.value || '');
    fs.writeFileSync(path.join(base, t.name + '.txt'), text.result.value || '');
    console.log(JSON.stringify({
      name: t.name,
      title: title.result.value,
      htmlBytes: (html.result.value || '').length,
      textChars: (text.result.value || '').length,
      preview: (text.result.value || '').slice(0, 300),
    }));
  }
  ws.close();
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
