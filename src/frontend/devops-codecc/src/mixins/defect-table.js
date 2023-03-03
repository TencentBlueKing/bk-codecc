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
  methods: {
    // 处理状态
    handleStatus(status, defectIssueInfoVO = {}) {
      let key = 1
      if (status === 1) {
        key = 1
      } else if (status & 2) {
        key = 2
      } else if (status & 4) {
        key = 4
      } else if (status & 8 || status & 16) { // 8是路径屏蔽，16是规则屏蔽
        key = 8
      }
      const statusMap = {
        1: this.$t('待修复'),
        2: this.$t('已修复'),
        4: this.$t('已忽略'),
        8: this.$t('已屏蔽'),
      }
      /**
       * submitStatus字段：
        1 - 排队中
        2 - 准备开始提单
        3 - 提单成功
        4 - 提单失败
       */
      let issueStatus = ''
      if (defectIssueInfoVO.submitStatus && defectIssueInfoVO.submitStatus !== 4) {
        issueStatus = this.$t('(已提单)')
      }
      return `${statusMap[key]}${issueStatus}`
    },
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
  },
}
