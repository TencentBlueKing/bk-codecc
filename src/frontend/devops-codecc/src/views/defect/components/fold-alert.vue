<template>
  <div class="fold-alert-container" v-if="filterDataList.length !== 0">
    <bk-alert :type="iconType">
      <div slot="title">
        <div class="alert-header flex">
          <div class="alert-header-title">{{ title }}</div>
          <bk-button
            v-if="isFold"
            text theme="primary"
            class="alert-header-action"
            @click="toggleFold">
            ▼ {{ $t('展开更多') }}
          </bk-button>
          <bk-button
            v-else text theme="primary"
            class="alert-header-action"
            @click="toggleFold">
            ▲ {{ $t('收起更多') }}
          </bk-button>
        </div>
        <div class="alert-content">
          <div v-for="item, index in filterDataList" :key="index" class="alert-content-items">
            <span class="alert-content-name">{{ `${item.fileName}:${item.lineNum} ${item.checker}` }}</span>
            <span class="alert-content-approval">
              <span class="author-name">{{ getApprover(item.ignoreApproverType, item.customIgnoreApprovers) }}</span>
              <span>{{ $t('审批') }}</span>
            </span>
          </div>
          <div class="alert-content-items" v-if="dataList.length > 50 && !isFold">...</div>
        </div>
      </div>
    </bk-alert>
  </div>
</template>

<script>
export default {
  name: 'FoldAlert',
  props: {
    iconType: {
      type: String,
      default: 'warning',
    },
    title: {
      type: String,
    },
    dataList: {
      type: Array,
    },
    maxLength: {
      type: Number,
      default: 50,
    },
    approverList: {
      type: Array,
    },
  },
  data() {
    return {
      isFold: true,
    };
  },
  computed: {
    maxShowLength() {
      return this.isFold ? 1 : this.maxLength;
    },
    filterDataList() {
      return this.dataList.slice(0, this.maxShowLength);
    },
  },
  methods: {
    toggleFold() {
      this.isFold = !this.isFold;
      this.$emit('toggleFold', this.isFold);
    },
    getApprover(approverType, customApprovers) {
      if (approverType !== 'CUSTOM_APPROVER') {
        const approver = this.approverList.find(item => item.key === approverType);
        return approver ? approver.name : '';
      }
      return customApprovers.join(', ');
    },
  },
};
</script>

<style lang="postcss" scoped>
  .fold-alert-container {
    .alert-header {
      justify-content: space-between;
    }

    .alert-content {
      .alert-content-items {
        padding: 2px 0;
        color: #63656E;

        .alert-content-approval {
          margin-left: 24px;

          .author-name {
            font-weight: 700;
          }
        }
      }
    }

    margin-bottom: 21px;
  }
</style>
