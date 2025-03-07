<template>
  <div class="ignore-content">
    <bk-button
      v-if="isRbac === true"
      key="create"
      v-perm="{
        disablePermissionApi: false,
        permissionData: {
          projectId: projectId,
          resourceType: 'codecc_ignore_type',
          resourceCode: projectId,
          action: 'codecc_ignore_type_manage',
        },
      }"
      icon="plus"
      theme="primary"
      @click="handleCreate"
    >
      {{ $t('新增忽略审批') }}
    </bk-button>
    <span
      v-else
      v-bk-tooltips="{
        content: $t('仅项目管理员或CI管理员可新增忽略类型'),
        disabled: isProjectManager,
      }"
    >
      <bk-button
        icon="plus"
        theme="primary"
        :disabled="!isProjectManager"
        @click="handleCreate"
      >
        {{ $t('新增忽略审批') }}
      </bk-button>
    </span>
    <span class="title-tips">
      {{ $t('忽略问题需经过审批，防止代码质量有漏网之鱼') }}
    </span>
    <bk-table
      v-bkloading="{ isLoading }"
      class="ignore-table"
      :data="data"
      :pagination="pagination"
      @page-change="handlePageChange"
      @page-limit-change="handlePageLimitChange">
      <bk-table-column :label="$t('审批配置名称')" prop="name" show-overflow-tooltip></bk-table-column>
      <bk-table-column :label="$t('问题维度')" prop="dimensions" show-overflow-tooltip>
        <template slot-scope="{ row }">{{ batchDataShow(row.dimensions, dimensionsMap) }}</template>
      </bk-table-column>
      <bk-table-column :label="$t('问题级别')" prop="severities">
        <template slot-scope="{ row }">{{ batchDataShow(row.severities, defectSeverityMap) }}</template>
      </bk-table-column>
      <bk-table-column :label="$t('问题创建时间')" prop="defectCreateTime" show-overflow-tooltip>
        <template slot-scope="{ row }">{{ formatDate(row.defectCreateTime) }}</template>
      </bk-table-column>
      <bk-table-column :label="$t('忽略类型')" prop="ignoreTypeIds" show-overflow-tooltip>
        <template
          v-if="isIgnoreTypeListReady"
          slot-scope="{ row }">
          {{ batchDataShow(row.ignoreTypeIds, typeListMap) }}
        </template>
      </bk-table-column>
      <bk-table-column :label="$t('任务范围')" prop="taskScopeType" show-overflow-tooltip>
        <template
          slot-scope="{ row }">
          {{ getTaskScopes(row.taskScopeType, row.taskScopeList) }}
        </template>
      </bk-table-column>
      <bk-table-column :label="$t('审批人')" prop="approverType" show-overflow-tooltip>
        <template
          slot-scope="{ row }">
          {{ getApprover(row.approverType, row.customApprovers) }}
        </template>
      </bk-table-column>
      <bk-table-column :label="$t('操作')" width="120">
        <template slot-scope="{ row }">
          <bk-popover :disabled="row.edit">
            <bk-button text @click="() => handleEdit(row)" :disabled="!row.edit">
              {{ $t('编辑') }}
            </bk-button>
            <div slot="content" v-if="row.disableEditReason === 'UNIFIED_CONFIG'">
              {{ $t('开源扫描/闭源扫描由公司运管统一配置忽略审批，不支持编辑和删除') }}
            </div>
            <div slot="content" v-if="row.disableEditReason === 'ONLY_PROJECT_MANAGER'">
              {{ $t('你还不是蓝盾项目管理员，不支持编辑和删除') }}
            </div>
          </bk-popover>

          <bk-popover :disabled="row.edit">
            <bk-button text @click="() => handleDelete(row)" :disabled="!row.edit">
              {{ $t('删除') }}
            </bk-button>
            <div slot="content" v-if="row.disableEditReason === 'UNIFIED_CONFIG'">
              {{ $t('开源扫描/闭源扫描由公司运管统一配置忽略审批，不支持编辑和删除') }}
            </div>
            <div slot="content" v-if="row.disableEditReason === 'ONLY_PROJECT_MANAGER'">
              {{ $t('你还不是蓝盾项目管理员，不支持编辑和删除') }}
            </div>
          </bk-popover>
        </template>
      </bk-table-column>
      <div slot="empty">
        <div class="codecc-table-empty-text">
          <img src="../../images/empty.png" class="empty-img" />
          <div>{{ $t('暂无数据') }}</div>
        </div>
      </div>
    </bk-table>
  </div>
</template>
<script>
import { getTaskList } from '@/common/preload';
import { format } from 'date-fns';
import { mapState } from 'vuex';

