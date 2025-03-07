<template>
  <section class="overview">
    <div>
      <div class="page-header">
        <span class="header-title">{{$t('总体统计')}}</span>
        <bk-select v-model="summaryDays" :clearable="false" style="width: 120px;" @change="getSummaryData">
          <bk-option
            v-for="option in dayList"
            :key="option.id"
            :id="option.id"
            :name="option.name">
          </bk-option>
        </bk-select>
      </div>
      <div class="image-container">
        <div class="image-wrapper">
          <div class="image">
            <div id="coverCodeRepoCount" ref="coverCodeRepoCount" class="chart-size"></div>
            <div><span class="imageFrontSize">{{$t('覆盖代码库数')}}</span></div>
          </div>
        </div>
        <div class="image-wrapper">
          <div class="image">
            <div id="issueCount" ref="issueCount" class="chart-size"></div>
            <div><span class="imageFrontSize">{{$t('检出问题数')}}</span></div>
          </div>
        </div>
        <div class="divider-line"></div>
        <div class="image-wrapper">
          <div class="image">
            <div id="analyzeCount" ref="analyzeCount" class="chart-size"></div>
            <div><span class="imageFrontSize">{{$t('执行次数')}}</span></div>
          </div>
        </div>
        <div class="image-wrapper">
          <div class="image">
            <div id="successRate" ref="successRate" class="chart-size"></div>
            <div><span class="imageFrontSize">{{$t('成功率')}}</span></div>
          </div>
        </div>
        <div class="image-wrapper">
          <div class="image">
            <div id="averageTime" ref="averageTime" class="chart-size"></div>
            <div><span class="imageFrontSize">{{$t('平均耗时')}}</span></div>
          </div>
        </div>
        <div class="divider-line"></div>
        <div class="image-wrapper">
          <div class="image">
            <div id="misreportRate" ref="misreportRate" class="chart-size"></div>
            <div><span class="imageFrontSize">{{$t('误报率')}}</span></div>
          </div>
        </div>
        <div class="image-wrapper">
          <div class="image">
            <div id="misreportCount" ref="misreportCount" class="chart-size"></div>
            <div><span class="imageFrontSize">{{$t('误报数量')}}</span></div>
            <div class="toBeConfirm">
              <span class="frontClass" @click="toIgnoreList">{{unconfirmed}}&nbsp;{{$t('个')}}{{$t('待确认')}}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="page-header" style="margin-top: 30px">
        <span class="header-title">{{$t('趋势图')}}</span>
        <bk-select v-model="trendDays" :clearable="false" style="width: 120px;" @change="getTrendChartData">
          <bk-option
            v-for="option in dayList"
            :key="option.id"
            :id="option.id"
            :name="option.name">
          </bk-option>
        </bk-select>
      </div>
      <div class="image-container">
        <div class="trend-chart-wrapper">
          <div id="analyzeCountTrend" ref="analyzeCountTrend" class="trend-size"></div>
        </div>
        <div class="trend-chart-wrapper">
          <div id="codeRepoCountTrend" ref="codeRepoCountTrend" class="trend-size"></div>
        </div>
      </div>
    </div>
  </section>
</template>

<script>
import 'echarts/lib/chart/pie';
import 'echarts/lib/chart/line';
import 'echarts/lib/component/legend';
import 'echarts/lib/component/tooltip';
import echarts from 'echarts/lib/echarts';

