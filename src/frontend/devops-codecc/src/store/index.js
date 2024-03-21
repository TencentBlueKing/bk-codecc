/**
 * @file main store
 * @author blueking
 */

import Vue from 'vue';
import Vuex from 'vuex';

import project from './modules/project';
import task from './modules/task';
import defect from './modules/defect';
import tool from './modules/tool';
import checker from './modules/checker';
import checkerset from './modules/checkerset';
import log from './modules/log';
import op from './modules/op';
import ignore from './modules/ignore';
import paas from './modules/paas';
import http from '@/api';
import { unifyObjectStyle } from '@/common/util';
import preci from './modules/preci';
import axios from 'axios';

if (process.env.NODE_ENV === 'development') {
  Vue.config.devtools = true;
  Vue.config.warnHandler = (msg, vm, trace) => {
    // 将警告信息输出到控制台
    // console.warn(msg, vm, trace);
    // 忽略vue prop 检查的警告信息
    if (msg.includes('Invalid prop')) {
      return false;
    }
    // 继续显示其他警告信息
    return true;
  };
}

Vue.use(Vuex);

const loadedPlugin = (store) => {
  store.subscribe((mutation, state) => {
    const { type } = mutation;
    if (type !== 'setMainContentLoading' && type !== 'updateLoadState') {
      store.commit('updateLoadState', type);
    }
  });
};

const store = new Vuex.Store({
  // 模块
  modules: {
    project,
    defect,
    tool,
    task,
    checker,
    checkerset,
    log,
    op,
    ignore,
    paas,
    preci,
  },
  plugins: [loadedPlugin],
  // 公共 store
  state: {
    mainContentLoading: false,
    // 系统当前登录用户
    user: {},
    toolMeta: {
      LANG: [],
      PARAM_TYPE: [],
      TOOL_PATTERN: [],
      TOOL_TYPE: [],
    },
    loaded: {},
    taskId: undefined,
    constants: {
      TOOL_PATTERN: {
        LINT: 'LINT',
        CCN: 'CCN',
        DUPC: 'DUPC',
        COVERITY: 'COVERITY',
        KLOCWORK: 'KLOCWORK',
        PINPOINT: 'PINPOINT',
      },
    },
    projectId: undefined,
    isRbac: undefined,
  },
  // 公共 getters
  getters: {
    mainContentLoading: state => state.mainContentLoading,
    user: state => state.user,
  },
  // 公共 mutations
  mutations: {
    updateLoadState(state, type) {
      state.loaded[type] = true;
    },
    updateTaskId(state, taskId) {
      state.taskId = Number(taskId);
    },
    updateProjectId(state, projectId) {
      state.projectId = projectId;
    },
    /**
     * 设置内容区的 loading 是否显示
     *
     * @param {Object} state store state
     * @param {boolean} loading 是否显示 loading
     */
    setMainContentLoading(state, loading) {
      state.mainContentLoading = loading;
    },

    /**
     * 更新当前用户 user
     *
     * @param {Object} state store state
     * @param {Object} user user 对象
     */
    updateUser(state, user) {
      state.user = Object.assign({}, user);
    },

    /**
     * 更新当前用户 user
     *
     * @param {Object} state store state
     * @param {Object} user user 对象
     */
    updateToolMeta(state, toolMeta) {
      state.toolMeta = Object.assign({}, toolMeta);
    },

    updateRbacPermission(state, rbacPermission) {
      state.isRbac = rbacPermission;
    },
  },
  actions: {
    /**
     * 获取用户信息
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     *
     * @return {Promise} promise 对象
     */
    userInfo({ commit, state, dispatch }, config) {
      // return http.get(`/app/index?invoke=userInfo`, config).then(response => {
      return http.get('/task/api/user/userInfo', config).then((response) => {
        const userData = response.data || {};
        commit('updateUser', userData);
        return userData;
      });
    },

    getToolMeta({ commit, state }) {
      if (!state.projectId) {
        return;
      }
      if (state.loaded.updateToolMeta === true) {
        return state.toolMeta;
      }

      const params = { metadataType: 'LANG;TOOL_TYPE;TOOL_PATTERN;PARAM_TYPE' };
      // return http.post('/app/index?invoke=metadata', params).then(res => {
      return http
        .get('/task/api/user/metadatas', { params })
        .then((res) => {
          const toolMeta = res.data || {};
          commit('updateToolMeta', toolMeta);
          return toolMeta;
        })
        .catch(e => e);
    },

    getRbacPermission({ commit, state }) {
      if (!state.projectId) {
        return;
      }
      return http
        .get(`/task/api/user/task/${state.projectId}/isRbacPermission`)
        .then((res) => {
          this.commit('updateRbacPermission', res.data);
          return res;
        });
    },

    getDeptTree({ commit }, payload) {
      return http.get('task/api/user/organization/tree')
        .then(response => response.data || {});
    },
  },
});

/**
 * hack vuex dispatch, add third parameter `config` to the dispatch method
 *
 * @param {Object|string} _type vuex type
 * @param {Object} _payload vuex payload
 * @param {Object} config config 参数，主要指 http 的参数，详见 src/api/index initConfig
 *
 * @return {Promise} 执行请求的 promise
 */
store.dispatch = function (_type, _payload, config = {}) {
  const { type, payload } = unifyObjectStyle(_type, _payload);

  const action = { type, payload, config };
  const entry = store._actions[type]; // eslint-disable-line
  if (!entry) {
    if (process.env.NODE_ENV !== 'production') {
      console.error(`[vuex] unknown action type: ${type}`);
    }
    return;
  }

  store._actionSubscribers.forEach((sub) => sub(action, store.state)); // eslint-disable-line

  return entry.length > 1
    ? Promise.all(entry.map(handler => handler(payload, config)))
    : entry[0](payload, config);
};

export default store;
