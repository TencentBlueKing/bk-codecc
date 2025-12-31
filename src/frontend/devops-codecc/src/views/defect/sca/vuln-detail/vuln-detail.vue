<template>
  <div class="detail-container">
    <div class="detail-header">
      <i class="back-icon codecc-icon icon-arrows-left-line" @click="handleCloseLicDetail"></i>
      <SeverityStatus :cur-severity="detail.severity" type="vuln"></SeverityStatus>
      <span class="vulnerability-name ml-3">{{ detail.name || '--' }}</span>
    </div>
    <div class="detail-content">
      <div class="detail-content-header-info">
        <div class="detail-content-header-info-text" v-for="(id, index) in detail.vulnerabilityIds" :key="index">
          {{ id }}
          <div class="vertical-line"></div>
        </div>
      </div>
      <div class="detail-content-items">
        <div class="vulnerability-title">
          <span class="vulnerability-title-text">{{ $t('漏洞描述') }}</span>
        </div>
        <div class="vulnerability-paragraph">
          {{ detail.message || '--' }}
        </div>
      </div>

      <div class="detail-content-items">
        <div class="vulnerability-title">
          <span class="vulnerability-title-text">{{ $t('修复建议') }}</span>
        </div>
        <SuggestionTable
          :affected-packages="detail?.affectedPackages || []"
        />
      </div>

      <div class="detail-content-items">
        <div class="vulnerability-title">
          <span class="vulnerability-title-text">{{ $t('CVSS 评分') }}</span>
        </div>
        <div class="custom-tab">
          <div
            :class="['tab-items', activeTab === 0 ? 'active-tab-items' : '']"
            @click="handleChangeTab(0)">
            CVSS V3
          </div>
          <div
            :class="['tab-items', activeTab === 1 ? 'active-tab-items' : '']"
            @click="handleChangeTab(1)">
            CVSS V2
          </div>
        </div>

        <div class="charts-container">
          <div class="charts-info">
            <div class="charts-info-label">{{ $t('攻击向量') }}</div>
            <div class="charts-info-text">
              {{ curCVSSVal || '--' }}
            </div>
            <div
              class="inline-block h-[22px] leading-[22px] rounded-[2px] px-[8px] text-[#fff] text-[12px]"
              :class="scoreRatingColorMap[curCVSScore.rating]">
              <span>{{ curCVSScore.score }}</span>
              <span>{{ curCVSScore.rating }}</span>
            </div>
          </div>

          <div class="charts-area">
            <div class="charts-area-usability">
              <div ref="radarChart" style="width: 600px; height: 400px;"></div>
            </div>
            <div class="charts-area-influence">
              <div ref="radarChart2" style="width: 600px; height: 400px;"></div>
            </div>
          </div>
        </div>
      </div>

      <div class="detail-content-items">
        <div class="vulnerability-title">
          <span class="vulnerability-title-text">{{ $t('参考网址') }}</span>
        </div>
        <SuggestionUrl
          :source="detail?.source || []"
        />
      </div>
    </div>
  </div>
</template>

