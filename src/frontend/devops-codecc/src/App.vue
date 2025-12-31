<template>
  <div id="app" v-bkloading="{ isLoading: appLoading, opacity: 0.1 }">
    <!-- 系统维护 -->
    <div class="banner" v-if="maintainMes.noticeSerial && !isMaintainClose">
      <!-- eslint-disable-next-line -->
      <span v-html="handleHttp(maintainMes.noticeDesc)"></span>
      <i class="bk-icon icon-close f22" @click="handleBanner"></i>
    </div>
    <component :is="layout">
      <div
        style="height: 100%"
        v-bkloading="{ isLoading: mainContentLoading, opacity: 0.3 }"
      >
        <router-view
          v-if="isRouterAlive"
          class="main-content"
          :key="$route.fullPath"
        />
      </div>
    </component>
    <app-auth ref="bkAuth"></app-auth>
    <bk-dialog
      v-model="oauthDialogVisible"
      theme="primary"
      :confirm-fn="goOauth"
    >
      <p class="f18">{{ $t('请先将工蜂OAuth授权给蓝盾') }}</p>
    </bk-dialog>
    <!-- 添加权限 -->
    <bk-dialog
      v-model="permissionDialogVisible"
      theme="primary"
    >
      <p class="f18">{{ $t('暂无权限，请联系任务管理员添加权限。') }}</p>
      <template slot="footer">
        <bk-button theme="primary" @click="confirmPermission">
          {{ $t('去申请') }}
        </bk-button>
        <bk-button @click="handleClickLink">
          {{ $t('取消') }}
        </bk-button>
      </template>
    </bk-dialog>
    <!-- 新版本更新 -->
    <bk-dialog
      v-model="newVersionDialogVisible"
      class="notice"
      width="760"
      :title="newVersionMes.noticeName"
    >
      <section class="notice-content new-version">
        <div
          class="notice-newversion"
          v-for="(list, index) in newVersionList"
          :key="index"
        >
          <p class="notice-header">{{ list[0] }}</p>
          <span class="f14">{{ list[1] }}</span>
        </div>
      </section>
      <div slot="footer" class="notice-footer">
        <bk-button theme="primary" @click="handleNotice('newVersionMes')">
          <a
            v-if="newVersionMes.buttonHref"
            target="_blank"
            :href="newVersionMes.buttonHref"
          >{{ newVersionMes.buttonName || $t('确定') }}</a
          >
          <span v-else>{{ newVersionMes.buttonName || $t('确定') }}</span>
        </bk-button>
      </div>
    </bk-dialog>
    <!-- 公告 -->
    <bk-dialog
      v-model="noticeDialogVisible"
      class="notice"
      :title="noticeMes.noticeName"
      width="760"
    >
      <section class="notice-content p20">
        <div
          class="notice-list"
          v-for="(list, index) in noticeList"
          :key="index"
        >
          <span>{{ list }}</span>
        </div>
      </section>
      <div slot="footer" class="notice-footer">
        <bk-button theme="primary" @click="handleNotice('noticeMes')">
          <a
            v-if="noticeMes.buttonHref"
            target="_blank"
            :href="noticeMes.buttonHref"
          >{{ noticeMes.buttonName || $t('确定') }}</a
          >
          <span v-else>{{ noticeMes.buttonName || $t('确定') }}</span>
        </bk-button>
      </div>
    </bk-dialog>
  </div>
</template>
<script>
import { mapGetters, mapState } from 'vuex';
import { bus } from './common/bus';
import { toggleLang } from './i18n';
import { getToolMeta, getToolList, getTaskList } from './common/preload';
import DEPLOY_ENV from '@/constants/env';
import BkUserDisplayName from '@blueking/bk-user-display-name';

