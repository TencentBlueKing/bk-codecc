<template>
  <bk-transfer
    :target-list="targetList"
    :source-list="list"
    :title="title"
    :empty-content="emptyContent"
    @change="change">
  </bk-transfer>
</template>
<script>
  export default {
    props: {
      visiable: {
        type: Boolean,
        default: true,
      },
      list: {
        type: Array,
        default: [],
      },
      targetList: {
        type: Array,
        default: [],
      },
    },
    data() {
      return {
        title: [this.$t('不接收通知'), this.$t('接收通知')],
        emptyContent: [this.$t('无数据'), this.$t('未选择')],
        selectedList: [],
      }
    },
    methods: {
      change(sourceList, targetList, tagetValueList) {
        this.selectedList = targetList
      },
    },
  }
</script>
