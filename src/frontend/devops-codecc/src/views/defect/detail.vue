<template>
  <div
    class="code-fullscreen full-active"
    v-bkloading="{ isLoading: isLoading, opacity: 0.3 }"
  >
    <div class="col-aside-left">
      <div class="col-aside-left-header">
        <bk-icon @click="closeDetail" class="arrows-left" type="arrows-left" />
        <div class="header-index">{{ fileIndex + 1 }}/{{ totalCount }}</div>
        <i
          class="codecc-icon icon-share"
          v-bk-tooltips="$t('分享此问题')"
          @click="shareVisible = true"
        ></i>
        <i
          class="codecc-icon icon-operate"
          v-bk-tooltips="$t('键盘操作指引')"
          @click="operateDialogVisible = true"
        ></i>
        <div class="dialog-block" v-if="shareVisible">
          <div class="share-header">{{ $t('分享此问题') }}</div>
          <i class="bk-icon icon-close" @click="shareVisible = false"></i>
          <div class="share-content" @click="shareDefect">
            <i class="codecc-icon icon-link-2"></i>
            <span>{{ $t('复制链接') }}</span>
          </div>
        </div>
      </div>
      <defect-block
        :list="list"
        :defect-index="fileIndex"
        :handle-file-list-row-click="handleFileListRowClick"
        :is-file-list-load-more="isFileListLoadMore"
        :defect-instances="isLoading ? [] : defectInstances || []"
        :trace-active-id="traceActiveId"
        :next-page-start-num="nextPageStartNum"
        :next-page-end-num="nextPageEndNum"
        @clickTrace="clickTrace"
        @scrollLoadMore="scrollLoadMore"
      >
      </defect-block>
    </div>

    <div class="col-main">
      <b
        class="filename"
        :title="currentTrace.filePath || currentFile.filePath"
      >{{
        (currentTrace.filePath || currentFile.filePath || '').split('/').pop()
      }}</b
      >
      <div
        id="codeViewerInDialog"
        :class="isFullScreen ? 'full-code-viewer' : 'un-full-code-viewer'"
        @click="handleCodeViewerInDialogClick"
      ></div>
    </div>

    <div class="col-aside">
      <div class="operate-section-defect">
        <div
          class="basic-info-defect"
          :class="{ 'full-screen-info': isFullScreen }"
          v-if="currentFile"
        >
          <div class="block" v-if="isPaas">
            <div class="item disb">
              <span>
                <span
                  v-if="currentFile.status === 1 && currentFile.mark === 1"
                  v-bk-tooltips="$t('已标记处理')"
                  class="codecc-icon icon-mark mr5"
                ></span>
                <span
                  v-if="currentFile.status === 1 && currentFile.markButNoFixed"
                  v-bk-tooltips="$t('标记处理后重新扫描仍为问题')"
                  class="codecc-icon icon-mark re-mark mr5"
                ></span>
                <span
                  v-if="
                    currentFile.defectIssueInfoVO &&
                      currentFile.defectIssueInfoVO.submitStatus &&
                      currentFile.defectIssueInfoVO.submitStatus !== 4
                  "
                  v-bk-tooltips="$t('已提单')"
                  class="codecc-icon icon-tapd"
                ></span>
              </span>
            </div>
            <div class="item">
              <bk-button
                class="item-button"
                :icon="row.processProgress === 1 ? 'check-1' : ''"
                :class="{ 'is-selected': row.processProgress === 1 }"
                @click="handleIgnoreProcess(1)"
              >
                {{ $t('已优化工具') }}
              </bk-button>
            </div>

            <div class="item">
              <bk-button
                class="item-button"
                :icon="row.processProgress === 2 ? 'check-1' : ''"
                :class="{ 'is-selected': row.processProgress === 2 }"
                @click="handleIgnoreProcess(2)"
              >
                {{ $t('非工具原因') }}
              </bk-button>
            </div>

            <div class="item">
              <bk-button
                class="item-button"
                :icon="row.processProgress === 3 ? 'check-1' : ''"
                :class="{ 'is-selected': row.processProgress === 3 }"
                @click="handleIgnoreProcess(3)"
              >
                {{ $t('其他') }}
              </bk-button>
            </div>
          </div>
          <div class="block" v-else>
            <div class="item disb">
              <span class="fail cc-status" v-if="currentFile.status === 1">
                <span class="cc-dot"></span>
                <span
                  v-if="buildNum"
                  v-bk-tooltips="
                    `#${buildNum}待修复(当前分支最新构建#${
                      lintDetail.lastBuildNumOfSameBranch
                    }该问题为${
                      lintDetail.defectIsFixedOnLastBuildNumOfSameBranch
                        ? '已修复'
                        : '待修复'
                    })`
                  "
                >
                  #{{ buildNum }}{{ $t('待修复') }}
                  <span style="color: #63656e"
                  >(#{{ lintDetail.lastBuildNumOfSameBranch
                  }}{{
                    lintDetail.defectIsFixedOnLastBuildNumOfSameBranch
                      ? $t('已修复')
                      : $t('待修复')
                  }})</span
                  >
                </span>
                <span v-else>{{ $t('待修复') }}</span>
              </span>
              <span class="success cc-status" v-else-if="currentFile.status & 2">
                <span class="cc-dot"></span>{{$t('已修复')}}
              </span>
              <span class="warn cc-status" v-else-if="currentFile.status & 4">
                <span class="cc-dot"></span>{{$t('已忽略')}}
              </span>
              <span class="warn cc-status" v-else-if="currentFile.status & 8 || currentFile.status & 16">
                <span class="cc-dot"></span>{{$t('已屏蔽')}}
              </span>
              <span>
                <span
                  v-if="currentFile.status === 1 && currentFile.mark === 1"
                  v-bk-tooltips="$t('已标记处理')"
                  class="codecc-icon icon-mark mr5"
                ></span>
                <span
                  v-if="currentFile.status === 1 && currentFile.markButNoFixed"
                  v-bk-tooltips="$t('标记处理后重新扫描仍为问题')"
                  class="codecc-icon icon-mark re-mark mr5"
                ></span>
                <span
                  v-if="
                    currentFile.defectIssueInfoVO &&
                      currentFile.defectIssueInfoVO.submitStatus &&
                      currentFile.defectIssueInfoVO.submitStatus !== 4
                  "
                  v-bk-tooltips="$t('已提单')"
                  class="codecc-icon icon-tapd"
                ></span>
              </span>
              <div v-if="isOpenIde && checkerShow()">
                <div v-if="openIdeDetail.data.length > 0 && openIdeDetail.success && !openIdeDetail.low">
                  <bk-alert class="close-ide" type="success">
                    <div slot="title">{{ $t('检测到您已安装PreCI') }}</div>
                  </bk-alert>
                  <div class="open-ide-message">
                    <div>
                      <span>
                        <a style="color: #3A84FF" href="javascript:;" @click="openPreci">PreCI IDE </a>
                        {{$t("插件修复问题更快捷。支持一键定位代码、注释忽略和个人待修复问题面板等。")}}
                      </span>
                    </div>
                    <div v-for="(item, index) in openIdeDetail.data" :key="item.ideName">
                      <button
                        v-if="item.tooltips"
                        v-bk-tooltips="item.tooltips"
                        class="open-button"
                        style="font-size: 12px;color:#3a84ff"
                        @click="openIde(index)">
                        {{$t("打开本地")}}{{ item.ideName }} PreCI
                      </button>
                      <button
                        v-else
                        class="open-button"
                        style="font-size: 12px;color:#3a84ff"
                        @click="openIde(index)">
                        {{$t("打开本地")}}{{ item.ideName }} PreCI
                      </button>
                    </div>
                  </div>
                </div>
                <div v-else-if="openIdeDetail.data.length === 0 && openIdeDetail.success">
                  <bk-alert class="close-ide" type="warning" closable @close="closeMessageFunc">
                    <div slot="title">{{ $t('本地工程未初始化PreCI') }}</div>
                  </bk-alert>
                  <div class="open-ide-message">
                    <div>
                      <span>
                        <a style="color: #3A84FF" href="javascript:;" @click="openPreci">PreCI IDE </a>
                        {{$t("插件修复问题更快捷。支持一键定位代码、注释忽略和个人待修复问题面板等。")}}
                      </span>
                    </div>
                    <div class="download" style="padding: 0 10px" @click="openInitPreCi">
                      <img :src="pushImg">
                      <a style="color: #3A84FF" href="javascript:;">
                        {{$t("本地工程如何初始化PreCI")}}
                      </a>
                    </div>
                  </div>
                </div>
                <div v-else>
                  <bk-alert class="close-ide" type="warning" closable @close="closeMessageFunc">
                    <div slot="title">{{ $t('PreCI未安装或版本过低') }}</div>
                  </bk-alert>
                  <div class="open-ide-message">
                    <div>
                      <span>
                        <a style="color: #3A84FF " href="javascript:;" @click="openPreci">PreCI IDE </a>
                        {{$t("插件修复问题更快捷。支持一键定位代码、注释忽略和个人待修复问题面板等。")}}
                      </span>
                    </div>
                    <div class="download" style="padding: 0 8px">
                      <img :src="downloadImg">
                      <a style="color: #3A84FF" href="javascript:;" @click="openJetPreci">
                        {{$t("安装")}} JetBrains {{$t("系列")}} PreCI
                      </a>
                    </div>
                    <!-- <div class="download" style="padding: 0 8px">
                      <img :src="downloadImg">
                      <a style="color: #3A84FF" href="javascript:;" @click="openVsCodePreci">
                        {{$t("安装")}} VSCode PreCI
                      </a>
                    </div> -->
                  </div>
                </div>
              </div>
            </div>
            <div v-if="currentFile.status === 1" class="item">
              <bk-button
                v-if="currentFile.mark"
                class="item-button"
                @click="handleMark(0, false, entityId)"
              >
                {{ $t('取消标记') }}
              </bk-button>
              <bk-button
                v-else
                theme="primary"
                class="item-button"
                @click="handleMark(1, false, entityId)"
              >
                {{ $t('标记处理') }}
              </bk-button>
            </div>
            <div
              v-if="currentFile.status & 4 && !currentFile.ignoreCommentDefect"
            >
              <div class="item">
                <bk-button
                  class="item-button"
                  @click="handleRevertIgnoreAndMark(entityId)"
                >
                  {{ $t('取消忽略并标记处理') }}
                </bk-button>
              </div>
              <div class="item" v-if="DEPLOY_ENV === 'tencent'">
                <bk-button
                  class="item-button"
                  @click="handleRevertIgnoreAndCommit(entityId)"
                >
                  {{ $t('取消忽略并提单') }}
                </bk-button>
              </div>
            </div>
            <div class="item">
              <bk-button
                v-if="currentFile.status & 4 && currentFile.ignoreCommentDefect"
                class="item-button"
                disabled
                :title="$t('注释忽略的问题不允许页面进行恢复操作')"
              >
                {{ $t('取消忽略') }}
              </bk-button>
              <bk-button
                v-else-if="currentFile.status & 4"
                class="item-button"
                @click="handleIgnore('RevertIgnore', false, entityId)"
              >
                {{ $t('取消忽略') }}
              </bk-button>
              <bk-button
                v-else-if="prohibitIgnore"
                disabled
                class="item-button"
                :title="
                  $t(
                    '已设置禁止页面忽略，可在代码行末或上一行使用注释忽略，例如// NOCC:rule1(ignore reason)'
                  )
                "
              >
                {{ $t('忽略问题') }}
              </bk-button>
              <bk-button
                v-else-if="
                  !(
                    currentFile.status & 2 ||
                    currentFile.status & 8 ||
                    currentFile.status & 16
                  )
                "
                class="item-button"
                @click="handleIgnore('IgnoreDefect', false, entityId)"
              >
                {{ $t('忽略问题') }}
              </bk-button>
            </div>
            <div class="item">
              <bk-button class="item-button" @click="handleComment(entityId)">{{
                $t('评论')
              }}</bk-button>
            </div>
            <div
              class="item"
              v-if="currentFile.status & 4 && !currentFile.ignoreCommentDefect"
            >
              <bk-button
                class="item-button"
                @click="handleChangeIgnoreType(currentFile, true)"
              >
                {{ $t('修改忽略类型') }}
              </bk-button>
            </div>
            <div
              v-if="
                currentFile.status === 1 &&
                  !(
                    currentFile.defectIssueInfoVO &&
                    currentFile.defectIssueInfoVO.submitStatus &&
                    currentFile.defectIssueInfoVO.submitStatus !== 4
                  )
              "
              class="item"
            >
              <bk-button
                v-if="DEPLOY_ENV === 'tencent'"
                class="item-button"
                @click="handleCommit('commit', false, entityId)"
              >{{ $t('提单') }}
              </bk-button>
            </div>
          </div>
          <div class="block">
            <div class="item">
              <dt>{{ $t('ID') }}</dt>
              <dd>{{ currentFile.id }}</dd>
            </div>
            <div class="item">
              <dt>{{ $t('级别') }}</dt>
              <dd>{{ defectSeverityMap[currentFile.severity] }}</dd>
            </div>
          </div>
          <div class="block">
            <div class="item">
              <dt>{{ $t('问题创建') }}</dt>
              <dd class="small">{{ currentFile.createTime | formatDate }} {{
                currentFile.createBuildNumber
                  ? '#' + currentFile.createBuildNumber
                  : '--'
              }}</dd>
            </div>
            <div class="item">
              <dt>
                {{ $t('处理人') }}
              </dt>
              <dd>
                {{ currentFile.author && currentFile.author.join(',') }}
                <span
                  v-if="(currentFile.status & 1 || currentFile.status & 4) && !isPaas"
                  @click.stop="handleAuthor(1, entityId, currentFile.author)"
                  class="curpt bk-icon icon-edit2 fs20"
                >
                </span>
              </dd>
            </div>
            <div class="item" v-if="currentFile.status & 2">
              <dt>{{ $t('修复时间') }}</dt>
              <dd class="small">{{ currentFile.fixedTime | formatDate }}</dd>
            </div>
            <div class="item">
              <dt>{{ $t('代码提交') }}</dt>
              <dd class="small">
                {{ currentFile.lineUpdateTime | formatDate }}
              </dd>
            </div>
            <div class="item">
              <dt>{{ $t('提交人') }}</dt>
              <dd>{{ currentFile.commitAuthor}}</dd>
            </div>
          </div>
          <div class="block" v-if="currentFile.status & 4">
            <div class="item">
              <dt>{{ $t('忽略时间') }}</dt>
              <dd class="small">{{ currentFile.ignoreTime | formatDate }}</dd>
            </div>
            <div class="item">
              <dt>{{ $t('忽略人') }}</dt>
              <dd>{{ currentFile.ignoreAuthor }}</dd>
            </div>
            <div class="item disb">
              <dt>{{ $t('忽略原因') }}</dt>
              <dd>
                {{ getIgnoreReasonByType(currentFile.ignoreReasonType) }}
                {{
                  currentFile.ignoreReason
                    ? '：' + currentFile.ignoreReason
                    : ''
                }}
              </dd>
            </div>
          </div>
          <div class="block">
            <div class="item disb">
              <dt>{{ $t('工具') }}</dt>
              <dd>
                {{ (toolMap[currentFile.toolName] || {}).displayName || '' }}
              </dd>
            </div>
            <div class="item disb">
              <dt>{{ $t('规则') }}</dt>
              <dd>{{ currentFile.checker }}</dd>
            </div>
          </div>
          <div class="block">
            <div class="item disb">
              <dt>{{ $t('代码库路径') }}</dt>
              <a target="_blank" :href="lintDetail.filePath">{{
                lintDetail.filePath
              }}</a>
            </div>
            <div class="item disb">
              <dt>{{ $t('版本号') }}</dt>
              <dd>{{ currentFile.revision }}</dd>
            </div>
          </div>
        </div>
      </div>
    </div>
    <bk-dialog
      v-model="openIdeDetail.showDialog"
      theme="primary"
      :mask-close="false"
      header-position="center"
      :show-footer="false"
      :title="$t('请确保JetBrains Toolbox已安装')"
    >
      <div>
        1、{{$t('由JetBrains官方提供的轻量App，可快速启动JetBrains系列IDE的指定工程。')}}
        <a href="https://www.jetbrains.com/toolbox-app/" target="_blank">{{ $t("前往安装") }}>></a>
      </div>
      <div style="margin-top: 10px">
        2、{{$t('更新JetBrains IDE版本到2022-1及以上。完成1后可使用Toolbox更新，更新后可做到一键打开相应工程的对应文件对应问题行，体验更佳。')}}
      </div>
      <div style="margin: 20px auto">
        <bk-button style="margin-left: 50px" :theme="'primary'" :title="$t('已全部完成')" class="mr10" @click="installDialogConfirm">
          {{ $t('已全部完成') }}
        </bk-button>
        <bk-button :theme="'default'" type="submit" :title="$t('等会再来')" @click="installDialogCancel" class="mr10">
          {{$t('等会再来')}}
        </bk-button>
      </div>
    </bk-dialog>
    <operate-dialog :visible.sync="operateDialogVisible"></operate-dialog>
    <process ref="process"></process>
  </div>
