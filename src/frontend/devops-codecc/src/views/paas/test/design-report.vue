<template>
  <div class="page-width">
    <bk-tab :active.sync="active" type="card-tab" style="margin-top: 2px;" @tab-change="changeTab">
      <bk-tab-panel v-for="(panel, index) in panels" v-bind="panel" :key="index"></bk-tab-panel>
    </bk-tab>
    <div style="margin-top: -22px;">
      <bk-form :model="searchForm" :rules="rules" ref="searchForm">
        <div class="form-container">
          <div>
            <bk-form-item :label="active === '3' ? $t('查看灰度版本') : $t('当前版本')" :required="true">
              <bk-select v-model="searchForm.currentVersion" style="width: 200px;">
                <bk-option v-for="option in versionList" :key="option" :id="option" :name="option"></bk-option>
              </bk-select>
            </bk-form-item>
          </div>
          <div v-if="active === '3'">
            <bk-form-item :label="$t('已发布版本') + ':'" label-width="170">
              <span>{{toolReleasedVersion}}</span>
            </bk-form-item>
          </div>
          <div v-else>
            <bk-form-item :label="$t('对比版本')" label-width="170">
              <bk-select v-model="searchForm.comparisonVersion" style="width: 200px;">
                <bk-option v-for="option in versionList" :key="option" :id="option" :name="option"></bk-option>
              </bk-select>
            </bk-form-item>
          </div>
          <div style="min-width: 210px;height: 32px; position: relative;flex: 1">
            <bk-button
              theme="primary"
              style="position: absolute;right: 16px"
              @click="downloadExcel"
              :disabled="isExporting">
              {{$t('导出Excel')}}
            </bk-button>
          </div>
        </div>
        <div style="width: 100%;height: 10px;margin-top: 12px;background-color: #F5F6FA"></div>
        <div class="form-container" style="margin-top: 10px">
          <div>
            <bk-form-item :label="$t('任务状态')">
              <bk-checkbox-group v-model="searchForm.taskStatusArr">
                <bk-checkbox :value="'1'">{{$t('成功')}}</bk-checkbox>
                <bk-checkbox :value="'2'">{{$t('失败')}}</bk-checkbox>
                <bk-checkbox :value="'4'">{{$t('进行中')}}</bk-checkbox>
              </bk-checkbox-group>
            </bk-form-item>
          </div>
          <div>
            <bk-form-item :label="$t('耗时变化')" v-if="active !== '3'">
              <bk-checkbox-group v-model="searchForm.costTimeDiffArr">
                <bk-checkbox :value="'1'">{{$t('上升')}}</bk-checkbox>
                <bk-checkbox :value="'2'">{{$t('下降')}}</bk-checkbox>
              </bk-checkbox-group>
            </bk-form-item>
          </div>
          <div>
            <bk-form-item :label="$t('问题数变化')" v-if="active !== '3'">
              <bk-checkbox-group v-model="searchForm.defectCountDiffArr">
                <bk-checkbox :value="'1'">{{$t('上升')}}</bk-checkbox>
                <bk-checkbox :value="'2'">{{$t('下降')}}</bk-checkbox>
              </bk-checkbox-group>
            </bk-form-item>
          </div>
          <!-- 这里特殊处理, active = 3时,上面的隐藏了,需要把执行时间挪上这里 -->
          <div v-if="active === '3'">
            <bk-form-item :label="$t('执行时间')" :property="'range'" :error-display-type="'normal'">
              <bk-date-picker
                :timer="false" :placeholder="generateDateRange()" v-model="searchForm.dateRange"
                :type="'daterange'" :editable="true" style="width: 240px"></bk-date-picker>
            </bk-form-item>
          </div>
        </div>
        <div style="margin-top: 13px" v-if="active !== '3'">
          <bk-form-item :label="$t('执行时间')" :property="'range'" :error-display-type="'normal'">
            <bk-date-picker
              :timer="false" :placeholder="generateDateRange()" v-model="searchForm.dateRange"
              :type="'daterange'" :editable="true" style="width: 240px"></bk-date-picker>
          </bk-form-item>
        </div>
      </bk-form>
    </div>
    <div class="table-wrapper" :style="{ height: tableAutoHeight + 'px' }">
      <bk-table
        :data="testReportList"
        :pagination="pagination" @page-change="handlePageChange" @page-limit-change="handlePageLimitChange">
        <bk-table-column :label="$t('项目')" show-overflow-tooltip>
          <template slot-scope="{ row }">
            {{ row.projectName ? row.projectName : row.projectId }}
          </template>
        </bk-table-column>
        <bk-table-column :label="$t('任务')" show-overflow-tooltip prop="nameCn">
          <template slot-scope="{ row }">
            <bk-link theme="primary" @click="openTaskOverview(row)">
              {{ row.nameCn }}
            </bk-link>
          </template>
        </bk-table-column>
        <bk-table-column :label="$t('代码库')" show-overflow-tooltip prop="repo">
          <template slot-scope="{ row }">
            <bk-link theme="primary" @click="openGitRepo(row)">
              {{ row.repo }}
            </bk-link>
          </template>
        </bk-table-column>
        <bk-table-column :label="$t('状态')" prop="status">
          <template slot-scope="props">
            <span class="dot" :class="`status-${props.row.status}`"></span>
            <span>{{ statusMap[props.row.status] }}</span>
          </template>
        </bk-table-column>
        <bk-table-column :label="$t('当前版本耗时')" prop="currentCostTime">
          <template slot-scope="props">
            <span>{{ props.row.currentCostTime ? formatMicroseconds(props.row.currentCostTime) : '--' }}</span>
          </template>
        </bk-table-column>
        <bk-table-column v-if="active !== '3'" :label="$t('对比版本耗时')" prop="comparisonCostTime">
          <template slot-scope="props">
            <span>{{ props.row.comparisonCostTime ? formatMicroseconds(props.row.comparisonCostTime) : '--' }}</span>
          </template>
        </bk-table-column>
        <bk-table-column :label="$t('耗时变化')" prop="diffCostTime" v-if="active !== '3'">
          <template slot-scope="props">
            <i
              class="bk-icon" :class="{
                'icon-arrows-up up':
                  Math.round(props.row.diffCostTime / 1000) >= 1,
                'icon-arrows-down down':
                  Math.round(props.row.diffCostTime / 1000) <= -1,
              }"></i>
            <span>{{ props.row.diffCostTime === undefined ? '--' : formatMicroseconds(props.row.diffCostTime) }}</span>
          </template>
        </bk-table-column>
        <bk-table-column :label="$t('当前版本问题数')" prop="currentDefectCount">
          <template slot-scope="props">
            <span>{{ props.row.currentDefectCount === undefined ? '--' : props.row.currentDefectCount }}</span>
          </template>
        </bk-table-column>
        <bk-table-column v-if="active !== '3'" :label="$t('对比版本问题数')" prop="comparisonDefectCount">
          <template slot-scope="props">
            <span>{{ props.row.comparisonDefectCount === undefined ? '--' : props.row.comparisonDefectCount }}</span>
          </template>
        </bk-table-column>
        <bk-table-column :label="$t('问题数变化')" prop="diffDefectCount" v-if="active !== '3'">
          <template slot-scope="props">
            <i
              class="bk-icon" :class="{
                'icon-arrows-up up':
                  props.row.diffDefectCount > 0,
                'icon-arrows-down down':
                  props.row.diffDefectCount < 0,
              }"></i>
            <span>{{ props.row.diffDefectCount === undefined ? '--' : props.row.diffDefectCount }}</span>
          </template>
        </bk-table-column>
        <bk-table-column :label="$t('执行时间')">
          <template slot-scope="props">
            <span>{{ props.row.executeDate === undefined ? '--'
              : formatDate(props.row.executeDate, 'yyyy-MM-dd HH:mm:ss') }}</span>
          </template>
        </bk-table-column>
      </bk-table>
    </div>
  </div>
