/**
 * @file axios 封装
 * @author blueking
 */

import Vue from 'vue';
import axios from 'axios';
import cookie from 'cookie';

import CachedPromise from './cached-promise';
import RequestQueue from './request-queue';
import { bus } from '../common/bus';
import { messageError } from '@/common/bkmagic';
import store from '@/store';
import I18n from '@/i18n';
// import router from '@/router'
import { showLoginModal } from '@blueking/login-modal';

// axios 实例
const axiosInstance = axios.create({
  baseURL: window.AJAX_URL_PREFIX,
  xsrfCookieName: 'paas_perm_csrftoken',
  xsrfHeaderName: 'X-CSRFToken',
  withCredentials: true,
});

/**
 * request interceptor
 */
axiosInstance.interceptors.request.use(
  (config) => {
    // 绝对路径不走 mock
    // if (!/^(https|http)?:\/\//.test(config.url)) {
    //   const prefix = config.url.indexOf('?') === -1 ? '?' : '&'
    //   config.url += `${prefix}isAjax=1`
    // }

    // 从 store 中获取 taskId 只适用于 router.afterEach 钩子之后的请求，理论上满足业务场景
    config.headers = {
      ...config.headers,
      // 'X-DEVOPS-PROJECT-ID': cookie.parse(document.cookie)['X-DEVOPS-PROJECT-ID'],
      'X-DEVOPS-PROJECT-ID': store.state.projectId || '',
      'X-DEVOPS-TASK-ID': store.state.taskId || '',
    };

    return config;
  },
  error => Promise.reject(error),
);

/**
 * response interceptor
 */
// axiosInstance.interceptors.response.use(
//     response => response.data,
//     error => Promise.reject(error)
// )
axiosInstance.interceptors.response.use(
  response => response.data,
  error => Promise.reject(error),
);
// if (error && error.response) {
//     switch (error.response.status) {
//         case 403:
//             router.push({
//                 name: '403'
//             })
//             break
//         // case 500:
//         //     router.push({
//         //         name: '500'
//         //     })
//         //     break
//     }
// }

const http = {
  queue: new RequestQueue(),
  cache: new CachedPromise(),
  cancelRequest: requestId => http.queue.cancel(requestId),
  cancelCache: requestId => http.cache.delete(requestId),
  cancel: requestId => Promise.all([http.cancelRequest(requestId), http.cancelCache(requestId)]),
};

const methodsWithoutData = ['delete', 'get', 'head', 'options'];
const methodsWithData = ['post', 'put', 'patch'];
const allMethods = [...methodsWithoutData, ...methodsWithData];

// 在自定义对象 http 上添加各请求方法
allMethods.forEach((method) => {
  Object.defineProperty(http, method, {
    get() {
      return getRequest(method);
    },
    // get: () => getRequest(method)
  });
});

/**
 * 获取 http 不同请求方式对应的函数
 *
 * @param {string} http method 与 axios 实例中的 method 保持一致
 *
 * @return {Function} 实际调用的请求函数
 */
function getRequest(method) {
  if (methodsWithData.includes(method)) {
    return (url, data, config) => getPromise(method, url, data, config);
  }
  return (url, config) => getPromise(method, url, null, config);
}

/**
 * 实际发起 http 请求的函数，根据配置调用缓存的 promise 或者发起新的请求
 *
 * @param {method} http method 与 axios 实例中的 method 保持一致
 * @param {string} 请求地址
 * @param {Object} 需要传递的数据, 仅 post/put/patch 三种请求方式可用
 * @param {Object} 用户配置，包含 axios 的配置与本系统自定义配置
 *
 * @return {Promise} 本次http请求的Promise
 */
async function getPromise(method, url, data, userConfig = {}) {
  const config = initConfig(method, url, userConfig);
  let promise;
  if (config.cancelPrevious) {
    await http.cancel(config.requestId);
  }

  if (config.clearCache) {
    http.cache.delete(config.requestId);
  } else {
    promise = http.cache.get(config.requestId);
  }

  if (config.fromCache && promise) {
    return promise;
  }

  promise = new Promise(async (resolve, reject) => {
    const axiosRequest = methodsWithData.includes(method)
      ? axiosInstance[method](url, data, config)
      : axiosInstance[method](url, config);

    try {
      const response = await axiosRequest;
      Object.assign(config, response.config || {});
      handleResponse({ config, response, resolve, reject });
    } catch (error) {
      Object.assign(config, error.config);
      reject(error);
    }
  })
    .catch(error => handleReject(error, config))
    .finally(() => {
      // console.log('finally', config)
    });

  // 添加请求队列
  http.queue.set(config);
  // 添加请求缓存
  http.cache.set(config.requestId, promise);

  return promise;
}

