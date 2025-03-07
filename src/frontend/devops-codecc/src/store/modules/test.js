import http from '@/api';

const prefix = '/task/api/user/test/task';

export default {
  namespaced: true,
  state: {
  },
  getters: {},
  mutations: {
    updateTestReportCount(state, count) {
      state.testReportCount = count;
    },
  },
  actions: {
    getProjectList({ commit }) {
      return http.get(`${prefix}/project/list`)
        .then(response => response.data || {});
    },
    getToolList({ commit }, toolName) {
      return http.get(`${prefix}/info/toolName/${toolName}`)
        .then(response => response.data || {});
    },
    getTaskList({ commit }, { toolName, projectId }) {
      return http.get(`${prefix}/list/${toolName}/${projectId}`)
        .then(response => response.data || {});
    },
    getResult({ commit }, { toolName, version, stage }) {
      return http.get(`${prefix}/getResult/${toolName}/${version}?stage=${stage}`)
        .then(response => response.data || {});
    },
    listVersion({ commit }, { toolName, stage }) {
      return http.get(`${prefix}/listVersion/${toolName}?stage=${stage}`)
        .then(response => response.data || {});
    },
    startTest({ commit }, { toolName, version, projectId, projectName }) {
      return http.post(`${prefix}/start/${toolName}`, { version, projectId, projectName })
        .then(response => response || {});
    },
    getThreshold({ commit }) {
      return http.get(`${prefix}/recommendedThreshold`)
        .then(response => response.data || {});
    },
    getRepoScaleList({ commit }) {
      return http.get(`${prefix}/repoScale/list`)
        .then(response => response.data || {});
    },
    startRandomTest({ commit }, { toolName, version, need, repoScaleId }) {
      return http.post(`${prefix}/start/randomTest`, { toolName, version, need, repoScaleId })
        .then(response => response || {});
    },
    getTestReportDetailList({ commit }, { toolName, stage, params }) {
      const { pageNum, pageSize, ...data } = params;
      const query = { pageNum, pageSize };
      console.log('params', params);
      return http.post(`${prefix}/testReport/${toolName}/${stage}`, data, { params: query })
        .then(response => response.data || []);
    },
    getTestReportDetailCount({ commit }, { toolName, stage, params }) {
      return http.post(`${prefix}/testReport/count/${toolName}/${stage}`, params)
        .then((response) => {
          const total = response.data || 0;
          commit('updateTestReportCount', total);
          return total;
        });
    },
    deleteTestTask({ commit }, { projectId, taskId }) {
      return http.put(`${prefix}/delete/testTask/${projectId}/${taskId}`);
    },
    hasTestTask({ commit }, { projectId }) {
      return http.get(`${prefix}/has/testTask/${projectId}`);
    },
    getToolStatOverview({ commit }, { toolName, summaryDays }) {
      return http.get(`/defect/api/user/statistic/tool/${toolName}/statOverview?recentlyDays=${summaryDays}`)
        .then(response => response.data || {});
    },
    getToolStatDailyTrend({ commit }, { toolName, trendDays }) {
      return http.get(`/defect/api/user/statistic/tool/${toolName}/statDailyTrend?recentlyDays=${trendDays}`)
        .then(response => response.data || []);
    },
    getToolLatestVersion({ commit }, { toolName }) {
      return http.get(`${prefix}/getLatestVersion/${toolName}`)
        .then(response => response.data || '--');
    },
  },
};
