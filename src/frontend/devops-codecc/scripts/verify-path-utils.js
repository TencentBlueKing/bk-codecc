const assert = require('assert');
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const sourcePath = path.resolve(__dirname, '../src/utils/path.js');
const source = fs.readFileSync(sourcePath, 'utf8');
const transformedSource = source
  .replace(/export function /g, 'function ')
  .concat('\nmodule.exports = { normalizeBasePath, normalizeAssetBase, joinUrl, getSitePath, getStaticUrl, withSitePath, withStaticUrl };');

const sandbox = {
  module: { exports: {} },
  exports: {},
  window: {},
  URL,
};

vm.runInNewContext(transformedSource, sandbox, { filename: sourcePath });

const {
  normalizeBasePath,
  normalizeAssetBase,
  joinUrl,
  getSitePath,
  getStaticUrl,
  withSitePath,
  withStaticUrl,
} = sandbox.module.exports;

assert.strictEqual(normalizeBasePath(), '/');
assert.strictEqual(normalizeBasePath(''), '/');
assert.strictEqual(normalizeBasePath('/'), '/');
assert.strictEqual(normalizeBasePath('codecc'), '/codecc/');
assert.strictEqual(normalizeBasePath('/codecc'), '/codecc/');
assert.strictEqual(normalizeBasePath('codecc/'), '/codecc/');
assert.strictEqual(normalizeBasePath('/bk/codecc/'), '/bk/codecc/');
assert.strictEqual(normalizeAssetBase('https://cdn.example.com/codecc'), 'https://cdn.example.com/codecc/');
assert.strictEqual(normalizeAssetBase('//cdn.example.com/codecc'), '//cdn.example.com/codecc/');

assert.strictEqual(joinUrl('/codecc-demo/', '/static/login_success.html'), '/codecc-demo/static/login_success.html');
assert.strictEqual(joinUrl('/', '/static/login_success.html'), '/static/login_success.html');
assert.strictEqual(joinUrl('https://example.com/codecc/', '/static/a.js'), 'https://example.com/codecc/static/a.js');
assert.strictEqual(joinUrl('/codecc-demo/', 'https://cdn.example.com/a.js'), 'https://cdn.example.com/a.js');
assert.strictEqual(joinUrl('/codecc-demo/', '//cdn.example.com/a.js'), '//cdn.example.com/a.js');

sandbox.window.BK_SITE_PATH = '/codecc-demo/';
sandbox.window.BK_STATIC_URL = '/static-prefix/';
delete sandbox.window.BK_STATIC_URL;
assert.strictEqual(getStaticUrl(), '/codecc-demo/');

sandbox.window.BK_STATIC_URL = '';
assert.strictEqual(getSitePath(), '/codecc-demo/');
assert.strictEqual(getStaticUrl(), '/codecc-demo/');
assert.strictEqual(withSitePath('/codecc/demo/task/list'), '/codecc-demo/codecc/demo/task/list');
assert.strictEqual(withStaticUrl('/static/login_success.html'), '/codecc-demo/static/login_success.html');

sandbox.window.BK_STATIC_URL = '/static-prefix/';
assert.strictEqual(getStaticUrl(), '/static-prefix/');
assert.strictEqual(withStaticUrl('/static/login_success.html'), '/static-prefix/static/login_success.html');

sandbox.window.BK_STATIC_URL = 'https://cdn.example.com/codecc';
assert.strictEqual(getStaticUrl(), 'https://cdn.example.com/codecc/');
assert.strictEqual(withStaticUrl('/static/login_success.html'), 'https://cdn.example.com/codecc/static/login_success.html');

console.log('path utils verifier passed');
