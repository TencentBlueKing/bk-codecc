const ABSOLUTE_URL_REGEXP = /^(?:https?:)?\/\//i;
const HTTP_URL_REGEXP = /^https?:\/\//i;

function getWindowValue(key) {
  if (typeof window === 'undefined') {
    return undefined;
  }

  return window[key];
}

function ensureTrailingSlash(value) {
  return value.endsWith('/') ? value : `${value}/`;
}

function stripLeadingSlashes(value) {
  return value.replace(/^\/+/, '');
}

function stripTrailingSlashes(value) {
  return value.replace(/\/+$/, '');
}

function isAbsoluteUrl(value) {
  return ABSOLUTE_URL_REGEXP.test(value);
}

function isHttpUrl(value) {
  return HTTP_URL_REGEXP.test(value);
}

function joinPathname(basePath, path) {
  return `${stripTrailingSlashes(basePath)}/${stripLeadingSlashes(path)}`;
}

export function normalizeBasePath(value = '/') {
  const normalizedValue = String(value || '').trim();
  const basePath = normalizedValue.replace(/^\/+|\/+$/g, '');

  return basePath ? `/${basePath}/` : '/';
}

export function normalizeAssetBase(value, fallback = '/') {
  const normalizedValue = value == null ? '' : String(value).trim();
  const assetBase = normalizedValue || fallback;

  if (isAbsoluteUrl(String(assetBase))) {
    return ensureTrailingSlash(String(assetBase));
  }

  return normalizeBasePath(assetBase);
}

export function joinUrl(base = '/', path = '') {
  const urlPath = String(path || '');

  if (isAbsoluteUrl(urlPath)) {
    return urlPath;
  }

  const normalizedBase = normalizeAssetBase(base);
  const normalizedPath = stripLeadingSlashes(urlPath);

  if (!normalizedPath) {
    return normalizedBase;
  }

  if (isHttpUrl(normalizedBase)) {
    const url = new URL(normalizedBase);
    url.pathname = joinPathname(url.pathname, normalizedPath);

    return url.toString();
  }

  return joinPathname(normalizedBase, normalizedPath);
}

export function getSitePath() {
  return normalizeBasePath(getWindowValue('BK_SITE_PATH') || '/');
}

export function getStaticUrl() {
  return normalizeAssetBase(getWindowValue('BK_STATIC_URL'), getSitePath());
}

export function withSitePath(path) {
  return joinUrl(getSitePath(), path);
}

export function withStaticUrl(path) {
  return joinUrl(getStaticUrl(), path);
}
