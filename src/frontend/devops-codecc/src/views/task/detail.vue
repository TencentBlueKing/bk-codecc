<template>
  <div class="task-detail">
    <div class="task-detail-left">
      <span class="tool-rate">
        <span class="mt4 pr10">{{$t('质量星级')}}</span>
        <bk-popover>
          <i class="codecc-icon icon-tips f12"></i>
          <div slot="content">
            <p><i class="codecc-icon icon-star-gray"></i>{{$t('按公司开源治理要求配置规则集')}}</p>
            <p><i class="codecc-icon icon-star-gray blue"></i>{{$t('自主配置规则集')}}</p>
          </div>
        </bk-popover>
        <!-- <bk-popover>
          <bk-rate
            :rate="0"
            :edit="false"
            :width="16"
            :height="16">
          </bk-rate>
          <div slot="content">
            功能开发中，敬请期待
          </div>
        </bk-popover> -->
        <bk-popover>
          <bk-rate
            :rate="rdScore.rdIndicatorsScore ? rdScore.rdIndicatorsScore / 20 : 0"
            :edit="false"
            :width="16"
            :height="16"
            :ext-cls="{ 'open-scan': !rdScore.openScan }">
          </bk-rate>
          <div slot="content">
            <p>{{ $t('质量星级：') }}<b>{{rdScore.rdIndicatorsScore ? (rdScore.rdIndicatorsScore / 20).toFixed(1) : 0}}</b>，
              {{$t('综合得分：')}}<b>{{rdScore.rdIndicatorsScore | formatUndefNum('fixed', 2)}}分</b>{{$t('，详情如下：')}}</p>
            <p>{{$t('【安全漏洞】严重问题数')}}{{rdScore.codeSecuritySeriousDefectCount | formatUndefNum}}，
              {{ $t('一般问题数') }}{{rdScore.codeSecurityNormalDefectCount | formatUndefNum}}，
              {{$t('得分：')}}{{rdScore.codeSecurityScore | formatUndefNum('fixed', 2)}}；</p>
            <p>{{$t('【代码规范】严重问题数密度')}}{{rdScore.averageSeriousStandardThousandDefect | formatUndefNum('fixed', 2)}}{{ $t('千行，') }}
              {{$t('一般问题数密度')}}{{rdScore.averageNormalStandardThousandDefect | formatUndefNum('fixed', 2)}}{{ $t('千行，') }}
              {{$t('得分：')}}{{rdScore.codeStyleScore | formatUndefNum('fixed', 2)}}；</p>
            <p>{{$t('【圈复杂度】千行超标复杂度')}}{{averageThousandDefect | formatUndefNum('fixed', 2)}}，
              {{$t('得分：')}}{{rdScore.codeCcnScore| formatUndefNum('fixed', 2)}}；</p>
            <p>{{$t('以上质量评价依照')}}{{$t('腾讯开源治理指标体系')}}
              {{$t('(其中文档质量暂按100分计算)，')}}
              {{$t('得分仅供参考，最终得分请以')}} {{$t('技术图谱')}} {{$t('为准。')}}</p>
            <p>{{$t('技术图谱每日凌晨刷新一次分数。')}}</p>
          </div>
        </bk-popover>
      </span>
      <bk-tab :active.sync="active" :label-height="42" type="unborder-card" class="detail-tab">
        <bk-tab-panel
          v-for="(panel, index) in panels"
          v-bind="panel"
          :key="index">
          <template slot="label">
            <span class="panel-name">{{panel.label}}</span>
          </template>
        </bk-tab-panel>
        <detail-tool v-if="active === 'tool'" :selected-type-data="toolDataList"></detail-tool>
        <detail-dimension v-else :cluster-list="clusterList" :rd-score="rdScore"></detail-dimension>
      </bk-tab>
    </div>
    <div class="task-detail-right">
      <section class="detail-content">
        <p class="detail-header">{{$t('待处理')}} <span class="flc">({{user.username}})</span></p>
        <bk-container :col="3" :gutter="4" class="person-data">
          <bk-row>
            <bk-col class="person-block">
              <div class="person-number" @click="handleToPage('defect')">{{personal.defectCount | formatBigNum}}</div>
              <div class="person-txt">{{$t('缺陷')}}</div>
            </bk-col>
            <bk-col class="person-block">
              <div class="person-number" @click="handleToPage('security')">{{personal.securityCount | formatBigNum}}</div>
              <div class="person-txt">{{$t('漏洞')}}</div>
            </bk-col>
            <bk-col class="person-block">
              <div class="person-number" @click="handleToPage('standard')">{{personal.standardCount | formatBigNum}}</div>
              <div class="person-txt">{{$t('规范问题')}}</div>
            </bk-col>
          </bk-row>
          <bk-row>
            <bk-col class="person-block">
              <div class="person-number" @click="handleToPage('ccn')">{{personal.riskCount | formatBigNum}}</div>
              <div class="person-txt">{{$t('风险函数')}}</div>
            </bk-col>
            <bk-col class="person-block">
              <div class="person-number" @click="handleToPage('dupc')">{{personal.dupFileCount | formatBigNum}}</div>
              <div class="person-txt">{{$t('重复文件')}}</div>
            </bk-col>
          </bk-row>
        </bk-container>
      </section>
      <section class="detail-content code-info">
        <p class="detail-header">{{$t('代码信息')}}</p>
        <div class="detail-chart">
          <div id="clocChart" ref="clocChart"></div>
          <div id="langChart" ref="langChart"></div>
        </div>
      </section>
      <section class="detail-content">
        <p class="detail-header">{{$t('multi.规则集')}}</p>
        <div class="checkerset" v-for="(value, key) in checkersetMap" :key="key">
          <dt class="checkerset-lang cc-ellipsis" :title="key">{{key}}</dt>
          <bk-popover class="msg-popover">
            <dd class="checkerset-name">{{value.join(',')}}</dd>
            <div slot="content">
              <p class="msg-content" v-for="(item, index) in value" :key="index">{{ item }}</p>
            </div>
          </bk-popover>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
  import 'echarts/lib/chart/pie'
  import 'echarts/lib/chart/bar'
  import 'echarts/lib/component/tooltip'
  import 'echarts/lib/component/title'
  import 'echarts/lib/component/legend'
  import detailTool from '@/components/detail-tool'
  import detailDimension from '@/components/detail-dimension'
  import echarts from 'echarts/lib/echarts'
  import taskWebsocket from '@/common/taskWebSocket'
  import { mapState } from 'vuex'
  import { numToThousand } from '@/common/util'

  export default {
    components: {
      detailTool,
      detailDimension,
    },
    data() {
      return {
        taskId: this.$route.params.taskId,
        active: window.localStorage.getItem('detail-active-tab') || 'tool',
        panels: [
          { name: 'tool', label: this.$t('按工具') },
          { name: 'dimension', label: this.$t('按维度') },
        ],
        personal: {},
        checkersetMap: {},
        rdScore: {},
        toolDataList: [],
        clusterList: [],
        hasRedPointStore: window.localStorage.getItem('redtips-dimension-20201207'),
      }
    },
    computed: {
      ...mapState(['user']),
      ...mapState('task', {
        taskDetail: 'detail',
      }),
      averageThousandDefect() {
        let count = 0
        const list = this.rdScore.lastClusterResultList || []
        list.forEach((item) => {
          if (item.baseClusterResultVO
            && item.baseClusterResultVO.averageThousandDefect
            && item.baseClusterResultVO.type === 'CCN') {
            count = item.baseClusterResultVO.averageThousandDefect.toFixed(2)
          }
        })
        return count
      },
      handleOpenHref() {
        return window.IWIKI_OPEN_SCORE
      },
      handleTechmapHref() {
        return window.TECHMAP_REPORT
      },
    },
    watch: {
      active(value) {
        window.localStorage.setItem('detail-active-tab', value)
        if (value === 'dimension') {
          window.localStorage.setItem('redtips-dimension-20201207', '1')
          this.hasRedPointStore = true
        }
      },
    },
    created() {
      this.init()
    },
    mounted() {
      this.fetchCloc()
      this.subscribeMsg()
    },
    methods: {
      init() {
        this.fetchRemain()
        this.fetchCheckerSet()
        this.fetchToolList()
        this.fetchDimensionList()
      },
      initChart(fileInfo, langInfo) {
        const clocChart = echarts.init(this.$refs.clocChart)
        const option = {
          color: ['#3a84ff', '#3fc06d', '#c4c6cc'],
          tooltip: {
            trigger: 'item',
            formatter: '{b}: {c} ({d}%)',
          },
          title: {
            text: [
              `{a| ${this.$t('总行数')}}`,
              `{b| ${fileInfo.totalLinesFormatted}}`,
            ].join('\n'),
            left: 'center',
            top: 'center',
            textStyle: {
              rich: {
                a: {
                  fontSize: 10,
                  height: 20,
                },
                b: {
                  fontSize: 14,
                  fontWeight: 'bold',
                },
              },
            },
          },
          series: [
            {
              name: this.$t('代码统计'),
              type: 'pie',
              radius: ['55%', '100%'],
              hoverAnimation: false,
              labelLine: {
                show: false,
              },
              data: [
                { value: fileInfo.codeLines, name: this.$t('代码行') },
                { value: fileInfo.commentLines, name: this.$t('注释行') },
                { value: fileInfo.blankLines, name: this.$t('空白行') },
              ],
            },
          ],
        }
        clocChart.setOption(option)

        const langChart = echarts.init(this.$refs.langChart)
        const option2  = {
          color: '#699df4',
          tooltip: {
            trigger: 'axis',
            axisPointer: {
              type: 'shadow',
            },
          },
          grid: {
            left: '0',
            right: '30%',
            top: '15px',
            bottom: '-15px',
            containLabel: true,
          },
          xAxis: {
            type: 'value',
            show: false,
            splitLine: false,
          },
          yAxis: {
            type: 'category',
            axisLine: {
              show: false,
            },
            axisTick: {
              show: false,
            },
            data: langInfo.langName,
            axisLabel: {
              fontSize: 10,
              formatter: value => (value.length > 10 ? `${value.slice(0, 8)}..` : value),
            },
          },
          series: [
            {
              name: this.$t('代码量'),
              type: 'bar',
              barMaxWidth: '20',
              label: {
                show: true,
                position: 'right',
                color: '#63656E',
                formatter(value) {
                  return numToThousand(value.data)
                },
              },
              data: langInfo.langValue,
            },
          ],
        }
        langChart.setOption(option2)
      },
      fetchRemain() {
        this.$store.dispatch('task/personal').then((res) => {
          this.personal = res
        })
      },
      fetchCheckerSet() {
        const params = { taskIdList: [this.$route.params.taskId] }
        this.$store.dispatch('checkerset/listForDefect', params).then((res) => {
          const checkersetMap = {}
          if (res && res.length) {
            res.forEach((checkerSet) => {
              const { checkerSetLang, checkerSetName } = checkerSet
              if (!checkersetMap[checkerSetLang]) {
                checkersetMap[checkerSetLang] = [checkerSetName]
              } else {
                checkersetMap[checkerSetLang].push(checkerSetName)
              }
            })
          }
          this.checkersetMap = checkersetMap
        })
      },
      fetchCloc() {
        const clocFile = this.$store.dispatch('defect/lintListCloc', { toolId: 'CLOC', type: 'FILE' })
        const clocLang = this.$store.dispatch('defect/lintListCloc', { toolId: 'CLOC', type: 'LANGUAGE' })
        Promise.all([clocFile, clocLang]).then(([fileData, langData]) => {
          const { codeLines, commentLines, blankLines, totalLines } = fileData.clocTreeNodeVO
          const totalLinesFormatted = numToThousand(totalLines)
          const fileInfo = { codeLines, commentLines, blankLines, totalLines, totalLinesFormatted }
          const langList = (langData.languageInfo && langData.languageInfo.slice(0, 5)) || []
          const langName = []
          const langValue = []
          langList.forEach((lang) => {
            if (lang.sumLines >= 100) {
              langName.unshift(lang.language)
              langValue.unshift(lang.sumLines)
              const formatted = numToThousand(lang.sumLines)
            }
          })
          const langInfo = { langName, langValue }
          this.initChart(fileInfo, langInfo)
        })
      },
      handleToPage(name) {
        this.$router.push({
          name: `defect-${name}-list`,
          query: {
            dimension: name.toUpperCase(),
            author: this.user.username,
            from: 'overview',
            isOpenScan: this.rdScore.openScan,
          },
        })
      },
      fetchToolList() {
        this.$store.dispatch(
          'task/overView',
          { taskId: this.taskId, showLoading: true, buildNum: this.$route.query.buildNum },
        ).then((res) => {
          if (res.lastAnalysisResultList) {
            this.toolDataList = res.lastAnalysisResultList.filter(item => item.toolName !== 'CLOC')
          }
        })
      },
      fetchDimensionList() {
        this.$store.dispatch(
          'task/overView',
          { taskId: this.$route.params.taskId, orderBy: 'dimension', buildNum: this.$route.query.buildNum },
        ).then((res) => {
          this.rdScore = res
          if (res && res.lastClusterResultList) {
            this.clusterList = res.lastClusterResultList
          }
        })
      },
      subscribeMsg() {
        const subscribe = `/topic/analysisInfo/taskId/${this.taskId}`
        if (taskWebsocket.stompClient.connected) {
          taskWebsocket.subscribeMsg(subscribe, {
            success: (res) => {
              const data = JSON.parse(res.body)
              console.log('🚀 ~ file: detail.vue ~ line 349 ~ subscribeMsg ~ data', data)
              let hasNewTool = 1
              this.toolDataList.forEach((item) => {
                if (item.toolName === data.toolName) {
                  Object.assign(item, data)
                  // hasNewTool = item.lastAnalysisResult ? 0 : 1
                  hasNewTool = 0
                }
              })
            // 暂时屏蔽刷新页面
              // if (hasNewTool && data.toolName !== 'CLOC') this.init()
            },
            // error: message => this.$showTips({ message, theme: 'error' }),
            error: message => console.error(message),
          })
        } else { // websocket还没连接的话，1s后重试
          setTimeout(() => {
            this.subscribeMsg()
          }, 1000)
        }
      },
    },
  }
