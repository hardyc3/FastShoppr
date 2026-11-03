// Throwaway Phase 0 helper: drive the locally-running Windows Chrome over CDP.
// Usage: node cdp_fetch.js <url> <output-html-path> [wait-ms]
const fs = require('fs');

const url = process.argv[2];
const out = process.argv[3];
const extraWait = Number(process.argv[4] || 8000);
if (!url || !out) {
  console.error('usage: node cdp_fetch.js <url> <out> [waitMs]');
  process.exit(2);
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function getTargets() {
  const res = await fetch('http://127.0.0.1:9222/json/list');
  return res.json();
}

async function main() {
  const targets = await getTargets();
  const page = targets.find((t) => t.type === 'page');
  if (!page) throw new Error('no page target');
  const ws = new WebSocket(page.webSocketDebuggerUrl);
  let id = 0;
  const pending = new Map();
  const events = [];
  ws.onmessage = (ev) => {
    const msg = JSON.parse(ev.data);
    if (msg.id && pending.has(msg.id)) {
      const { resolve, reject } = pending.get(msg.id);
      pending.delete(msg.id);
      if (msg.error) reject(new Error(JSON.stringify(msg.error)));
      else resolve(msg.result);
    } else if (msg.method) {
      events.push(msg);
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
  await send('Network.enable');
  await send('Page.navigate', { url });

  // Wait for load, then give client-side rendering / challenge scripts time.
  const start = Date.now();
  let last = '';
  while (Date.now() - start < 45000) {
    await sleep(1500);
    try {
      const r = await send('Runtime.evaluate', {
        expression: 'document.body ? document.body.innerText.slice(0,300) : ""',
        returnByValue: true,
      });
      last = r.result && r.result.value ? r.result.value : '';
      if (
        Date.now() - start > 6000 &&
        !/Access Denied|sec-if-cpt|Checking your browser|Just a moment/i.test(last)
      ) {
        break;
      }
    } catch (e) {
      // page may be navigating
    }
  }
  await sleep(extraWait);

  const html = await send('Runtime.evaluate', {
    expression: 'document.documentElement ? document.documentElement.outerHTML : ""',
    returnByValue: true,
  });
  const text = await send('Runtime.evaluate', {
    expression: 'document.body ? document.body.innerText : ""',
    returnByValue: true,
  });
  fs.writeFileSync(out, html.result.value || '');
  fs.writeFileSync(out + '.txt', text.result.value || '');
  console.log(
    JSON.stringify({
      url,
      title: (await send('Runtime.evaluate', { expression: 'document.title', returnByValue: true })).result.value,
      htmlBytes: (html.result.value || '').length,
      textChars: (text.result.value || '').length,
      preview: (text.result.value || '').slice(0, 400),
    })
  );
  ws.close();
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