</template>

<script>
import CodeMirror from '@/common/codemirror';
import { addClass, formatDiff, getClosest, hasClass } from '@/common/util';
import { mapState } from 'vuex';
import { format } from 'date-fns';
import { bus } from '@/common/bus';
import DefectBlock from './defect-block/defect-block';
import OperateDialog from '@/components/operate-dialog';
import downloadImg from '@/images/download.png';
import pushImg from '@/images/push.svg';
import { openUrlWithInputTimeoutHack } from '@/common/open';
import marked from 'marked';
import Process from '../paas/list/process.vue';
import Vue from 'vue';
import AiSuggestion from './ai-suggestion.vue';
import CheckerDetail from './checker-detail.vue';
import DEPLOY_ENV from '@/constants/env';

export default {
  components: {
    DefectBlock,
    OperateDialog,
    Process,
  },
  props: {
    list: Array,
    isPaas: {
      type: Boolean,
      default: false,
    },
    row: {
      type: Object,
      default: () => ({}),
    },
    type: {
      type: String,
      default: 'file',
    },
    isLoading: {
      type: Boolean,
      default: false,
    },
    visible: {
      type: Boolean,
      default: false,
    },
    isFullScreen: {
      type: Boolean,
      default: false,
    },
    currentFile: {
      type: Object,
      default: {},
    },
    fileIndex: {
      type: Number,
      default: 0,
    },
    entityId: {
      type: String,
    },
    totalCount: {
      type: Number,
      default: 0,
    },
    lintDetail: {
      type: Object,
      default: {},
    },
    handleMark: {
      type: Function,
    },
    handleComment: {
      type: Function,
    },
    handleCommit: {
      type: Function,
    },
    deleteComment: {
      type: Function,
    },
    handleIgnore: {
      type: Function,
    },
    handleAuthor: {
      type: Function,
    },
    prohibitIgnore: {
      type: Boolean,
    },
    buildNum: {
      type: String,
    },
    handleRevertIgnoreAndMark: {
      type: Function,
    },
    handleRevertIgnoreAndCommit: {
      type: Function,
    },
    handleChangeIgnoreType: {
      type: Function,
    },
    isProjectDefect: {
      type: Boolean,
    },
    handleFileListRowClick: Function,
    isFileListLoadMore: Boolean,
    nextPageStartNum: Number,
    nextPageEndNum: Number,
    ignoreList: {
      type: Array,
      default: () => [],
    },
  },
  data() {
    return {
      pushImg,
      downloadImg,
      isOpenIde: true,
      openIdeDetail: {
        data: [],
        closeMessage: this.$store.state.preci.closeMessage,
        initMessage: this.$store.state.preci.initMessage,
        gitUrl: '',
        hostData: {
          osName: '',
          ip: '',
          osType: '',
        },
        closeMessageObj: {
          content: this.$t('本次不再提醒'),
          showOnInit: false,
          placements: ['top-start'],
        },
        installDialog: this.$store.state.preci.installDialog,
        showDialog: false,
        index: -1,
        preCiWebSocket: undefined,
        success: false,
        lastPreCiVersion: '',
        userVersion: '',
        low: false,
      },
      state: this.$store.state,
      toolId: this.$route.params.toolId,
      defectSeverityMap: {
        1: this.$t('严重'),
        2: this.$t('一般'),
        3: this.$t('提示'),
        4: this.$t('提示'),
      },
      defectSeverityDetailMap: {
        1: this.$t('严重'),
        2: this.$t('一般'),
        3: this.$t('提示'),
        4: this.$t('提示'),
      },
      currentDefectDetail: {
        hintId: undefined,
        eventTimes: 0,
        eventSource: undefined,
      },
      codeViewerInDialog: null,
      codeMirrorDefaultCfg: {
        lineNumbers: true,
        scrollbarStyle: 'simple',
        theme: 'summerfruit',
        lineWrapping: true,
        placeholder: this.emptyText,
        firstLineNumber: 1,
        readOnly: true,
      },
      commentList: [],
      scrollLine: 0,
      rowIndex: 0,
      shareVisible: false,
      operateDialogVisible: false,
      traceDataList: [],
      currentTrace: {},
      currentTraceIndex: 0,
      traceActiveId: '',
      mainTraceId: '',
      startLine: 1,
      fileInfoMapStorage: {},
      checkerDetailMapStorage: {},
      hideDefectDetail: true,
      aiSuggestionVisible: false,
      aiSuggestionVM: null,
      checkerDetailVisible: false,
      checkerDetailVM: null,
      isInnerSite: DEPLOY_ENV === 'tencent',
    };
  },
  computed: {
    ...mapState('tool', {
      toolMap: 'mapList',
    }),
    defectInstances() {
      const { defectInstances = [] } = this.currentFile;
      const traceDataList = [];
      const handleLinkTrace = function (linkTrace = [], id, index) {
        linkTrace.forEach((link, linkIndex) => {
          const linkId = `${id}-${linkIndex}`;
          const newIndex = `${index}.${link.traceNum}`;
          traceDataList.push(link);
          link.id = linkId;
          link.index = newIndex;
          handleLinkTrace(link.linkTrace, linkId, newIndex);
        });
      };
      defectInstances.forEach((instance, instanceIndex) => {
        instance.traces.forEach((trace, traceIndex) => {
          const id = `${instanceIndex}-${traceIndex}`;
          traceDataList.push(trace);
          trace.id = id;
          trace.index = trace.traceNum;
          trace.expanded = !!trace.main;
          trace.linkTrace
            && handleLinkTrace(trace.linkTrace, id, trace.traceNum);
        });
      });
      this.traceDataList = traceDataList;
      return defectInstances;
    },
  },
  watch: {
    currentDefectDetail: {
      handler() {
        this.emptyText = this.$t('未选择文件');
        // 把main trace设为当前trace
        const tracesList = (this.defectInstances[0]
          && this.defectInstances[0].traces) || [{}];
        this.currentTrace = tracesList.find(item => item.main)
          || tracesList[tracesList.length - 1];
        this.traceActiveId = this.currentTrace.id;
        this.mainTraceId = this.currentTrace.id;
        this.currentTraceIndex = this.traceDataList.findIndex(item => item.id === this.currentTrace.id);
        this.updateCodeViewer();
        this.aiSuggestionVM?.$destroy();
        this.aiSuggestionVM = null;
        this.aiSuggestionVisible = false;
        this.checkerDetailVM?.$destroy();
        this.checkerDetailVM = null;
        this.checkerDetailVisible = false;
        // this.locateHint()
      },
      deep: true,
    },
    visible(val) {
      if (!val) {
        this.codeViewerInDialog.setValue('');
        this.codeViewerInDialog.setOption('firstLineNumber', 1);
      }
    },
    currentFile() {
      this.currentTrace = {};
    },
    aiSuggestionVisible() {
      setTimeout(() => {
        this.codeViewerInDialog?.refresh();
      }, 600);
    },
    checkerDetailVisible() {
      setTimeout(() => {
        this.codeViewerInDialog?.refresh();
      }, 600);
    },
  },
  async created() {
    if (this.isInnerSite) {
      await this.getTaskOpenIdeConfig();
      if (this.isOpenIde) {
        this.linkPreCiWebSocket();
      }
    }
  },
  methods: {
    handleIgnoreProcess(id) {
      this.$refs.process.handleDefect(id, this.row);
    },
    async checkerVersion() {
      await this.getPreCiLastVersion();
      await this.getUserVersion();
      const useVersionInt = this.versionToInt(this.openIdeDetail.userVersion);
      const lastVersionInt = this.versionToInt(this.openIdeDetail.lastPreCiVersion);
      if (useVersionInt < lastVersionInt) {
        this.openIdeDetail.low = true;
      }
    },
    versionToInt(version) {
      if (!version) {
        return 0;
      }
      const re = /\d/;
      let data = '';
      for (let x = 0; x < version.length; x++) {
        if (re.test(version.charAt(x))) {
          data += version.charAt(x);
        }
      }
      return parseInt(data, 10);
    },
    async getUserVersion() {
      const body = {
        gitUrl: this.openIdeDetail.gitUrl,
        user: this.state.user.username,
      };
      const data = await this.$store.dispatch('preci/getUserUseVersion', body);
      if (data) {
        this.openIdeDetail.userVersion = data;
      }
    },
    openJetPreci() {
      window.open(window.DOWNLOAD_JD_PRECI_URL, '_blank');
    },
    openVsCodePreci() {
      window.open(window.DOWNLOAD_VS_PRECI_URL, '_blank');
    },
    openPreci() {
      window.open(window.PRECI_URL, '_blank');
    },
    openInitPreCi() {
      window.open(window.PRECI_INIT_PROJECT_URL, '_blank');
    },
    checkerShow() {
      const status = this.currentFile.status & 2;
      if (status) {
        return false;
      }
      const { lintDefectDetailVO } = this.lintDetail;
      const { username } = this.$store.state.user;
      return Boolean(lintDefectDetailVO?.author?.includes(username));
    },
    async getGitUrl() {
      let gitUrl = this.$store.state.task.detail.codeLibraryInfo.codeInfo.length > 0
        ? this.$store.state.task.detail.codeLibraryInfo.codeInfo[0].url : '';
      if (!gitUrl && gitUrl !== '') {
        const projCode = this.$route.params.projectId;
        const repoList = await this.$store.dispatch('task/getRepoList', { projCode });
        gitUrl = this.$store.state.task.detail.codeLibraryInfo.codeInfo[0].aliasName;
        for (const repo of repoList) {
          if (repo.aliasName === gitUrl) {
            gitUrl = repo.url;
            break;
          }
        }
      }
      this.openIdeDetail.gitUrl = gitUrl;
    },
    async getPreCiLastVersion() {
      const data = await this.$store.dispatch('preci/getPreCiLastVersion');
      if (data.data) {
        this.openIdeDetail.lastPreCiVersion = data.data.version;
      }
    },
    async getTaskOpenIdeConfig() {
      const { taskId } = this.$route.params;
      if (!taskId ||  taskId === 0) {
        this.isOpenIde = false;
        console.log('task id is null');
        return;
      }
      const data = await this.$store.dispatch('task/getTaskOpenIdeConfig');
      this.isOpenIde = data.data;
    },
    async getOpenIdeDetail() {
      await this.getGitUrl();
      this.checkerVersion();
      const data = {
        gitUrl: this.openIdeDetail.gitUrl,
        userName: this.state.user.username,
        ideType: 'JetBrains',
        osType: this.openIdeDetail.hostData.osType,
        hostIp: this.openIdeDetail.hostData.ip,
        hostName: this.openIdeDetail.hostData.osName,
      };
      this.openIdeDetail.data = [];
      const ideData = await this.$store.dispatch('task/getOpenIdeDetailData', data);
      const names = [];
      for (let x = 0; x < ideData.length; x++) {
        const ide = ideData[x];
        if (names.indexOf(ide.ideName) === -1) {
          names.push(ide.ideName);
          this.openIdeDetail.data.push(ide);
        } else {
          for (let y = 0; y < this.openIdeDetail.data.length; y++) {
            const saveData = this.openIdeDetail.data[y];
            if (saveData.hostName !== ide.hostName || saveData.projectName !== ide.projectName) {
              const saveIde = {
                ...ide,
                tooltips: {
                  content: `${saveData.hostName}机器${saveData.projectName}项目`,
                  showOnInit: false,
                  placements: ['top-start'],
                },
              };
              this.openIdeDetail.data.push(saveIde);
            }
          }
        }
      }
    },
    closeMessageFunc() {
      this.openIdeDetail.closeMessage = true;
      this.$store.state.preci.closeMessage = true;
    },
    closeInitMessageFunc() {
      this.openIdeDetail.initMessage = true;
      this.$store.state.preci.initMessage = true;
    },
    linkPreCiWebSocket() {
      if (typeof WebSocket === 'undefined') {
        return;
      }
      this.openIdeDetail.preCiWebSocket = new WebSocket('ws://127.0.0.1:22101/websocket');
      this.openIdeDetail.preCiWebSocket.onopen = () => {
        console.log('preCi webSocket 链接成功');
        this.openIdeDetail.success = true;
        this.openIdeDetail.preCiWebSocket.send('{"apiName": "GET_HOST_INFO"}');
      };
      this.openIdeDetail.preCiWebSocket.onmessage = this.preCiWebSocketOnMessage;
      this.openIdeDetail.preCiWebSocket.onerror = (e) => {
        console.log('preCi webSocket 链接失败');
        this.getOpenIdeDetail();
      };
    },
    preCiWebSocketOnMessage(e) {
      const data = JSON.parse(e.data);
      this.openIdeDetail.hostData.osName = data.hostName;
      this.openIdeDetail.hostData.ip = data.hostIp;
      this.openIdeDetail.hostData.osType = data.osType;
      this.getOpenIdeDetail();
    },
    getRelativeFilePath(filePath, ind) {
      let path = filePath.replaceAll('\\', '/');
      if (path.startsWith('/')) {
        path = path.substring(1, path.length);
      }
      const pathSplit = path.split('/');
      const projectPath = this.openIdeDetail.data[ind].projectPath.replaceAll('\\', '/');
      const projectPathSplit = projectPath.split('/');
      let index = 0;
      let projectIndex = 0;
      for (let x = 0; x < projectPathSplit.length; x++) {
        if (pathSplit[index] === projectPathSplit[x]) {
          projectIndex = x;
          break;
        }
      }
      if (projectIndex === 0) {
        return path;
      }
      for (index; index < pathSplit.length; index++) {
        if (pathSplit[index] === projectPathSplit[projectIndex]) {
          projectIndex += 1;
        }
        if (projectIndex === projectPathSplit.length) {
          index += 1;
          break;
        }
      }
      if (index === pathSplit.length - 1) {
        return path;
      }
      const relative = [];
      for (index; index < pathSplit.length; index++) {
        relative.push(pathSplit[index]);
      }
      return relative.join('/');
    },
    installDialogConfirm() {
      this.getOpenIdeUrl(this.openIdeDetail.index);
    },
    installDialogCancel() {
      this.openIdeDetail.showDialog = false;
    },
    openIde(index) {
      // 表示并没有弹出过dialog提示框，首次需要弹出一次
      if (!this.$store.state.preci.installDialog) {
        this.openIdeDetail.index = index;
        this.openIdeDetail.showDialog = true;
        this.$store.state.preci.installDialog = true;
        return;
      }
      this.getOpenIdeUrl(index);
    },
    getOpenIdeUrl(index) {
      this.openIdeDetail.showDialog = false;
      let ide = '';
      switch (this.openIdeDetail.data[index].ideName) {
        case 'IntelliJ IDEA':
          ide = 'idea';
          break;
        case 'AppCode':
          ide = 'appcode';
          break;
        case 'CLion':
          ide = 'clion';
          break;
        case 'PyCharm':
          ide = 'pycharm';
          break;
        case 'PhpStorm':
          ide = 'php-storm';
          break;
        case 'RubyMine':
          ide = 'rubymine';
          break;
        case 'WebStorm':
          ide = 'web-storm';
          break;
        case 'Rider':
          ide = 'rd';
          break;
        case 'GoLand':
          ide = 'goland';
          break;
        default:
          break;
      }
      const path = this.getRelativeFilePath(this.lintDetail.lintDefectDetailVO.relPath, index);
      const line = this.lintDetail.lintDefectDetailVO.lineNum;
      // eslint-disable-next-line vue/max-len
      let url = `jetbrains://${ide}/preci/reference?project=${this.openIdeDetail.data[index].projectName}&path=${path}:${line}:0`;
      if (this.lintDetail.lintDefectDetailVO.author.indexOf(this.$store.state.user.username) !== -1) {
        const { projectId, taskId } = this.$route.params;
        let type = '';
        const entries = Object.entries(this.$store.state.tool.mapList);
        for (let x = 0; x < entries.length; x++) {
          if (this.lintDetail.lintDefectDetailVO.toolName === entries[x][0]) {
            type = entries[x][1].type;
          }
        }
        // eslint-disable-next-line vue/max-len
        url += `&projectId=${projectId}&taskId=${taskId}&checker=${this.lintDetail.lintDefectDetailVO.checker}&dimension=${type}`;
      }
      console.log(`open ide url : ${url}`);
      openUrlWithInputTimeoutHack(url, () => {
        this.openIdeDetail.showDialog = true;
      }, () => {});
    },
    setFullScreen() {
      this.$emit('update:is-full-screen', !this.isFullScreen);
    },
    handleCodeFullScreen() {
      if (!this.codeViewerInDialog) {
        const codeMirrorConfig = {
          ...this.codeMirrorDefaultCfg,
          ...{ autoRefresh: true },
        };
        this.codeViewerInDialog = CodeMirror(document.getElementById('codeViewerInDialog'), codeMirrorConfig);

        this.codeViewerInDialog.on('update', () => {});
        if (!window.localStorage.getItem('opreate-keyboard-20220411')) {
          this.operateDialogVisible = true;
        }
      }
      this.currentDefectDetail.eventTimes += 1;

      // this.updateCodeViewer();
    },
    // 代码展示相关
    async updateCodeViewer() {
      const codeViewer = this.codeViewerInDialog;
      const { fileName, trimBeginLine } = this.lintDetail;
      const { fileInfoMap, checker, toolName } = this.currentFile;
      let fileMd5 = this.currentTrace.fileMd5 || this.currentFile.fileMd5;
      if (!fileMd5) {
        const tracesList = (this.defectInstances[0]
          && this.defectInstances[0].traces) || [{}];
        this.currentTrace = tracesList.find(item => item.main)
          || tracesList[tracesList.length - 1];
        this.traceActiveId = this.currentTrace.id;
        this.mainTraceId = this.currentTrace.id;
        this.currentTraceIndex = this.traceDataList.findIndex(item => item.id === this.currentTrace.id);
        // eslint-disable-next-line prefer-destructuring
        fileMd5 = this.currentTrace.fileMd5;
      }
      if (!fileInfoMap[fileMd5]) return;
      const { startLine, contents } = fileInfoMap[fileMd5];
      if (this.isGetFileInfo) return;
      const toolList = ['COVERITY', 'KLOCWORK', 'PINPOINT'];
      if (!toolList.includes(toolName) && (!this.fileInfoMapStorage[fileMd5] || startLine !== 1)) {
        this.isGetFileInfo = true;
        const content = await this.getFileContentSegment();
        this.fileInfoMapStorage[fileMd5] = content;
        this.isGetFileInfo = false;
      }
      const checkerKey = `${toolName}-${checker}`;
      if (this.isGetCheckerDetail) return;
      if (!this.checkerDetailMapStorage[checkerKey]) {
        this.isGetCheckerDetail = true;
        const checkerDetail = await this.getCheckerDetail();
        this.checkerDetailMapStorage[checkerKey] = checkerDetail;
        this.isGetCheckerDetail = false;
      }
      const fileContent = toolList.includes(toolName) ? contents : this.fileInfoMapStorage[fileMd5]?.fileContent;
      // const { fileContent } = this.fileInfoMapStorage[fileMd5];
      this.currentFile.startLine = startLine;
      const codeMirrorMode = CodeMirror.findModeByFileName(fileName);
      const mode = codeMirrorMode && codeMirrorMode.mode;
      import(`codemirror/mode/${mode}/${mode}.js`)
        .then((m) => {
          codeViewer.setOption('mode', mode);
        })
        .finally(() => {
          codeViewer.setOption(
            'firstLineNumber',
            startLine !== 0 ? startLine : 1,
          );
          if (!fileContent) {
            this.emptyText = this.$t('文件内容为空');
            codeViewer.setValue(this.emptyText);
          } else {
            codeViewer.setValue(fileContent);
          }
          // 创建问题提示块
          this.buildLintHints(codeViewer);
          codeViewer.refresh();
          this.locateHint();
        });
    },
    // 创建问题提示块
    buildLintHints(codeViewer) {
      const {
        defectSeverityDetailMap,
        currentFile: { startLine, checker, toolName },
      } = this;
      const { lintDefectDetailVO: detailVO } = this.lintDetail;
      let defectList = null;
      // 有defectInstances的情况
      if (this.defectInstances.length) {
        const { id } = this.currentTrace;
        const idList = id.split('-');
        defectList = this.defectInstances[idList[0]].traces;
        if (idList.length > 2) {
          for (let i = 1; i < idList.length - 1; i++) {
            defectList = defectList[idList[i]].linkTrace;
          }
        }
      } else {
        defectList = [detailVO];
      }
      defectList.forEach((defect) => {
        if (
          this.currentTrace.fileMd5
          && this.currentTrace.fileMd5 !== defect.fileMd5
        ) return;
        let checkerComment = '';
        const hints = document.createElement('div');
        const { index, lineNum, message } = defect;
        const messageDom = document.createElement('span');
        messageDom.style.width = '100%';
        const newMessage = `${index ? `${index}.` : ''}${message}`;
        messageDom.innerHTML = toolName === 'WECHECK'
          ? `<pre style="margin: 0;overflow-y: auto">${newMessage}</pre>`
          : marked.parse(newMessage);
        const hintId = `${lineNum}-${0}`;
        let mainClass = '';
        const hasRedPoint = !localStorage.getItem('hasRedPoint');
        if (this.mainTraceId === defect.id || !this.defectInstances.length) {
          mainClass = 'main';
          // 评论
          if (detailVO.codeComment) {
            for (const comment of detailVO.codeComment.commentList) {
              checkerComment += `<p class="comment-item">
                <span class="info">
                  <i class="codecc-icon icon-commenter"></i>
                  <span>${comment.userName}</span>
                  <span title="${comment.comment}">
                    ${comment.comment}
                  </span>
                </span>
                <span class="handle">
                  <span>${this.formatTime(comment.commentTime)}</span>
                  <i class="bk-icon icon-delete"
                    data-singlecommentid="comment-${comment.singleCommentId}"
                    data-commentid="comment-${detailVO.codeComment.entityId}"
                    data-comment="${comment.comment}"
                    data-entityid="${detailVO.entityId}"
                  ></i>
                </span>
              </p>`;
            }
          }
          hints.innerHTML = `
            <div class="lint-info">
                <div class="lint-info-main">
                    <i class="lint-icon bk-icon icon-right-shape toggle-checker-detail curpt"></i>
                    <div class="lint-head">
                        ${messageDom.outerHTML}
                    </div>
                    <p class="tag-line">
                        <span class="tag">
                            <span class="bk-icon icon-exclamation-circle-shape type-${
  detailVO.severity
}"></span>
                            ${detailVO.checker} ${
  detailVO.checkerType ? `| ${detailVO.checkerType}` : ''
}
                            | ${defectSeverityDetailMap[detailVO.severity]}
                        </span>
                        ${this.isPaas || DEPLOY_ENV !== 'tencent'
    ? `
    <span>
      <span class="toggle-checker-detail cc-link-primary">${this.$t('规则详情')}</span>
    </span>`
    : `
    <span>
      <span class="toggle-checker-detail cc-link-primary pr-8 border-right">${this.$t('规则详情')}</span>
      <span class="ai-suggestion cc-link-primary pl-8">
        ${this.$t('AI 修复建议')}
        ${hasRedPoint ? '<span class="red-point"></span>' : ''}
      </span>
    </span>`
}
                        
                    </p>
                </div>
                <div id="checker-detail" class="checker-detail"></div>
                <div id="ai-suggestion" class="ai-suggestion-wrapper"></div>
                ${
  checkerComment
    ? `<div class="checker-comment">${checkerComment}</div>`
    : ''
}
            </div>`;
        } else {
          hints.innerHTML = `
            <div class="lint-info">
                <div class="lint-info-main">
                    <div class="lint-head">
                        ${messageDom.outerHTML}
                    </div>
                </div>
            </div>`;
        }

        let activeClass = '';
        if (
          (!this.defectInstances.length
            || this.currentTrace.id === defect.id)
          && !this.hideDefectDetail
        ) {
          activeClass = 'active';
        }
        hints.className = `lint-hints lint-hints-${hintId} ${activeClass}`;
        hints.dataset.hintId = hintId;

        // const startLine = trimBeginLine === 0 ? 1 : trimBeginLine
        codeViewer.addLineWidget(lineNum - startLine, hints, {
          coverGutter: false,
          noHScroll: false,
          above: true,
        });
        codeViewer.addLineClass(
          lineNum - startLine,
          'wrap',
          `lint-hints-wrap ${mainClass} ${activeClass}`,
        );
      });
      this.scrollIntoView();
      bus.$emit('hide-app-loading');
      // codeViewer.refresh()
    },
    handleCodeViewerInDialogClick(event, eventSource) {
      this.codeViewerClick(event, 'dialog-code');
    },
    codeViewerClick(event, eventSource) {
      const lintHints = getClosest(event.target, '.lint-hints');
      const lintInfo = getClosest(event.target, '.toggle-checker-detail');
      const headHandle = getClosest(event.target, '.btn');
      const editAuthor = getClosest(event.target, '.icon-edit2');
      // const commentCon = getClosest(event.target, '.checker-comment')
      const delHandle = getClosest(event.target, '.icon-delete');
      const checkerDetail = getClosest(event.target, '.checker-detail');
      const suggestion = getClosest(event.target, '.ai-suggestion');

      // AI修复建议
      if (suggestion) {
        this.getSuggestion();
      }

      if (lintHints) {
        const { hintId } = lintHints.dataset;
        this.scrollLine = Number(hintId.split('-')[0]);
        this.rowIndex = Number(hintId.split('-')[1]);
      }

      // 如果点击的是标记/忽略/评论按钮
      if (headHandle) {
        const type = headHandle.getAttribute('data-option');
        const entityId = headHandle.dataset.entityid;
        if (type === 'comment') {
          const commentId = headHandle.getAttribute('data-commentId');
          this.handleComment(entityId, commentId);
        } else if (type === 'mark') {
          let { mark } = headHandle.dataset;
          mark = mark === '0' || mark === 'undefined' ? 1 : 0;
          this.handleMark(mark, false, entityId);
        } else if (type === 'ignore' && !this.prohibitIgnore) {
          this.handleIgnore('IgnoreDefect', false, entityId);
        }
        return;
      }
      if (editAuthor) {
        const { author } = editAuthor.dataset;
        const entityId = editAuthor.dataset.entityid;
        this.handleAuthor(1, entityId, author);
        return;
      }
      // 如果点击的是删除评论
      if (delHandle) {
        const that = this;
        this.$bkInfo({
          title: this.$t('删除评论'),
          subTitle: this.$t('确定要删除该条评论吗？'),
          maskClose: true,
          confirmFn() {
            const delSingleObj = delHandle.getAttribute('data-singlecommentid');
            const delCommentObj = delHandle.getAttribute('data-commentid');
            const commentStr = delHandle.getAttribute('data-comment');
            const defectEntityId = delHandle.getAttribute('data-entityid');
            const singleCommentId = delSingleObj.split('-').pop();
            const commentId = delCommentObj.split('-').pop();
            that.deleteComment(
              commentId,
              singleCommentId,
              defectEntityId,
              commentStr,
            );
          },
        });
        return;
      }
      // 如果点击的是规则详情，不执行操作
      if (checkerDetail) {
        return;
      }
      // 点击规则详情
      if (lintInfo) {
        this.toggleCheckerDetail();
        lintHints.classList.toggle('active', this.checkerDetailVisible);
      }
      // 如果点击的是lint问题区域
      // if (lintInfo) {
      //   // 触发watch
      //   this.currentDefectDetail.hintId = lintHints.dataset.hintId;
      //   this.currentDefectDetail.eventSource = eventSource;
      //   this.currentDefectDetail.eventTimes += 1;
      //   this.hideDefectDetail = hasClass(lintHints, 'active');
      //   this.handleCodeFullScreen();
      // }
    },
    handleDefectListRowInDialogClick(row, event, column) {
      // if (!this.lintDetail.fileContent) return

      // 代码所在行
      const lineNum = row.lineNum - 1;

      // 得到表格行索引
      const rowIndex = event ? getClosest(event.target, 'tr').rowIndex : 0;
      this.rowIndex = rowIndex;

      // 记录当前问题id
      const hintId = `${lineNum}-${rowIndex}`;

      // 触发watch
      this.currentDefectDetail.hintId = hintId;
      this.currentDefectDetail.eventSource = 'dialog-row';
      this.currentDefectDetail.eventTimes += 1;
      this.locateHint();
      this.handleCodeFullScreen();
    },
    locateHint() {
      const eventFrom = this.currentDefectDetail.eventSource
        ?.split('-')
        .shift();
      // 默认处理页面中的代码展示
      this.locateHintByName(eventFrom);
    },
    locateHintByName(name, visibleToggle) {
      const { hintId, eventSource } = this.currentDefectDetail;

      // 确实存在未点击问题直接打开全屏的情况，这种情况没有hintId
      if (!hintId) {
        return;
      }

      const [lineNum, rowIndex] = hintId.split('-');
      const eventTrigger = eventSource.split('-').pop();
      const codeViewer = name === 'main' ? this.codeViewer : this.codeViewerInDialog;
      const lintWrapper = codeViewer.getWrapperElement();

      if (
        eventTrigger === 'row'
        || eventTrigger === 'code'
        || visibleToggle === true
      ) {
        // 滚动到问题代码位置
        setTimeout(this.scrollTrace, 10);
      }

      // 问题代码区域高亮
      const lintHints = lintWrapper.getElementsByClassName(`lint-hints-${hintId}`);
      // this.activeLintHint(lintHints[0])
    },
    activeLintHint(lintHint) {
      if (!lintHint) return;
      // 切换所有lint wrap的active
      const lintHintsWrap = getClosest(lintHint, '.lint-hints-wrap');
      const isActive = lintHint.classList.contains('active');
      document
        .querySelectorAll('.active')
        .forEach(el => el.classList.remove('active'));
      if (!isActive) {
        addClass(lintHint, 'active');
        addClass(lintHintsWrap, 'active');
      }
    },
    locateFirst() {
      this.$nextTick(() => {
        this.handleDefectListRowInDialogClick(this.lintDetail.lintDefectDetailVO);
      });
    },
    getIgnoreReasonByType(type) {
      const typeMap = this.ignoreList.reduce((result, item) => {
        result[item.ignoreTypeId] = item.name;
        return result;
      }, {});
      return typeMap[type];
    },
    formatTime(time) {
      return formatDiff(time);
    },
    formatDate(date) {
      return date ? format(date, 'yyyy-MM-dd') : '--';
    },
    scrollLoadMore() {
      this.$emit('scrollLoadMore');
    },
    closeDetail() {
      this.$emit('closeDetail');
    },
    clickTrace(trace) {
      const { fileMd5, lineNum, id } = this.currentTrace || {};
      this.currentTraceIndex = this.traceDataList.findIndex(item => item.id === trace.id);
      this.currentTrace = trace;
      this.traceActiveId = trace.id;
      this.defectInstances.forEach((instance) => {
        instance.traces.forEach((item) => {
          if (item.id === trace.id.split('-').slice(0, 2)
            .join('-')) {
            item.expanded = true;
          }
        });
      });
      if (
        fileMd5 !== trace.fileMd5
        || id.length !== trace.id.length
        || id.split('-').shift() !== trace.id.split('-').shift()
      ) {
        this.updateCodeViewer();
        this.preLineNum = undefined;
      } else {
        this.preLineNum = lineNum;
      }
      this.scrollTrace();
    },
    // 问题上下文
    scrollTrace() {
      const { lineNum, startColumn = 0, endColumn = 0 } = this.defectInstances.length
        ? this.currentTrace
        : this.currentFile;
      const { startLine = 1 } = this.currentFile;
      const codeViewer = this.codeViewerInDialog;
      if (!codeViewer || !lineNum) return false;
      if (this.preLineNum) {
        codeViewer.removeLineClass(
          this.preLineNum - startLine,
          'wrap',
          'defect-trace',
        );
      }
      this.codeViewerMark?.clear();
      this.codeViewerMark = codeViewer.markText(
        { line: lineNum - startLine, ch: startColumn },
        { line: lineNum - startLine, ch: endColumn },
        {
          className: 'lint-hints-mark',
          atomic: true,
          inclusiveLeft: true,
          inclusiveRight: true,
        },
      );
      codeViewer.addLineClass(lineNum - startLine, 'wrap', 'defect-trace');
      this.scrollIntoView(lineNum);
    },
    // 默认滚动到问题位置
    scrollIntoView(number) {
      const codeViewer = this.codeViewerInDialog;
      if (!codeViewer || !codeViewer.getScrollerElement()) return false;
      const { startLine = 0 } = this.currentFile;
      const middleHeight = codeViewer.getScrollerElement().offsetHeight / 2;
      const lineHeight = codeViewer.defaultTextHeight();
      const { lineNum = number } = this.defectInstances.length
        ? this.currentTrace
        : this.currentFile;
      if (!number) {
        codeViewer.removeLineClass(lineNum - startLine, 'wrap', 'defect-trace');
      }
      setTimeout(() => {
        codeViewer.scrollIntoView(
          { line: lineNum - startLine, ch: 0 },
          middleHeight - lineHeight,
        );
        // bus.$emit('hide-app-loading')
        this.detailLoading = false;
      }, 1);
    },
    traceUp() {
      if (this.currentTraceIndex > 0) {
        this.currentTraceIndex -= 1;
        const trace = this.traceDataList[this.currentTraceIndex];
        this.clickTrace(trace);
      }
    },
    traceDown() {
      if (this.currentTraceIndex < this.traceDataList.length - 1) {
        this.currentTraceIndex += 1;
        const trace = this.traceDataList[this.currentTraceIndex];
        this.clickTrace(trace);
      }
    },
    /**
     * 分享问题链接
     */
    shareDefect() {
      const { projectId, taskId } = this.$route.params;
      const { toolName, entityId, defectId, status } = this.currentFile;
      let prefix = `${location.host}`;
      if (window.self !== window.top) {
        prefix = `${window.DEVOPS_SITE_URL}/console`;
      }
      let url = `${prefix}/codecc/${projectId}/task/${taskId}/defect/lint/${toolName}/list
?entityId=${entityId}&status=${status}`;
      if (this.isProjectDefect) {
        url = `${prefix}/codecc/${projectId}/defect/list
?entityId=${entityId}&status=${status}`;
      }
      if (this.isPaas) {
        url = `${prefix}/paas/ignored/${toolName}/list?entityId=${defectId}`;
      }
      const input = document.createElement('input');
      document.body.appendChild(input);
      input.setAttribute('value', url);
      input.select();
      document.execCommand('copy');
      document.body.removeChild(input);
      this.$bkMessage({
        theme: 'success',
        message: this.$t('链接已复制到粘贴板'),
      });
    },

    /**
     * 获取文件内容
     */
    async getFileContentSegment() {
      const filePath = this.currentTrace?.filePath || this.currentFile?.filePath;
      const { toolName, entityId } = this.currentFile;
      const url = this.isPaas ? 'paas/fileContentSegment' : 'defect/fileContentSegment';
      return await this.$store.dispatch(
        url,
        { toolName, entityId, filePath },
      );
    },

    /**
     * 获取AI推荐详情
     */
    async getSuggestion() {
      localStorage.setItem('hasRedPoint', true);
      const { currentFile, aiSuggestionVisible } = this;
      this.aiSuggestionVisible = !aiSuggestionVisible;

      if (!this.aiSuggestionVM) {
        this.aiSuggestionVM = new Vue({
          el: '#ai-suggestion',
          components: { AiSuggestion },
          data: {
            currentFile,
            closeAiSuggestion: this.closeAiSuggestion,
          },
          store: this.$store,
          i18n: this.$i18n,
          router: this.$router,
          template: '<AiSuggestion :current-file="currentFile" :close-ai-suggestion="closeAiSuggestion"></AiSuggestion>',
        });
      } else {
        this.aiSuggestionVM.$children[0].isShow = !aiSuggestionVisible;
      }
    },

    closeAiSuggestion() {
      this.aiSuggestionVisible = false;
    },

    /**
     * 获取规则详情
     */
    async getCheckerDetail() {
      const { toolName, checker } = this.currentFile;
      return await this.$store
        .dispatch('defect/getWarnContent', {
          toolName,
          checkerKey: checker,
        });
    },

    // 规则详情展开收起
    toggleCheckerDetail() {
      const { checkerDetailVisible } = this;
      this.checkerDetailVisible = !checkerDetailVisible;

      if (!this.checkerDetailVM) {
        this.checkerDetailVM = new Vue({
          el: '#checker-detail',
          components: { CheckerDetail },
          data: {
            checkerDetail: this.checkerDetailMapStorage[`${this.currentFile.toolName}-${this.currentFile.checker}`],
          },
          i18n: this.$i18n,
          template: '<CheckerDetail :checker-detail="checkerDetail"></CheckerDetail>',
        });
      } else {
        this.checkerDetailVM.$children[0].isShow = !checkerDetailVisible;
      }
    },
  },
};
</script>

