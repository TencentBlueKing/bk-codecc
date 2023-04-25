<template>
  <div class="main-content-outer main-content-task">
    <div v-if="!isFetched">
      <tool-bar :task-count="tasksTotal" :search-info="searchInfo" @changeOrder="changeOrder"></tool-bar>
      <div class="task-list-inuse" v-if="!isEmpty">
        <div class="task-card-list">
          <div class="task-card-item"
               v-for="(task, taskIndex) in tasksList"
               :key="taskIndex">
            <task-card :task="task" :get-task-link="getTaskLink" :handle-task="handleTask"></task-card>
          </div>
        </div>
      </div>
      <div v-else>
        <div class="task-card-list">
          <div class="task-card-item" v-for="item in skeletonCount" :key="item">
            <task-card-skeleton></task-card-skeleton>
          </div>
        </div>
      </div>
    </div>
    <div v-else-if="projectId && (!isEmpty || isSearch)">
      <tool-bar :task-count="tasksTotal" :search-info="searchInfo" @changeOrder="changeOrder"></tool-bar>
      <div class="task-list-inuse" v-if="!isEmpty">
        <div ref="taskCardList" class="task-card-list">
          <div class="task-card-item"
               v-for="(task, taskIndex) in tasksList"
               :key="taskIndex">
            <task-card :task="task" :get-task-link="getTaskLink" :handle-task="handleTask"></task-card>
          </div>
          <div v-if="isMoreLoading" class="more-loading" v-bkloading="{ isLoading: isMoreLoading }"></div>
        </div>
      </div>
      <div slot="empty" v-else-if="isSearch">
        <div class="codecc-table-empty-text">
          <img src="../../images/empty-search.png" class="empty-img">
          <div>{{$t('搜索结果为空')}}</div>
        </div>
      </div>
      <div slot="empty" v-else>
        <div class="codecc-table-empty-text">
          <img src="../../images/empty.png" class="empty-img">
          <div>{{$t('暂无数据')}}</div>
        </div>
      </div>
    </div>
    <div v-else-if="!projectId" class="no-task" v-show="!projectId">
      <empty :title="$t('暂无项目')" :desc="$t('你可以通过按钮跳转至项目管理，来创建新项目')">
        <template v-slot:action>
          <bk-button size="large" theme="primary" @click="createProject">{{$t('项目管理')}}</bk-button>
        </template>
      </empty>
    </div>
    <div v-else-if="projectId && isEmpty && !isSearch" class="no-task" v-show="isEmpty">
      <empty :title="$t('暂无任务')" :desc="$t('你可以通过新增按钮，来创建代码检查任务')">
        <template v-slot:action>
          <bk-button size="large" theme="primary" @click="$router.push({ name: 'task-new' })">{{$t('新增任务')}}</bk-button>
        </template>
      </empty>
    </div>
    <bk-dialog v-model="dialogVisible"
               :theme="'primary'"
               :mask-close="false"
               @confirm="reAnalyse"
               :title="$t('重新检查')">
      {{this.$t('任务正在分析中，是否中断并重新分析？')}}
    </bk-dialog>
  </div>
</template>

