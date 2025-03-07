<template>
  <div class="detail-container">
    <div class="detail-header">
      <i class="back-icon codecc-icon icon-arrows-left-line" @click="handleCloseLicDetail"></i>
      <SeverityStatus :cur-severity="detail.severity" type="lic"></SeverityStatus>
      <span class="license-name mr-5">{{ detail.name }}</span>
      <bk-button class="mr-2" v-if="detail.osi">{{ $t('OSI 认证') }}</bk-button>
      <bk-button class="mr-2" v-if="detail.fsf">{{ $t('FSF 许可') }}</bk-button>
      <bk-button class="mr-2" v-if="detail.spdx">{{ $t('SPDX 认证') }}</bk-button>
    </div>
    <div class="detail-content">
      <div class="license-info">
        <div class="license-info-items">
          <div class="license-info-item">
            <span class="license-label">{{ $t('许可证全名') }}:</span>
            <span class="license-value">{{ detail.fullName || '-' }}</span>
          </div>
        </div>
      </div>

      <div class="license-info license-link-info">
        <span class="license-label">{{ $t('许可证链接') }}:</span>
        <div class="license-value license-links">
          <bk-link
            class="color-purple license-link"
            v-for="(link, index) in detail.urls"
            :href="link"
            :key="index"
            target="_blank">
            {{ link }}
          </bk-link>
        </div>
      </div>

      <div class="license-info">
        <span class="license-label">{{ $t('GPL兼容性说明') }}:</span>
        <span class="license-value">{{ detail.gplDesc || '-' }}</span>
      </div>

      <div class="license-info">
        <span class="license-label">{{ $t('风险说明') }}:</span>
        <span class="license-value">{{ detail.severityDesc || '-' }}</span>
      </div>

      <div class="license-info license-summary-info">
        <span class="license-label license-summary-label">{{ $t('摘要') }}:</span>
        <span class="license-value license-summary-value">{{ detail.summary || '-' }}</span>
      </div>

      <div class="licensing">
        <div class="licensing-title">
          <span class="license-title-text">{{ $t('许可授权') }}</span>
        </div>
        <div class="obligations-rights">
          <div class="obligations-box">
            <div class="obligations-box-title">
              {{ $t('为了合理使用该许可证下的代码，您的义务是') }}
            </div>
            <div class="obligations-box-content">
              <div class="obligations-box-content-item">
                <div class="red-header">{{ $t('必须') }}</div>
                <div class="obligations-box-content-list">
                  <div
                    class="obligations-box-content-list-items"
                    v-for="(item, index) in detail.required"
                    :key="index">
                    <div class="obligations-box-content-list-items-title">
                      {{ `${index + 1}、${item.split(': ')[0]}` }}
                    </div>
                    <div class="obligations-box-content-list-items-text">
                      {{ item.split(': ')[1] }}
                    </div>
                  </div>
                </div>
              </div>
              <div class="obligations-box-content-item">
                <div class="green-header">{{ $t('无需') }}</div>
                <div class="obligations-box-content-list">
                  <div
                    class="obligations-box-content-list-items"
                    v-for="(item, index) in detail.unnecessary"
                    :key="index">
                    <div class="obligations-box-content-list-items-title">
                      {{ `${index + 1}、${item.split(': ')[0]}` }}
                    </div>
                    <div class="obligations-box-content-list-items-text">
                      {{ item.split(': ')[1] }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="obligations-box">
            <div class="obligations-box-title">
              {{ $t('该许可证授权您的权利是') }}
            </div>
            <div class="obligations-box-content">
              <div class="obligations-box-content-item">
                <div class="green-header">{{ $t('允许') }}</div>
                <div class="obligations-box-content-list">
                  <div
                    class="obligations-box-content-list-items"
                    v-for="(item, index) in detail.permitted"
                    :key="index">
                    <div class="obligations-box-content-list-items-title">
                      {{ `${index + 1}、${item.split(': ')[0]}` }}
                    </div>
                    <div class="obligations-box-content-list-items-text">
                      {{ item.split(': ')[1] }}
                    </div>
                  </div>
                </div>
              </div>
              <div class="obligations-box-content-item">
                <div class="red-header">{{ $t('禁止') }}</div>
                <div class="obligations-box-content-list">
                  <div
                    class="obligations-box-content-list-items"
                    v-for="(item, index) in detail.forbidden"
                    :key="index">
                    <div class="obligations-box-content-list-items-title">
                      {{ `${index + 1}、${item.split(': ')[0]}` }}
                    </div>
                    <div class="obligations-box-content-list-items-text">
                      {{ item.split(': ')[1] }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
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
    licenseName: {
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
        licenseName: this.licenseName,
        dimension: 'SCA',
        entityId: this.entityId,
        buildId: this.buildId,
        pattern: 'SCA',
        scaDimensionList: ['LICENSE'],

        // query
        sortField: 'SEVERITY', // 按严重程度排序
        sortType: 'ASC', // 排序方式
      },
      lintDetail: {
        scaLicenseDetailVO: {},
      },
    };
  },
  computed: {
    detail() {
      return this.lintDetail.scaLicenseDetailVO;
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
      this.lintDetail.scaLicenseDetailVO = res.scaLicenseDetailVO;
    },
  },
};
</script>

