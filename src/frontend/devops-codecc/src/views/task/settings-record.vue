<template>
  <bk-table
    :data="list"
    :size="size"
    :pagination="pagination"
    v-bkloading="{ isLoading: loading }"
    @page-change="handlePageChange"
    @page-limit-change="handlePageLimitChange">
    <bk-table-column type="index" :label="$t('序号')" align="center" width="100"></bk-table-column>
    <bk-table-column :label="$t('操作人')" prop="operator" width="200"></bk-table-column>
    <bk-table-column :label="$t('操作类型')" prop="operTypeName" width="200"></bk-table-column>
    <bk-table-column :label="$t('相关信息')" prop="operMsg">
      <template slot-scope="props"> <span class="operMsg" :title="props.row.operMsg">{{ props.row.operMsg }}</span></template>
    </bk-table-column>
    <bk-table-column :label="$t('操作时间')" prop="time" width="220">
      <template slot-scope="props">{{ props.row.time | formatDate }}</template>
    </bk-table-column>
    <div slot="empty">
      <div class="codecc-table-empty-text" v-if="!loading">
        <img src="../../images/empty.png" class="empty-img">
        <div>{{$t('暂无操作记录')}}</div>
      </div>
    </div>
  </bk-table>
</template>
<script>
  export default {
    data() {
      return {
        loading: true,
        operateRecords: [],
        pagination: {
          current: 1,
          count: 0,
          limit: 10,
          align: 'right',
        },
      }
    },
    computed: {
      list() {
        const { operateRecords, pagination } = this
        const { current, limit } = pagination
        const list = operateRecords.slice((current - 1) * limit, current * limit)
        return list
      },
    },
    created() {
      this.init()
    },
    methods: {
      init() {
        const funcId = [
          'register_tool',
          'tool_switch',
          'task_info',
          'task_switch',
          'task_code',
          'checker_config',
          'code_comment_add',
          'code_comment_del',
          'issue_defect',
          'scan_schedule',
          'filter_path',
          'defect_manage',
          'trigger_analysis',
          'defect_ignore',
          'revert_ignore',
          'defect_marked',
          'defect_unmarked',
          'assign_defect',
          'settings_authority',
        ]
        const postData = {
          taskId: this.$route.params.taskId,
          funcId,
        }
        this.$store.dispatch('defect/getOperatreRecords', postData).then((res) => {
          this.loading = false
          this.operateRecords = res || []
          this.pagination.count = this.operateRecords.length
        })
      },
      handlePageChange(page) {
        this.pagination.current = page
      },
      handlePageLimitChange(limit) {
        this.pagination.limit = limit
        this.pagination.current = 1
      },
    },
  }
</script>
<style lang="postcss" scoped>
    .operMsg {
      display: block;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
</style>