export default {
  data() {
    return {
      isLoading: false,
      countLoading: true,
      data: [],
      pagination: {
        current: 1,
        count: 0,
        limit: 10,
        limitList: [10, 20, 30],
      },
      dimensionsMap: {
        DEFECT: this.$t('代码缺陷'),
        SECURITY: this.$t('安全漏洞'),
        STANDARD: this.$t('代码规范'),
      },
      defectSeverityMap: {
        1: this.$t('严重'),
        2: this.$t('一般'),
        4: this.$t('提示'),
      },
      typeListMap: [],
      approverList: [],
      isIgnoreTypeListReady: false,
    };
  },
  computed: {
    ...mapState(['isProjectManager', 'isRbac']),
    projectId() {
      return this.$route.params.projectId;
    },
    ...mapState('task', {
      taskList: 'list',
    }),
  },
  mounted() {
    getTaskList();
    this.handleFetchList();
    this.handleFetchIgnoreType();
    this.handleFetchApprover();
  },
  methods: {
    formatDate(date) {
      if (!date) return '--';
      return format(date, 'yyyy-MM-dd HH:mm:ss');
    },
    handleCreate() {
      this.$router.push({
        name: 'approvalOperation',
      });
    },
    handleDelete(row) {
      const { entityId, name } = row;
      const h = this.$createElement;
      this.$bkInfo({
        theme: 'danger',
        title: this.$t('确认删除该配置？'),
        subHeader: h('div', { style: { 'text-align': 'center' } }, [
          h('span', { class: 'ignore-del-approval-config' }, this.$t('审批配置：')),
          h('span', { class: 'ignore-del-approval-name' }, name),
        ]),
        okText: this.$t('删除'),
        confirmFn: async () => {
          await this.$store
            .dispatch('ignore/deleteIgnoreApproval', entityId)
            .then((res) => {
              const msg = res.data
                ? this.$t('忽略审批删除成功')
                : this.$t('忽略审批无法删除');
              const theme = res.data ? 'success' : 'error';
              this.$bkMessage({
                message: msg,
                theme,
              });
              this.handleFetchList();
            });
        },
      });
    },
    handleFetchList() {
      this.isLoading = true;
      const params = {
        pageNum: this.pagination.current,
        pageSize: this.pagination.limit,
      };
      this.$store
        .dispatch('ignore/fetchIgnoreApprovalList', params)
        .then((res) => {
          this.data = res.data.records;
          this.pagination.count = res.data.count;
        })
        .finally(() => {
          this.isLoading = false;
        });
    },
    handleEdit(row) {
      this.$router.push({
        name: 'approvalOperation',
        params: {
          entityId: row.entityId,
        },
      });
    },
    handlePageChange(currentPage) {
      this.pagination.current = currentPage;
      this.handleFetchList();
    },
    handlePageLimitChange(pageSize) {
      this.pagination.limit = pageSize;
      this.handleFetchList();
    },
    batchDataShow(array, map, splitSign = ', ') {
      return array
        .filter(item => item in map)
        .map(item => map[item])
        .join(splitSign) || '--';
    },
    handleFetchIgnoreType() {
      this.$store.dispatch('ignore/fetchIgnoreList').then((res) => {
        res.data.forEach(item => this.typeListMap[item.ignoreTypeId] = item.name);
        this.isIgnoreTypeListReady = true;
      });
    },
    handleFetchApprover() {
      this.$store.dispatch('ignore/fetchApproverList').then((res) => {
        this.approverList = res?.data?.APPROVER_TYPE;
      });
    },
    getTaskScopes(taskScopeType, taskScopeList = []) {
      if (taskScopeType === 'ALL') {
        return this.$t('项目所有任务');
      }
      if (taskScopeType === 'INCLUDE') {
        const list = taskScopeList.reduce((acc, item) => {
          const foundItem = this.taskList?.enableTasks.find(task => task.taskId === item);
          if (foundItem) {
            acc.push(foundItem.nameCn);
          }
          return acc;
        }, []);
        return `${this.$t('包含以下任务')} : ${list.join(', ')}`;
      }
      if (taskScopeType === 'EXCLUDE') {
        const list = taskScopeList.reduce((acc, item) => {
          const foundItem = this.taskList?.enableTasks.find(task => task.taskId === item);
          if (foundItem) {
            acc.push(foundItem.nameCn);
          }
          return acc;
        }, []);
        return `${this.$t('排除以下任务')} : ${list.join(', ')}`;
      }
      return '--';
    },
    getApprover(approverType, customApprovers) {
      if (approverType !== 'CUSTOM_APPROVER') {
        const approver = this.approverList.find(item => item.key === approverType);
        return approver ? approver.name : '--';
      }
      return customApprovers.join(', ');
    },
  },
};
</script>

<style lang="postcss" scoped>
.ignore-title {
  font-size: 16px;
  font-weight: bold;
  color: #63656e;
}

.title-tips {
  margin-left: 18px;
  font-size: 12px;
  color: #c4c6cc;
}

.ignore-table {
  margin-top: 20px;
}

.ignore-del-approval-config {
  font-size: 14px;
  line-height: 22px;
  color: #63656E;
}

.ignore-del-approval-name {
  font-size: 14px;
  line-height: 22px;
  color: #313238;
}
</style>
