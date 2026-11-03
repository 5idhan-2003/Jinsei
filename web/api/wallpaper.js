const { render } = require('../lib/render');

// GET /api/wallpaper?type=goal&title=Marathon&start=2026-01-01&end=2026-12-31&device=oneplus13s
module.exports = (req, res) => {
  try {
    const q = req.query || {};
    const png = render({
      type: q.type,
      dob: q.dob,
      lifespan: q.lifespan,
      year: q.year,
      title: q.title,
      start: q.start,
      end: q.end,
      device: q.device,
      accent: q.accent,
      tz: q.tz,
      today: q.today,
    });
    res.setHeader('Content-Type', 'image/png');
    res.setHeader('Cache-Control', 'public, max-age=0, s-maxage=600');
    res.status(200).send(png);
  } catch (e) {
    res.status(500).json({ error: String(e && e.message ? e.message : e) });
  }
};

