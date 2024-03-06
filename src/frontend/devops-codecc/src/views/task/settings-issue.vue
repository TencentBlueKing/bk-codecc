<template>
  <div
    class="main-content-inner main-content-form"
    :class="editDisabled ? 'from-pipeline' : ''"
  >
    <div v-if="editDisabled" class="f14" :model="issueData">
      <div
        v-if="taskDetail.createFrom === 'api_trigger'"
        class="fs12 to-pipeline"
      >
        {{ $t('问题提单配置由CodeCC开源扫描集群自动生成') }}
      </div>
      <div v-else class="to-pipeline">
        <span
        >{{ $t('修改问题提单配置，请前往流水线') }}
          <a @click="handleToPipeline" href="javascript:;">{{
            $t('立即前往>>')
          }}</a></span
        >
      </div>
      <div class="pipeline-item">
        <span class="pipeline-label">{{ $t('提单类型') }}</span>
        <span>{{ issueData.system }}</span>
      </div>
      <div class="pipeline-item">
        <span class="pipeline-label">{{ $t('Tapd库') }}</span>
        <span>{{ issueData.subSystemCn }}</span>
      </div>
      <div class="pipeline-item">
        <span class="pipeline-label">{{ $t('缺陷处理人') }}</span>
        <span>{{ issueData.resolvers && issueData.resolvers.join(',') }}</span>
      </div>
      <div class="pipeline-item">
        <span class="pipeline-label">{{ $t('缺陷抄送人') }}</span>
        <span>{{ issueData.receivers && issueData.receivers.join(',') }}</span>
      </div>
      <div class="pipeline-item">
        <span class="pipeline-label">{{ $t('发现版本') }}</span>
        <span>{{ issueData.findByVersion }}</span>
      </div>
      <div class="pipeline-item">
        <span class="pipeline-label">{{ $t('提单上限') }}</span>
        <span>{{ issueData.maxIssue }}</span>
      </div>
      <div class="pipeline-item">
        <span class="pipeline-label">{{ $t('自动提单') }}</span>
        <span>{{ issueData.autoCommit ? $t('是') : $t('否') }}</span>
      </div>
      <template v-if="issueData.autoCommit">
        <div class="pipeline-item">
          <span class="pipeline-label">{{ $t('工具') }}</span>
          <span>{{ handleTools(issueData.tools) }}</span>
        </div>
        <div class="pipeline-item">
          <span class="pipeline-label">{{ $t('级别') }}</span>
          <span>{{ handleSeverity(issueData.severities) }}</span>
        </div>
      </template>
    </div>
    <div v-else>
      <bk-form :label-width="130" :model="issueData" ref="issueInfo">
        <bk-form-item
          :label="$t('提单类型')"
          property="system"
          :required="true"
          :rules="rules.required"
        >
          <bk-radio-group v-model="issueData.system">
            <bk-radio
              v-for="item in issueData.issueSystemInfoVOList || []"
              :key="item.system"
              :value="item.system"
            >{{ item.detail }}</bk-radio
            >
          </bk-radio-group>
        </bk-form-item>
        <bk-form-item
          :label="$t('Tapd库')"
          property="subSystem"
          :required="true"
          :rules="rules.required"
        >
          <bk-popover
            v-if="issueData.subSystemId"
            placement="right"
            theme="light"
          >
            <bk-link theme="primary">{{ issueData.subSystemCn }}</bk-link>
            <div slot="content">
              <bk-link theme="primary" @click="jumpTo('tapd')">{{
                $t('去Tapd')
              }}</bk-link>
              <span class="split"></span>
              <bk-link theme="primary" @click="jumpTo('change')">{{
                $t('切换授权')
              }}</bk-link>
            </div>
          </bk-popover>
          <bk-button v-else @click="jumpTo('change')">{{
            $t('授权OAuth')
          }}</bk-button>
        </bk-form-item>
        <bk-form-item :label="$t('缺陷处理人')" property="resolvers">
          <bk-tag-input allow-create
            v-model="issueData.resolvers"
            :placeholder="$t('默认为问题处理人')"
          ></bk-tag-input>
        </bk-form-item>
        <bk-form-item :label="$t('缺陷抄送人')">
          <bk-tag-input allow-create
            v-model="issueData.receivers"
          ></bk-tag-input>
        </bk-form-item>
        <bk-form-item :label="$t('发现版本')">
          <bk-input v-model="issueData.findByVersion"></bk-input>
        </bk-form-item>
        <bk-form-item
          :label="$t('提单上限')"
          :desc="
            $t(
              '若Tapd库中待修复问题数超过提单上限，将不再提单。最大上限为10000条。'
            )
          "
        >
          <bk-input
            v-model="issueData.maxIssue"
            type="number"
            :min="0"
            :max="10000"
            :placeholder="$t('默认为1000条')"
          ></bk-input>
        </bk-form-item>
        <bk-form-item :label="$t('自动提单')">
          <bk-switcher v-model="issueData.autoCommit"></bk-switcher>
        </bk-form-item>
        <template v-if="issueData.autoCommit">
          <bk-form-item :label="$t('工具')">
            <bk-select v-model="issueData.tools" multiple>
              <bk-option
                v-for="option in toolList"
                :key="option.toolName"
                :id="option.toolName"
                :name="option.toolDisplayName"
              >
              </bk-option>
            </bk-select>
          </bk-form-item>
          <bk-form-item :label="$t('级别')">
            <bk-select v-model="issueData.severities" multiple>
              <bk-option
                v-for="option in severityList"
                :key="option.id"
                :id="option.id"
                :name="option.name"
              >
              </bk-option>
            </bk-select>
          </bk-form-item>
        </template>
        <bk-form-item>
          <bk-button
            v-if="isRbac === true"
            key="save"
            v-perm="{
              hasPermission: true,
              disablePermissionApi: false,
              permissionData: {
                projectId: projectId,
                resourceType: 'codecc_task',
                resourceCode: taskId,
                action: 'codecc_task_setting',
              },
            }"
            theme="primary"
            @click="save"
          >
            {{ $t('保存') }}
          </bk-button>
          <bk-button v-else theme="primary" @click="save">{{
            $t('保存')
          }}</bk-button>
        </bk-form-item>
      </bk-form>
    </div>
  </div>
