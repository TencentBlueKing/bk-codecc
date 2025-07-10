/**
 * @file 通用方法
 * @author blueking
 */
import { isArray } from 'lodash';
import i18n from '@/i18n';

/**
 * 函数柯里化
 *
 * @example
 *     function add (a, b) {return a + b}
 *     curry(add)(1)(2)
 *
 * @param {Function} fn 要柯里化的函数
 *
 * @return {Function} 柯里化后的函数
 */
export function curry(fn) {
  const judge = (...args) => (args.length === fn.length ? fn(...args) : arg => judge(...args, arg));
  return judge;
}

/**
 * 判断是否是对象
 *
 * @param {Object} obj 待判断的
 *
 * @return {boolean} 判断结果
 */
export function isObject(obj) {
  return obj !== null && typeof obj === 'object';
}

/**
 * 规范化参数
 *
 * @param {Object|string} type vuex type
 * @param {Object} payload vuex payload
 * @param {Object} options vuex options
 *
 * @return {Object} 规范化后的参数
 */
export function unifyObjectStyle(type, payload, options) {
  if (isObject(type) && type.type) {
    options = payload; // eslint-disable-line
    payload = type; // eslint-disable-line
    type = type.type; // eslint-disable-line
  }

  if (process.env.NODE_ENV !== 'production') {
    if (typeof type !== 'string') {
      console.warn(`expects string as the type, but found ${typeof type}.`);
    }
  }

  return { type, payload, options };
}

/**
 * 以 baseColor 为基础生成随机颜色
 *
 * @param {string} baseColor 基础颜色
 * @param {number} count 随机颜色个数
 *
 * @return {Array} 颜色数组
 */
export function randomColor(baseColor, count) {
  const segments = baseColor.match(/[\da-z]{2}/g);
  // 转换成 rgb 数字
  for (let i = 0; i < segments.length; i++) {
    segments[i] = parseInt(segments[i], 16);
  }
  const ret = [];
  // 生成 count 组颜色，色差 20 * Math.random
  for (let i = 0; i < count; i++) {
    ret[i] = `#${Math.floor((segments[0] + (Math.random() < 0.5 ? -1 : 1)) * Math.random() * 20).toString(16)}${Math.floor((segments[1] + (Math.random() < 0.5 ? -1 : 1)) * Math.random() * 20).toString(16)}${Math.floor((segments[2] + (Math.random() < 0.5 ? -1 : 1)) * Math.random() * 20).toString(16)}`;
  }
  return ret;
}

/**
 * min max 之间的随机整数
 *
 * @param {number} min 最小值
 * @param {number} max 最大值
 *
 * @return {number} 随机数
 */
export function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1) + min);
}

/**
 * 异常处理
 *
 * @param {Object} err 错误对象
 * @param {Object} ctx 上下文对象，这里主要指当前的 Vue 组件
 */
export function catchErrorHandler(err, ctx) {
  const { data } = err;
  if (data) {
    if (!data.code || data.code === 404) {
      ctx.exceptionCode = {
        code: '404',
        msg: '当前访问的页面不存在',
      };
    } else if (data.code === 403) {
      ctx.exceptionCode = {
        code: '403',
        msg: 'Sorry，您的权限不足!',
      };
    } else {
      console.error(err);
      ctx.bkMessageInstance = ctx.$bkMessage({
        theme: 'error',
        message: err.message || err.data.msg || err.statusText,
      });
    }
  } else {
    console.error(err);
    ctx.bkMessageInstance = ctx.$bkMessage({
      theme: 'error',
      message: err.message || err.data.msg || err.statusText,
    });
  }
}

/**
 * 获取字符串长度，中文算两个，英文算一个
 *
 * @param {string} str 字符串
 *
 * @return {number} 结果
 */
export function getStringLen(str) {
  let len = 0;
  for (let i = 0; i < str.length; i++) {
    if (str.charCodeAt(i) > 127 || str.charCodeAt(i) === 94) {
      len += 2;
    } else {
      len += 1;
    }
  }
  return len;
}

/**
 * 转义特殊字符
 *
 * @param {string} str 待转义字符串
 *
 * @return {string} 结果
 */
export const escape = str => String(str).replace(/([.*+?^=!:${}()|[\]/\\])/g, '\\$1');

/**
 * 对象转为 url query 字符串
 *
 * @param {*} param 要转的参数
 * @param {string} key key
 *
 * @return {string} url query 字符串
 */
