<template>
  <div v-bkloading="{ isLoading: !mainContentLoading && contentLoading, opacity: 0.3 }">
    <div class="cloc-list" v-if="taskDetail.enableToolList.find(item => item.toolName === 'SCC' || item.toolName === 'CLOC')">
      <div class="breadcrumb">
        <div class="breadcrumb-name">
          <bk-tab :active.sync="active" :label-height="42" @tab-change="handleTableChange" type="unborder-card">
            <bk-tab-panel
              v-for="(panel, index) in panels"
              v-bind="panel"
              :key="index">
            </bk-tab-panel>
          </bk-tab>
        </div>
      </div>
      <div class="main-container" ref="mainContainer">
        <div class="main-content-inner main-content-list">
          <div class="catalog">
            <span class="mr10">
              {{this.$t('共x种主要语言', { num: mainLangNo })}}
            </span>
          </div>
          <bk-table class="cloc-list-table"
                    :data="clocList"
                    size="medium"
                    v-show="isFetched"
                    v-bkloading="{ isLoading: fileLoading, opacity: 0.6 }">
            <bk-table-column class-name="name" :label="$t('语言')" prop="language"></bk-table-column>
            <bk-table-column :label="$t('总行数')" align="right" class-name="pr40" prop="sumLines"></bk-table-column>
            <bk-table-column :label="$t('占比')" align="right" class-name="pr40" prop="proportion">
              <template slot-scope="props">
                <span>{{props.row.proportion + '%'}}</span>
              </template>
            </bk-table-column>
            <bk-table-column :label="$t('代码行')" align="right" class-name="pr40" prop="sumCode">
              <template slot-scope="props">
                <span v-bk-tooltips="{ content: $t('代码行比率') + handleRate(props.row.sumCode, props.row.sumLines) }">
                  {{props.row.sumCode}}
                </span>
              </template>
            </bk-table-column>
            <!-- <bk-table-column :label="$t('注释行')" align="right" class-name="pr40" prop="sumComment">
              <template slot-scope="props">
                <span v-bk-tooltips="{ content: $t('注释率') + handleCommentRate(props.row.commentRate) }">
                  {{props.row.sumComment}}
                </span>
              </template>
            </bk-table-column> -->
            <bk-table-column :label="$t('注释行')" align="right" class-name="pr40" prop="sumEfficientComment" :render-header="renderHeader">
              <template slot-scope="props">
                <span v-if="realTool === 'SCC' && props.row.sumEfficientComment !== undefined" v-bk-tooltips="{ content: $t('注释率') + handleCommentRate(props.row.efficientCommentRate) }">
                  {{props.row.sumEfficientComment}}
                </span>
                <span v-else>--</span>
              </template>
            </bk-table-column>
            <bk-table-column :label="$t('空白行')" align="right" class-name="pr40" prop="sumBlank">
              <template slot-scope="props">
                <span v-bk-tooltips="{ content: $t('空白率') + handleRate(props.row.sumBlank, props.row.sumLines) }">
                  {{props.row.sumBlank}}
                </span>
              </template>
            </bk-table-column>
            <div slot="empty">
              <div class="codecc-table-empty-text">
                <img src="../../images/empty.png" class="empty-img">
                <div>{{$t('没有查询到数据')}}</div>
              </div>
            </div>
          </bk-table>
        </div>
      </div>
    </div>
    <div class="cloc-list" v-else>
      <div class="main-container large boder-none">
        <div class="no-task">
          <empty title="" :desc="$t('CodeCC集成了代码统计工具，可以检测代码中所包含的语言种类，以及代码行、注释行、空白行等占比')">
            <template v-slot:action>
              <bk-button size="large" theme="primary" @click="addTool({ from: 'cloc' })">{{$t('配置规则集')}}</bk-button>
            </template>
          </empty>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  import { mapState } from 'vuex'
  import Empty from '@/components/empty'
  export default {
    components: {
      Empty,
    },
    data() {
      return {
        contentLoading: true,
        fileLoading: false,
        isFetched: false,
        panels: [
          { name: 'list', label: this.$t('按目录') },
          { name: 'lang', label: this.$t('按语言') },
        ],
        active: 'lang',
        clocList: [],
        mainLangNo: 0,
        realTool: '',
      }
    },
    computed: {
      ...mapState('task', {
        taskDetail: 'detail',
      }),
      toolId() {
        // return this.taskDetail.enableToolList.find(item => item.toolName === 'SCC') ? 'SCC' : 'CLOC'
        return 'CLOC'
      },
    },
    created() {
      if (!this.taskDetail.nameEn
        || this.taskDetail.enableToolList.find(item => item.toolName === 'SCC' || item.toolName === 'CLOC')) {
        this.init(true)
      }
    },
    methods: {
      async init(isInit) {
        isInit ? this.contentLoading = true : this.fileLoading = true
        const res = await this.$store.dispatch('defect/lintListCloc', { toolId: this.toolId, type: 'LANGUAGE' })
        if (isInit) {
          this.contentLoading = false
          this.isFetched = true
        }
        if (res) {
          this.fileLoading = false
          const { otherInfo, totalInfo, languageInfo } = res
          const languageInfoNew = languageInfo.filter(item => item.language !== 'OTHERS')
          this.mainLangNo = languageInfoNew.length
          this.clocList = [totalInfo, ...languageInfoNew, otherInfo]
          this.realTool = res.toolName
        }
      },
      handleTableChange(value) {
        this.$router.push({ name: `defect-cloc-${value}` })
      },
      handleRate(num, sum) {
        return `${(num * 100 / sum).toFixed(2)}%`
      },
      handleCommentRate(num) {
        return `${(num * 100).toFixed(2)}%`
      },
      addTool(query) {
        if (this.taskDetail.createFrom.indexOf('pipeline') !== -1) {
          const that = this
          this.$bkInfo({
            title: this.$t('配置规则集'),
            subTitle: this.$t('此代码检查任务为流水线创建，规则集需前往相应流水线配置。'),
            maskClose: true,
            confirmFn(name) {
              window.open(`${window.DEVOPS_SITE_URL}/console/pipeline/${that.taskDetail.projectId}
/${that.taskDetail.pipelineId}/edit#${that.taskDetail.atomCode}`, '_blank')
            },
          })
        } else {
          this.$router.push({ name: 'task-settings-checkerset', query })
        }
      },
      renderHeader(h, data) {
        const id = '820084513'
        const directive = {
          name: 'bkTooltips',
          content: `<p>${this.$t('根据腾讯代码委员会要求，注释行需排除重复文件头注释、代码注释、无内容注释行等无信息量的内容。')}
          </p>`,
        }
        return <span class="custom-header-cell" v-bk-tooltips={ directive }>{ data.column.label }</span>
      },
    },

  }
</script>

<style lang="postcss" scoped>
    @import "./defect-list.css";

    .cloc-list {
      padding: 16px 20px 0px 16px;
    }
    .main-container {
      /* border-top: 1px solid #dcdee5; */
      margin: 0px!important;
      background: white;
    }
    .breadcrumb {
      padding: 0px!important;
      .breadcrumb-name {
        background: white;
      }
    }
    .bk-table.cloc-list-table {
      margin: 10px 0;
      >>>.bk-table-body .name {
        font-weight: bold;

        /* font-size: 14px!important; */
      }
    }
    .catalog {
      font-size: 14px;
    }
</style>
