<template>
  <div class="dimension-card">
    <section class="card-item" v-for="item in clusterList" :key="item.type">
      <bk-container
        :col="5"
        :gutter="4"
        v-if="
          item.baseClusterResultVO && item.baseClusterResultVO.type === 'DEFECT'
        "
      >
        <bk-row>
          <bk-col class="card-name">
            <div class="desc">
              {{ $t('代码缺陷') }}
            </div>
            <div
              class="name"
              :title="formatTool(item.baseClusterResultVO.toolList)"
            >
              {{ item.baseClusterResultVO.toolNum || 0 }}{{ $t('个工具') }}
            </div>
          </bk-col>
          <bk-col class="history">
            <div class="number" @click="handleToPage('defect')">
              {{ item.baseClusterResultVO.totalCount | formatUndefNum }}
            </div>
            <div class="name">{{ $t('遗留问题数') }}</div>
          </bk-col>
          <bk-col class="new">
            <div class="number" @click="handleToPage('defect')">
              {{ item.baseClusterResultVO.newDefectCount | formatUndefNum }}
            </div>
            <div class="name">{{ $t('新增问题数') }}</div>
          </bk-col>
          <bk-col>
            <div class="number" @click="handleToPage('defect')">
              {{ item.baseClusterResultVO.fixDefectCount | formatUndefNum }}
            </div>
            <div class="name">{{ $t('修复问题数') }}</div>
          </bk-col>
          <bk-col class="filter">
            <div class="number" @click="handleToPage('defect')">
              {{ item.baseClusterResultVO.maskDefectCount | formatUndefNum }}
            </div>
            <div class="name">{{ $t('屏蔽问题数') }}</div>
          </bk-col>
        </bk-row>
      </bk-container>
      <bk-container
        :col="5"
        :gutter="4"
        v-else-if="
          item.baseClusterResultVO &&
            item.baseClusterResultVO.type === 'SECURITY'
        "
      >
        <bk-row>
          <bk-col class="card-name">
            <div class="desc">
              {{ $t('安全漏洞') }}
              <bk-popover>
                <i
                  class="f18"
                  :class="{
                    'codecc-icon icon-safety-fill grey':
                      !item.baseClusterResultVO.toolList,
                    'bk-icon icon-exclamation-triangle-shape fail':
                      rdScore.codeSecurityScore < 60,
                    'bk-icon icon-exclamation-circle-shape warn':
                      rdScore.codeSecurityScore >= 60 &&
                      rdScore.codeSecurityScore < 80,
                    'codecc-icon icon-safety-fill success':
                      rdScore.codeSecurityScore >= 80,
                  }"
                ></i>
                <div slot="content">
                  <p>
                    {{ $t('【安全漏洞】严重问题数')
                    }}{{
                      rdScore.codeSecuritySeriousDefectCount | formatUndefNum
                    }}， {{ $t('一般问题数')
                    }}{{
                      rdScore.codeSecurityNormalDefectCount | formatUndefNum
                    }}，{{ $t('得分：')
                    }}<b>{{
                      rdScore.codeSecurityScore | formatUndefNum('fixed', 2)
                    }}</b>
                  </p>
                </div>
              </bk-popover>
            </div>
            <div
              class="name"
              :title="formatTool(item.baseClusterResultVO.toolList)"
            >
              {{ item.baseClusterResultVO.toolNum || 0 }}{{ $t('个工具') }}
            </div>
          </bk-col>
          <bk-col class="history">
            <div class="number" @click="handleToPage('security')">
              {{ item.baseClusterResultVO.totalCount | formatUndefNum }}
            </div>
            <div class="name">{{ $t('遗留问题数') }}</div>
          </bk-col>
          <bk-col class="new">
            <div class="number" @click="handleToPage('security')">
              {{ item.baseClusterResultVO.newDefectCount | formatUndefNum }}
            </div>
            <div class="name">{{ $t('新增问题数') }}</div>
          </bk-col>
          <bk-col>
            <div class="number" @click="handleToPage('security')">
              {{ item.baseClusterResultVO.fixDefectCount | formatUndefNum }}
            </div>
            <div class="name">{{ $t('修复问题数') }}</div>
          </bk-col>
          <bk-col class="filter">
            <div class="number" @click="handleToPage('security')">
              {{ item.baseClusterResultVO.maskDefectCount | formatUndefNum }}
            </div>
            <div class="name">{{ $t('屏蔽问题数') }}</div>
          </bk-col>
        </bk-row>
      </bk-container>
      <bk-container
        :col="5"
        :gutter="4"
        v-else-if="
          item.baseClusterResultVO &&
            item.baseClusterResultVO.type === 'STANDARD'
        "
      >
        <bk-row>
          <bk-col class="card-name">
            <div class="desc">
              {{ $t('代码规范') }}
              <bk-popover>
                <i
                  class="f18"
                  :class="{
                    'codecc-icon icon-safety-fill grey':
                      !item.baseClusterResultVO.toolList,
                    'bk-icon icon-exclamation-triangle-shape fail':
                      rdScore.codeStyleScore < 60,
                    'bk-icon icon-exclamation-circle-shape warn':
                      rdScore.codeStyleScore >= 60 &&
                      rdScore.codeStyleScore < 80,
                    'codecc-icon icon-safety-fill success':
                      rdScore.codeStyleScore >= 80,
                  }"
                ></i>
                <div slot="content">
                  <p>
                    {{ $t('【代码规范】严重问题数密度')
                    }}{{
                      rdScore.averageSeriousStandardThousandDefect
                        | formatUndefNum('fixed', 2)
                    }}{{ $t('千行，') }} {{ $t('一般问题数密度')
                    }}{{
                      rdScore.averageNormalStandardThousandDefect
                        | formatUndefNum('fixed', 2)
                    }}{{ $t('千行，') }}
                    <b
                    >{{ $t('得分：')
                    }}{{
                      rdScore.codeStyleScore | formatUndefNum('fixed', 2)
                    }}</b
                    >
                  </p>
                </div>
              </bk-popover>
            </div>
            <div
              class="name"
              :title="formatTool(item.baseClusterResultVO.toolList)"
            >
              {{ item.baseClusterResultVO.toolNum || 0 }}{{ $t('个工具') }}
            </div>
          </bk-col>
          <bk-col class="history">
            <div class="number" @click="handleToPage('standard')">
              {{ item.baseClusterResultVO.totalCount | formatUndefNum }}
            </div>
            <div class="name">{{ $t('遗留问题数') }}</div>
          </bk-col>
          <bk-col class="new">
            <div class="number" @click="handleToPage('standard')">
              {{
                item.baseClusterResultVO.defectChange | formatUndefNum('abs')
              }}
              <i
                class="bk-icon f12"
                :class="{
                  'icon-up-shape fail':
                    item.baseClusterResultVO.defectChange > 0,
                  'icon-down-shape success':
                    item.baseClusterResultVO.defectChange < 0,
                }"
              >
              </i>
            </div>
            <div class="name">{{ $t('问题趋势') }}</div>
          </bk-col>
          <bk-col>
            <div class="number" @click="handleToPage('standard')">
              {{
                (item.baseClusterResultVO.averageThousandDefect &&
                  item.baseClusterResultVO.averageThousandDefect.toFixed(1))
                  | formatUndefNum
              }}
            </div>
            <div class="name">{{ $t('千行问题数') }}</div>
          </bk-col>
          <bk-col class="filter">
            <div class="number" @click="handleToPage('standard')">
              {{
                (item.baseClusterResultVO.averageThousandDefectChange &&
                  item.baseClusterResultVO.averageThousandDefectChange.toFixed(
                    1
                  )) | formatUndefNum('abs')
              }}
              <i
                class="bk-icon f12"
                :class="{
                  'icon-up-shape fail':
                    item.baseClusterResultVO.averageThousandDefectChange > 0,
                  'icon-down-shape success':
                    item.baseClusterResultVO.averageThousandDefectChange < 0,
                }"
              >
              </i>
            </div>
            <div class="name">{{ $t('千行问题数趋势') }}</div>
          </bk-col>
        </bk-row>
      </bk-container>
      <bk-container
        :col="5"
        :gutter="4"
        v-else-if="
          item.baseClusterResultVO && item.baseClusterResultVO.type === 'CCN'
        "
      >
        <bk-row>
          <bk-col class="card-name">
            <div class="desc">
              {{ $t('圈复杂度') }}
              <bk-popover>
                <i
                  class="f18"
                  :class="{
                    'codecc-icon icon-safety-fill grey':
                      !item.baseClusterResultVO.toolList,
                    'bk-icon icon-exclamation-triangle-shape fail':
                      rdScore.codeCcnScore < 60,
                    'bk-icon icon-exclamation-circle-shape warn':
                      rdScore.codeCcnScore >= 60 && rdScore.codeCcnScore < 80,
                    'codecc-icon icon-safety-fill success':
                      rdScore.codeCcnScore >= 80,
                  }"
                ></i>
                <div slot="content">
                  <p>
                    {{ $t('【圈复杂度】千行超标复杂度')
                    }}{{
                      item.baseClusterResultVO.averageThousandDefect
                        | formatUndefNum('fixed', 2)
                    }}，
                    <b
                    >{{ $t('得分：')
                    }}{{
                      rdScore.codeCcnScore | formatUndefNum('fixed', 2)
                    }}</b
                    >
                  </p>
                </div>
              </bk-popover>
            </div>
            <div
              class="name"
              :title="formatTool(item.baseClusterResultVO.toolList)"
            >
              {{ item.baseClusterResultVO.toolNum || 0 }}{{ $t('个工具') }}
            </div>
          </bk-col>
          <bk-col class="history">
            <div class="number" @click="handleToPage('ccn')">
              {{ item.baseClusterResultVO.totalCount | formatUndefNum }}
            </div>
            <div class="name">{{ $t('风险函数个数') }}</div>
          </bk-col>
          <bk-col class="new">
            <div class="number" @click="handleToPage('ccn')">
              {{ item.baseClusterResultVO.totalChange | formatUndefNum('abs') }}
              <i
                class="bk-icon f12"
                :class="{
                  'icon-up-shape fail':
                    item.baseClusterResultVO.totalChange > 0,
                  'icon-down-shape success':
                    item.baseClusterResultVO.totalChange < 0,
                }"
              >
              </i>
            </div>
            <div class="name">{{ $t('风险函数个数趋势') }}</div>
          </bk-col>
          <bk-col>
            <div class="number" @click="handleToPage('ccn')">
              {{
                (item.baseClusterResultVO.averageThousandDefect &&
                  item.baseClusterResultVO.averageThousandDefect.toFixed(1))
                  | formatUndefNum
              }}
            </div>
            <div class="name">{{ $t('千行超标复杂度') }}</div>
          </bk-col>
          <bk-col class="filter">
            <div class="number" @click="handleToPage('ccn')">
              {{
                (item.baseClusterResultVO.averageThousandDefectChange &&
                  item.baseClusterResultVO.averageThousandDefectChange.toFixed(
                    1
                  )) | formatUndefNum('abs')
              }}
              <i
                class="bk-icon f12"
                :class="{
                  'icon-up-shape fail':
                    item.baseClusterResultVO.averageThousandDefectChange > 0,
                  'icon-down-shape success':
                    item.baseClusterResultVO.averageThousandDefectChange < 0,
                }"
              >
              </i>
            </div>
            <div class="name">{{ $t('千行超标复杂度趋势') }}</div>
          </bk-col>
        </bk-row>
      </bk-container>
      <bk-container
        :col="5"
        :gutter="4"
        v-else-if="
          item.baseClusterResultVO &&
            item.baseClusterResultVO.type === 'DUPC' &&
            item.baseClusterResultVO
        "
      >
        <bk-row>
          <bk-col class="card-name">
            <div class="desc">{{ $t('重复率') }}</div>
            <div
              class="name"
              :title="formatTool(item.baseClusterResultVO.toolList)"
            >
              {{ item.baseClusterResultVO.toolNum || 0 }}{{ $t('个工具') }}
            </div>
          </bk-col>
          <bk-col class="history">
            <div class="number" @click="handleToPage('dupc')">
              {{ item.baseClusterResultVO.totalCount | formatUndefNum }}
            </div>
            <div class="name">{{ $t('overview.重复文件') }}</div>
          </bk-col>
          <bk-col class="new">
            <div class="number" @click="handleToPage('dupc')">
              {{
                item.baseClusterResultVO.defectChange | formatUndefNum('abs')
              }}
              <i
                class="bk-icon f12"
                :class="{
                  'icon-up-shape fail':
                    item.baseClusterResultVO.defectChange > 0,
                  'icon-down-shape success':
                    item.baseClusterResultVO.defectChange < 0,
                }"
              >
              </i>
            </div>
            <div class="name">{{ $t('重复文件趋势') }}</div>
          </bk-col>
          <bk-col>
            <div class="number" @click="handleToPage('dupc')">
              {{
                item.baseClusterResultVO.dupRate &&
                  item.baseClusterResultVO.dupRate.toFixed(1) | formatUndefNum
              }}
              <span
                class="f12"
                v-if="item.baseClusterResultVO.dupRate !== undefined"
              >%</span
              >
            </div>
            <div class="name">{{ $t('平均重复率') }}</div>
          </bk-col>
          <bk-col class="filter">
            <div class="number" @click="handleToPage('dupc')">
              {{
                item.baseClusterResultVO.dupRateChange &&
                  item.baseClusterResultVO.dupRateChange.toFixed(1)
                  | formatUndefNum('abs')
              }}
              <span
                class="f12"
                v-if="item.baseClusterResultVO.dupRateChange !== undefined"
              >%</span
              >
              <i
                class="bk-icon f12"
                :class="{
                  'icon-up-shape fail':
                    item.baseClusterResultVO.dupRateChange > 0,
                  'icon-down-shape success':
                    item.baseClusterResultVO.dupRateChange < 0,
                }"
              >
              </i>
            </div>
            <div class="name">{{ $t('重复率趋势') }}</div>
          </bk-col>
        </bk-row>
      </bk-container>
      <bk-container
        :col="5"
        :gutter="4"
        v-else-if="
          item.baseClusterResultVO &&
            item.baseClusterResultVO.type === 'SCA' &&
            item.baseClusterResultVO
        "
      >
        <bk-row>
          <bk-col class="card-name">
            <div class="desc">{{ $t('软件成分') }}</div>
            <div
              class="name"
              :title="$t('软件成分')"
            >
              {{ item.baseClusterResultVO.toolNum || 0 }}{{ $t('个工具') }}
            </div>
          </bk-col>
          <bk-col class="history">
            <div class="number" @click="handleToPage('sca-pkg')">
              {{ item.baseClusterResultVO.packageCount | formatUndefNum }}
            </div>
            <div class="name">{{ $t('组件数') }}</div>
          </bk-col>
          <!-- 隐藏漏洞数 -->
          <!-- <bk-col class="new">
            <div class="number" @click="handleToPage('sca-vuln')">
              {{ item.baseClusterResultVO.newVulCount | formatUndefNum }}
            </div>
            <div class="name">{{ $t('漏洞数') }}</div>
          </bk-col> -->
          <bk-col>
            <div class="number" @click="handleToPage('sca-lic')">
              {{ item.baseClusterResultVO.licenseCount | formatUndefNum }}
            </div>
            <div class="name">{{ $t('许可证数') }}</div>
          </bk-col>
        </bk-row>
      </bk-container>
    </section>
  </div>
