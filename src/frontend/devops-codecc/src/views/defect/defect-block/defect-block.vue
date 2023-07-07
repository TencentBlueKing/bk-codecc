<template>
  <div class="defect-block" id="defect-block">
    <defect-block-item
      v-for="(defect, key) in list"
      :key="key"
      :defect="defect"
      :active="key === defectIndex"
      :defect-instances="key === defectIndex ? defectInstances : []"
      :handle-file-list-row-click="handleFileListRowClick"
      :trace-active-id="traceActiveId"
      @clickTrace="clickTrace">
    </defect-block-item>
    <div v-if="isFileListLoadMore" class="center">
      {{$t('正在加载第x-y个，请稍后···', { x: nextPageStartNum, y: nextPageEndNum })}}
    </div>
  </div>
</template>

<script>
  import DefectBlockItem from './defect-block-item'

  export default {
    name: 'defect-block',
    components: {
      DefectBlockItem,
    },
    props: {
      list: Array,
      defectIndex: Number,
      handleFileListRowClick: Function,
      isFileListLoadMore: Boolean,
      defectInstances: Array,
      traceActiveId: String,
      nextPageStartNum: Number,
      nextPageEndNum: Number,
    },
    data() {
      return {
        defectBlockDom: null,
        isThrottled: false,
        isMoreLoading: false,
      }
    },
    watch: {
      defectIndex() {
        setTimeout(() => {
          if (document.querySelector('#defect-block .active')) {
            document.querySelector('#defect-block .active').scrollIntoView({ block: 'center' })
          }
        })
      },
    },
    created() {
      this.$nextTick(() => {
        this.defectBlockDom = document.querySelector('#defect-block')
        this.defectBlockDom.addEventListener('scroll', this.scrollLoading)
      })
    },
    beforeDestroy() {
      this.defectBlockDom.removeEventListener('scroll', this.scrollLoading)
    },
    methods: {
      scrollLoading() {
        if (!this.isThrottled && !this.isMoreLoading) {
          this.isThrottled = true
          this.isTimer = setTimeout(async () => {
            this.isThrottled = false
            const { scrollHeight, offsetHeight, scrollTop } = this.defectBlockDom
            if (scrollHeight - offsetHeight - scrollTop < 60) {
              this.$emit('scrollLoadMore')
            }
          }, 300)
        }
      },
      clickTrace(trace) {
        this.$emit('clickTrace', trace)
      },
    },
  }
</script>

<style lang="postcss" scoped>
  .defect-block {
    padding: 8px 0;
    max-height: calc(100vh - 48px);
    overflow: auto;
  }
  .center {
    text-align: center;
  }
</style>
