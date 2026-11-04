const { createCanvas, GlobalFonts } = require('@napi-rs/canvas');
const fs = require('fs');
const path = require('path');

const DEVICES = {
  oneplus13s: [1216, 2640],
  oneplus13: [1440, 3168],
};

const DEFAULT_ACCENT = '#FF5B35';

let FONT = 'sans-serif';
let fontTried = false;
function initFont() {
  if (fontTried) return;
  fontTried = true;
  const candidates = [
    path.join(__dirname, '../node_modules/@fontsource/inter/files/inter-latin-400-normal.woff'),
    path.join(process.cwd(), 'node_modules/@fontsource/inter/files/inter-latin-400-normal.woff'),
  ];
  for (const p of candidates) {
    try {
      if (fs.existsSync(p)) {
        GlobalFonts.registerFromPath(p, 'JinseiInter');
        FONT = 'JinseiInter';
        return;
      }
    } catch (e) { /* fall back */ }
  }
}

function parseISO(s) {
  const m = /^(\d{4})-(\d{1,2})-(\d{1,2})$/.exec(s || '');
  if (!m) return null;
  const t = Date.UTC(+m[1], +m[2] - 1, +m[3]);
  const d = new Date(t);
  if (d.getUTCMonth() !== +m[2] - 1) return null;
  return t;
}
const daysBetween = (a, b) => Math.round((b - a) / 86400000);

function todayIn(tz) {
  const iso = new Date().toLocaleDateString('en-CA', { timeZone: tz });
  return parseISO(iso);
}

function isLeap(y) { return (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0; }

function stats(total, idx, unit) {
  const elapsed = Math.min(Math.max(idx + 1, 0), total);
  const left = total - elapsed;
  const pct = Math.floor((elapsed * 100) / total);
  return [`${left}${unit} left`, `  \u00B7  ${pct}%`];
}

function buildSpec(o, today) {
  const isLight = o.theme === 'light';
  // Plain solid flat backgrounds - zero gradients
  const bg = isLight ? '#F5F5F7' : (o.type === 'goal' ? '#121214' : '#000000');
  const past = isLight ? '#1C1C1E' : '#FFFFFF';
  const future = isLight ? '#D1D1D6' : '#242428';
  const futureLife = isLight ? '#D8D8DC' : '#1C1C20';
  const textColor = isLight ? '#6E6E73' : '#8E8E93';

  const todayDate = new Date(today);
  if (o.type === 'life') {
    const dob = parseISO(o.dob) ?? today;
    const lifespan = Math.min(Math.max(parseInt(o.lifespan, 10) || 90, 1), 120);
    const total = lifespan * 52;
    const idx = Math.floor(daysBetween(dob, today) / 7);
    const [a, b] = stats(total, idx, ' weeks');
    return { total, idx, cols: 52, widthFrac: 0.70, dotRatio: 0.38, title: null, a, b, bg, past, future: futureLife, textColor };
  }
  if (o.type === 'goal') {
    const start = parseISO(o.start) ?? today;
    let end = parseISO(o.end) ?? start + 86400000;
    if (end <= start) end = start + 86400000;
    const total = daysBetween(start, end);
    const idx = daysBetween(start, today);
    const cols = total <= 210 ? 15 : Math.ceil(Math.sqrt(total * 0.75));
    const [a, b] = stats(total, idx, 'd');
    return { total, idx, cols, widthFrac: 0.63, dotRatio: 0.31, title: o.title || 'My Goal', a, b, bg, past, future, textColor };
  }
  // year
  const cy = todayDate.getUTCFullYear();
  const y = parseInt(o.year, 10) || cy;
  const total = isLeap(y) ? 366 : 365;
  let idx;
  if (y < cy) idx = total;
  else if (y > cy) idx = -1;
  else idx = daysBetween(Date.UTC(y, 0, 1), today);
  const [a, b] = stats(total, idx, 'd');
  return { total, idx, cols: 15, widthFrac: 0.72, dotRatio: 0.34, title: null, a, b, bg, past, future, textColor };
}

function resolveSize(device) {
  if (DEVICES[device]) return DEVICES[device];
  const m = /^(\d{3,4})x(\d{3,4})$/.exec(device || '');
  if (m) return [Math.min(+m[1], 2000), Math.min(+m[2], 4500)];
  return DEVICES.oneplus13s;
}

function render(o) {
  initFont();
  const [w, h] = resolveSize(o.device);
  const tz = o.tz || 'Asia/Kolkata';
  const today = (o.today && parseISO(o.today)) || todayIn(tz);
  const accent = /^[0-9a-fA-F]{6}$/.test(o.accent || '') ? `#${o.accent}` : DEFAULT_ACCENT;
  const s = buildSpec(o, today);

  const canvas = createCanvas(w, h);
  const c = canvas.getContext('2d');
  c.fillStyle = s.bg;
  c.fillRect(0, 0, w, h);

  const rows = Math.ceil(s.total / s.cols);
  const titleSize = w * 0.030;
  const statsSize = w * 0.024;
  const titleH = s.title ? titleSize + w * 0.038 : 0;
  const statsH = w * 0.030 + statsSize;

  let step = (w * s.widthFrac) / s.cols;
  const minTopFrac = s.cols === 52 ? 0.38 : 0.45;
  var top = Math.max(h * minTopFrac, h * 0.53 - (titleH + step * rows + statsH) / 2);
  step = Math.min(step, (h * 0.86 - top - titleH - statsH) / rows);
  top = Math.max(h * minTopFrac, Math.min(top, h * 0.53 - (titleH + step * rows + statsH) / 2));

  const gridTop = top + titleH;
  const gridBottom = gridTop + step * rows;
  const x0 = (w - step * s.cols) / 2;
  const radius = step * s.dotRatio;

  if (s.title) {
    c.font = `${titleSize}px ${FONT}`;
    c.fillStyle = s.textColor;
    const tw = c.measureText(s.title).width;
    c.fillText(s.title, (w - tw) / 2, top + titleSize);
  }

  for (let i = 0; i < s.total; i++) {
    const col = i % s.cols;
    const row = Math.floor(i / s.cols);
    c.fillStyle = i < s.idx ? s.past : i === s.idx ? accent : s.future;
    c.beginPath();
    c.arc(x0 + step * (col + 0.5), gridTop + step * (row + 0.5), radius, 0, Math.PI * 2);
    c.fill();
  }

  c.font = `${statsSize}px ${FONT}`;
  const wl = c.measureText(s.a).width;
  const wr = c.measureText(s.b).width;
  const sx = (w - (wl + wr)) / 2;
  const sy = gridBottom + w * 0.030 + statsSize;
  c.fillStyle = accent;
  c.fillText(s.a, sx, sy);
  c.fillStyle = s.textColor;
  c.fillText(s.b, sx + wl, sy);

  return canvas.toBuffer('image/png');
}

module.exports = { render, DEVICES };