export default {
  name: 'App',
  data() {
    return {
      appLoading: false,
      oauthDialogVisible: false,
      permissionDialogVisible: false,
      noticeMesVisible: true,
      newVersionMesVisible: true,
      isRouterAlive: true,
      isInnerSite: DEPLOY_ENV === 'tencent',
    };
  },
  provide() {
    return {
      reload: this.reload,
    };
  },
  computed: {
    ...mapGetters(['user']),
    ...mapGetters(['mainContentLoading']),
    ...mapState('task', {
      status: 'status',
    }),
    ...mapState('displayname', {
      tenantId: 'tenantId',
    }),
    ...mapGetters('op', {
      isMaintainClose: 'isMaintainClose',
    }),
    ...mapState('op', {
      maintainMes: 'maintain',
      noticeMes: 'notice',
      newVersionMes: 'newVersion',
    }),
    layout() {
      return `layout-${this.$route.meta.layout}`;
    },
    projectId() {
      return this.$route.params.projectId;
    },
    taskId() {
      return this.$route.params.taskId;
    },
    newVersionDialogVisible() {
      return (
        !this.noticeDialogVisible
        && this.newVersionMesVisible
        && this.newVersionMes.noticeSerial
        && !window.localStorage.getItem(`codecc-notice-${this.newVersionMes.noticeSerial}`)
      );
    },
    noticeDialogVisible() {
      return (
        this.noticeMesVisible
        && this.noticeMes.noticeSerial
        && !window.localStorage.getItem(`codecc-notice-${this.noticeMes.noticeSerial}`)
      );
    },
    newVersionList() {
      if (!this.newVersionMes.noticeDesc) return [];
      const contents = this.newVersionMes.noticeDesc.split(/\n{2,}/g, 4);
      const list = contents.map(content => content.split(/\n/g, 2));
      return list;
    },
    noticeList() {
      if (!this.noticeMes.noticeDesc) return [];
      return this.noticeMes.noticeDesc.split(/\n/g);
    },
  },
  watch: {
    async '$route.fullPath'(val) {
      // 同步地址到蓝盾
      if (window.self !== window.top) {
        console.log('$route.fullPath', val);
        // iframe嵌入
        devopsUtil.syncUrl(val.replace(/^\/codecc\//, '/')); // eslint-disable-line
      }
      // 进到具体项目页面，项目停用跳转到任务管理
      if (this.taskId) {
        const res = await this.$store.dispatch('task/status');
        if (res.status === 1) {
          this.$router.push({ name: 'task-settings-manage' });
        }
        if (!res.status.gongfengProjectId) {
          // 工蜂开源项目少调一个接口
          getTaskList();
        }
      }
      getToolMeta();
      getToolList();
    },
  },
  created() {
    this.initDisplayName();
    // 蓝盾切换项目
    window.addEventListener('change::$currentProjectId', (data) => {
      if (
        this.$route.params.projectId
        && this.$route.params.projectId !== data.detail.currentProjectId
      ) {
        this.goHome(data.detail.currentProjectId);
        document.cookie = `X-DEVOPS-PROJECT-ID=${data.detail.currentProjectId};domain=${location.host};path=/`;
      }
    });
    // 蓝盾回到首页
    window.addEventListener('order::backHome', (data) => {
      if (this.$route.name !== 'task-list') {
        this.goHome();
      }
    });
  },
  mounted() {
    const self = this;
    bus.$on('show-login-modal', () => {
      self.$refs.bkAuth.showLoginModal();
    });
    bus.$on('close-login-modal', () => {
      self.$refs.bkAuth.hideLoginModal();
      setTimeout(() => {
        window.location.reload();
      }, 0);
    });

    bus.$on('show-app-loading', () => {
      self.appLoading = true;
    });
    bus.$on('hide-app-loading', () => {
      self.appLoading = false;
    });

    bus.$on('show-content-loading', () => {
      this.$store.commit('setMainContentLoading', true, { root: true });
    });
    bus.$on('hide-content-loading', () => {
      this.$store.commit('setMainContentLoading', false, { root: true });
    });

    bus.$on('show-oauth-dialog', () => {
      this.oauthDialogVisible = true;
    });
    bus.$on('show-permission-dialog', () => {
      this.permissionDialogVisible = true;
    });
  },
  methods: {
    /**
     * router 跳转
     *
     * @param {string} idx 页面指示
     */
    goPage(idx) {
      this.$router.push({
        name: idx,
      });
    },
    handleToggleLang() {
      toggleLang();
    },
    goHome(projectId) {
      const params = projectId ? { projectId } : {};
      this.$router.replace({
        name: 'task-list',
        params,
      });
    },
    handleBanner() {
      const localKey = `codecc-maintain-${this.maintainMes.noticeSerial}`;
      window.localStorage.setItem(localKey, true);
      this.$store.commit('op/updateMaintainClose', true);
    },
    goOauth() {
      this.oauthDialogVisible = false;
      this.$store
        .dispatch('defect/oauthUrl', { toolName: this.$route.params.toolId || this.$route.params.toolName })
        .then((res) => {
          window.open(res, '_blank');
        });
    },
    handleNotice(key) {
      const localKey = `codecc-notice-${this[key].noticeSerial}`;
      window.localStorage.setItem(localKey, true);
      this[`${key}Visible`] = false;
    },
    handleHttp(str) {
      let newStr = str.replace(/>/g, '&gt;').replace(/</g, '&lt;');
      const reg = /\[([^\]]+)\]\(([http:\\|https:\\]{1}[^)]+)\)/g;
      newStr = newStr.replace(reg, '<a target="_blank" href=\'$2\'>$1</a>');
      return newStr;
    },
    confirmPermission() {
      this.permissionDialogVisible = false;
      if (this.taskId && this.isInnerSite) {
        this.$router.push({ name: 'task-settings-authority' });
      } else {
        window.open(`${window.DEVOPS_SITE_URL}/console/permission/apply?project_code=${this.projectId}&projectName=${
          this.projectId}&resourceType=project&resourceName=${this.projectId}&iamResourceCode=${
          this.projectId}&iamRelatedResourceType=project`, '_blank');
      }
    },
    handleClickLink() {
      this.permissionDialogVisible = false;
    },
    reload() {
      this.isRouterAlive = false;
      this.$nextTick(() => {
        this.isRouterAlive = true;
      });
    },
    async initDisplayName() {
      await this.$store.dispatch('displayname/getTenantId');
      BkUserDisplayName.configure({
        // 必填，租户 ID
        tenantId: this.tenantId,
        // 必填，网关地址
        apiBaseUrl: window.BK_API_TENANT_BASE_URL,
        // 可选，缓存时间，单位为毫秒, 默认 5 分钟, 只对单一值生效
        cacheDuration: 1000 * 60 * 5,
        // 可选，当输入为空时，显示的文本，默认为 '--'
        emptyText: '--',
      });
    },
  },
};
</script>

<style>
@import url('./css/reset.css');
@import url('./css/app.css');

#app {
  min-width: 1280px;
  color: #737987;

  > .bk-loading {
    z-index: 999999;
  }
}
</style>
<style scoped lang="postcss">
.banner {
  width: 100%;
  height: 30px;
  padding: 0 20px;
  font-size: 14px;
  line-height: 30px;
  color: #ec531d;
  text-align: center;
  background-color: #fdf6e2;
  border-bottom: 1px solid #d5dbe0;

  .icon-close {
    float: right;
    line-height: 30px;
    cursor: pointer;
  }
}

.notice {
  z-index: 9999;

  .notice-content {
    height: auto;
    min-height: 200px;
    margin-top: -20px;
    font-size: 16px;

    .notice-list {
      line-height: 30px;
    }
  }

  .notice-newversion {
    padding-left: 60px;

    .notice-header {
      font-size: 16px;
      font-weight: bold;
      line-height: 3;

      &::before {
        position: absolute;
        left: 60px;
        width: 15px;
        height: 15px;
        margin-top: 18px;
        background: url('images/guide-txt.png') no-repeat;
        content: '';
      }
    }
  }

  .notice-footer {
    text-align: center;

    a {
      color: #fff;
    }
  }
}
</style>
