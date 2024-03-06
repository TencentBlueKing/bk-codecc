// const mockServer = require('./mock-server');
// const fs = require('fs');

module.exports = {
  host: process.env.BK_APP_HOST,
  port: process.env.BK_APP_PORT,
  // publicPath: process.env.BK_STATIC_URL,
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
    devServer: {
      https: true,
      client: {
        overlay: false,
      },
    },
  },
};
