/**
 * @file app store
 * @author blueking
 */

import http from '@/api';
import axios from 'axios';

export default {
  namespaced: true,
  state: {
    list: [],
    visitable: null,
    projectInfo: {},
  },
  getters: {},
  mutations: {
    updateList(state, list) {
      state.list = list;
    },
    updateVisitable(state, status) {
      state.visitable = status;
    },
    updateProjectInfo(state, data) {
      state.projectInfo = data;
    },
  },
  actions: {
    list({ commit, state, rootState }) {
      if (rootState.loaded['project/updateList'] === true) {
        return state.list;
      }

      // return http.get('/project/index?invoke=list').then(res => {
      return http
        .get('/task/api/user/projects')
        .then((res) => {
          const list = res.data || [];
          commit('updateList', list);
          return list;
        })
        .catch(e => e);
    },
    visitable({ commit, state, rootState }) {
      if (!rootState.projectId) {
        return;
      }
      return http
        .get('/task/api/user/task/multiTaskVisitable')
        .then((res) => {
          const status = res.data;
          commit('updateVisitable', status);
          return status;
        })
        .catch(e => e);
    },
    getProjectInfo({ commit, state, rootState }) {
      if (!rootState.projectId) {
        return;
      }
      // 由小写字母+数字+中划线组成
      const { projectId } = rootState;
      if (!/^[a-z0-9-]+$/.test(projectId)) {
        throw new Error('非法的项目ID');
      }
      axios.get(
        `${window.DEVOPS_SITE_URL}/ms/project/api/user/projects/${projectId}`,
        { withCredentials: true },
      )
        .then((res) => {
          const data = res.data.data || {};
          commit('updateProjectInfo', data);
          return data;
        })
        .catch(e => e);
    },
  },
};