<script>
  import { mapState, mapGetters } from 'vuex'
  import Empty from '@/components/empty'
  import TaskCard from './task-card'
  import ToolBar from './tool-bar'
  import projectWebSocket from '@/common/projectWebSocket'
  import TaskCardSkeleton from './task-card-skeleton.vue'

  export default {
    components: {
      Empty,
      TaskCard,
      TaskCardSkeleton,
      ToolBar,
    },
    data() {
      return {
        retryTask: {},
        isShowDisused: false,
        dialogVisible: false,
        isSearch: false,
        isFetched: false,
        orderType: 'CREATE_DATE',
        tasksList: [],
        tasksTotal: 0,
        searchInfo: {
          taskStatus: '',
          taskStatusList: [],
          taskSource: '',
          showDisabledTask: false,
        },
        pageInfo: {
          pageable: true,
          page: 1,
          pageSize: 15,
        },
        scrollDom: null,
        isThrottled: false,
        isPageOver: false,
        isMoreLoading: false,
      }
    },
    computed: {
      ...mapState([
        'toolMeta',
      ]),
      // ...mapGetters(['mainContentLoading']),
      isEmpty() {
        return !this.tasksList.length
      },
      projectId() {
        return this.$route.params.projectId
      },
      skeletonCount() {
        let count = Math.floor((window.innerHeight - 160) / 96)
        count = count > 3 ? count : 3
        return count
      },
    },
    watch: {
      searchInfo: {
        deep: true,
        handler(newVal) {
          this.isSearch = true
          this.isFetched = false
          this.pageInfo.page = 1
          this.tasksList = []
          this.fetchPageData(true)
        },
      },
    },
    mounted() {
      this.initWebSocket()
    },
    created() {
      if (!this.$route.params.projectId && window.self === window.top) {
        window.location.href = `${window.DEVOPS_SITE_URL}/console/codecc`
      }
      this.$nextTick(() => {
        this.pageMainDom = document.querySelector('.page-main')
        this.pageMainDom.addEventListener('scroll', this.scrollLoading)
      })
    },
    beforeDestroy() {
      projectWebSocket.disconnect()
      this.pageMainDom.removeEventListener('scroll', this.scrollLoading)
    },
    methods: {
      async fetchPageData(type) {
        let params = Object.assign({}, this.searchInfo, this.pageInfo, { orderType: this.orderType })
        if (type === 'toggleTaskTop') {
          params = Object.assign(params, {
            page: 1,
            pageSize: this.tasksList.length,
          })
        } else if (type === 'toggleOrder') {
          this.tasksList = []
          this.isFetched = false
          params = Object.assign(params, {
            page: 1,
          })
        } else {
          this.isMoreLoading = true
        }
        try {
          const res = await this.$store.dispatch('task/list', params)
          if (['toggleTaskTop', 'toggleOrder'].includes(type)) {
            this.tasksList = res.pageTasks.content
          } else {
            this.tasksList = [...this.tasksList, ...res.pageTasks.content]
          }
          this.tasksTotal = res.pageTasks.totalElements
          this.isPageOver = this.tasksList.length === res.pageTasks.totalElements
          this.isMoreLoading = false
        } catch {
          return false
        } finally {
          this.isFetched = true
        }
      },
      /**
       * 滚动加载
       */
      scrollLoading() {
        if (!this.isPageOver && !this.isThrottled && !this.isMoreLoading) {
          this.isThrottled = true
          this.isTimer = setTimeout(async () => {
            this.isThrottled = false
            this.taskListDom = this.$refs.taskCardList
            const { scrollHeight } = this.taskListDom // 列表高度
            const { offsetHeight } = this.pageMainDom // 可视区域高度(除头部部分)
            const { scrollTop } = this.pageMainDom // 滚动出去的高度
            if (scrollHeight - offsetHeight - scrollTop < 60) {
              this.pageInfo.page += 1
              await this.fetchPageData()
            }
          }, 300)
        }
      },
      async handleTask(task, type) {
        if (type === 'top') {
          this.toggleTaskTop(task)
        } else if (type === 'execute') {
          this.analyse(task)
        } else if (type === 'retry') {
          this.dialogVisible = true
          this.retryTask = task
        } else if (type) {
          this.enableTask(task)
        }
      },
      toggleTaskTop(task) {
        const params = {
          taskId: task.taskId,
          topFlag: task.topFlag !== 1,
        }
        this.$store.dispatch('task/editTaskTop', params).then((res) => {
          if (res.code === '0') {
            this.fetchPageData('toggleTaskTop')
          }
        })
          .catch((e) => {
            this.$bkMessage({ theme: 'error', message: this.$t('置顶失败') })
          })
      },
      enableTask(task) {
        if (task.createFrom === 'bs_codecc') {
          this.$store.commit('updateTaskId', task.taskId)
          this.$store.dispatch('task/startManage', task.taskId).then((res) => {
            if (res.data === true) {
              this.$bkMessage({
                theme: 'success',
                message: this.$t('启用任务成功'),
              })
            }
            this.fetchPageData()
          })
            .catch((e) => {
              console.error(e)
            })
        } else {
          window.open(`${window.DEVOPS_SITE_URL}/console/pipeline/${this.projectId}/${task.pipelineId}/edit`, '_blank')
        }
      },
      async analyse(task) {
        if (task.createFrom.indexOf('pipeline') !== -1) {
          const { pipelineId } = task
          const { projectId } = this
          this.$bkInfo({
            title: this.$t('立即检查'),
            subTitle: this.$t('此代码检查任务需要到流水线启动，是否前往流水线？'),
            maskClose: true,
            confirmFn(name) {
              window.open(`${window.DEVOPS_SITE_URL}/console/pipeline/${projectId}/${pipelineId}/edit`, '_blank')
            },
          })
        } else {
          this.$store.commit('updateTaskId', task.taskId)
          await this.$store.dispatch('task/triggerAnalyse').then((res) => {
            if (res.code === '0') {
              // this.$bkMessage({
              //   theme: 'success',
              //   message: this.$t('触发成功'),
              // })
              // this.fetchPageData()
              this.retryTask = {}
            }
          })
        }
      },
      reAnalyse() {
        this.analyse(this.retryTask, 1)
      },
      changeOrder(order) {
        if (order !== this.orderType) {
          this.orderType = order
          this.fetchPageData('toggleOrder')
        }
      },
      toggleDisused() {
        this.isShowDisused = !this.isShowDisused
      },
      getTaskLink(task, type) {
        const link = { params: { projectId: this.projectId, taskId: task.taskId } }
        if (type === 'detail') {
          link.name = 'task-detail'
          link.query = { buildNum: 'latest' }
        } else if (type === 'logs') {
          link.name = 'task-detail-log'
          link.params.toolId = task.displayToolName
        } else if (type === 'setting') {
          link.name = 'task-settings-code'
        }

        this.$router.push(link)
      },
      createProject() {
        window.open(`${window.DEVOPS_SITE_URL}/console/pm/`)
      },

      initWebSocket() {
        const subscribe = `/topic/analysisProgress/projectId/${this.projectId}`
        projectWebSocket.connect(this.projectId, subscribe, {
          success: (res) => {
            const data = JSON.parse(res.body)
            console.log('🚀 ~ file: list.vue ~ line 258 ~ initWebSocket ~ data', data)
            this.tasksList.forEach((task) => {
              if (task.taskId === data.taskId) {
                Object.assign(task, data)
              }
            })
          },
          // error: message => this.$showTips({ message, theme: 'error' }),
          error: message => console.error(message),
        })
      },
    },
  }
</script>

<style lang="postcss" scoped>
    @import "../../css/mixins.css";
    @import "../../css/main-content-outer.css";

    .main-content-outer {
      width: 1236px;
    }
    .main-content-task {
      .toolbar {
        margin-bottom: 16px;
      }
      .codecc-table-empty-text {
        text-align: center;
        padding-top: 120px;
      }
    }
    .task-card-list {
      display: flex;
      flex-wrap: wrap;
      .task-card-item {
        margin: 0 20px 16px 0;
        float: left;
      }
      .more-loading {
        width: 100%;
        height: 40px;
      }
    }
    .task-list-disused {
      .disused-head {
        border-bottom: 1px solid #dcdee5;
        padding: 8px 0;
        .title {
          display: inline-block;
          color: #313238;
          cursor: pointer;
          .arrow-icon {
            font-size: 11px;
            color: #313238;

            @mixin transition-rotate 0, 90;
          }
        }
      }
      .disused-body {
        margin-top: 12px;
      }
    }
    .no-task {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100%;
    }
</style>