<style lang="postcss" scoped>
@import url('../../css/mixins.css');

.open-message {
  width: 200px;
  height: 32px;
  font-size: 12px;
  line-height: 32px;
  text-align: center;
  border-radius: 2px;
}

.open-message-bg-lv {
  background-color: #F2FFF4;
  border: 1px solid #b3ffc1;
}

.open-message-bg-ch {
  background-color: #FFF4E2;
  border: 1px solid #FFDFAC;
}

.open-ide-message {
  width: 200px;
  font-size: 14px;
  line-height: 20px;
  background-color: #F0F1F5;
}

.open-ide-message div {
  padding: 10px;
}

.download {
  font-size: 12px;
  line-height: 20px;
  color: #3A84FF;
}

.download img {
  position: relative;
  top: 3px;
  width: 16px;
  height: 13.09px;
}

.open-button {
  display: block;
  width: auto;
  height: auto;
  margin: 0 auto;
  font-size: 12px;
  line-height: 18px;
  color: #3A84FF;
  text-align: center;
  background: #FFF;
  border: 1px solid #3A84FF;
  border-radius: 2px;
}

.close-ide:hover {
  cursor: pointer;
}

.download:hover {
  cursor: pointer;
}

.code-fullscreen {
  display: flex;

  .toggle-full-icon {
    position: absolute;
    top: -22px;
    right: 14px;
    color: #979ba5;
    cursor: pointer;

    &.icon-un-full-screen {
      top: 8px;
    }
  }

  .col-main {
    flex: 1;
    max-width: calc(100% - 585px);
    margin-top: 47px;
  }

  .col-aside {
    width: 240px;
    padding: 12px 20px;
    margin-top: 47px;
    background: #f5f7fa;
    flex: none;

    .operate-section {
      height: calc(100vh - 270px);

      &.full-operate-section {
        height: calc(100vh - 170px);
      }
    }
  }

  .col-aside-left {
    flex: none;
    width: 336px;
    background: #f5f7fa;

    .col-aside-left-header {
      height: 48px;
      border-bottom: 1px solid #dcdee5;

      .arrows-left {
        position: absolute;
        z-index: 9999;
        font-size: 36px !important;
        line-height: 48px;
        cursor: pointer;
      }

      .header-index {
        display: inline-block;
        width: 100%;
        font-weight: bold;
        line-height: 48px;
        text-align: center;
      }
    }
  }

  .filename {
    position: absolute;
    top: 0;
    padding-left: 24px;
    font-size: 16px;
    line-height: 48px;
  }

  .file-bar {
    height: 42px;

    .filemeta {
      display: inline-block;
      padding-left: 8px;
      margin-top: -2px;
      font-size: 12px;
      border-left: 4px solid #3a84ff;

      .filename {
        font-size: 16px;
      }

      .filepath {
        display: inline-block;
        width: 700px;
        margin-left: 8px;
        line-height: 24px;
        vertical-align: bottom;

        @mixin ellipsis;
      }
    }
  }

  .operate-section,
  .basic-info {
    .title {
      font-size: 14px;
      color: #313238;
    }
  }

  .basic-info {
    .item {
      display: flex;
      padding-bottom: 9px;

      dt {
        width: 90px;
        flex: none;
      }

      dd {
        flex: 1;
        color: #313238;
      }
    }
  }

  .operate-section-defect {
    height: 100%;
  }

  .basic-info-defect {
    /* height: calc(100% - 60px); */
    max-height: calc(100vh - 240px);
    padding-right: 20px;
    margin-right: -29px;
    overflow-y: scroll;

    &.full-screen-info {
      max-height: calc(100vh - 71px);
    }

    .title {
      font-size: 14px;
      color: #313238;
    }

    .block {
      padding: 5px 0;
      border-bottom: 1px dashed #c4c6cc;

      &:last-of-type {
        padding-bottom: 20px;
        border-bottom: 0;
      }

      .item {
        display: flex;
        padding: 5px 0;

        dt {
          width: 90px;
          flex: none;
        }

        dd {
          /* flex: 1; */
          color: #313238;
          word-break: break-all;

          &.small {
            width: 80px;
          }
        }

        a {
          color: #313238;
          word-break: break-all;
        }

        .item-button {
          width: 200px;
        }
      }
    }
  }
}