<script>
import echarts from 'echarts/lib/echarts';
import 'echarts/lib/chart/radar';
import 'echarts/lib/component/tooltip';
import 'echarts/lib/component/title';
import 'echarts/lib/component/legend';
import SeverityStatus from '../../components/severity-status.vue';
import SuggestionTable from './components/suggestion-table.vue';
import SuggestionUrl from './components/suggestion-url.vue';
export default {
  components: {
    SeverityStatus,
    SuggestionTable,
    SuggestionUrl,
  },
  props: {
    entityId: {
      type: String,
      default: '',
    },
    buildId: {
      type: String,
      default: '',
    },
    toolName: {
      type: String,
      default: '',
    },
  },
  data() {
    return {
      searchParams: {
        // data
        taskId: this.$route.params.taskId,
        toolName: this.toolName,
        dimension: 'SCA',
        entityId: this.entityId,
        buildId: this.buildId,
        pattern: 'SCA',
        scaDimensionList: ['VULNERABILITY'],

        // query
        sortField: 'SEVERITY', // 按严重程度排序
        sortType: 'ASC', // 排序方式
      },
      lintDetail: {
        scaVulnerabilityDetailVO: {},
      },
      radarChart: null,
      radarChart2: null,
      chart: null,
      chart2: null,
      activeTab: 0,
      exploitability: [],
      impact: [],
    };
  },
  computed: {
    detail() {
      return this.lintDetail.scaVulnerabilityDetailVO;
    },
    curCVSSVal() {
      if (this.activeTab === 0) {
        return this.detail?.cvssV3 ? this.detail.cvssV3?.vector : '';
      }
      return this.detail?.cvssV2 ? this.detail.cvssV2?.vector : '';
    },
    curCVSScore() {
      let score = 0;
      if (this.activeTab === 0) {
        score =  this.detail?.cvssV3 ? this.detail.cvssV3?.score : 0;
      } else {
        score = this.detail?.cvssV2 ? this.detail.cvssV2?.score : 0;
      }
      return {
        score,
        rating: this.getScoreRating(score),
      };
    },
    CVSS3VectorMap() {
      return {
        AV: { N: 1, A: 2, L: 3, P: 4 },
        AC: { L: 1, H: 2 },
        PR: { N: 1, L: 2, H: 3 },
        UI: { N: 0, R: 1 },
        S: { U: 1, C: 2 },
        C: { N: 1, L: 2, H: 3 },
        I: { N: 1, L: 2, H: 3 },
        A: { N: 1, L: 2, H: 3 },
      };
    },
    CVSS2VectorMap() {
      return {
        AV: { L: 1, A: 2, N: 3 },
        AC: { H: 1, M: 2, L: 3 },
        Au: { M: 1, S: 2, N: 3 },
        C: { N: 1, P: 2, C: 3 },
        I: { N: 1, P: 2, C: 3 },
        A: { N: 1, P: 2, C: 3 },
      };
    },
    CVSS3Indicator() {
      return [
        { name: this.$t('攻击向量'), max: 4 },
        { name: this.$t('攻击复杂度'), max: 3 },
        { name: this.$t('所需特权'), max: 3 },
        { name: this.$t('用户交互'), max: 2 },
        { name: this.$t('范围'), max: 2 },
      ];
    },
    CVSS2Indicator() {
      return [
        { name: this.$t('攻击向量'), max: 3 },
        { name: this.$t('攻击复杂度'), max: 3 },
        { name: this.$t('认证'), max: 3 },
      ];
    },
    exploitabilityChartOptions() {
      return {
        title: {
          text: this.$t('可利用性指标'),
        },
        radar: {
          name: {
            textStyle: {
              color: '#fff',
              backgroundColor: '#999',
              borderRadius: 3,
              padding: [3, 5],
            },
          },
          indicator: this.activeTab === 0 ? this.CVSS3Indicator : this.CVSS2Indicator,
        },
        series: [{
          name: '',
          type: 'radar',
          symbol: 'circle', // 设置数据点的形状为圆形
          symbolSize: 8, // 设置数据点的大小
          data: [
            {
              value: this.exploitability,
              name: '',
              lineStyle: {
                color: '#f0af59',
              },
              areaStyle: {
                color: '#f4e3e1b3',
              },
              itemStyle: {
                color: '#f0b04f',
              },
            },
          ],
        }],
      };
    },
    impactChartOption() {
      return {
        title: {
          text: this.$t('影响性指标'),
        },
        radar: {
          shape: 'circle',
          name: {
            textStyle: {
              color: '#fff',
              backgroundColor: '#999',
              borderRadius: 3,
              padding: [3, 5],
            },
          },
          indicator: [
            { name: this.$t('保密性影响'), max: 3 },
            { name: this.$t('完整性影响'), max: 3 },
            { name: this.$t('可用性影响'), max: 3 },
          ],
        },
        series: [{
          name: '',
          type: 'radar',
          symbol: 'circle', // 设置数据点的形状为圆形
          symbolSize: 8, // 设置数据点的大小
          data: [
            {
              value: this.impact,
              name: '',
              lineStyle: {
                color: '#f0af59',
              },
              areaStyle: {
                color: '#f4e3e1b3',
              },
              itemStyle: {
                color: '#f0b04f',
              },
            },
          ],
        }],
      };
    },
    scoreRatingColorMap() {
      return {
        Critical: 'bg-[#910A0A]',
        High: 'bg-[#D2634D]',
        Medium: 'bg-[#F5A21F]',
        Low: 'bg-[#E3CA0F]',
        None: 'bg-[#C4C6CC]',
      };
    },
  },
  created() {
    this.fetchLintDetail();
  },
  mounted() {
    this.initRadar();
  },
  methods: {
    handleChangeTab(value) {
      this.activeTab = value;
      if (value === 0) {
        this.updateChart(3);
      } else {
        this.updateChart(2);
      }
    },
    handleCloseLicDetail() {
      this.$emit('closeDetail');
    },
    initRadar() {
      this.chart = echarts.init(this.$refs.radarChart);
      this.chart.setOption(this.exploitabilityChartOptions);
      // 监听窗口大小变化，重绘图表
      window.addEventListener('resize', () => {
        this.chart.resize();
      });
      this.chart2 = echarts.init(this.$refs.radarChart2);
      this.chart2.setOption(this.impactChartOption);
      // 监听窗口大小变化，重绘图表
      window.addEventListener('resize', () => {
        this.chart2.resize();
      });
    },
    updateChart(type = 3) {
      if (type === 3) {
        const cvss3Value = this.calculateCVSSScore(type);
        this.exploitability = cvss3Value.slice(0, 5);
        this.impact = cvss3Value.slice(5);
      } else {
        const cvss2Value = this.calculateCVSSScore(type);
        this.exploitability = cvss2Value.slice(0, 5);
        this.impact = cvss2Value.slice(5);
      }
      this.chart.setOption(this.exploitabilityChartOptions);
      this.chart2.setOption(this.impactChartOption);
    },
    calculateCVSSScore(type) {
      let vector = null;
      let vectorMap = null;
      if (type === 3) {
        vectorMap = this.CVSS3VectorMap;
        vector = this.detail?.cvssV3?.vector || '';
      } else {
        vectorMap = this.CVSS2VectorMap;
        vector = this.detail?.cvssV2?.vector || '';
      }
      if (!vector || typeof vector !== 'string') {
        return [];
      }
      const vectorArray = vector.split('/');
      if (!Array.isArray(vectorArray) || vectorArray.length === 0) {
        return [];
      }
      vectorArray.splice(0, 1);
      return vectorArray.map((item) => {
        if (typeof item !== 'string') {
          return null;
        }
        const parts = item.split(':');
        if (parts.length < 2) {
          return null;
        }
        const [key, value] = parts;
        if (vectorMap && vectorMap[key] && vectorMap[key][value] !== undefined) {
          return vectorMap[key][value];
        }
        return null;
      }).filter(score => score !== null); // 可选：过滤掉解析失败的项，返回有效分数数组；如果希望保留null可去掉这行
    },
    getScoreRating(score) {
      if (score >= 9) {
        return 'Critical';
      } if (score >= 7) {
        return 'High';
      } if (score >= 4) {
        return 'Medium';
      } if (score >= 0.1) {
        return 'Low';
      }
      return 'None';
    },
    onBeforeUnmount() {
      // 组件销毁前移除事件监听
      window.removeEventListener('resize', () => {
        if (this.chart) {
          this.chart.resize();
        }
        if (this.chart2) {
          this.chart2.resize();
        }
      });
    },
    async fetchLintDetail() {
      const res = await this.$store.dispatch('defect/lintDetail', this.searchParams);
      this.lintDetail.scaVulnerabilityDetailVO = res.scaVulnerabilityDetailVO;
      this.updateChart();
    },
  },
};
</script>

