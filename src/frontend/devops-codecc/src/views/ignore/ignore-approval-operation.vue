<template>
  <div
    class="cc-ignore-operation main-content-outer main-content-ignore-operation"
  >
    <div class="operation-header">
      <span class="back-icon" @click="handleRouterBack">
        <i class="bk-icon icon-angle-left f20"></i>
        {{ $t('返回') }}
      </span>
      <span class="operation-title ml10">{{ title }}</span>
    </div>
    <div class="operation-content" v-bkloading="{ isLoading }">
      <bk-form
        class="form-content"
        ref="form"
        :model="formData"
        :rules="rules"
        :label-width="formLabelWidth"
      >
        <bk-form-item
          :label="$t('忽略审批名称')"
          required
          property="name"
          error-display-type="normal"
        >
          <bk-input
            ref="nameInput"
            :placeholder="$t('请输入审批配置名称')"
            maxlength="50"
            v-model="formData.name"
          ></bk-input>
        </bk-form-item>
        <div class="line"></div>
        <div class="review-setting-title">
          {{ $t('忽略什么问题会经过审批') }}
        </div>
        <bk-form-item :label="$t('问题维度')" property="dimensions" error-display-type="normal" required>
          <bk-checkbox-group v-model="formData.dimensions">
            <bk-checkbox
              v-for="(value, key) in dimensionsList"
              :key="key"
              :value="key"
              class="mr40"
            >
              {{ value }}
            </bk-checkbox>
          </bk-checkbox-group>
        </bk-form-item>
        <bk-form-item :label="$t('问题级别')" property="severities" error-display-type="normal" required>
          <bk-checkbox-group v-model="formData.severities">
            <bk-checkbox
              v-for="(value, key, index) in defectSeverityMap"
              :value="Number(key)"
              :key="index"
              class="mr40"
            >
              {{ value }}
            </bk-checkbox>
          </bk-checkbox-group>
        </bk-form-item>
        <bk-form-item :label="$t('问题创建时间')" property="defectCreateTime" error-display-type="normal">
          <bk-date-picker
            v-model="formData.defectCreateTime"
            :placeholder="$t('请选择忽略类型')"
            type="datetime"
            clearable
          >
          </bk-date-picker>
        </bk-form-item>
        <bk-form-item :label="$t('忽略类型')" property="ignoreTypeIds" error-display-type="normal" required>
          <bk-select
            v-model="formData.ignoreTypeIds"
            multiple
            :placeholder="$t('请选择忽略类型')"
            style="width: 490px"
          >
            <bk-option
              v-for="(item, index) in typeList"
              :key="index"
              :id="item.id"
              :name="item.name"
            />
          </bk-select>
        </bk-form-item>
        <bk-form-item :label="$t('任务范围')" property="taskScopeType" error-display-type="normal" required class="mb-8">
          <bk-radio-group v-model="formData.taskScopeType">
            <bk-radio
              v-for="item in taskScope"
              :key="item.value"
              :value="item.value"
              class="mr-5"
            >{{ item.name }}</bk-radio>
          </bk-radio-group>
          <bk-select
            v-if="formData.taskScopeType !== 'ALL'"
            v-model="formData.taskScopeList"
            multiple
            :placeholder="$t('请选择任务')"
            style="width: 490px; margin-top: 12px"
          >
            <bk-option
              v-for="item in taskList.enableTasks"
              :key="item.taskId"
              :id="item.taskId"
              :name="item.nameCn"
            >
            </bk-option>
          </bk-select>
        </bk-form-item>
        <div class="line"></div>

        <div class="review-setting-title">
          {{ $t('忽略什么问题会经过审批') }}
        </div>
        <bk-form-item :label="$t('审批人')" property="approverTypes" error-display-type="normal" required>
          <bk-select
            multiple
            v-model="formData.approverTypes"
            :placeholder="$t('请选择审批人')"
          >
            <bk-option
              v-for="(item, index) in approverList"
              :key="index"
              :id="item.id"
              :name="item.name"
            >
              <span>{{ item.name }}</span>
            </bk-option>
          </bk-select>
        </bk-form-item>
        <bk-form-item
          v-if="hasCustomApprover"
          property="customApprovers"
          error-display-type="normal"
          class="!mt-[12px]">
          <UserSelector
            allow-create
            style="width: 420px; margin-left: 10px"
            :value.sync="formData.customApprovers"
          />
        </bk-form-item>
        <bk-form-item>
          <bk-button theme="primary" class="mr10" :loading="buttonLoading" @click="handleConfirm">
            {{ $t('确定') }}
          </bk-button>
          <bk-button @click="handleRouterBack">{{ $t('取消') }}</bk-button>
        </bk-form-item>
      </bk-form>
    </div>
  </div>