export default {
  data() {
    return {
      summaryDays: '30',
      trendDays: '30',
      summaryData: {},
      trendData: [],
      unconfirmed: 0,
      dayList: [
        {
          id: '7',
          name: this.$t('最近{0}天', [7]),
        },
        {
          id: '15',
          name: this.$t('最近{0}天', [15]),
        },
        {
          id: '30',
          name: this.$t('最近{0}天', [30]),
        },
        {
          id: '60',
          name: this.$t('最近{0}天', [60]),
        },
        {
          id: '90',
          name: this.$t('最近{0}天', [90]),
        },
        {
          id: '180',
          name: this.$t('最近半年'),
        },
        {
          id: '365',
          name: this.$t('最近一年'),
        },
      ],
    };
  },
  watch: {},
  created() {
    this.getSummaryData();
    this.getTrendChartData();
  },
  mounted() {
  },
  methods: {
    getSummaryData() {
      this.$store.dispatch('test/getToolStatOverview', {
        toolName: this.$route.params.toolName,
        summaryDays: this.summaryDays,
      }).then((res) => {
        this.initPieChart(res);
        this.unconfirmed = res.unconfirmedCount || 0;
      });
    },
    async getTrendChartData() {
      this.$store.dispatch('test/getToolStatDailyTrend', {
        toolName: this.$route.params.toolName,
        trendDays: this.trendDays,
      }).then((res) => {
        const renderTrendData = res.reduce((acc, { date, analysisCount, analysisSuccessCount, codeRepoCount,
          codeRepoNewAddCount }) => {
          acc.dayList.push(date);
          acc.analysisCountList.push(analysisCount);
          acc.successRateList.push(analysisCount ? ((analysisSuccessCount * 100) / analysisCount).toFixed(1) : '0');
          acc.codeRepoCountList.push(codeRepoCount);
          acc.codeRepoNewAddCountList.push(codeRepoNewAddCount);
          return acc;
        }, {
          dayList: [],
          analysisCountList: [],
          successRateList: [],
          codeRepoCountList: [],
          codeRepoNewAddCountList: [],
        });

        this.initTrendChart(renderTrendData);
      });
    },
    initPieChart(summaryData) {
      const codeRepoChart = echarts.init(this.$refs.coverCodeRepoCount);
      const optionCodeRepo = this.getNewBlueChart();
      optionCodeRepo.series[0].data[0].name = summaryData.codeRepoCount.toString();
      codeRepoChart.setOption(optionCodeRepo);

      const issueChart = echarts.init(this.$refs.issueCount);
      const optionIssue = this.getNewBlueChart();
      optionIssue.series[0].data[0].name = summaryData.newIssueCount.toString();
      issueChart.setOption(optionIssue);

      const analyzeChart = echarts.init(this.$refs.analyzeCount);
      const option = this.getGreenChart();
      option.series[0].data[0].name = summaryData.analysisCount.toString();
      analyzeChart.setOption(option);

      const successRateChart = echarts.init(this.$refs.successRate);
      const optionSuccessRate = this.getGreenChart();
      optionSuccessRate.series[0].data[0].name = summaryData.analysisCount ? `${(summaryData.analysisSuccessCount * 100 / summaryData.analysisCount).toFixed(1)}%` : '0%';
      successRateChart.setOption(optionSuccessRate);

      const averageTimeChart = echarts.init(this.$refs.averageTime);
      const optionAverageTime = this.getGreenChart();
      optionAverageTime.series[0].data[0].name = summaryData.codeLine ? `${(summaryData.elapseTime / summaryData.codeLine).toFixed(1)}\ns/kloc` : '--';
      averageTimeChart.setOption(optionAverageTime);

      const misreportRateChart = echarts.init(this.$refs.misreportRate);
      const optionMisreportRate = this.getOrangeChart();
      optionMisreportRate.series[0].data[0].name = summaryData.newIssueCount ? `${(summaryData.misreportIgnoreCount * 100 / summaryData.newIssueCount).toFixed(1)}%` : '--';
      misreportRateChart.setOption(optionMisreportRate);

      const misreportChart = echarts.init(this.$refs.misreportCount);
      const optionMisreport = this.getOrangeChart();
      optionMisreport.series[0].data[0].name = summaryData.misreportIgnoreCount.toString();
      misreportChart.setOption(optionMisreport);
    },
    initTrendChart(trendData) {
      const analyzeTrendChart = echarts.init(this.$refs.analyzeCountTrend);
      const optionAnalyzeTrend = this.getBaseTrendChartOption();
      optionAnalyzeTrend.xAxis.data = trendData.dayList;
      optionAnalyzeTrend.legend.data[0].name = this.$t('执行次数');
      optionAnalyzeTrend.legend.data[1].name = this.$t('成功率');
      optionAnalyzeTrend.series[0].name = this.$t('执行次数');
      optionAnalyzeTrend.series[1].name = this.$t('成功率');
      optionAnalyzeTrend.tooltip.formatter = '{b0}<br />{a0}: {c0}<br />{a1}: {c1}%';
      optionAnalyzeTrend.series[0].data = trendData.analysisCountList;
      optionAnalyzeTrend.series[1].data = trendData.successRateList;
      analyzeTrendChart.setOption(optionAnalyzeTrend);
      window.addEventListener('resize', () => {
        analyzeTrendChart.resize();
      });

      const codeRepoTrendChart = echarts.init(this.$refs.codeRepoCountTrend);
      const optionCodeRepoTrend = this.getBaseTrendChartOption();
      optionCodeRepoTrend.xAxis.data = trendData.dayList;
      optionCodeRepoTrend.legend.data[0].name = this.$t('扫描代码库数');
      optionCodeRepoTrend.legend.data[1].name = this.$t('新扫代码库数');
      optionCodeRepoTrend.series[0].name = this.$t('扫描代码库数');
      optionCodeRepoTrend.series[1].name = this.$t('新扫代码库数');
      optionCodeRepoTrend.series[0].data = trendData.codeRepoCountList;
      optionCodeRepoTrend.series[1].data = trendData.codeRepoNewAddCountList;
      codeRepoTrendChart.setOption(optionCodeRepoTrend);
      window.addEventListener('resize', () => {
        codeRepoTrendChart.resize();
      });
    },
    getNewBlueChart() {
      const option = this.getBasePeiChartOption();
      option.series[0].data[0].itemStyle.color.colorStops[0].color = '#699DF4';
      option.series[0].data[0].itemStyle.color.colorStops[1].color = '#EBF0FA';
      return JSON.parse(JSON.stringify(option));
    },
    getGreenChart() {
      const option = this.getBasePeiChartOption();
      option.series[0].data[0].itemStyle.color.colorStops[0].color = '#2DCB9D';
      option.series[0].data[0].itemStyle.color.colorStops[1].color = '#E6F0EB';
      return JSON.parse(JSON.stringify(option));
    },
    getOrangeChart() {
      const option = this.getBasePeiChartOption();
      option.series[0].data[0].itemStyle.color.colorStops[0].color = '#FFB848';
      option.series[0].data[0].itemStyle.color.colorStops[1].color = '#F0EBE1';
      return JSON.parse(JSON.stringify(option));
    },
    getBasePeiChartOption() {
      const baseOption = {
        animation: true,
        tooltip: {
          show: false,
        },
        series: [
          {
            type: 'pie',
            radius: ['70%', '90%'],
            avoidLabelOverlap: false,
            hoverAnimation: false,
            cursor: 'auto',
            label: {
              show: true,
              fontSize: 14,
              position: 'center',
            },
            startAngle: 90,
            clockwise: true,
            labelLine: {
              show: true,
            },
            data: [
              {
                value: 150,
                itemStyle: {
                  color: {
                    type: 'linear',
                    x: 0.7,
                    y: 0.9,
                    x2: 1,
                    y2: 0.5,
                    colorStops: [
                      { offset: 0 },
                      { offset: 0.9 },
                    ],
                  },
                },
              },
              { value: 50, itemStyle: { color: '#F3F3F6FF' } },
            ],
          },
        ],
      };
      return JSON.parse(JSON.stringify(baseOption));
    },
    getBaseTrendChartOption() {
      const option = {
        color: ['#e73232', '#fac42f'],
        legend: {
          left: 'left',
          show: true,
          data: [{}, {}],
          textStyle: {
            color: '#777',
            fontSize: 13,
          },
          itemWidth: 11,
          itemHeight: 13,
        },
        xAxis: {
          type: 'category',
          axisLabel: {
            color: '#777',
          },
          axisLine: {
            lineStyle: {
              color: '#777',
            },
          },
        },
        yAxis: [
          {
            type: 'value',
            position: 'left',
            axisTick: {
              show: false,
            },
            axisLine: {
              show: false,
            },
            splitLine: {
              show: true,
              lineStyle: {
                color: '#d0eef6',
                type: 'dashed',
              },
            },
            axisLabel: {
              color: '#777',
            },
          },
          {
            type: 'value',
            position: 'right',
            axisTick: {
              show: false,
            },
            axisLine: {
              show: false,
            },
            splitLine: {
              show: true,
              lineStyle: {
                color: '#d0eef6',
                type: 'dashed',
              },
            },
            axisLabel: {
              color: '#777',
            },
          },
        ],
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'line',
          },
        },
        series: [
          {
            type: 'line',
            yAxisIndex: 0,
            smooth: true,
            symbol: 'none',
          },
          {
            type: 'line',
            yAxisIndex: 1,
            smooth: true,
            symbol: 'none',
          },
        ],
      };
      return JSON.parse(JSON.stringify(option));
    },
    toIgnoreList() {
      const toolId = this.$route.params.toolName;
      const { href } = this.$router.resolve({
        name: 'paas-list',
        params: {
          toolName: toolId,
        }
      });
      // const fullUrl = encodeURIComponent(`${window.location.origin}${href}`);
      // const v3Url = `${window.PAAS_V3_URL}/plugin-center/plugin/${window.PAAS_V3_APP}/${
      //   toolId}/ignore-list?url=${fullUrl}`;
      window.open(`${window.location.origin}${href}`);
    },
  },
};

