/**
 * @file app store
 * @author blueking
 */
/* eslint-disable max-len */

import http from '@/api';
import { json2Query } from '@/common/util';

export default {
  namespaced: true,
  state: {
    list: {
      enableTasks: [],
      disableTasks: [],
    },
    detail: {
      nameEn: '',
      nameCn: '',
      langs: [],
      enableToolList: [],
      disableToolList: [],
    },
    isDetailInit: false,
    ignore: {},
    ignoreTree: {},
    codes: {},
    status: {},
  },
  getters: {},
  mutations: {
    updateList(state, list) {
      state.list = { ...state.list, ...list };
    },
    updateDetail(state, detail) {
      state.detail = Object.assign({}, { enableToolList: [] }, detail);
    },
    updateIgnore(state, ignore) {
      state.ignore = ignore;
    },
    updateIgnoreTree(state, ignoreTree) {
      state.ignoreTree = ignoreTree;
    },
    updateCodeBase(state, codes) {
      state.codes = codes;
    },
    updateStatus(state, status) {
      state.status = status;
    },
  },
  actions: {
    status({ commit, state, rootState }) {
      commit('setMainContentLoading', false, { root: true });
      if (!rootState.projectId) {
        return;
      }
      return http
        .get('/task/api/user/task/status')
        .then((res) => {
          const status = res.data || {};
          commit('updateStatus', status);
          return status;
        })
        .catch(e => e);
    },
    list({ commit, state, rootState }, params) {
      if (!rootState.projectId) {
        return;
      }
      if (params.showLoading) {
        commit('setMainContentLoading', true, { root: true });
      }
      const { orderType } = params;
      delete params.orderType;
      return http
        .post(`/task/api/user/task/taskSortType/${orderType}`, params)
        .then((res) => {
          const list = res.data || {};
          return list;
        })
        .catch(e => e)
        .finally(() => {
          if (params.showLoading) {
            commit('setMainContentLoading', false, { root: true });
          }
        });
    },
    editTaskTop({ commit, state, rootState }, params) {
      if (!rootState.projectId) {
        return;
      }
      return http
        .put(`/task/api/user/task/top/config/taskId/${params.taskId}/topFlag/${params.topFlag}`)
        .then(res => res)
        .catch(e => e);
    },
    requestRepolist({ commit, state, rootState }, idList) {
      if (!rootState.projectId) {
        return;
      }
      return http
        .post('/defect/api/user/repo/list', idList)
        .then((res) => {
          const list = res.data || {};
          return list;
        })
        .catch(e => e);
    },
    basicList({ commit, state, rootState }) {
      if (!rootState.projectId) {
        return;
      }
      return http
        .get('/task/api/user/task/base')
        .then((res) => {
          const list = res.data || {};
          commit('updateList', list);
          return list;
        })
        .catch(e => e);
    },
    detail({ commit, state, rootState }, config = {}) {
      state.isDetailInit = false;
      if (config.showLoading) {
        commit('setMainContentLoading', true, { root: true });
      }
      // return http.get('/task/index?invoke=detail').then(res => {
      return http
        .get('/task/api/user/task/taskInfo')
        .then((res) => {
          const detail = res.data || [];
          commit('updateDetail', detail);
          state.isDetailInit = true;
          return detail;
        })
        .catch((e) => {
          console.error(e);
        })
        .finally(() => {
          if (config.showLoading) {
            commit('setMainContentLoading', false, { root: true });
          }
        });
    },
    basicInfo({ commit, state, rootState }, params) {
      if (params.showLoading) {
        commit('setMainContentLoading', true, { root: true });
      }
      return http
        .get(`/task/api/user/task/taskId/${params.taskId}`)
        .then((res) => {
          const task = res.data || {};
          return task;
        })
        .catch(e => e)
        .finally(() => {
          if (params.showLoading) {
            commit('setMainContentLoading', false, { root: true });
          }
        });
    },
    memberInfo({ commit, state, rootState }) {
      return http
        .get('/task/api/user/task/memberList')
        .then((res) => {
          const task = res.data || {};
          return task;
        })
        .catch(e => e);
    },
    updateBasicInfo({ commit, state, rootState }, params) {
      return http
        .put('/task/api/user/task', params)
        .then((res) => {
          const data = res.data || {};
          return data;
        })
        .catch(e => e);
    },
    create({ commit, rootState }, data) {
      data.projectName = (
        rootState.project.list.find(project => project.projectId === rootState.projectId) || {}
      ).projectName;
      // return http.post('/task/index?invoke=create', data).then(res => {
      return http.post('/task/api/user/task', data).then((res) => {
        const data = res.data || {};
        commit('updateTaskId', data.taskId, { root: true });
        return data;
      });
    },
    update({ commit, rootState }, data) {
      data.projectName = (
        rootState.project.list.find(project => project.projectId === rootState.projectId) || {}
      ).projectName;
      return http.put('/task/api/user/task', data).then((res) => {
        const data = res.data || {};
        commit('updateTaskId', data.taskId, { root: true });
        return data;
      });
    },
    createIgnore({ commit, rootState }, data) {
      // return http.post('/task/index?invoke=create', data).then(res => {
      return http
        .post('/task/api/user/task/add/filter/path', data)
        .then((res) => {
          const create = res.data || [];
          return create;
        })
        .catch(e => e);
    },
    deleteIgnore({ commit, rootState }, params) {
      return http
        .delete('/task/api/user/task/del/filter', params)
        .then((res) => {
          const data = res.data || [];
          return data;
        })
        .catch(e => e);
    },
    ignore({ commit, state, rootState }, taskId) {
      return http
        .get(`/task/api/user/task/filter/path/${taskId}`)
        .then((res) => {
          // return http.get('/task/index?invoke=ignore').then(res => {
          const ignore = res.data || {};
          commit('updateIgnore', ignore);
          return ignore;
        })
        .catch(e => e);
    },
    ignoreTree({ commit, state, rootState }) {
      return http
        .get('/task/api/user/task/filter/path/tree')
        .then((res) => {
          // return http.get('/task/index?invoke=ignoreTree').then(res => {
          const ignoreTree = res.data || {};
          commit('updateIgnoreTree', ignoreTree);
          return ignoreTree;
        })
        .catch(e => e);
    },
    trigger({ commit, rootState }, data) {
      return http
        .post(
          `/task/api/user/task/taskId/${data.taskId}/scanConfiguration`,
          data,
        )
        .then((res) => {
          const data = res.data || {};
          return data;
        });
    },
    getRepoList(store, params = {}) {
      // return http.get('/repo/index?invoke=list', { params }).then(res => {
      return http
        .get(`/task/api/user/tool/repos/projCode/${params.projCode}`)
        .then((res) => {
          const data = res.data || [];
          return data;
        });
    },
    checkname(store, params) {
      // return http.get('/task/index?invoke=checkname', { params, globalError: false })
      return http.get(
        `/task/api/user/task/duplicate/streamName/${params.nameEn}`,
        { globalError: false },
      );
    },
    addTool({ commit }, data) {
      return http.post('/task/api/user/task', data);
    },
    addTestTool({ commit }, data) {
      return http.post('/task/api/user/test/task', data);
    },
    changeToolStatus({ commit }, data) {
      return http.put('/task/api/user/tool/status', data);
    },
    overView({ commit }, data) {
      if (data.showLoading) {
        commit('setMainContentLoading', true, { root: true });
      }
      // return http.post('/task/index?invoke=overview', data).then(res => {
      //     return res.data || {}
      // })
      // const extraParams = data.buildNum ? `?buildNum=${data.buildNum}` : ''
      return http
        .get(`/task/api/user/task/overview/${data.taskId}?${json2Query(data)}`)
        .then((res) => {
          const data = res.data || {};
          return data;
        })
        .finally(() => {
          if (data.showLoading) {
            commit('setMainContentLoading', false, { root: true });
          }
        });
    },
    startManage({ commit }, taskId) {
      return http.put('/task/api/user/task/start');
    },
    stopManage({ commit }, data) {
      return http.put('/task/api/user/task/stop', data);
    },
    deleteTask({ commit }, data) {
      return http.put('/task/api/user/task/delete', data);
    },
    relatedPipelines({ commit }) {
      return http.get('/task/api/user/task/related/pipelines');
    },
    getCodeMessage({ commit }, store) {
      return http
        .get('/task/api/user/task/code/lib')
        .then((res) => {
          const data = res.data || [];
          commit('updateCodeBase', data);
          return data;
        })
        .catch(e => e);
    },
    saveCodeMessage({ commit }, params) {
      return http.put('/task/api/user/task/code/lib/update', params);
    },
    triggerAnalyze({ commit }) {
      return http.post('/task/api/user/task/execute');
    },
    getBranches({ commit }, data) {
      return http.get(`/task/api/user/tool/branches?projCode=${data.projCode}&url=${data.url}&type=${data.type}`);
    },
    getCompileTool({ commit }, data) {
      return http.get(`/task/api/user/buildEnv?os=${data}`);
    },
    notifyCustom({ commit }, data) {
      return http
        .post('/task/index?invoke=notifyCustom', data)
        .then(res => res.data || {});
    },
    saveReport({ commit }, data) {
      return http
        .post('/task/api/user/task/report', data)
        .then(res => res.data || {});
    },
    getRelateCheckerSetTools({ commit }, data) {
      return http.get('/defect/api/user/backendParams');
    },
    updateMembers({ commit }, data) {
      return http
        .put(`/task/api/user/task/member/taskId/${data.taskId}`, data)
        .then(res => res.data || {});
    },
    getYml({ commit }, taskId) {
      return http
        .get(`/task/api/user/task/code//yml/filter/taskId/${taskId}/list`)
        .then(res => res.data || {});
    },
    personal({ commit }, data) {
      return http
        .get('/defect/api/user/statistic/overview/personal')
        .then(res => res.data || {});
    },
    taskLog({ commit }, params) {
      return http
        .get(`/defect/api/user/tasklog/overview?${json2Query(params)}`)
        .then((res) => {
          const data = res.data || {};
          return data;
        });
    },
    issueTypeList({ commit }, params) {
      return http
        .get('/task/api/user/issue/listSubSystemInfo')
        .then(res => res.data || []);
    },
    taskIssue({ commit }, params) {
      return http
        .get(`/task/api/user/issue/task/${params.taskId}/getTaskIssueByTaskId`, { params })
        .then(res => res.data || {});
    },
    getLatestRelationship({ commit }, params) {
      return http
        .get(`/task/api/user/issue/task/${params.taskId}/getLatestRelationship`, { params })
        .then(res => res.data || {});
    },
    updateTaskIssue({ commit }, params) {
      return http
        .put('/task/api/user/issue/updateTaskIssue', params)
        .then(res => res || {});
    },
    getOpenScanAndPreProdCheckerSetMap({ commit }, params) {
      return http
        .get('/task/api/user/getOpenScanAndPreProdCheckerSetMap')
        .then(res => res.data || {});
    },
    getOpenIdeDetailData({ commit }, data) {
      return http.post('/task/api/user/preCI/get/local/project/ide/info', data).then(res => res.data || {});
    },
    getTaskOpenIdeConfig({ commit }) {
      return http.get('/task/api/user/preCI/task/config').then(res => res);
    },
  },
};
