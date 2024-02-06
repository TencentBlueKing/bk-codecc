import http from '@/api';
import axios from 'axios';

export default {
  namespaced: true,
  state: {
    closeMessage: false, // 是否关闭安装preci提示
    installDialog: false,
    initMessage: false,
  },
  mutations: {
  },
  actions: {
    getPreCiLastVersion({ commit }) {
      return axios.get(
        `${window.DEVOPS_SITE_URL}/prebuild/api/user/prebuild/pluginVersion?pluginType=JETBRAINS`,
        { withCredentials: true },
      )
        .then(res => res);
      // 测试环境这个接口没有数据，调试的时候需要打开下面注释
      // return http.get(`https://蓝盾域名/prebuild/api/user/prebuild/pluginVersion?pluginType=JETBRAINS`)
      //     .then(res => res);
    },
    getUserUseVersion({ commit }, data) {
      return http.post('/task/api/user/preCI/use/version', data).then(res => res.data || {});
    },
  },
};
