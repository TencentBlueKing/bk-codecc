const fs = require('fs');
const path = require('path');

const distDir = path.resolve(__dirname, '../dist');
const indexPath = path.join(distDir, 'index.html');
const expectedSitePath = process.argv[2] || '/';
const expectedStaticUrl = process.argv[3] || expectedSitePath;

function fail(message) {
  console.error(message);
  process.exit(1);
}

if (!fs.existsSync(indexPath)) {
  fail(`Missing ${indexPath}`);
}

const html = fs.readFileSync(indexPath, 'utf8');

if (!html.includes('BK_SITE_PATH')) {
  fail('dist/index.html does not define BK_SITE_PATH');
}

if (!html.includes('BK_STATIC_URL')) {
  fail('dist/index.html does not define BK_STATIC_URL');
}

if (!html.includes(expectedSitePath)) {
  fail(`dist/index.html does not contain expected site path ${expectedSitePath}`);
}

if (!html.includes(expectedStaticUrl)) {
  fail(`dist/index.html does not contain expected static url ${expectedStaticUrl}`);
}

if (expectedStaticUrl !== '/' && html.includes('src="/js/')) {
  fail('dist/index.html contains root-relative js entry in subpath mode');
}

console.log('subpath dist verification passed');
