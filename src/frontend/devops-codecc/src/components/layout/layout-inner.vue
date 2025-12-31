<template>
  <div class="layout-inner">
    <!-- <nav-top /> -->
    <header class="page-header">
      <section class="task-info">
        <!-- <span @click="$router.push({ name: 'task-list' })" class="codecc-icon icon-codecc curpt"></span> -->
        <span
          @click="handleToHomePage"
          class="curpt breadcrumb-txt codecc-cc cc-link"
        >
          <span class="codecc-cc-icon"></span>CodeCC
        </span>
        <i class="bk-icon icon-angle-right fs20"></i>
        <div class="bread-crumb-name" v-bk-clickoutside="toggleCrumbList">
          <label
            :class="[
              isActiveRecords ? 'expand' : '',
              isTaskDetail ? 'active' : 'focus',
            ]"
          >
            <span
              class="task-name"
              :title="taskDetail.nameCn"
              @click="handleNameClick"
            >
              {{
                taskDetail.nameCn.length > 30
                  ? taskDetail.nameCn.slice(0, 30) + '...'
                  : taskDetail.nameCn
              }}
            </span>
            <i
              :class="[
                'bk-icon icon-angle-right fs20',
                { active: isActiveRecords },
              ]"
              @click.stop="breadCrumbItemClick"
            ></i>
          </label>
          <crumb-records
            v-if="isActiveRecords"
            :param-id="'taskId'"
            :param-name="'nameCn'"
            :records="taskList.enableTasks"
            :handle-record-click="handleRecordClick"
            :active-id="taskId"
          >
          </crumb-records>
        </div>
        <template v-if="breadcrumb.children">
          <span
            @click="$router.push({ name: breadcrumb.children.prev })"
            class="curpt breadcrumb-txt cc-link"
          >
            {{ breadcrumb.name }}
          </span>
          <i class="bk-icon icon-angle-right fs20"></i>
          <span class="breadcrumb-txt">{{ breadcrumb.children.name }}</span>
        </template>
        <span v-else class="breadcrumb-txt">{{ breadcrumb.name }}</span>
        <bk-popover theme="light" :delay="[600, 10]">
          <span class="breadcrumb-repo">{{ codeURLString }}</span>
          <div slot="content">
            <section>
              <p
                class="codelib-item"
                v-for="item in codeURLs"
                :key="item.aliasName"
              >
                {{ `${item.aliasName}@${item.branch}` }}
              </p>
            </section>
          </div>
        </bk-popover>
      </section>
      <section class="task-status">
        <div class="build-progress-content small" v-if="llmIgnoreTotal && llmIgnoreCurrent !== llmIgnoreTotal">
          <p class="progress-text">
            {{ $t('LLM告警验证中') }}
            <span class="progress-precent"
            >{{ llmIgnoreCurrent }}/{{ llmIgnoreTotal }}</span
            >
          </p>
          <bk-progress
            :percent="llmIgnoreCurrent / llmIgnoreTotal || 0"
            :show-text="false"
            :color="'#3a84ff'"
            stroke-width="4"
          ></bk-progress>
        </div>
        <template v-if="status && status.status === 1">
          <bk-button disabled theme="primary" icon="bk-icon icon-play-circle-shape" class="cc-white">{{
            $t('立即检查')
          }}</bk-button>
        </template>
        <template v-else-if="isAnalyzeLoading">
          <bk-button disabled theme="primary" icon="loading" class="cc-white">{{
            $t('立即检查')
          }}</bk-button>
        </template>
        <template v-else-if="isAnalysing">
          <div
            class="build-progress-content"
            v-if="
              Object.prototype.hasOwnProperty.call(
                buildProgressRule,
                'displayStep'
              )
            "
          >
            <p class="progress-text" v-if="buildProgressRule.displayName">
              <span class="display-step">
                <span>{{ buildProgressRule.displayName }}</span
                >：
                <span>{{
                  $t(
                    `${getToolStatus(
                      buildProgressRule.displayStep,
                      buildProgressRule.displayToolName
                    )}`
                  )
                }}</span>
              </span>
              <span class="progress-precent"
              >{{ buildProgressRule.displayProgress }}%</span
              >
            </p>
            <bk-progress
              :percent="buildProgressRule.displayProgress / 100 || 0"
              :show-text="false"
              :color="'#3a84ff'"
              stroke-width="4"
            ></bk-progress>
          </div>
          <li class="cc-fading-circle">
            <div class="cc-circle1 cc-circle"></div>
            <div class="cc-circle2 cc-circle"></div>
            <div class="cc-circle3 cc-circle"></div>
            <div class="cc-circle4 cc-circle"></div>
            <div class="cc-circle5 cc-circle"></div>
            <div class="cc-circle6 cc-circle"></div>
            <div class="cc-circle7 cc-circle"></div>
            <div class="cc-circle8 cc-circle"></div>
            <div class="cc-circle9 cc-circle"></div>
            <div class="cc-circle10 cc-circle"></div>
            <div class="cc-circle11 cc-circle"></div>
            <div class="cc-circle12 cc-circle"></div>
          </li>
          <span
            v-bk-tooltips="tipsHtmlConfig"
            @click="goToLog"
            class="update-time cc-link-primary"
            style="color: #3a84ff"
          >{{ $t('分析中') }}</span
          >
          <bk-button
            v-if="isRbac === true && taskDetail.createFrom === 'bs_codecc'"
            key="triggerAnalyze"
            v-perm="{
              hasPermission: true,
              disablePermissionApi: false,
              permissionData: {
                projectId: projectId,
                resourceType: 'codecc_task',
                resourceCode: taskId,
                action: 'codecc_task_analyze',
              },
            }"
            @click="triggerAnalyze"
            icon="icon codecc-icon icon-refresh-2"
          >
            {{ $t('重新检查') }}
          </bk-button>
          <bk-button
            v-else
            @click="triggerAnalyze"
            icon="icon codecc-icon icon-refresh-2"
          >{{ $t('重新检查') }}</bk-button
          >
        </template>
        <template v-else>
          <span
            class="bk-icon codecc-icon"
            :class="{
              'icon-pipeline': taskDetail.createFrom === 'bs_pipeline',
              'icon-manual-trigger': taskDetail.createFrom === 'bs_codecc',
            }"
          >
          </span>
          <span
            v-bk-tooltips="tipsHtmlConfig"
            @click="goToLog"
            class="update-time"
          >
            {{ $t('最近检查') }}:
            {{ latestUpdate['buildNum'] ? `#${latestUpdate['buildNum']}` : '' }}
            {{ formatDate(latestUpdate['lastAnalysisTime']) }}
            <span
              v-if="latestUpdate['buildNum'] && taskStatus !== 0"
              :class="{ fail: taskStatus === 1 }"
            >({{ ['成功', '失败', '', '进行中'][taskStatus] }})</span
            >
          </span>
          <bk-popover
            v-if="projectId === 'CUSTOMPROJ_PCG_RD'"
            theme="light"
            :content="$t('由PCG EP度量平台创建的任务，暂不支持立即检查')"
            placement="bottom"
          >
            <bk-button theme="primary" disabled>{{ $t('立即检查') }}</bk-button>
          </bk-popover>
          <bk-popover
            v-else-if="projectId.startsWith('CLOSED_SOURCE_')"
            theme="light"
            :content="$t('由工蜂创建的闭源仓库扫描任务，暂不支持立即检查')"
            placement="bottom"
          >
            <bk-button theme="primary" disabled>{{ $t('立即检查') }}</bk-button>
          </bk-popover>
          <bk-button
            v-else-if="isRbac === true && taskDetail.createFrom === 'bs_codecc'"
            key="analyze"
            v-perm="{
              hasPermission: true,
              disablePermissionApi: false,
              permissionData: {
                projectId: projectId,
                resourceType: 'codecc_task',
                resourceCode: taskId,
                action: 'codecc_task_analyze',
              },
            }"
            theme="primary"
            @click="triggerAnalyze"
            icon="bk-icon icon-play-circle-shape"
          >
            {{ $t('立即检查') }}
          </bk-button>
          <bk-button
            v-else-if="taskDetail.hasNoPermission"
            theme="primary"
            disabled
          >{{ $t('立即检查') }}</bk-button
          >
          <bk-button
            v-else
            theme="primary"
            @click="triggerAnalyze"
            icon="bk-icon icon-play-circle-shape"
          >
            {{ $t('立即检查') }}
          </bk-button>
        </template>
        <!-- <tempalte v-else>
                    <p class="progress-desc">60%</p>
                    <bk-progress :percent="percent" :show-text="false" color="#7572dc" stroke-width="6"></bk-progress>
                    <i class="bk-icon card-tool-status icon-circle-2-1 spin-icon"></i>
                    <i class="bk-icon codecc-icon icon-pause" @click="triggerAnalyze"></i>
                </tempalte> -->
      </section>
    </header>
    <main
      class="page-main"
      :class="{ 'has-banner': maintainMes.noticeSerial && !isMaintainClose }"
    >
      <div class="page-sider">
        <nav class="nav">
          <bk-navigation-menu
            ref="menu"
            class="menu"
            @select="handleMenuSelect"
            :default-active="activeMenu.id"
            :before-nav-change="handleNavChange"
            :toggle-active="true"
            item-hover-bg-color="#e1ecff"
            item-hover-color="#3a84ff"
            item-active-bg-color="#e1ecff"
            item-active-color="#3a84ff"
            item-active-icon-color="#3a84ff"
            item-hover-icon-color="#3a84ff"
            sub-menu-open-bg-color="#f0f1f5"
            item-default-bg-color="#fff"
            item-default-color="#63656e"
          >
            <bk-navigation-menu-item
              v-for="item in menus"
              :has-child="item.children && !!item.children.length"
              :group="item.group"
              :key="item.id"
              :icon="item.icon"
              :disabled="item.disabled"
              :id="item.id"
              :href="item.href"
            >
              <span>
                {{ item.name }}
              </span>
              <template #child>
                <bk-navigation-menu-item
                  :id="child.id"
                  :disabled="child.disabled"
                  :icon="child.icon"
                  :key="child.id"
                  :href="child.href"
                  v-for="child in item.children"
                >
                  <span>{{ child.name }}</span>
                </bk-navigation-menu-item>
              </template>
            </bk-navigation-menu-item>
          </bk-navigation-menu>
        </nav>
      </div>
      <div class="page-content">
        <template v-if="$route.meta.breadcrumb !== 'inside'">
          <!-- <div class="breadcrumb">
                        <div class="breadcrumb-name">{{breadcrumb.name}}</div>
                        <div class="breadcrumb-extra" v-if="$route.meta.record !== 'none'">
                            <a @click="openSlider"><i class="bk-icon icon-order"></i>{{$t('操作记录')}}</a>
                        </div>
                    </div> -->
          <div
            class="main-container"
            :class="{
              'has-banner': maintainMes.noticeSerial && !isMaintainClose,
              'task-detail': $route.name === 'task-detail',
            }"
          >
            <slot />
          </div>
        </template>
        <template v-else>
          <slot />
        </template>
      </div>
      <!-- <Record :visible.sync="show" :data="this.$route.name" /> -->
    </main>
    <bk-dialog
      v-model="dialogVisible"
      :theme="'primary'"
      @confirm="reAnalyze"
      :title="$t('重新检查')"
    >
      {{ $t('任务正在分析中，是否中断并重新分析？') }}
    </bk-dialog>
    <bk-dialog
      v-model="coldTaskVisible"
      :theme="'primary'"
      @confirm="triggerAnalyze"
      :title="$t('立即分析')"
    >
      {{ $t('由于当前任务长时间未分析，部分数据已归档。可以重新触发一次分析。') }}
    </bk-dialog>
    <div class="dn">
      <div id="analysis-tips">
        <div class="pb10">{{ $t('点击文字可查看分析详情和记录') }}</div>
        <div class="guide-btn">
          <span class="btn-item" @click="confirm">{{ $t('我知道了') }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import logo from '@/images/logo.svg';
// import NavTop from './nav-top'
import { mapGetters, mapState } from 'vuex';
import { format } from 'date-fns';
import { getToolStatus } from '@/common/util';
// import Record from '@/components/operate-record/index'
import taskWebsocket from '@/common/taskWebSocket';
import crumbRecords from '@/components/crumb-records';
import { leaveConfirm } from '@/common/leave-confirm';

export default {
  components: {
    crumbRecords,
    // NavTop,
    // Record
  },
  data() {
    return {
      logo,
      show: false,
      isActiveRecords: false,
      toolTips: {
        list: {
          content: this.$t('我的任务'),
        },
        new: {
          content: this.$t('新建任务'),
        },
        version: {
          content: this.$t('切到旧版CodeCC'),
        },
      },
      lastAnalysisResultList: [],
      dialogVisible: false,
      funcId: [
        'register_tool',
        'tool_switch',
        'task_info',
        'task_switch',
        'task_code',
        'checker_config',
        'scan_schedule',
        'filter_path',
        'defect_manage',
        'trigger_analysis',
      ],
      isAnalyzeLoading: false,
      isShowSelectTime: null,
      buildProgressRule: {},
      taskStatus: 0,
      hasRedPointStore: window.localStorage.getItem('tapd-20210628'),
      tipsHtmlConfig: {},
      dimension: 'DEFECT',
      coldTaskVisible: false,
      llmIgnoreCurrent: 0,
      llmIgnoreTotal: 0,
    };
  },
  computed: {
    ...mapGetters('op', {
      isMaintainClose: 'isMaintainClose',
    }),
    ...mapState('op', {
      maintainMes: 'maintain',
    }),
    ...mapState(['toolMeta', 'taskId', 'constants', 'isRbac']),
    ...mapState('task', {
      taskList: 'list',
      taskDetail: 'detail',
      status: 'status',
    }),
    ...mapState('defect', {
      scaList: 'scaList',
    }),
    ...mapState('tool', {
      toolMap: 'mapList',
    }),
    projectId() {
      return this.$route.params.projectId || '';
    },
    toolId() {
      return this.$route.params.toolId;
    },
    toolDisplayName() {
      return this.toolMap[this.toolId] && this.toolMap[this.toolId].displayName;
    },
    menus() {
      // const { enableToolList } = this.taskDetail
      const enableToolList = this.taskDetail.enableToolList || [];
      const routeParams = { ...this.$route.params };
      const firstTool = enableToolList[0] || {};
      let { toolName } = firstTool;
      let toolPattern = firstTool.toolPattern && firstTool.toolPattern.toLocaleLowerCase();
      // 没有工具或第一个工具是圈复杂度或重复率，就跳转到coverity代码问题
      if (
        !toolName
        || toolName === 'CCN'
        || toolName === 'DUPC'
        || toolName === 'CLOC'
        || toolName === 'STAT'
        || toolName === 'SCC'
      ) {
        toolName = 'COVERITY';
        toolPattern = 'coverity';
      }
      const params = { ...this.$route.params, toolId: toolName };
      // delete routeParams.toolId
      const menuBase = [
        {
          id: 'task-detail',
          name: this.$t('总览'),
          routeName: 'task-detail',
          icon: 'icon-apps',
          href: this.$router.resolve({
            name: 'task-detail',
            params: routeParams,
            query: { buildNum: 'latest' },
          }).href,
        },
        {
          id: 'task-settings',
          name: this.$t('设置'),
          routeName: 'task-settings',
          icon: 'codecc-icon icon-setting',
          group: true,
          href: this.$router.resolve({
            name: 'task-settings',
            params: routeParams,
          }).href,
        },
        {
          id: 'defect',
          name: this.$t('代码问题'),
          routeName: 'defect-lint',
          icon: 'icon-order',
          href: this.$router.resolve({
            name: `defect-${this.dimension.toLocaleLowerCase()}-list`,
            params,
            query: { dimension: this.dimension },
          }).href,
        },
        {
          id: 'ccn',
          name: this.$t('圈复杂度'),
          routeName: 'defect-ccn',
          icon: 'codecc-icon icon-complexity',
          href: this.$router.resolve({ name: 'defect-ccn-list', params }).href,
        },
        {
          id: 'dupc',
          name: this.$t('重复率'),
          routeName: 'defect-dupc',
          icon: 'codecc-icon icon-repeat-rate',
          href: this.$router.resolve({ name: 'defect-dupc-list', params }).href,
        },
        {
          id: 'cloc',
          name: this.$t('代码统计'),
          routeName: 'defect-cloc',
          icon: 'codecc-icon icon-statistics',
          href: this.$router.resolve({ name: 'defect-cloc-list', params }).href,
        },
      ];
      const scaMenusConfig = {
        id: 'sca',
        name: this.$t('软件成分'),
        routeName: 'defect-sca',
        icon: 'codecc-icon icon-software-components',
        href: this.$router.resolve({ name: 'defect-sca-pkg-list', params }).href,
      };
      // 若scaList不为空，则展示软件成分
      if (this.scaList.length > 0) {
        menuBase.push(scaMenusConfig);
      }

      return menuBase;
    },
    isTaskDetail() {
      return this.$route.name === 'task-detail';
    },
    activeMenu() {
      // 从所有菜单项中找出path与$route中的path一致或包含则为当前菜单项
      const routeName = this.$route.name;
      let activeMenu = {};
      this.menus.forEach((menu) => {
        if (routeName.indexOf(menu.routeName) !== -1) {
          activeMenu = { id: menu.id, name: menu.name, toolId: menu.toolId };
        } else if (!activeMenu.id && routeName.indexOf('defect-') !== -1) {
          activeMenu = { id: 'defect', name: this.$t('代码问题') };
        }
        // if (activeMenu.id === 'task-settings') {
        //   window.localStorage.setItem('redtips-nav-settings-20210628', '1')
        //   this.hasRedPointStore = true
        // }
      });

      return activeMenu;
    },
    breadcrumb() {
      const { name } = this.activeMenu;
      let children = null;
      if (this.$route.name === 'task-detail-log') {
        children = { name: this.$t('分析记录'), prev: 'task-detail' };
      }

      return { name, children };
    },
    allTasks() {
      return this.taskList.enableTasks.concat(this.taskList.disableTasks);
    },
    isAnalysing() {
      let isAnalysing = 0;
      this.lastAnalysisResultList.forEach((result) => {
        if (result.curStep < 5 && result.curStep > 0 && result.stepStatus !== 1) isAnalysing = 1;
      });
      if (
        Object.prototype.hasOwnProperty.call(
          this.buildProgressRule,
          'displayStep',
        )
      ) {
        if (this.buildProgressRule.displayStep >= 5) isAnalysing = 0;
      }
      return isAnalysing;
    },
    hasRecords() {
      return (
        this.taskList
        && Array.isArray(this.taskList.enableTasks)
        && this.taskList.enableTasks.length
      );
    },
    latestUpdate() {
      const timeList = this.lastAnalysisResultList.map(item => item.lastAnalysisTime);
      const maxTime = Math.max(...timeList);
      const latestUpdate = this.lastAnalysisResultList.find(item => item.lastAnalysisTime === maxTime);
      return latestUpdate || {};
    },
    codeURLs() {
      const codeURL = this.taskDetail.codeLibraryInfo
        && this.taskDetail.codeLibraryInfo.codeInfo;
      return codeURL || [];
    },
    codeURLString() {
      const urlList = this.codeURLs.map(item => `${item.aliasName}@${item.branch}`);
      return urlList.join(',');
    },
  },
  watch: {
    taskId(newValue, oldValue) {
      this.init();
    },
    activeMenu(active) {
      if (
        active.id === 'task-settings'
        && window.localStorage.getItem('tapd-20210628')
      ) {
        this.hasRedPointStore = true;
      }
    },
    'status.status'(newValue, oldValue) {
      if (newValue === 2) {
        this.coldTaskVisible = true;
      }
    },
    'taskDetail.projectId'(val) {
      const { projectId } = this.$route.params;
      if (val !== projectId && val && this.taskDetail.createFrom === 'gongfeng_scan') {
        this.$router.replace({
          params: {
            projectId: val,
          },
        });
      }
    },
  },
  created() {
    if (
      !window.localStorage.getItem('tips-analyse-20211119')
      && !this.$route.query.entityId
    ) {
      this.tipsHtmlConfig = {
        allowHtml: true,
        width: 280,
        delay: 300,
        theme: 'analysis-tips light',
        trigger: 'click',
        showOnInit: true,
        content: '#analysis-tips',
      };
    }
    this.init();
    window.addEventListener('beforeunload', this.beforeunloadFn);
  },
  mounted() {
    this.initWebSocket();
    this.initOtherWebSocket();
    this.initScaList();
  },
  beforeDestroy() {
    taskWebsocket.disconnect();
    window.removeEventListener('beforeunload', this.beforeunloadFn);
  },
  updated() {
    if (this.$refs.menu && this.$refs.menu.$children[2]) {
      if (this.$refs.menu.$children.every(item => !item.collapse)) {
        this.$refs.menu.$children[2].handleSbmenuClick();
      }
    }
  },
  methods: {
    async init() {
      this.fetchDimension();
      if (!this.taskId) return;
      const res = await this.$store.dispatch('task/overView', {
        taskId: this.taskId,
        buildNum: this.$route.query.buildNum,
        buildId: this.$route.query.buildId,
      });
      if (res.lastAnalysisResultList) {
        this.taskDetail = this.detail;
        this.lastAnalysisResultList = res.lastAnalysisResultList || [];
        this.taskStatus = res.status;
      }
    },
    handleTaskChange(taskId) {
      // const task = this.taskList.enableTasks.find(task => task.taskId === taskId) || {}
      this.$router.push({
        name: 'task-detail',
        params: { ...this.$route.params, taskId },
        query: { buildNum: 'latest' },
      });
    },
    handleRecordClick(task) {
      const { taskId } = task;
      this.isActiveRecords = false;
      this.$router.push({
        name: 'task-detail',
        params: { ...this.$route.params, taskId },
        query: { buildNum: 'latest' },
      });
    },
    handleMenuSelect(id, item) {
      if (item.href !== this.$route.path) {
        this.$router.push(item.href);
      }
    },
    openSlider() {
      this.show = true;
    },
    triggerAnalyze() {
      if (this.taskDetail.projectId.startsWith('git_')) {
        const { projectName, pipelineId } = this.taskDetail;
        const { buildId } = this.latestUpdate;
        this.$bkInfo({
          title: this.$t('立即检查'),
          subTitle: this.$t('此代码检查任务需要到Stream启动，是否前往Stream？'),
          maskClose: true,
          confirmFn(name) {
            window.open(
              `${window.STREAM_SITE_URL}/pipeline/${pipelineId}/detail/${buildId}/?page=1#${projectName}`,
              '_blank',
            );
          },
        });
      } else if (this.taskDetail.createFrom.indexOf('pipeline') !== -1) {
        const { projectId, pipelineId } = this.taskDetail;
        this.$bkInfo({
          title: this.$t('立即检查'),
          subTitle: this.$t('此代码检查任务需要到流水线启动，是否前往流水线？'),
          maskClose: true,
          confirmFn(name) {
            window.open(
              `${window.DEVOPS_SITE_URL}/console/pipeline/${projectId}/${pipelineId}/history`,
              '_blank',
            );
          },
        });
      } else {
        this.isAnalysing ? (this.dialogVisible = true) : this.analyse();
      }
    },
    async analyse(isAnalysing = 0) {
      this.isAnalyzeLoading = true;
      this.tipsHtmlConfig = {};
      await this.$store.dispatch('task/triggerAnalyze').finally(() => {
        setTimeout(() => {
          this.isAnalyzeLoading = false;
          this.init();
        }, 100);
      });
    },
    reAnalyze() {
      this.analyse(1);
    },
    formatDate(dateNum, isTime) {
      if (dateNum) {
        return isTime
          ? format(dateNum, 'HH:mm:ss')
          : format(dateNum, 'yyyy-MM-dd HH:mm:ss');
      }
      return '-- --';
    },
    getToolStatus(num, tool) {
      return getToolStatus(num, tool);
    },
    initWebSocket() {
      const subscribe = `/topic/analysisInfo/taskId/${this.taskId}`;

      taskWebsocket.connect(this.projectId, this.taskId, subscribe, {
        success: (res) => {
          const data = JSON.parse(res.body);
          // console.log(data, 'analysisInfo')
          let hasNewTool = 1;
          this.lastAnalysisResultList.forEach((item) => {
            if (item.toolName === data.toolName) {
              Object.assign(item, data);
              hasNewTool = 0;
            }
          });
          // 暂时屏蔽刷新页面
          // if (hasNewTool && this.$route.name === 'task-detail') this.init()
        },
        // error: message => this.$showTips({ message, theme: 'error' }),
        error: message => console.error(message),
      });
    },
    initOtherWebSocket() {
      if (taskWebsocket.stompClient.connected) {
        // 扫描进度
        taskWebsocket.subscribeMsg(`/topic/generalProgress/taskId/${this.taskId}`, {
          success: (res) => {
            const data = JSON.parse(res.body);
            console.log(`/topic/generalProgress/taskId/${this.taskId}`, data);
            this.buildProgressRule = data;
            // console.log(this.buildProgressRule);
          },
          error: message => this.$showTips({ message, theme: 'error' }),
        });
        // 大模型忽略进度
        taskWebsocket.subscribeMsg(`/topic/defect/llm/${this.taskId}`, {
          success: (res) => {
            const data = res.body;
            console.log(`/topic/defect/llm/${this.taskId}`, data);
            // 大模型忽略进度, data为 string, 例如'1/2'，需要转换成数字
            const [current, total] = data.split('/').map(Number);
            this.llmIgnoreCurrent = current;
            this.llmIgnoreTotal = total;
          },
          error: message => this.$showTips({ message, theme: 'error' }),
        });
      } else {
        // websocket还没连接的话，1s后重试
        setTimeout(() => {
          this.initOtherWebSocket();
        }, 1000);
      }
    },
    initScaList() { // 初始化scaList 用于软件成分的展示
      this.$store.dispatch('defect/getScaList');
    },
    toggleCrumbList(isShow) {
      if (this.hasRecords) {
        this.isActiveRecords = typeof isShow === 'boolean' ? isShow : false;
      }
    },
    breadCrumbItemClick() {
      this.toggleCrumbList(!this.isActiveRecords);
    },
    handleNameClick() {
      if (this.isTaskDetail) {
        this.breadCrumbItemClick();
      } else {
        const link = {
          name: 'task-detail',
          params: { projectId: this.projectId, taskId: this.taskId },
          query: { buildNum: 'latest' },
        };
        this.$router.push(link);
      }
    },
    handleToHomePage() {
      // if (this.taskDetail.createFrom !== 'gongfeng_scan') {
      this.$router.push({ name: 'task-list' });
      // }
    },
    beforeunloadFn() {
      taskWebsocket.disconnect();
    },
    goToLog() {
      this.$router.push({ name: 'task-detail-log' });
    },
    confirm() {
      localStorage.setItem('tips-analyse-20211119', 1);
      this.tipsHtmlConfig.content = false;
    },
    fetchDimension() {
      this.$store
        .dispatch('defect/getDimension', {
          taskIdList: [this.$route.params.taskId],
        })
        .then((res) => {
          const list = res.filter(item => item.key !== 'CCN'
              && item.key !== 'DUPC'
              && item.key !== 'CLOC'
              && item.key !== 'STAT'
              && item.key !== 'SCC');
          this.dimension = (list[0] && list[0].key) || 'DEFECT';
        });
    },
    async handleNavChange() {
      if (window.changeAlert) {
        const next = await leaveConfirm();
        return next;
      }
      return true;
    },
  },
};
</script>

<style>
@import url('../../assets/bk_icon_font/style.css');

.tippy-popper .tippy-tooltip.analysis-tips-theme.light-theme {
  width: 280px !important;
  height: 67px;
  padding: 10px;
  color: #fff;
  background-color: #3b91fb;

  .guide-btn {
    position: absolute;
    right: 18px;
    bottom: 15px;
  }

  .btn-item {
    padding: 2px 5px;
    color: #3b91fb;
    cursor: pointer;
    background-color: #fff;
    border-radius: 3px;
  }
}
</style>

<style lang="postcss" scoped>
@import url('../../css/variable.css');

.layout-inner {
  --headerHeight: 60px;

  .page-header {
    display: flex;
    height: var(--headerHeight);
    align-items: center;
    justify-content: space-between;
    text-align: center;
    background: #fff;

    /* border-bottom: 1px solid #d1d1d1; */
    border-bottom: 1px solid #dcdee5;

    .task-info {
      display: flex;
      align-items: center;

      .icon-angle-right {
        font-weight: bold;
        transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      }
    }

    .bread-crumb-name {
      height: 60px;
      padding-right: 4px;
      margin: 0 0 0 8px;
      line-height: 60px;

      label {
        padding: 6px 4px 6px 0;
        border-radius: 10px;
      }

      label.expand,
      label.active:hover {
        background-color: #f5f5f5;
      }

      label.focus:hover {
        color: #3c96ff;
        cursor: pointer;
      }

      .icon-angle-right {
        color: #3c96ff;

        &.active {
          transform: rotate(90deg);
        }
      }

      .task-name {
        margin-right: 4px;
      }

      .icon-angle-right {
        display: inline-block;
        cursor: pointer;
      }
    }

    .task-status {
      display: flex;
      align-items: center;
      padding-right: 32px;
      font-size: 12px;

      .icon-pipeline,
      .icon-manual-trigger {
        margin-right: 8px;
        font-size: 14px;
        color: #8f9aae;
      }

      .update-time {
        margin-right: 16px;
        cursor: pointer;

        .success {
          color: $successColor;
        }

        .fail {
          color: $failColor;
        }

        &:hover {
          color: #699df4;
        }
      }

      .cc-fading-circle {
        margin-right: 8px;
      }
    }

    .build-progress-content {
      width: 260px;
      margin-right: 14px;

      &.small {
        width: 160px;

        .bk-progress {
          width: 160px;
        }
      }

      .bk-progress {
        position: relative;
        top: -2px;
        width: 260px;
      }
    }

    .progress-text {
      display: flex;
      justify-content: space-between;
      font-size: 12px;
      color: #b8bdc3;

      .progress-precent {
        color: #333;
      }
    }

    .progress-desc {
      position: absolute;
      top: 8px;
      right: 60px;
      padding-right: 12px;
      color: #333;
      text-align: right;
    }

    .bk-progress {
      display: inline-flex;
      width: 384px;
    }

    .icon-pause {
      margin-left: 20px;
      font-size: 16px;
    }

    .app-logo {
      width: var(--siderWidth);
      height: var(--headerHeight);
      line-height: 55px;
      text-align: center;

      /* border-right: 1px solid #d1d1d1; */
      img {
        height: 25px;
        cursor: pointer;
      }
    }

    .app-list,
    .app-new {
      width: 56px;
      height: var(--headerHeight);
      line-height: 50px;
      cursor: pointer;
      border-right: 1px solid #d1d1d1;

      &:hover {
        color: #3a84ff;
      }

      .icon-plus {
        font-weight: bolder;
      }
    }

    .app-version {
      position: absolute;
      right: 15px;
      font-size: 22px;
      cursor: pointer;

      &:hover {
        color: #3a84ff;
      }
    }
  }

  .page-sider {
    flex: 0 0 var(--siderWidth);
    width: var(--siderWidth);
    background: #fff;
    border-right: 1px solid #f0f1f5;

    .task-selector {
      height: 60px;
      border-bottom: 1px solid #dcdee5;

      /* padding: 14px 8px; */
    }

    .select-task {
      margin: 0 4px;
      font-size: 16px !important;
      border: 0 none;

      .bk-select-angle {
        top: 25px;
        right: 16px;
      }

      .bk-select-name {
        height: 60px;
        padding-left: 18px;
        line-height: 60px;
      }

      &.is-focus,
      &:focus {
        outline: none !important;
        box-shadow: none !important;
      }
    }

    .nav {
      /* border-bottom: 1px solid #d1d1d1; */
      max-height: calc(100vh - 160px);
      margin-bottom: 50px;
      overflow: auto;

      /* &::-webkit-scrollbar {
                    width: 6px;
                }
                &::-webkit-scrollbar-thumb {
                    border-radius: 13px;
                    background-color: #d4dae3;
                } */
    }

    .menu {
      background: #fff;

      .navigation-menu-item[group],
      .navigation-sbmenu[group] {
        margin-bottom: 0;
        border-bottom: 1px solid #f0f1f5;
      }

      .navigation-menu-item,
      .navigation-sbmenu-title {
        height: 49px;
        flex: 0 0 49px;
        margin: 0;
      }

      ::after {
        height: 0;
      }

      .navigation-sbmenu-content {
        margin-top: 0 !important;
      }
    }
  }

  .page-content {
    width: calc(100% - var(--siderWidth));
    overflow: hidden;
    background: #f5f7fa;
    flex: auto;

    >>> .breadcrumb {
      display: flex;
      height: 42px;
      padding: 0 16px;
      color: #333;
      background: #fff;
      align-items: center;

      /* border-bottom: 1px solid #dcdee5; */
      .breadcrumb-name {
        flex: 1;
      }

      .breadcrumb-extra {
        flex: none;
        font-size: 12px;

        .line {
          margin: 0 8px;
          color: #dcdee5;
        }

        a {
          cursor: pointer;

          .bk-icon {
            margin-right: 2px;
          }
        }
      }
    }
  }

  >>> .page-main {
    display: flex;
    min-height: calc(100vh - var(--navTopHeight));

    &.has-banner {
      height: calc(100vh - var(--navTopHeight) - var(--bannerHeight));
      min-height: calc(100vh - var(--navTopHeight) - var(--bannerHeight));
    }
  }

  >>> .main-container {
    height: calc(100vh - var(--navTopHeight));
    min-height: 554px;
    padding: 16px 20px 0 16px;
    margin-right: -9px;
    overflow-y: auto;

    &.has-banner {
      height: calc(100vh - var(--navTopHeight) - var(--bannerHeight));
    }

    &.task-detail {
      padding: 0;
      background: #fff;
    }
  }

  >>> .main-content {
    height: 100%;

    > .bk-tab {
      height: 100%;
    }
  }
}

>>> .navigation-menu-item-icon {
  text-align: center;
}

.icon-codecc {
  padding-left: 30px;
  font-size: 22px;
}

>>> .select-task {
  font-size: 16px;
  border: 0;

  &.bk-select.is-focus {
    box-shadow: none;
  }

  .bk-select-angle {
    transform: rotate(-90deg);
  }

  &.bk-select.is-focus .bk-select-angle {
    transform: rotate(0deg);
  }

  .bk-select-name {
    padding: 0 32px 0 6px;
  }
}

.breadcrumb-txt {
  padding: 0 8px;
  font-size: 16px;
  color: #63656e;
}

.breadcrumb-repo {
  display: block;
  max-width: 260px;
  padding-left: 15px;
  margin-bottom: -3px;
  overflow: hidden;
  font-size: 12px;
  color: #999;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.breadcrumb-text {
  margin: -2px 0 0 -6px;
  color: #63656e;
}

>>> .bk-select-dropdown-content {
  min-width: 160px !important;
}

>>> .bk-button .bk-icon {
  &.icon-refresh-2 {
    top: -1px;
    font-size: 14px;
  }

  .loading {
    color: #fff;
  }

  &.icon-play-circle-shape {
    font-size: 16px;
  }
}

.codecc-cc {
  .codecc-cc-icon {
    padding-left: 36px;
    margin-left: 15px;
    background: url('../../images/cc-grey.png') no-repeat center;
    background-size: 18px;
  }

  &:hover {
    .codecc-cc-icon {
      background: url('../../images/cc.png') no-repeat center;
      background-size: 18px;
    }
  }
}
</style>
