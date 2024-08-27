import http from '@/api';

const prefix = '/defect/api/user/ignored/negative/defect';

export default {
  namespaced: true,
  state: {
    list: [],
    listLoading: false,
    totalCount: 0,
  },
  getters: {},
  mutations: {
    listLoading(state, loading) {
      state.listLoading = loading;
    },
    updateList(state, list) {
      state.list = list;
    },
    updateTotalCount(state, totalCount) {
      state.totalCount = totalCount;
    },
  },
  actions: {
    getStatistics({ commit }, payload) {
      const { toolName, days } = payload;
      return http.get(`${prefix}/statistic/toolName/${toolName}/last/${days}`)
        .then(response => response.data || {});
    },
    getOptional({ commit }, payload) {
      const { toolName, days } = payload;
      return http.get(`${prefix}/list/optional/toolName/${toolName}`)
        .then(response => response.data || {});
    },
    getCount({ state, commit }, payload) {
      const { toolName, days, params } = payload;
      return http.post(`${prefix}/count/defect/toolName/${toolName}/last/${days}`, params)
        .then((response) => {
          const { data } = response;
          commit('updateTotalCount', data);
          return data || {};
        });
    },
    getList({ state, commit }, payload) {
      const { toolName, days, params, lastInd = '', sortField, sortType } = payload;
      commit('listLoading', true);
      return http.post(`${prefix}/list/defect/toolName/${toolName}/last/${days}
?pageSize=100&lastInd=${lastInd}&sortField=${sortField}&sortType=${sortType}`, params)
        .then((response) => {
          const { data } = response;
          let { list } = state;
          if (lastInd) {
            list = list.concat(data);
          } else {
            list = data;
          }
          commit('updateList', list);
          return data || {};
        })
        .finally(() => {
          commit('listLoading', false);
        });
    },
    processDefect({ commit }, payload) {
      const { toolName, entityId, ...params } = payload;
      return http.post(`${prefix}/process/defect/toolName/${toolName}/entityId/${entityId}`, params)
        .then(response => response);
    },
    getDetail({ commit }, payload) {
      const { toolName } = payload;
      return http.post(`${prefix}/withoutFileContent/detail/toolName/${toolName}`, payload)
        .then(response => response.data || {});
    },
    fileContentSegment({ commit }, payload) {
      const { toolName } = payload;
      return http.post(`${prefix}/defectFileContentSegment/toolName/${toolName}`, payload)
        .then(response => response.data || {});
    },
  },
};
