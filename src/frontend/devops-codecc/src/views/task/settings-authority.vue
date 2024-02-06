<template>
  <div class="authority">
    <div v-if="taskDetail.createFrom === 'gongfeng_scan'">
      <div class="no-authority">
        <div class="desc">
          {{ $t('该任务与工蜂仓库权限保持同步') }}
        </div>
        <bk-button size="large" theme="primary" @click="handleToGongFeng()">{{
          $t('前往工蜂成员管理')
        }}</bk-button>
      </div>
    </div>
    <div v-else-if="taskDetail.createFrom === 'bs_pipeline'">
      <div class="no-authority">
        <div class="desc">
          {{ $t('该任务与流水线权限保持同步') }}
        </div>
        <bk-button size="large" theme="primary" @click="handleToPipeline()">{{
          $t('前往流水线权限管理')
        }}</bk-button>
      </div>
    </div>
    <bk-permission
      v-else-if="isRbac === true"
      class="bk-permission"
      :resource-type="'codecc_task'"
      :resource-code="taskId"
      :resource-name="taskDetail.nameCn"
      :project-code="projectId"
      :show-create-group="false"
      :ajax-prefix="ajaxPrefix"
    />
    <bk-tab
      v-else
      class="settings-authority-tab"
      :label-height="42"
      :active.sync="active"
      type="unborder-card"
      @tab-change="changeTab"
    >
      <div v-if="!pipelineCondition">
        <bk-tab-panel
          v-for="(panel, index) in codeccPanels"
          v-bind="panel"
          :key="index"
        >
        </bk-tab-panel>
      </div>
      <div v-if="pipelineCondition">
        <bk-tab-panel
          v-for="(panel, index) in linePanels"
          v-bind="panel"
          :key="index"
        >
        </bk-tab-panel>
      </div>
      <div v-if="pipelineCondition" class="header-tab-right">
        {{ $t('权限与创建该任务的流水线保持一致') }}
      </div>
      <div>
        <div v-show="tabSelect === 'role'">
          <bk-collapse
            class="collapse"
            v-for="(item, index) in roleList"
            :key="index"
            v-model="roleActive"
          >
            <bk-collapse-item :name="item.role_code">
              <div>
                <b class="item-title">{{
                  $t(`${changeName(item.role_name)}`)
                }}</b>
                <span v-if="item.role_code === 'manager'">{{
                  $t('(执行检查，问题管理，查看问题，查看报表，任务设置)')
                }}</span>
                <span v-else-if="item.role_code === 'viewer'">{{
                  $t('(查看问题，查看报表)')
                }}</span>
                <span v-else-if="item.role_code === 'member' || 'executor'">{{
                  $t('(执行检查，问题管理，查看问题，查看报表)')
                }}</span>
              </div>
              <bk-form slot="content">
                <bk-form-item :label-width="80" :label="$t('用户')">
                  <bk-select v-model="item.user_list" multiple searchable>
                    <bk-option
                      v-for="users in userList"
                      :key="users"
                      :id="users"
                      :name="users"
                    >
                    </bk-option>
                  </bk-select>
                  <div class="setting-extra" v-if="item.extra_user_list.length">
                    <bk-popover
                      placement="bottom"
                      :content="item.extra_user_list.join(',')"
                    >
                      <p class="setting-extra-list">
                        {{ $t('附加：') }}{{ item.extra_user_list.join(',') }}
                      </p>
                    </bk-popover>
                    <bk-popover
                      class="setting-extra-tip"
                      placement="bottom-end"
                      width="260"
                      :content="
                        $t(
                          '附加人员或组是拥有你项目下任意流水线相关权限的人员或组，由项目管理员授权，如需移除，请联系你的项目管理员。'
                        )
                      "
                    >
                      <p>{{ $t('为什么会有附加人员或组？') }}</p>
                    </bk-popover>
                  </div>
                </bk-form-item>
                <bk-form-item :label-width="80" :label="$t('用户组')">
                  <bk-select v-model="item.group_list" multiple searchable>
                    <bk-option
                      v-for="groupName in userGroupList"
                      :key="groupName"
                      :id="groupName.groupRoleId"
                      :name="groupName.groupName"
                    >
                    </bk-option>
                  </bk-select>
                  <div
                    class="setting-extra"
                    v-if="
                      item.extra_group_list
                        .map((item) => item.group_name)
                        .join().length
                    "
                  >
                    <bk-popover
                      placement="bottom"
                      :content="
                        item.extra_group_list
                          .map((item) => item.group_name)
                          .join(',')
                      "
                    >
                      <p class="setting-extra-list">
                        {{ $t('附加：')
                        }}{{
                          item.extra_group_list
                            .map((item) => item.group_name)
                            .join(',')
                        }}
                      </p>
                    </bk-popover>
                  </div>
                </bk-form-item>
              </bk-form>
            </bk-collapse-item>
          </bk-collapse>
          <bk-button
            theme="primary"
            class="mt10"
            :disabled="isDisabled"
            @click="saveUserSetting"
          >{{ $t('保存') }}</bk-button
          >
        </div>
        <div v-show="tabSelect === 'function'">
          <bk-collapse
            class="collapse"
            v-for="item in policyList"
            :key="item"
            v-model="funcActive"
          >
            <bk-collapse-item :name="item.policy_code">
              <div>
                <b class="item-title">{{ $t(`${item.policy_name}`) }}</b>
                <span v-if="item.policy_code === 'analyze'">{{
                  $t('(触发立即检查)')
                }}</span>
                <span v-else-if="item.policy_code === 'defect_manage'">{{
                  $t('(忽略问题、标记问题、修改处理人)')
                }}</span>
                <span v-else-if="item.policy_code === 'defect_view'">{{
                  $t('(查看问题列表、查看代码片段)')
                }}</span>
                <span v-else-if="item.policy_code === 'report_view'">{{
                  $t('(查看数据报表)')
                }}</span>
                <span v-else-if="item.policy_code === 'task_manage'">{{
                  $t(
                    '(基础信息修改、规则集配置、通知报告、扫描触发、路径屏蔽、人员权限、任务管理)'
                  )
                }}</span>
              </div>
              <bk-form slot="content">
                <bk-form-item :label-width="80" :label="$t('用户')">
                  <bk-select v-model="item.user_list" multiple searchable>
                    <bk-option
                      v-for="users in userList"
                      :key="users"
                      :id="users"
                      :name="users"
                    >
                    </bk-option>
                  </bk-select>
                  <div class="setting-extra" v-if="item.extra_user_list.length">
                    <bk-popover
                      placement="bottom"
                      :content="item.extra_user_list.join(',')"
                    >
                      <p class="setting-extra-list">
                        {{ $t('附加：') }}{{ item.extra_user_list.join(',') }}
                      </p>
                    </bk-popover>
                    <bk-popover
                      class="setting-extra-tip"
                      placement="bottom-end"
                      width="260"
                      :content="
                        $t(
                          '附加人员或组是拥有你项目下任意流水线相关权限的人员或组，由项目管理员授权，如需移除，请联系你的项目管理员。'
                        )
                      "
                    >
                      <p>{{ $t('为什么会有附加人员或组？') }}</p>
                    </bk-popover>
                  </div>
                </bk-form-item>
                <bk-form-item :label-width="80" :label="$t('用户组')">
                  <bk-select v-model="item.group_list" searchable multiple>
                    <bk-option
                      v-for="groupName in userGroupList"
                      :key="groupName"
                      :id="groupName.groupRoleId"
                      :name="groupName.groupName"
                    >
                    </bk-option>
                  </bk-select>
                  <div
                    class="setting-extra"
                    v-if="
                      item.extra_group_list
                        .map((item) => item.group_name)
                        .join().length
                    "
                  >
                    <bk-popover
                      placement="bottom"
                      :content="
                        item.extra_group_list
                          .map((item) => item.group_name)
                          .join(',')
                      "
                    >
                      <p class="setting-extra-list">
                        {{ $t('附加：')
                        }}{{
                          item.extra_group_list
                            .map((item) => item.group_name)
                            .join(',')
                        }}
                      </p>
                    </bk-popover>
                  </div>
                </bk-form-item>
              </bk-form>
            </bk-collapse-item>
          </bk-collapse>
          <bk-button
            theme="primary"
            class="mt10"
            :disabled="isDisabled"
            @click="saveUserSetting"
          >{{ $t('保存') }}</bk-button
          >
        </div>
      </div>
    </bk-tab>
  </div>
