const fs = require('fs');
const path = require('path');
const { render } = require('../lib/render');

const out = path.join(__dirname, '../out');
fs.mkdirSync(out, { recursive: true });

const cases = {
  year: { type: 'year', today: '2026-11-04' },
  goal: { type: 'goal', title: 'New York City Marathon', start: '2026-09-01', end: '2026-12-31', today: '2026-11-04' },
  life: { type: 'life', dob: '2000-01-01', today: '2026-11-04' },
};
for (const [name, o] of Object.entries(cases)) {
  fs.writeFileSync(path.join(out, `${name}.png`), render({ device: 'oneplus13s', ...o }));
  console.log('wrote', name);
}