/**
 * 处理 http 请求成功结果
 *
 * @param {Object} 请求配置
 * @param {Object} cgi 原始返回数据
 * @param {Function} promise 完成函数
 * @param {Function} promise 拒绝函数
 */
function handleResponse({ config, response, resolve, reject }) {
  // 调用蓝盾接口的时候，没有code和message字段
  const code = response.code ? response.code : response.status;
  const message = response.message ? response.message : '';
  if (String(code) !== '0' && config.globalError) {
    reject({ message, code });
  } else {
    resolve(config.originalResponse ? response : response.data, config);
  }
  http.queue.delete(config.requestId);
}

/**
 * 处理 http 请求失败结果
 *
 * @param {Object} Error 对象
 * @param {config} 请求配置
 *
 * @return {Promise} promise 对象
 */
function handleReject(error, config) {
  if (axios.isCancel(error)) {
    return Promise.reject(error);
  }
  // op接口直接reject, 不出现弹框
  if (config.url && config.url.includes(window.OP_AJAX_URL_PREFIX)) {
    return Promise.reject(error);
  }
  http.queue.delete(config.requestId);

  if (config.globalError && error.response) {
    const { status, data } = error.response;
    const nextError = { message: error.message, response: error.response };
    if (status === 401) {
      if (config.url.includes('/user/userInfo')) {
        window.location.href = `${
          window.PAAS_SERVICE_URL
        }/?c_url=${encodeURIComponent(window.location.href)}`;
      } else {
        // 登录成功之后的回调地址，用于执行关闭登录窗口或刷新父窗口页面等动作
        const successUrl = `${window.location.origin}/static/login_success.html`;

        // 系统的登录页地址
        const siteLoginUrl = window.PAAS_SERVICE_URL || '';
        if (!siteLoginUrl) {
          console.error('Login URL not configured!');
          return;
        }

        // 处理登录地址为登录小窗需要的格式，主要是设置c_url参数
        const [loginBaseUrl] = siteLoginUrl.split('?');
        const newUrl = loginBaseUrl.includes('login') ? `${loginBaseUrl}/` : `${loginBaseUrl}login/`;
        const loginUrl = `${newUrl}plain?size=small&c_url=${encodeURIComponent(successUrl)}`;
        showLoginModal({
          loginUrl,
        });

        return;
      }
      // bus.$emit('show-login-modal')
    } else if (status === 403) {
      bus.$emit('show-permission-dialog');
    } else if (status === 500) {
      nextError.message = I18n.t('系统出现异常');
      messageError(nextError.message);
    } else if (data && data.message) {
      nextError.message = data.message;
      messageError(nextError.message);
    }
    console.error(nextError.message);
    return Promise.reject(nextError);
  }
  // OAuth认证
  if (error.code === '2300005') {
    bus.$emit('show-oauth-dialog');
  }
  messageError(error.message);
  console.error(error.message);
  return Promise.reject(error);
}

/**
 * 初始化本系统 http 请求的各项配置
 *
 * @param {string} http method 与 axios 实例中的 method 保持一致
 * @param {string} 请求地址, 结合 method 生成 requestId
 * @param {Object} 用户配置，包含 axios 的配置与本系统自定义配置
 *
 * @return {Promise} 本次 http 请求的 Promise
 */
function initConfig(method, url, userConfig) {
  const defaultConfig = {
    ...getCancelToken(),
    // http 请求默认 id
    requestId: `${method}_${url}`,
    // 是否全局捕获异常
    globalError: true,
    // 是否直接复用缓存的请求
    fromCache: false,
    // 是否在请求发起前清除缓存
    clearCache: false,
    // 响应结果是否返回原始数据
    originalResponse: true,
    // 当路由变更时取消请求
    cancelWhenRouteChange: true,
    // 取消上次请求
    cancelPrevious: true,
  };
  return Object.assign(defaultConfig, userConfig);
}

/**
 * 生成 http 请求的 cancelToken，用于取消尚未完成的请求
 *
 * @return {Object} {cancelToken: axios 实例使用的 cancelToken, cancelExcutor: 取消http请求的可执行函数}
 */
function getCancelToken() {
  let cancelExcutor;
  const cancelToken = new axios.CancelToken((excutor) => {
    cancelExcutor = excutor;
  });
  return {
    cancelToken,
    cancelExcutor,
  };
}

Vue.prototype.$http = http;

export default http;

// 跨域处理
export function injectCSRFTokenToHeaders() {
  const CSRFToken = cookie.parse(document.cookie).paas_perm_csrftoken;
  if (CSRFToken !== undefined) {
    axiosInstance.defaults.headers.common['X-CSRFToken'] = CSRFToken;
  } else {
    console.warn('Can not find paas_perm_csrftoken in document.cookie');
  }
}
