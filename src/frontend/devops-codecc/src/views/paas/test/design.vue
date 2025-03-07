<template>
  <div class="paas-test bg-white p-[24px]">
    <div class="flex flex-row space-x-4">
      <section class="basis-1/2 px-[16px] py-[12px]">
        <p class="pb-[24px] flex justify-between">
          <span class="text-[#313238] text-[14px] font-bold">{{ $t('测试对象') }}</span>
        </p>
        <bk-form label-width="95">
          <bk-form-item required :label="$t('测试项目')">
            <bk-select
              v-model="testProject"
              :disabled="!!process"
              :loading="projLoading"
              searchable
              @toggle="handleToggleProj"
              :placeholder="$t('请选择')">
              <bk-option
                v-for="option in projectList"
                :key="option.projectId"
                :id="option.projectId"
                :name="option.projectName">
              </bk-option>
              <div slot="extension" @click="handleCreateProj" class="cursor-pointer">
                <i class="bk-icon icon-plus-circle"></i>
                {{ $t('新建项目') }}
              </div>
            </bk-select>
          </bk-form-item>
          <bk-form-item required :label="$t('测试任务')">
            <section class="max-h-[160px] overflow-auto">
              <div
                v-for="item in checkerSetList"
                :key="item.checkerSetId"
                class="flex flex-row space-x-4 mb-[8px]">
                <bk-input class="basis-1/3" v-model="item.checkerSetLang" disabled>
                  <template slot="prepend">
                    <div class="group-text">{{ $t('语言') }}</div>
                  </template>
                </bk-input>
                <bk-input class="basis-2/3" v-model="item.checkerSetName" disabled>
                  <template slot="prepend">
                    <div class="group-text">{{ $t('规则集') }}</div>
                  </template>
                </bk-input>
              </div>
            </section>

          </bk-form-item>
          <bk-form-item class="!mt-0">
            <div class="p-[8px] bg-[#F5F7FA] text-[12px]" v-bkloading="{ isLoading: taskLoading }">
              <bk-button
                icon="plus"
                class="w-full bg-white"
                :disabled="!testProject || process === 'testing'"
                @click="addNewTestTask">
                {{ $t('新建测试任务') }}
              </bk-button>
              <div v-if="taskList.length">
                <section class="max-h-[160px] mt-[8px] overflow-auto border border-[#DCDEE5] bg-white">
                  <div
                    v-for="item in taskList" :key="item.taskId"
                    @click="goToTask(item.taskId)"
                    class="task-list px-[16px] py-[8px] leading-[20px] border-y hover:bg-[#FAFBFD] cursor-pointer
                    relative">
                    <p class="text-[#63656E]">{{ item.nameCn }}</p>
                    <p class="text-[#979BA5]" v-for="lib in item.codeLibraryInfo.codeInfo" :key="lib">
                      {{ lib.aliasName }}@{{ lib.branch }}
                    </p>
                    <bk-icon
                      v-if="process !== 'testing'"
                      class="task-list-icon absolute right-[12px] top-[16px] text-[#C4C6CC] hover:text-[#63656E]
                      !text-[14px] p-[5px]"
                      @click.stop="handleDelete(item)"
                      type="minus-circle-shape" />
                  </div>
                </section>
              </div>
              <div class="flex justify-between">
                <span>{{ $t('共x个任务', { num: taskList.length }) }}</span>
                <span>
                  <bk-link
                    theme="primary"
                    class="ml-[2px] mr-[16px]" icon="codecc-icon icon-refresh-2"
                    @click="getTaskList">
                    {{ refresh ? $t('已创建测试任务？点此刷新数据') : $t('刷新数据') }}
                  </bk-link>
                  <bk-link theme="primary" icon="codecc-icon icon-link" @click="openTaskList">
                    {{ $t('前往任务列表') }}
                  </bk-link>
                </span>
              </div>
            </div>
          </bk-form-item>
          <bk-form-item>
            <bk-button
              theme="primary"
              :disabled="!testProject || process === 'testing' || !taskList.length"
              @click="startTest">
              {{ process === 'testing' ? $t('测试中...') : $t('开始测试') }}
            </bk-button>
            <span v-if="process === 'success'">
              <bk-icon class="text-[14px] text-[#2DCB56] pl-[15px]" type="check-circle-shape" />
              <span class="text-[12px]">{{ $t('测试已通过') }}</span>
            </span>
            <span v-if="process === 'fail'">
              <bk-icon class="text-[14px] text-[#FF5A5A] pl-[15px]" type="close-circle-shape" />
              <span class="text-[12px]">{{ $t('测试未通过') }}</span>
            </span>
          </bk-form-item>
        </bk-form>
      </section>
      <section class="basis-1/2 px-[16px] py-[12px] bg-[#F5F7FA] text-[12px]">
        <result ref="resultRef" :stage="1" @updateResult="updateResult"></result>
      </section>

      <bk-dialog v-model="deleteVisible" width="450" :show-footer="false" class="delete-dialog">
        <div class="bk-icon icon-exclamation">
        </div>
        <div class="header">{{ $t('确定删除此任务？') }}</div>
        <div>{{ $t('任务') }}: {{ deleteTask.nameCn }}</div>
        <div class="side-header">
          {{ $t('删除的任务将') }}
          <span class="text-danger">{{ $t('无法找回') }}</span>
          {{ $t('，请谨慎操作！') }}
        </div>
        <div class="footer">
          <bk-button
            theme="danger"
            :loading="deleteLoading"
            @click="handleDeleteConfirm">
            {{ $t('删除') }}
          </bk-button>
          <bk-button @click="deleteVisible = false">{{ $t('取消') }}</bk-button>
        </div>
      </bk-dialog>
    </div>
  </div>
