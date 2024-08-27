const List = () => import(/* webpackChunkName: 'paas-list' */ '../views/paas/list');

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
];

export default routes;