<style lang="postcss" scoped>
.detail-container {
  width: 100%;
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

    .license-name {
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

    .license-link-info {
      display: grid;
      line-height: 25px;
      grid-template-columns: 80px calc(100% - 80px);

      .license-links {
        .license-link {
          display: block;
          margin-top: 5px;

          &:hover {
            color: #ac5fa0;
          }
        }
      }
    }

    .license-summary-info {
      display: grid;
      line-height: 25px;
      grid-template-columns: 40px calc(100% - 40px);

      .license-summary-value {
        max-height: 80px;
        padding-right: 5px;
        overflow: auto;
      }
    }

    .license-info {
      padding-top: 15px;

      .license-info-items {
        display: flex;
        width: 100%;

        .license-info-item {
          flex: 1;

          .license-label {
            margin-right: 5px;
            font-weight: bold;
            color: #727272;
          }

          .license-value {
            font-weight: bold;
          }
        }
      }

      .license-label {
        display: inline-block;
        margin-right: 5px;
        font-weight: bold;
        color: #727272;
      }

      .license-value {
        font-weight: bold;
      }
    }

    .licensing {
      margin-top: 30px;

      .licensing-title {
        display: flex;
        align-items: center;
        margin-bottom: 20px;

        .license-title-text {
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

      .obligations-rights {
        display: flex;
        justify-content: space-between;
        width: 100%;

        .obligations-box {
          width: 49%;
          padding: 20px;
          border: 2px solid #f3f3f4;

          .obligations-box-title {
            margin-bottom: 10px;
            font-weight: bold;
            color: #000;
          }

          .obligations-box-content {
            display: flex;
            width: 100%;
            justify-content: space-between;

            .obligations-box-content-item {
              width: 49%;

              .red-header {
                padding: 10px;
                color: #f8fafb;
                text-align: center;
                background-color: #e15b64;
                border-radius: 2px;
              }

              .green-header {
                padding: 10px;
                color: #f8fafb;
                text-align: center;
                background-color: #74ca86;
                border-radius: 2px;
              }

              .obligations-box-content-list {
                width: 100%;
                height: 380px;
                padding: 20px;
                padding-top: 15px;
                border: 2px solid #f3f3f4;

                .obligations-box-content-list-items {
                  margin-bottom: 15px;

                  .obligations-box-content-list-items-title {
                    margin-bottom: 6px;
                  }

                  .obligations-box-content-list-items-text {
                    margin-left: 20px
                  }
                }
              }
            }
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
