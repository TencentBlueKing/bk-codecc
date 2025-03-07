/**
 * @file checker
 * @author blueking
 */
/* eslint-disable max-len */

import request from '@/common/request';
import http from '@/api';

export default {
  namespaced: true,
  state: {},
  mutations: {},
  actions: {
    fetchIgnoreList({ commit }) {
      return http.get('/defect/api/user/ignoreType/project/list').catch((e) => {
        console.error(e);
        return { data: [] };
      });
    },
    fetchIgnoreListCount({ commit }) {
      return http
        .get('/defect/api/user/ignoreType/project/defect/stat')
        .catch((e) => {
          console.error(e);
          return { data: [] };
        });
    },
    createIgnore({ commit }, params) {
      return http
        .post('/defect/api/user/ignoreType/project/save', params)
        .catch((e) => {
          console.error(e);
        });
    },
    deleteIgnore({ commit }, params) {
      return http
        .post('/defect/api/user/ignoreType/project/updateStatus', params)
        .catch((e) => {
          console.error(e);
        });
    },
    updateNotify({ commit }, params) {
      return http
        .post('/defect/api/user/ignoreType/project/save', params)
        .catch((e) => {
          console.error(e);
        });
    },
    getIgnoreInfo({ commit }, id) {
      return http
        .get(`/defect/api/user/ignoreType/project/detail?ignoreTypeId=${id}`)
        .catch((e) => {
          console.error(e);
        });
    },
    getIgnorePermission({ commit }, id) {
      return http
        .get('/defect/api/user/ignoreType/project/hasAddPermissions')
        .catch((e) => {
          console.error(e);
        });
    },
    fetchIgnoreApprovalList({ commit }, params) {
      return http.get(`/defect/api/user/ignoreApproval/project/config/list?pageNum=${params.pageNum}&pageSize=${params.pageSize}`).catch((e) => {
        console.error(e);
        return { data: [] };
      });
    },
    createIgnoreApproval({ commit }, params) {
      return http
        .post('/defect/api/user/ignoreApproval/project/saveConfig', params)
        .catch((e) => {
          console.error(e);
        });
    },
    fetchIgnoreApprovalDetail({ commit }, ignoreApprovalId) {
      return http
        .get(`/defect/api/user/ignoreApproval/project/config/detail?ignoreApprovalId=${ignoreApprovalId}`)
        .catch((e) => {
          console.error(e);
        });
    },
    deleteIgnoreApproval({ commit }, ignoreApprovalId) {
      return http
        .put(`/defect/api/user/ignoreApproval/project/config/delete?ignoreApprovalId=${ignoreApprovalId}`)
        .catch((e) => {
          console.error(e);
        });
    },
    fetchApproverList({ commit }) {
      return http
        .get('/task/api/user/metadatas?metadataType=APPROVER_TYPE')
        .catch((e) => {
          console.error(e);
        });
    },
    fetchTaskScopeType({ commit }) {
      return http
        .get('/task/api/user/metadatas?metadataType=IGNORE_APPROVAL_TASK_SCOPE_TYPE')
        .catch((e) => {
          console.error(e);
        });
    },
  },
};
