<template>
  <div class="card">
    <div class="card-head">
      <span class="card-tool" v-if="data.toolName">{{
        (toolMap[data.toolName] && toolMap[data.toolName]['displayName']) || ''
      }}</span>
      <!-- <span v-if="data.lastAnalysisResult" class="card-split">|</span> -->
      <!-- <a class="card-analys" href="javascript:;" @click.stop="toLogs">{{$t('分析详情')}}>></a> -->
      <span v-if="data.lastAnalysisResult" v-for="(type, index) in data.toolTypes" :key="index" class="card-type">{{
        toolMap[data.toolName] && toolType[type]
      }}</span>
      <span v-if="data.lastAnalysisResult" class="card-time">
        <span v-if="stepStatus === 3"
        >{{ $t('开始于') }} {{ formatDate(data.lastAnalysisTime) }}</span
        >
        <span v-else
        >{{ $t('耗时') }} {{ formatSeconds(data.elapseTime) }}</span
        >
      </span>
      <span
        v-if="data.lastAnalysisResult"
        class="card-step"
        :class="{
          success: stepStatus === 0,
          fail: stepStatus === 1,
          loading: stepStatus === 3,
        }"
        @click.stop="toLogs"
      >
        <i
          class="bk-icon card-tool-status"
          :class="{
            'icon-check-circle-shape': stepStatus === 0,
            'icon-close-circle-shape': stepStatus === 1,
          }"
        >
        </i>
        <li class="cc-fading-circle" v-if="stepStatus === 3">
          <div class="cc-circle1 cc-circle"></div>
          <div class="cc-circle2 cc-circle"></div>
          <div class="cc-circle3 cc-circle"></div>
          <div class="cc-circle4 cc-circle"></div>
          <div class="cc-circle5 cc-circle"></div>
          <div class="cc-circle6 cc-circle"></div>
          <div class="cc-circle7 cc-circle"></div>
          <div class="cc-circle8 cc-circle"></div>
          <div class="cc-circle9 cc-circle"></div>
          <div class="cc-circle10 cc-circle"></div>
          <div class="cc-circle11 cc-circle"></div>
          <div class="cc-circle12 cc-circle"></div>
        </li>
        <!-- <i class="bk-icon card-tool-status icon-circle-2-1 spin-icon" v-if="stepStatus === 3"></i> -->
        <span class="card-step-txt">{{
          $t(`${getToolStatus(data.curStep, data.toolName)}`)
        }}</span>
      </span>
    </div>
    <div v-if="data.lastAnalysisResult">
      <bk-container
        class="card-content"
        :col="4"
        v-if="data.toolName === 'CCN'"
      >
        <bk-row class="card-num">
          <bk-col
          ><a @click="toList()">{{
            data.lastAnalysisResult.defectCount === undefined
              ? '--'
              : data.lastAnalysisResult.defectCount
          }}</a></bk-col
          >
          <bk-col
          ><a @click="toList()"
          >{{
             data.lastAnalysisResult.defectChange === undefined
               ? '--'
               : Math.abs(data.lastAnalysisResult.defectChange)
           }}
            <i
              class="bk-icon"
              :class="{
                'icon-arrows-up up': data.lastAnalysisResult.defectChange > 0,
                'icon-arrows-down down':
                  data.lastAnalysisResult.defectChange < 0,
              }"
            >
            </i
            ></a>
          </bk-col>
          <bk-col
          ><a @click="toList()">{{
            data.lastAnalysisResult.averageCCN === undefined
              ? '--'
              : data.lastAnalysisResult.averageCCN.toFixed(2)
          }}</a></bk-col
          >
          <bk-col
          ><a @click="toList()"
          >{{
             data.lastAnalysisResult.averageCCNChange === undefined
               ? '--'
               : Math.abs(data.lastAnalysisResult.averageCCNChange).toFixed(
                 2
               )
           }}
            <i
              class="bk-icon"
              :class="{
                'icon-arrows-up up':
                  data.lastAnalysisResult.averageCCNChange > 0,
                'icon-arrows-down down':
                  data.lastAnalysisResult.averageCCNChange < 0,
              }"
            >
            </i
            ></a>
          </bk-col>
        </bk-row>
        <bk-row class="card-txt">
          <bk-col
          ><i class="codecc-icon icon-risky-function"></i
          >{{ $t('overview.风险函数') }}</bk-col
          >
          <bk-col
          ><i class="codecc-icon icon-trend"></i
          >{{ $t('风险函数趋势') }}</bk-col
          >
          <bk-col
          ><i class="codecc-icon icon-pie"></i
          >{{ $t('平均圈复杂度') }}</bk-col
          >
          <bk-col
          ><i class="codecc-icon icon-trend"></i
          >{{ $t('圈复杂度趋势') }}</bk-col
          >
        </bk-row>
      </bk-container>

      <bk-container
        class="card-content"
        :col="4"
        v-else-if="data.toolName === 'DUPC'"
      >
        <bk-row class="card-num">
          <bk-col
          ><a @click="toList()">{{
            data.lastAnalysisResult.defectCount === undefined
              ? '--'
              : data.lastAnalysisResult.defectCount
          }}</a></bk-col
          >
          <bk-col
          ><a @click="toList()"
          >{{
             data.lastAnalysisResult.defectChange === undefined
               ? '--'
               : Math.abs(data.lastAnalysisResult.defectChange)
           }}
            <i
              class="bk-icon"
              :class="{
                'icon-arrows-up up': data.lastAnalysisResult.defectChange > 0,
                'icon-arrows-down down':
                  data.lastAnalysisResult.defectChange < 0,
              }"
            >
            </i
            ></a>
          </bk-col>
          <bk-col
          ><a @click="toList()"
          >{{
            data.lastAnalysisResult.defectChange === undefined
              ? '--'
              : data.lastAnalysisResult.dupRate.toFixed(2)
          }}<span v-if="data.lastAnalysisResult.defectChange !== undefined"
          >%</span
          ></a
          ></bk-col
          >
          <bk-col
          ><a @click="toList()"
          >{{
             data.lastAnalysisResult.dupRateChange === undefined
               ? '--'
               : Math.abs(data.lastAnalysisResult.dupRateChange).toFixed(2)
           }}<span v-if="data.lastAnalysisResult.dupRateChange !== undefined"
           >%</span
           >
            <i
              class="bk-icon"
              :class="{
                'icon-arrows-up up':
                  data.lastAnalysisResult.dupRateChange > 0,
                'icon-arrows-down down':
                  data.lastAnalysisResult.dupRateChange < 0,
              }"
            >
            </i
            ></a>
          </bk-col>
        </bk-row>
        <bk-row class="card-txt">
          <bk-col
          ><i class="codecc-icon icon-risky-file"></i
          >{{ $t('overview.重复文件') }}</bk-col
          >
          <bk-col
          ><i class="codecc-icon icon-trend"></i
          >{{ $t('重复文件趋势') }}</bk-col
          >
          <bk-col
          ><i class="codecc-icon icon-pie"></i>{{ $t('平均重复率') }}</bk-col
          >
          <bk-col
          ><i class="codecc-icon icon-trend"></i
          >{{ $t('重复率趋势') }}</bk-col
          >
        </bk-row>
      </bk-container>

      <bk-container
        class="card-content"
        :col="4"
        v-else-if="
          data.toolName === 'COVERITY' ||
            data.toolName === 'KLOCWORK' ||
            data.toolName === 'PINPOINT'
        "
      >
        <bk-row class="card-num">
          <bk-col
          ><a @click="toList()">{{
            data.lastAnalysisResult.existCount === undefined
              ? '--'
              : data.lastAnalysisResult.existCount
          }}</a></bk-col
          >
          <bk-col
          ><a @click="toList()">{{
            data.lastAnalysisResult.newCount === undefined
              ? '--'
              : Math.abs(data.lastAnalysisResult.newCount)
          }}</a></bk-col
          >
          <bk-col
          ><a @click="toList({ status: 2 })">{{
            data.lastAnalysisResult.fixedCount === undefined
              ? '--'
              : data.lastAnalysisResult.fixedCount
          }}</a></bk-col
          >
          <bk-col
          ><a @click="toList()">{{
            data.lastAnalysisResult.excludeCount === undefined
              ? '--'
              : data.lastAnalysisResult.excludeCount
          }}</a></bk-col
          >
        </bk-row>
        <bk-row class="card-txt">
          <bk-col
          ><i class="codecc-icon icon-danger"></i>{{ $t('遗留问题') }}</bk-col
          >
          <bk-col
          ><i class="codecc-icon icon-trend"></i>{{ $t('新增问题') }}</bk-col
          >
          <bk-col
          ><i class="codecc-icon icon-danger"></i>{{ $t('修复问题') }}</bk-col
          >
          <bk-col
          ><i class="codecc-icon icon-danger"></i>{{ $t('屏蔽问题') }}</bk-col
          >
        </bk-row>
      </bk-container>

      <bk-container
        class="card-content"
        :col="4"
        v-else-if="data.toolName === 'CLOC'"
      >
        <bk-row class="card-num">
          <bk-col
          ><a @click="toList()" :title="data.lastAnalysisResult.totalLines">{{
            data.lastAnalysisResult.totalLines === undefined
              ? '--'
              : handleBigNum(data.lastAnalysisResult.totalLines)
          }}</a></bk-col
          >
          <bk-col>
            <a
              @click="toList()"
              :title="Math.abs(data.lastAnalysisResult.linesChange)"
            >{{
               data.lastAnalysisResult.linesChange === undefined
                 ? '--'
                 : handleBigNum(Math.abs(data.lastAnalysisResult.linesChange))
             }}
              <i
                class="bk-icon"
                :class="{
                  'icon-arrows-up up': data.lastAnalysisResult.linesChange > 0,
                  'icon-arrows-down down':
                    data.lastAnalysisResult.linesChange < 0,
                }"
              >
              </i
              ></a>
          </bk-col>
          <bk-col
          ><a @click="toList()">{{
            data.lastAnalysisResult.fileNum === undefined
              ? '--'
              : data.lastAnalysisResult.fileNum
          }}</a></bk-col
          >
          <bk-col>
            <a @click="toList()"
            >{{
               data.lastAnalysisResult.fileNumChange === undefined
                 ? '--'
                 : Math.abs(data.lastAnalysisResult.fileNumChange)
             }}
              <i
                class="bk-icon"
                :class="{
                  'icon-arrows-up up':
                    data.lastAnalysisResult.fileNumChange > 0,
                  'icon-arrows-down down':
                    data.lastAnalysisResult.fileNumChange < 0,
                }"
              >
              </i
              ></a>
          </bk-col>
        </bk-row>
        <bk-row class="card-txt">
          <bk-col
          ><i class="codecc-icon icon-task-line"></i
          >{{ $t('总行数') }}</bk-col
          >
          <bk-col
          ><i class="codecc-icon icon-trend"></i>{{ $t('行数趋势') }}</bk-col
          >
          <bk-col
          ><i class="codecc-icon icon-risky-file"></i
          >{{ $t('文件数') }}</bk-col
          >
          <bk-col
          ><i class="codecc-icon icon-trend"></i
          >{{ $t('文件数趋势') }}</bk-col
          >
        </bk-row>
      </bk-container>

      <bk-container
        class="card-content"
        :col="4"
        v-else-if="data.toolName === 'PECKER_SCA'"
      >
        <bk-row class="card-num">
          <bk-col>
            <a @click="toList({}, 'sca-pkg')">
              {{ data.lastAnalysisResult.packageCount === undefined
                ? '--'
                : handleBigNum(data.lastAnalysisResult.packageCount) }}
            </a>
          </bk-col>
          <!-- 隐藏漏洞数 -->
          <!-- <bk-col>
            <a @click="toList({}, 'sca-vuln')">
              {{ data.lastAnalysisResult.newVulCount === undefined
                ? '--'
                : handleBigNum(data.lastAnalysisResult.newVulCount) }}
            </a>
          </bk-col> -->
          <bk-col>
            <a @click="toList({}, 'sca-lic')">
              {{ data.lastAnalysisResult.licenseCount === undefined
                ? '--'
                : handleBigNum(data.lastAnalysisResult.licenseCount) }}
            </a>
          </bk-col>
        </bk-row>
        <bk-row class="card-txt">
          <bk-col>{{ $t('组件数') }}</bk-col>
          <!-- 隐藏漏洞数 -->
          <!-- <bk-col>{{ $t('漏洞数') }}</bk-col> -->
          <bk-col>{{ $t('许可证数') }}</bk-col>
        </bk-row>
      </bk-container>

      <bk-container class="card-content" :col="4" v-else>
        <bk-row class="card-num">
          <bk-col
          ><a @click="toList()">{{
            data.lastAnalysisResult.defectCount === undefined
              ? '--'
              : data.lastAnalysisResult.defectCount
          }}</a></bk-col
          >
          <bk-col
          ><a @click="toList()"
          >{{
             data.lastAnalysisResult.defectChange === undefined
               ? '--'
               : Math.abs(data.lastAnalysisResult.defectChange)
           }}
            <i
              class="bk-icon"
              :class="{
                'icon-arrows-up up': data.lastAnalysisResult.defectChange > 0,
                'icon-arrows-down down':
                  data.lastAnalysisResult.defectChange < 0,
              }"
            >
            </i
            ></a>
          </bk-col>
          <bk-col
          ><a @click="toList()">{{
            data.lastAnalysisResult.fileCount === undefined
              ? '--'
              : data.lastAnalysisResult.fileCount
          }}</a></bk-col
          >
          <bk-col
          ><a @click="toList()"
          >{{
             data.lastAnalysisResult.fileChange === undefined
               ? '--'
               : Math.abs(data.lastAnalysisResult.fileChange)
           }}
            <i
              class="bk-icon"
              :class="{
                'icon-arrows-up up': data.lastAnalysisResult.fileChange > 0,
                'icon-arrows-down down':
                  data.lastAnalysisResult.fileChange < 0,
              }"
            >
            </i
            ></a>
          </bk-col>
        </bk-row>
        <bk-row class="card-txt">
          <bk-col
          ><i class="codecc-icon icon-danger"></i>{{ $t('问题数') }}</bk-col
          >
          <bk-col
          ><i class="codecc-icon icon-trend"></i>{{ $t('问题趋势') }}</bk-col
          >
          <bk-col
          ><i class="codecc-icon icon-risky-file"></i
          >{{ $t('文件数') }}</bk-col
          >
          <bk-col
          ><i class="codecc-icon icon-trend"></i
          >{{ $t('文件数趋势') }}</bk-col
          >
        </bk-row>
      </bk-container>
    </div>
    <div class="card-empty" v-if="!data.lastAnalysisResult">
      <empty
        :title="data.toolName ? $t('暂无分析结果') : $t('暂无工具')"
        size="small"
      />
    </div>
  </div>
