/**
 * @file app store
 * @author blueking
 */

import http from '@/api'

export default {
  namespaced: true,
  state: {
    list: [],
    visitable: null,
  },
  getters: {
  },
  mutations: {
    updateList(state, list) {
      state.list = list
    },
    updateVisitable(state, status) {
      state.visitable = status
    },
  },
  actions: {
    list({ commit, state, rootState }) {
      if (rootState.loaded['project/updateList'] === true) {
        return state.list
      }

      // return http.get('/project/index?invoke=list').then(res => {
      return http.get('/task/api/user/projects').then((res) => {
        const list = res.data || []
        commit('updateList', list)
        return list
      })
        .catch(e => e)
    },
    visitable({ commit, state, rootState }) {
      if (!rootState.projectId) {
        return
      }
      return http.get('/task/api/user/task/multiTaskVisitable').then((res) => {
        const status = res.data
        commit('updateVisitable', status)
        return status
      })
        .catch(e => e)
    },
  },
}
