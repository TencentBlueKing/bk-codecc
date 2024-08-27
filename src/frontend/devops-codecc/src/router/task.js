const TaskList = () => import(/* webpackChunkName: 'task-list' */ '../views/task/list');
const TaskNew = () => import(/* webpackChunkName: 'task-new' */ '../views/task/new');
const TaskDetail = () => import(/* webpackChunkName: 'task-detail' */ '../views/task/detail');
const TaskLog = () => import(/* webpackChunkName: 'task-log' */ '../views/task/log');

// 任务设置模块，使用嵌套路由方式
const TaskSettings = () => import(/* webpackChunkName: 'task-settings-code' */ '../views/task/settings');
const TaskSettingsCode = () => import(
  /* webpackChunkName: 'task-settings-code' */ '../views/task/settings-code');
const TaskSettingsAuthority = () => import(
  /* webpackChunkName: 'task-settings' */ '../views/task/settings-authority');
const TaskSettingsTrigger = () => import(
  /* webpackChunkName: 'task-settings' */ '../views/task/settings-trigger');
const TaskSettingsReport = () => import(
  /* webpackChunkName: 'task-settings' */ '../views/task/settings-report');
const TaskSettingsIssue = () => import(
  /* webpackChunkName: 'task-settings' */ '../views/task/settings-issue');
const TaskSettingsIgnore = () => import(
  /* webpackChunkName: 'task-settings' */ '../views/task/settings-ignore');
const TaskSettingsRecord = () => import(
  /* webpackChunkName: 'task-settings' */ '../views/task/settings-record');
const TaskSettingsManage = () => import(
  /* webpackChunkName: 'task-settings' */ '../views/task/settings-manage');
const TaskSettingsCheckerset = () => import(
  /* webpackChunkName: 'task-settings' */ '../views/task/settings-checkerset');

const routes = [
  {
    path: '/codecc/:projectId/task/list',
    name: 'task-list',
    component: TaskList,
    meta: {
      layout: 'outer',
      title: '我的任务',
      notNeedToolList: true,
    },
  },

  {
    path: '/codecc/:projectId/coverity/myproject',
    redirect: { name: 'task-list' },
  },

  // 新建任务，包括已有任务未添加工具，当访问路径中带有taskId则识别为已有任务
  {
    path: '/codecc/:projectId/task/:taskId?/new',
    name: 'task-new',
    component: TaskNew,
    meta: {
      layout: 'outer',
      title: '我的任务',
    },
  },
  {
    path: '/codecc/:projectId/task/:taskId/detail',
    name: 'task-detail',
    component: TaskDetail,
    meta: {
      layout: 'inner',
      record: 'none',
    },
  },
  {
    path: '/codecc/:projectId/task/:taskId/detail/log',
    name: 'task-detail-log',
    component: TaskLog,
    meta: {
      layout: 'inner',
      record: 'none',
    },
  },
  {
    path: '/codecc/:projectId/task/:taskId/settings',
    name: 'task-settings',
    component: TaskSettings,
    children: [
      {
        path: '',
        redirect: { name: 'task-settings-code' },
      },
      // 基础信息
      {
        path: 'code',
        name: 'task-settings-code',
        component: TaskSettingsCode,
      },
      // 规则集配置
      {
        path: 'checkerset',
        name: 'task-settings-checkerset',
        component: TaskSettingsCheckerset,
      },
      // 通知报告
      {
        path: 'report',
        name: 'task-settings-report',
        component: TaskSettingsReport,
      },
      // 问题提单
      {
        path: 'issue',
        name: 'task-settings-issue',
        component: TaskSettingsIssue,
      },
      // 扫描触发
      {
        path: 'trigger',
        name: 'task-settings-trigger',
        component: TaskSettingsTrigger,
      },
      // 路径屏蔽
      {
        path: 'ignore',
        name: 'task-settings-ignore',
        component: TaskSettingsIgnore,
      },
      // 人员权限
      {
        path: 'authority',
        name: 'task-settings-authority',
        component: TaskSettingsAuthority,
      },
      // 操作记录
      {
        path: 'record',
        name: 'task-settings-record',
        component: TaskSettingsRecord,
      },
      // 任务管理
      {
        path: 'manage',
        name: 'task-settings-manage',
        component: TaskSettingsManage,
      },
    ],
  },
];

export default routes;
