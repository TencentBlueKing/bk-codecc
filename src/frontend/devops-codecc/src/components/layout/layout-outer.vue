<template>
  <div class="layou-outer">
    <!-- <nav-top /> -->
    <header class="page-header">
      <div class="app-logo">
        <img
          @click="$router.push({ name: 'task-list' })"
          :src="logo"
          :alt="isInnerSite ? $t('腾讯代码分析') : $t('代码分析')"
        />
        <div class="breadcrumb">
          {{ isInnerSite ? $t('腾讯代码分析') : $t('代码分析') }}
          <a v-if="isInnerSite" class="sub-header-link" target="_blank" :href="iwikiCodeccHome">
            <span class="bk-icon icon-question-circle"></span>
          </a>
        </div>
      </div>
      <bk-tab
        ext-cls="cc-panels"
        :label-height="59"
        :key="currentNavTabKey"
        type="unborder-card"
        :active.sync="currentNavTab"
        @tab-change="changeTab"
      >
        <bk-tab-panel
          v-for="(panel, index) in panels"
          v-bind="panel"
          :key="index"
        >
          <span slot="label" @click="handleRedPoint(panel.name)">
            <span>{{ panel.label }}</span>
            <span v-if="panel.name === 'checker' && !hasRedPointStore" class="red-point"></span>
          </span>
        </bk-tab-panel>
      </bk-tab>
      <!-- <div class="breadcrumb">{{title}}</div> -->
    </header>
    <main
      class="page-main"
      :class="{ 'has-banner': maintainMes.noticeSerial && !isMaintainClose }"
    >
      <div class="page-content">
        <div class="main-container">
          <slot />
        </div>
      </div>
    </main>
  </div>
</template>

<script>
import logo from '@/images/logo.svg';
import { mapGetters, mapState } from 'vuex';
import { leaveConfirm } from '@/common/leave-confirm';
// import NavTop from './nav-top'
import DEPLOY_ENV from '@/constants/env';

export default {
  components: {
    // NavTop
  },
  data() {
    return {
      logo,
      toolTips: {
        version: {
          content: this.$t('切到旧版CodeCC'),
        },
      },
      panels: [
        { name: 'task', label: this.$t('任务') },
        { name: 'defect', label: this.$t('问题') },
        { name: 'checkerset', label: this.$t('multi.规则集') },
        { name: 'checker', label: this.$t('规则') },
        { name: 'ignore', label: this.$t('忽略配置') },
      ],
      iwikiCodeccHome: window.IWIKI_CODECC_HOME,
      hasRedPointStore: window.localStorage.getItem('red-point-checker-20241101'),
      moreDropdownList: [
        // { name: 'pathShield', label: this.$t('路径屏蔽') },
        { name: 'ignoreList', label: this.$t('忽略配置') },
        // { name: 'operationAudit', label: this.$t('操作审计') },
      ],
      isInnerSite: DEPLOY_ENV === 'tencent',
      currentNavTabKey: 1,
    };
  },
  computed: {
    ...mapGetters('op', {
      isMaintainClose: 'isMaintainClose',
    }),
    ...mapState('op', {
      maintainMes: 'maintain',
    }),
    title() {
      const { title } = this.$route.meta;
      return this.$t(`${title}`);
    },
    currentNavTab() {
      const routeName = this.$route.name;
      const navMap = {
        'task-new': 'task',
        'task-list': 'task',
        'project-defect-list': 'defect',
        'project-ccn-list': 'defect',
        'checker-list': 'checker',
        'checkerset-list': 'checkerset',
        'checkerset-manage': 'checkerset',
        ignoreList: 'ignore',
      };
      return navMap[routeName];
    },
  },
  methods: {
    changeTab(name) {
      if (name === 'checker') {
        this.$router.push({ name: 'checker-list' });
      } else if (name === 'task') {
        this.$router.push({ name: 'task-list' });
      } else if (name === 'checkerset') {
        this.$router.push({ name: 'checkerset-list' });
      } else if (name === 'ignore') {
        this.$router.push({ name: 'ignoreList' });
      } else if (name === 'defect') {
        this.$router.push({
          name: 'project-defect-list',
          query: { author: this.$store.state.user.username },
        });
      }
    },
    handleRedPoint(name) {
      if (name === 'checker' && !this.hasRedPointStore) {
        this.currentNavTabKey = 2;
        window.localStorage.setItem('red-point-checker-20241101', '1');
        this.hasRedPointStore = true;
        this.$router.push({ name: 'checker-list' });
      }
    },
    triggerHandler(item) {
      if (this.$route.name === item.name) return;
      leaveConfirm().then(() => {
        this.showMoreCom = true;
        this.$router.push({
          name: item.name,
        });
      });
    },
  },
};
</script>

<style lang="postcss" scoped>
@import url('../../css/variable.css');

.layou-outer {
  --headerHeight: 60px;

  .page-header {
    display: flex;
    height: var(--headerHeight);
    align-items: center;
    padding: 0 28px;
    background: #fff;
    border-bottom: 1px solid #dcdee5;

    .app-logo {
      position: absolute;
      display: flex;
      flex: 2;

      img {
        height: 25px;
        margin-top: 15px;
        cursor: pointer;
      }
    }

    .breadcrumb {
      margin-left: 8px;
      line-height: 60px;
      color: #63656e;
    }

    >>> .cc-panels {
      display: flex;
      flex: 5;
      justify-content: center;

      .bk-tab-label-item,
      .bk-tab-label-item.active {
        background-color: #fff;
      }

      .bk-tab-label {
        font-size: 16px;
      }

      .bk-tab-section {
        display: none;
      }
    }

    .app-version {
      display: flex;
      font-size: 22px;
      cursor: pointer;
      flex: 2;

      &:hover {
        color: #3a84ff;
      }
    }

    .sub-header-link span {
      padding-left: 4px;
      font-size: 14px;
      color: #c4cdd6;
      cursor: pointer;
    }
  }

  .page-main {
    height: calc(100vh - var(--navTopHeight));
    overflow: hidden auto;

    &.has-banner {
      height: calc(100vh - var(--navTopHeight) - var(--bannerHeight));
    }

    .page-content,
    .main-container,
    .main-content {
      height: 100%;
    }

    .main-container {
      padding: 20px 0;
    }
  }

  .red-point {
    margin-bottom: 10px;
    margin-left: -3px;
  }
}
</style>
