<template>
  <div class="code-fullscreen full-active" v-bkloading="{ isLoading: isLoading, opacity: 0.3 }">

    <div class="col-aside-left">
      <div class="col-aside-left-header">
        <bk-icon @click="closeDetail" class="arrows-left" type="arrows-left" />
        <div class="header-index">{{fileIndex + 1}}/{{totalCount}}</div>
        <i class="codecc-icon icon-share" @click="shareVisiable = true"></i>
        <i class="codecc-icon icon-operate" @click="operateDialogVisiable = true"></i>
        <div class="share-block" v-if="shareVisiable">
          <div class="share-header">{{$t('分享此问题')}}</div>
          <i class="bk-icon icon-close" @click="shareVisiable = false"></i>
          <div class="share-content" @click="shareDefect">
            <i class="codecc-icon icon-link-2"></i>
            <span>{{$t('复制链接')}}</span>
          </div>
        </div>
      </div>
      <defect-block
        :list="list"
        :defect-index="fileIndex"
        :handle-file-list-row-click="handleFileListRowClick"
        :is-file-list-load-more="isFileListLoadMore"
        :defect-instances="isLoading ? [] : (defectInstances || [])"
        :trace-active-id="traceActiveId"
        :next-page-start-num="nextPageStartNum"
        :next-page-end-num="nextPageEndNum"
        @clickTrace="clickTrace"
        @scrollLoadMore="scrollLoadMore">
      </defect-block>
    </div>

    <div class="col-main">
      <b class="filename" :title="(currentTrace.filePath || currentFile.filePath)">{{((currentTrace.filePath || currentFile.filePath) || '').split('/').pop()}}</b>
      <div id="codeViewerInDialog" :class="isFullScreen ? 'full-code-viewer' : 'un-full-code-viewer'" @click="handleCodeViewerInDialogClick"></div>
    </div>

    <div class="col-aside">
      <div class="operate-section-defect">
        <div class="basic-info-defect" :class="{ 'full-screen-info': isFullScreen }" v-if="currentFile">
          <div class="block">
            <div class="item disb">
              <span class="fail" :class="{ 'cc-status': currentFile.mark }" v-if="currentFile.status === 1">
                <span class="cc-dot"></span>
                <span v-if="buildNum" v-bk-tooltips="`#${buildNum}待修复(当前分支最新构建#${lintDetail.lastBuildNumOfSameBranch}该问题为${lintDetail.defectIsFixedOnLastBuildNumOfSameBranch ? '已修复' : '待修复'})`">
                  #{{buildNum}}{{$t('待修复')}}
                  <span style="color: #63656e">(#{{lintDetail.lastBuildNumOfSameBranch}}{{lintDetail.defectIsFixedOnLastBuildNumOfSameBranch ? $t('已修复') : $t('待修复')}})</span>
                </span>
                <span v-else>{{$t('待修复')}}</span>
                <span v-if="currentFile.defectIssueInfoVO && currentFile.defectIssueInfoVO.submitStatus && currentFile.defectIssueInfoVO.submitStatus !== 4">{{$t('(已提单)')}}</span>
              </span>
              <span class="success" v-else-if="currentFile.status & 2"><span class="cc-dot"></span>{{$t('已修复')}}</span>
              <span class="warn" v-else-if="currentFile.status & 4"><span class="cc-dot"></span>{{$t('已忽略')}}</span>
              <span class="warn" v-else-if="currentFile.status & 8 || currentFile.status & 16"><span class="cc-dot"></span>{{$t('已屏蔽')}}</span>
              <span v-if="currentFile.status === 1 && currentFile.mark" class="cc-mark disb">
                <span v-if="currentFile.mark === 1" v-bk-tooltips="'已标记处理'">
                  <span class="codecc-icon icon-mark"></span>
                  <span>{{$t('已标记处理')}}</span>
                </span>
                <span v-if="currentFile.mark === 2" v-bk-tooltips="'标记处理后重新扫描仍为问题'">
                  <span class="codecc-icon icon-mark re-mark"></span>
                  <span>{{$t('已标记处理')}}</span>
                </span>
              </span>
            </div>
            <div v-if="currentFile.status === 1" class="item">
              <bk-button v-if="currentFile.mark" class="item-button" @click="handleMark(0, false, entityId)">
                {{$t('取消标记')}}
              </bk-button>
              <bk-button v-else theme="primary" class="item-button" @click="handleMark(1, false, entityId)">
                {{$t('标记处理')}}
              </bk-button>
            </div>
            <div class="item">
              <bk-button class="item-button" @click="handleComent(entityId)">{{$t('评论')}}</bk-button>
            </div>
            <div class="item">
              <bk-button v-if="currentFile.status & 4 && currentFile.ignoreCommentDefect" class="item-button" disabled :title="$t('注释忽略的问题不允许页面进行恢复操作')">
                {{$t('恢复忽略')}}
              </bk-button>
              <bk-button v-else-if="currentFile.status & 4" class="item-button" @click="handleIgnore('RevertIgnore', false, entityId)">
                {{$t('恢复忽略')}}
              </bk-button>
              <bk-button v-else-if="prohibitIgnore" disabled class="item-button" :title="$t('已设置禁止页面忽略，可在代码行末或上一行使用注释忽略，例如// NOCC:rule1(ignore reason)')">
                {{$t('忽略问题')}}
              </bk-button>
              <bk-button v-else-if="!(currentFile.status & 2 || currentFile.status & 8 || currentFile.status & 16)" class="item-button" @click="handleIgnore('IgnoreDefect', false, entityId)">
                {{$t('忽略问题')}}
              </bk-button>
            </div>
            <div v-if="currentFile.status === 1 && !(currentFile.defectIssueInfoVO && currentFile.defectIssueInfoVO.submitStatus && currentFile.defectIssueInfoVO.submitStatus !== 4)" class="item">
              <bk-button
                class="item-button"
                @click="handleCommit('commit', false, entityId)">{{$t('提单')}}
              </bk-button>
            </div>
          </div>
          <div class="block">
            <div class="item">
              <dt>{{$t('ID')}}</dt>
              <dd>{{currentFile.id}}</dd>
            </div>
            <div class="item">
              <dt>{{$t('级别')}}</dt>
              <dd>{{defectSeverityMap[currentFile.severity]}}</dd>
            </div>
          </div>
          <div class="block">
            <div class="item">
              <dt>{{$t('创建时间')}}</dt>
              <dd class="small">{{currentFile.createTime | formatDate}}</dd>
            </div>
            <div class="item" v-if="currentFile.status & 2">
              <dt>{{$t('修复时间')}}</dt>
              <dd class="small">{{currentFile.fixedTime | formatDate}}</dd>
            </div>
            <div class="item">
              <dt>{{$t('首次发现')}}</dt>
              <dd>{{currentFile.createBuildNumber ? '#' + currentFile.createBuildNumber : '--'}}</dd>
            </div>
            <div class="item">
              <dt>{{$t('提交时间')}}</dt>
              <dd class="small">{{currentFile.lineUpdateTime | formatDate}}</dd>
            </div>
          </div>
          <div class="block" v-if="currentFile.status & 4">
            <div class="item">
              <dt>{{$t('忽略时间')}}</dt>
              <dd class="small">{{currentFile.ignoreTime | formatDate}}</dd>
            </div>
            <div class="item">
              <dt>{{$t('忽略人')}}</dt>
              <dd>{{currentFile.ignoreAuthor}}</dd>
            </div>
            <div class="item disb">
              <dt>{{$t('忽略原因')}}</dt>
              <dd>{{getIgnoreReasonByType(currentFile.ignoreReasonType)}}
                {{currentFile.ignoreReason ? '：' + currentFile.ignoreReason : ''}}
              </dd>
            </div>
          </div>
          <div class="block">
            <div class="item">
              <dt v-if="currentFile.status === 1" class="curpt" @click.stop="handleAuthor(1, entityId, currentFile.author)">
                {{$t('处理人')}}
                <span class="bk-icon icon-edit2 fs20"></span>
              </dt>
              <dt v-else>
                {{$t('处理人')}}
              </dt>
              <dd>{{currentFile.author && currentFile.author.join(',')}}</dd>
            </div>
          </div>
          <div class="block">
            <div class="item disb">
              <dt>{{$t('工具')}}</dt>
              <dd>{{(toolMap[currentFile.toolName] || {}).displayName || ''}}</dd>
            </div>
            <div class="item disb">
              <dt>{{$t('规则')}}</dt>
              <dd>{{currentFile.checker}}</dd>
            </div>
          </div>
          <div class="block">
            <div class="item disb">
              <dt>{{$t('代码库路径')}}</dt>
              <a target="_blank" :href="lintDetail.filePath">{{lintDetail.filePath}}</a>
            </div>
            <div class="item disb">
              <dt>{{$t('版本号')}}</dt>
              <dd>{{currentFile.revision}}</dd>
            </div>
          </div>
        </div>
      </div>
    </div>

    <operate-dialog :visiable.sync="operateDialogVisiable"></operate-dialog>
  </div>
</template>

<script>
  import CodeMirror from '@/common/codemirror'
  import { getClosest, addClass, formatDiff, hasClass } from '@/common/util'
  import { mapState } from 'vuex'
  import { format } from 'date-fns'
  import { bus } from '@/common/bus'
  import DefectBlock from './defect-block/defect-block'
  import OperateDialog from '@/components/operate-dialog'

  export default {
    components: {
      DefectBlock,
      OperateDialog,
    },
    props: {
      list: Array,
      type: {
        type: String,
        default: 'file',
      },
      isLoading: {
        type: Boolean,
        default: false,
      },
      visiable: {
        type: Boolean,
        default: false,
      },
      isFullScreen: {
        type: Boolean,
        default: false,
      },
      currentFile: {
        type: Object,
        default: {},
      },
      fileIndex: {
        type: Number,
        default: 0,
      },
      entityId: {
        type: String,
      },
      totalCount: {
        type: Number,
        default: 0,
      },
      lintDetail: {
        type: Object,
        default: {},
      },
      handleMark: {
        type: Function,
      },
      handleComent: {
        type: Function,
      },
      handleCommit: {
        type: Function,
      },
      deleteComment: {
        type: Function,
      },
      handleIgnore: {
        type: Function,
      },
      handleAuthor: {
        type: Function,
      },
      prohibitIgnore: {
        type: Boolean,
      },
      buildNum: {
        type: String,
      },
      handleFileListRowClick: Function,
      isFileListLoadMore: Boolean,
      nextPageStartNum: Number,
      nextPageEndNum: Number,
    },
    data() {
      return {
        toolId: this.$route.params.toolId,
        defectSeverityMap: {
          1: this.$t('严重'),
          2: this.$t('一般'),
          3: this.$t('提示'),
          4: this.$t('提示'),
        },
        defectSeverityDetailMap: {
          1: this.$t('严重'),
          2: this.$t('一般'),
          3: this.$t('提示'),
          4: this.$t('提示'),
        },
        currentDefectDetail: {
          hintId: undefined,
          eventTimes: 0,
          eventSource: undefined,
        },
        codeViewerInDialog: null,
        codeMirrorDefaultCfg: {
          lineNumbers: true,
          scrollbarStyle: 'simple',
          theme: 'summerfruit',
          lineWrapping: true,
          placeholder: this.emptyText,
          firstLineNumber: 1,
          readOnly: true,
        },
        commentList: [],
        scrollLine: 0,
        rowIndex: 0,
        shareVisiable: false,
        operateDialogVisiable: false,
        traceDataList: [],
        currentTrace: {},
        currentTraceIndex: 0,
        traceActiveId: '',
        mainTraceId: '',
        startLine: 1,
      }
    },
    computed: {
      ...mapState('tool', {
        toolMap: 'mapList',
      }),
      defectInstances() {
        const { defectInstances = [] } = this.currentFile
        const traceDataList = []
        const handleLinkTrace = function (linkTrace = [], id, index) {
          linkTrace.forEach((link, linkIndex) => {
            const linkId = `${id}-${linkIndex}`
            const newIndex = `${index}.${link.traceNum}`
            traceDataList.push(link)
            link.id = linkId
            link.index = newIndex
            handleLinkTrace(link.linkTrace, linkId, newIndex)
          })
        }
        defectInstances.forEach((instance, instanceIndex) => {
          instance.traces.forEach((trace, traceIndex) => {
            const id = `${instanceIndex}-${traceIndex}`
            traceDataList.push(trace)
            trace.id = id
            trace.index = trace.traceNum
            trace.expanded = !!trace.main
            trace.linkTrace && handleLinkTrace(trace.linkTrace, id, trace.traceNum)
          })
        })
        this.traceDataList = traceDataList
        return defectInstances
      },
    },
    watch: {
      currentDefectDetail: {
        handler() {
          this.emptyText = this.$t('未选择文件')
          // 把main trace设为当前trace
          const tracesList = (this.defectInstances[0] && this.defectInstances[0].traces) || [{}]
          this.currentTrace = tracesList.find(item => item.main) || tracesList[tracesList.length - 1]
          this.traceActiveId = this.currentTrace.id
          this.mainTraceId = this.currentTrace.id
          this.currentTraceIndex = this.traceDataList.findIndex(item => item.id === this.currentTrace.id)
          // this.locateHint()
        },
        deep: true,
      },
      visiable(val) {
        if (!val) {
          this.codeViewerInDialog.setValue('')
          this.codeViewerInDialog.setOption('firstLineNumber', 1)
        }
      },
      currentFile() {
        this.currentTrace = {}
      },
    },
    methods: {
      setFullScreen() {
        this.$emit('update:is-full-screen', !this.isFullScreen)
      },
      handleCodeFullScreen(hideDefectDetail) {
        if (!this.codeViewerInDialog) {
          const codeMirrorConfig = {
            ...this.codeMirrorDefaultCfg,
            ...{ autoRefresh: true },
          }
          this.codeViewerInDialog = CodeMirror(document.getElementById('codeViewerInDialog'), codeMirrorConfig)

          this.codeViewerInDialog.on('update', () => {})
          if (!window.localStorage.getItem('opreate-keyboard-20220411')) {
            this.operateDialogVisiable = true
          }
        }

        this.updateCodeViewer(hideDefectDetail)
      },
      // 代码展示相关
      updateCodeViewer(hideDefectDetail) {
        const codeViewer = this.codeViewerInDialog
        const { fileName, trimBeginLine } = this.lintDetail
        const { fileInfoMap } = this.currentFile
        let fileMd5 = this.currentTrace.fileMd5 || this.currentFile.fileMd5
        if (!fileMd5) {
          const tracesList = (this.defectInstances[0] && this.defectInstances[0].traces) || [{}]
          this.currentTrace = tracesList.find(item => item.main) || tracesList[tracesList.length - 1]
          this.traceActiveId = this.currentTrace.id
          this.mainTraceId = this.currentTrace.id
          this.currentTraceIndex = this.traceDataList.findIndex(item => item.id === this.currentTrace.id)
          // eslint-disable-next-line prefer-destructuring
          fileMd5 = this.currentTrace.fileMd5
        }
        const { contents, startLine } = fileInfoMap[fileMd5]
        this.currentFile.startLine = startLine
        const codeMirrorMode = CodeMirror.findModeByFileName(fileName)
        const mode = codeMirrorMode && codeMirrorMode.mode
        import(`codemirror/mode/${mode}/${mode}.js`).then((m) => {
            codeViewer.setOption('mode', mode)
        }).finally(() => {
            codeViewer.setOption('firstLineNumber', startLine !== 0 ? startLine : 1)
            if (!contents) {
                this.emptyText = this.$t('文件内容为空')
                codeViewer.setValue(this.emptyText)
            } else {
                codeViewer.setValue(contents)
            }
            // 创建问题提示块
            this.buildLintHints(codeViewer, hideDefectDetail)
            codeViewer.refresh()
            this.locateHint()
        })
      },
      // 创建问题提示块
      buildLintHints(codeViewer, hideDefectDetail) {
        /**
         * 处理markdown链接
         * @param {*} str 待处理字符串
         */
        function handleUrl(str = '') {
          let newStr = str.replace(/>/g, '&gt;').replace(/</g, '&lt;')
          const reg = /\[([^\]]+)\]\(([http:\\|https:\\]{1}[^)]+)\)/g
          newStr = newStr.replace(reg, '<a target="_blank" href=\'$2\'>$1</a>')
          return newStr
        }

        const { defectSeverityDetailMap, currentFile: { startLine } } = this
        const { lintDefectDetailVO: detailVO } = this.lintDetail
        let defectList = null
        // 有defectInstances的情况
        if (this.defectInstances.length) {
          const { id } = this.currentTrace
          const idList = id.split('-')
          defectList = this.defectInstances[idList[0]].traces
          if (idList.length > 2) {
            for (let i = 1; i < idList.length - 1; i++) {
              defectList = defectList[idList[i]].linkTrace
            }
          }
        } else {
          defectList = [detailVO]
        }
        defectList.forEach((defect) => {
          if (this.currentTrace.fileMd5 && this.currentTrace.fileMd5 !== defect.fileMd5) return
          let checkerComment = ''
          const hints = document.createElement('div')
          const { index, lineNum, message } = defect
          const messageDom = document.createElement('span')
          messageDom.innerHTML = `${index ? `${index}.` : ''}${handleUrl(message)}`
          const hintId = `${lineNum}-${0}`
          let mainClass = ''
          if (this.mainTraceId === defect.id || !this.defectInstances.length) {
            mainClass = 'main'
            // 评论
            if (detailVO.codeComment) {
              for (let i = 0; i < detailVO.codeComment.commentList.length; i++) {
                checkerComment
                  += `<p class="comment-item">
                        <span class="info">
                            <i class="codecc-icon icon-commenter"></i>
                            <span>${detailVO.codeComment.commentList[i].userName}</span>
                            <span title="${detailVO.codeComment.commentList[i].comment}">
                            ${detailVO.codeComment.commentList[i].comment}
                            </span>
                        </span>
                        <span class="handle">
                            <span>${this.formatTime(detailVO.codeComment.commentList[i].commentTime)}</span>
                            <i class="bk-icon icon-delete"
                                data-singlecommentid="comment-${detailVO.codeComment.commentList[i].singleCommentId}"
                                data-commentid="comment-${detailVO.codeComment.entityId}"
                                data-comment="${detailVO.codeComment.commentList[i].comment}"
                                data-entityid="${detailVO.entityId}"
                            ></i>
                        </span>
                    </p>`
              }
            }
            const { checkerDetail } = detailVO
            const checkerDetailArr = checkerDetail.split('\n')
            const newcheckerDetailArr = checkerDetailArr.map((item) => {
              let detail = item.replace(/>/g, '&gt;').replace(/</g, '&lt;')
              const reg = /\[([^\]]+)\]\(([http:\\|https:\\]{1}[^)]+)\)/g
              detail = detail.replace(reg, '<a target="_blank" href=\'$2\'>$1</a>')
              return `<div>${detail}</div>`
            })
            hints.innerHTML = `
            <div class="lint-info">
                <div class="lint-info-main">
                    <i class="lint-icon bk-icon icon-right-shape"></i>
                    <div class="lint-head">
                        ${messageDom.outerHTML}
                    </div>
                    <p class="tag-line">
                        <span class="tag">
                            <span class="bk-icon icon-exclamation-circle-shape type-${detailVO.severity}"></span>
                            ${detailVO.checker} ${detailVO.checkerType ? `| ${detailVO.checkerType}` : ''}
                            | ${defectSeverityDetailMap[detailVO.severity]}
                        </span>
                        <span class="tag">
                            <span class="codecc-icon icon-creator"></span>
                            ${detailVO.author || '--'}
                            ${this.type === 'defect' ? '' : `<span class="bk-icon icon-edit2 fs20"
                            data-entityid="${detailVO.entityId}" data-author="${detailVO.author || '--'}"></span>`}
                        </span>
                        <span class="tag">
                            <span class="codecc-icon icon-time"></span>
                            ${this.formatDate(detailVO.createTime)}
                            ${detailVO.createBuildNumber ? `#${detailVO.createBuildNumber}` : '--'}创建
                        </span>
                    </p>
                </div>
                <div class="checker-detail">${newcheckerDetailArr.join('')}</div>
                ${checkerComment ? `<div class="checker-comment">${checkerComment}</div>` : ''}
            </div>`
          } else {
            hints.innerHTML = `
            <div class="lint-info">
                <div class="lint-info-main">
                    <div class="lint-head">
                        ${messageDom.outerHTML}
                    </div>
                </div>
            </div>`
          }

          let activeClass = ''
          if ((!this.defectInstances.length || this.currentTrace.id === defect.id) && !hideDefectDetail) {
            activeClass = 'active'
          }
          hints.className = `lint-hints lint-hints-${hintId} ${activeClass}`
          hints.dataset.hintId = hintId

          // const startLine = trimBeginLine === 0 ? 1 : trimBeginLine
          codeViewer.addLineWidget(lineNum - startLine, hints, {
            coverGutter: false,
            noHScroll: false,
            above: true,
          })
          codeViewer.addLineClass(lineNum - startLine, 'wrap', `lint-hints-wrap ${mainClass} ${activeClass}`)
        })
        this.scrollIntoView()
        bus.$emit('hide-app-loading')
        // codeViewer.refresh()
      },
      handleCodeViewerInDialogClick(event, eventSource) {
        this.codeViewerClick(event, 'dialog-code')
      },
      codeViewerClick(event, eventSource) {
        const lintHints = getClosest(event.target, '.lint-hints')
        const lintInfo = getClosest(event.target, '.icon-right-shape')
        const headHanle = getClosest(event.target, '.btn')
        const editAuthor = getClosest(event.target, '.icon-edit2')
        // const commentCon = getClosest(event.target, '.checker-comment')
        const delHandle = getClosest(event.target, '.icon-delete')
        const checkerDetail = getClosest(event.target, '.checker-detail')

        if (lintHints) {
          const { hintId } = lintHints.dataset
          this.scrollLine = Number(hintId.split('-')[0])
          this.rowIndex = Number(hintId.split('-')[1])
        }

        // 如果点击的是标记/忽略/评论按钮
        if (headHanle) {
          const type = headHanle.getAttribute('data-option')
          const entityId = headHanle.dataset.entityid
          if (type === 'comment') {
            const commentId = headHanle.getAttribute('data-commentId')
            this.handleComent(entityId, commentId)
          } else if (type === 'mark') {
            let { mark } = headHanle.dataset
            mark = mark === '0' || mark === 'undefined' ? 1 : 0
            this.handleMark(mark, false, entityId)
          } else if (type === 'ignore' && !this.prohibitIgnore) {
            this.handleIgnore('IgnoreDefect', false, entityId)
          }
          return
        }
        if (editAuthor) {
          const { author } = editAuthor.dataset
          const entityId = editAuthor.dataset.entityid
          this.handleAuthor(1, entityId, author)
          return
        }
        // 如果点击的是删除评论
        if (delHandle) {
          const that = this
          this.$bkInfo({
            title: '删除评论',
            subTitle: '确定要删除该条评论吗？',
            maskClose: true,
            confirmFn() {
              const delSingleObj = delHandle.getAttribute('data-singlecommentid')
              const delCommentObj = delHandle.getAttribute('data-commentid')
              const commentStr = delHandle.getAttribute('data-comment')
              const defectEntityId = delHandle.getAttribute('data-entityid')
              const singleCommentId = delSingleObj.split('-').pop()
              const commentId = delCommentObj.split('-').pop()
              that.deleteComment(commentId, singleCommentId, defectEntityId, commentStr)
            },
          })
          return
        }
        // 如果点击的是规则详情，不执行操作
        if (checkerDetail) {
          return
        }
        // 如果点击的是lint问题区域
        if (lintInfo) {
          // 触发watch
          this.currentDefectDetail.hintId = lintHints.dataset.hintId
          this.currentDefectDetail.eventSource = eventSource
          this.currentDefectDetail.eventTimes += 1
          const hideDefectDetail = hasClass(lintHints, 'active')
          this.handleCodeFullScreen(hideDefectDetail)
        }
      },
      handleDefectListRowInDialogClick(row, event, column) {
        // if (!this.lintDetail.fileContent) return

        // 代码所在行
        const lineNum = row.lineNum - 1

        // 得到表格行索引
        const rowIndex = event ? getClosest(event.target, 'tr').rowIndex : 0
        this.rowIndex = rowIndex

        // 记录当前问题id
        const hintId = `${lineNum}-${rowIndex}`

        // 触发watch
        this.currentDefectDetail.hintId = hintId
        this.currentDefectDetail.eventSource = 'dialog-row'
        this.currentDefectDetail.eventTimes += 1
        this.locateHint()
        this.handleCodeFullScreen()
      },
      locateHint() {
        const eventFrom = this.currentDefectDetail.eventSource?.split('-').shift()
        // 默认处理页面中的代码展示
        this.locateHintByName(eventFrom)
      },
      locateHintByName(name, visiableToggle) {
        const { hintId, eventSource } = this.currentDefectDetail

        // 确实存在未点击问题直接打开全屏的情况，这种情况没有hintId
        if (!hintId) {
          return
        }

        const [lineNum, rowIndex] = hintId.split('-')
        const eventTrigger = eventSource.split('-').pop()
        const codeViewer = name === 'main' ? this.codeViewer : this.codeViewerInDialog
        const lintWrapper = codeViewer.getWrapperElement()

        if (eventTrigger === 'row' || eventTrigger === 'code' || visiableToggle === true) {
          // 滚动到问题代码位置
          setTimeout(this.scrollTrace, 10)
        }

        // 问题代码区域高亮
        const lintHints = lintWrapper.getElementsByClassName(`lint-hints-${hintId}`)
        // this.activeLintHint(lintHints[0])
      },
      activeLintHint(lintHint) {
        if (!lintHint) return
        // 切换所有lint wrap的active
        const lintHintsWrap = getClosest(lintHint, '.lint-hints-wrap')
        const isActive = lintHint.classList.contains('active')
        document.querySelectorAll('.active').forEach(el => el.classList.remove('active'))
        if (!isActive) {
          addClass(lintHint, 'active')
          addClass(lintHintsWrap, 'active')
        }
      },
      locateFirst() {
        this.$nextTick(() => {
          this.handleDefectListRowInDialogClick(this.lintDetail.lintDefectDetailVO)
        })
      },
      getIgnoreReasonByType(type) {
        const typeMap = {
          1: this.$t('检查工具误报'),
          2: this.$t('设计如此'),
          4: this.$t('其他'),
        }
        return typeMap[type]
      },
      formatTime(time) {
        return formatDiff(time)
      },
      formatDate(date) {
        return date ? format(date, 'YYYY-MM-DD') : '--'
      },
      scrollLoadMore() {
        this.$emit('scrollLoadMore')
      },
      closeDetail() {
        this.$emit('closeDetail')
      },
      clickTrace(trace) {
        const { fileMd5, lineNum, id } = this.currentTrace || {}
        this.currentTraceIndex = this.traceDataList.findIndex(item => item.id === trace.id)
        this.currentTrace = trace
        this.traceActiveId = trace.id
        this.defectInstances.forEach((instance) => {
          instance.traces.forEach((item) => {
            if (item.id === trace.id.split('-').slice(0, 2)
              .join('-')) {
              item.expanded = true
            }
          })
        })
        if (fileMd5 !== trace.fileMd5
          || id.length !== trace.id.length
          || id.split('-').shift() !== trace.id.split('-').shift()) {
          this.updateCodeViewer()
          this.preLineNum = undefined
        } else {
          this.preLineNum = lineNum
        }
        this.scrollTrace()
      },
      // 问题上下文
      scrollTrace() {
        const { lineNum } = this.defectInstances.length ? this.currentTrace : this.currentFile
        const { startLine } = this.currentFile
        const codeViewer = this.codeViewerInDialog
        if (!codeViewer || !lineNum) return false
        if (this.preLineNum) {
          codeViewer.removeLineClass(this.preLineNum - startLine, 'wrap', 'defect-trace')
        }
        codeViewer.addLineClass(lineNum - startLine, 'wrap', 'defect-trace')
        this.scrollIntoView(lineNum)
      },
      // 默认滚动到问题位置
      scrollIntoView(number) {
        const codeViewer = this.codeViewerInDialog
        if (!codeViewer || !codeViewer.getScrollerElement()) return false
        const { startLine = 0 } = this.currentFile
        const middleHeight = codeViewer.getScrollerElement().offsetHeight / 2
        const lineHeight = codeViewer.defaultTextHeight()
        const { lineNum = number } = this.defectInstances.length ? this.currentTrace : this.currentFile
        if (!number) {
          codeViewer.removeLineClass(lineNum - startLine, 'wrap', 'defect-trace')
        }
        setTimeout(() => {
          codeViewer.scrollIntoView({ line: lineNum - startLine, ch: 0 }, middleHeight - lineHeight)
          // bus.$emit('hide-app-loading')
          this.detailLoading = false
        }, 1)
      },
      traceUp() {
        if (this.currentTraceIndex > 0) {
          this.currentTraceIndex -= 1
          const trace = this.traceDataList[this.currentTraceIndex]
          this.clickTrace(trace)
        }
      },
      traceDown() {
        if (this.currentTraceIndex < this.traceDataList.length - 1) {
          this.currentTraceIndex += 1
          const trace = this.traceDataList[this.currentTraceIndex]
          this.clickTrace(trace)
        }
      },
      /**
       * 分享问题链接
       */
      shareDefect() {
        const { projectId, taskId } = this.$route.params
        const { toolName, entityId, status } = this.currentFile
        let prefix = `${location.protocol}//${location.host}`
        if (window.self !== window.top) {
          prefix = `${location.protocol}${window.DEVOPS_SITE_URL}/console`
        }
        const url = `${prefix}/codecc/${projectId}/task/${taskId}/defect/lint/${toolName}/list
?entityId=${entityId}&status=${status}`
        const input = document.createElement('input')
        document.body.appendChild(input)
        input.setAttribute('value', url)
        input.select()
        document.execCommand('copy')
        document.body.removeChild(input)
        this.$bkMessage({ theme: 'success', message: '链接已复制到粘贴板' })
      },
    },
  }
