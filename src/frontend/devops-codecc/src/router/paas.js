const List = () => import(/* webpackChunkName: 'paas-list' */ '../views/paas/list');
const Overview = () => import(/* webpackChunkName: 'paas-overview' */ '../views/paas/overview');

const routes = [
  {
    path: '/paas',
    name: 'paas',
    redirect: { name: 'paas-list' },
  },
  {
    path: '/paas/ignored/:toolName/list',
    name: 'paas-list',
    component: List,
    meta: {
      layout: 'full',
    },
  },
  {
    path: '/paas/:toolName/overview',
    name: 'paas-overview',
    component: Overview,
    meta: {
      layout: 'full',
    },
  },
  {
    path: '/paas/test/design/:toolName',
    name: 'paas-test-design',
    component: () => import(/* webpackChunkName: 'paas-test-design' */ '../views/paas/test/design'),
    meta: {
      layout: 'full',
    },
  },
  {
    path: '/paas/test/design/:toolName/report',
    name: 'paas-test-design-report',
    component: () => import(/* webpackChunkName: 'paas-test-design-report' */ '../views/paas/test/design-report'),
    meta: {
      layout: 'full',
    },
  },
  {
    path: '/paas/test/random/:toolName',
    name: 'paas-test-random',
    component: () => import(/* webpackChunkName: 'paas-test-random' */ '../views/paas/test/random'),
    meta: {
      layout: 'full',
    },
  },
  {
    path: '/paas/test/gray/:toolName/result',
    name: 'paas-test-gray-result',
    component: () => import(/* webpackChunkName: 'paas-test-result-gray' */ '../views/paas/test/result-gray'),
    meta: {
      layout: 'full',
    },
  },
];

export default routes;
