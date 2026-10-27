// Renders claude-edit.html to a 1080x1920 / 30 fps MP4 with its soundtrack.
// Usage: node claude-edit/render-mp4.js [out.mp4]
//        node claude-edit/render-mp4.js --stills sheet.png   (contact sheet for a quick look)
const { chromium } = require('playwright');
const { execFileSync, spawn } = require('child_process');
const fs = require('fs');
const os = require('os');
const path = require('path');

const FPS = 30;
const HTML = path.join(__dirname, 'claude-edit.html');
const FONT_CSS = 'https://fonts.googleapis.com/css2?family=Instrument+Serif:ital@0;1&family=JetBrains+Mono:wght@500;700&family=Unbounded:wght@400;900&display=swap';
const UA = 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36';

// Google Fonts CSS with every font file inlined, so the headless browser needs no network.
function inlineFonts() {
  const css = execFileSync('curl', ['-sSf', '-A', UA, FONT_CSS]).toString();
  return css.replace(/url\((https:[^)]+)\)/g, (_, url) => {
    const bytes = execFileSync('curl', ['-sSf', url], { maxBuffer: 1 << 26 });
    return `url(data:font/woff2;base64,${bytes.toString('base64')})`;
  });
}

async function openPage(browser) {
  const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
  const css = inlineFonts();
  await page.route('https://fonts.googleapis.com/**', r => r.fulfill({ status: 200, contentType: 'text/css', body: css }));
  await page.route('https://fonts.gstatic.com/**', r => r.abort());
  const body = fs.readFileSync(HTML, 'utf8');
  await page.setContent(`<!doctype html><html><head><meta charset="utf-8"></head><body>${body}</body></html>`);
  await page.waitForFunction(() => window.__edit && window.__edit.ready, null, { timeout: 20000 });
  return page;
}

async function stills(page, out) {
  const times = [1.2, 3.2, 4.25, 5.3, 6.9, 7.8, 8.9, 9.8, 10.6, 11.9, 12.6, 13.6, 14.3, 15.2, 15.65, 16.62, 18.5, 19.4, 20.6, 21.6, 22.3, 23.4, 24.6, 25.4];
  const png = await page.evaluate(ts => {
    const src = document.getElementById('stage'), cols = 6, w = 270, h = 480;
    const sheet = document.createElement('canvas');
    sheet.width = cols * w; sheet.height = Math.ceil(ts.length / cols) * h;
    const g = sheet.getContext('2d');
    ts.forEach((t, i) => {
      window.__edit.renderAt(t);
      g.drawImage(src, (i % cols) * w, Math.floor(i / cols) * h, w, h);
      g.fillStyle = '#fff'; g.font = '700 16px monospace'; g.fillText(t.toFixed(2) + 's', (i % cols) * w + 8, Math.floor(i / cols) * h + 20);
    });
    return sheet.toDataURL('image/png').split(',')[1];
  }, times);
  fs.writeFileSync(out, Buffer.from(png, 'base64'));
}

async function audioWav(page) {
  const b64 = await page.evaluate(async () => {
    const buf = await window.__edit.renderAudio(48000);
    const L = buf.getChannelData(0), R = buf.getChannelData(1), n = buf.length, sr = buf.sampleRate;
    const v = new DataView(new ArrayBuffer(44 + n * 4));
    const tag = (o, s) => { for (let i = 0; i < 4; i++) v.setUint8(o + i, s.charCodeAt(i)); };
    tag(0, 'RIFF'); v.setUint32(4, 36 + n * 4, true); tag(8, 'WAVE'); tag(12, 'fmt ');
    v.setUint32(16, 16, true); v.setUint16(20, 1, true); v.setUint16(22, 2, true);
    v.setUint32(24, sr, true); v.setUint32(28, sr * 4, true); v.setUint16(32, 4, true); v.setUint16(34, 16, true);
    tag(36, 'data'); v.setUint32(40, n * 4, true);
    for (let i = 0, o = 44; i < n; i++, o += 4) {
      const l = Math.max(-1, Math.min(1, L[i])), r = Math.max(-1, Math.min(1, R[i]));
      v.setInt16(o, l * 32767, true); v.setInt16(o + 2, r * 32767, true);
    }
    const bytes = new Uint8Array(v.buffer);
    let bin = '';
    for (let i = 0; i < bytes.length; i += 0x8000) bin += String.fromCharCode.apply(null, bytes.subarray(i, i + 0x8000));
    return btoa(bin);
  });
  return Buffer.from(b64, 'base64');
}

async function video(page, out) {
  const wav = path.join(os.tmpdir(), `claude-edit-${process.pid}.wav`);
  fs.writeFileSync(wav, await audioWav(page));
  const duration = await page.evaluate(() => window.__edit.DURATION);
  const ff = spawn('ffmpeg', ['-y', '-loglevel', 'error', '-f', 'image2pipe', '-framerate', String(FPS), '-c:v', 'mjpeg', '-i', '-',
    '-i', wav, '-c:v', 'libx264', '-preset', 'slow', '-crf', '20', '-pix_fmt', 'yuv420p', '-r', String(FPS),
    '-c:a', 'aac', '-b:a', '192k', '-shortest', '-movflags', '+faststart', out], { stdio: ['pipe', 'inherit', 'inherit'] });
  const done = new Promise((res, rej) => ff.on('close', c => c === 0 ? res() : rej(new Error('ffmpeg ' + c))));
  const frames = Math.round(duration * FPS);
  for (let i = 0; i < frames; i++) {
    const jpg = await page.evaluate(t => {
      window.__edit.renderAt(t);
      return document.getElementById('stage').toDataURL('image/jpeg', .95).split(',')[1];
    }, i / FPS);
    if (!ff.stdin.write(Buffer.from(jpg, 'base64'))) await new Promise(r => ff.stdin.once('drain', r));
    if (i % 60 === 0) process.stdout.write(`\rklatka ${i}/${frames}`);
  }
  ff.stdin.end();
  await done;
  fs.unlinkSync(wav);
  process.stdout.write(`\rgotowe: ${out}\n`);
}

(async () => {
  const args = process.argv.slice(2);
  const browser = await chromium.launch();
  try {
    const page = await openPage(browser);
    if (args[0] === '--stills') await stills(page, args[1] || 'sheet.png');
    else await video(page, args[0] || path.join(__dirname, 'claude-edit.mp4'));
  } finally {
    await browser.close();
  }
})().catch(e => { console.error(e); process.exit(1); });
