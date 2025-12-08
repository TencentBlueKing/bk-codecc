<template>
  <div>
    <p class="pb-[24px] flex justify-between">
      <span class="text-[#313238] text-[14px] font-bold">{{ $t('测试结果') }}</span>
      <span class="bk-link">
        <span>{{ $t('数据更新时间') }}: {{ current.updatedDate | formatDate }}</span>
        <span class="border-x mx-[8px] h-[14px]"></span>
        <bk-link theme="primary" class="ml-[2px] mr-[16px]" icon="codecc-icon icon-refresh-2" @click="init">
          {{ $t('刷新数据') }}
        </bk-link>
        <bk-link theme="primary" icon="bk-icon icon-order" @click="openTestReport(stage)">
          {{ $t('查看详细报告') }}
        </bk-link>
      </span>
    </p>
    <bk-table
      v-if="!isEmpty"
      :data="tableData"
      :header-row-style="{ height: '50px' }"
      :row-class-name="({ row }) =>
        (current.successCount + current.failCount === current.taskCount && row.isPass === false) ? '!bg-[#FFEEEE]' : ''"
      v-bkloading="{ isLoading: loading }">
      <bk-table-column :label="$t('指标')" prop="metrics"></bk-table-column>
      <bk-table-column :label="$t('当前版本')" prop="current" :render-header="handleRenderCurrent">
        <template slot-scope="{ row }">
          <span v-html="row.current"></span>
          <template v-if="current.successCount + current.failCount === current.taskCount">
            <bk-icon v-if="row.isPass === true" class="text-[#2DCB56] !text-[14px]" type="check-circle-shape" />
            <bk-icon v-else-if="row.isPass === false" class="text-[#F5222D] !text-[14px]" type="close-circle-shape" />
          </template>
        </template>
      </bk-table-column>
      <bk-table-column :label="$t('对比版本')" prop="compare" :render-header="handleRenderCompare"></bk-table-column>
      <bk-table-column :label="$t('建议值')" prop="suggestion"></bk-table-column>
    </bk-table>
    <bk-exception v-else type="empty" scene="part">
      <div>{{ $t('暂无结果') }}</div>
      <div class="text-[#979BA5] text-[12px] leading-[30px]">{{ $t('请先选择测试项目，并创建 CodeCC 任务进行测试') }}</div>
    </bk-exception>
  </div>
</template>

<script>
import VersionSelect from './version-select.vue';
import TestWebSocket from '@/common/testWebSocket';
import spinner from '@/images/spinner.svg';

