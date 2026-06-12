# CodeCC Frontend Subpath Support Design

Date: 2026-05-13

## Background

CodeCC frontend currently assumes that the application is served from the domain root in several places. The frontend uses Vue Router history mode with no router base, production `index.html` extracts project id from paths that start with `/codecc/`, login callback uses `/static/login_success.html`, and the project cookie is written with `path=/`.

The target is to support arbitrary deploy-time subpaths such as `/`, `/codecc/`, or `/bk/codecc/`, while keeping one reusable frontend image/package across environments.

This design follows the iWiki "support subpath deployment" guidance and the bk-job subpath commit pattern:

- inject path globals in `index.html`;
- use one variable for web route base;
- use one variable for static assets;
- set webpack public path at runtime before other frontend imports;
- keep API URLs explicit.

## Goals

- Support arbitrary deploy-time site paths with normalized values: `/` or `/path/`.
- Preserve current root-path behavior when no subpath is configured.
- Keep static asset prefix independent from route prefix so future CDN/static hosting changes do not require route changes.
- Avoid broad route-table rewrites; use Vue Router base for history mode.
- Provide a concrete verification path for build output, local subpath serving, and business smoke testing.

## Non-Goals

- This design does not move CodeCC backend API endpoints under the site subpath. API requests continue to use the configured `AJAX_URL_PREFIX`, which is `/ms` in production today.
- This design does not rewrite BlueKing CI, Stream, IAM, PaaS, or external product URLs with CodeCC's site path. Those URLs are separate applications.
- This design does not change business route names, route params, or page structure.

## Path Model

Two deploy-time variables define the behavior:

- `BK_SITE_PATH`: the web application mount path. It controls Vue Router history base, internal absolute links, login callback path, and cookie scope.
- `BK_STATIC_URL`: the static asset base URL. It controls webpack chunks, generated script/style references, favicon, and files under `static/`.

Both values must be normalized to `/` or a leading-and-trailing slash path such as `/codecc/`. Empty strings, missing values, and invalid single-sided slashes are normalized before use.

`BK_STATIC_URL` may equal `BK_SITE_PATH` for normal subpath deployment. It remains separate to allow static resources to move to another prefix later.

## Frontend Design

### Entry HTML

`src/frontend/devops-codecc/index.html` should inject both variables:

- `window.BK_SITE_PATH` from `process.env.BK_SITE_PATH`, defaulting to `/`;
- `window.BK_STATIC_URL` from `process.env.BK_STATIC_URL`, defaulting to `window.BK_SITE_PATH` when empty.

The entry page should define `window.__loadAssetsUrl__(src)` and use it for favicon, generated JavaScript, generated CSS, and static files that are loaded outside webpack module resolution.

The production project-id extraction should strip `BK_SITE_PATH` before matching `codecc/:projectId`. This keeps the current behavior for `/codecc/demo/task/list` and adds support for `/bk/codecc/codecc/demo/task/list`.

The `X-DEVOPS-PROJECT-ID` cookie should use `path=BK_SITE_PATH` instead of `path=/` so multiple mounted apps on the same host do not accidentally share the cookie.

### Runtime Public Path

Add `src/frontend/devops-codecc/static/webpack_public_path.js` and import it as the first import in `src/frontend/devops-codecc/src/main.js`.

The file sets webpack runtime public path from `window.BK_STATIC_URL`. This ensures async chunks and webpack-managed assets resolve under the deploy-time static prefix.

Webpack-managed assets imported from JavaScript or CSS should stay as imports or relative CSS URLs. Only manually constructed absolute static URLs need to be routed through `__loadAssetsUrl__` or a helper.

### Path Helpers

Add a small frontend utility for path handling so slash normalization is not repeated:

- `normalizeBasePath(value)`: returns `/` or `/path/`;
- `joinUrl(base, path)`: joins path fragments without duplicate or missing slashes;
- `withSitePath(path)`: prefixes a CodeCC internal absolute path with `BK_SITE_PATH`;
- `withStaticUrl(path)`: prefixes a static path with `BK_STATIC_URL`.

The global `window.__loadAssetsUrl__` can use the same logic inline in `index.html`; JavaScript modules can use the helper directly.

### Router

`src/frontend/devops-codecc/src/router/index.js` should keep the route table unchanged and configure Vue Router as:

- `mode: 'history'`;
- `base: window.BK_SITE_PATH || '/'`;
- existing `routes`.

Keeping routes such as `/codecc/:projectId/task/list` avoids broad route churn. The browser URL becomes `<BK_SITE_PATH>/codecc/:projectId/task/list`.

### Manual URL Call Sites

The implementation should audit manual URL construction and only change CodeCC-owned URLs:

