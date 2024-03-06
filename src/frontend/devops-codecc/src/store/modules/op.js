/**
 * @file 公告
 * @author blueking
 */
/* eslint-disable max-len */

import http from '@/api';

export default {
  namespaced: true,
  state: {
    maintain: {},
    notice: {},
    newVersion: {},
    maintainClose: false,
  },
  getters: {
    isMaintainClose(state) {
      const localKey = `codecc-maintain-${state.maintain.noticeSerial}`;
      return state.maintainClose || window.localStorage.getItem(localKey);
    },
  },
  mutations: {
    updatemaintain(state, message) {
      state.maintain = message;
    },
    updatenotice(state, message) {
      state.notice = message;
    },
    updatenewVersion(state, message) {
      state.newVersion = message;
    },
    updateMaintainClose(state, status) {
      state.maintainClose = status;
    },
  },
  actions: {
    notice({ commit, state, rootState }, config) {
      const params = Object.assign({ operType: 'Q', findType: '3' }, config);
      return http
        .post(`${window.OP_AJAX_URL_PREFIX}/api/op/notice`, params)
        .then((res) => {
          const configMap = {
            1: 'maintain',
            2: 'notice',
            3: 'newVersion',
          };
          const data = res.data || {};
          commit(`update${configMap[config.noticeType]}`, data);
          return data;
        });
    },
  },
};