</template>
<script>
import axios from 'axios';
import cookie from 'cookie';
import { mapState } from 'vuex';
import { BkPermission } from 'bk-permission';

export default {
  components: {
    BkPermission,
  },
  data() {
    return {
      codeccPanels: [
        { name: 'role', label: this.$t('按角色') },
        { name: 'function', label: this.$t('按功能') },
      ],
      linePanels: [{ name: 'role', label: this.$t('按角色') }],
      active: 'role',
      tabSelect: 'role',
      funcActive: 'analyze',
      params: {},
      roleList: [],
      policyList: [],
      authList: [],
      userList: [],
      userGroupList: [],
      pipelineList: [],
      ajaxPrefix: window.DEVOPS_SITE_URL,
    };
  },
  computed: {
    ...mapState('task', {
      taskDetail: 'detail',
      status: 'status',
      codes: 'codes',
    }),
    ...mapState(['isRbac']),
    taskId() {
      return this.$route.params.taskId;
    },
    projectId() {
      return this.$route.params.projectId;
    },
    pipelineId() {
      return this.taskDetail.pipelineId;
    },
    pipelineCondition() {
      return this.taskDetail.createFrom === 'bs_pipeline';
    },
    isDisabled() {
      return this.status.gongfengProjectId;
    },
    roleActive() {
      if (this.pipelineCondition) {
        return ['manager', 'viewer', 'executor'];
      }
      return ['manager', 'viewer', 'member'];
    },
  },
  created() {
    if (this.isRbac !== true) {
      this.init();
    }
  },
  methods: {
    changeTab(name) {
      this.tabSelect = name;
    },
    init() {
      if (this.taskDetail.createFrom) {
        if (this.taskDetail.createFrom !== 'gongfeng_scan') {
          this.$store.dispatch('task/getCodeMessage');
          this.getCodeccList();
        }
      } else {
        setTimeout(() => this.init(), 500);
      }
    },
    getCodeccList() {
      axios
        .get(
          `${window.DEVOPS_API_URL}/project/api/user/projects/${this.projectId}`,
          {
            withCredentials: true,
            headers: { 'X-DEVOPS-PROJECT-ID': this.projectId },
          },
        )
        .then((res) => {
          this.params = this.pipelineCondition
            ? {
              projectId: res.data.data.projectId,
              pipelineId: this.pipelineId,
            }
            : { projectId: res.data.data.projectId, taskId: this.taskId };
          this.$store.commit('setMainContentLoading', true);
        })
        .finally(() => {
          this.pipelineCondition
            ? this.getPipeLineAuth()
            : this.getCodeccAuth();
          this.getUserList();
          this.getGroupName();
        });
    },
    getCodeccAuth() {
      axios
        .get(
          `${window.DEVOPS_API_URL}/backend/api/perm/service/codecc/mgr_resource/permission/
?project_id=${this.params.projectId}&resource_type_code=task&resource_code=${this.params.taskId}`,
          { withCredentials: true },
        )
        .then((res) => {
          this.authList = res.data;
          // 在刚取值的时候对用户组数据进行处理，用数值表示用户组组名
          this.roleList = res.data.data.role;
          this.roleList.forEach((role) => {
            role.group_list = role.group_list.map(grouplist => grouplist.group_id);
          });
          this.policyList = res.data.data.policy;
          this.policyList.forEach((policy) => {
            policy.group_list = policy.group_list.map(grouplist => grouplist.group_id);
          });
        })
        .finally(() => {
          this.$store.commit('setMainContentLoading', false);
        });
    },
    getUserList() {
      axios
        .get(
          `${window.DEVOPS_API_URL}/project/api/user/users/projects/${this.projectId}/list`,
          { withCredentials: true },
        )
        .then((res) => {
          this.userList = res.data.data;
        });
    },
    getGroupName() {
      axios
        .get(
          `${window.DEVOPS_API_URL}/experience/api/user/groups/${this.projectId}/projectGroupAndUsers`,
          { withCredentials: true },
        )
        .then((res) => {
          this.userGroupList = res.data.data;
        });
    },
    getPipeLineAuth() {
      axios
        .get(
          `${window.DEVOPS_API_URL}/backend/api/perm/service/pipeline/mgr_resource/permission/
?project_id=${this.params.projectId}&resource_type_code=pipeline&resource_code=${this.pipelineId}`,
          { withCredentials: true },
        )
        .then((res) => {
          this.pipelineList = res.data;
          this.roleList = res.data.data.role;
          // 前端调整一下流水线role权限数据顺序，和codecc权限样式保持一致
          if (this.roleList) {
            [this.roleList[0], this.roleList[1]] = this.roleList[0].role_code === 'executor'
              ? [this.roleList[1], this.roleList[0]]
              : [this.roleList[0], this.roleList[1]];
          }
          this.roleList.forEach((role) => {
            role.group_list = role.group_list.map(grouplist => grouplist.group_id);
          });
        })
        .finally(() => {
          this.$store.commit('setMainContentLoading', false);
        });
    },
    saveUserSetting() {
      const data = this.pipelineCondition
        ? {
          project_id: this.params.projectId,
          resource_type_code: 'pipeline',
          resource_code: this.pipelineId,
          role: this.roleList,
        }
        : {
          project_id: this.params.projectId,
          resource_type_code: 'task',
          resource_code: this.params.taskId,
          role: this.roleList,
          policy: this.policyList,
        };
      this.isDisabled = true;
      try {
        const url = this.pipelineCondition
          ? `${window.DEVOPS_API_URL}/backend/api/perm/service/pipeline/mgr_resource/permission/`
          : `${window.DEVOPS_API_URL}/backend/api/perm/service/codecc/mgr_resource/permission/`;
        axios
          .put(url, data, {
            withCredentials: true,
            headers: {
              'X-CSRFToken': cookie.parse(document.cookie).paas_perm_csrftoken,
            },
          })
          .then((res) => {
            if (res.data && res.data.code === 0) {
              this.saveOperationHistory();
              this.$bkMessage({
                message: this.$t('保存权限成功'),
                theme: 'success',
              });
            } else {
              this.$bkMessage({
                message: res.data.message || this.$t('保存权限失败'),
                theme: 'error',
              });
            }
          });
        this.handleUpdateMembers(data);
      } catch (err) {
        this.$showTips({
          message: err.message || err,
          theme: 'error',
        });
      }
      this.isDisabled = false;
    },
    // 保存操作记录
    async saveOperationHistory() {
      // 组装接口参数
      const params = this.getOperationHistoryParam(this.roleList.concat(this.policyList));
      await this.$store.dispatch('defect/saveSetAuthorityOperaHis', params);
    },
    // 格式化操作记录所需参数
    getOperationHistoryParam(dataList) {
      const params = [];
      dataList.forEach((i) => {
        const userGroupStr = `${i.user_list.join('、')} ${this.formatGroups(i.group_list)}`;
        params.push(userGroupStr);
      });
      return params;
    },
    formatGroups(groupList) {
      const list = [];
      groupList.forEach((id) => {
        this.userGroupList.forEach((userGroup) => {
          if (id === userGroup.groupRoleId) {
            list.push(userGroup.groupName);
          }
        });
      });
      return list.join('、');
    },
    changeName(name) {
      let roleName = name;
      switch (name) {
        case '成员':
          roleName = this.$t('开发人员');
          break;
        default:
          break;
      }
      return roleName;
    },
    handleUpdateMembers(data) {
      const { userGroupList } = this;
      let taskOwner = [];
      let taskMember = [];
      ['policy', 'role'].forEach((item) => {
        const list = data[item] || [];
        list.forEach((user) => {
          const { user_list, extra_user_list, group_list, extra_group_list } = user;
          const groupList = handleGroupMembers(group_list);
          const extraGroupList = handleGroupMembers(extra_group_list);
          if (user.role_code === 'manager') {
            taskOwner = Array.from(new Set(taskOwner
              .concat(user_list)
              .concat(extra_user_list)
              .concat(groupList)
              .concat(extraGroupList)));
          } else {
            taskMember = Array.from(new Set(taskMember
              .concat(user_list)
              .concat(extra_user_list)
              .concat(groupList)
              .concat(extraGroupList)));
          }
        });
      });
      function handleGroupMembers(group = []) {
        return group.reduce((pre, cur) => {
          const id = cur.group_id || cur;
          return pre.concat(userGroupList.find(user => user.groupRoleId === id).users || []);
        }, []);
      }
      this.$store.dispatch('task/updateMembers', {
        taskId: this.taskId,
        taskOwner,
        taskMember,
      });
    },
    handleToGongFeng() {
      const [repoInfo = {}] = this.codes.codeInfo || [];
      const repoUrl = repoInfo.url.replace('.git', '');
      window.open(`${repoUrl}/-/project_members`, '_blank');
    },
    handleToPipeline() {
      window.open(
        `${window.DEVOPS_SITE_URL}/console/pipeline/${this.taskDetail.projectId}/
${this.taskDetail.pipelineId}/edit/auth`,
        '_blank',
      );
    },
  },
};
</script>

