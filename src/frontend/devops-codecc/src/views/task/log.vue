<template>
  <div v-bkloading="{ isLoading: loading, opacity: 0.1 }">
    <bk-table
      v-show="!loading"
      row-key="buildId"
      size="small"
      :data="logList"
      :expand-row-keys="expendRowKey"
      :pagination="pagination"
      @page-change="handlePageChange"
      @page-limit-change="handlePageLimitChange">
      <bk-table-column type="expand" width="30">
        <template slot-scope="props">
          <div class="log-expand">
            <p class="log-txt">{{$t('触发人')}}：{{props.row.buildUser}}</p>
            <p class="log-txt">
              <span class="mr5">{{$t('版本号')}}：{{props.row.version}}</span>
              <bk-popover placement="right-start" theme="light">
                <i class="bk-icon icon-info-circle-shape"></i>
                <div slot="content">
                  <div class="code-content" v-for="(repoInfo, infoIndex) in props.row.repoInfoStrList" :key="infoIndex">
                    <p v-for="(item, index) in repoInfo.split(/[,，\n]+/)" :key="index">{{item}}</p>
                  </div>
                </div>
              </bk-popover>
            </p>
            <bk-container
              class="tool-container"
              :class="{ 'small-container': props.row.taskLogVOList && props.row.taskLogVOList.length < 4 }"
              :col="4"
              :gutter="0"
              :margin="0">
              <bk-row>
                <bk-col class="tool-col" v-for="item in props.row.taskLogVOList" :key="item.toolName">
                  <bk-popover theme="light">
                    <p class="col-name">{{(toolMap[item.toolName] || {}).displayName || item.toolName}}</p>
                    <template slot="content" v-if="item.stepArray && item.stepArray.length">
                      <bk-table
                        class="tool-col-table"
                        :data="reverseStepArray(item.stepArray)"
                        :outer-border="true"
                        :header-cell-style="{ borderRight: 'none' }">
                        <bk-table-column width="125" :label="$t('步骤')">
                          <template slot-scope="scope">
                            <i class="step" :class="{ 'status': scope.row.index === 1, 'status-success': scope.row.flag === 1, 'status-fail': scope.row.flag === 2 || scope.row.flag === 4 }"></i>
                            <span>{{ $t(getToolStatus(scope.row.stepNum, item.toolName)) }}</span>
                          </template>
                        </bk-table-column>
                        <bk-table-column width="70" :label="$t('状态')">
                          <template slot-scope="scope">
                            <span :class="{ 'status': scope.row.index === 1, 'status-success': scope.row.flag === 1, 'status-fail': scope.row.flag === 2 || scope.row.flag === 4 }">
                              {{ $t(getLogFlag(scope.row.flag)) }}
                            </span>
                          </template>
                        </bk-table-column>
                        <bk-table-column min-width="130" :label="$t('信息')">
                          <template slot-scope="scope">
                            <bk-popover theme="light" class="msg-popover" v-if="scope.row.msg" placement="left">
                              <span v-if="formatMsg(scope.row.msg)" style="color: #ff9c01">
                                <tempalte v-if="item.toolName === 'CCN'">
                                  {{$t('低于阈值文件x个', { X: formatMsg(scope.row.msg)['defectCount'] })}}，
                                </tempalte>
                                <tempalte v-else>
                                  {{$t('x个大文件y个问题', { X: formatMsg(scope.row.msg)['fileCount'], Y: formatMsg(scope.row.msg)['defectCount'] })}}，
                                </tempalte>
                                <a download target="_blank" :href="getDownloadUrl(formatMsg(scope.row.msg))">{{$t('点击下载文件')}}</a>
                              </span>
                              <span v-else>{{ scope.row.msg }}</span>
                              <div slot="content">
                                <span v-if="formatMsg(scope.row.msg)">
                                  <p>
                                    <tempalte v-if="item.toolName === 'CCN'">
                                      {{$t('低于阈值文件x个', { X: formatMsg(scope.row.msg)['defectCount'] })}}，
                                    </tempalte>
                                    <tempalte v-else>
                                      {{$t('x个大文件y个问题', { X: formatMsg(scope.row.msg)['fileCount'], Y: formatMsg(scope.row.msg)['defectCount'] })}}，
                                    </tempalte>
                                    <a download target="_blank" :href="getDownloadUrl(formatMsg(scope.row.msg))">{{$t('点击下载文件')}}</a>
                                  </p>
                                  <p v-if="item.toolName !== 'CCN'">{{$t('其中：')}}</p>
                                  <p v-for="(gatherFile, index) in formatMsg(scope.row.msg)['gatherFileList']" :key="index">
                                    {{gatherFile.relPath}}{{$t('共x个问题', { num: gatherFile.total })}}
                                  </p>
                                </span>
                                <p v-else class="msg-content" v-for="(msgItem, msgIndex) in scope.row.msg.split(/[,，\n]+/)" :key="msgIndex">{{ msgItem }}</p>
                              </div>
                            </bk-popover>
                          </template>
                        </bk-table-column>
                        <bk-table-column width="80" :label="$t('耗时')" prop="elapseTime">
                          <template slot-scope="scope">
                            <span>{{formatSeconds(scope.row.elapseTime)}}</span>
                          </template>
                        </bk-table-column>
                        <bk-table-column width="150" :label="$t('开始时间')" prop="startTime">
                          <template slot-scope="scope">
                            <span>{{scope.row.startTime | formatDate}}</span>
                          </template>
                        </bk-table-column>
                        <bk-table-column width="150" :label="$t('结束时间')" prop="endTime">
                          <template slot-scope="scope">
                            <span>{{scope.row.endTime | formatDate}}</span>
                          </template>
                        </bk-table-column>
                        <div slot="empty">
                          <div class="codecc-table-empty-text">
                            <img src="../../images/empty.png" class="empty-img">
                            <div>{{$t('暂无数据')}}</div>
                          </div>
                        </div>
                      </bk-table>
                    </template>
                  </bk-popover>
                  <p class="col-status"
                     :class="{
                       'success': item.flag === 1,
                       'fail': item.flag === 2 || item.flag === 4,
                       'going': item.flag === 3
                     }">
                    <span class="tool-col-status cc-ellipsis"
                          v-if="item.flag === 1 && getToolStatus(item.currStep, item.toolName) === '成功'"
                          :title="getAnalyseMsg(item.stepArray) + getToolStatus(item.currStep, item.toolName)">
                      <i class="bk-icon card-tool-status icon-check-circle-shape"></i>
                      {{getAnalyseMsg(item.stepArray)}}{{getToolStatus(item.currStep, item.toolName)}}
                    </span>
                    <span class="tool-col-status cc-ellipsis" v-else-if="item.flag === 2 || item.flag === 4"
                          :title="getToolStatus(item.currStep, item.toolName) + '-' + item.stepArray[item.stepArray.length - 1].msg">
                      <i class="bk-icon card-tool-status icon-close-circle-shape"></i>
                      {{$t(getToolStatus(item.currStep, item.toolName))}}-{{ $t(item.stepArray[item.stepArray.length - 1].msg) }}
                    </span>
                    <span v-else>
                      <li v-if="item.flag === 3" class="cc-fading-circle">
                        <div class="cc-circle1 cc-circle"></div>
                        <div class="cc-circle2 cc-circle"></div>
                        <div class="cc-circle3 cc-circle"></div>
                        <div class="cc-circle4 cc-circle"></div>
                        <div class="cc-circle5 cc-circle"></div>
                        <div class="cc-circle6 cc-circle"></div>
                        <div class="cc-circle7 cc-circle"></div>
                        <div class="cc-circle8 cc-circle"></div>
                        <div class="cc-circle9 cc-circle"></div>
                        <div class="cc-circle10 cc-circle"></div>
                        <div class="cc-circle11 cc-circle"></div>
                        <div class="cc-circle12 cc-circle"></div>
                      </li>
                      {{getToolStatus(item.currStep, item.toolName)}}
                    </span>
                    <span class="tool-col-status cc-ellipsis" v-if="item.stepArray && item.stepArray.length">
                      <bk-popover theme="light" class="msg-popover" v-if="handleStepArray(item.stepArray) || item.stepArray[item.stepArray.length - 1].msg">
                        <span v-if="handleStepArray(item.stepArray)" style="color: #ff9c01">
                          <tempalte v-if="item.toolName === 'CCN'">
                            {{$t('低于阈值文件x个', { X: handleStepArray(item.stepArray)['defectCount'] })}}，
                          </tempalte>
                          <tempalte v-else>
                            {{$t('x个大文件y个问题', { X: handleStepArray(item.stepArray)['fileCount'], Y: handleStepArray(item.stepArray)['defectCount'] })}}，
                          </tempalte>
                          <a download target="_blank" :href="getDownloadUrl(handleStepArray(item.stepArray))">{{$t('点击下载文件')}}</a>
                        </span>
                        <div slot="content">
                          <span v-if="handleStepArray(item.stepArray)">
                            <p>
                              <tempalte v-if="item.toolName === 'CCN'">
                                {{$t('低于阈值文件x个', { X: handleStepArray(item.stepArray)['defectCount'] })}}，
                              </tempalte>
                              <tempalte v-else>
                                {{$t('x个大文件y个问题', { X: handleStepArray(item.stepArray)['fileCount'], Y: handleStepArray(item.stepArray)['defectCount'] })}}，
                              </tempalte>
                              <a download target="_blank" :href="getDownloadUrl(handleStepArray(item.stepArray))">{{$t('点击下载文件')}}</a>
                            </p>
                            <p v-if="item.toolName !== 'CCN'">{{$t('其中：')}}</p>
                            <p v-for="(gatherFile, index) in handleStepArray(item.stepArray)['gatherFileList']" :key="index">
                              {{gatherFile.relPath}}{{$t('共x个问题', { num: gatherFile.total })}}
                            </p>
                          </span>
                        </div>
                      </bk-popover>
                    </span>
                  </p>
                  <tick-time v-if="item.flag === 3" :time="item.elapseTime" :start-time="item.startTime" class="col-time"></tick-time>
                  <span v-else class="col-time">{{formatSeconds(item.elapseTime)}}</span>
                </bk-col>
              </bk-row>
            </bk-container>
          </div>
        </template>
      </bk-table-column>
      <bk-table-column :label="$t('构建号')" prop="buildNum">
        <template slot-scope="scope">
          <span>#{{scope.row.buildNum}}</span>
        </template>
      </bk-table-column>
      <bk-table-column :label="$t('状态')">
        <template slot-scope="scope">
          <span class="status" :class="{ 'status-success': scope.row.status === 0, 'status-fail': scope.row.status === 1 }">
            <li v-if="scope.row.status === 3" class="cc-fading-circle">
              <div class="cc-circle1 cc-circle"></div>
              <div class="cc-circle2 cc-circle"></div>
              <div class="cc-circle3 cc-circle"></div>
              <div class="cc-circle4 cc-circle"></div>
              <div class="cc-circle5 cc-circle"></div>
              <div class="cc-circle6 cc-circle"></div>
              <div class="cc-circle7 cc-circle"></div>
              <div class="cc-circle8 cc-circle"></div>
              <div class="cc-circle9 cc-circle"></div>
              <div class="cc-circle10 cc-circle"></div>
              <div class="cc-circle11 cc-circle"></div>
              <div class="cc-circle12 cc-circle"></div>
            </li>
            <i v-else class="status-icon" :class="{ 'fail': scope.row.status === 1 }"></i>
            {{ [$t('成功'), $t('失败'), '', $t('进行中')][scope.row.status] }}
          </span>
          <bk-popover theme="light" class="msg-popover" v-if="handleStepArray(scope.row.stepArray)">
            <i class="codecc-icon icon-tips"></i>
            <div slot="content">
              <span>
                <p>
                  <tempalte v-if="scope.row.toolName === 'CCN'">
                    {{$t('低于阈值文件x个', { X: handleStepArray(scope.row.stepArray)['defectCount'] })}}，
                  </tempalte>
                  <tempalte v-else>
                    {{$t('x个大文件y个问题', { X: handleStepArray(scope.row.stepArray)['fileCount'], Y: handleStepArray(scope.row.stepArray)['defectCount'] } )}}，
                  </tempalte>
                  <a download target="_blank" :href="getDownloadUrl(handleStepArray(scope.row.stepArray))">{{$t('点击下载文件')}}</a>
                </p>
                <p v-if="scope.row.toolName !== 'CCN'">{{$t('其中：')}}</p>
                <p v-for="(item, index) in handleStepArray(scope.row.stepArray)['gatherFileList']" :key="index">
                  {{item.relPath}}{{$t('共x个问题', { num: item.total })}}
                </p>
              </span>
            </div>
          </bk-popover>
        </template>
      </bk-table-column>
      <bk-table-column :label="$t('耗时')" prop="elapseTime">
        <template slot-scope="scope">
          <tick-time v-if="scope.row.status === 3" :time="scope.row.elapseTime" :start-time="scope.row.startTime" class="col-time"></tick-time>
          <span v-else class="col-time">{{formatSeconds(scope.row.elapseTime)}}</span>
        </template>
      </bk-table-column>
      <bk-table-column :label="$t('开始时间')" prop="startTime">
        <template slot-scope="scope">
          <span>{{scope.row.startTime | formatDate}}</span>
        </template>
      </bk-table-column>
      <bk-table-column :label="$t('结束时间')" prop="endTime">
        <template slot-scope="scope">
          <span>{{scope.row.endTime | formatDate}}</span>
        </template>
      </bk-table-column>
      <bk-table-column :label="$t('操作')" width="150">
        <template slot-scope="scope">
          <bk-button theme="primary" text @click="showLog(scope.row.buildId, scope.row.buildNum)">{{$t('查看日志')}}</bk-button>
        </template>
      </bk-table-column>
      <div slot="empty">
        <div class="codecc-table-empty-text">
          <img src="../../images/empty.png" class="empty-img">
          <div>{{$t('暂无数据')}}</div>
        </div>
      </div>
    </bk-table>
    <article class="cc-log-home" v-if="slider.isShow">
      <section class="cc-log-main" v-bk-clickoutside="closeLog">
        <section class="cc-log-head">
          <span>CodeCC #{{buildNum}}</span>
          <bk-log-search :down-load-link="downloadUrl"></bk-log-search>
        </section>

        <bk-log class="cc-log" ref="bkLog"></bk-log>
      </section>
    </article>
  </div>
