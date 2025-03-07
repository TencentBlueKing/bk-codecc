<template>
  <div class="manage-section">
    <section class="relate-pipeline" v-if="relatedPipelines.length">
      <div class="section-header">
        <span>{{ $t('此代码检查任务已被配置在以下流水线中。如需停用或删除，请先前往流水线更改相关任务配置。') }}</span>
        <bk-button class="refresh" text @click="getRelatedPipelines">
          <span class="codecc-icon icon-refresh-2"></span>
          {{ $t('刷新') }}
        </bk-button>
      </div>
      <div class="section-content" v-bkloading="{ isLoading: refreshLoading }">
        <div
          v-for="item in relatedPipelines"
          class="content-item"
          :key="item.pipelineId"
          @click="goToPipeline(item.projectId, item.pipelineId)">
          <span class="codecc-icon icon-link"></span>
          <span>{{ item.pipelineName }}</span>
        </div>
      </div>
    </section>
    <bk-form
      v-if="!isShow.status || isShow.status === 0"
      class="disable-form"
      :model="formData"
      form-type="vertical"
      ref="validateForm"
    >
      <div class="form-header">{{ $t('停用任务') }}</div>
      <bk-form-item>
        <bk-alert type="warning">
          <span slot="title">
            {{ $t('停用后将') }}
            <span class="text-warning">{{ $t('不再执行') }}</span>
            {{ $t('定时扫描，只在我的任务 > 已停用任务中出现') }}
          </span>
        </bk-alert>
      </bk-form-item>
      <bk-form-item
        class="mt10"
        required
        :rules="rules.desc"
        :property="'desc'"
        :label="$t('停用原因')"
      >
        <bk-input
          :placeholder="$t('请输入停用原因，不得少于10个字符')"
          :type="'textarea'"
          :rows="4"
          :maxlength="100"
          v-model="formData.desc"
        >
        </bk-input>
      </bk-form-item>
      <bk-form-item>
        <bk-button
          theme="primary"
          v-if="isRbac === true"
          key="disable"
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
          :title="
            projectId === 'CUSTOMPROJ_PCG_RD'
              ? $t('PCG EP度量平台自动生成任务无法停用')
              : $t('停用')
          "
          :disabled="projectId === 'CUSTOMPROJ_PCG_RD' || !!relatedPipelines.length"
          :loading="buttonLoading"
          @click.stop.prevent="disable"
        >
          {{ $t('停用') }}
        </bk-button>
        <bk-button
          v-else
          theme="primary"
          key="disable"
          :title="
            projectId === 'CUSTOMPROJ_PCG_RD'
              ? $t('PCG EP度量平台自动生成任务无法停用')
              : $t('停用')
          "
          :disabled="projectId === 'CUSTOMPROJ_PCG_RD' || !!relatedPipelines.length || formData.desc.length < 10"
          :loading="buttonLoading"
          @click.stop.prevent="disable"
        >
          {{ $t('停用') }}
        </bk-button>
      </bk-form-item>
    </bk-form>
    <bk-form form-type="vertical" v-else-if="isShow.status === 1">
      <bk-form-item :label="$t('启用任务')">
        <bk-alert type="gray" :show-icon="false">
          <span slot="title">
            <bk-icon class="minus-circle" type="minus-circle" />
            {{ $t('任务当前状态为已停用') }}
          </span>
        </bk-alert>
      </bk-form-item>
      <bk-form-item>
        <bk-button
          v-if="isCreateFromPipeline"
          theme="primary"
          @click="goToPipeline"
        >{{ $t('去流水线启用') }}</bk-button
        >
        <bk-button
          v-else
          theme="primary"
          :title="$t('启用')"
          :loading="buttonLoading"
          @click.stop.prevent="enable"
        >{{ $t('启用') }}</bk-button
        >
      </bk-form-item>
    </bk-form>

    <bk-form form-type="vertical" v-else>
      <bk-form-item :label="$t('停用任务')">
        <bk-alert type="gray" :show-icon="false">
          <span slot="title">
            <bk-icon class="minus-circle" type="minus-circle" />
            {{ $t('测试任务无法在此停用和删除') }},
            <a class="cursor-pointer" @click.prevent="handleToV3">{{ $t('请前往蓝鲸开发者中心') }}</a>
          </span>
        </bk-alert>
      </bk-form-item>
    </bk-form>

    <bk-divider></bk-divider>

    <bk-form form-type="vertical" v-if="isShow.status !== 3">
      <div class="form-header">{{ $t('删除任务') }}</div>
      <bk-form-item>
        <bk-alert type="warning">
          <span slot="title">
            {{ $t('删除的任务将') }}
            <span class="text-warning">{{ $t('无法找回') }}</span>
            {{ $t('，请谨慎操作！') }}
          </span>
        </bk-alert>
      </bk-form-item>
      <bk-form-item>
        <bk-button
          theme="danger"
          :outline="true"
          v-if="isRbac === true"
          key="delete"
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
          :title="
            projectId === 'CUSTOMPROJ_PCG_RD'
              ? $t('PCG EP度量平台自动生成任务无法删除')
              : $t('删除')
          "
          :disabled="projectId === 'CUSTOMPROJ_PCG_RD' || !!relatedPipelines.length"
          :loading="deleteButtonLoading"
          @click.stop.prevent="deleteVisible = true"
        >
          {{ $t('删除') }}
        </bk-button>
        <bk-button
          v-else
          theme="danger"
          key="delete"
          :outline="true"
          :title="
            projectId === 'CUSTOMPROJ_PCG_RD'
              ? $t('PCG EP度量平台自动生成任务无法删除')
              : $t('删除')
          "
          :disabled="projectId === 'CUSTOMPROJ_PCG_RD' || !!relatedPipelines.length"
          :loading="deleteButtonLoading"
          @click.stop.prevent="deleteVisible = true"
        >
          {{ $t('删除') }}
        </bk-button>
      </bk-form-item>
    </bk-form>

    <bk-dialog v-model="deleteVisible" width="450" :show-footer="false" class="delete-dialog">
      <div class="bk-icon icon-exclamation">
      </div>
      <div class="header">{{ $t('确定删除此任务？') }}</div>
      <div class="side-header">
        {{ $t('删除的任务将') }}
        <span class="text-danger">{{ $t('无法找回') }}</span>
        {{ $t('，请谨慎操作！') }}
      </div>
      <div class="content">
        <div class="task-name">
          {{ $t('请输入任务名') }}
          <span class="task-name-text">{{ taskName }}</span>
          {{ $t('以确认删除') }}
        </div>
        <bk-input v-model="taskNameInput"></bk-input>
      </div>
      <div class="footer">
        <bk-button
          theme="danger"
          :disabled="taskName !== taskNameInput"
          :loading="deleteLoading"
          @click="handleDelete">
          {{ $t('删除') }}
        </bk-button>
        <bk-button @click="deleteVisible = false">{{ $t('取消') }}</bk-button>
      </div>
    </bk-dialog>
  </div>
