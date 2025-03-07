<template>
  <div class="severity-status-container" v-if="curSeverityMap[curSeverity]">
    <div class="severity-status-content" v-if="type !== 'pkg'">
      <bk-tag :theme="!isNaN(curSeverity) ? curSeverityMap[curSeverity].theme : ''">
        {{ !isNaN(curSeverity) ? curSeverityMap[curSeverity].text : '' }}
      </bk-tag>
    </div>

    <div class="severity-status-content" v-else>
      <span v-if="curSeverity === 0">{{ curSeverityMap[curSeverity].text }}</span>
      <span class="serious-text" v-if="curSeverity === 1">{{ curSeverityMap[curSeverity].text }}</span>
      <span class="medium-text" v-if="curSeverity === 2">{{ curSeverityMap[curSeverity].text }}</span>
      <span class="low-text" v-if="curSeverity === 4">{{ curSeverityMap[curSeverity].text }}</span>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    curSeverity: {
      type: String,
      default: '',
    },
    type: {
      type: String,
      default: '',
    },
  },
  data() {
    return {
      licSeverityMap: {
        0: {
          theme: '',
          text: this.$t('未知'),
        },
        1: {
          theme: 'danger',
          text: this.$t('高危'),
        },
        2: {
          theme: 'warning',
          text: this.$t('中危'),
        },
        4: {
          theme: 'info',
          text: this.$t('低危'),
        },
      },
      vulnSeverityMap: {
        1: {
          theme: 'danger',
          text: this.$t('高危'),
        },
        2: {
          theme: 'warning',
          text: this.$t('中危'),
        },
        4: {
          theme: 'info',
          text: this.$t('低危'),
        },
      },
      pkgSeverityMap: {
        0: {
          theme: '',
          text: this.$t('未知'),
        },
        1: {
          theme: 'danger',
          text: this.$t('高'),
        },
        2: {
          theme: 'warning',
          text: this.$t('中'),
        },
        4: {
          theme: 'info',
          text: this.$t('低'),
        },
      },
    };
  },
  computed: {
    curSeverityMap() {
      if (this.type === 'pkg') return this.pkgSeverityMap;
      if (this.type === 'pkgTag') return this.pkgSeverityMap;
      if (this.type === 'vuln') return this.vulnSeverityMap;
      if (this.type === 'lic') return this.licSeverityMap;
      return {};
    },
  },
};
</script>

<style lang="postcss" scoped>
.severity-status-container {
  display: inline-block;

  .severity-status-content {
    display: inline-block;
  }
}

/deep/ .serious-tag {
  font-weight: bold;
  color: #a43a43;
  background-color: #f0dddf;
}

.serious-text {
  color: #c86861;
}

.high-text {
  color: #FF5656;
}

.medium-text {
  color: #FFB848;
}

.low-text {
  color: #1768EF;
}
</style>