</template>

<script>
import { mapState } from 'vuex';

export default {
  components: {},
  data() {
    return {
      rules: {
        required: [
          {
            required: true,
            message: this.$t('必填项'),
            trigger: 'blur',
          },
        ],
      },
      severityList: [
        {
          id: 1,
          name: '严重',
        },
        {
          id: 2,
          name: '一般',
        },
        {
          id: 4,
          name: '提示',
        },
      ],
      issueTypeList: [{}],
      issueData: {
        issueSystemInfoVOList: [],
      },
    };
  },
  computed: {
    ...mapState(['toolMeta', 'isRbac']),
    ...mapState('tool', {
      toolMap: 'mapList',
    }),
    ...mapState('task', {
      taskDetail: 'detail',
    }),
    toolList() {
      return this.taskDetail.enableToolList.filter(item => item.toolName !== 'CLOC'
          && item.toolName !== 'STAT'
          && item.toolName !== 'SCC'
          && item.toolName !== 'DUPC');
    },
    taskId() {
      return this.$route.params.taskId;
    },
    projectId() {
      return this.$route.params.projectId;
    },
    editDisabled() {
      // 最老的v1插件atomCode为空，但createFrom === 'bs_pipeline', 也可编辑
      return (
        (this.taskDetail.atomCode
          && this.taskDetail.createFrom === 'bs_pipeline')
        || this.taskDetail.createFrom === 'api_trigger'
      );
    },
  },
  watch: {},
  created() {
    this.init();
  },
  methods: {
    async init() {
      const params = { taskId: this.taskId };
      const issueInfo = (await this.$store.dispatch('task/taskIssue', params)) || {};
      this.issueData = issueInfo;
    },
    handleToPipeline() {
      if (/^git_\d+$/.test(this.projectId)) {
        window.open(
          `${window.STREAM_SITE_URL}/pipeline/${this.taskDetail.pipelineId}
#${this.taskDetail.projectName}`,
          '_blank',
        );
      } else {
        window.open(
          `${window.DEVOPS_SITE_URL}/console/pipeline/${this.taskDetail.projectId}/
${this.taskDetail.pipelineId}/edit#${this.taskDetail.atomCode}`,
          '_blank',
        );
      }
    },
    async save() {
      const payload = { taskId: this.taskId, ...this.issueData };
      const res = await this.$store.dispatch('task/updateTaskIssue', payload);
      if (res.code === '0') {
        this.$bkMessage({ theme: 'success', message: this.$t('保存成功') });
      } else {
        this.$bkMessage({
          theme: 'error',
          message: res.message || this.$t('保存失败'),
        });
      }
    },
    jumpTo(to) {
      const list = this.issueData.issueSystemInfoVOList || [];
      const info = list.find(item => item.system === this.issueData.system);
      if (info) {
        if (to === 'tapd') {
          window.open(info.homeUrl);
        } else if (to === 'change') {
          window.open(info.oauthUrl);
        }
      }
    },
    handleSeverity(severity = []) {
      const list = this.severityList
        .filter(item => severity.includes(item.id))
        .map(item => item.name);
      return list.join(', ');
    },
    handleTools(tools = []) {
      const list = this.toolList
        .filter(item => tools.includes(item.toolName))
        .map(item => item.toolDisplayName);
      return list.join(', ');
    },
  },
};
</script>

<style lang="postcss" scoped>
.split {
  padding: 0 8px;

  &::after {
    color: #e1ecff;
    content: '|';
  }
}

.from-pipeline {
  padding: 0 35px 0 20px;

  .to-pipeline {
    height: 32px;
    margin-bottom: 20px;
    font-size: 12px;
    border-bottom: 1px solid #e3e3e3;

    a {
      margin-left: 12px;
    }
  }

  .pipeline-item {
    padding-bottom: 22px;
  }

  .pipeline-label {
    display: inline-block;
    width: 104px;
    font-size: 14px;
    font-weight: 600;
    text-align: left;
  }
}
</style>
