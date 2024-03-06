const fs = require('fs');

// 设置组件库和aegis依赖
const magicVueList = [
  './package.json',
  './build/webpack.base.conf.babel.js',
  './src/common/bkmagic.js',
  './src/i18n/index.js',
  './src/App.vue',
  './src/common/permission.js',
];
magicVueList.forEach((item) => {
  let fileData = fs.readFileSync(item).toString();
  fileData = fileData.replace(/@tencent\//g, '');
  fs.writeFileSync(item, fileData);
});

// 人员选择器处理
const memberSelectorList = [
  './src/views/defect/ccn-list.vue',
  './src/views/defect/coverity-list.vue',
  './src/views/task/settings-issue.vue',
  './src/views/task/settings-report.vue',
  './src/views/task/settings-trigger.vue',
  './src/views/defect/defect-list.vue',
  './src/views/ignore/ignore-operation.vue',
];
memberSelectorList.forEach((item) => {
  let fileData = fs.readFileSync(item).toString();
  fileData = fileData
    .replace(/<bk-member-selector/g, '<bk-tag-input allow-create')
    .replace(/<\/bk-member-selector>/g, '</bk-tag-input>');
  fs.writeFileSync(item, fileData);
});

// 设置部署环境
let deployEnv = fs.readFileSync('./src/constants/env.js').toString();
deployEnv = deployEnv.replace(/const DEPLOY_ENV = 'tencent'/g, 'const DEPLOY_ENV = \'github\'');
fs.writeFileSync('./src/constants/env.js', deployEnv);

// 去掉公告相关接口
const NoticeList = ['./src/router/index.js'];
NoticeList.forEach((item) => {
  let fileData = fs.readFileSync(item).toString();
  fileData = fileData.replace(/getNotice\(\);/g, '');
  fs.writeFileSync(item, fileData);
});
