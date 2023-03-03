<template>
  <div class="main-content-inner checker">
    <span v-if="taskDetail.createFrom === 'gongfeng_scan' || taskDetail.createFrom === 'api_trigger'" class="fs12 go-pipeline">{{$t('规则集配置由CodeCC开源扫描集群自动生成')}}</span>
    <span v-else-if="editDisabled" class="fs12 go-pipeline">
      {{$t('规则集为x。如需修改，请前往流水线', { name: checkersetStr })}}
      <a class="ml15" @click="hanldeToPipeline" href="javascript:;">{{$t('立即前往>>')}}</a>
    </span>
    <div v-if="checkersetList.length">
      <p class="checker-title">
        {{$t('已启用')}}
        <span class="checker-title-num">{{enableList.length}}</span>
        <i class="codecc-icon icon-tips" v-bk-tooltips="{ content: $t('以下规则集适合于当前任务语言。新启用的规则集将在下次检查时生效') }"></i>
        <a v-if="taskDetail.createFrom !== 'gongfeng_scan' && taskDetail.createFrom !== 'api_trigger'" class="fr fs12 cc-link" @click="$router.push({ name: 'checkerset-list', params: { projectId: projectId } })">{{$t('创建/安装更多规则集')}}</a>
      </p>
      <card v-for="checkerset in list" :key="checkerset.checkerSetId"
            :checkerset="checkerset"
            :from="'task'"
            :has-ccn="hasCcnTips(checkerset)"
            :has-new="hasNewTips(checkerset)"
            :is-new-atom="editDisabled"
            :is-enable="checkerset.taskUsing"
            :handle-manage="handleCheckerset">
      </card>
    </div>
    <div v-else-if="isFetched">
      <div class="codecc-table-empty-text">
        <img src="../../images/empty.png" class="empty-img">
        <div>{{$t('暂无数据')}}</div>
      </div>
    </div>
    <bk-dialog
      v-model="checkersetDialogVisiable"
      :render-directive="'if'"
      :fullscreen="true"
      :draggable="false"
      :mask-close="false"
      :show-footer="false"
      :close-icon="true">
      <div class="checkerset-dialog-main">
        <checkerset-manage
          :is-from-settings="true"
          :checkerset-id="checkersetId"
          :version="version"
          :update-checker-list="updateCheckerList"
          @closeDialog="checkersetDialogVisiable = false">
        </checkerset-manage>
      </div>
    </bk-dialog>
    <bk-dialog
      v-model="checkersetListDialogVisiable"
      :render-directive="'if'"
      :fullscreen="true"
      :draggable="false"
      :mask-close="false"
      :show-footer="false"
      :close-icon="true">
      <div class="checkerset-dialog-main">
        <checkerset-list></checkerset-list>
      </div>
    </bk-dialog>
  </div>
</template>

