# CodeCC Frontend Subpath Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make CodeCC frontend work when deployed at `/` or any configured subpath such as `/codecc-demo/`, without rebuilding per environment.

**Architecture:** Inject `BK_SITE_PATH` and `BK_STATIC_URL` in the rendered entry HTML, normalize both at runtime, and let Vue Router use `BK_SITE_PATH` as its history base. Use webpack runtime public path plus a small URL helper so static assets, login callback URLs, internal links, and project cookie path respect the configured deployment path while backend API URLs remain controlled by `AJAX_URL_PREFIX`.

**Tech Stack:** Vue 2.7, Vue Router 3, bk-cli-service-webpack, webpack runtime public path, nginx/OpenResty gateway templates, Helm templates, shell deployment env rendering.

---

## File Structure

- Create `src/frontend/devops-codecc/static/webpack_public_path.js`: sets webpack runtime public path before app imports.
- Create `src/frontend/devops-codecc/src/utils/path.js`: shared path normalization and URL joining helpers for JavaScript modules.
- Create `src/frontend/devops-codecc/scripts/verify-path-utils.js`: Node verifier for path helpers.
- Modify `src/frontend/devops-codecc/index.html`: inject `BK_SITE_PATH`, normalize entry-page globals, define `__loadAssetsUrl__`, fix project-id cookie path, and fix iframe utility prefix handling.
- Modify `src/frontend/devops-codecc/src/main.js`: import runtime public path before all other imports.
- Modify `src/frontend/devops-codecc/src/router/index.js`: configure Vue Router history base.
- Modify `src/frontend/devops-codecc/src/api/index.js`: build login modal success callback with the static asset prefix.
- Modify CodeCC-owned internal URL call sites that manually construct root-relative URLs:
  - `src/frontend/devops-codecc/src/views/paas/test/design-report.vue`
  - any additional call sites found by the audit command in Task 4.
- Modify deploy-time env sources:
  - `src/frontend/devops-codecc/.bk.production.env`
  - `scripts/deploy-codecc/codecc.properties`
  - `docker-images/core/codecc/gateway/scripts/codecc.env`
  - `helm-charts/core/codecc/templates/gateway/deployment.yaml`
- Modify `support-files/codecc/templates/core/gateway#core#vhosts#codecc.frontend.conf`: strip the configured site path before frontend `try_files` checks.
- Create `src/frontend/devops-codecc/scripts/verify-subpath-dist.js`: local build-output verifier for root and subpath modes.
- Create `src/frontend/devops-codecc/scripts/serve-subpath-dist.js`: local static server with history fallback for manual browser verification.

## Task 1: Add Path Helpers

**Files:**
- Create: `src/frontend/devops-codecc/src/utils/path.js`
- Create: `src/frontend/devops-codecc/scripts/verify-path-utils.js`

- [ ] **Step 1: Write the failing path helper verifier**

Create `src/frontend/devops-codecc/scripts/verify-path-utils.js`:

```js
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

assert.strictEqual(joinUrl('/codecc-demo/', '/static/login_success.html'), '/codecc-demo/static/login_success.html');
assert.strictEqual(joinUrl('/', '/static/login_success.html'), '/static/login_success.html');
assert.strictEqual(joinUrl('https://example.com/codecc/', '/static/a.js'), 'https://example.com/codecc/static/a.js');
assert.strictEqual(joinUrl('/codecc-demo/', 'https://cdn.example.com/a.js'), 'https://cdn.example.com/a.js');

sandbox.window.BK_SITE_PATH = '/codecc-demo/';
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
```

- [ ] **Step 2: Run the focused test and verify it fails**

Run:

```bash
cd /Users/brooklin/project/github/bk-codecc/src/frontend/devops-codecc
node scripts/verify-path-utils.js
```

Expected: FAIL with `ENOENT` because `src/utils/path.js` does not exist yet.

- [ ] **Step 3: Implement the path helper**

Create `src/frontend/devops-codecc/src/utils/path.js`:

