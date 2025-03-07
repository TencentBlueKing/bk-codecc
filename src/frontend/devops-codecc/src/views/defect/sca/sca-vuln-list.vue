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
          <bk-tab
            active="sca-vuln"
            :label-height="42"
            @tab-change="handleTabChange"
            type="unborder-card"
          >
            <bk-tab-panel
              v-for="(panel, index) in panels"
              v-bind="panel"
              :key="index"
            >
            </bk-tab-panel>
          </bk-tab>
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
                    >
                      <div slot-scope="{ node, data }">
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
                                >{{ $t('设置提醒') }}</span
                                >
                                {{ $t('，以便定期review和修复。') }}
                              </div>
                              <div class="guide-btn">
                                <span
                                  class="btn-item"
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
              <div class="cc-col-2">
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
                <bk-form-item :label="$t('依赖方式')">
                  <bk-select
                    v-model="searchParams.direct"
                    :clearable="true"
                    searchable
                  >
                    <bk-option
                      v-for="(item, index) in searchFormData.directList"
                      :key="index"
                      :id="item.value"
                      :name="item.name"
                    ></bk-option>
                  </bk-select>
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
                <bk-form-item :label="$t('语言')">
                  <bk-select v-model="searchParams.languageList" searchable>
                    <bk-option
                      v-for="lang in toolMeta.LANG"
                      :key="lang.key"
                      :id="parseInt(lang.key, 10)"
                      :name="lang.fullName"
                    >
                    </bk-option>
                  </bk-select>
                </bk-form-item>
              </div>
              <div class="cc-col">
                <bk-form-item :label="$t('日期')">
                  <date-picker
                    :date-range="searchParams.daterange"
                    :handle-change="handleDateChange"
                    :status-union="searchParams.statusUnion"
                    :selected="dateType"
                  ></date-picker>
                </bk-form-item>
              </div>
            </container>
          </bk-form>

          <div class="cc-table">

            <bk-table
              :data="vulnerabilityList"
              ref="scaTable"
              :pagination="pagination"
              @page-change="handlePageChange"
              @row-click="handleClickRow"
              v-bkloading="{ isLoading: tableLoading, opacity: 0.6 }">
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
                :label="$t('漏洞名称')"
                min-width="100"
                show-overflow-tooltip
                prop="vulName">
                <template slot-scope="props">
                  <span class="purple-text">{{ props.row.vulName || '--' }}</span>
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
                :label="$t('漏洞类型')"
                min-width="100"
                show-overflow-tooltip
                prop="vulType">
                <template slot-scope="props">
                  <span>{{ props.row.vulType || '--' }}</span>
                </template>
              </bk-table-column>
              <bk-table-column
                :label="$t('攻击类型')"
                min-width="100"
                show-overflow-tooltip
                prop="attackType">
                <template slot-scope="props">
                  <span>{{ props.row.attackType || '--' }}</span>
                </template>
              </bk-table-column>
              <bk-table-column
                :label="$t('CVSS 评分')"
                min-width="100"
                show-overflow-tooltip
                prop="cvss_rate">
                <template slot-scope="props">
                  <div class="cvss-display">
                    <div>{{ `V3: ${props.row.cvssV3}` }}</div>
                    <div>{{ `V2: ${props.row.cvssV2}` }}</div>
                  </div>
                </template>
              </bk-table-column>
              <bk-table-column
                :label="$t('是否可被利用')"
                min-width="100"
                show-overflow-tooltip
                prop="exploitable">
                <template slot-scope="props">
                  {{ props.row.exploitable ? $t('是') : $t('否') }}
                </template>
              </bk-table-column>
              <bk-table-column
                :label="$t('更新时间')"
                min-width="100"
                show-overflow-tooltip
                prop="releaseDate">
                <template slot-scope="props">
                  <span>{{ formatDate(props.row.releaseDate, 1) }}</span>
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
        :build-id="curRow.buildId"
      ></scaVulnDetail>
    </bk-dialog>
  </div>
</template>