- login modal success callback in `src/frontend/devops-codecc/src/api/index.js` should use the static prefix for `static/login_success.html`;
- places using `window.location.origin + routerResolvedHref` for CodeCC internal pages should be verified after adding router base, because `router.resolve(...).href` should already include the base;
- PaaS test report links that manually construct `/codecc/...` should use the site path helper when the URL points back to CodeCC;
- CI/Stream/IAM/PaaS links should continue to use their existing product base URLs.

The iframe `devops-utils.js` loader in `index.html` should keep using `BK_CI_PUBLIC_PATH_PREFIX` or `DEVOPS_SITE_URL`, but it should avoid treating a non-rendered placeholder as a valid URL.

## Deployment Design

Add `BK_SITE_PATH` to deploy-time configuration:

- `src/frontend/devops-codecc/.bk.production.env`;
- `scripts/deploy-codecc/codecc.properties`;
- `docker-images/core/codecc/gateway/scripts/codecc.env`;
- `helm-charts/core/codecc/templates/gateway/deployment.yaml`.

Default value is `/`.

For normal subpath deployment, set both:

- `BK_SITE_PATH=/your/path/`;
- `BK_PUBLIC_PATH_PREFIX=/your/path/`, which feeds the existing `BK_STATIC_URL` path.

The existing gateway startup flow already renders `/data/workspace/frontend/index.html`, so it can continue to inject deployment values into the built frontend entry file.

## Gateway Design

`support-files/codecc/templates/core/gateway#core#vhosts#codecc.frontend.conf` should serve the same frontend under both root and configured subpath.

Required behavior:

- static files under the configured site path return the matching file from `$static_dir_codecc`;
- history-mode page refreshes under the site path fall back to `index.html`;
- root deployment continues to work unchanged;
- `/ms/...` backend API proxying remains available at its current path.

If a future deployment requires APIs under the same subpath, that should be a separate change because it affects `AJAX_URL_PREFIX`, websocket URLs, and backend nginx locations together.

## Verification Plan

### Build Output

Build twice:

- root mode: `BK_SITE_PATH=/` and `BK_STATIC_URL=/`;
- subpath mode: `BK_SITE_PATH=/codecc-demo/` and `BK_STATIC_URL=/codecc-demo/`.

Check the generated `dist/index.html`:

- `window.BK_SITE_PATH` is normalized;
- `window.BK_STATIC_URL` is normalized;
- favicon, scripts, styles, and static callback path use the expected prefix;
- generated runtime/chunk paths do not point to the domain root in subpath mode.

### Local Static Serving

Serve the same `dist` at `/codecc-demo/` using a small nginx or Express static server with history fallback.

Open and refresh:

- `/codecc-demo/codecc/demo/task/list`;
- `/codecc-demo/codecc/demo/task/123/detail`;
- `/codecc-demo/403` and an unknown route.

Inspect Network:

- entry JavaScript and CSS load from `/codecc-demo/`;
- async chunks load from `/codecc-demo/`;
- `static/login_success.html` loads from `/codecc-demo/static/login_success.html`;
- page refresh returns `index.html` rather than a 404.

### Business Smoke

In an environment with backend access or mocks:

- current-user request and CSRF header injection still work;
- task list, task detail, defect list, checker set, and ignore pages route correctly;
- `X-DEVOPS-PROJECT-ID` is set for the configured path;
- 401 login modal uses a callback URL under the static prefix;
- internal new-window links do not duplicate or drop the site path;
- external CI/Stream/IAM/PaaS links remain unchanged.

## Risks and Mitigations

- Internal links built with `window.location.origin` may be inconsistent. Mitigation: audit all CodeCC-owned manual URL call sites and prefer router-resolved hrefs with the configured router base.
- Some static references may be hidden in templates or string literals. Mitigation: search for `/static`, `static/`, `location.origin`, and `window.open` during implementation and verify through Network.
- Deployment may configure `BK_SITE_PATH` and `BK_PUBLIC_PATH_PREFIX` inconsistently. Mitigation: document that normal subpath deployment sets both to the same normalized value unless static assets intentionally use another prefix.
- API subpath support may be requested later. Mitigation: keep the current design explicit that API remains controlled by `AJAX_URL_PREFIX`, so a future API-path change can be scoped separately.

## Acceptance Criteria

- CodeCC works from `/` exactly as before.
- CodeCC works from an arbitrary configured subpath without rebuilding for that specific path.
- Deep-link refreshes under the subpath load the frontend app.
- Webpack async chunks and static files load under `BK_STATIC_URL`.
- Vue Router navigation and router-resolved internal links include `BK_SITE_PATH`.
- Login callback and project cookie behavior work under the configured subpath.
