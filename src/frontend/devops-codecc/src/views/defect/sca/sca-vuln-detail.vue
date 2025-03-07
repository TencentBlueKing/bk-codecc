<template>
  <div class="detail-container">
    <div class="detail-header">
      <i class="back-icon codecc-icon icon-arrows-left-line" @click="handleCloseLicDetail"></i>
      <SeverityStatus :cur-severity="detail.severity" type="vuln"></SeverityStatus>
      <span class="vulnerability-name ml-3">{{ detail.vulName || '-' }}</span>
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
          {{ isEn ? (detail.descriptionEn || '-') : (detail.description || '-') }}
        </div>
      </div>

      <div class="detail-content-items">
        <div class="vulnerability-title">
          <span class="vulnerability-title-text">{{ $t('修复建议') }}</span>
        </div>
        <div class="vulnerability-paragraph">
          {{ detail.suggestion || '-' }}
        </div>
      </div>

      <div class="detail-content-items">
        <div class="vulnerability-title">
          <span class="vulnerability-title-text">{{ $t('CVSS 评分') }}</span>
        </div>
        <div class="custom-tab" @click="handleChangeTab">
          <div id="cvss-v3" :class="['tab-items', activeTab === 'cvss-v3' ? 'active-tab-items' : '']">CVSS V3</div>
          <div id="cvss-v2" :class="['tab-items', activeTab === 'cvss-v2' ? 'active-tab-items' : '']">CVSS V2</div>
        </div>

        <div class="charts-container">
          <div class="charts-info">
            <div class="charts-info-label">{{ $t('攻击向量') }}</div>
            <div class="charts-info-text">
              {{ curCVSSVal || '-' }}
            </div>
            <bk-tag theme="warning" type="filled">
              {{ `${curCVSScore} MEDIUM` }}
            </bk-tag>
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
    </div>
  </div>
</template>

<script>
import echarts from 'echarts/lib/echarts';
import 'echarts/lib/chart/radar';
import 'echarts/lib/component/tooltip';
import 'echarts/lib/component/title';
import 'echarts/lib/component/legend';
import SeverityStatus from '../components/severity-status.vue';
export default {
  components: {
    SeverityStatus,
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
  },
  data() {
    return {
      searchParams: {
        // data
        taskId: this.$route.params.taskId,
        toolName: 'PECKER_SCA',
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
      activeTab: 'cvss-v3',
    };
  },
  computed: {
    detail() {
      return this.lintDetail.scaVulnerabilityDetailVO;
    },
    curCVSSVal() {
      if (this.activeTab === 'cvss-v3') {
        return this.detail.cvss_v3 ? this.detail.cvss_v3.split('BS:')[0] : '';
      }
      return this.detail.cvss_v2 ? this.detail.cvss_v2.split('BS:')[0] : '';
    },
    curCVSScore() {
      if (this.activeTab === 'cvss-v3') {
        return this.detail.cvss_v3 ? this.detail.cvss_v3.split('BS:')[1] : '';
      }
      return this.detail.cvss_v2 ? this.detail.cvss_v2.split('BS:')[1] : '';
    },
  },
  created() {
    this.fetchLintDetail();
  },
  mounted() {
    this.initRadar();
  },
  methods: {
    handleChangeTab(e) {
      this.activeTab = e.target.id;
      // 后续需要补充 切换tab对应切换echarts
    },
    handleCloseLicDetail() {
      this.$emit('closeDetail');
    },
    initRadar() {
      this.chart = echarts.init(this.$refs.radarChart);
      const option = {
        title: {
          text: this.$t('可利用性指标'),
        },
        tooltip: {},
        radar: {
          // shape: 'circle',
          name: {
            textStyle: {
              color: '#fff',
              backgroundColor: '#999',
              borderRadius: 3,
              padding: [3, 5],
            },
          },
          indicator: [
            { name: this.$t('攻击向量'), max: 6500 },
            { name: this.$t('攻击复杂度'), max: 16000 },
            { name: this.$t('所需特权'), max: 30000 },
            { name: this.$t('用户交互'), max: 38000 },
            { name: this.$t('范围'), max: 52000 },
          ],
        },
        series: [{
          name: '',
          type: 'radar',
          symbol: 'circle', // 设置数据点的形状为圆形
          symbolSize: 8, // 设置数据点的大小
          data: [
            {
              value: [1500, 16000, 7500, 15000, 20000],
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
      this.chart.setOption(option);
      // 监听窗口大小变化，重绘图表
      window.addEventListener('resize', () => {
        this.chart.resize();
      });


      this.chart2 = echarts.init(this.$refs.radarChart2);
      const option2 = {
        title: {
          text: this.$t('影响性指标'),
        },
        tooltip: {},
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
            { name: this.$t('保密性影响'), max: 6500 },
            { name: this.$t('完整性影响'), max: 16000 },
            { name: this.$t('可用性影响'), max: 30000 },
          ],
        },
        series: [{
          name: '',
          type: 'radar',
          symbol: 'circle', // 设置数据点的形状为圆形
          symbolSize: 8, // 设置数据点的大小
          data: [
            {
              value: [1500, 3000, 30000],
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
      this.chart2.setOption(option2);
      // 监听窗口大小变化，重绘图表
      window.addEventListener('resize', () => {
        this.chart2.resize();
      });
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
