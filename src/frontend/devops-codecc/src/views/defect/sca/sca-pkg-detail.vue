<template>
  <div class="detail-container">
    <div class="detail-content">
      <div class="detail-content-header-info">
        <SeverityStatus :cur-severity="detail.severity" type="pkgTag"></SeverityStatus>
        <span class="package-name">{{ detail.name }}</span>
        <bk-tag type="stroke" v-if="detail.version">{{ detail.version }}</bk-tag>
      </div>
      <div class="detail-content-items">
        <div class="package-title">
          <span class="package-title-text">{{ $t('基本信息') }}:</span>
        </div>
        <div class="package-info">
          <div class="package-info-items">
            <div class="package-info-item">
              <span class="package-label">{{ $t('语言') }}:</span>
              <span class="package-value">{{ detail.language || '-' }}</span>
            </div>
            <div class="package-info-item">
              <span class="package-label">{{ $t('发布日期') }}:</span>
              <span class="package-value">{{ detail.releaseDate || '-' }}</span>
            </div>
          </div>
        </div>
        <div class="package-info">
          <span class="package-label">{{ $t('组件描述') }}:</span>
          <span class="package-value">{{ detail.description || '-' }}</span>
        </div>
        <div class="package-info">
          <div class="package-info-items">
            <div class="package-info-item">
              <span class="package-label">{{ $t('下载地址') }}:</span>
              <span class="package-value color-purple">{{ detail.downloadLocation || '-' }}</span>
            </div>
            <div class="package-info-item">
              <span class="package-label">{{ $t('主页地址') }}:</span>
              <span class="package-value color-purple">{{ detail.homepage || '-' }}</span>
            </div>
          </div>
        </div>
        <div class="package-info">
          <div class="package-info-items">
            <div class="package-info-item">
              <span class="package-label">{{ $t('源码地址') }}:</span>
              <span class="package-value color-purple">{{ detail.sourceUrl || '-' }}</span>
            </div>
            <div class="package-info-item">
              <span class="package-label">{{ $t('文档地址') }}:</span>
              <span class="package-value">{{ detail.docUrl || '-' }}</span>
            </div>
          </div>
        </div>
        <div class="package-info">
          <div class="package-info-items">
            <div class="package-info-item">
              <span class="package-label">{{ $t('依赖方式') }}:</span>
              <span class="package-value">{{ getDirect(detail.direct) }}</span>
            </div>
            <div class="package-info-item">
              <span class="package-label">{{ $t('依赖层级') }}:</span>
              <span class="package-value">{{ detail.depth || '-' }}</span>
            </div>
          </div>
        </div>
        <div class="package-info">
          <span class="package-label">{{ $t('依赖来源') }}:</span>
          <span class="package-value">{{ detail.filesAnalyzed ? $t('模糊匹配') : $t('包管理器') }}</span>
        </div>
      </div>
      <div class="detail-content-items">
        <div class="package-title">
          <span class="package-title-text">{{ $t('文件位置') }}</span>
        </div>
        <bk-table
          ext-cls="import-file-table"
          :data="detail.fileInfos"
          :pagination="fileInfosPagination">
          <bk-table-column
            :label="$t('文件绝对路径')"
            min-width="80"
            prop="filePath">
            <template slot-scope="props">
              <span class="purple-text">{{ props.row.filePath || '--' }}</span>
            </template>
          </bk-table-column>
        </bk-table>
      </div>
      <div class="detail-content-items">
        <div class="package-title">
          <span class="package-title-text">{{ $t('许可证信息') }}</span>
        </div>
        <bk-table
          ext-cls="license-info-table"
          :data="detail.licenseList"
          :pagination="licensePagination">
          <bk-table-column
            :label="$t('许可证名称')"
            min-width="80"
            prop="name">
            <template slot-scope="props">
              <span class="purple-text">{{ props.row.name || '--' }}</span>
            </template>
          </bk-table-column>
          <bk-table-column
            :label="$t('许可证全名')"
            min-width="240"
            prop="fullName">
            <template slot-scope="props">
              <span>{{ props.row.fullName || '--' }}</span>
            </template>
          </bk-table-column>
          <bk-table-column
            :label="$t('风险等级')"
            min-width="100"
            prop="severity">
            <template slot-scope="props">
              <SeverityStatus :cur-severity="props.row.severity" type="lic"></SeverityStatus>
            </template>
          </bk-table-column>
          <bk-table-column
            :label="$t('OSI 认证')"
            min-width="50"
            prop="osi">
            <template slot-scope="props">
              <i class="bk-icon success-icon codecc-icon icon-check-line" v-if="props.row.osi" />
              <i class="bk-icon fail-icon codecc-icon icon-close-line" v-else />
            </template>
          </bk-table-column>
          <bk-table-column
            :label="$t('FSF 许可')"
            min-width="50"
            prop="fsf">
            <template slot-scope="props">
              <i class="bk-icon success-icon codecc-icon icon-check-line" v-if="props.row.fsf" />
              <i class="bk-icon fail-icon codecc-icon icon-close-line" v-else />
            </template>
          </bk-table-column>
          <bk-table-column
            :label="$t('SPDX 认证')"
            min-width="80"
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
      <!-- 隐藏漏洞数 -->
      <!-- <div class="detail-content-items">
        <div class="package-title">
          <span class="package-title-text">{{ $t('漏洞信息') }}</span>
        </div>
        <bk-table
          ext-cls="vulnerability-info-table"
          :data="detail.vulnerabilityList"
          :pagination="vulnPagination">
          <bk-table-column
            :label="$t('风险等级')"
            min-width="50"
            show-overflow-tooltip
            prop="severity">
            <template slot-scope="props">
              <SeverityStatus :cur-severity="props.row.severity" type="vuln"></SeverityStatus>
            </template>
          </bk-table-column>
          <bk-table-column
            :label="$t('漏洞名称')"
            min-width="100"
            show-overflow-tooltip
            prop="vulName">
            <template slot-scope="props">
              <span class="purple-text">{{ props.row.vulName || '--' }}</span>
            </template>
          </bk-table-column>
          <bk-table-column
            :label="$t('漏洞编号')"
            min-width="200"
            show-overflow-tooltip
            prop="vulnerabilityIds">
            <template slot-scope="props">
              <span class="purple-text">{{ props.row.vulnerabilityIds.join(', ') || '--' }}</span>
            </template>
          </bk-table-column>
          <bk-table-column
            :label="$t('漏洞类型')"
            min-width="100"
            show-overflow-tooltip
            prop="vulType">
            <template slot-scope="props">
              <span>{{ props.row.vulType || '--' }}</span>
            </template>
          </bk-table-column>
          <bk-table-column
            :label="$t('攻击类型')"
            min-width="100"
            show-overflow-tooltip
            prop="attackType">
            <template slot-scope="props">
              <span>{{ props.row.attackType || '--' }}</span>
            </template>
          </bk-table-column>
          <bk-table-column
            :label="$t('CVSS 评分')"
            min-width="100"
            show-overflow-tooltip
            prop="cvss_rate">
            <template slot-scope="props">
              <div class="cvss-display">
                <div>{{ `V3: ${props.row.cvssV3}` }}</div>
                <div>{{ `V2: ${props.row.cvssV2}` }}</div>
              </div>
            </template>
          </bk-table-column>
          <div slot="empty">
            <div class="codecc-table-empty-text">
              <img src="../../../images/empty.png" class="empty-img" />
              <div>{{ $t('没有查询到数据') }}</div>
            </div>
          </div>
        </bk-table>
      </div> -->
    </div>
  </div>