<style lang="postcss" scoped>
@import url('../../css/variable.css');

.authority .bk-permission {
  border: 1px solid $borderColor;
}

.settings-authority-tab {
  margin: -17px -5px;
  border-top: 0;
}

.header-tab-right {
  position: relative;
  top: -50px;
  padding-right: 15px;
  margin-bottom: -16px;
  font-size: 12px;
  color: $fontLightColor;
  text-align: right;
}

.bk-collapse {
  :hover {
    color: #63656e;
  }

  .bk-collapse-item {
    padding-bottom: 10px;

    >>> .bk-collapse-item-header {
      font-size: 12px;
      background: $bgLightColor;
      border: 1px solid $borderColor;

      .item-title {
        margin-right: 15px;
        font-weight: 700;
      }
    }

    >>> .bk-collapse-item-content {
      padding: 20px 15px 15px;
      font-size: 12px;
      border: 1px solid $borderColor;
      border-top: 0;

      .setting-extra {
        margin: 5px 0 0;
        cursor: default;

        .setting-extra-tip {
          position: absolute;
          right: 0;
          color: #3c96ff;

          :hover {
            color: #3c96ff;
          }
        }
      }
    }
  }
}

.no-authority {
  padding-top: 150px;
  text-align: center;

  .desc {
    margin: 8px 0;
    font-size: 14px;
  }
}
</style>