export default {
  props: {
    /**
     * 1: 指定测试， 2: 随机测试
     */
    stage: {
      type: Number,
      default: 1,
    },
  },
  data() {
    return {
      spinner,
      current: {
        toolName: '',
        version: '',
        taskCount: 0,
        successCount: 0,
        failCount: 0,
        costTime: 0,
        defectCount: 0,
        codeCount: 0,
      },
      compare: {
        toolName: '',
        version: '',
        taskCount: 0,
        successCount: 0,
        failCount: 0,
        costTime: 0,
        defectCount: 0,
        codeCount: 0,
      },
      threshold: {},
      testWs: null,
      loading: false,
      isEmpty: true,
    };
  },

  computed: {
    tableData() {
      return [
        {
          metrics: this.$t('已执行任务数'),
          current: this.current.taskCount
            ? (`${this.current.successCount + this.current.failCount}/${this.current.taskCount}${
              this.current.runningCount ? `<img src="${this.spinner}" class="w-[14px] text-[#3a84ff] inline-block ml-[10px] mt-[-4px] mr-[6px]">${this.$t('{0}个进行中', [this.current.runningCount])}` : ''}`)
            : '--',
          compare: this.compare.taskCount ? `${this.compare.successCount + this.compare.failCount}/${this.compare.taskCount}` : '--',
          suggestion: `>= ${this.threshold.taskNum}`,
        },
        {
          metrics: this.$t('执行成功率'),
          current: this.current.successCount !== undefined ? `${(this.current.successCount * 100 / this.current.taskCount).toFixed(2)}%` : '--',
          compare: this.compare.successCount !== undefined ? `${(this.compare.successCount * 100 / this.compare.taskCount).toFixed(2)}%` : '--',
          suggestion: `>= ${this.threshold.successRate}`,
          isPass: (this.current.successCount * 100 / this.current.taskCount) >= this.threshold.successRate,
        },
        {
          metrics: this.$t('执行耗时均值'),
          current: this.current.costTime ? `${(this.current.costTime / this.current.codeCount).toFixed(2)} s/kloc` : '--',
          compare: this.compare.costTime ? `${(this.compare.costTime / this.compare.codeCount).toFixed(2)} s/kloc` : '--',
          suggestion: `<= ${this.threshold.averCostTime} s/kloc`,
          isPass: (this.current.costTime / this.current.codeCount) <= this.threshold.averCostTime,
        },
        {
          metrics: this.$t('扫出问题总数'),
          current: this.current.defectCount ?? '--',
          compare: this.compare.defectCount ?? '--',
          suggestion: '--',
        },
        {
          metrics: this.$t('扫描代码总量'),
          current: this.current.codeCount ?? '--',
          compare: this.compare.codeCount ?? '--',
          suggestion: '--',
        },
        {
          metrics: this.$t('扫出问题密度'),
          current: this.current.defectCount ? `${(this.current.defectCount * 1000 / this.current.codeCount).toFixed(2)} /kloc` : '--',
          compare: this.compare.defectCount ? `${(this.compare.defectCount * 1000 / this.compare.codeCount).toFixed(2)} /kloc` : '--',
          suggestion: this.stage === 1 ? `> ${this.threshold.defectDensity}` : '--',
        },
      ];
    },
  },

  watch: {
    current(newVal, oldVal) {
      this.$emit('updateResult', newVal);
    },
  },

  async beforeMount() {
    await this.getThreshold();
    this.init();
    this.initWebSocket();
  },

  methods: {
    async getThreshold() {
      this.threshold = await this.$store.dispatch('test/getThreshold');
    },
    async init() {
      this.loading = true;
      this.current = await this.getResult();
      // 如果满足条件的任务数量 !== 任务数量，说明随机测试有降级
      if (this.current.eligibleCount && this.current.taskCount !== this.current.eligibleCount) {
        this.$bkMessage({
          theme: 'warning',
          message: this.$t('后台已搜索到{0}个符合要求的代码库，剩余{1}个代码库将降低代码规模进行搜索', [this.current.eligibleCount, this.current.taskCount - this.current.eligibleCount]),
          delay: 10000,
          limit: 1,
        });
      }
      // this.current 不为空对象时，isEmpty 为 false
      this.isEmpty = !Object.keys(this.current).length;
      const isPass = this.tableData.every(item => item.isPass !== false);
      this.current.isPass = isPass;
      this.loading = false;
    },
    handleRenderCurrent(h, { column }) {
      return h(
        'div',
        {
          class: 'leading-[20px]',
        },
        [
          h('span', column.label),
          h('p', {
            style: {
              color: '#63656E',
            },
          }, this.$route.query.version),
        ],
      );
    },
    handleRenderCompare(h, { column }) {
      return h(
        'div',
        {
          class: 'leading-[20px]',
        },
        [
          h('span', column.label),
          h(
            VersionSelect,
            {
              on: {
                change: data => this.handleChange(data),
              },
              props: {
                stage: this.stage,
              },
            },
          ),
        ],
      );
    },

    async handleChange(data) {
      this.loading = true;
      this.compare = await this.getResult(data);
      this.loading = false;
    },

    async getResult(version = this.$route.query.version) {
      if (!version) return {};
      const payload = {
        toolName: this.$route.params.toolName,
        version,
        stage: this.stage,
      };
      return await this.$store.dispatch('test/getResult', payload);
    },

    initWebSocket() {
      this.testWs = new TestWebSocket();
      const { toolName } = this.$route.params;
      const { version } = this.$route.query;
      const subscribe = `/topic/user/testStatus/${toolName}/${version}/${this.stage}`;

      this.testWs.connect(this.getSuggestion, subscribe, {
        success: (res) => {
          console.log('🚀@@@@@@ ~ initWebSocket ~ res:', res, res.body);
          // this.handleMessage(res.body);
          if (res.body.includes('TEST STATUS REPORT CONSUMER')) {
            this.init();
          }
        },
        error: message => console.error(message),
      });
    },

    openTestReport(stage) {
      const queryParam = {
        stage,
        currentVersion: this.current.version,
      };
      if (this.compare.version) {
        queryParam.comparisonVersion = this.compare.version;
      }
      const toolId = this.$route.params.toolName;
      const { href } = this.$router.resolve({
        name: 'paas-test-design-report',
        params: {
          toolName: toolId,
        },
        query: queryParam,
      });
      const fullUrl = encodeURIComponent(`${window.location.origin}${href}`);
      const v3Url = `${window.PAAS_V3_URL}/plugin-center/plugin/${window.PAAS_V3_APP}/${
        toolId}/test-report?type=test&url=${fullUrl}`;
      console.log(href);
      window.open(v3Url, '_blank');
    },
  },
};
</script>
