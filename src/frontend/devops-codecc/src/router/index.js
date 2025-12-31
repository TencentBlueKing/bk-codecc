/**
 * @file router 配置
 * @author blueking
 */

import Vue from 'vue';
import VueRouter from 'vue-router';

import store from '@/store';
import http from '@/api';
import preload, {
  getTaskDetail,
  getToolMeta,
  getToolList,
  getNotice,
  getRbacPermission,
  getIsProjectManager,
} from '@/common/preload';

import taskRoutes from './task';
import defectRoutes from './defect';
import checkerRoutes from './checker';
import checkersetRoutes from './checkerset';
import ignoreRoutes from './ignore';
import paasRoutes from './paas';

Vue.use(VueRouter);

const NotFound = () => import(/* webpackChunkName: 'none' */ '../views/404');
const Auth = () => import(/* webpackChunkName: 'auth' */ '../views/403');
const Serve = () => import(/* webpackChunkName: 'serve' */ '../views/500');

const rootRoutes = [
  {
    path: '/403',
    name: '403',
    component: Auth,
    meta: {
      layout: 'full',
    },
  },
  {
    path: '/500',
    name: '500',
    component: Serve,
    meta: {
      layout: 'full',
    },
  },
  // 404
  {
    path: '*',
    name: '404',
    component: NotFound,
    meta: {
      layout: 'full',
    },
  },
];
const defaultRouters = [
  {
    path: '/',
    redirect: { name: 'task-list' },
  },
  {
    path: '/codecc/:projectId',
    redirect: { name: 'task-list' },
  },
  {
    path: '/codecc/:projectId/*',
    redirect: { name: 'task-list' },
  },
];

const routes = rootRoutes.concat(
  taskRoutes,
  defectRoutes,
  checkerRoutes,
  checkersetRoutes,
  ignoreRoutes,
  paasRoutes,
  defaultRouters,
);

const router = new VueRouter({
  mode: 'history',
  routes,
});

const cancelRequest = async () => {
  const allRequest = http.queue.get();
  const requestQueue = allRequest.filter(request => request.cancelWhenRouteChange);
  await http.cancel(requestQueue.map(request => request.requestId));
};

let preloading = true;
let canceling = true;
// let pageMethodExecuting = true

router.beforeEach(async (to, from, next) => {
  canceling = true;
  await cancelRequest();
  canceling = false;

  try {
    // const projectList = await getProjectList()
    // 获取蓝盾跳转过来时的项目id
    if (Object.prototype.hasOwnProperty.call(to.query, 'bkci-projectId')) {
      store.commit('updateProjectId', to.query['bkci-projectId']);
      next({
        name: 'task-list',
        params: { projectId: to.query['bkci-projectId'] },
        replace: true,
      });
    }
  } catch (e) {
    console.error(e);
  }

  if (to.params.projectId) {
    store.commit('updateProjectId', to.params.projectId);
  }
  if (store.state.project.visitable === null) {
    await store.dispatch('project/visitable');
  }
  if (to.params.taskId) {
    store.commit('updateTaskId', to.params.taskId);
    if (to.meta.needDetail && !store.state.task.detail.nameEn) {
      await getTaskDetail();
    }
    if (
      to.name === 'defect-defect-list'
      || to.name === 'defect-coverity-list'
      || to.name === 'defect-klocwork-list'
      || to.name === 'defect-pinpoint-list'
    ) {
      next({
        name: 'defect-common-list',
        query: to.query,
        params: to.params,
        replace: true,
      });
    }
    // await getTaskDetail()
  }
  if (store.state.isRbac === undefined) {
    getRbacPermission();
  }

  if (store.state.isProjectManager === undefined && store.state.projectId) {
    getIsProjectManager();
  }

  if (!Object.prototype.hasOwnProperty.call(to.meta, 'layout')) {
    to.meta.layout = 'inner';
    next();
  } else {
    next();
  }
});

router.afterEach(async (to, from) => {
  // store.commit('setMainContentLoading', true)

  const pageDataMethods = [];
  const routerList = to.matched;
  const routeParams = to.params;

  if (routeParams.projectId) {
    store.commit('updateProjectId', routeParams.projectId);
  }

  try {
    if (to.params.taskId) {
      getTaskDetail();
    }
    // 当store里面基础数据还没有，且页面需要这些元素，先加载
    if (!store.state.toolMeta.LANG.length && to.meta && !to.meta.notNeedMeta) {
      getToolMeta();
    }
    if (!store.state.tool.mapList.CCN && to.meta && !to.meta.notNeedToolList) {
      getToolList();
    }

    preloading = true;
    await preload();
    preloading = false;
    window.changeAlert = false;
  } catch (e) {
    console.error(e, e.message);
  }

  routerList.forEach((r) => {
    const fetchPageData = r.instances.default && r.instances.default.fetchPageData;
    if (fetchPageData && typeof fetchPageData === 'function') {
      pageDataMethods.push(r.instances.default.fetchPageData());
    }
  });

  try {
    // pageMethodExecuting = true
    await Promise.all(pageDataMethods);
    // pageMethodExecuting = false
  } catch (e) {
    console.error(e, e.message);
  }

  if (!preloading && !canceling) {
    // store.commit('setMainContentLoading', false)
  }
});

export default router;