```js
export function normalizeBasePath(value = '/') {
  const rawValue = String(value || '/').trim();
  if (!rawValue || rawValue === '/') {
    return '/';
  }

  const normalized = rawValue.replace(/^\/+|\/+$/g, '');
  return normalized ? `/${normalized}/` : '/';
}

export function normalizeAssetBase(value = '/', fallback = '/') {
  const rawValue = String(value || fallback || '/').trim();
  if (/^https?:\/\//.test(rawValue) || rawValue.startsWith('//')) {
    return rawValue.endsWith('/') ? rawValue : `${rawValue}/`;
  }
  return normalizeBasePath(rawValue);
}

export function joinUrl(base = '/', path = '') {
  const baseValue = String(base || '/');
  const pathValue = String(path || '');

  if (/^https?:\/\//.test(pathValue) || pathValue.startsWith('//')) {
    return pathValue;
  }

  if (/^https?:\/\//.test(baseValue)) {
    const url = new URL(baseValue);
    url.pathname = [url.pathname, pathValue].join('/').replace(/\/+/g, '/');
    return url.toString().replace(/\/$/, pathValue ? '' : '/');
  }

  const startsWithRoot = baseValue.startsWith('/');
  const joined = [baseValue, pathValue]
    .filter(item => item !== '')
    .join('/')
    .replace(/\/+/g, '/');
  const result = startsWithRoot ? `/${joined.replace(/^\/+/, '')}` : joined;
  return result || (startsWithRoot ? '/' : '');
}

export function getSitePath() {
  return normalizeBasePath(window.BK_SITE_PATH || '/');
}

export function getStaticUrl() {
  return normalizeAssetBase(window.BK_STATIC_URL, getSitePath());
}

export function withSitePath(path) {
  return joinUrl(getSitePath(), path);
}

export function withStaticUrl(path) {
  return joinUrl(getStaticUrl(), path);
}
```

- [ ] **Step 4: Run the path helper verifier**

Run:

```bash
cd /Users/brooklin/project/github/bk-codecc/src/frontend/devops-codecc
node scripts/verify-path-utils.js
```

Expected: `path utils verifier passed`.

- [ ] **Step 5: Commit Task 1**

Run:

```bash
git add src/frontend/devops-codecc/src/utils/path.js src/frontend/devops-codecc/scripts/verify-path-utils.js
git commit -m "feat(frontend): add subpath url helpers"
```

Expected: commit succeeds.

## Task 2: Inject Entry Variables and Runtime Public Path

**Files:**
- Create: `src/frontend/devops-codecc/static/webpack_public_path.js`
- Modify: `src/frontend/devops-codecc/index.html`
- Modify: `src/frontend/devops-codecc/src/main.js`
- Modify: `src/frontend/devops-codecc/.bk.production.env`

- [ ] **Step 1: Add webpack runtime public path file**

Create `src/frontend/devops-codecc/static/webpack_public_path.js`:

```js
__webpack_public_path__ = window.BK_STATIC_URL || '/';
```

- [ ] **Step 2: Import runtime public path before all app imports**

Modify the top of `src/frontend/devops-codecc/src/main.js` so the first executable import is:

```js
import '../static/webpack_public_path';
```

Expected beginning of file:

```js
/**
 * @file main entry
 * @author blueking
 */

import '../static/webpack_public_path';
import Vue from 'vue';
```

- [ ] **Step 3: Add `BK_SITE_PATH` to production env**

Modify `src/frontend/devops-codecc/.bk.production.env`:

```env
BK_SITE_PATH = '__BK_SITE_PATH__'
BK_STATIC_URL = '__BK_PUBLIC_PATH_PREFIX__'
```

Place `BK_SITE_PATH` immediately before `BK_STATIC_URL`.

- [ ] **Step 4: Add entry-page path normalization and `__loadAssetsUrl__`**

In `src/frontend/devops-codecc/index.html`, immediately before the current environment variable declarations inside the first `<script>`, add:

