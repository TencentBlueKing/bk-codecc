const express = require('express');
const path = require('path');

const app = express();
const port = Number(process.env.PORT || 8099);
const sitePath = process.env.BK_SITE_PATH || '/codecc-demo/';
const normalizedSitePath = sitePath === '/' ? '/' : `/${sitePath.replace(/^\/+|\/+$/g, '')}/`;
const distDir = path.resolve(__dirname, '../dist');

app.use(normalizedSitePath, express.static(distDir));
app.get(`${normalizedSitePath}*`, (req, res) => {
  res.sendFile(path.join(distDir, 'index.html'));
});

app.listen(port, () => {
  console.log(`Serving ${distDir} at http://127.0.0.1:${port}${normalizedSitePath}`);
});
