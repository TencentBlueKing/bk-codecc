/**
 * @file checker
 * @author blueking
 */

import http from '@/api';

export default {
  namespaced: true,
  state: {},
  mutations: {},
  actions: {
    count({ commit, rootState }, data) {
      return http
        .post('/defect/api/user/checker/count', data)
        .then((res) => {
          const data = res.data || {};
          return data;
        })
        .catch((e) => {
          console.error(e);
        });
    },
    list({ commit, rootState }, params) {
      const { pageNum, pageSize, sortField, sortType, ...data } = params;
      const query = { pageNum, pageSize, sortField, sortType };
      return http
        .post('/defect/api/user/checker/list', data, { params: query })
        .then((res) => {
          const data = res.data || {};
          return data;
        })
        .catch((e) => {
          console.error(e);
        });
    },
    create({ commit, rootState }, params) {
      const { uid, ...param } = params;
      return http
        .post('/defect/api/user/checker/custom', param)
        .then((res) => {
          const data = res || {};
          return data;
        })
        .catch((e) => {
          console.error(e);
        });
    },
    deleteCustomeChecker({ commit, rootState }, params) {
      const { uid, ...param } = params;
      return http
        .post('/defect/api/user/checker/deleteCustomChecker', param)
        .then((res) => {
          const data = res || {};
          return data;
        })
        .catch((e) => {
          console.error(e);
        });
    },
    updateCustomeChecker({ commit, rootState }, params) {
      const { uid, ...param } = params;
      return http
        .post('/defect/api/user/checker/updateCustomChecker', param)
        .then((res) => {
          const data = res || {};
          return data;
        })
        .catch((e) => {
          console.error(e);
        });
    },
    permission({ commit, rootState }, params) {
      return http
        .post('/defect/api/user/checker/userManagementPermission', params)
        .then((res) => {
          const data = res || {};
          return data;
        })
        .catch((e) => {
          console.error(e);
        });
    },
  },
};
