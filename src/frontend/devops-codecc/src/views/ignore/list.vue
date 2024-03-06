<template>
  <div class="cc-ignore main-content-outer main-content-ignore">
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
        {{ $t('新增忽略类型') }}
      </bk-button>
      <span
        v-else
        v-bk-tooltips="{
          content: $t('仅项目管理员或CI管理员可新增忽略类型'),
          disabled: hasPermission,
        }"
      >
        <bk-button
          icon="plus"
          theme="primary"
          :disabled="!hasPermission"
          @click="handleCreate"
        >
          {{ $t('新增忽略类型') }}
        </bk-button>
      </span>
      <span class="title-tips">{{
        $t('配置忽略类型，可定期提醒重新 review 相应忽略问题')
      }}</span>
      <bk-table v-bkloading="{ isLoading }" class="ignore-table" :data="data">
        <bk-table-column
          :label="$t('忽略类型')"
          prop="name"
          show-overflow-tooltip
        ></bk-table-column>
        <!-- <bk-table-column :label="$t('任务')">
          <template slot-scope="{ row }">
            {{ row.taskCount !== null && row.taskCount !== undefined ? row.taskCount : '--' }}
          </template>
        </bk-table-column> -->
        <bk-table-column :label="$t('问题数量')">
          <template slot-scope="{ row }">
            <bk-button
              text
              @click="goToDefect('project-defect-list', row.ignoreTypeId)"
            >{{
              row.defect !== null && row.defect !== undefined
                ? row.defect
                : '--'
            }}</bk-button
            >
          </template>
        </bk-table-column>
        <bk-table-column :label="$t('风险函数数量')">
          <template slot-scope="{ row }">
            <bk-button
              text
              @click="goToDefect('project-ccn-list', row.ignoreTypeId)"
            >{{
              row.riskFunction !== null && row.riskFunction !== undefined
                ? row.riskFunction
                : '--'
            }}</bk-button
            >
          </template>
        </bk-table-column>
        <bk-table-column :label="$t('提醒日期')" show-overflow-tooltip>
          <template slot-scope="{ row }">
            {{ handleGetRemindDateStr(row.notify) }}
          </template>
        </bk-table-column>
        <bk-table-column :label="$t('通知人角色')" show-overflow-tooltip>
          <template slot-scope="{ row }">
            {{
              (row.receiverTypesStr && row.receiverTypesStr.join('、')) || '--'
            }}
          </template>
        </bk-table-column>
        <bk-table-column :label="$t('通知方式')" show-overflow-tooltip>
          <template slot-scope="{ row }">
            {{ (row.notifyTypesStr && row.notifyTypesStr.join('、')) || '--' }}
          </template>
        </bk-table-column>
        <bk-table-column :label="$t('操作')" width="120">
          <template slot-scope="{ row }">
            <span
              class="mr10"
              v-bk-tooltips="{
                content: $t('仅项目管理员或CI管理员可编辑忽略类型'),
                disabled: row.edit,
              }"
            >
              <bk-button text @click="handleEdit(row)" :disabled="!row.edit">{{
                $t('编辑')
              }}</bk-button>
            </span>
            <span
              v-bk-tooltips="{
                content:
                  row.createFrom === 'sys'
                    ? $t('系统内置忽略配置不可删除')
                    : $t('仅项目管理员或CI管理员可删除忽略类型'),
                disabled: row.createFrom !== 'sys' && row.edit,
              }"
            >
              <bk-button
                text
                @click="handleDelete(row)"
                :disabled="
                  countLoading || row.createFrom === 'sys' || !row.edit
                "
              >{{ $t('删除') }}</bk-button
              >
            </span>
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
  </div>
