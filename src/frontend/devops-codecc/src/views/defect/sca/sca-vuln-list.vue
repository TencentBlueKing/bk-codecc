<template>
  <div
    v-bkloading="{
      isLoading: !mainContentLoading && contentLoading,
      opacity: 0.3,
    }"
  >
    <section
      class="sca-list"
      v-if="enableSCA"
    >
      <div class="breadcrumb">
        <div class="breadcrumb-name">
          <ActiveTab :list="panels" value="sca-vuln" @update:value="handleTabChange" />
          <div
            class="tab-extra-icon"
          >
            <span class="mr20">
              <bk-link theme="primary" @click="reload">
                <span class="f12">{{ $t('重置筛选条件') }}</span>
              </bk-link>
            </span>
            <span
              :class="{
                'filter-collapse-icon mr20': true,
                'mac-filter-search-icon': isMac,
              }"
            >
              <bk-popover
                ext-cls="handle-menu"
                ref="handleMenu"
                theme="light"
                placement="left-start"
                trigger="click"
              >
                <i
                  class="bk-icon codecc-icon icon-filter-collapse"
                  :class="
                    isSearchDropdown
                      ? 'icon-filter-collapse'
                      : 'icon-filter-expand'
                  "
                  v-bk-tooltips="
                    isSearchDropdown ? $t('收起筛选项') : $t('展开筛选项')
                  "
                  @click="toggleSearch"
                ></i>
              </bk-popover>
            </span>
            <span
              :class="{
                'filter-search-icon': true,
                'mac-filter-search-icon': isMac,
              }"
            >
              <bk-popover
                ext-cls="handle-menu"
                ref="handleMenu"
                theme="light"
                placement="left-start"
                trigger="click"
              >
                <i
                  class="bk-icon codecc-icon icon-filter-set"
                  v-bk-tooltips="$t('设置筛选条件')"
                ></i>
                <div slot="content">
                  <filter-search-option
                    :default-option="defaultOption"
                    :custom-option="customOption"
                    @selectAll="handleSelectAllSearchOption"
                    @confirm="handleConfirmSearchOption"
                  />
                </div>
              </bk-popover>
            </span>
            <span class="excel-icon pl20">
              <bk-button
                style="border: 0"
                v-if="exportLoading"
                icon="loading"
                :disabled="true"
                :title="$t('导出Excel')"
              ></bk-button>
              <span
                v-else
                class="codecc-icon icon-export-excel excel-download"
                @click="downloadExcel"
                v-bk-tooltips="$t('导出Excel')"
              ></span>
            </span>
          </div>
        </div>
      </div>
      <div class="main-container" ref="mainContainer">
        <div class="main-content-inner main-content-list">
          <bk-form
            :label-width="70"
            class="search-form main-form"
            :class="{ collapse: !isSearchDropdown }"
          >
            <container :class="['cc-container', { fold: !isSearchDropdown }]">
              <div class="cc-col">
                <bk-form-item :label="$t('工具')">
                  <bk-select multiple v-model="searchParams.toolNameList">
                    <bk-option
                      v-for="option in toolList"
                      :key="option.toolName"
                      :id="option.toolName"
                      :name="option.toolDisplayName"
                    >
                    </bk-option>
                  </bk-select>
                </bk-form-item>
              </div>
              <div class="cc-col">
                <bk-form-item :label="$t('快照')">
                  <bk-select
                    v-model="searchParams.buildId"
                    :clearable="true"
                    searchable
                    :loading="selectLoading.buildListLoading"
                  >
                    <bk-option
                      v-for="item in searchFormData.buildList"
                      :key="item.buildId"
                      :id="item.buildId"
                      :name="`#${item.buildNum} ${item.branch} ${
                        item.buildUser
                      } ${formatDate(item.buildTime) || ''}${$t('触发')}`"
                    >
                      <div
                        class="cc-ellipsis"
                        :title="`#${item.buildNum} ${item.branch} ${
                          item.buildUser
                        } ${formatDate(item.buildTime) || ''}${$t('触发')}`"
                      >
                        {{
                          `#${item.buildNum} ${item.branch} ${item.buildUser} ${
                            formatDate(item.buildTime) || ''
                          }${$t('触发')}`
                        }}
                      </div>
                    </bk-option>
                  </bk-select>
                </bk-form-item>
              </div>
              <div class="cc-col">
                <bk-form-item :label="$t('状态')">
                  <bk-select
                    ref="statusSelect"
                    searchable
                    multiple
                    v-model="searchParams.statusUnion"
                    :key="statusTreeKey"
                    :remote-method="handleStatusRemote"
                    :tag-fixed-height="false"
                    :loading="selectLoading.statusLoading"
                    @tab-remove="handleStatusValuesChange"
                    @clear="handleStatusClear"
                  >
                    <bk-big-tree
                      :data="statusTreeData"
                      size="small"
                      show-checkbox
                      class="tree-select"
                      ref="statusTree"
                      v-if="hasCountData && hasIgnoreList"
                      :default-checked-nodes="searchParams.statusUnion"
                      :default-expand-all="!guideFlag"
                      @check-change="handleStatusCheckChange">
                      <div slot-scope="{ data }">
                        <div
                          v-if="
                            data.id === '4-24' && !guideFlag && hasIgnoreList
                          "
                        >
                          <bk-popover
                            ref="guidePopover"
                            placement="right-start"
                            :delay="2000"
                            theme="dot-menu light"
                          >
                            <span>{{ data.name }}</span>
                            <div class="guide-content" slot="content">
                              <div style="font-weight: bold; line-height: 22px">
                                {{ $t('【存量问题】可在这里查看') }}
                              </div>
                              <div>
                                {{ $t('针对特定忽略类型可') }}
                                <span
                                  theme="primary"
                                  class="set-tips"
                                  @click="handleSetReview"
                                >{{ $t('设置提醒') }}</span
                                >
                                {{ $t('，以便定期review和修复。') }}
                              </div>
                              <div class="guide-btn">
                                <span
                                  class="btn-item"
                                  @click="handleGuideNextStep"
                                >{{ $t('我知道了') }}</span
                                >
                              </div>
                            </div>
                          </bk-popover>
                        </div>
                        <div v-else>{{ data.name }}</div>
                      </div>
                    </bk-big-tree>
                  </bk-select>
                </bk-form-item>
              </div>
              <div class="cc-col">
                <bk-form-item :label="$t('风险级别')">
                  <bk-checkbox-group
                    v-model="searchParams.severity"
                    class="checkbox-group"
                  >
                    <bk-checkbox
                      v-for="(value, key, index) in vulnSeverityMap"
                      :value="Number(key)"
                      :key="index"
                    >
                      {{ value }}
                      <span
                      >(<em
                        :class="[
                          'count',
                          `count-${['major', 'minor', 'info'][index]}`,
                        ]"
                      >{{ getVulnCountBySeverity(key) }}</em
                      >)</span
                      >
                    </bk-checkbox>
                  </bk-checkbox-group>
                </bk-form-item>
              </div>
              <div class="cc-col">
                <bk-form-item :label="$t('组件名称')">
                  <bk-input
                    clearable
                    :placeholder="$t('搜索')"
                    :right-icon="'bk-icon icon-search'"
                    v-model="searchParams.keyword">
                  </bk-input>
                </bk-form-item>
              </div>
              <div class="cc-col">
                <bk-form-item :label="$t('处理人')">
                  <bk-select v-model="searchParams.author" searchable>
                    <bk-option
                      v-for="author in searchFormData.authorList"
                      :key="author.id"
                      :id="author.id"
                      :name="author.name"
                    >
                    </bk-option>
                  </bk-select>
                </bk-form-item>
              </div>
              <div class="cc-col">
                <bk-form-item :label="$t('日期')">
                  <bk-date-picker
                    editable
                    v-model="searchParams.daterange"
                    :placeholder="$t('请选择')"
                    type="daterange">
                  </bk-date-picker>
                </bk-form-item>
              </div>
            </container>
          </bk-form>

          <div class="cc-table">
            <div class="cc-selected">
              <span>{{
                $t('已选择x条,共y条', { x: selectedLen, y: pagination.count })
              }}</span>
            </div>
            <div v-if="isBatchOperationShow" class="cc-operate pb10">
              <div class="cc-operate-buttons">
                <bk-button
                  size="small"
                  ext-cls="cc-operate-button"
                  theme="primary"
                  @click="handleMark(1, true)"
                >{{ $t('标记处理') }}</bk-button
                >
                <bk-button
                  size="small"
                  ext-cls="cc-operate-button"
                  @click="handleAuthor(2)"
                  theme="primary"
                >{{ $t('分配') }}</bk-button
                >
                <bk-button
                  size="small"
                  ext-cls="cc-operate-button"
                  @click="handleIgnore('RevertIgnore', true)"
                  v-if="
                    !searchParams.status.length ||
                      searchParams.status.includes(4)
                  "
                  theme="primary"
                >
                  {{ $t('取消忽略') }}
                </bk-button>
                <bk-dropdown-menu
                  ext-cls="cc-operate-button"
                  ref="operateDropdown"
                  @show="isDropdownShow = true"
                  @hide="isDropdownShow = false"
                >
                  <bk-button size="small" slot="dropdown-trigger">
                    <span>{{ $t('更多') }}</span>
                    <i
                      :class="[
                        'bk-icon icon-angle-down',
                        { 'icon-flip': isDropdownShow },
                      ]"
                    ></i>
                  </bk-button>
                  <div class="handle-menu-tips" slot="dropdown-content">
                    <p class="entry-link" @click.stop="handleMark(0, true)">
                      {{ $t('取消标记') }}
                    </p>
                    <p
                      v-if="taskDetail.prohibitIgnore"
                      class="entry-link-allowed"
                      @click.stop="handleIgnore('IgnoreDefect', true)"
                      :title="
                        $t(
                          '已设置禁止页面忽略，可在代码行末或上一行使用注释忽略，例如// NOCC:rule1(ignore reason)'
                        )
                      "
                    >
                      {{ $t('忽略') }}
                    </p>
                    <p
                      v-else
                      class="entry-link"
                      @click.stop="handleIgnore('IgnoreDefect', true)"
                    >
                      {{ $t('忽略') }}
                    </p>
                  </div>
                </bk-dropdown-menu>
              </div>
            </div>

            <bk-table
              :data="vulnerabilityList"
              ref="scaTable"
              row-auto-height
              :pagination="pagination"
              :height="tableHeight"
              @page-change="handlePageChange"
              @page-limit-change="handlePageLimitChange"
              @row-click="handleClickRow"
              @selection-change="handleSelectionChange"
              v-bkloading="{ isLoading: tableLoading, opacity: 0.6 }">
              <bk-table-column
                type="selection"
                width="50"
                align="center"
              ></bk-table-column>
              <bk-table-column
                :label="$t('漏洞名称')"
                min-width="100"
                show-overflow-tooltip
                prop="name">
                <template slot-scope="props">
                  <span>{{ props.row.name || '--' }}</span>
                </template>
              </bk-table-column>
              <bk-table-column
                :label="$t('漏洞编号')"
                min-width="200"
                show-overflow-tooltip
                prop="vulnerabilityIds">
                <template slot-scope="props">
                  <span class="purple-text">{{ props.row.vulnerabilityIds.join(', ') || '--' }}</span>
                </template>
              </bk-table-column>
              <bk-table-column
                :label="$t('风险等级')"
                min-width="50"
                show-overflow-tooltip
                :filters="severityFilters"
                :filter-method="severityFilterMethod"
                prop="severity">
                <template slot-scope="props">
                  <SeverityStatus :cur-severity="props.row.severity" type="vuln"></SeverityStatus>
                </template>
              </bk-table-column>
              <bk-table-column
                :label="$t('组件名称')"
                min-width="100"
                show-overflow-tooltip
                prop="packageName">
                <template slot-scope="props">
                  <span>{{ props.row.packageName || '--' }}</span>
                </template>
              </bk-table-column>
              <bk-table-column
                :label="$t('组件版本')"
                min-width="100"
                show-overflow-tooltip
                prop="packageVersion">
                <template slot-scope="props">
                  <span>{{ props.row.packageVersion || '--' }}</span>
                </template>
              </bk-table-column>
              <bk-table-column
                :label="$t('CVSS 评分')"
                min-width="100"
                show-overflow-tooltip
                prop="cvss_rate">
                <template slot-scope="props">
                  <div class="cvss-display">
                    <div>{{ `V3: ${props.row?.cvssV3?.score || '--'}` }}</div>
                    <div>{{ `V2: ${props.row?.cvssV2?.score || '--'}` }}</div>
                  </div>
                </template>
              </bk-table-column>
              <bk-table-column
                :label="$t('处理人')"
                min-width="100"
                show-overflow-tooltip
                prop="author">
                <template slot-scope="props">
                  <div
                    v-if="props.row.status & 1 || props.row.status & 4"
                    @mouseenter="handleAuthorIndex(props.$index)"
                    @mouseleave="handleAuthorIndex(-1)"
                    @click.stop="handleAuthor(1, props.row.entityId, props.row.author)"
                  >
                    <span>{{ array2Str(props.row.author) }}</span>
                    <span
                      v-if="hoverAuthorIndex === props.$index"
                      class="bk-icon icon-edit2 fs18"
                    ></span>
                  </div>
                  <div v-else>
                    <span>
                      {{ array2Str(props.row.author) }}
                    </span>
                  </div>
                </template>
              </bk-table-column>
              <bk-table-column
                :label="$t('漏洞更新时间')"
                min-width="100"
                show-overflow-tooltip
                prop="modifiedDate">
                <template slot-scope="props">
                  <span>{{ formatDate(props.row.modifiedDate, 1) }}</span>
                </template>
              </bk-table-column>
              <bk-table-column
                :label="$t('最新状态')"
                min-width="100"
                show-overflow-tooltip
                prop="status">
                <template slot-scope="props">
                  <div>
                    {{ handleStatus(props.row.status, props.row.ignoreReasonType) }}
                  </div>
                  <div>
                    <span
                      v-if="props.row.status === 1 && props.row.mark === 1"
                      v-bk-tooltips="$t('已标记处理')"
                      class="codecc-icon icon-mark"
                    ></span>
                  </div>
                </template>
              </bk-table-column>
              <bk-table-column :label="$t('操作')" width="60">
                <template slot-scope="props">
                  <!-- 已修复问题没有这些操作 -->
                  <span
                    v-if="!(props.row.status & 2)"
                    class="cc-operate-more"
                    @click.prevent.stop
                  >
                    <bk-popover z-index="99" theme="light" placement="bottom" trigger="click">
                      <span
                        class="bk-icon icon-more guide-icon"
                        v-if="!(props.row.status & 8 || props.row.status & 16)">
                      </span>
                      <div slot="content" class="handle-menu-tips txal">
                        <!-- 待修复问题的操作 -->
                        <template v-if="props.row.status === 1">
                          <p
                            v-if="props.row.mark"
                            class="entry-link"
                            @click.stop="handleMark(0, false, props.row.entityId)"
                          >
                            {{ $t('取消标记') }}
                          </p>
                          <p
                            v-else
                            class="entry-link"
                            @click.stop="handleMark(1, false, props.row.entityId)"
                          >
                            {{ $t('标记处理') }}
                          </p>
                        </template>
                        <!-- 已忽略问题的操作 -->
                        <p
                          v-if="props.row.status & 4 && props.row.ignoreCommentDefect"
                          class="disabled"
                          :title="$t('注释忽略的问题不允许页面进行恢复操作')"
                        >
                          {{ $t('取消忽略') }}
                        </p>
                        <p
                          v-else-if="props.row.status & 4"
                          class="entry-link"
                          @click.stop="
                            handleIgnore('RevertIgnore', false, props.row.entityId)
                          "
                        >
                          {{ $t('取消忽略') }}
                        </p>
                        <p
                          v-if="props.row.status & 4 && !props.row.ignoreCommentDefect"
                          class="entry-link"
                          @click.stop="handleRevertIgnoreAndMark(props.row.entityId)"
                        >
                          {{ $t('取消忽略并标记处理') }}
                        </p>
                        <p
                          v-else-if="taskDetail.prohibitIgnore"
                          class="entry-link disabled"
                          :title="
                            $t(
                              '已设置禁止页面忽略，可在代码行末或上一行使用注释忽略，例如// NOCC:rule1(ignore reason)'
                            )
                          "
                        >
                          {{ $t('忽略问题') }}
                        </p>
                        <bk-popover
                          v-else
                          z-index="99"
                          ref="guidePopover2"
                          placement="left"
                          theme="dot-menu light"
                          trigger="click"
                        >
                          <div>
                            <span class="guide-flag"></span>
                            <span
                              :class="[
                                'entry-link',
                                'ignore-item',
                                [1, 2].includes(props.row.ignoreApprovalStatus) ? 'disabled' : ''
                              ]"
                              @click.stop="handleIgnore(
                                'IgnoreDefect',
                                false,
                                props.row.entityId,
                                null,
                                props.row.ignoreApprovalStatus
                              )"
                            >{{ $t('忽略问题') }}</span
                            >
                          </div>
                          <div class="guide-content" slot="content">
                            <div :style="lineHeight">
                              {{ $t('支持忽略无需处理或暂缓处理的问题。') }}
                              {{ $t('针对特定忽略类型可') }}
                              <span
                                theme="primary"
                                class="set-tips"
                                @click="handleTableSetReview"
                              >{{ $t('设置提醒') }}</span
                              >
                              {{ $t('，以便定期review和修复。') }}
                            </div>
                            <div class="guide-btn">
                              <span class="btn-item" @click="handleTableGuideNextStep">{{
                                $t('我知道了')
                              }}</span>
                            </div>
                          </div>
                        </bk-popover>
                        <p
                          v-if="props.row.status & 4 && !props.row.ignoreCommentDefect"
                          class="entry-link"
                          @click.stop="handleChangeIgnoreType(props.row)"
                        >
                          {{ $t('修改忽略类型') }}
                        </p>
                      </div>
                    </bk-popover>
                  </span>
                </template>
              </bk-table-column>
              <div slot="empty">
                <div class="codecc-table-empty-text">
                  <img src="../../../images/empty.png" class="empty-img" />
                  <div>{{ $t('没有查询到数据') }}</div>
                </div>
              </div>
            </bk-table>
          </div>
        </div>
      </div>
    </section>
    <div class="vuln-list" v-else>
      <div class="main-container large border-none">
        <div class="no-task">
          <empty
            title=""
            :desc="
              $t(
                'CodeCC集成了数十款工具，支持检查代码缺陷、安全漏洞、代码规范、软件成分等问题'
              )
            "
          >
            <template #action>
              <bk-button
                size="large"
                theme="primary"
                @click="addTool({ from: 'lint' })">
                {{ $t('配置规则集') }}
              </bk-button
              >
            </template>
          </empty>
        </div>
      </div>
    </div>

    <bk-dialog
      v-model="vulnDetailVisible"
      :ext-cls="'file-detail-dialog'"
      :fullscreen="isFullScreen"
      :position="{ top: `${isFullScreen ? 0 : 50}` }"
      :draggable="false"
      :mask-close="false"
      :show-footer="false"
      :close-icon="false"
      width="80%"
    >
      <scaVulnDetail
        v-if="vulnDetailVisible"
        @closeDetail="vulnDetailVisible = false"
        :entity-id="curRow.entityId"
        :tool-name="curRow.toolName"
        :build-id="curRow.buildId" />
    </bk-dialog>
    <AuthorEditDialog
      ref="authorEditRef"
      :change-author-type="operateParams.changeAuthorType"
      :source-author="operateParams.sourceAuthor"
      @before-close="handleBeforeClose"
      @confirm="handleAuthorEditConfirm"
      @targetAuthorChange="handleTargetAuthorChange"
      @sourceAuthorChange="handleSourceAuthorChange"
    />

    <IgnoreDialog
      ref="ignoreRef"
      :ignore-list="ignoreList"
      :approver-list="approverList"
      :title="ignoreReasonDialogTitle"
      :handle-set-review="handleSetReview"
      :ignore-reason-able="ignoreReasonAble"
      :is-loading="preIgnoreApprovalLoading"
      :pre-ignore-approval="preIgnoreApproval"
      :ignore-reason-required="ignoreReasonRequired"
      :ignore-reason-type="operateParams.ignoreReasonType"
      :ignore-reason="operateParams.ignoreReason"
      @confirm="handleIgnoreConfirm"
      @refresh="handelFetchIgnoreList"
      @before-close="handleBeforeClose"
      @ignoreReasonChange="handleChangeIgnoreReason"
      @ignoreReasonTypeChange="handleChangeIgnoreReasonType"
    />
  </div>
</template>

<script>
import _ from 'lodash';
import Empty from '@/components/empty';
import filterSearchOption from '../filter-search-option';
import scaVulnDetail from './vuln-detail/vuln-detail.vue';
import util from '@/mixins/defect-list';
import { mapState } from 'vuex';
import SeverityStatus from '../components/severity-status.vue';
import scaUtil from '@/mixins/sca-list';
import IgnoreDialog from './ignore-dialog.vue';
import AuthorEditDialog from './author-edit-dialog.vue';
import { array2Str } from '@/common/util';
import { format } from 'date-fns';
import ActiveTab from '../components/active-tab.vue';
import DEPLOY_ENV from '@/constants/env';

// 搜索过滤项缓存
const SEARCH_OPTION_CACHE = 'search_option_columns_sca';

export default {
  components: {
    Empty,
    filterSearchOption,
    scaVulnDetail,
    SeverityStatus,
    AuthorEditDialog,
    IgnoreDialog,
    ActiveTab,
  },
  mixins: [util, scaUtil],
  data() {
    this.getDefaultOption = () => ([
      { id: 'buildId', name: this.$t('快照'), isChecked: true },
    ]);
    this.getCustomOption = val => ([
      { id: 'status', name: this.$t('状态'), isChecked: val },
      { id: 'severity', name: this.$t('风险级别'), isChecked: val },
      { id: 'keyword', name: this.$t('组件名称'), isChecked: val },
      { id: 'author', name: this.$t('处理人'), isChecked: val },
      { id: 'daterange', name: this.$t('日期'), isChecked: val },
    ]);
    const { query } = this.$route;
    let status = [1];
    let statusUnion = [1]; // 状态和忽略类型组合的新字段
    if (query.status) {
      status = this.numToArray(query.status);
      statusUnion = status.slice();
    }
    let ignoreReasonTypes = [];
    if (query.ignoreTypeId) {
      ignoreReasonTypes = query.ignoreTypeId.split(',').map(i => Number(i));
      const statusIndex = statusUnion.findIndex(item => item === 4);
      if (statusIndex !== -1) {
        statusUnion.splice(statusIndex, 1);
      }
      const typesArr = ignoreReasonTypes.map(item => `4-${item}`);
      statusUnion = statusUnion.concat(typesArr);
    }
    return {
      isInnerSite: DEPLOY_ENV === 'tencent',
      dimension: ['SCA'],
      taskIdList: [this.$route.params.taskId],
      // loading area
      contentLoading: false,
      exportLoading: false,
      tableLoading: false,
      selectLoading: {
        buildListLoading: false, // 获取buildList loading
        otherParamsLoading: false, // 获取一些其他参数时的loading()
        statusLoading: false, // 获取状态 loading
      },
      preIgnoreApprovalLoading: false,

      // visible area
      isSearchDropdown: true,
      vulnDetailVisible: false,
      isSelectAll: '',
      selectedLen: 0,
      isRowChangeIgnoreType: false,
      isDropdownShow: false,

      // other ui need
      statusTreeKey: 1,
      ignoreList: [], // 忽略类型列表
      preIgnoreApproval: {
        count: 0,
        defectList: [],
      },
      tableHeight: 300,
      approverList: [],
      hasIgnoreList: true,
      hasCountData: false,
      guideFlag: false,
      dateType: 'createTime',
      curRow: {},
      defaultOption: this.getDefaultOption(),
      customOption: this.getCustomOption(true),
      isSearch: false,

      searchParams: {
        toolNameList: [],
        // data
        scaDimensionList: ['VULNERABILITY'],
        // data filter
        buildId: query.buildId ? query.buildId : '', // 快照
        status,
        statusUnion, // 状态
        severity: this.numToArray(query.severity), // 风险级别
        keyword: query.keyword || '', // 组件名称
        authors: query.author ? [query.author] : [], // 处理人
        daterange: [query.startTime, query.endTime], // 日期
      },
      searchFormData: {
        buildList: [], // 快照列表
        authorList: [],
        superHighCount: 0,
        highCount: 0, // 风险系数高的个数
        mediumCount: 0, // 风险系数中的个数
        lowCount: 0, // 风险系数低的个数

        totalCount: 0, // 许可证总数

        existCount: 0, // 待修复
        fixCount: 0, // 已修复
        ignoreCount: 0, // 已忽略
        maskCount: 0, // 已屏蔽
      },
      operateParams: {
        toolName: '',
        dimension: '',
        ignoreReasonType: '',
        ignoreReason: '',
        changeAuthorType: 1, // 1:单个修改处理人，2:批量修改处理人，3:固定修改处理人
        sourceAuthor: [],
        targetAuthor: [],
      },

      // table data
      listData: {
        vulnerabilityList: {
          records: [],
          count: 0,
        },
      },
      pagination: {
        current: 1,
        count: 0,
        limit: 10,
      },
      array2Str,
    };
  },
  computed: {
    ...mapState(['user']),
    vulnerabilityList() {
      return this.listData.vulnerabilityList.records || [];
    },
    searchParamsWatch() {
      return JSON.parse(JSON.stringify(this.searchParams));
    },
    ignoreReasonDialogTitle() {
      if (this.isRowChangeIgnoreType) {
        return this.$t('忽略');
      }
      return (this.isSelectAll === 'Y' ? this.pagination.count : this.selectedLen) > 1
        ? `${this.$t('忽略')}（
          ${this.$t('共x个问题', {
    num: this.isSelectAll === 'Y' ? this.pagination.count : this.selectedLen,
  })}）`
        : this.$t('忽略');
    },
    ignoreReasonRequired() {
      return this.operateParams.ignoreReasonType === 4;
    },
    ignoreReasonAble() {
      if (
        this.operateParams.ignoreReasonType === 4
          && this.operateParams.ignoreReason === ''
      ) {
        return true;
      }
      return !this.operateParams.ignoreReasonType;
    },
  },
  inject: ['reload'],
  watch: {
    vulnerabilityList(val, oldVal) {
      this.setTableHeight();
    },
    searchParamsWatch: {
      handler: _.debounce(function (newVal, oldVal) {
        if (_.isEqual(newVal, oldVal)) return;
        // 筛选状态，先特殊处理
        if (!_.isEqual(newVal.statusUnion, oldVal.statusUnion)) {
          const status = [];
          let ignoreTypes = [];
          newVal.statusUnion.forEach((item) => {
            if (typeof item === 'string' && item.startsWith('4-')) {
              ignoreTypes.push(Number(item.replace('4-', '')));
            } else {
              status.push(item);
            }
          });

          if (newVal.statusUnion.includes(4)) {
            // 全选已忽略，就不用传忽略类型
            ignoreTypes = [];
          } else if (ignoreTypes.length && !status.includes(4)) {
            // 选了忽略类型，参数同时要选已忽略状态
            status.push(4);
          }
          this.$refs.statusTree.setChecked(newVal.statusUnion);
          if (newVal.statusUnion.includes(2)) {
            this.dateType = 'fixTime';
          } else {
            this.dateType = 'createTime';
          }
          this.searchParams.status = status;
          this.searchParams.ignoreReasonTypes = ignoreTypes;
          return;
        }
        if (this.isSearch) {
          this.pagination.current = 1;
          this.initParams();
          this.fetchList();
        }
      }, 500),
      deep: true,
    },
  },
  created() {
    this.contentLoading = true;
    this.isSearch = true;
    this.initParams();
    this.fetchList();
    this.contentLoading = false;

    // 读取缓存搜索过滤项
    const columnsCache = JSON.parse(localStorage.getItem(SEARCH_OPTION_CACHE));
    const defaultOptionColumn = this.getCustomOption(true);
    if (columnsCache) {
      defaultOptionColumn.forEach((item) => {
        item.isChecked = columnsCache.find(column => column.id === item.id)?.isChecked;
      });
      this.customOption = defaultOptionColumn;
    }
    this.selectedOptionColumn = _.cloneDeep(defaultOptionColumn);
    this.guideFlag = Boolean(localStorage.getItem('guideEnd') || !this.isInnerSite);
  },
  mounted() {
    this.setTableHeight();
    window.addEventListener('resize', () => {
      this.setTableHeight();
    });
  },
  methods: {
    initParams() {
      this.fetchBuildList();
      this.fetchOtherParams();
      this.handleFetchApprover();
      this.handelFetchIgnoreList();
      this.fetchSeverityParams();
      this.fetchStatusParams();
    },
    // 获取table数据
    async fetchList() {
      this.selectLoading.statusLoading = true;
      this.tableLoading = true;
      try {
        const params = this.getSearchParams();
        const res = await this.$store.dispatch('defect/lintList', params);
        if (res?.vulnerabilityList) {
          this.listData.vulnerabilityList.records = res.vulnerabilityList.records;
          this.listData.vulnerabilityList.count = res.vulnerabilityList.count;
          this.pagination.count = res.vulnerabilityList.count;
        } else {
          // 清空数据
          this.listData.vulnerabilityList.records = [];
          this.listData.vulnerabilityList.count = 0;
          this.pagination.count = 0;
        }
      } catch (err) {
        console.error(err);
      } finally {
        this.selectLoading.statusLoading = false;
        this.tableLoading = false;
      }
    },
    handlePageChange(current) {
      this.pagination.current = current;
      this.fetchList();
    },
    handlePageLimitChange(current) {
      this.pagination.limit = current;
      this.fetchList();
    },
    // 获取处理人列表 authorList
    async fetchOtherParams() {
      if (this.visitable === false) return;
      this.selectLoading.otherParamsLoading = true;
      const { taskIdList, dimensionList } = this.basicSearchParams;
      const { buildId, scaDimensionList } = this.searchParams;
      const params = {
        dimensionList,
        buildId,
        taskIdList,
        multiTaskQuery: false,
        scaDimension: scaDimensionList[0],
      };
      const res = await this.$store.dispatch('defect/lintOtherParams', params);
      const { authorList = [] } = res;
      const newAuthorList = authorList.filter(item => item !== this.user.username);
      newAuthorList.unshift(this.user.username);
      const newList = newAuthorList.map(item => ({ id: item, name: item }));
      this.searchFormData = Object.assign(this.searchFormData, {
        authorList: newList,
      });
      this.selectLoading.otherParamsLoading = false;
    },
    // 选择table行的callback
    handleSelectionChange(selection) {
      this.selectedLen = selection.length || 0;
      this.isBatchOperationShow = Boolean(selection.length);
      // 如果长度是最长，那么就是Y，否则是N
      this.isSelectAll = (this.selectedLen && this.selectedLen === this.vulnerabilityList.length) ? 'Y' : 'N';
    },
    // 获取风险级别的统计量展示
    async fetchSeverityParams() {
      if (this.visitable === false) return;
      const params = this.getSearchParams();
      params.statisticType = 'SEVERITY';
      const res = await this.$store.dispatch('defect/lintSearchParams', params);
      const { unknownCount, seriousCount, normalCount, promptCount } = res;
      this.searchFormData = Object.assign(this.searchFormData, {
        unknownCount,
        seriousCount,
        normalCount,
        promptCount,
      });
      this.hasCountData = true;
    },
    // 请求table数据时，返回请求参数
    getSearchParams() {
      const { daterange } = this.searchParams;
      const params = {
        ...this.basicSearchParams,
        ...this.searchParams,
        pageNum: this.pagination.current,
        pageSize: this.pagination.limit,
      };
      const startTime = 'startCreateTime';
      const endTime = 'endCreateTime';
      params[startTime] = daterange[0] ? format(daterange[0], 'yyyy-MM-dd') : '';
      params[endTime] = daterange[1] ? format(daterange[1], 'yyyy-MM-dd') : '';
      delete params.daterange;
      delete params.statusUnion;
      return params;
    },
    // 获取状态的统计量展示
    async fetchStatusParams() {
      if (this.visitable === false) return;
      this.selectLoading.statusLoading = true;
      const params = this.getSearchParams();
      params.statisticType = 'STATUS';
      const res = await this.$store.dispatch('defect/lintSearchParams', params);
      const { existCount, fixCount, ignoreCount, maskCount } = res;
      this.searchFormData = Object.assign(this.searchFormData, {
        existCount,
        fixCount,
        ignoreCount,
        maskCount,
      });
      this.selectLoading.statusLoading = false;
    },
    // 获取快照列表
    async fetchBuildList() {
      if (this.visitable === false) return;
      this.selectLoading.buildListLoading = true;
      this.searchFormData.buildList = await this.$store.dispatch('defect/getBuildList', {
        taskId: this.$route.params.taskId,
      });
      this.selectLoading.buildListLoading = false;
    },
    // 获取忽略审批列表
    handleFetchApprover() {
      this.$store.dispatch('ignore/fetchApproverList').then((res) => {
        this.approverList = res?.data?.APPROVER_TYPE;
      });
    },
    // 获取忽略列表
    handelFetchIgnoreList() {
      if (this.visitable === false) return;
      this.$store.dispatch('ignore/fetchIgnoreList').then((res) => {
        this.ignoreList = res.data;
        this.hasIgnoreList = true;
      });
    },
    // table row click callback
    handleClickRow(row) {
      this.vulnDetailVisible = true;
      this.curRow = row;
    },
    // 处理并展示状态文字
    handleStatus(status, ignoreReasonType) {
      let key = 1;
      if (status === 1) {
        key = 1;
      } else if (status & 2) {
        key = 2;
      } else if (status & 4) {
        key = 4;
      } else if (status & 8 || status & 16) {
        // 8是路径屏蔽，16是规则屏蔽
        key = 8;
      }
      const statusMap = {
        1: this.$t('待修复'),
        2: this.$t('已修复'),
        4: this.$t('已忽略'),
        8: this.$t('已屏蔽'),
      };
      let ignoreStr = '';
      if (status & 4 && ignoreReasonType) {
        const ignoreName = this.ignoreList.find(item => item.ignoreTypeId === ignoreReasonType)?.name;
        ignoreName && (ignoreStr = `-${ignoreName}`);
      }
      return `${statusMap[key]}${ignoreStr}`;
    },
    // 新增类型
    handleSetReview() {
      this.$refs.guidePopover?.hideHandler();
      let prefix = `${location.host}`;
      if (window.self !== window.top) {
        prefix = `${window.DEVOPS_SITE_URL}/console`;
      }
      const route = this.$router.resolve({
        name: 'ignoreList',
      });
      window.open(prefix + route.href, '_blank');
      this.handleGuideNextStep();
    },
    // 忽略审批切换radio
    async handleChangeIgnoreReasonType(value) {
      this.preIgnoreApproval.count = 0;
      this.preIgnoreApproval.defectList = [];
      let data = this.operateParams;
      data.ignoreReasonType = value.ignoreTypeId;
      if (this.isSelectAll === 'Y') {
        data = {
          ...data,
          isSelectAll: 'Y',
          queryDefectCondition: JSON.stringify(this.getSearchParams()),
        };
      }
      const {
        taskIdList,
        dimensionList,
      } = this;
      const { toolNameList } = this.searchParams;
      data = { ...data, taskIdList, toolNameList, dimensionList };
      this.preIgnoreApprovalLoading = true;
      const result = await this.$store.dispatch('defect/getPreIgnoreApproval', data);
      this.preIgnoreApprovalLoading = false;
      this.preIgnoreApproval.count = result.count;
      this.preIgnoreApproval.defectList = result.defectList;
    },
    // 跳转新增类型后的下一步指导
    handleGuideNextStep() {
      this.$nextTick(() => {
        this.$refs.guidePopover?.hideHandler();
        bus.$emit('handleNextGuide');
        localStorage.setItem('guideEnd', true);
        this.guideFlag = true;
      });
    },
    // 设置提醒
    handleTableSetReview() {
      let prefix = `${location.host}`;
      if (window.self !== window.top) {
        prefix = `${window.DEVOPS_SITE_URL}/console`;
      }
      const route = this.$router.resolve({
        name: 'ignoreList',
      });
      window.open(prefix + route.href, '_blank');
      document.body.click();
      localStorage.setItem('guide2End', true);
    },
    // 关闭设置提醒
    handleTableGuideNextStep() {
      document.body.click();
      localStorage.setItem('guide2End', true);
    },
    // 分配
    handleAuthor(changeAuthorType, entityId, author) {
      this.operateParams.changeAuthorType = changeAuthorType;
      if (author !== undefined) {
        this.operateParams.sourceAuthor = author;
      }
      this.$refs.authorEditRef.handleShow();
      this.operateParams.defectKeySet = [entityId];
    },
    // 处理人修改
    handleAuthorEditConfirm() {
      let data = this.operateParams;
      const sourceAuthor = data.sourceAuthor
        ? new Set(data.sourceAuthor)
        : new Set();
      if (data.changeAuthorType === 2) {
        const defectKeySet = [];
        this.$refs.scaTable.selection.forEach((item) => {
          defectKeySet.push(item.entityId);
          item.author && sourceAuthor.add(...item.author);
        });
        this.operateParams.defectKeySet = defectKeySet;
        data.defectKeySet = defectKeySet;
      }
      data.bizType = 'AssignDefect';

      data.sourceAuthor = Array.from(sourceAuthor);
      data.newAuthor = data.targetAuthor;
      if (this.isSelectAll === 'Y') {
        data = {
          ...data,
          isSelectAll: 'Y',
          queryDefectCondition: JSON.stringify(this.getSearchParams()),
        };
      }
      const {
        taskIdList,
        dimensionList,
      } = this.basicSearchParams;

      const { scaDimensionList, toolNameList } = this.searchParams;

      data = { ...data, taskIdList, toolNameList, dimensionList, scaDimension: scaDimensionList[0] };
      const dispatchUrl = data.changeAuthorType === 3 ? 'defect/authorEdit' : 'defect/batchEdit';
      this.$refs.authorEditRef.handleHide();
      this.tableLoading = true;
      this.$store
        .dispatch(dispatchUrl, data)
        .then((res) => {
          if (res.code === '0') {
            this.$bkMessage({
              theme: 'success',
              message: this.$t('修改处理人成功'),
            });
            this.operateParams.targetAuthor = [];
            this.operateParams.sourceAuthor = [];
            if (data.changeAuthorType === 1) {
              this.listData.vulnerabilityList.records.forEach((item) => {
                if (item.entityId === data.defectKeySet[0]) {
                  item.author = data.newAuthor;
                }
              });
              this.listData.vulnerabilityList.records = this.listData.vulnerabilityList.records.slice();
            } else {
              this.fetchList();
            }
            this.initParams();
          }
        })
        .catch((e) => {
          console.error(e);
        })
        .finally(() => {
          this.tableLoading = false;
        });
    },
    handleTargetAuthorChange(targetAuthor) {
      this.operateParams.targetAuthor = targetAuthor;
    },
    handleSourceAuthorChange(sourceAuthor) {
      this.operateParams.sourceAuthor = sourceAuthor;
    },
    handleAuthorIndex(index) {
      this.hoverAuthorIndex = index;
    },
    // 重置搜索过滤项
    handleSelectAllSearchOption() {
      const isSelectAll = this.customOption.every(item => item.isChecked);
      this.customOption = this.getCustomOption(!isSelectAll);
    },
    // 确认搜索过滤项
    handleConfirmSearchOption() {
      this.$refs.handleMenu.instance.hide();
      this.selectedOptionColumn = _.cloneDeep(this.customOption);
      localStorage.setItem(
        SEARCH_OPTION_CACHE,
        JSON.stringify(this.selectedOptionColumn),
      );
      this.setTableHeight();
    },
    // 标记
    handleMark(markFlag, batchFlag, entityId) {
      // markFlag 0: 取消标记, 1: 标记修改
      // batchFlag true: 批量操作
      let bizType = 'MarkDefect';
      let defectKeySet = [];
      if (batchFlag) {
        this.$refs.scaTable.selection.forEach((item) => {
          defectKeySet.push(item.entityId);
        });
        if (markFlag) bizType = 'MarkDefect';
      } else {
        defectKeySet = [entityId];
      }
      const {
        taskIdList,
        dimensionList,
      } = this.basicSearchParams;

      const { scaDimensionList, toolNameList } = this.searchParams;

      let data = {
        ...this.operateParams,
        bizType,
        defectKeySet,
        markFlag,
        taskIdList,
        toolNameList,
        dimensionList,
        scaDimension: scaDimensionList[0],
      };
      if (this.isSelectAll === 'Y') {
        data = {
          ...data,
          isSelectAll: 'Y',
          queryDefectCondition: JSON.stringify(this.getSearchParams()),
        };
      }
      this.tableLoading = true;
      this.$store
        .dispatch('defect/batchEdit', data)
        .then((res) => {
          if (res.code === '0') {
            let message = markFlag
              ? this.$t('标记为已处理成功')
              : this.$t('取消标记成功');
            if (batchFlag) {
              this.fetchList();
              if (markFlag) {
                const list = res.data || [];
                let markCount = 0;
                let failCount = 0;
                list.forEach((item) => {
                  if (item.bizType === 'RevertIgnore') {
                    revertCount = item.count;
                  } else if (item.bizType === 'MarkDefect') {
                    markCount = item.count;
                    failCount = item.failCount;
                  }
                });
                if (markCount) {
                  message = this.$t('x个问题标记为已处理成功。', [markCount]);
                }
                if (failCount) {
                  message += this.$t('x个问题由于状态原因标记失败。', [failCount]);
                }
              }
            } else {
              this.listData.vulnerabilityList.records.forEach((item) => {
                if (item.entityId === entityId) {
                  item.mark = markFlag;
                }
              });
              this.listData.vulnerabilityList.records = this.listData.vulnerabilityList.records.slice();
            }
            this.$bkMessage({
              theme: 'success',
              message,
            });
          }
        })
        .catch((e) => {
          console.error(e);
        })
        .finally(() => {
          this.tableLoading = false;
        });
    },
    async handleRevertIgnoreAndMark(entityId) {
      await this.handleIgnore('RevertIgnore', false, entityId);
      setTimeout(() => {
        this.handleMark(1, false, entityId);
      }, 500);
    },
    // 打开忽略dialog
    handleIgnore(ignoreType, batchFlag, entityId, filePath, ignoreApprovalStatus) {
      this.$refs.operateDropdown?.hide();
      if (this.taskDetail.prohibitIgnore) return;
      if (ignoreApprovalStatus && [1, 2].includes(ignoreApprovalStatus)) return;
      this.operateParams.fileList = [filePath];
      this.operateParams.bizType = ignoreType;
      this.operateParams.batchFlag = batchFlag;
      if (batchFlag) {
        const defectKeySet = [];
        const fileList = [];
        this.$refs.scaTable.selection.forEach((item) => {
          defectKeySet.push(item.entityId);
          fileList.push(item.filePath);
        });
        this.operateParams.defectKeySet = defectKeySet;
        this.operateParams.fileList = fileList;
      } else {
        this.isSelectAll = 'N';
        this.operateParams.defectKeySet = [entityId];
      }
      if (ignoreType === 'RevertIgnore') {
        this.handleIgnoreConfirm();
      } else {
        this.$refs.ignoreRef.handleShow();
      }
    },
    // 忽略
    handleIgnoreConfirm() {
      let data = this.operateParams;
      this.tableLoading = true;
      this.$refs.ignoreRef.handleHide();
      if (this.isSelectAll === 'Y') {
        data = {
          ...data,
          isSelectAll: 'Y',
          queryDefectCondition: JSON.stringify(this.getSearchParams()),
        };
      }
      const {
        dimensionList,
      } = this.basicSearchParams;
      const { scaDimensionList, toolNameList } = this.searchParams;
      const { taskIdList } = this.basicSearchParams;

      data = { ...data, taskIdList, toolNameList, dimensionList, scaDimension: scaDimensionList[0] };
      this.isRowChangeIgnoreType = false;
      this.$store
        .dispatch('defect/batchEdit', data)
        .then((res) => {
          if (res.code === '0') {
            let message = '';
            let isIgnoreApproval = false;
            const list = res.data || [];
            if (this.operateParams.bizType === 'ChangeIgnoreType') {
              message = this.$t('修改忽略类型成功');
            } else {
              const sortMessage = [];
              list.forEach((item) => {
                const typeMap = {
                  IgnoreDefect: this.$t('忽略'),
                  RevertIgnore: this.$t('取消忽略'),
                };
                if (Object.keys(typeMap).includes(item.bizType)) {
                  sortMessage.push({
                    text: this.$t(`成功${typeMap[item.bizType]}x个问题。`, [item.count || 0]),
                    index: 0,
                  });
                }
                if (item.failCount) {
                  sortMessage.push({
                    text: this.$t(`剩余x个问题由于状态原因${typeMap[item.bizType]}失败。`, [item.failCount || 0]),
                    index: 1,
                  });
                }
                if (item.bizType === 'IgnoreApproval' && item.count !== 0) {
                  isIgnoreApproval = true;
                  sortMessage.push({
                    text: this.$t('另外x个问题正在忽略审批中。', [item.count]),
                    index: 2,
                  });
                }
                message = sortMessage
                  .sort((a, b) => a.index - b.index)
                  .map(item => item.text)
                  .join('');
              });
            }
            this.$bkMessage({
              theme: 'success',
              message,
            });
            if (!data.batchFlag) {
              const index = this.listData.vulnerabilityList.records.findIndex(item => item.entityId === data.defectKeySet[0]);
              if (index !== -1) {
                this.listData.vulnerabilityList.records.forEach((item) => {
                  if (item.entityId === data.defectKeySet[0]) {
                    if (this.operateParams.bizType === 'ChangeIgnoreType') {
                      item.ignoreReasonType = this.operateParams.ignoreReasonType;
                      item.ignoreReason = this.operateParams.ignoreReason;
                    } else if (this.operateParams.bizType === 'IgnoreDefect') {
                      if (isIgnoreApproval) {
                        item.ignoreApprovalStatus = 1;
                      } else {
                        item.status = 4;
                        item.ignoreReasonType = this.operateParams.ignoreReasonType;
                        item.ignoreReason = this.operateParams.ignoreReason;
                      }
                    } else {
                      item.status = 1;
                    }
                  }
                });
                this.listData.vulnerabilityList.records = this.listData.vulnerabilityList.records.slice();
              }
            } else {
              this.fetchList();
            }
            this.initParams();

            this.operateParams.ignoreReason = '';
            this.operateParams.ignoreReasonType = '';
          }
        })
        .catch((e) => {
          console.error(e);
        })
        .finally(() => {
          this.tableLoading = false;
        });
    },
    /**
     * 修改忽略类型
     */
    handleChangeIgnoreType(row) {
      this.isSelectAll = 'N';
      this.isRowChangeIgnoreType = true;
      this.operateParams.ignoreReasonType = row.ignoreReasonType;
      this.operateParams.ignoreReason = row.ignoreReason;
      this.handleIgnore('ChangeIgnoreType', false, row.entityId);
    },
    /**
     * @description 导出表格数据
     */
    // 导出表格数据
    downloadExcel() {
      if (this.tableLoading) {
        this.$bkMessage({
          message: this.$t('漏洞列表加载中，请等待列表加载完再尝试导出。'),
        });
        return;
      }

      if (this.pagination.count > 300000) {
        this.$bkMessage({
          message: this.$t('当前问题数已超过30万个，无法直接导出excel，请筛选后再尝试导出。'),
        });
        return;
      }

      this.exportLoading = true;
      const params = {
        ...this.basicSearchParams,
        ...this.searchParams,
        pageNum: this.pagination.current,
        pageSize: this.pagination.limit,
      };
      this.$store
        .dispatch('defect/lintList', params)
        .then((res) => {
          const list = res && res.vulnerabilityList && res.vulnerabilityList.records;
          const headerProps = {
            index: this.$t('序号'),
            entityId: this.$t('entityId'),
            name: this.$t('漏洞名称'),
            vulnerabilityIds: this.$t('漏洞编号'),
            severity: this.$t('风险等级'),
            packageName: this.$t('组件名称'),
            packageVersion: this.$t('组件版本'),
            cvss_rate: this.$t('CVSS 评分'),
            author: this.$t('处理人'),
            modifiedDate: this.$t('漏洞更新时间'),
            status: this.$t('最新状态'),
          };
          this.generateExcel(headerProps, list);
        })
        .finally(() => {
          this.exportLoading = false;
        });
    },
    // 处理表格数据
    formatJson(filterVal, list) {
      let index = 0;
      return list.map(item => filterVal.map((j) => {
        if (j === 'index') {
          return (index += 1);
        }
        if (j === 'vulnerabilityIds') {
          return item[j].join(', ');
        }
        if (j === 'severity') {
          return this.vulnSeverityMap[item[j]];
        }
        if (j === 'cvss_rate') {
          return `V3: ${item.row?.cvssV3?.score || '--'} V2: ${item.row?.cvssV2?.score || '--'}`;
        }
        if (j === 'modifiedDate') {
          return this.formatDate(item[j], 1);
        }
        if (j === 'status') {
          return this.handleStatus(item[j].status, item[j].ignoreReasonType);
        }
        return item[j];
      }));
    },

    /**
     * @description 以下 methods 作用为UI交互
     */
    // 展开/折叠筛选项
    toggleSearch() {
      this.isSearchDropdown = !this.isSearchDropdown;
      window.localStorage.setItem(
        'scaVulnSearchExpend',
        JSON.stringify(this.isSearchDropdown),
      );
      this.setTableHeight();
    },
    // 设置表格高度
    setTableHeight() {
      this.$nextTick(() => {
        if (this.vulnerabilityList.length === 0) {
          this.tableHeight = 336;
          return;
        }
        const otherElementHeight = 310;
        this.tableHeight = window.innerHeight - otherElementHeight;
      });
    },
  },
};
</script>

<style>
@import url('../codemirror.css');
</style>

<style lang="postcss" scoped>
@import url('../../../css/variable.css');
@import url('../../../css/mixins.css');
@import url('../defect-list.css');

.sca-list {
  margin: 16px 20px 0 16px;
}

.breadcrumb {
  padding: 0 !important;

  .breadcrumb-name {
    background: white;
  }
}

.main-container {
  margin: 0 !important;
  background: white;
}

>>> .checkbox-group {
  min-width: 293px;
  font-size: 14px;

  .popover {
    position: relative;
    top: 1px;
    font-size: 16px;
  }

  .bk-checkbox-text {
    font-size: 12px;
  }
}

>>> .bk-date-picker {
  width: 298px;
}

.bk-date-picker {
  width: 300px;
}

>>> .bk-table {
  .mark-row {
    .cell {
      padding: 0;
    }
  }
}

.cc-table {
  position: relative;

  .cc-selected {
    float: left;
    height: 42px;
    padding-right: 10px;
    font-size: 12px;
    line-height: 32px;
    color: #333;
  }

  .cc-operate {
    display: inline-block;

    .cc-operate-buttons {
      display: flex;

      .cc-operate-button {
        margin-left: 10px;
      }
    }
  }
}


.main-container::-webkit-scrollbar {
  width: 0;
}

.excel-download {
  padding-right: 10px;
  line-height: 32px;
  cursor: pointer;

  &:hover {
    color: #3a84ff;
  }
}

/deep/ .bk-dialog-body {
  padding: 0;
}

/deep/ .bk-dialog-header {
  padding: 3px 24px 0 !important;
}

.purple-text {
  font-weight: 500;
  color: #9e3e8f;
}

.search-form.main-form.collapse {
  /* height: 48px; */
  overflow: hidden;
}

/deep/ .cvss-display {
  display: flex;
  flex-direction: column;
  padding: 10px;
}

.icon-mark {
  color: #53cad1;

  &.re-mark {
    color: #facc48;
  }
}

.handle-menu-tips {
  text-align: center;

  .entry-link {
    padding: 4px 0;
    font-size: 12px;
    color: $fontWeightColor;
    cursor: pointer;

    &.disabled {
      color: $borderColor;
      cursor: not-allowed;

      &:hover {
        color: $borderColor;

        > a {
          color: $borderColor;
        }
      }
    }

    > a {
      color: $fontWeightColor;
    }

    &:hover {
      color: $primaryColor;

      > a {
        color: $primaryColor;
      }
    }
  }

  .entry-link-allowed {
    padding: 4px 0;
    font-size: 12px;
    color: $fontWeightColor;
    cursor: not-allowed;

    &:hover {
      color: $borderColor;

      > a {
        color: $borderColor;
      }
    }
  }
}

.cc-operate-more {
  cursor: pointer;

  >>> .icon-more {
    font-size: 20px;
  }
}

>>> .bk-label {
  font-size: 12px;
}
</style>
