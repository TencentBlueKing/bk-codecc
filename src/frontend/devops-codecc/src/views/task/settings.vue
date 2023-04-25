<template>
  <div :class="{ 'mini': active === 'tools' }">
    <bk-tab type="border-card" :label-height="42" class="cc-settings" :active.sync="active" :before-toggle="beforeToggle" :tab-change="handleTabChange">
      <bk-tab-panel
        v-for="(panel, index) in panels"
        v-bind="panel"
        render-directive="if"
        :key="index">
        <template slot="label">
          <span class="panel-name">{{panel.label}}</span>
          <i v-if="panel.name === 'issue' && !hasRedPointStore" class="red-point"></i>
        </template>
        <router-view></router-view>
      </bk-tab-panel>
    </bk-tab>
  </div>
</template>

<script>
  import { mapState } from 'vuex'
  import config from '../../../config'
  export default {
    data() {
      const panels = [
        { name: 'code', label: this.$t('基础信息') },
        { name: 'checkerset', label: this.$t('规则集配置') },
        { name: 'report', label: this.$t('通知报告') },
        { name: 'issue', label: this.$t('问题提单') },
        { name: 'trigger', label: this.$t('扫描触发') },
        { name: 'ignore', label: this.$t('路径屏蔽') },
        { name: 'authority', label: this.$t('人员权限') },
        { name: 'record', label: this.$t('操作记录') },
        { name: 'manage', label: this.$t('任务管理') },
      ]
      return {
        panels: panels.filter(item => config.settings.includes(item.name)),
        active: this.$route.name.split('-').pop(),
        hasRedPointStore: window.localStorage.getItem('tapd-20210628'),
      }
    },
    beforeRouteUpdate(to, from, next) {
      this.active = to.name.split('-').pop()
      next()
    },
    computed: {
      ...mapState('task', {
        taskDetail: 'detail',
      }),
    },
    watch: {
      active(value) {
        if (value === 'issue') {
          window.localStorage.setItem('tapd-20210628', '1')
          this.hasRedPointStore = true
        }
      },
    },
    created() {
      const { panel } = this.$route.query
      if (panel) {
        this.$router.push({ name: `task-settings-${panel}` })
      }
    },
    methods: {
      beforeToggle(name) {
        if (this.taskDetail.createFrom.indexOf('pipeline') !== -1) {
          if (name === 'tools' || name === 'manage') {
            const titleMap = {
              tools: this.$t('此代码检查任务为流水线创建，工具需前往相应流水线添加。'),
              manage: this.$t('此代码检查任务为流水线创建，任务需前往相应流水线管理。'),
            }
            const that = this
            this.$bkInfo({
              title: this.$t('温馨提示'),
              subTitle: titleMap[name],
              maskClose: true,
              confirmFn(name) {
                window.open(`${window.DEVOPS_SITE_URL}/console/pipeline/${that.taskDetail.projectId}/
${that.taskDetail.pipelineId}/edit#${that.taskDetail.atomCode}`, '_blank')
              },
            })
            return false
          }
          this.$router.push({ name: `task-settings-${name}` })
        } else {
          this.$router.push({ name: `task-settings-${name}` })
        }
      },
      handleTabChange(name) {
        console.log('🚀 ~ file: detail.vue ~ line 356 ~ handleTabChange ~ name', name)
      },
    },
  }
</script>

<style>
    /* hack tab内容区域高度 */
    .bk-tab-section {
      min-height: calc(100% - 43px);
      background: #fff;
    }
</style>

<style lang="postcss" scoped>
    .main-content.mini {
      max-width: calc(100% - 350px);
      min-width: 1085px;
      >>>.params-side {
        top: 57px;
        max-height: calc(100vh - 152px);
        toolparams {
          max-height: calc(100vh - 262px);
        }
      }
    }
    >>> .cc-settings>.bk-tab-header>.bk-tab-label-wrapper>.bk-tab-label-list>.bk-tab-label-item {
      &:nth-of-type(1),
      &:nth-of-type(2) {
        >.bk-tab-label::after {
          content: "*";
          color: #ff5656;
          position: relative;
          margin: 2px -7px 0 2px;
          display: inline-block;
          vertical-align: middle;
        }
      }
    }
</style>