```html
      function normalizeBasePath(value) {
        var rawValue = String(value || '/').trim();
        if (!rawValue || rawValue === '/') {
          return '/';
        }
        var normalized = rawValue.replace(/^\/+|\/+$/g, '');
        return normalized ? '/' + normalized + '/' : '/';
      }

      function normalizeAssetBase(value, fallback) {
        var rawValue = String(value || fallback || '/').trim();
        if (/^https?:\/\//.test(rawValue) || rawValue.indexOf('//') === 0) {
          return rawValue.charAt(rawValue.length - 1) === '/' ? rawValue : rawValue + '/';
        }
        return normalizeBasePath(rawValue);
      }

      function joinUrl(base, path) {
        var baseValue = String(base || '/');
        var pathValue = String(path || '');
        if (/^https?:\/\//.test(pathValue) || pathValue.indexOf('//') === 0) {
          return pathValue;
        }
        if (/^https?:\/\//.test(baseValue)) {
          var url = new URL(baseValue);
          url.pathname = [url.pathname, pathValue].join('/').replace(/\/+/g, '/');
          return url.toString().replace(/\/$/, pathValue ? '' : '/');
        }
        var startsWithRoot = baseValue.indexOf('/') === 0;
        var joined = [baseValue, pathValue]
          .filter(function (item) { return item !== ''; })
          .join('/')
          .replace(/\/+/g, '/');
        return startsWithRoot ? '/' + joined.replace(/^\/+/, '') : joined;
      }

      var BK_SITE_PATH = normalizeBasePath('<%= process.env.BK_SITE_PATH %>');
```

Then replace:

```js
      var BK_STATIC_URL = '<%= process.env.BK_STATIC_URL %>'
```

with:

```js
      var BK_STATIC_URL = normalizeAssetBase('<%= process.env.BK_STATIC_URL %>', BK_SITE_PATH)
      window.BK_SITE_PATH = BK_SITE_PATH
      window.BK_STATIC_URL = BK_STATIC_URL
      window.__loadAssetsUrl__ = function (src) {
        return joinUrl(window.BK_STATIC_URL || '/', src);
      }
```

- [ ] **Step 5: Fix project-id extraction and cookie path in `index.html`**

Replace the production project id block:

```js
          var projectId =
            document.location.pathname.match(/^\/codecc\/([\w.-_]+)\/?/)?.[1] ||
            document.location.search.match(/projectId=([\w.-_]+)/)?.[1] || '';
```

with:

```js
          var sitePath = window.BK_SITE_PATH || '/';
          var pathWithoutSitePath = document.location.pathname;
          if (sitePath !== '/' && pathWithoutSitePath.indexOf(sitePath) === 0) {
            pathWithoutSitePath = '/' + pathWithoutSitePath.slice(sitePath.length);
          }
          var projectId =
            pathWithoutSitePath.match(/^\/codecc\/([\w._-]+)\/?/)?.[1] ||
            document.location.search.match(/projectId=([\w._-]+)/)?.[1] || '';
```

Replace cookie path:

```js
              ';path=/';
```

with:

```js
              ';path=' +
              (window.BK_SITE_PATH || '/');
```

- [ ] **Step 6: Fix iframe devops utility prefix handling**

Replace:

```js
          var prefix = '__BK_CI_PUBLIC_PATH_PREFIX__' ? '__BK_CI_PUBLIC_PATH_PREFIX__' : DEVOPS_SITE_URL
```

with:

```js
          var ciPublicPathPrefix = '__BK_CI_PUBLIC_PATH_PREFIX__';
          var prefix = ciPublicPathPrefix && ciPublicPathPrefix.indexOf('__') !== 0
            ? ciPublicPathPrefix
            : DEVOPS_SITE_URL;
```

- [ ] **Step 7: Verify build still completes**

Run:

```bash
cd /Users/brooklin/project/github/bk-codecc/src/frontend/devops-codecc
npm run build
```

Expected: build exits 0 and `dist/index.html` exists.

- [ ] **Step 8: Commit Task 2**

Run:

```bash
git add src/frontend/devops-codecc/static/webpack_public_path.js src/frontend/devops-codecc/index.html src/frontend/devops-codecc/src/main.js src/frontend/devops-codecc/.bk.production.env
git commit -m "feat(frontend): inject runtime subpath settings"
```

Expected: commit succeeds.

## Task 3: Configure Router Base and Login Callback

**Files:**
- Modify: `src/frontend/devops-codecc/src/router/index.js`
- Modify: `src/frontend/devops-codecc/src/api/index.js`

- [ ] **Step 1: Configure Vue Router base**

Modify `src/frontend/devops-codecc/src/router/index.js`:

```js
const router = new VueRouter({
  mode: 'history',
  base: window.BK_SITE_PATH || '/',
  routes,
});
```

- [ ] **Step 2: Import static URL helper in API wrapper**

In `src/frontend/devops-codecc/src/api/index.js`, add:

```js
import { withStaticUrl } from '@/utils/path';
```

Place it near the other local imports.