</template>

<script>
import { bkSelect, bkOption } from 'bk-magic-vue';
import Result from './result.vue';

export default {
  components: {
    bkSelect,
    bkOption,
    Result,
  },
  data() {
    const { toolName } = this.$route.params;
    const testProject = window.localStorage.getItem(`${toolName}-test-project`) || '';
    return {
      projectList: [],
      testProject,
      checkerSetList: [],
      taskList: [],
      result: {},
      process: '', // 测试进度，testing, success, fail
      deleteVisible: false,
      deleteLoading: false,
      taskLoading: false,
      deleteTask: {},
      refresh: false,
      projLoading: false,
    };
  },
  watch: {
    testProject(val) {
      if (val) {
        this.getTaskList();
        const { toolName } = this.$route.params;
        window.localStorage.setItem(`${toolName}-test-project`, val);
      }
    },
    process(val) {
      console.log('🚀 ~ process ~ val:', val);
      if (val === 'success') {
        window.parent.postMessage({
          type: 'design-test',
          data: val,
        }, '*');
      }
    },
  },
  beforeCreate() {
    this.$store.dispatch('test/getToolList', this.$route.params.toolName).then((res) => {
      this.checkerSetList = res?.checkerSetList || [];
    });
  },
  created() {
    this.getTaskList();
    this.getProjectList();
  },
  methods: {
    handleToggleProj(isVisible) {
      if (isVisible) {
        this.getProjectList();
      }
    },
    getProjectList() {
      this.projLoading = true;
      this.$store.dispatch('test/getProjectList').then((res) => {
        this.projectList = res;
      })
        .finally(() => {
          this.projLoading = false;
        });
    },
    getTaskList() {
      this.refresh = false;
      if (!this.testProject) return;
      this.taskLoading = true;
      const payload = {
        toolName: this.$route.params.toolName,
        projectId: this.testProject,
      };
      this.$store.dispatch('test/getTaskList', payload).then((res) => {
        this.taskList = res || [];
        this.taskLoading = false;
      });
    },
    addNewTestTask() {
      this.refresh = true;
      window.open(
        `${window.DEVOPS_SITE_URL}/console/codecc/${this.testProject}/task/new?toolName=${this.$route.params.toolName}&isTestTask=true&version=${this.$route.query.version}`,
        '_blank',
      );
    },
    openTaskList() {
      window.open(
        `${window.DEVOPS_SITE_URL}/console/codecc/${this.testProject}/task/list?toolName=${this.$route.params.toolName}&taskType=3`,
        '_blank',
      );
    },
    startTest() {
      this.$store.dispatch('test/startTest', {
        version: this.$route.query.version,
        projectId: this.testProject,
        projectName: this.projectList.find(item => item.projectId === this.testProject)?.projectName,
        toolName: this.$route.params.toolName,
      }).then((res) => {
        if (res.data) {
          this.$refs.resultRef.init();
        } else {
          this.$bkMessage({
            theme: 'error',
            message: res.message || this.$t('开始测试失败'),
          });
        }
      });
    },
    updateResult(result) {
      this.result = result;
      if (result.projectId) {
        this.projectList.push({
          projectId: result.projectId,
          projectName: result.projectName,
        });
        this.testProject = result.projectId;
        const { taskCount, failCount, successCount, isPass } = result;
        if (taskCount === failCount + successCount) {
          if (failCount || !isPass) {
            this.process = 'fail';
          } else {
            this.process = 'success';
          }
        } else {
          this.process = 'testing';
        }
      }
    },
    handleDelete(task) {
      this.deleteTask = task;
      this.deleteVisible = true;
    },
    handleDeleteConfirm() {
      this.deleteLoading = true;
      this.$store.dispatch('test/deleteTestTask', {
        projectId: this.testProject,
        taskId: this.deleteTask.taskId,
      }).then((res) => {
        if (res.data) {
          this.$bkMessage({
            theme: 'success',
            message: this.$t('删除成功'),
          });
          this.taskList = this.taskList.filter(item => item.taskId !== this.deleteTask.taskId);
          this.deleteVisible = false;
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
    goToTask(taskId) {
      window.open(
        `${window.DEVOPS_SITE_URL}/console/codecc/${this.testProject}/task/${taskId}/detail`,
        '_blank',
      );
    },
    handleCreateProj() {
      window.open(
        `${window.DEVOPS_SITE_URL}/console/manage/apply`,
        '_blank',
      );
    },
  },
};
</script>
<style>
#app {
  min-width: 960px;
}
</style>

<style lang="postcss" scoped>

.paas-test {
  >>> .bk-link .bk-link-text {
    font-size: 12px;
  }
}

.task-list {
  .task-list-icon {
    display: none;
  }

  &:hover {
    .task-list-icon {
      display: block;
    }
  }
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


  .footer {
    margin-top: 20px;

    .bk-button {
      margin-left: 20px;
    }
  }
}
</style>
