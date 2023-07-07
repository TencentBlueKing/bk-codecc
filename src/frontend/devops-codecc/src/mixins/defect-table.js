import { bus } from '@/common/bus'
export default {
  props: {
    type: {
      type: String,
      default: 'file',
    },
    list: {
      type: Array,
      default: [],
    },
    screenHeight: {
      type: Number,
      default: 336,
    },
    fileIndex: {
      type: Number,
      default: 0,
    },
    handleFileListRowClick: {
      type: Function,
    },
    handleMark: {
      type: Function,
    },
    handleIgnore: {
      type: Function,
    },
    handleAuthor: {
      type: Function,
    },
    handleCommit: {
      type: Function,
    },
    handleSortChange: {
      type: Function,
    },
    handleSelectionChange: {
      type: Function,
    },
    toSelectAll: {
      type: Function,
    },
    prohibitIgnore: {
      type: Boolean,
    },
    isFileListLoadMore: Boolean,
    nextPageStartNum: Number,
    nextPageEndNum: Number,
    handleRevertIgnoreAndMark: {
      type: Function,
    },
    handleRevertIgnoreAndCommit: {
      type: Function,
    },
    handleChangeIgnoreType: {
      type: Function,
    },
    guideFlag: {
      type: String,
      default: '',
    },
    taskListMap: {
      type: Object,
    },
    isProjectDefect: Boolean,
    handleStatus: Function,
  },
  data() {
    return {
      defectSeverityMap: {
        1: this.$t('严重'),
        2: this.$t('一般'),
        4: this.$t('提示'),
      },
      hoverAuthorIndex: -1,
    }
  },
  created() {
    bus.$on('handleNextGuide', () => {
      if (!localStorage.getItem('guide2End')) {
        const index = this.list.findIndex(item => item.status === 1)
        document.getElementsByClassName('guide-icon')[index]?.click()
        setTimeout(() => {
          document.getElementsByClassName('guide-flag')[0]?.click()
        }, 200)
      }
    })
  },
  methods: {
    handleAuthorIndex(index) {
      this.hoverAuthorIndex = index
    },
    handleRowClassName({ row, rowIndex }) {
      let rowClass = 'list-row'
      if (this.fileIndex === rowIndex) rowClass += ' current-row'
      return rowClass
    },
    formatSeverity(list = []) {
      const severityList = list.map(item => this.defectSeverityMap[item])
      return severityList.join('、')
    },
    handleSelectable(row, index) {
      return !(row.status & 2)
    },
    handleTableSetReview() {
      let prefix = `${location.host}`
      if (window.self !== window.top) {
        prefix = `${window.DEVOPS_SITE_URL}/console`
      }
      const route = this.$router.resolve({
        name: 'ignoreList',
      })
      window.open(prefix + route.href, '_blank')
      document.body.click()
      localStorage.setItem('guide2End', true)
    },
    handleTableGuideNextStep() {
      document.body.click()
      localStorage.setItem('guide2End', true)
    },
    goToTask(taskId) {
      this.$router.push({
        name: 'task-detail',
        params: {
          taskId,
        },
      })
    },
  },
}