#codeViewerInDialog {
  width: 100%;
  font-size: 14px;
  border: 1px solid #eee;
  border-right: 0;
  border-left: 0;
}

.un-full-code-viewer {
  height: calc(100vh - 200px);
}

.full-code-viewer {
  height: calc(100vh - 47px);
}

>>> .icon-mark {
  color: #53cad1;

  &.re-mark {
    color: #facc48;
  }

  &.un-mark {
    color: #b0b0b0;
  }
}

>>> .icon-marked {
  font-size: 18px;
  color: #53cad1;

  &.re-marked {
    color: #facc48;
  }
}

.cc-status {
  width: 60px;
  padding-right: 8px;
}

.cc-mark {
  width: 114px;
  height: 23px;
  padding: 0 8px;
  margin-top: 8px;
  line-height: 23px;
  background: white;
  border-radius: 12px;
}

>>> .bk-table {
  .mark-row {
    .cell {
      padding-left: 15px !important;
    }
  }
}

.icon-share,
.icon-operate {
  position: absolute;
  top: 0;
  right: 24px;
  font-size: 16px;
  line-height: 48px;
  cursor: pointer;

  &:hover {
    color: #3a84ff;
  }
}

.icon-operate {
  right: 60px;
}

.dialog-block {
  position: absolute;
  right: 24px;
  z-index: 99;
  width: 272px;
  height: 150px;
  background: #fff;
  border-radius: 2px;
  box-shadow: 0 4px 12px 0 rgb(0 0 0 / 20%);

  .share-header {
    padding: 18px 0 10px 24px;
    font-size: 20px;
    line-height: 28px;
    color: #313238;
  }

  .icon-close {
    position: absolute;
    top: 10px;
    right: 10px;
    font-size: 28px;
    cursor: pointer;
  }

  .share-content {
    width: 200px;
    height: 32px;
    padding-left: 10px;
    margin: 16px 36px 0;
    line-height: 32px;
    cursor: pointer;
    background: #fff;
    border-radius: 2px;
    box-shadow: 0 2px 6px 0 rgb(0 0 0 / 10%);

    &:hover {
      color: #3a84ff;
    }
  }
}

>>> .bk-button.is-selected {
  padding-right: 44px;
  color: #3a84ff;
  background-color: #e1ecff;
  border-color: #3a84ff;
}
</style>
