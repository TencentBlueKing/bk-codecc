/**
 * @file main entry
 * @author blueking
 */

import Vue from 'vue';

import router from './router';
import store from './store';
import { injectCSRFTokenToHeaders } from './api';
import auth from './common/auth';
import Img403 from './images/403.png';
import Exception from './components/exception';
import { bus } from './common/bus';
import AuthComponent from './components/auth';
import LayoutInner from './components/layout/layout-inner';
import LayoutOuter from './components/layout/layout-outer';
import LayoutFull from './components/layout/layout-full';
import i18n from './i18n';
import './common/bkmagic';
import App from './App';
import { format } from 'date-fns';
import VueCompositionAPI from '@vue/composition-api';
import { PermissionDirective, BkPermission } from 'bk-permission';
import { handleTaskNoPermission } from './common/permission';

Vue.component('LayoutOuter', LayoutOuter);
Vue.component('LayoutInner', LayoutInner);
Vue.component('LayoutFull', LayoutFull);
Vue.component('AppException', Exception);
Vue.component('AppAuth', AuthComponent);
Vue.filter('formatDate', (value, arg1) => {
  if (!value) return '--';
  return arg1 === 'date'
    ? format(value, 'yyyy-MM-dd')
    : format(value, 'yyyy-MM-dd HH:mm:ss');
});

Vue.filter('formatBigNum', (value) => {
  if (!value) return 0;
  return value > 99999 ? '10万+' : value;
});

Vue.filter('formatUndefNum', (value, type, x) => {
  if (value === undefined) return '--';
  if (type === 'abs') return Math.abs(value);
  if (type === 'fixed') return Number(value).toFixed(x);
  return value;
});

Vue.filter('countLen', (value = []) => value.length);
Vue.use(VueCompositionAPI);
Vue.use(PermissionDirective(handleTaskNoPermission, window.DEVOPS_SITE_URL));
Vue.use(BkPermission, {
  i18n,
});

/**
 * @desc 页面数据的编辑状态
 */
global.changeAlert = false;

/**
 * @desc 浏览器框口关闭提醒
 */
global.addEventListener('beforeunload', (event) => {
  if (!global.changeAlert) {
    return null;
  }
  const e = event || global.event;
  if (e) {
    e.returnValue = i18n.t('离开将会导致未保存信息丢失');
  }
  return i18n.t('离开将会导致未保存信息丢失');
});

auth.requestCurrentUser().then(
  (user) => {
    injectCSRFTokenToHeaders();
    if (!user.isAuthenticated) {
      auth.redirectToLogin();
    } else {
      global.bus = bus;
      global.mainComponent = new Vue({
        el: '#app',
        i18n,
        router,
        store,
        components: { App },
        template: '<App/>',
      });
    }
  },
  (err) => {
    let message;
    if (err.status === 403) {
      message = i18n.t('Sorry，您的权限不足！');
      if (err.data && err.data.msg) {
        message = err.data.msg;
      }
    } else {
      message = i18n.t('无法连接到后端服务，请稍候再试。');
    }

    const divStyle = ''
      + 'text-align: center;'
      + 'width: 400px;'
      + 'margin: auto;'
      + 'position: absolute;'
      + 'top: 50%;'
      + 'left: 50%;'
      + 'transform: translate(-50%, -50%);';

    const h2Style = 'font-size: 20px;color: #979797; margin: 32px 0;font-weight: normal';

    const content = ''
      + `<div class="bk-exception bk-exception-center" style="${divStyle}">`
      + `<img src="${Img403}"><h2 class="exception-text" style="${h2Style}">${message}</h2>`
      + '</div>';

    document.write(content);
  },
);