<script>
import _ from 'lodash';
import Empty from '@/components/empty';
import filterSearchOption from '../filter-search-option';
import DatePicker from '@/components/date-picker/index.vue';
import scaVulnDetail from './sca-vuln-detail.vue';
import { mapState } from 'vuex';
import SeverityStatus from '../components/severity-status.vue';
import scaUtil from '@/mixins/sca-list';

export default {
  components: {
    DatePicker,
    Empty,
    filterSearchOption,
    scaVulnDetail,
    SeverityStatus,
  },
  mixins: [scaUtil],
  data() {
    const { query } = this.$route;
    let status = [1];
    let statusUnion = [1]; // 状态和忽略类型组合的新字段
    if (query.status) {
      status = this.numToArray(query.status);
      statusUnion = status.slice();
    }
    return {
      // loading area
      contentLoading: false,
      exportLoading: false,
      tableLoading: false,
      selectLoading: {
        buildListLoading: false, // 获取buildList loading
        otherParamsLoading: false, // 获取一些其他参数时的loading()
        statusLoading: false, // 获取状态 loading
      },

      // visible area
      isSearchDropdown: true,
      vulnDetailVisible: false,

      // other ui need
      statusTreeKey: 1,
      ignoreList: [], // 忽略类型列表
      hasIgnoreList: true,
      hasCountData: false,
      guideFlag: false,
      dateType: 'createTime',
      curRow: {},
      isSearch: false,

      searchParams: {
        // data
        scaDimensionList: ['VULNERABILITY'],
        // data filter
        buildId: query.buildId ? query.buildId : '', // 快照
        statusUnion, // 状态
        severity: this.numToArray(query.severity), // 风险级别
        keyword: query.keyword || '', // 组件名称
        direct: query.direct || '', // 依赖方式，true为直接依赖，false为间接依赖，默认为空，查询所有依赖方式
        authors: query.author ? [query.author] : [], // 处理人
        languageList: query.languageList ? [query.languageList] : [], // 语言列表筛选条件
        daterange: [query.startTime, query.endTime], // 日期
      },
      searchFormData: {
        buildList: [], // 快照列表
        directList: [
          {
            name: this.$t('直接依赖'),
            value: true,
          },
          {
            name: this.$t('间接依赖'),
            value: false,
          },
        ], // 依赖方式
        checkerLanguage: [],
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
        showLimit: false,
      },
    };
  },
  computed: {
    ...mapState(['user']),
    ...mapState(['toolMeta']),
    vulnerabilityList() {
      return this.listData.vulnerabilityList.records || [];
    },
    searchParamsWatch() {
      return JSON.parse(JSON.stringify(this.searchParams));
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
          this.fetchList();
        }
      }, 500),
      deep: true,
    },
  },
  created() {
    this.contentLoading = true;
    // 快照数据列表获取
    this.fetchBuildList();
    // 处理人数据获取
    this.fetchOtherParams();
    // 风险级别数据获取
    this.fetchSeverityParams();
    // 状态数据获取
    this.fetchStatusParams();

    this.fetchList();
    this.contentLoading = false;
    this.isSearch = true;
  },
  methods: {
    // 获取table数据
    async fetchList() {
      this.selectLoading.statusLoading = true;
      this.tableLoading = true;
      try {
        const params = {
          ...this.basicSearchParams,
          ...this.searchParams,
          pageNum: this.pagination.current,
          pageSize: this.pagination.limit,
        };
        const res = await this.$store.dispatch('defect/lintList', params);
        if (res.vulnerabilityList) {
          this.listData.vulnerabilityList.records = res.vulnerabilityList.records;
          this.listData.vulnerabilityList.count = res.vulnerabilityList.count;
          this.pagination.count = res.vulnerabilityList.count;
        } else {
          // 清空数据
          this.listData.vulnerabilityList.records = [];
          this.listData.vulnerabilityList.count = 0;
          this.pagination.count = 0;
        }

        // // 状态数据初始化
        // const { existCount, fixCount, ignoreCount, maskCount } = res;
        // this.searchFormData = Object.assign(this.searchFormData, {
        //   existCount,
        //   fixCount,
        //   ignoreCount,
        //   maskCount,
        // });
        // // 风险级别初始化
        // const { superHighCount, highCount, mediumCount, lowCount } = res;
        // this.searchFormData = Object.assign(this.searchFormData, {
        //   superHighCount,
        //   highCount,
        //   mediumCount,
        //   lowCount,
        // });
      } catch (err) {

      } finally {
        this.selectLoading.statusLoading = false;
        this.tableLoading = false;
      }
    },
    handlePageChange(current) {
      this.pagination.current = current;
      this.fetchList();
    },
    // 获取处理人列表 authorList
    async fetchOtherParams() {
      if (this.visitable === false) return;
      this.selectLoading.otherParamsLoading = true;
      const { taskIdList, dimensionList } = this.basicSearchParams;
      const { buildId } = this.searchParams;
      const params = {
        dimensionList,
        buildId,
        taskIdList,
        multiTaskQuery: false,
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
    // 获取风险级别的统计量展示
    async fetchSeverityParams() {
      if (this.visitable === false) return;
      const params = {
        ...this.basicSearchParams,
        ...this.searchParams,
      };
      params.statisticType = 'SEVERITY';
      const res = await this.$store.dispatch('defect/lintSearchParams', params);
      const { unknownCount, highCount, mediumCount, lowCount } = res;
      this.searchFormData = Object.assign(this.searchFormData, {
        unknownCount,
        highCount,
        mediumCount,
        lowCount,
      });
      this.hasCountData = true;
    },
    // 获取状态的统计量展示
    async fetchStatusParams() {
      if (this.visitable === false) return;
      this.selectLoading.statusLoading = true;
      const params = {
        ...this.basicSearchParams,
        ...this.searchParams,
      };
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

    /**
     * @description 处理form-item status
     */
    handleStatusRemote(keyword) {
      this.$refs.statusTree && this.$refs.statusTree.filter(keyword);
    },
    handleStatusValuesChange(options) {
      this.$refs.statusTree
        && this.$refs.statusTree.setChecked(options.id, {
          emitEvent: true,
          checked: false,
        });
    },
    handleStatusClear() {
      this.$refs.statusTree
        && this.$refs.statusTree.removeChecked({ emitEvent: false });
    },

    // date-picker callback
    handleDateChange(date, type) {
      this.searchParams.daterange = date;
      this.dateType = type;
    },
    // table row click callback
    handleClickRow(row) {
      this.vulnDetailVisible = true;
      this.curRow = row;
    },
    /**
     * @description 导出表格数据
     */
    // 导出表格数据
    downloadExcel() {
      if (this.tableLoading) {
        this.$bkMessage({
          message: this.$t('许可证列表加载中，请等待列表加载完再尝试导出。'),
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
            severity: this.$t('风险等级'),
            vulName: this.$t('漏洞名称'),
            vulnerabilityIds: this.$t('漏洞类型'),
            vulType: this.$t('攻击类型'),
            attackType: this.$t('CVSS 评分'),
            exploitable: this.$t('是否可被利用'),
            releaseDate: this.$t('更新时间'),
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
        if (j === 'severity') {
          return this.vulnSeverityMap[item[j]];
        }
        if (j === 'vulnerabilityIds') {
          return item[j].join(', ');
        }
        if (j === 'exploitable') {
          return item[j] ? this.$t('是') : this.$t('否');
        }
        if (j === 'releaseDate') {
          return this.formatDate(item[j], 1);
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
        let smallHeight = 0;
        let largeHeight = 0;
        let tableHeight = 0;
        const i = this.vulnerabilityList.length || 0;
        if (this.$refs.scaTable) {
          const $main = document.getElementsByClassName('main-form');
          smallHeight = $main.length > 0 ? $main[0].clientHeight : 0;
          largeHeight = this.$refs.mainContainer
            ? this.$refs.mainContainer.clientHeight
            : 0;
          tableHeight = this.$refs.scaTable.$el.scrollHeight;
          this.screenHeight = i * 42 > tableHeight ? largeHeight - smallHeight - 62 : i * 42 + 43;
          this.screenHeight = this.screenHeight === 43 ? 336 : this.screenHeight;
        }
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
  marginpadding: 16px 20px 0 16px;
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
</style>