</template>
<script>
import { mapState } from 'vuex';
export default {
  data() {
    return {
      isLoading: false,
      countLoading: true,
      hasPermission: false,
      data: [],
      notifyReceiverTypesMap: {
        ignore_author: this.$t('问题忽略人'),
        defect_author: this.$t('问题处理人'),
        task_creator: this.$t('任务创建人'),
      },
      notifyTypesMap: {
        rtx: this.$t('企业微信'),
        email: this.$t('邮件'),
      },
      monthsStrMap: {
        1: this.$t('一月'),
        2: this.$t('二月'),
        3: this.$t('三月'),
        4: this.$t('四月'),
        5: this.$t('五月'),
        6: this.$t('六月'),
        7: this.$t('七月'),
        8: this.$t('八月'),
        9: this.$t('九月'),
        10: this.$t('十月'),
        11: this.$t('十一月'),
        12: this.$t('十二月'),
      },
      weekOfMonthsStrMap: {
        1: this.$t('第一个星期'),
        2: this.$t('第二个星期'),
        3: this.$t('第三个星期'),
        4: this.$t('第四个星期'),
        5: this.$t('第五个星期'),
      },
      dayOfWeekMap: {
        1: this.$t('星期一'),
        2: this.$t('星期二'),
        3: this.$t('星期三'),
        4: this.$t('星期四'),
        5: this.$t('星期五'),
        6: this.$t('星期六'),
        7: this.$t('星期日'),
      },
    };
  },
  computed: {
    ...mapState(['isRbac']),
    projectId() {
      return this.$route.params.projectId;
    },
  },
  mounted() {
    this.handelFetchList();
    this.getIgnorePermission();
  },
  methods: {
    handelFetchList() {
      this.isLoading = true;
      this.$store
        .dispatch('ignore/fetchIgnoreList')
        .then((res) => {
          this.handelFetchListCount();
          this.data = res.data;
          this.data.forEach((i) => {
            i.receiverTypesStr = i.notify.notifyReceiverTypes
              && i.notify.notifyReceiverTypes.map(i => this.notifyReceiverTypesMap[i]);
            i.notifyTypesStr = i.notify.notifyTypes
              && i.notify.notifyTypes.map(i => this.notifyTypesMap[i]);
          });
        })
        .finally(() => {
          this.isLoading = false;
        });
    },
    getIgnorePermission() {
      this.$store.dispatch('ignore/getIgnorePermission').then((res) => {
        this.hasPermission = res.data;
      });
    },
    handelFetchListCount() {
      this.countLoading = true;
      this.$store.dispatch('ignore/fetchIgnoreListCount').then((res) => {
        const list = res.data || [];
        this.data = this.data.map((item) => {
          const { ignoreTypeId } = item;
          const ignoreItem = list.find(i => i.ignoreTypeId === ignoreTypeId);
          return { ...item, ...ignoreItem };
        });
        this.countLoading = false;
      });
    },
    goToDefect(name, id) {
      this.$router.push({
        name,
        query: {
          ignoreTypeId: `${id}`,
          status: 4,
        },
      });
    },
    handleCreate() {
      this.$router.push({
        name: 'ignoreOperation',
      });
    },
    handleEdit(row) {
      this.$router.push({
        name: 'ignoreOperation',
        params: {
          entityId: row.entityId,
          id: row.ignoreTypeId,
          isSysIgnore: row.createFrom === 'sys',
        },
      });
    },
    handleDelete(row) {
      const { entityId, name, taskCount } = row;
      if (taskCount) {
        this.$bkMessage({
          theme: 'error',
          message: '当前忽略类型问题或风险函数数量大于0，无法被删除。',
        });
        return;
      }
      const params = {
        entityId,
        status: 1,
      };
      this.$bkInfo({
        title: this.$t('删除忽略类型'),
        subTitle: `${this.$t('删除【x】后，本忽略类型将无法使用', [name])}`,
        confirmLoading: true,
        confirmFn: async () => {
          await this.$store
            .dispatch('ignore/deleteIgnore', params)
            .then((res) => {
              const msg = res.data
                ? this.$t('忽略类型删除成功')
                : this.$t('忽略类型无法删除');
              const theme = res.data ? 'success' : 'error';
              this.$bkMessage({
                message: msg,
                theme,
              });
              this.handelFetchList();
            });
        },
      });
    },
    handleGetRemindDateStr(notify) {
      let str = '--';
      if (notify.notifyDayOfWeeks && notify.notifyDayOfWeeks.length) {
        const {
          notifyMonths,
          notifyWeekOfMonths,
          notifyDayOfWeeks,
          everyMonth,
          everyWeek,
        } = notify;
        const monthsTextMap = notifyMonths.map(i => this.monthsStrMap[i]);
        const weekOfMonthTextMap = notifyWeekOfMonths.map(i => this.weekOfMonthsStrMap[i]);
        const dayTextMap = notifyDayOfWeeks.map(i => this.dayOfWeekMap[i]);
        if (everyMonth && everyWeek && notifyDayOfWeeks.length === 7) {
          str = this.$t('每天');
        } else if (everyMonth && everyWeek && notifyDayOfWeeks.length) {
          str = this.$t('每个月的每周的') + dayTextMap.join('、');
        } else if (
          everyMonth
          && notifyWeekOfMonths.length
          && notifyDayOfWeeks.length
        ) {
          str = this.$t('每个月的')
            + weekOfMonthTextMap.join('、')
            + this.$t('的')
            + dayTextMap.join('、');
        } else if (
          notifyMonths.length
          && everyWeek
          && notifyDayOfWeeks.length
        ) {
          str = this.$t('每年的')
            + monthsTextMap.join('、')
            + this.$t('每周的')
            + dayTextMap.join('、');
        } else if (
          notifyMonths.length
          && notifyWeekOfMonths.length
          && notifyDayOfWeeks.length
        ) {
          str = this.$t('每年的')
            + monthsTextMap.join('、')
            + this.$t('的')
            + weekOfMonthTextMap.join('、')
            + this.$t('的')
            + dayTextMap.join('、');
        }
      }
      return str;
    },
  },
};
</script>

<style lang="postcss" scoped>
.main-content-outer {
  width: 1236px;

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
}
</style>
