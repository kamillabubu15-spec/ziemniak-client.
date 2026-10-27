// Renders the Claude edit (engine.html + music.py) to a 1080x1920 / 30 fps MP4.
// Usage:
//   node claude-edit/render.js                      full video -> claude-edit/claude-edit.mp4
//   node claude-edit/render.js --stills sheet.png   contact sheet of key moments
//   node claude-edit/render.js --frame 6.1 f.png    one full-size frame
const { chromium } = require('playwright');
const { execFileSync, spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

const FPS = 30;
const DIR = __dirname;
const BUILD = path.join(DIR, 'build');
const ENGINE = path.join(DIR, 'engine.html');
const UA = 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36';

function soundtrack() {
  fs.mkdirSync(BUILD, { recursive: true });
  if (!fs.existsSync(path.join(BUILD, 'events.json')) || process.argv.includes('--music')) {
    execFileSync('python3', [path.join(DIR, 'music.py'), BUILD], { stdio: 'inherit' });
  }
  return JSON.parse(fs.readFileSync(path.join(BUILD, 'events.json'), 'utf8'));
}

// The engine's Google Fonts stylesheet with every font file inlined, so rendering needs no network.
function inlineFonts() {
  const href = fs.readFileSync(ENGINE, 'utf8').match(/href="(https:\/\/fonts\.googleapis\.com\/css2[^"]+)"/)[1].replace(/&amp;/g, '&');
  const css = execFileSync('curl', ['-sSf', '-A', UA, href]).toString();
  return css.replace(/url\((https:[^)]+)\)/g, (_, url) => {
    const bytes = execFileSync('curl', ['-sSf', url], { maxBuffer: 1 << 26 });
    return `url(data:font/woff2;base64,${bytes.toString('base64')})`;
  });
}

async function openEngine(events) {
  const browser = await chromium.launch({ args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
  const page = await browser.newPage({ viewport: { width: 1080, height: 1920 } });
  page.on('pageerror', e => console.error('page error:', e.message));
  page.on('console', m => { if (m.type() === 'error') console.error('console:', m.text()); });
  const css = inlineFonts();
  await page.route('https://fonts.googleapis.com/**', r => r.fulfill({ status: 200, contentType: 'text/css', body: css }));
  await page.route('https://fonts.gstatic.com/**', r => r.abort());
  await page.goto('file://' + ENGINE);
  await page.waitForFunction(() => window.__engine, null, { timeout: 20000 });
  const info = await page.evaluate(ev => window.__engine.init(ev), events);
  return { browser, page, info };
}

const grab = (page, t, type = 'image/jpeg', q = 0.95) => page.evaluate(([t, type, q]) => {
  window.__engine.frame(t);
  return document.getElementById('gl').toDataURL(type, q).split(',')[1];
}, [t, type, q]);

async function stills(page, out, times) {
  const cols = 6, w = 270, h = 480;
  const tiles = [];
  for (const t of times) tiles.push(await grab(page, t, 'image/jpeg', 0.9));
  const png = await page.evaluate(async ([tiles, times, cols, w, h]) => {
    const sheet = document.createElement('canvas');
    sheet.width = cols * w; sheet.height = Math.ceil(tiles.length / cols) * h;
    const g = sheet.getContext('2d');
    for (let i = 0; i < tiles.length; i++) {
      const img = new Image();
      img.src = 'data:image/jpeg;base64,' + tiles[i];
      await img.decode();
      const x = (i % cols) * w, y = Math.floor(i / cols) * h;
      g.drawImage(img, x, y, w, h);
      g.fillStyle = 'rgba(0,0,0,.6)'; g.fillRect(x, y, 64, 22);
      g.fillStyle = '#fff'; g.font = '700 15px monospace'; g.fillText(times[i].toFixed(2), x + 6, y + 16);
    }
    return sheet.toDataURL('image/png').split(',')[1];
  }, [tiles, times, cols, w, h]);
  fs.writeFileSync(out, Buffer.from(png, 'base64'));
}

async function video(page, duration, out) {
  const wav = path.join(BUILD, 'soundtrack.wav');
  const ff = spawn('ffmpeg', ['-y', '-loglevel', 'error', '-f', 'image2pipe', '-framerate', String(FPS), '-c:v', 'mjpeg', '-i', '-',
    '-i', wav, '-map', '0:v', '-map', '1:a', '-c:v', 'libx264', '-preset', 'slow', '-crf', '17', '-pix_fmt', 'yuv420p', '-r', String(FPS),
    '-c:a', 'aac', '-b:a', '256k', '-t', duration.toFixed(3), '-movflags', '+faststart', out], { stdio: ['pipe', 'inherit', 'inherit'] });
  const done = new Promise((res, rej) => ff.on('close', c => (c === 0 ? res() : rej(new Error('ffmpeg exited with ' + c)))));
  const frames = Math.round(duration * FPS), t0 = Date.now();
  for (let i = 0; i < frames; i++) {
    const jpg = Buffer.from(await grab(page, i / FPS), 'base64');
    if (!ff.stdin.write(jpg)) await new Promise(r => ff.stdin.once('drain', r));
    if (i % 30 === 0) {
      const el = (Date.now() - t0) / 1000, eta = i ? el / i * (frames - i) : 0;
      console.log(`klatka ${i}/${frames}  ${el.toFixed(0)} s  (zostało ~${(eta / 60).toFixed(1)} min)`);
    }
  }
  ff.stdin.end();
  await done;
  console.log('gotowe:', out);
}

(async () => {
  const args = process.argv.slice(2).filter(a => a !== '--music');
  const events = soundtrack();
  const { browser, page, info } = await openEngine(events);
  try {
    if (args[0] === '--stills') {
      const times = args.length > 2 ? args.slice(2).map(Number)
        : [1.2, 3.0, 4.6, 5.7, 6.5, 7.9, 8.8, 9.7, 10.7, 11.6, 12.6, 13.4, 14.0, 15.2, 16.0, 17.3, 19.0, 19.9, 20.45, 21.3, 22.6, 23.4, 24.3, 24.9, 25.3, 26.2, 26.9, 28.3, 29.0, 30.0, 30.7, 31.1, 31.6, 32.4, 33.2, 33.9];
      await stills(page, args[1] || 'sheet.png', times);
    } else if (args[0] === '--frame') {
      fs.writeFileSync(args[2] || 'frame.png', Buffer.from(await grab(page, Number(args[1]), 'image/png'), 'base64'));
    } else {
      await video(page, info.duration, args[0] || path.join(DIR, 'claude-edit.mp4'));
    }
  } finally {
    await browser.close();
  }
})().catch(e => { console.error(e); process.exit(1); });
