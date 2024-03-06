const ignoreList = () => import(/* webpackChunkName: 'defect-list' */ '../views/ignore/list');
const ignoreOperation = () => import(
  /* webpackChunkName: 'defect-list' */ '../views/ignore/ignore-operation');

const routes = [
  {
    path: '/codecc/:projectId/ignore/list',
    name: 'ignoreList',
    component: ignoreList,
    meta: {
      layout: 'outer',
    },
  },
  {
    path: '/codecc/:projectId/ignore/operation/:id?/:entityId?',
    name: 'ignoreOperation',
    props: route => ({ ...route.params }),
    component: ignoreOperation,
    meta: {
      layout: 'outer',
    },
  },
];

export default routes;