</template>

<script>
import { mapState } from 'vuex';

export default {
  props: {
    clusterList: {
      type: Array,
      default: [],
    },
    rdScore: {
      type: Object,
      default: {},
    },
  },
  data() {
    return {};
  },
  computed: {
    ...mapState('tool', {
      toolMap: 'mapList',
    }),
    ...mapState('task', {
      taskDetail: 'detail',
    }),
    handleOpenHref() {
      return window.IWIKI_OPEN_SCORE;
    },
  },
  created() {},
  methods: {
    handleToPage(name) {
      this.$router.push({
        name: `defect-${name}-list`,
        query: {
          dimension: name.toUpperCase(),
          from: 'overview',
          isOpenScan: this.rdScore.openScan,
        },
      });
    },
    formatTool(tools = []) {
      return tools
        .map(tool => (this.toolMap[tool] && this.toolMap[tool].displayName) || '')
        .join(', ');
    },
  },
};
</script>

<style lang="postcss" scoped>
@import url('../../css/variable.css');

.card-item {
  height: 130px;
  padding: 35px;
  margin-bottom: 10px;
  text-align: center;
  background: #fff;

  &:hover {
    .number {
      color: #3a84ff;
    }
  }

  .card-name {
    height: 66px;
    text-align: left;
    border-right: 1px solid #dcdee5;
  }

  .desc {
    font-size: 14px;
    font-weight: bold;
    line-height: 35px;
    color: #63656e;
  }

  .number {
    padding-bottom: 8px;
    font-size: 24px;
    color: #000;
    cursor: pointer;
  }

  .name {
    font-size: 12px;
    color: #979ba5;
    cursor: default;
  }
}

.grey {
  color: #979ba5;
}
</style>