- [ ] **Step 3: Use static prefix for login modal callback**

Replace:

```js
        const successUrl = `${window.location.origin}/static/login_success.html`;
```

with:

```js
        const successUrl = `${window.location.origin}${withStaticUrl('/static/login_success.html')}`;
```

- [ ] **Step 4: Verify lint for touched files**

Run:

```bash
cd /Users/brooklin/project/github/bk-codecc/src/frontend/devops-codecc
npx eslint src/router/index.js src/api/index.js src/utils/path.js
```

Expected: exits 0. If the repo's eslint config reports pre-existing style issues outside these edits, fix only issues in the touched lines.

- [ ] **Step 5: Commit Task 3**

Run:

```bash
git add src/frontend/devops-codecc/src/router/index.js src/frontend/devops-codecc/src/api/index.js
git commit -m "feat(frontend): apply site path to router and login callback"
```

Expected: commit succeeds.

## Task 4: Audit and Fix CodeCC-Owned Manual Internal URLs

**Files:**
- Modify: `src/frontend/devops-codecc/src/views/paas/test/design-report.vue`
- Modify additional files only when the audit proves they construct CodeCC-owned root URLs.

- [ ] **Step 1: Run the manual URL audit**

Run:

```bash
cd /Users/brooklin/project/github/bk-codecc
rg -n "window\\.location\\.origin|location\\.origin|/codecc/|/static/login_success|router\\.resolve|\\$router\\.resolve" src/frontend/devops-codecc/src -g '*.js' -g '*.vue'
```

Expected: output lists candidate call sites. Classify each as one of:

- CodeCC internal URL that needs `withSitePath`;
- webpack/static URL that needs `withStaticUrl`;
- external product URL that must stay unchanged;
- router-resolved href that already includes `router.base`.

- [ ] **Step 2: Fix PaaS design report internal URL**

In `src/frontend/devops-codecc/src/views/paas/test/design-report.vue`, add:

```js
import { withSitePath } from '@/utils/path';
```

Replace:

```js
      const url = `${window.location.origin}/codecc/${row.projectId}/task/${row.taskId}/detail`;
```

with:

```js
      const url = `${window.location.origin}${withSitePath(`/codecc/${row.projectId}/task/${row.taskId}/detail`)}`;
```

- [ ] **Step 3: Fix any additional CodeCC-owned root URL from the audit**

For each audited CodeCC internal URL with a literal `/codecc/...`, use this pattern:

```js
import { withSitePath } from '@/utils/path';

const url = `${window.location.origin}${withSitePath('/codecc/demo/task/list')}`;
```

Do not apply this pattern to `window.DEVOPS_SITE_URL`, `window.STREAM_SITE_URL`, `window.PAAS_SERVICE_URL`, `window.PAAS_V3_URL`, `window.IWIKI_SITE_URL`, or IAM URLs.

- [ ] **Step 4: Verify no unclassified root CodeCC URLs remain**

Run:

```bash
cd /Users/brooklin/project/github/bk-codecc
rg -n "window\\.location\\.origin.*\\/codecc/|location\\.origin.*\\/codecc/|`\\$\\{window\\.location\\.origin\\}/codecc/" src/frontend/devops-codecc/src -g '*.js' -g '*.vue'
```

Expected: no output.

- [ ] **Step 5: Run lint for touched files**

Run:

```bash
cd /Users/brooklin/project/github/bk-codecc/src/frontend/devops-codecc
npx eslint src/views/paas/test/design-report.vue src/utils/path.js
```

Expected: exits 0.

- [ ] **Step 6: Commit Task 4**

Run:

```bash
git add src/frontend/devops-codecc/src/views/paas/test/design-report.vue
git commit -m "feat(frontend): respect site path in internal urls"
```

If Step 3 touched additional files, include them in `git add`.

Expected: commit succeeds.

## Task 5: Add Deployment Variables

**Files:**
- Modify: `scripts/deploy-codecc/codecc.properties`
- Modify: `docker-images/core/codecc/gateway/scripts/codecc.env`
- Modify: `helm-charts/core/codecc/templates/gateway/deployment.yaml`

- [ ] **Step 1: Add traditional deployment default**

In `scripts/deploy-codecc/codecc.properties`, near `BK_PUBLIC_PATH_PREFIX`, add:

```properties
# BK_SITE_PATH 默认为 /，用于 CodeCC 前端 history 路由子路径。值必须为 / 或 /path/
BK_SITE_PATH=/
```

- [ ] **Step 2: Add container env template**

In `docker-images/core/codecc/gateway/scripts/codecc.env`, add:

```env
BK_SITE_PATH=$BK_SITE_PATH
```

Place it near the existing `BK_PUBLIC_PATH_PREFIX` or frontend URL variables.

- [ ] **Step 3: Add Helm gateway environment variable**

In `helm-charts/core/codecc/templates/gateway/deployment.yaml`, add:

```yaml
            - name: BK_SITE_PATH
              value: {{ default "/" .Values.config.bkSitePath }}