</template>
<script>
import { mapState } from 'vuex';
export default {
  data() {
    return {
      formData: {
        desc: '',
      },
      rules: {
        desc: [
          {
            required: true,
            message: this.$t('必填项'),
            trigger: 'blur',
          },
          {
            min: 10,
            message: this.$t('不能少于x个字符', { num: 10 }),
            trigger: 'blur',
          },
          {
            max: 200,
            message: this.$t('不能多于x个字符', { num: 200 }),
            trigger: 'blur',
          },
        ],
      },
      buttonLoading: false,
      deleteButtonLoading: false,
      deleteVisible: false,
      taskNameInput: '',
      deleteLoading: false,
      relatedPipelines: [],
      refreshLoading: false,
    };
  },
  computed: {
    ...mapState('task', {
      taskDetail: 'detail',
      isShow: 'status',
    }),
    ...mapState(['isRbac']),
    isCreateFromPipeline() {
      return this.taskDetail.createFrom === 'bs_pipeline';
    },
    projectId() {
      return this.$route.params.projectId;
    },
    taskId() {
      return this.$route.params.taskId;
    },
    taskName() {
      return this.taskDetail.nameCn;
    },
  },
  watch: {
    formData: {
      handler() {
        this.handleFormDataChange();
      },
      deep: true,
    },
  },
  mounted() {
    this.getRelatedPipelines();
  },
  methods: {
    handleFormDataChange() {
      window.changeAlert = true;
    },
    // 停用任务
    disable() {
      this.$refs.validateForm.validate().then(() => {
        this.buttonLoading = true;
        const params = {
          taskId: this.taskDetail.taskId,
          disableReason: this.formData.desc,
        };
        const data = this.isShow;
        this.$store
          .dispatch('task/stopManage', params)
          .then((res) => {
            if (res.data === true) {
              this.$bkMessage({
                theme: 'success',
                message: this.$t('停用任务成功'),
              });
              this.$nextTick(() => {
                window.changeAlert = false;
              });
            }
            this.$store.dispatch('task/status');
            this.formData = {
              desc: '',
            };
          })
          .catch((e) => {
            console.error(e);
          })
          .finally(() => {
            this.$store.dispatch('task/detail', data);
            this.buttonLoading = false;
          });
      });
    },
    // 启用任务
    enable() {
      this.$store
        .dispatch('task/startManage', this.taskDetail.taskId)
        .then((res) => {
          if (res.data === true) {
            this.$bkMessage({
              theme: 'success',
              message: this.$t('启用任务成功'),
            });
          }
          this.$store.dispatch('task/status');
        })
        .catch((e) => {
          console.error(e);
        })
        .finally(() => {
          this.$store.dispatch('task/detail', this.taskDetail.taskId);
        });
    },
    goToPipeline(projectId = this.taskDetail.projectId, pipelineId = this.taskDetail.pipelineId) {
      window.open(
        `${window.DEVOPS_SITE_URL}/console/pipeline/${projectId}/
${pipelineId}/edit#${this.taskDetail.atomCode}`,
        '_blank',
      );
    },
    // 获取关联流水线
    getRelatedPipelines() {
      this.refreshLoading = true;
      this.$store.dispatch('task/relatedPipelines')
        .then((res) => {
          this.relatedPipelines = res.data || [];
          this.refreshLoading = false;

          this.$nextTick(() => {
            window.changeAlert = false;
          });
        });
    },
    // 删除任务
    handleDelete() {
      this.deleteLoading = true;
      this.$store.dispatch('task/deleteTask', {
        taskId: this.taskId,
      }).then((res) => {
        if (res.data) {
          this.$bkMessage({
            theme: 'success',
            message: this.$t('删除成功'),
          });
          this.$router.push({
            name: 'task-list',
          });
        } else {
          this.$bkMessage({
            theme: 'error',
            message: this.$t('删除失败'),
          });
        }
      })
        .catch((e) => {
          console.error(e);
          this.$bkMessage({
            theme: 'error',
            message: this.$t('删除失败'),
          });
        })
        .finally(() => {
          this.deleteLoading = false;
        });
    },

    handleToV3() {
      const url = `${window.PAAS_V3_URL}/plugin-center/plugin/${window.PAAS_V3_APP}/${this.taskDetail.testTool}/version-manage`;
      window.open(url, '_blank');
    },
  },
};
</script>

