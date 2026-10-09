// node run.js <script.js> : abre o Blockbench web, roda o script dentro da pagina (async, recebe "args"),
// o script devolve {files: {nome: texto}, shots: [{name, ...}]}. Screenshots sao tirados via window.__shot(name).
const fs = require('fs');
const { chromium } = require('playwright');
(async () => {
  const script = fs.readFileSync(process.argv[2], 'utf8');
  const args = process.argv[3] ? JSON.parse(fs.readFileSync(process.argv[3], 'utf8')) : {};
  const b = await chromium.launch({ args: ['--use-gl=swiftshader', '--enable-webgl', '--ignore-gpu-blocklist'] });
  const p = await b.newPage({ viewport: { width: 1400, height: 900 } });
  p.on('console', m => { const t = m.text(); if (m.type() === 'error' || t.startsWith('LOG')) console.log(m.type(), t.slice(0, 400)); });
  p.on('pageerror', e => console.log('PAGEERR', e.message.slice(0, 400)));
  await p.goto('https://web.blockbench.net/', { waitUntil: 'networkidle', timeout: 120000 });
  await p.waitForTimeout(3000);
  await p.exposeFunction('__shot', async (name) => { await p.screenshot({ path: name }); return true; });
  const out = await p.evaluate(`(async (args) => { ${script} })(${JSON.stringify(args)})`);
  if (out && out.files) for (const [n, t] of Object.entries(out.files)) fs.writeFileSync(n, t);
  console.log('done', out ? Object.keys(out.files || {}) : null, out && out.log ? out.log : '');
  await b.close();
})();
