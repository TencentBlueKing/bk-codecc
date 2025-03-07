/**
 * @file 页面公共请求即每切换 router 时都必须要发送的请求
 * @author blueking
 */

import store from '@/store';

const config = {
  fromCache: false,
  cancelWhenRouteChange: false,
};

/**
 * 获取 user 信息
 *
 * @return {Promise} promise 对象
 */
function getUser() {
  // eslint-disable-line
  return store.dispatch('userInfo', config);
}

export function getRbacPermission() {
  return store.dispatch('getRbacPermission');
}

export function getIsProjectManager() {
  return store.dispatch('getIsProjectManager');
}

/**
 * 获取工具元数据
 *
 * @returns {Object}
 */
export function getToolMeta() {
  return store.dispatch('getToolMeta', config);
}

/**
 * 获取工具列表
 *
 * @returns {Object}
 */
export function getToolList() {
  return store.dispatch('tool/list', config);
}

/**
 * 获取任务列表
 *
 * @returns {Object}
 */
export function getTaskList() {
  return store.dispatch('task/basicList', config);
}

/**
 * 获取任务详情，在路由钩子中统一调用
 *
 * @returns {Object}
 */
export function getTaskDetail() {
  return store.dispatch('task/detail', config);
}

/**
 * 获取项目列表
 *
 * @returns {Object}
 */
export function getProjectList() {
  return store.dispatch('project/list', config);
}

/**
 * 获取任务状态
 *
 * @returns {Object}
 */
export function getTaskStatus() {
  return store.dispatch('task/status', config);
}

/**
 * 获取公告
 * noticeType 1(维护通知，小黄条), 2(公告通知，弹窗), 3(新功能弹窗)
 */
export function getNotice() {
  store.dispatch('op/notice', { noticeType: '1' });
  store.dispatch('op/notice', { noticeType: '2' });
  store.dispatch('op/notice', { noticeType: '3' });
}

export default function () {
  if (store.state.task.status.status === 1) {
    return false;
  }
  return true;
  // if (store.state.task.status.gongfengProjectId) {
  //     return Promise.all([
  //         getToolMeta(),
  //         getToolList()
  //     ])
  // }
  // return Promise.all([
  //     getToolMeta(),
  //     getToolList(),
  //     getTaskList()
  // ])
}
