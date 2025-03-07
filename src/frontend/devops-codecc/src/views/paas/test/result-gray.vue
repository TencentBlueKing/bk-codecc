<template>
  <div>
    <bk-collapse v-model="active">
      <bk-collapse-item name="1" :custom-trigger-area="true" ext-cls="resultClass" hide-arrow>
        <bk-icon :type="activeCollapse ? 'down-shape' : 'right-shape'" />
        <span class="text-[#313238] text-[14px] font-bold">{{ $t('发布结果') }}</span>
        <span slot="no-trigger" class="bk-link">
          <span style="cursor: default">{{ $t('数据更新时间') }}: {{ current.updatedDate | formatDate }}</span>
          <span class="border-x mx-[8px] h-[14px]"></span>
          <bk-link theme="primary" class="ml-[2px] mr-[16px]" icon="codecc-icon icon-refresh-2" @click="init">
            {{ $t('刷新数据') }}
          </bk-link>
          <bk-link theme="primary" icon="bk-icon icon-order" @click="openGrayReport(stage)">
            {{ $t('查看详细报告') }}
          </bk-link>
        </span>
        <div slot="content">
          <bk-table :data="tableData" v-if="!isEmpty" :header-row-style="{ height: '50px' }" :outer-border="false"
                    :header-border="false" v-bkloading="{ isLoading: loading }">
            <bk-table-column :label="$t('指标')" prop="metrics"></bk-table-column>
            <bk-table-column :label="$t('当前版本')" prop="current" :render-header="handleRenderCurrent">
              <template slot-scope="{ row }">
                <span v-html="row.current"></span>
                <template v-if="current.successCount">
                  <bk-icon v-if="row.isPass === true" class="text-[#2DCB56] !text-[14px]" type="check-circle-shape" />
                  <bk-icon
                    v-else-if="row.isPass === false" class="text-[#F5222D] !text-[14px]" type="close-circle-shape" />
                </template>
              </template>
            </bk-table-column>
            <bk-table-column :label="$t('建议值')" prop="suggestion"></bk-table-column>
          </bk-table>
          <bk-exception v-else type="empty" scene="part">
            <div>{{ $t('暂无结果') }}</div>
            <div class="text-[#979BA5] text-[12px] leading-[30px]">{{ $t('请先选择灰度项目，并创建 CodeCC 任务进行灰度') }}</div>
          </bk-exception>
        </div>
      </bk-collapse-item>
    </bk-collapse>
  </div>
</template>

<script>
import {bkCollapse, bkCollapseItem} from 'bk-magic-vue';
import spinner from '@/images/spinner.svg';

export default {
  name: 'ResultGray',
  components: {
    bkCollapse,
    bkCollapseItem,
  },
  data() {
    return {
      // 灰度发布
      stage: 3,
      spinner,
      active: ['1'],
      scrollHeight: 351,
      current: {
        toolName: '',
        version: '',
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
    activeCollapse() {
      console.log('🚀 ~ height ~ val:', this.scrollHeight);
      window.parent.postMessage({
        type: 'gray-result-height',
        data: this.scrollHeight,
      }, '*');
      this.scrollHeight = document.body.scrollHeight === 0 ? 42 : document.body.scrollHeight;
      return this.active.includes('1');
    },
    tableData() {
      return [
        {
          metrics: this.$t('已执行任务数'),
          current: `${this.current.successCount}/${this.current.successCount + this.current.failCount}`,
          suggestion: `>= ${this.threshold.taskNum}`,
        },
        {
          metrics: this.$t('执行成功率'),
          current: this.current.successCount !== undefined ? `${(this.current.successCount * 100 / (this.current.successCount + this.current.failCount)).toFixed(2)}%` : '--',
          suggestion: `>= ${this.threshold.successRate}`,
          isPass: (this.current.successCount * 100 / (this.current.successCount + this.current.failCount))
              >= this.threshold.successRate,
        },
        {
          metrics: this.$t('执行耗时均值'),
          current: this.current.costTime ? `${(this.current.costTime / this.current.codeCount).toFixed(2)} s/kloc` : '--',
          suggestion: `<= ${this.threshold.averCostTime} s/kloc`,
          isPass: (this.current.costTime / this.current.codeCount) <= this.threshold.averCostTime,
        },
        {
          metrics: this.$t('扫出问题总数'),
          current: this.current.defectCount ?? '--',
          suggestion: '--',
        },
        {
          metrics: this.$t('扫描代码总量'),
          current: this.current.codeCount ?? '--',
          suggestion: '--',
        },
        {
          metrics: this.$t('扫出问题密度'),
          current: this.current.defectCount ? `${(this.current.defectCount * 1000 / this.current.codeCount).toFixed(2)} /kloc` : '--',
          suggestion: `> ${this.threshold.defectDensity}`,
          isPass: (this.current.defectCount * 1000 / this.current.codeCount) > this.threshold.defectDensity,
        },
      ];
    },
  },

  async beforeMount() {
    await this.getThreshold();
    this.init();
  },
  methods: {
    async getThreshold() {
      this.threshold = await this.$store.dispatch('test/getThreshold');
    },
    async init() {
      this.loading = true;
      this.current = await this.getResult();
      // this.current 不为空对象时，isEmpty 为 false
      this.isEmpty = !Object.keys(this.current).length;
      this.current.isPass = this.tableData.every(item => item.isPass !== false);
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
    openGrayReport(stage) {
      const queryParam = {
        stage,
        currentVersion: this.current.version,
      };
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
        toolId}/test-report?type=gray&url=${fullUrl}`;
      console.log(href);
      window.open(v3Url, '_blank');
    },
  },
};
</script>
<style>
#app {
  min-width: 960px;
  height: 458px;
  background: #FFFFFF;
  box-shadow: 0 2px 4px 0 #1919290d;
  border-radius: 2px;
}
</style>
<style scoped>
.icon-right-shape, .icon-down-shape {
  font-size: large !important;
}
</style>