<style lang="postcss" scoped>
.manage-section {
  width: 690px;
  padding-left: 20px;
}

.relate-pipeline {
  margin-bottom: 40px;
  font-size: 12px;
  border: 1px solid #dcdee5;

  .section-header {
    position: relative;
    min-height: 40px;
    padding: 10px 80px 10px 24px;
    background: #F5F7FA;
  }

  .refresh {
    position: absolute;
    top: 50%;
    right: 10px;
    float: right;
    font-size: 12px;
    color: #3A84FF;
    cursor: pointer;
    transform: translateY(-50%);

    .icon-refresh-2 {
      position: absolute;
      top: 50%;
      left: -20px;
      transform: translateY(-60%);
    }
  }

  .section-content {
    padding: 15px 24px;
    line-height: 24px;
    color: #3A84FF;

    .content-item {
      /* display: inline-block; */
      cursor: pointer;
    }
  }
}

.disable-form {
  margin-bottom: 40px;
}

.form-header {
  font-size: 14px;
  font-weight: 700;
  line-height: 32px;
  color: #63656E;
}

.text-warning {
  color: #ff9c01;
}

.text-danger {
  color: #ea3636;
}

.bk-alert-gray {
  background-color: #F0F1F5;
  border: #F0F1F5;
}

.minus-circle {
  padding-right: 5px;
  font-size: 16px !important;
}

.bk-form.bk-form-vertical .bk-form-item+.bk-form-item {
  margin-top: 24px;
}

.delete-dialog {
  text-align: center;

  .bk-icon {
    display: inline-block;
    width: 42px;
    height: 42px;
    font-size: 26px;
    line-height: 42px;
    color: #ff9c01;
    background: #FFE8C3;
    border-radius: 50%;
  }

  .header {
    height: 32px;
    margin-top: 19px;
    font-size: 20px;
    line-height: 32px;
    color: #313238;
  }

  .side-header {
    margin: 8px 0;
    font-size: 12px;
    color: #63656e;
  }

  .content {
    padding: 8px 16px 16px;
    font-size: 12px;
    text-align: left;
    background: #F5F7FA;

    .task-name {
      line-height: 28px;

      .task-name-text {
        color: #000;
      }
    }
  }

  .footer {
    margin-top: 20px;

    .bk-button {
      margin-left: 20px;
    }
  }
}
</style>
