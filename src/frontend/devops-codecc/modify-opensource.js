const fs = require('fs')

// 设置组件库和aegis依赖
const magicVueList =  [
  './package.json',
  './build/webpack.base.conf.babel.js',
  './src/common/bkmagic.js',
  './src/i18n/index.js',
  './src/App.vue',
]
magicVueList.forEach((item) => {
  let fileData = fs.readFileSync(item).toString()
  fileData = fileData.replace(/@tencent\//g, '')
  fs.writeFileSync(item, fileData)
})

// 人员选择器处理
const memberSelectorList =   [
  './src/views/defect/ccn-list.vue',
  './src/views/defect/coverity-list.vue',
  './src/views/task/settings-issue.vue',
  './src/views/task/settings-report.vue',
  './src/views/task/settings-trigger.vue',
  './src/views/defect/defect-list.vue',
  './src/views/ignore/ignore-operation.vue',
]
memberSelectorList.forEach((item) => {
  let fileData = fs.readFileSync(item).toString()
  fileData = fileData.replace(/<bk-member-selector/g, '<bk-tag-input allow-create')
    .replace(/<\/bk-member-selector>/g, '</bk-tag-input>')
  fs.writeFileSync(item, fileData)
})

// 设置内容处理
let settings = fs.readFileSync('./config.js').toString()
settings = settings.replace(
  '\'code\', \'checkerset\', \'report\', \'issue\', \'trigger\', \'ignore\', \'authority\', \'record\', \'manage\'',
  '\'code\', \'checkerset\', \'issue\', \'trigger\', \'ignore\', \'record\', \'manage\'',
)
fs.writeFileSync('./config.js', settings)

// 去掉公告相关接口
const NoticeList =  [
  './src/router/index.js',
]
NoticeList.forEach((item) => {
  let fileData = fs.readFileSync(item).toString()
  fileData = fileData.replace(/getNotice\(\)/g, '')
  fs.writeFileSync(item, fileData)
})
