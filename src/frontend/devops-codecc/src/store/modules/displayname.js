import http from '@/api';
import axios from 'axios';

export default {
  namespaced: true,
  state: {
    usersMap: new Map(),
    baseApiUrl: window.BK_API_TENANT_BASE_URL,
    tenantId: '',
    /** 批量查询限制的bk_usernames数量 */
    batchSize: 100,
  },
  mutations: {
    updateTenantId(state, tenantId) {
      state.tenantId = tenantId;
    },
    updateUsersMap(state, users) {
      const userList = Array.isArray(users) ? users : [users];
      for (const item of userList) {
        const { bk_username } = item;
        if (!state.usersMap.has(bk_username)) {
          state.usersMap.set(bk_username, item);
        }
      }
    },
  },
  actions: {
    getTenantId({ commit }) {
      return http
        .get('/task/api/user/base/tenantId')
        .then((res) => {
          const result = res.data || {};
          commit('updateTenantId', result);
          return result;
        })
        .catch((e) => {
          console.error(e);
        });
    },
    /**
     * 同步过滤出未缓存的用户ID
     * @param {string|Array} userIds - 单个ID或ID数组
     * @returns {Array} 未被缓存的ID数组
     */
    filterMissingUsers({ state }, userIds) {
      if (userIds === '' || userIds === null) return [];
      const userIdList = Array.isArray(userIds) ? userIds : [userIds];
      const newUsers = userIdList.filter(id => !state.usersMap.has(id));
      return newUsers;
    },
    /**
     * @description 批量查询
     * 由于用户展示名查询bk_usernames最大限制为100（batchSize），因此需要考虑超出100时的情况
     */
    async batchGetDisplayName({ state, commit, dispatch }, userIds) {
      const missingUsers = await dispatch('filterMissingUsers', userIds);
      if (missingUsers.length === 0) return state.usersMap;

      const batches = [];
      // 将用户列表分成多个批次
      for (let i = 0; i < missingUsers.length; i += state.batchSize) {
        batches.push(missingUsers.slice(i, i + state.batchSize));
      }

      // 并行发起所有批次请求
      const results = await Promise.all(batches.map(async (batch) => {
        const userNames = batch.join(',');
        const config = {
          headers: {
            'X-Bk-Tenant-Id': state.tenantId,
          },
          withCredentials: true,
          params: { bk_usernames: userNames },
        };
        const res = await axios.get(
          `${state.baseApiUrl}/api/v3/open-web/tenant/users/-/display_info/`,
          config,
        );
        return res.data?.data || [];
      }));

      const combinedData = results.flat();
      commit('updateUsersMap', combinedData);
      dispatch('setEmptyDisplayName', userIds);
      return state.usersMap;
    },
    /** 单个查询 */
    async getDisplayName({ state, commit, dispatch }, userId) {
      const [userName] = await dispatch('filterMissingUsers', userId);
      if (userName === '' || userName === null) return state.usersMap;

      const config = {
        headers: {
          'X-Bk-Tenant-Id': state.tenantId,
        },
        withCredentials: true,
      };
      const res = await axios.get(`${state.baseApiUrl}/api/v3/open-web/tenant/users/${userName}/display_info/`, config);
      const { data } = res.data || {};
      commit('updateUsersMap', {
        bk_username: userName,
        ...data,
      });
      dispatch('setEmptyDisplayName', userId);
      return state.usersMap;
    },
    /**
     * @description 由于查询仅缓存有结果的数据，而没有结果的数据却得不到记录
     * 这里其实会造成资源的浪费，因此在每次查询完后，需要找出没有结果的数据，直接记录他们的bk_username
     */
    setEmptyDisplayName({ state, commit }, users) {
      const userList = Array.isArray(users) ? users : [users];
      for (const item of userList) {
        if (!state.usersMap.get(item)) {
          state.usersMap.set(item, {
            bk_username: item,
            display_name: item,
          });
        }
      }
    },
  },
};