export function json2Query(param, key) {
  if (!param) return '';
  const mappingOperator = '=';
  const separator = '&';
  let paramStr = '';

  if (
    param instanceof String
    || typeof param === 'string'
    || param instanceof Number
    || typeof param === 'number'
    || param instanceof Boolean
    || typeof param === 'boolean'
  ) {
    paramStr += separator + key + mappingOperator + encodeURIComponent(param);
  } else {
    Object.keys(param).forEach((p) => {
      const value = param[p];
      const k = key === null || key === '' || key === undefined
        ? p
        : key + (param instanceof Array ? `[${p}]` : `.${p}`);
      paramStr += separator + json2Query(value, k);
    });
  }
  return paramStr.substr(1);
}

/**
 * 字符串转换为驼峰写法
 *
 * @param {string} str 待转换字符串
 *
 * @return {string} 转换后字符串
 */
export function camelize(str) {
  return str.replace(/-(\w)/g, (strMatch, p1) => p1.toUpperCase());
}

/**
 * 获取元素的样式
 *
 * @param {Object} elem dom 元素
 * @param {string} prop 样式属性
 *
 * @return {string} 样式值
 */
export function getStyle(elem, prop) {
  if (!elem || !prop) {
    return false;
  }

  // 先获取是否有内联样式
  let value = elem.style[camelize(prop)];

  if (!value) {
    // 获取的所有计算样式
    let css = '';
    if (document.defaultView && document.defaultView.getComputedStyle) {
      css = document.defaultView.getComputedStyle(elem, null);
      value = css ? css.getPropertyValue(prop) : null;
    }
  }

  return String(value);
}

/**
 *  获取元素相对于页面的高度
 *
 *  @param {Object} node 指定的 DOM 元素
 */
export function getActualTop(node) {
  let actualTop = node.offsetTop;
  let current = node.offsetParent;

  while (current !== null) {
    actualTop += current.offsetTop;
    current = current.offsetParent;
  }

  return actualTop;
}

/**
 *  获取元素相对于页面左侧的宽度
 *
 *  @param {Object} node 指定的 DOM 元素
 */
export function getActualLeft(node) {
  let actualLeft = node.offsetLeft;
  let current = node.offsetParent;

  while (current !== null) {
    actualLeft += current.offsetLeft;
    current = current.offsetParent;
  }

  return actualLeft;
}

/**
 * document 总高度
 *
 * @return {number} 总高度
 */
export function getScrollHeight() {
  let scrollHeight = 0;
  let bodyScrollHeight = 0;
  let documentScrollHeight = 0;

  if (document.body) {
    bodyScrollHeight = document.body.scrollHeight;
  }

  if (document.documentElement) {
    documentScrollHeight = document.documentElement.scrollHeight;
  }

  scrollHeight = bodyScrollHeight - documentScrollHeight > 0
    ? bodyScrollHeight
    : documentScrollHeight;

  return scrollHeight;
}

/**
 * 滚动条在 y 轴上的滚动距离
 *
 * @return {number} y 轴上的滚动距离
 */
export function getScrollTop() {
  let scrollTop = 0;
  let bodyScrollTop = 0;
  let documentScrollTop = 0;

  if (document.body) {
    bodyScrollTop = document.body.scrollTop;
  }

  if (document.documentElement) {
    documentScrollTop = document.documentElement.scrollTop;
  }

  scrollTop = bodyScrollTop - documentScrollTop > 0 ? bodyScrollTop : documentScrollTop;

  return scrollTop;
}

/**
 * 浏览器视口的高度
 *
 * @return {number} 浏览器视口的高度
 */
export function getWindowHeight() {
  const windowHeight = document.compatMode === 'CSS1Compat'
    ? document.documentElement.clientHeight
    : document.body.clientHeight;

  return windowHeight;
}

// 节流函数 : 减少浏览器内存消耗
function throttle(ele, callback) {
  let isRunning = false;
  return function () {
    if (isRunning) return;
    isRunning = true;
    // requestAnimationFrame:回调间隔 = 浏览器重绘频率
    window.requestAnimationFrame((timestamp) => {
      if (ele.scrollTop + ele.clientHeight >= ele.scrollHeight) {
        // 检测是否滚动到元素底部
        callback();
      }
      isRunning = false;
    });
  };
}

/**
 * 监听HTML元素是否滚动到底部 : 兼容ES5
 * @param {object} ele HTML元素
 * @param {function} callback 滚动到底部后的回调函数
 */
export function listenScrollToBottom(ele, callback) {
  if (ele === null || ele === undefined) {
    // 节点不存在：抛出错误
    throw new Error('Undefined DOM');
  }
  ele.addEventListener('scroll', throttle(ele, callback), false); // 监听 scroll 事件
}