```

Place it immediately before `BK_PUBLIC_PATH_PREFIX`.

- [ ] **Step 4: Verify Helm template syntax**

Run:

```bash
cd /Users/brooklin/project/github/bk-codecc/helm-charts/core/codecc
helm template codecc . >/tmp/codecc-helm-render.yaml
```

Expected: exits 0 and `/tmp/codecc-helm-render.yaml` contains `name: BK_SITE_PATH`.

- [ ] **Step 5: Commit Task 5**

Run:

```bash
git add scripts/deploy-codecc/codecc.properties docker-images/core/codecc/gateway/scripts/codecc.env helm-charts/core/codecc/templates/gateway/deployment.yaml
git commit -m "feat(deploy): add codecc frontend site path variable"
```

Expected: commit succeeds.

## Task 6: Add Gateway Subpath Rewrite Before SPA Fallback

**Files:**
- Modify: `support-files/codecc/templates/core/gateway#core#vhosts#codecc.frontend.conf`

- [ ] **Step 1: Strip site path in the root frontend location**

Modify `location /` in `support-files/codecc/templates/core/gateway#core#vhosts#codecc.frontend.conf` from:

```nginx
  location / {
    header_filter_by_lua_file 'conf/lua/cors_filter.lua';
    add_header Cache-Control no-store;
    index index.html index.htm;
    try_files $uri @fallback;
  }
```

to:

```nginx
  location / {
    header_filter_by_lua_file 'conf/lua/cors_filter.lua';
    add_header Cache-Control no-store;
    index index.html index.htm;
    rewrite ^__BK_SITE_PATH__(.*)$ /$1 break;
    try_files $uri @fallback;
  }
```

When `BK_SITE_PATH=/`, the rendered rewrite becomes `rewrite ^/(.*)$ /$1 break;`, which keeps root deployment behavior unchanged.

- [ ] **Step 2: Strip site path in html fallback location**

Modify `location ~* \.(html)$` from:

```nginx
  location ~* \.(html)$ {
    header_filter_by_lua_file 'conf/lua/cors_filter.lua';
    add_header Cache-Control no-store;
    try_files $uri  @fallback;
  # 匹配所有以 html结尾的请求
  }
```

to:

```nginx
  location ~* \.(html)$ {
    header_filter_by_lua_file 'conf/lua/cors_filter.lua';
    add_header Cache-Control no-store;
    rewrite ^__BK_SITE_PATH__(.*)$ /$1 break;
    try_files $uri  @fallback;
  # 匹配所有以 html结尾的请求
  }
```

- [ ] **Step 3: Strip site path in static asset location**

Modify `location ~* \.(js|css|ttf)$` from:

```nginx
  location ~* \.(js|css|ttf)$ {
    header_filter_by_lua_file 'conf/lua/cors_filter.lua';
    add_header Cache-Control max-age=2592000;
    try_files $uri  @fallback;
  # 匹配所有以 js,css或tff 结尾的请求
  }
```

to:

```nginx
  location ~* \.(js|css|ttf)$ {
    header_filter_by_lua_file 'conf/lua/cors_filter.lua';
    add_header Cache-Control max-age=2592000;
    rewrite ^__BK_SITE_PATH__(.*)$ /$1 break;
    try_files $uri  @fallback;
  # 匹配所有以 js,css或tff 结尾的请求
  }
```

- [ ] **Step 4: Keep fallback rooted at built index**

Confirm `location @fallback` still rewrites to `/index.html`:

```nginx
  location @fallback {
    header_filter_by_lua_file 'conf/lua/cors_filter.lua';
    add_header Cache-Control no-store;
    rewrite .* /index.html break;
  }
```

