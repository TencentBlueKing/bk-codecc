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
            active="sca-lic"
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
          <div class="cc-table">
            <bk-table
              :data="licenseList"
              :pagination="pagination"
              @page-change="handlePageChange"
              @row-click="handleClickRow"
              v-bkloading="{ isLoading: tableLoading, opacity: 0.6 }">
              <bk-table-column
                :label="$t('许可证名称')"
                show-overflow-tooltip
                min-width="100"
                prop="name">
                <template slot-scope="props">
                  <span class="purple-text">{{ props.row.name || '--' }}</span>
                </template>
              </bk-table-column>
              <bk-table-column
                :label="$t('许可证全名')"
                show-overflow-tooltip
                min-width="200"
                prop="fullName">
                <template slot-scope="props">
                  <span>{{ props.row.fullName || '--' }}</span>
                </template>
              </bk-table-column>
              <bk-table-column
                :label="$t('风险等级')"
                min-width="50"
                prop="severity"
                :filters="severityFilters"
                :filter-method="severityFilterMethod">
                <template slot-scope="props">
                  <SeverityStatus :cur-severity="props.row.severity" type="lic"></SeverityStatus>
                </template>
              </bk-table-column>
              <bk-table-column
                :label="$t('OSI 认证')"
                min-width="100"
                prop="osi">
                <template slot-scope="props">
                  <i class="bk-icon success-icon codecc-icon icon-check-line" v-if="props.row.osi" />
                  <i class="bk-icon fail-icon codecc-icon icon-close-line" v-else />
                </template>
              </bk-table-column>
              <bk-table-column
                :label="$t('FSF 许可')"
                min-width="100"
                prop="fsf">
                <template slot-scope="props">
                  <i class="bk-icon success-icon codecc-icon icon-check-line" v-if="props.row.fsf" />
                  <i class="bk-icon fail-icon codecc-icon icon-close-line" v-else />
                </template>
              </bk-table-column>
              <bk-table-column
                :label="$t('SPDX 认证')"
                min-width="100"
                prop="spdx">
                <template slot-scope="props">
                  <i class="bk-icon success-icon codecc-icon icon-check-line" v-if="props.row.spdx" />
                  <i class="bk-icon fail-icon codecc-icon icon-close-line" v-else />
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
    <div class="lic-list" v-else>
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
      v-model="licDetailDialogVisible"
      :ext-cls="'file-detail-dialog'"
      :fullscreen="isFullScreen"
      :position="{ top: `${isFullScreen ? 0 : 50}` }"
      :draggable="false"
      :mask-close="false"
      :show-footer="false"
      :close-icon="false"
      width="80%"
    >
      <scaLicDetail
        v-if="licDetailDialogVisible"
        :entity-id="curRow.entityId"
        :build-id="curRow.buildId"
        :license-name="curRow.name"
        @closeDetail="licDetailDialogVisible = false"
      ></scaLicDetail>
    </bk-dialog>
  </div>
</template>

<script>
import Empty from '@/components/empty';
import scaLicDetail from './sca-lic-detail.vue';
import SeverityStatus from '../components/severity-status.vue';
import scaUtil from '@/mixins/sca-list';

export default {
  components: {
    Empty,
    scaLicDetail,
    SeverityStatus,
  },
  mixins: [scaUtil],
  data() {
    return {
      searchParams: {
        scaDimensionList: ['LICENSE'],
      },
      listData: {
        licenseList: {
          records: [],
          count: 0,
        },
      },
      licDetailDialogVisible: false,
      mainContentLoading: false,
      tableLoading: false,
      contentLoading: false,
      curRow: {},
      severityFilters: [
        {
          text: this.$t('未知'),
          value: 0,
        },
        {
          text: this.$t('高危'),
          value: 1,
        },
        {
          text: this.$t('中危'),
          value: 2,
        },
        {
          text: this.$t('低危'),
          value: 4,
        },
      ],
      pagination: {
        current: 1,
        count: 0,
        limit: 10,
        showLimit: false,
      },
    };
  },
  computed: {
    licenseList() {
      return this.listData.licenseList.records || [];
    },
  },
  created() {
    this.contentLoading = true;
    this.fetchList();
    this.contentLoading = false;
  },
  methods: {
    // table row click callback
    handleClickRow(row) {
      this.licDetailDialogVisible = true;
      this.curRow = row;
    },
    handlePageChange(current) {
      this.pagination.current = current;
      this.fetchList();
    },
    // 获取table数据
    async fetchList() {
      this.tableLoading = true;
      try {
        const params = {
          ...this.basicSearchParams,
          ...this.searchParams,
          pageNum: this.pagination.current,
          pageSize: this.pagination.limit,
        };
        const res = await this.$store.dispatch('defect/lintList', params);
        if (res.licenseList) {
          this.listData.licenseList.records = res.licenseList.records;
          this.listData.licenseList.count = res.licenseList.count;
          this.pagination.count = res.licenseList.count;
        } else {
          // 清空数据
          this.listData.licenseList.records = [];
          this.listData.licenseList.count = 0;
          this.pagination.count = 0;
        }
      } catch (err) {

      } finally {
        this.tableLoading = false;
      }
    },
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
        pageNum: 1,
        pageSize: 300000,
      };
      this.$store
        .dispatch('defect/lintList', params)
        .then((res) => {
          const list = res && res.licenseList && res.licenseList.records;
          const headerProps = {
            index: this.$t('序号'),
            entityId: this.$t('entityId'),
            severity: this.$t('风险等级'),
            name: this.$t('许可证名称'),
            fullName: this.$t('许可证全名'),
            osi: this.$t('OSI 认证'),
            fsf: this.$t('FSF 认证'),
            spdx: this.$t('SPDX 认证'),
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
          const curSeverity = this.severityFilters.find(severity => severity.value === item[j]);
          return curSeverity.text;
        }
        if (['osi', 'fsf', 'spdx'].includes(j)) {
          return item[j] ? this.$t('是') : this.$t('否');
        }
        return item[j];
      }));
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

.file-detail-dialog {
  .bk-dialog {
    min-width: 1010px;
  }
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

.fail-icon {
  font-size: 22px;
  color: #e2646d;
}

.success-icon {
  font-size: 22px;
  color: #75ca87;
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

/deep/ .bk-dialog-header {
  padding: 3px 24px 0 !important;
}

/deep/ .bk-dialog-body {
  padding: 0;
}

.purple-text {
  font-weight: 500;
  color: #9e3e8f;
}
</style>

