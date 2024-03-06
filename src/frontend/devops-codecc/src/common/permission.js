import { handleNoPermission } from 'bk-permission';
import * as BKUI from 'bk-magic-vue';
import Vue from 'vue';

// 处理流水线无权限的情况
export const handleTaskNoPermission = (query, data) => handleNoPermission(
  BKUI,
  {
    resourceType: 'codecc_task',
    ...query,
  },
  new Vue().$createElement,
  data,
  window.DEVOPS_SITE_URL,
);