</template>
<script>
  import { format } from 'date-fns'
  import { getToolStatus, getLogFlag, formatSeconds } from '@/common/util'
  import { mapState, mapActions } from 'vuex'
  import taskWebsocket from '@/common/taskWebSocket'
  import tickTime from '@/components/tick-time'
  import log from '@blueking/log'
  import Vue from 'vue'

  Vue.use(log)
  export default {
    components: {
      tickTime,
    },
    data() {
      return {
        pagination: {
          current: 1,
          count: 100,
          limit: 10,
        },
        slider: {
          isShow: false,
          title: this.$t('查看日志'),
        },
        params: {
          toolName: this.$route.params.toolId,
          page: 1,
          pageSize: 10,
        },
        projectId: this.$route.params.projectId,
        taskId: this.$route.params.taskId,
        toolName: this.$route.params.toolId,
        downloadUrl: '',
        buildId: '',
        hasTime: false,
        buildNum: '',
        loading: true,
        logList: [],
        getToolStatus,
        getLogFlag,
      }
    },
    computed: {
      ...mapState('task', {
        taskDetail: 'detail',
      }),
      ...mapState('tool', {
        toolMap: 'mapList',
      }),
      logPostData() {
        return {
          projectId: this.projectId,
          pipelineId: this.taskDetail && this.taskDetail.pipelineId,
          buildId: '',
          lineNo: 0,
          id: undefined,
        }
      },
      expendRowKey() {
        const row = this.logList[0] || {}
        return [row.buildId]
      },
    },
    watch: {
      params: {
        handler() {
          this.init()
        },
        deep: true,
      },
    },
    created() {
      this.init()
    },
    mounted() {
      this.subscribeMsg()
    },
    methods: {
      ...mapActions('devops', [
        'getInitLog',
        'getAfterLog',
      ]),
      async init() {
        this.loading = true
        const res = await this.$store.dispatch('task/taskLog', this.params)
        this.loading = false
        if (res) {
          this.logList = res.content || []
          this.pagination.count = res.totalElements
        }
      },
      formatSeconds(s) {
        return formatSeconds(s)
      },
      showLog(buildId, buildNum) {
        this.buildNum = buildNum
        window.scrollTo(0, 0)
        if (buildId) {
          this.logPostData.buildId = buildId
        }
        this.slider.isShow = true
        const logUrl = `${window.DEVOPS_API_URL}/log/api/user/logs
/${this.projectId}/${this.taskDetail.pipelineId}`
        this.downloadUrl = `${logUrl}/${this.logPostData.buildId}/download`

        this.getInitLog(this.logPostData).then((res) => {
          this.handleLogRes(res)
        })
      },
      handleLogRes(res) {
        if (this.$refs.bkLog === undefined) return
        const logs = res.data.logs || []
        this.$refs.bkLog.addLogData(logs)

        const lastLog = logs[logs.length - 1] || {}
        const lastLogNo = lastLog.lineNo || this.logPostData.lineNo - 1 || -1
        this.logPostData.lineNo = +lastLogNo + 1
        if (res.data.finished) {
          if (res.data.hasMore) {
            this.getAfterLogApi(100)
          }
        } else {
          this.getAfterLogApi(1000)
        }
      },
      getAfterLogApi(mis) {
        this.logPostData.id = setTimeout(() => {
          if (this.$refs.bkLog === undefined) return
          this.getAfterLog(this.logPostData).then((res) => {
            this.handleLogRes(res)
          })
        }, mis)
      },
      handlePageChange(page) {
        this.pagination.current = page
        this.params = { ...this.params, page }
      },
      handlePageLimitChange(pageSize) {
        this.params.page = 1
        this.pagination.current = 1
        this.params = { ...this.params, pageSize }
      },
      subscribeMsg() {
        const subscribe = `/topic/analysisDetail/taskId/${this.taskId}`
        if (taskWebsocket.stompClient.connected) {
          taskWebsocket.subscribeMsg(subscribe, {
            success: (res) => {
              const data = JSON.parse(res.body)
              console.log('🚀 ~ file: log.vue ~ line 373 ~ subscribeMsg ~ data', data)
              let hasNewLog = 1
              this.logList.forEach((item) => {
                if (item.buildId === data.buildId) {
                  Object.assign(item, data)
                  hasNewLog = 0
                }
              })
              if (hasNewLog) this.init()
            },
            // error: message => this.$showTips({ message, theme: 'error' }),
            error: message => console.error(message),
          })
        } else { // websocket还没连接的话，1s后重试
          setTimeout(() => {
            console.log('websocket reconnect')
            this.subscribeMsg()
          }, 1000)
        }
      },
      formatMsg(msg) {
        try {
          const gatherFile = JSON.parse(msg)
          console.log('gatherFile: ', gatherFile)
          return gatherFile
        } catch (error) {
          return false
        }
      },
      getDownloadUrl(gatherFile) {
        return `${window.AJAX_URL_PREFIX}/schedule/api/user/fs/download/type/GATHER/filename/${gatherFile.fileName}`
      },
      closeLog(event) {
        this.slider.isShow = false
        clearTimeout(this.logPostData.id)
      },
      handleStepArray(stepArray = []) {
        const newArray = stepArray.map(item => this.formatMsg(item.msg)).filter(item => item)
        return newArray[0]
      },
      reverseStepArray(stepArray = []) {
        const newArray = stepArray.slice().reverse()
        return newArray.map((item, index) => {
          item.index = index + 1
          return item
        })
      },
      getAnalyseMsg(stepArray = []) {
        const step = stepArray.find(item => item.stepNum === 3) || {}
        return step.msg || ''
      },
    },
  }