Expected: no change is needed in this block.

- [ ] **Step 5: Verify nginx rendered config for root and subpath**

Run the render command used by gateway startup with root values:

```bash
cd /Users/brooklin/project/github/bk-codecc
tmpdir=$(mktemp -d)
cp support-files/codecc/templates/core/gateway#core#vhosts#codecc.frontend.conf "$tmpdir/template.conf"
BK_SITE_PATH=/ docker-images/core/codecc/gateway/scripts/render_tpl -u -p "$tmpdir" -m . "$tmpdir/template.conf"
```

Expected: rendered root config contains `rewrite ^/(.*)$ /$1 break;` inside frontend locations and no new `location ^~ /` block.

Run again with subpath:

```bash
cd /Users/brooklin/project/github/bk-codecc
tmpdir=$(mktemp -d)
cp support-files/codecc/templates/core/gateway#core#vhosts#codecc.frontend.conf "$tmpdir/template.conf"
BK_SITE_PATH=/codecc-demo/ docker-images/core/codecc/gateway/scripts/render_tpl -u -p "$tmpdir" -m . "$tmpdir/template.conf"
```

Expected: rendered subpath config contains `rewrite ^/codecc-demo/(.*)$ /$1 break;` inside frontend locations and fallback to `/index.html`.

- [ ] **Step 6: Run nginx config syntax check when OpenResty is available**

Run:

```bash
openresty -t -c /path/to/rendered/nginx.conf
```

Expected: `syntax is ok` and `test is successful`. If OpenResty is not installed locally, record that this check must run in the gateway image in Task 8.

- [ ] **Step 7: Commit Task 6**

Run:

```bash
git add support-files/codecc/templates/core/gateway#core#vhosts#codecc.frontend.conf
git commit -m "feat(gateway): serve frontend under configured subpath"
```

Expected: commit succeeds.

## Task 7: Add Build Output Verification

**Files:**
- Create: `src/frontend/devops-codecc/scripts/verify-subpath-dist.js`
- Modify: `src/frontend/devops-codecc/package.json`

- [ ] **Step 1: Add dist verifier script**

Create `src/frontend/devops-codecc/scripts/verify-subpath-dist.js`:

```js
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

if (!html.includes(`BK_SITE_PATH`)) {
  fail('dist/index.html does not define BK_SITE_PATH');
}

if (!html.includes(`BK_STATIC_URL`)) {
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
```

- [ ] **Step 2: Add package script**

Modify `src/frontend/devops-codecc/package.json` scripts:

```json
"verify:subpath": "node ./scripts/verify-subpath-dist.js"
```

- [ ] **Step 3: Verify root build output**

Run:

```bash
cd /Users/brooklin/project/github/bk-codecc/src/frontend/devops-codecc
BK_SITE_PATH=/ BK_STATIC_URL=/ npm run build
npm run verify:subpath -- / /
```

Expected: `subpath dist verification passed`.

- [ ] **Step 4: Verify subpath build output**

Run:

```bash
cd /Users/brooklin/project/github/bk-codecc/src/frontend/devops-codecc
BK_SITE_PATH=/codecc-demo/ BK_STATIC_URL=/codecc-demo/ npm run build
npm run verify:subpath -- /codecc-demo/ /codecc-demo/
```

Expected: `subpath dist verification passed`.

- [ ] **Step 5: Commit Task 7**

Run:

```bash
git add src/frontend/devops-codecc/scripts/verify-subpath-dist.js src/frontend/devops-codecc/package.json
git commit -m "test(frontend): verify subpath build output"
```

Expected: commit succeeds.

## Task 8: End-to-End Local Subpath Verification

**Files:**
- Create: `src/frontend/devops-codecc/scripts/serve-subpath-dist.js`
- No production code changes unless verification exposes a defect.

- [ ] **Step 1: Add local static server**

Create `src/frontend/devops-codecc/scripts/serve-subpath-dist.js`:

```js
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
```

- [ ] **Step 2: Build subpath dist**

Run:

```bash
cd /Users/brooklin/project/github/bk-codecc/src/frontend/devops-codecc
BK_SITE_PATH=/codecc-demo/ BK_STATIC_URL=/codecc-demo/ npm run build
```

Expected: build exits 0.

- [ ] **Step 3: Start the local server**

Run:

```bash
cd /Users/brooklin/project/github/bk-codecc/src/frontend/devops-codecc
BK_SITE_PATH=/codecc-demo/ PORT=8099 node scripts/serve-subpath-dist.js
```

Expected: server prints `Serving ... at http://127.0.0.1:8099/codecc-demo/` and stays running.

- [ ] **Step 4: Browser smoke test deep links**

Open these URLs in the in-app browser or Chrome:

```text
http://127.0.0.1:8099/codecc-demo/codecc/demo/task/list
http://127.0.0.1:8099/codecc-demo/codecc/demo/task/123/detail
http://127.0.0.1:8099/codecc-demo/403
```

Expected:

- each page returns the frontend `index.html`;
- JavaScript and CSS requests use `/codecc-demo/`;
- async chunks use `/codecc-demo/`;
- no request for `/js/...`, `/css/...`, or `/static/login_success.html` appears at the domain root.

- [ ] **Step 5: Verify gateway image nginx syntax if local OpenResty was unavailable**

Run the gateway image or a matching OpenResty environment and execute:

```bash
nginx -t
```

Expected: `syntax is ok` and `test is successful`.

- [ ] **Step 6: Commit Task 8**

Run:

```bash
git add src/frontend/devops-codecc/scripts/serve-subpath-dist.js
git commit -m "test(frontend): add local subpath smoke server"
```

Expected: commit succeeds.

## Task 9: Final Regression and Documentation

**Files:**
- Modify docs only if verification reveals deployment notes that should be preserved.

- [ ] **Step 1: Run final frontend lint**

Run:

```bash
cd /Users/brooklin/project/github/bk-codecc/src/frontend/devops-codecc
npm run lint
```

Expected: exits 0.

- [ ] **Step 2: Run final root build**

Run:

```bash
cd /Users/brooklin/project/github/bk-codecc/src/frontend/devops-codecc
BK_SITE_PATH=/ BK_STATIC_URL=/ npm run build
npm run verify:subpath -- / /
```

Expected: both commands exit 0.

- [ ] **Step 3: Run final subpath build**

Run:

```bash
cd /Users/brooklin/project/github/bk-codecc/src/frontend/devops-codecc
BK_SITE_PATH=/codecc-demo/ BK_STATIC_URL=/codecc-demo/ npm run build
npm run verify:subpath -- /codecc-demo/ /codecc-demo/
```

Expected: both commands exit 0.

- [ ] **Step 4: Check final diff**

Run:

```bash
cd /Users/brooklin/project/github/bk-codecc
git status --short
git log --oneline -10
```

Expected:

- worktree is clean after all task commits;
- recent commits correspond to Tasks 1 through 8 plus this final task if docs were updated.

- [ ] **Step 5: Summarize verification results**

Prepare a final note with:

```text
Verified:
- npm run lint
- BK_SITE_PATH=/ BK_STATIC_URL=/ npm run build
- npm run verify:subpath -- / /
- BK_SITE_PATH=/codecc-demo/ BK_STATIC_URL=/codecc-demo/ npm run build
- npm run verify:subpath -- /codecc-demo/ /codecc-demo/
- local browser smoke under /codecc-demo/

Not verified:
- list any environment-only checks that could not run locally, such as gateway image nginx -t
```

- [ ] **Step 6: Commit final documentation if changed**

If Step 5 produced a checked-in doc update, run:

```bash
git add docs/superpowers/specs/2026-05-13-codecc-subpath-design.md docs/superpowers/plans/2026-05-13-codecc-subpath-implementation.md
git commit -m "docs: document codecc subpath verification"
```

Expected: commit succeeds. If no files changed, skip this step.

## Self-Review Notes

- Spec coverage: Tasks 1-4 cover frontend path variables, runtime public path, router base, login callback, internal URL audit, and cookie/project-id behavior. Tasks 5-6 cover deployment variables and gateway fallback. Tasks 7-9 cover build, local serving, and final verification.
- Placeholder scan: no task intentionally leaves unresolved implementation placeholders. Audit-driven Task 4 allows additional files only when the command proves they are CodeCC-owned root URLs.
- Type consistency: helper names are consistent across tasks: `normalizeBasePath`, `normalizeAssetBase`, `joinUrl`, `getSitePath`, `getStaticUrl`, `withSitePath`, and `withStaticUrl`.