export function getClosest(elem, selectors) {
  if (!Element.prototype.matches) {
    Element.prototype.matches = Element.prototype.msMatchesSelector
      || Element.prototype.webkitMatchesSelector;
  }

  if (!Element.prototype.closest) {
    Element.prototype.closest = function (s) {
      let el = this; // eslint-disable-line
      if (!document.documentElement.contains(el)) return null;
      do {
        if (el.matches(s)) return el;
        el = el.parentElement;
      } while (el !== null);
      return null;
    };
  }

  return elem.closest(selectors);
}

export function hasClass(elem, cls) {
  return elem.getAttribute('class').indexOf(cls) > -1;
}

export function addClass(elem, cls) {
  if (elem.classList) {
    elem.classList.add(cls);
  } else if (!hasClass(elem, cls)) {
    elem.setAttribute('class', `${elem.getAttribute('class')} ${cls}`);
  }
}

export function removeClass(elem, cls) {
  if (elem.classList) {
    elem.classList.remove(cls);
  } else if (hasClass(elem, cls)) {
    elem.setAttribute('class', elem.getAttribute('class').replace(cls, ' '));
  }
}

export function toggleClass(elem, cls) {
  if (elem.classList && elem.classList.toggle) {
    elem.classList.toggle(cls);
  } else if (hasClass(elem, cls)) {
    removeClass(elem, cls);
  } else {
    addClass(elem, cls);
  }
}

// 工具状态值对应
// TODO 国际化
export function getToolStatus(num, tool) {
  const arr1 = [
    i18n.t('等待分析'),
    i18n.t('构建'),
    i18n.t('进入等候队列'),
    i18n.t('分析中，analyze'),
    i18n.t('分析中，commit'),
    i18n.t('生成问题'),
    i18n.t('成功'),
  ];
  const arr2 = [
    '',
    i18n.t('启动扫描'),
    i18n.t('拉取代码'),
    i18n.t('扫描分析'),
    i18n.t('生成问题'),
    i18n.t('成功'),
  ];
  const toolStatus = tool === 'COVERITY' || tool === 'KLOCWORK' ? arr1 : arr2;
  return toolStatus[num];
}

// 分析日志状态值对应
export function getLogFlag(num) {
  const logFlag = [
    '',
    i18n.t('small.成功'),
    i18n.t('small.失败'),
    i18n.t('small.进行中'),
    i18n.t('small.中断'),
  ];
  return logFlag[num];
}

// 将秒换成时分秒
export function formatSeconds(s) {
  if (!s || s < 0) {
    return '--';
  }
  let t = '';
  const sencond = Math.floor(s / 1000);
  const hour = Math.floor(sencond / 3600);
  const min = Math.floor(sencond / 60) % 60;
  const sec = Math.floor(sencond) % 60;
  t = hour < 10 ? `0${hour}:` : `${hour}:`;
  t += min < 10 ? `0${min}:` : `${min}:`;
  t += sec < 10 ? `0${sec}` : `${sec}`;
  return t;
}

// 计算时长换成 1min 1h 1天等
export function formatDiff(time) {
  let duration;
  const leave = Date.parse(new Date()) - time * 1000;
  const diff = Math.floor(leave / (60 * 60 * 1000));
  if (diff < 1) {
    const used = Math.floor(leave / (60 * 1000));
    duration = `${used <= 0 ? 1 : used}min${i18n.t('前')}`;
  } else if (diff < 24) {
    const used = Math.floor(leave / (60 * 60 * 1000));
    duration = `${used === 0 ? 1 : used}h${i18n.t('前')}`;
  } else if (diff > 24) {
    const used = Math.floor(leave / (24 * 60 * 60 * 1000));
    duration = `${used === 0 ? 1 : used}d${i18n.t('前')}`;
  }
  return time ? duration : '--';
}

export function urlJoin(...args) {
  return args
    .filter(arg => arg)
    .join('/')
    .replace(/([^:]\/)\/+/g, '$1');
}

export function numToThousand(num) {
  if (!num || num <= 0) {
    return 0;
  }
  if (num < 1000) {
    return `${(num / 1000).toFixed(1)}K`;
  }
  return `${(num / 1000).toFixed(0)}K`;
}

export function deepClone(obj) {
  const curObj = {};
  // eslint-disable-next-line no-restricted-syntax
  for (const key in obj) {
    if (obj[key].toString().toLowerCase() === '[object object]') {
      curObj[key] = deepClone(obj[key]);
    } else {
      curObj[key] = key === 'text' ? '' : obj[key];
    }
  }

  return curObj;
}

/**
 * 数组转字符串
 * @param {*} arr
 * @param {*} separator
 * @returns str
 */
export function array2Str(arr, separator = ';') {
  if (!arr) return '--';
  if (typeof arr === 'string') return arr;
  if (isArray(arr)) return arr.join(separator);
}

export function isJSON(str = '') {
  try {
    JSON.parse(str);
    return true;
  } catch (e) {
    return false;
  }
};