<script>
  import { mapState } from 'vuex'
  import card from './../checkerset/card'
  import checkersetManage from './../checkerset/manage'
  import checkersetList from './../checkerset/list'
  import { format } from 'date-fns'

  export default {
    components: {
      card,
      checkersetManage,
      checkersetList,
    },
    data() {
      return {
        isFetched: false,
        checkersetList: [],
        checkersetDialogVisiable: false,
        checkersetListDialogVisiable: false,
        checkersetId: '',
        version: '',
        checkerSetEnvData: {},
      }
    },
    computed: {
      ...mapState('task', {
        taskDetail: 'detail',
      }),
      projectId() {
        return this.$route.params.projectId
      },
      taskId() {
        return this.$route.params.taskId
      },
      list() {
        const enableList = this.checkersetList.filter(item => item.taskUsing)
        const disableList = this.checkersetList.filter(item => !item.taskUsing)
        return enableList.concat(disableList)
      },
      enableList() {
        return this.checkersetList.filter(item => item.taskUsing) || []
      },
      editDisabled() {
        return (this.taskDetail.atomCode && this.taskDetail.createFrom === 'bs_pipeline')
          || this.taskDetail.createFrom === 'gongfeng_scan' || this.taskDetail.createFrom === 'api_trigger'
      },
      checkersetStr() {
        const { checkerSetEnvData } = this
        const { checkerSetType, checkerSetEnvType = 'prod' } = this.taskDetail
        if (checkerSetType === 'normal') {
          return this.$t('自主配置')
        }
        const preProdKey = `preProd${checkerSetType.replaceAll('_', '')}TimeGap`.toLocaleLowerCase()
        const prodKey = `prod${checkerSetType.replaceAll('_', '')}TimeGap`.toLocaleLowerCase()
        const preProdTimeKey = Object.keys(checkerSetEnvData).find(item => item.toLocaleLowerCase() === preProdKey)
        const prodTimeKey = Object.keys(checkerSetEnvData).find(item => item.toLocaleLowerCase() === prodKey)
        const preProdStartTime = this.formatTime(checkerSetEnvData[preProdTimeKey]
          && checkerSetEnvData[preProdTimeKey].startTime)
        const preProdEndTime = this.formatTime(checkerSetEnvData[preProdTimeKey]
          && checkerSetEnvData[preProdTimeKey].endTime)
        const ProdTime = this.formatTime(checkerSetEnvData[prodTimeKey] && checkerSetEnvData[prodTimeKey].startTime)
        const strMap = {
          prod: `${this.$t('正式版')}（${ProdTime}${this.$t('发布')}）`,
          preProd: `${this.$t('预发布版')}（${preProdStartTime}${this.$t('发布')}，
          ${preProdEndTime}${this.$t('转正式版')}）`,
        }
        const checkersetMap = {
          openScan: this.$t('内网开源治理'),
          communityOpenScan: this.$t('外网开源'),
          epcScan: 'PCG EPC',
        }
        return `${checkersetMap[checkerSetType]}-${strMap[checkerSetEnvType]}`
      },
    },
    created() {
      this.fetchList()
      this.getOpenScanAndPreProdCheckerSetMap()
    },
    methods: {
      async fetchList() {
        this.$store.commit('setMainContentLoading', true)
        const { projectId, taskId } = this
        const params = { projectId, taskId, showLoading: true }
        const res = await this.$store.dispatch('checkerset/list', params)
        this.checkersetList = res
        this.isFetched = true
        this.$store.commit('setMainContentLoading', false)
      },
      async getOpenScanAndPreProdCheckerSetMap() {
        this.checkerSetEnvData = await this.$store.dispatch('task/getOpenScanAndPreProdCheckerSetMap') || {}
      },
      handleCheckerset(checkerset) {
        this.checkersetId = checkerset.checkerSetId
        this.version = checkerset.version
        this.checkersetDialogVisiable = true
      },
      hanldeToPipeline() {
        if (/^git_\d+$/.test(this.projectId)) {
          window.open(`${window.STREAM_SITE_URL}/pipeline/${this.taskDetail.pipelineId}
#${this.taskDetail.projectName}`, '_blank')
        } else {
          window.open(`${window.DEVOPS_SITE_URL}/console/pipeline/${this.taskDetail.projectId}/
${this.taskDetail.pipelineId}/edit#${this.taskDetail.atomCode}`, '_blank')
        }
      },
      hasCcnTips(checkerset) {
        const isNotPipeline = this.taskDetail.createFrom !== 'bs_pipeline'
        const ownList = this.checkersetList.filter(item => !item.checkerSetSource)
        const isFromCcndupc = this.$route.query.from === 'ccndupc'
        return isFromCcndupc && isNotPipeline && ownList && ownList[0] === checkerset
      },
      hasNewTips(checkerset) {
        return this.checkersetList[0] === checkerset
      },
      updateCheckerList() {
        this.fetchList()
        this.checkersetDialogVisiable = false
      },
      formatTime(time) {
        return time ? format(Number(time), 'YYYY-MM-DD') : '--'
      },
    },

  }
</script>

<style lang="postcss" scoped>
    .main-content-inner {
      padding: 0 20px;
    }
    .go-pipeline {
      display: block;
      padding-bottom: 15px;
      border-bottom: 1px solid #dcdee5;
      margin-bottom: 7px;
    }
    .checker-title {
      line-height: 30px;
      padding-bottom: 10px;
      font-size: 14px;
      .checker-title-num {
        /* padding: 0 10px; */
        color: #bbb;
      }
      .icon-tips {
        position: relative;
        top: -1px;
      }
    }
    .codecc-table-empty-text {
      text-align: center;
      padding-top: 200px;
    }
    .checkerset-dialog-main {
      height: 100%;
      padding-top: 30px;
      .checkerset-manage {
        height: 100%;
      }
    }
</style>
