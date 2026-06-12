// const mockServer = require('./mock-server');
// const fs = require('fs');

function normalizeBasePath(value) {
  if (!value) {
    return '/';
  }
  const text = String(value).trim();
  if (!text || text === '/') {
    return '/';
  }
  return `/${text.replace(/^\/+|\/+$/g, '')}/`;
}

const isDevelopment = process.env.NODE_ENV === 'development';
const devSitePath = normalizeBasePath(process.env.BK_SITE_PATH || '/');

const devServerConfig = {
  https: true,
  client: {
    overlay: false,
  },
};

if (isDevelopment) {
  devServerConfig.devMiddleware = {
    publicPath: devSitePath,
  };
  devServerConfig.historyApiFallback = {
    index: `${devSitePath}index.html`,
    disableDotRule: true,
  };
}

module.exports = {
  host: process.env.BK_APP_HOST,
  port: process.env.BK_APP_PORT,
  ...(isDevelopment ? { publicPath: devSitePath } : {}),
  cache: true,
  open: false,
  replaceStatic: {
    key: '__BK_PUBLIC_PATH_PREFIX__',
  },
  // resource: {
  //   main: {
  //     entry: './src/main',
  //     html: {
  //       filename: 'index.html',
  //       template: './index-dev.html',
  //     },
  //   },
  // },

  // webpack config 配置
  configureWebpack: {
    devServer: devServerConfig,
  },
};