</script>

<style lang="postcss">
    @import "../../css/log.1.0.5.min.css";
</style>
<style lang="postcss" scoped>
    @import "../../css/variable.css";
    .cc-log-home {
      position: fixed;
      top: 0;
      left: 0;
      bottom: 0;
      right: 0;
      background-color: rgba(0,0,0,.2);
      z-index: 1000;
      .cc-log-main {
        position: relative;
        width: 80%;
        height: calc(100% - 32px);
        float: right;
        display: flex;
        flex-direction: column;
        margin: 16px;
        border-radius: 6px;
        overflow: hidden;
        transition-property: transform, opacity;
        transition: transform 200ms cubic-bezier(.165, .84, .44, 1),opacity 100ms cubic-bezier(.215, .61, .355, 1);
        background: #1e1e1e;
        .cc-log-head {
          line-height: 48px;
          padding: 5px 20px;
          border-bottom: 1px solid;
          border-bottom-color: #2b2b2b;
          display: flex;
          align-items: center;
          justify-content: space-between;
          color: #d4d4d4;
        }
        .cc-log {
          height: calc(100% - 60px)
        }
      }
    }
    .icon-tips {
      color: #ff9c01;
    }
    .code-content {
      padding: 20px;
      &:not(:last-child) {
        border-bottom: 1px solid #dcdee5;
      }
    }
    .log-expand {
      padding: 20px 15px;
      .log-txt {
        font-size: 14px;
        line-height: 22px;
        padding-bottom: 6px;
      }
      .tool-container {
        margin: 15px 0;
        border-top: 1px solid #dcdee5;
        border-left: 1px solid #dcdee5;
        .tool-col {
          border-right: 1px solid #dcdee5;
          border-bottom: 1px solid #dcdee5;
          height: 80px;
          padding-top: 15px;
          .col-name {
            font-size: 16px;
            padding-left: 30px;
            height: 20px;
          }
          .col-status {
            padding-left: 30px;
            padding-top: 5px;
            color: #979BA5;
            &.success {
              color: $successColor;
            }
            &.fail {
              color: $failColor;
            }
            &.going {
              color: $goingColor;
            }
          }
          .col-time {
            position: absolute;
            top: 3px;
            right: 8px;
            color: #c4c6cc;
          }
          .tool-col-status {
            display: inline-block;
            max-width: 98%;
          }
        }
        &.small-container {
          border-top: none;
          .tool-col {
            border-top: 1px solid #dcdee5;
          }
        }
      }
    }
    .status-icon {
      display: inline-block;
      margin-bottom: 1px;
      width: 8px;
      height: 8px;
      border-radius: 8px;
      background: #86e7a9;
      border: 1px solid #3fc06d;
      &.fail {
        background: #fd9c9c;
        border: 1px solid #ea3636;
      }
    }
    .tool-col-table {
      td {
        .step {
          display: inline-block;
          width: 6px;
          height: 6px;
          background-color: #c4c6cc;
          border-radius: 6px;
          &.status {
            background-color: $goingColor;
            &.status-success {
              background-color: $successColor;
            }
            &.status-fail {
              background-color: $failColor;
            }
          }
        }
      }
      td {
        .status {
          color: $goingColor;
          &.status-success {
            color: $successColor;
          }
          &.status-fail {
            color: $failColor;
          }
        }
      }
      .msg-popover {
        max-width: 100%;
        .bk-tooltip-ref {
          max-width: 100%;
        }
        span {
          display: inline-block;
          max-width: 100%;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
      }
    }
    .icon-info-circle-shape {
      position: relative;
      top: -1px;
      color: #a3c5fd;
    }
</style>