</template>

<script>
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
    packageName: {
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
        buildId: this.buildId,
        packageName: this.packageName,
        dimension: 'SCA',
        entityId: this.entityId,
        pattern: 'SCA',
        scaDimensionList: ['PACKAGE'],

        // query
        sortField: 'SEVERITY', // 按严重程度排序
        sortType: 'ASC', // 排序方式
      },
      lintDetail: {
        scaPackageDetailVO: {},
      },
      vulnPagination: {
        current: 1,
        count: 0,
        limit: 10,
        showLimit: false,
      },
      licensePagination: {
        current: 1,
        count: 0,
        limit: 10,
        showLimit: false,
      },
      fileInfosPagination: {
        current: 1,
        count: 0,
        limit: 10,
        showLimit: false,
      },
    };
  },
  computed: {
    detail() {
      return this.lintDetail.scaPackageDetailVO;
    },
  },
  created() {
    this.fetchLintDetail();
  },
  methods: {
    handleCloseLicDetail() {
      this.$emit('closeDetail');
    },
    async fetchLintDetail() {
      const res = await this.$store.dispatch('defect/lintDetail', this.searchParams);
      this.lintDetail.scaPackageDetailVO = res.scaPackageDetailVO;
      const licenseList = res.scaPackageDetailVO.licenseList || [];
      this.licensePagination.count = licenseList.length;
    },
    getDirect(direct) {
      if (direct === '') return '-';
      return direct ? this.$t('直接依赖') : this.$t('间接依赖');
    },
  },
};
</script>

<style lang="postcss" scoped>
.detail-container {
  width: 100%;
  height: 100%;
  overflow-y: auto;
  background-color:#f8fafb;

  .detail-content {
    height: calc(100% - 54px);
    padding: 10px;
    padding-right: 20px;
    padding-left: 20px;
    margin-top: 15px;
    background-color: #fff;

    .package-info {
      padding-top: 15px;
      font-size: 14px;
      color: #000;

      .package-info-items {
        display: flex;
        width: 100%;

        .package-info-item {
          flex: 1;

          .package-label {
            margin-right: 5px;
            color: #727272;
          }
        }
      }

      .package-label {
        margin-right: 5px;
        font-weight: bold;
        color: #727272;
      }
    }

    .detail-content-header-info {
      margin-left: -5px;

      .package-name {
        margin-right: 5px;
        margin-left: 10px;
        font-weight: bold;
        color: #000;
      }
    }

    .detail-content-items {
      .package-title {
        display: flex;
        align-items: center;
        margin-top: 20px;
        color: #000;

        .package-title-text {
          font-weight: bold;
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

      .import-file-table {
        margin-top: 20px;
        margin-bottom: 50px;
      }

      .license-info-table {
        margin-top: 20px;
      }

      .vulnerability-info-table {
        margin-top: 20px;
        margin-bottom: 50px;
      }
    }
  }

  .color-purple {
    color: #9e3e8f !important;
  }

  .fail-icon {
    font-size: 22px;
    color: #e2646d;
  }

  .success-icon {
    font-size: 22px;
    color: #75ca87;
  }

  /deep/ .cvss-display {
    display: flex;
    flex-direction: column;
    padding: 10px;
  }
}
</style>