<style lang="postcss" scoped>

.detail-container {
  width: 100%;
  height: 100%;
  background-color:#f8fafb;

  .detail-header {
    padding: 10px;
    background-color: #fff;
    border: 1px solid #f3f3f4;

    .back-icon {
      margin-right: 5px;
      font-size: 22px;
      cursor: pointer;
    }

    .vulnerability-name {
      font-weight: bold;
    }
  }

  .detail-content {
    padding: 10px;
    padding-right: 20px;
    padding-left: 20px;
    margin-top: 15px;
    background-color: #fff;
    border: 1px solid #f3f3f4;

    .detail-content-header-info {
      .detail-content-header-info-text {
        display: inline-block;
        margin-bottom: 30px;
      }

      .vertical-line {
        display: inline-block;
        width: 1px;
        height: 16px;
        margin: 0 15px;
        line-height: 26px;
        vertical-align: middle;
        cursor: auto;
        background: #f3f3f4;
      }
    }

    .detail-content-items {
      &:last-child {
        padding-bottom: 24px;
      }

      .vulnerability-title {
        display: flex;
        align-items: center;
        margin-bottom: 20px;

        .vulnerability-title-text {
          font-weight: bold;
          color: #000;
        }

        &::before {
          display: inline-block;
          width: 3px; /* 竖线的宽度 */
          height: 15px; /* 竖线的高度 */
          margin-right: 5px;
          background-color: #9e3e8f; /* 竖线的颜色 */
          content: '';
        }
      }

      .vulnerability-paragraph {
        padding: 10px;
        margin-bottom: 30px;
        color: #000;
        background-color: #f8fafb;
      }

      .custom-tab {
        display: flex;
        margin-bottom: 20px;

        .tab-items {
          padding: 2px 8px;
          color: #000;
          cursor: pointer;
          background-color: #fff;

          &:first-child {
            border: 1px solid #dbdbdb;
            border-right: none;
            border-radius: 5px 0 0 5px;
          }

          &:last-child {
            border: 1px solid #dbdbdb;
            border-left: none;
            border-radius: 0 5px 5px 0;
          }
        }

        .active-tab-items {
          color: #fff;
          background-color: #9e3e8f;
        }
      }

      .charts-container {
        .charts-info {
          display: flex;
          color: #000;

          .charts-info-label {
            margin-right: 10px;
            font-weight: bold;
          }

          .charts-info-text {
            margin-right: 5px;
          }
        }

        .charts-area {
          display: flex;
          margin-top: 20px;
          margin-bottom: 30px;
          justify-content: space-between;

          .charts-area-usability {
            width: 49%;
            padding: 20px;
            box-shadow: 0 0 0 1px #f3f3f4;
          }

          .charts-area-influence {
            width: 49%;
            padding: 20px;
            box-shadow: 0 0 0 1px #f3f3f4;
          }
        }
      }
    }
  }

  .color-purple {
    color: #9e3e8f;
  }
}
</style>