</template>
<script>
import { mapState } from 'vuex';
import { format } from 'date-fns';
import { getToolStatus, formatSeconds } from '@/common/util';
import Empty from '@/components/empty';
import { language } from '../../i18n';

export default {
  components: {
    Empty,
  },
  props: {
    data: {
      type: Object,
      default() {
        return {
          lastAnalysisTime: 0,
          elapseTime: 0,
          lastAnalysisResult: {},
          toolName: '',
        };
      },
    },
  },
  data() {
    return {};
  },
  computed: {
    ...mapState('tool', {
      toolMap: 'mapList',
    }),
    ...mapState(['toolMeta']),
    toolType() {
      const obj = {};
      this.toolMeta.TOOL_TYPE.forEach((item) => {
        if (item.name === '代码安全') item.name = '安全漏洞';
        obj[item.key] = item.name;
      });
      return obj;
    },
    stepStatus() {
      let stepStatus = 0;
      if (
        this.data.curStep
        && this.data.curStep < 5
        && this.data.curStep > 0
        && this.data.stepStatus !== 1
      ) {
        stepStatus = 3;
      } else if (this.data.stepStatus === 1) {
        stepStatus = 1;
      }
      return stepStatus;
    },
  },
  created() {},
  methods: {
    formatDate(date) {
      if (!date) return '--';
      return format(date, 'yyyy-MM-dd HH:mm:ss');
    },
    formatSeconds(s) {
      return formatSeconds(s);
    },
    getToolStatus(num, tool) {
      return getToolStatus(num, tool);
    },
    toLogs() {
      const { params } = this.$route;
      params.toolId = this.data.toolName;
      this.$router.push({
        name: 'task-detail-log',
        params,
      });
    },
    toList(query = {}, name = '') {
      let routerName = '';
      switch (this.data.toolName) {
        case 'CCN':
          routerName = 'defect-ccn-list';
          break;
        case 'DUPC':
          routerName = 'defect-dupc-list';
          break;
        case 'CLOC':
          routerName = 'defect-cloc-list';
          break;
        case 'COVERITY':
          routerName = 'defect-coverity-list';
          break;
        case 'KLOCWORK':
          routerName = 'defect-coverity-list';
          break;
        case 'PINPOINT':
          routerName = 'defect-coverity-list';
          break;
        case 'PECKER_SCA':
          routerName = `defect-${name}-list`;
          break;
        default:
          routerName = 'defect-lint-list';
          break;
      }
      const { params } = this.$route;
      params.toolId = this.data.toolName;
      if (this.data.buildNum === this.$route.query.buildNum) {
        query.buildId = this.data.buildId;
      }
      this.$router.push({
        name: routerName,
        params,
        query,
      });
    },
    handleBigNum(number) {
      return number > 99999999 ? '1亿+' : number;
    },
  },
};
</script>
<style lang="postcss" scoped>
@import url('../../css/variable.css');