</template>

<script>
import { format } from 'date-fns';
// eslint-disable-next-line
import { export_json_to_excel } from '@/vendor/export2Excel';

export default {
  name: 'DesignReport',
  data() {
    const { toolName } = this.$route.params;
    return {
      active: '',
      versionList: [],
      isExporting: false,
      testReportList: [],
      toolReleasedVersion: '--',
      searchForm: {
        toolName,
        currentVersion: '',
        comparisonVersion: '',
        taskStatusArr: [],
        costTimeDiffArr: [],
        defectCountDiffArr: [],
        dateRange: [],
      },
      panels: [
        { name: '1', label: this.$t('指定测试报告') },
        { name: '2', label: this.$t('随机测试报告') },
        { name: '3', label: this.$t('灰度发布报告') },
      ],
      statusMap: {
        1: this.$t('成功'),
        2: this.$t('失败'),
        4: this.$t('进行中'),
      },
      pagination: {
        current: 1,
        count: 1,
        limit: 10,
      },
      rules: {
      },
    };
  },
  computed: {
    tableAutoHeight() {
      return window.innerHeight - 230;
    },
  },
  watch: {
    searchForm: {
      handler(newVal, oldVal) {
        this.pagination.current = 1;
        const searchParams = { ...this.getSearchParams(), pageNum: 1, pageSize: this.pagination.limit };
        this.queryTestReportDetailList(searchParams);
      },
      deep: true,
    },
    active(value) {
      if (value) {
        const searchParams = { ...this.getSearchParams(), pageNum: 1, pageSize: this.pagination.limit };
        this.queryTestReportDetailList(searchParams);
      }
    },
  },
  created() {
    const { query } = this.$route;
    this.active = query && query.stage ? query.stage : '1';
    this.queryTestVersions();

    this.searchForm.currentVersion = query && query.currentVersion ? query.currentVersion : '';
    this.searchForm.comparisonVersion = query && query.comparisonVersion ? query.comparisonVersion : '';
    if (this.searchForm.currentVersion) {
      const searchParams = { ...this.getSearchParams(), pageNum: 1, pageSize: this.pagination.limit };
      this.queryTestReportDetailList(searchParams);
    }
  },
  methods: {
    async queryTestReportDetailList(searchParams) {
      if (!searchParams.currentVersion) {
        this.testReportList = [];
        return;
      }
      const finalParam = { toolName: this.searchForm.toolName, stage: this.active, params: searchParams };
      this.$store.dispatch('test/getTestReportDetailList', finalParam)
        .then((res) => {
          this.testReportList = res;
        });
      this.$store.dispatch('test/getTestReportDetailCount', finalParam)
        .then((res) => {
          this.pagination.count = res;
        });
    },
    queryTestVersions() {
      this.$store.dispatch('test/listVersion', { toolName: this.searchForm.toolName, stage: this.active })
        .then((res) => {
          this.versionList = res || [];

          if (!this.versionList.includes(this.searchForm.currentVersion)) {
            this.searchForm.currentVersion = '';
          }
          if (!this.versionList.includes(this.searchForm.comparisonVersion)) {
            this.searchForm.comparisonVersion = '';
          }
        });
      if (this.active === '3') {
        this.queryToolLatestVersion();
      }
    },
    queryToolLatestVersion() {
      this.$store.dispatch('test/getToolLatestVersion', { toolName: this.searchForm.toolName }).then((res) => {
        this.toolReleasedVersion = res;
      });
    },
    changeTab(name) {
      this.pagination.current = 1;
      this.queryTestVersions();
    },
    formatDate(date, token = 'yyyy-MM-dd', options = {}) {
      return date ? format(Number(date), token, options) : '';
    },
    arrayToNum(arr) {
      return arr.reduce((acc, curr) => acc + Number(curr), 0);
    },
    // 统一从这获取查询参数
    getSearchParams() {
      const { dateRange, costTimeDiffArr, taskStatusArr, defectCountDiffArr, ...others } = this.searchForm;
      // 处理日期范围
      const [startTime, endTime] = dateRange.map(date => (date ? date.getTime() : null));
      const costTimeDiff = this.arrayToNum(costTimeDiffArr);
      const taskStatus = this.arrayToNum(taskStatusArr);
      const defectCountDiff = this.arrayToNum(defectCountDiffArr);
      return {
        ...others,
        ...(dateRange[0] && { startTime, endTime }),
        ...(costTimeDiff && { costTimeDiff }),
        ...(taskStatus && { taskStatus }),
        ...(defectCountDiff && { defectCountDiff }),
      };
    },
    generateDateRange() {
      const endDate = new Date();
      const startDate = new Date();
      startDate.setDate(startDate.getDate() - 6);
      return `如：${this.formatDate(startDate)} - ${this.formatDate(endDate)}`;
    },
    downloadExcel() {
      this.isExporting = true;
      const searchParams = this.getSearchParams();
      searchParams.pageSize = 50000;
      const finalParam = { toolName: this.searchForm.toolName, stage: this.active, params: searchParams };
      this.$store.dispatch('test/getTestReportDetailList', finalParam).then((res) => {
        console.log('export excel line: ', res.length);
        this.generateExcel(res);
      });
      this.isExporting = false;
    },
    generateExcel(list = []) {
      const tHeader = [
        this.$t('项目'),
        this.$t('任务'),
        this.$t('代码库'),
        this.$t('状态'),
        this.$t('当前版本耗时'),
        this.$t('对比版本耗时'),
        this.$t('耗时变化'),
        this.$t('当前版本问题数'),
        this.$t('对比版本问题数'),
        this.$t('问题数变化'),
      ];
      const filterVal = [
        'projectName',
        'nameCn',
        'repo',
        'status',
        'currentCostTime',
        'comparisonCostTime',
        'diffCostTime',
        'currentDefectCount',
        'comparisonDefectCount',
        'diffDefectCount',
      ];
      const data = this.formatJson(filterVal, list);
      const reportTypeName = this.panels.find(panel => panel.name === this.active).label;
      const title = `${this.searchForm.toolName}-${reportTypeName}-${new Date().toISOString()}`;
      export_json_to_excel(tHeader, data, title);
    },
    formatJson(filterVal, list) {
      return list.map(item => filterVal.map((j) => {
        if (j === 'status') {
          return this.statusMap[item.status];
        }
        if (j === 'currentCostTime' || j === 'comparisonCostTime' || j === 'diffCostTime') {
          return item[j] ? this.formatMicroseconds(item[j]) : '--';
        }
        if (j === 'currentDefectCount' || j === 'comparisonDefectCount' || j === 'diffDefectCount') {
          return item[j] !== undefined ? item[j] : '--';
        }
        return item[j];
      }));
    },
    formatMicroseconds(microseconds) {
      microseconds = Math.abs(microseconds);
      const totalSeconds = Math.round(microseconds / 1000);
      const hours = Math.floor(totalSeconds / 3600);
      const minutes = Math.floor((totalSeconds % 3600) / 60);
      const seconds = totalSeconds % 60;
      if (hours === 0) {
        if (minutes === 0) {
          return `${seconds} s`;
        }
        if (seconds === 0) {
          return `${minutes} min`;
        }
        return `${minutes} min ${seconds} s`;
      }
      return `${hours} h ${minutes} min ${seconds} s`;
    },
    handlePageChange(page) {
      this.pagination.current = page;
      const searchParams = { ...this.getSearchParams(), pageNum: page, pageSize: this.pagination.limit };
      this.queryTestReportDetailList(searchParams);
    },
    handlePageLimitChange(currentLimit) {
      this.pagination.current = 1;
      this.pagination.limit = currentLimit;
      const searchParams = { ...this.getSearchParams(), pageNum: 1, pageSize: currentLimit };
      this.queryTestReportDetailList(searchParams);
    },
    openTaskOverview(row) {
      const url = `${window.location.origin}/codecc/${row.projectId}/task/${row.taskId}/detail`;
      window.open(url, '_blank');
    },
    openGitRepo(row) {
      window.open(row.repo, '_blank');
    },
  },
};

</script>

<style>
#app {
  min-width: 960px;
}
</style>
<style scoped>
.page-width {
  min-width: 960px;
  padding: 1px 5px 1px 5px;
}

.table-wrapper {
  margin-top: 13px;
  overflow: auto;

  /deep/ .bk-link {
    display: block !important;

    .bk-link-text {
      font-size: 12px;
    }
  }
}

.form-container {
  display: flex;
}

/deep/ .bk-form-checkbox .bk-checkbox-text {
  margin: 0 15px 0 6px;
}

.dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-right: 4px;
  background: #F0F1F5;
  border: 1px solid #C4C6CC;
  border-radius: 50%;
}

.status-1 {
  background: #E5F6EA;
  border: 1px solid #3FC06D;
}

.status-2 {
  background: #FFE8C3;
  border: 1px solid red;
}

.bk-icon {
  position: absolute;
  font-size: 27px;
}

.up {
  margin: -4px 0 0 -22px;
  color: #ff5656;
}

.down {
  margin: -4px 0 0 -22px;
  color: #2dcb56;
}

</style>