</script>

<style lang="postcss" scoped>
  .main-container {
    background: #fff;
  }
  .task-detail {
    position: relative;
    .task-detail-left {
      float: left;
      width: calc(100% - 325px);
      border-right: 1px solid #f0f1f5;
      .tool-rate {
        display: inline-flex;
        align-items: center;
        font-size: 16px;
        line-height: 36px;
        padding-left: 30px;
        padding-top: 20px;
        color: #000;
        font-weight: bold;
      }
      .detail-tab {
        height: calc(100% - 52px);
        >>>.bk-tab-section {
          height: calc(100% - 42px);
          min-height: calc(100vh - 154px);
          background: #f5f7fa;
        }
      }
    }
    .task-detail-right {
      position: absolute;
      right: 0;
      width: 325px;
      .detail-content {
        padding: 25px 30px;
        border-bottom: 1px solid #f0f1f5;
        &.code-info {
          padding-right: 10px;
        }
        .detail-header {
          font-size: 14px;
          color: #000;
          font-weight: bold;
          .flc {
            font-weight: 200;
          }
        }
        .detail-chart {
          padding-top: 10px;
          position: relative;
          height: 110px;
          #clocChart {
            position: absolute;
            left: 0;
            height: 110px;
            width: 110px;
            padding-right: 10px;
            border-right: 1px solid #f0f1f5;
          }
          #langChart {
            position: absolute;
            right: 0;
            height: 110px;
            width: 170px;
            padding-left: 5px;
          }
        }
        .checkerset {
          display: flex;
          font-size: 14px;
          padding-top: 15px;
          .checkerset-lang {
            font-weight: 550;
            color: #63656e;
            width: 75px;
          }
          .checkerset-name {
            max-width: 190px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }
      }
      .person-data {
        margin: 0 -20px 0 -20px;
        .bk-grid-row {
          padding: 25px 0 13px;
        }
      }
      .person-block {
        text-align: center;
        .person-number {
          color: #000;
          font-size: 24px;
          cursor: pointer;
          &:hover {
            color: #3a84ff;
          }
        }
        .person-txt {
          font-size: 12px;
          color: #979BA5;
        }
      }
    }
    .open-scan {
      >>>svg.bk-yellow {
        fill: #3a84ff;
      }
    }
  }
  .mt4 {
    margin-top: -4px;
  }
  .icon-tips {
    position: relative;
    top: -3px;
    color: #c4c6cc;
    padding-left: 5px;
    padding-right: 10px;
  }
  .icon-star-gray {
    color: #fe9c00;
    position: relative;
    top: -1px;
    &.blue {
      color: #3a84ff;
    }
  }
</style>