</template>

<script>
import { leaveConfirm } from '@/common/leave-confirm';
import { getTaskList } from '@/common/preload';
import { deepClone } from '@/common/util';
import { mapState } from 'vuex';
import UserSelector from '@/components/user-selector/index.vue';
export default {
  components: {
    UserSelector,
  },
  props: {
    entityId: {
      type: String,
      default: '',
    },
  },
  data() {
    return {
      isLoading: false,
      buttonLoading: false,
      pendingRequests: 0,
      formData: {
        name: '',
        dimensions: [],
        severities: [],
        defectCreateTime: '',
        ignoreTypeIds: [],
        taskScopeType: 'ALL',
        taskScopeList: [],
        approverTypes: [],
        customApprovers: [],
      },
      dimensionsList: {
        DEFECT: this.$t('代码缺陷'),
        SECURITY: this.$t('安全漏洞'),
        STANDARD: this.$t('代码规范'),
      },
      defectSeverityMap: {
        1: this.$t('严重'),
        2: this.$t('一般'),
        4: this.$t('提示'),
      },
      taskScopeMap: {
        ALL: this.$t('全选'),
        INCLUDE: this.$t('包含部分任务'),
        EXCLUDE: this.$t('排除部分任务'),
      },
      typeList: [],
      taskScope: [],
      approverList: [],
      specificPersonnel: [
        { label: '具体人员1', value: 1 },
        { label: '具体人员2', value: 1 },
      ],
      rules: {
        name: [
          {
            required: true,
            message: this.$t('请填写忽略类型名称'),
            trigger: 'blur',
          },
          {
            validator: val => /^[\u4e00-\u9fa5_A-Za-z0-9]{1,50}$/.test(val),
            message: this.$t('可使用中文、字母（不分大小写）、数字、下划线，长度不超过50个字符'),
            trigger: 'blur',
          },
        ],
        dimensions: [
          {
            required: true,
            message: this.$t('请选择问题维度'),
            trigger: 'blur',
          },
        ],
        severities: [
          {
            required: true,
            message: this.$t('请选择问题级别'),
            trigger: 'blur',
          },
        ],
        ignoreTypeIds: [
          {
            required: true,
            message: this.$t('请选择忽略类型'),
            trigger: 'blur',
          },
        ],
        taskScopeType: [
          {
            validator: () => !!this.formData.taskScopeType,
            message: this.$t('请选择任务范围'),
            trigger: 'blur',
          },
          {
            validator: () => this.formData.taskScopeType === 'ALL' || this.formData.taskScopeList.length !== 0,
            message: this.$t('请选择任务范围'),
            trigger: 'blur',
          },
        ],
        approverTypes: [
          {
            required: true,
            message: this.$t('请选择审批人'),
            trigger: 'blur',
          },
        ],
        customApprovers: [
          {
            required: true,
            message: this.$t('请添加自定义审批人'),
            trigger: 'blur',
          },
        ],
      },
    };
  },
  computed: {
    ...mapState('task', {
      taskList: 'list',
    }),
    title() {
      return this.entityId ? this.$t('编辑审批配置') : this.$t('新增审批配置');
    },
    langType() {
      return window.navigator.systemLanguage
        ? window.navigator.systemLanguage
        : window.navigator.language;
    },
    formLabelWidth() {
      return this.langType === 'zh-CN' ? 110 : 150;
    },
    hasCustomApprover() {
      const CUSTOM_APPROVER = 'CUSTOM_APPROVER';
      return this.formData.approverTypes.includes(CUSTOM_APPROVER);
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
    this.handleInitForm();
  },
  methods: {
    handleFormDataChange() {
      global.changeAlert = true;
    },
    handleRouterBack() {
      leaveConfirm().then(() => {
        this.$router.push({
          name: 'ignoreList',
          query: {
            active: 'ignore-approval',
          },
        });
      });
    },
    handleConfirm() {
      this.$refs.form.validate().then((e) => {
        this.buttonLoading = true;
        const curData = deepClone(this.formData);
        curData.defectCreateTime = new Date(curData.defectCreateTime).valueOf();
        if (curData.taskScopeType === 'ALL') {
          curData.taskScopeList = [];
        }
        if (this.entityId) {
          curData.entityId = this.entityId;
        }
        this.$store
          .dispatch('ignore/createIgnoreApproval', curData)
          .then((res) => {
            if (res) {
              this.$bkMessage({
                message: this.entityId ? this.$t('更新成功') : this.$t('创建成功'),
                theme: 'success',
              });
              this.$router.push({
                name: 'ignoreList',
                query: {
                  active: 'ignore-approval',
                },
              });
            }
          })
          .finally((e) => {
            this.buttonLoading = false;
          });
      });
    },
    handleFetchIgnoreType() {
      return new Promise((resolve) => {
        this.$store.dispatch('ignore/fetchIgnoreList').then((res) => {
          this.typeList = res.data.map(item => ({
            id: item.ignoreTypeId,
            name: item.name,
          }));
        })
          .finally(() => resolve());
      });
    },
    handleFetchApprover() {
      return new Promise((resolve) => {
        this.$store.dispatch('ignore/fetchApproverList').then((res) => {
          this.approverList = res?.data?.APPROVER_TYPE.map(item => ({
            id: item.key,
            name: item.name,
          }));
        })
          .finally(() => resolve());
      });
    },
    handleFetchTaskScope() {
      return new Promise((resolve) => {
        this.$store.dispatch('ignore/fetchTaskScopeType').then((res) => {
          this.taskScope = res?.data?.IGNORE_APPROVAL_TASK_SCOPE_TYPE.map(item => ({
            value: item.key,
            name: this.taskScopeMap[item.key],
          }));
        })
          .finally(() => resolve());
      });
    },
    fetchIgnoreInfo(entityId) {
      return new Promise((resolve) => {
        this.$store.dispatch('ignore/fetchIgnoreApprovalDetail', entityId).then(({ data }) => {
          this.formData.name = data.name;
          this.formData.dimensions = data.dimensions;
          this.formData.severities = data.severities;
          this.formData.defectCreateTime = data.defectCreateTime ? new Date(data.defectCreateTime) : '';
          this.formData.ignoreTypeIds = data.ignoreTypeIds;
          this.formData.taskScopeType = data.taskScopeType;
          this.formData.taskScopeList = data.taskScopeList;
          this.formData.approverTypes = data.approverTypes;
          this.formData.customApprovers = data.customApprovers;
          this.$nextTick(() => {
            global.changeAlert = false;
          });
        })
          .finally(() => resolve());
      });
    },
    // 初始化表单数据，统一loading
    async handleInitForm() {
      this.isLoading = true;
      const requests = [
        this.trackRequest(() => getTaskList()),
        this.trackRequest(() => this.handleFetchIgnoreType()),
        this.trackRequest(() => this.handleFetchApprover()),
        this.trackRequest(() => this.handleFetchTaskScope()),

        // 未放在watch处理，因为需要统一表单loading
        this.entityId && this.trackRequest(() => this.fetchIgnoreInfo(this.entityId)),
      ];

      try {
        await Promise.all(requests);
      } catch (err) {
        console.warn(err);
      } finally {
        this.isLoading = false;
      }
    },
    trackRequest(fn) {
      this.pendingRequests += 1;
      return fn().finally(() => {
        this.pendingRequests -= 1;
        if (this.pendingRequests === 0) {
          this.isLoading = false;
        }
      });
    },
  },
};
</script>

<style lang="postcss" scoped>
.main-content-outer {
  display: block;
  width: 1236px;
  background: #fff;
  border-radius: 2px;
  box-shadow: 0 2px 12px 0 hsl(0deg 0% 87% / 50%),
    0 2px 13px 0 hsl(0deg 0% 87% / 50%);

  .operation-header {
    width: 100%;
    height: 56px;
    padding: 0 30px;
    line-height: 56px;
    background: #fafbfd;
    border-bottom: 1px solid #dcdee5;
  }

  .back-icon {
    font-size: 14px;
    color: #3a84ff;
    cursor: pointer;
  }

  .operation-title {
    font-size: 14px;
    color: #313238;
  }

  .operation-content {
    display: flex;
    width: 100%;
    background-color: #fff;
    justify-content: center;
  }

  .form-content {
    width: 600px;
    padding: 40px 0;
  }

  .line {
    position: relative;
    right: 50px;
    width: 720px;
    height: 1px;
    margin: 20px 0;
    background: #dcdee5;
  }

  .review-setting-title {
    margin-bottom: 20px;
    font-size: 14px;
    font-weight: bold;
    color: #737987;
  }

  .date-select {
    /deep/ .bk-form-content {
      display: flex;
      justify-content: space-between;
    }
  }
}
</style>