.up {
  margin-left: -10px;
  color: $failColor;
}

.down {
  margin-left: -10px;
  color: $successColor;
}

.card {
  width: 100%;
  height: 130px;

  /* border: 1px solid $borderColor; */
  padding: 12px;
  margin-bottom: 10px;
  background: #fff;

  &:hover {
    /* box-shadow: 0 3px 8px 0 rgba(0, 0, 0, 0.2), 0 0 0 1px rgba(0, 0, 0, 0.08); */

    /* cursor: pointer; */

    /* .card-head .card-time {
                transition: opacity 0s ease-in-out;
                opacity: 0;
                font-size: 0;
            } */

    /* .card-head .card-step {
                transition: opacity 0s ease-in-out;
                opacity: 0;
                font-size: 0;
            } */

    /* .card-head .card-analys {
                display: block;
                transition: opacity 500ms ease-in-out;
                opacity: 1;
                font-size: 12px;
            } */
    .card-step-txt {
      cursor: pointer;

      /* text-decoration: underline; */
    }

    a {
      cursor: pointer;
    }

    .card-content {
      .card-num {
        a {
          color: #3a84ff;
        }
      }
    }
  }

  .card-head {
    .card-tool {
      padding: 0 9px 0 3px;
      font-size: 14px;
      font-weight: bolder;
      color: #63656e;
    }

    .card-split {
      padding: 0 6px 0 3px;
      color: #dcdee5;
    }

    .card-type {
      display: inline-block;
      padding: 2px 6px;
      margin-top: 4px;
      margin-left: 4px;
      font-size: 12px;
      color: #737987;
      background-color: #c9dffa;
      border-radius: 2px;
    }

    .card-step {
      float: right;
      padding-right: 10px;
      font-size: 12px;
      opacity: 100%;
      transition: opacity 500ms ease-in-out;

      .card-tool-status {
        position: relative;
        font-size: 14px;

        /* top: 1px; */
      }
    }

    .card-time {
      float: right;
      font-size: 12px;
      color: #979ba5;

      /* width: 80px; */
      text-align: right;
      opacity: 100%;
      transition: opacity 500ms ease-in-out;
    }

    .card-analys {
      float: right;
      font-size: 0;

      /* width: 80px; */
      text-align: right;
      opacity: 0%;
    }
  }

  .card-content {
    width: 100%;
    font-size: 12px;
    text-align: center;

    .card-num {
      padding: 18px 0 4px;
      font-size: 24px;

      a {
        color: #000;
      }

      span {
        font-size: 16px;
      }

      .bk-icon {
        position: absolute;
        bottom: 2px;
        padding-left: 7px;
        font-size: 24px;
      }
    }

    .card-txt {
      i {
        padding: 0 3px;
      }
    }
  }

  .card-num {
    display: flex;
  }

  .card-empty {
    /* padding-top: 10px; */
    >>> .empty .title {
      color: #737987;
    }

    >>> .empty .empty-img {
      display: inline-block;
      width: 110px;
    }
  }
}

.spin-icon {
  display: inline-block;
  font-size: 14px;
  color: $primaryColor;
  animation: loading 0.8s linear infinite;
}

@keyframes loading {
  from {
    transform: rotate(0);
  }

  to {
    transform: rotate(360deg);
  }
}
</style>