</script>
<style>
#app {
  min-width: 870px;
}
</style>
<style scoped>
.overview {
  min-width: 888px;
  min-height: 100vh;
  padding: 24px;
  margin-top: 40px;
  background: #ffffff;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  margin: 0 14px;
}
.header-title {
  height: 22px;
  font-family: "MicrosoftYaHei-Bold";
  font-weight: 700;
  font-size: 17px;
  color: #313238;
  line-height: 22px;
}
.image-container {
  display: flex;
  padding: 16px;
  min-width: 720px;
}
.image-wrapper {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  .chart-size {
    width: 100px;
    height: 100px;
  }
}
.trend-chart-wrapper {
  width: 50%;
  .trend-size {
    min-width: 330px;
    height: 300px;
  }
}
.image {
  text-align: center;
  width: 100px;
  height: 120px;
}
.divider-line {
  border-left: 1px solid #ccc;
  height: 120px;
  margin: 0 5px;
}
.imageFrontSize {
  font-size: 14px;
}
.toBeConfirm {
  width: 107px;
  height: 24px;
  background: #F0F1F5;
  border-radius: 12px;
}
.frontClass {
  width: 66px;
  height: 20px;
  font-family: MicrosoftYaHei;
  font-size: 14px;
  color: #3A84FF;
  cursor: pointer;
  letter-spacing: 0;
  line-height: 20px;
}
</style>