</script>

<style lang="postcss" scoped>
    @import "../../css/mixins.css";

    .code-fullscreen {
      display: flex;
      .toggle-full-icon {
        position: absolute;
        top: -22px;
        right: 14px;
        color: #979ba5;
        cursor: pointer;
        &.icon-un-full-screen {
          top: 8px;
        }
      }
      .col-main {
        flex: 1;
        max-width: calc(100% - 585px);
        margin-top: 47px;
      }
      .col-aside {
        flex: none;
        width: 240px;
        background: #f5f7fa;
        padding: 12px 20px;
        margin-top: 47px;
        .operate-section {
          height: calc(100vh - 270px);
          &.full-operate-section {
            height: calc(100vh - 170px);
          }
        }
      }
      .col-aside-left {
        flex: none;
        width: 336px;
        background: #f5f7fa;
        .col-aside-left-header {
          height: 48px;
          border-bottom: 1px solid #dcdee5;
          .arrows-left {
            position: absolute;
            font-size: 36px!important;
            line-height: 48px;
            cursor: pointer;
            z-index: 9999;
          }
          .header-index {
            display: inline-block;
            text-align: center;
            width: 100%;
            line-height: 48px;
            font-weight: bold;
          }
        }
      }
      .filename {
        position: absolute;
        top: 0;
        padding-left: 24px;
        font-size: 16px;
        line-height: 48px;
      }
      .file-bar {
        height: 42px;
        .filemeta {
          display: inline-block;
          margin-top: -2px;
          font-size: 12px;
          border-left: 4px solid #3a84ff;
          padding-left: 8px;
          .filename {
            font-size: 16px;
          }
          .filepath {
            width: 700px;
            display: inline-block;
            vertical-align: bottom;
            margin-left: 8px;
            line-height: 24px;

            @mixin ellipsis;
          }
        }
      }
      .operate-section,
      .basic-info {
        .title {
          font-size: 14px;
          color: #313238;
        }
      }
      .basic-info {
        .item {
          display: flex;
          padding-bottom: 9px;
          dt {
            width: 90px;
            flex: none;
          }
          dd {
            flex: 1;
            color: #313238;
          }
        }
      }
      .operate-section-defect {
        height: 100%;
      }
      .basic-info-defect {
        /* height: calc(100% - 60px); */
        max-height: calc(100vh - 240px);
        overflow-y: scroll;
        margin-right: -29px;
        padding-right: 20px;
        &.full-screen-info {
          max-height: calc(100vh - 71px);
        }
        .title {
          font-size: 14px;
          color: #313238;
        }
        .block {
          padding: 5px 0;
          border-bottom: 1px dashed #c4c6cc;
          &:last-of-type {
            border-bottom: 0;
            padding-bottom: 20px;
          }
          .item {
            display: flex;
            padding: 5px 0;
            dt {
              width: 90px;
              flex: none;
            }
            dd {
              /* flex: 1; */
              color: #313238;
              word-break: break-all;
              &.small {
                width: 80px;
              }
            }
            a {
              color: #313238;
              word-break: break-all;
            }
            .item-button {
              width: 200px;
            }
          }
        }
      }
    }
    #codeViewerInDialog {
      font-size: 14px;
      width: 100%;
      border: 1px solid #eee;
      border-left: 0;
      border-right: 0;
    }
    .un-full-code-viewer {
      height: calc(100vh - 200px);
    }
    .full-code-viewer {
      height: calc(100vh - 47px);
    }
    >>>.icon-mark {
      color: #53cad1;
      &.re-mark {
        color: #facc48;
      }
      &.un-mark {
        color: #b0b0b0;
      }
    }
    >>>.icon-marked {
      color: #53cad1;
      font-size: 18px;
      &.re-marked {
        color: #facc48;
      }
    }
    .cc-status {
      width: 60px;
      padding-right: 8px;
    }
    .cc-mark {
      width: 114px;
      background: white;
      border-radius: 12px;
      padding: 0 8px;
      line-height: 23px;
      height: 23px;
      margin-top: 8px;
    }
    >>>.bk-table {
      .mark-row {
        .cell {
          padding-left: 15px!important;
        }
      }
    }
    .icon-share, .icon-operate {
      position: absolute;
      top: 0;
      right: 24px;
      font-size: 16px;
      cursor: pointer;
      line-height: 48px;
      &:hover {
        color: #3a84ff;
      }
    }
    .icon-operate {
      right: 60px;
    }
    .share-block {
      position: absolute;
      right: 24px;
      width: 272px;
      height: 150px;
      background: #fff;
      box-shadow: 0 4px 12px 0 rgba(0,0,0,0.20);
      border-radius: 2px;
      z-index: 99;
      .share-header {
        font-size: 20px;
        color: #313238;
        line-height: 28px;
        padding: 18px 0 10px 24px;
      }
      .icon-close {
        position: absolute;
        right: 10px;
        top: 10px;
        font-size: 28px;
        cursor: pointer;
      }
      .share-content {
        width: 200px;
        height: 32px;
        background: #fff;
        box-shadow: 0 2px 6px 0 rgba(0,0,0,0.10);
        border-radius: 2px;
        line-height: 32px;
        margin: 16px 36px 0;
        padding-left: 10px;
        cursor: pointer;
        &:hover {
          color: #3a84ff;
        }
      }
    }
</style>
