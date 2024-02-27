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
    listData: {
      lintFileList: {
        content: [],
      },
    },
    lintDetail: {
      fileContent: '',
    },
    dupcList: [],
    dupcDetail: {
      blockInfoList: [{}],
    },
    records: {},
  },
  mutations: {
    updateLintList(state, list) {
      state.listData = { ...state.listData, ...list };
    },
    updateDupcList(state, list) {
      state.dupcList = list;
    },
    updateDupcDetail(state, detail) {
      state.dupcDetail = { ...state.dupcDetail, ...detail };
    },
    updateLintDetail(state, detail) {
      state.lintDetail = { ...state.lintDetail, ...detail };
    },
    updateOperateRecords(state, records) {
      state.records = records;
    },
  },
  actions: {
    lintDetail({ commit, state, dispatch }, params, config = {}) {
      if (config.showLoading) {
        commit('setMainContentLoading', true, { root: true });
      }
      const { sortField, sortType, ...data } = params;
      const query = { sortField, sortType };
      // return http.get('/defect/index?invoke=lintdetail', data, { params: query }).then(res => {
      return http
        .post('/defect/api/user/warn/issue/detail', data, {
          params: query,
          cancelPrevious: false,
        })
        .then((res) => {
          const detail = res.data || {};
          return detail;
        })
        .catch((e) => {
          console.error(e);
          return e;
        })
        .finally(() => {
          if (config.showLoading) {
            commit('setMainContentLoading', false, { root: true });
          }
        });
    },
    lintDetailWithoutContent({ commit, state, dispatch }, params, config = {}) {
      if (config.showLoading) {
        commit('setMainContentLoading', true, { root: true });
      }
      const { sortField, sortType, ...data } = params;
      const query = { sortField, sortType };
      // return http.get('/defect/index?invoke=lintdetail', data, { params: query }).then(res => {
      return http
        .post('/defect/api/user/warn/withoutFileContent/issue/detail', data, {
          params: query,
          cancelPrevious: false,
        })
        .then((res) => {
          const detail = res.data || {};
          return detail;
        })
        .catch((e) => {
          console.error(e);
          return e;
        })
        .finally(() => {
          if (config.showLoading) {
            commit('setMainContentLoading', false, { root: true });
          }
        });
    },
    fileContentSegment({ commit, state, dispatch }, params) {
      return http
        .post('/defect/api/user/warn/defectFileContentSegment', params)
        .then((res) => {
          const detail = res.data || {};
          return detail;
        })
        .catch((e) => {
          console.error(e);
          return e;
        });
    },
    lintList({ commit, state, dispatch }, params, config = {}) {
      if (config.showLoading) {
        commit('setMainContentLoading', true, { root: true });
      }
      const { pageNum, pageSize, sortField, sortType, ...data } = params;
      const query = { pageNum, pageSize, sortField, sortType };
      // return http.post('/defect/index?invoke=lintlist', data, { params: query }).then(res => {
      return http
        .post('/defect/api/user/warn/issue/list', data, { params: query })
        .then((res) => {
          const list = res.data || {};
          commit('updateLintList', list);
          return list;
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
    lintParams({ commit }, data) {
      return http
        .get(`/defect/api/user/warn/checker/authors/toolName/${data.toolId}?buildId=${data.buildId}&multiTaskQuery=${data.multiTaskQuery}`)
        .then((res) => {
          const params = res.data || {};
          return params;
        });
    },
    lintOtherParams({ commit }, params) {
      return http
        .post('/defect/api/user/warn/checker/authors/list', params)
        .then(res => res.data || {});
    },
    lintSearchParams({ commit }, data) {
      return http
        .post('/defect/api/user/warn/initpage', data, { cancelPrevious: false })
        .then((res) => {
          const list = res.data || {};
          return list;
        })
        .catch((e) => {
          console.error(e);
        });
    },
    batchEdit({ commit }, data) {
      return http.post('/defect/api/user/warn/batch', data).then(res => res);
    },
    dupcList({ commit, state, dispatch }) {
      return http
        .get('/defect/index?invoke=dupclist')
        .then((res) => {
          const list = res.data || [];
          commit('updateDupcList', list);
          return list;
        })
        .catch(e => e);
    },
    dupcDetail({ commit, state, dispatch }) {
      return http.get('/defect/index?invoke=dupcdetail').then((res) => {
        const detail = res.data || {};
        commit('updateDupcDetail', detail);
        return detail;
      });
    },
    publish({ commit, state, dispatch }, index) {
      const data = { name: state.list[index].name };
      return http
        .post('/tool/publish', data)
        .then((res) => {
          commit('updateTool', index);
          return res;
        })
        .catch(e => e);
    },
    sort({ commit, state, dispatch }, data) {
      return http.post('/tool/sort', data);
    },
    report({ commit, state, dispatch }, data) {
      if (data.showLoading) {
        commit('setMainContentLoading', true, { root: true });
      }
      const { startTime, endTime } = data;
      const query = { startTime, endTime };
      return http
        .get(`/defect/api/user/report/toolName/${data.toolId}`, {
          params: query,
        })
        .then((res) => {
          const charts = res.data || [];
          return charts;
        })
        .catch((e) => {
          console.error(e);
        })
        .finally(() => {
          if (data.showLoading) {
            commit('setMainContentLoading', false, { root: true });
          }
        });
    },
    fileContent({ commit, state, dispatch }, params) {
      return http
        .post('/defect/api/user/warn/fileContentSegment', params)
        .then((res) => {
          const detail = res.data || {};
          return detail;
        })
        .catch((e) => {
          console.error(e);
        });
    },
    getOperateRecords({ commit, state, dispatch, rootState }, data) {
      if (rootState.task.status.status === 1) {
        return;
      }
      return http
        .post(`/defect/api/user/operation/taskId/${data.taskId}`, data.funcId)
        .then((res) => {
          const records = res.data || [];
          commit('updateOperateRecords', records);
          return records;
        })
        .catch(e => e);
    },
    saveSetAuthorityOperaHis({ commit, state, dispatch }, params) {
      return http
        .post('/defect/api/user/operation//settings/authority', params)
        .then(res => res.data || {})
        .catch((e) => {
          console.error(e);
        });
    },
    newVersion({ commit, state, dispatch }, params) {
      return http
        .post(
          `/defect/api/user/checker/tools/${params.toolName}/checkerSets/${params.checkerSetId}/versions/difference`,
          params,
        )
        .then((res) => {
          const data = res.data || {};
          return data;
        })
        .catch((e) => {
          console.error(e);
        });
    },
    getWarnContent({ commit, state, dispatch }, data) {
      return http
        .get(
          `/defect/api/user/checker/detail/toolName/${data.toolName}?checkerKey=${data.checkerKey}`,
          data,
        )
        .then((res) => {
          const content = res.data || [];
          return content;
        })
        .catch((e) => {
          console.error(e);
        });
    },
    getTransferAuthorList({ commit, rootState }) {
      return http
        .get('/defect/api/user/transferAuthor/list')
        .then((res) => {
          const data = res.data || {};
          return data;
        })
        .catch((e) => {
          console.error(e);
        });
    },
    getBuildList({ commit, rootState }, data) {
      // TODO: 多分支接口暂时回滚
      return http
        .get(`/defect/api/user/warn/tasks/${data.taskId}/buildInfosWithBranches`)
        .then((res) => {
          // return http.get(`/defect/api/user/warn/tasks/${data.taskId}/buildInfos`).then((res) => {
          const data = res.data || {};
          return data;
        })
        .catch((e) => {
          console.error(e);
        });
    },
    commentDefect({ commit, state, dispatch }, params) {
      const { singleCommentId, userName, comment, ...query } = params;
      const data = { singleCommentId, userName, comment };
      return http
        .post(
          `/defect/api/user/warn/codeComment/toolName/${query.toolName}`,
          data,
          { params: query },
        )
        .then(res => res || {})
        .catch((e) => {
          console.error(e);
        });
    },
    deleteComment({ commit, state, dispatch }, params) {
      const {
        commentId,
        singleCommentId,
        toolName,
        defectEntityId,
        commentStr,
      } = params;
      return http
        .delete(
          `/defect/api/user/warn/codeComment/commentId/${commentId}/singleCommentId/${singleCommentId}/toolName/${toolName}`,
          { params: { entityId: defectEntityId, comment: commentStr } },
        )
        .then(res => res || {})
        .catch((e) => {
          console.error(e);
        });
    },
    updateComment({ commit, state, dispatch }, params) {
      const { commentId, toolName, singleCommentId, userName, comment } = params;
      const data = { singleCommentId, userName, comment };
      return http
        .put(
          `/defect/api/user/warn/codeComment/commentId/${commentId}/toolName/${toolName}`,
          data,
        )
        .then(res => res || {})
        .catch((e) => {
          console.error(e);
        });
    },
    gatherFile({ commit, state, dispatch }, params) {
      return http
        .post('/defect/api/user/warn/queryFileDefectGather', params)
        .then(res => res || {});
    },
    lintListCloc({ commit }, params) {
      return http
        .get(`/defect/api/user/warn/list/toolName/${params.toolId}/orderBy/${params.type}`)
        .then((res) => {
          const params = res.data || {};
          return params;
        });
    },
    listToolName({ commit }, params) {
      return http
        .post('/defect/api/user/warn/listToolName', params)
        .then(res => res.data || []);
    },
    oauthUrl({ commit, state, dispatch }, params) {
      return http
        .get(`/defect/api/user/repo/oauth/url?toolName=${params.toolName}`)
        .then(res => res.data || {})
        .catch((e) => {
          console.error(e);
        });
    },
    getDimension({ commit }, params) {
      return http
        .post('/task/api/user/task/listDimension', params)
        .then(res => res.data || {});
    },
    defectCommit({ commit }, params) {
      return http
        .post('/defect/api/user/issue/batchSubmitIssue', params)
        .then(res => res || {});
    },
    getTaskList({ commit }, params) {
      return http
        .get('/task/api/user/task/listTaskBase', params)
        .then(res => res.data || []);
    },
  },
};
