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
        :rules="isSysIgnore ? null : rules"
        :label-width="formLabelWidth"
      >
        <bk-form-item
          :label="$t('忽略类型名称')"
          required
          property="name"
          error-display-type="normal"
        >
          <bk-input
            ref="nameInput"
            maxlength="50"
            v-model="formData.name"
            :disabled="isSysIgnore"
          ></bk-input>
        </bk-form-item>
        <template v-if="isInnerSite">
          <div class="line"></div>
          <div class="review-setting-title">
            {{ $t('忽略问题 review 提醒设置') }}
          </div>
          <bk-form-item :label="$t('提醒日期')" ext-cls="date-select">
            <bk-select
              v-model="formData.notify.notifyMonths"
              multiple
              style="width: 150px"
              @change="handleMonthsSelect"
            >
              <bk-option
                v-for="(item, index) in monthsMap"
                :key="index"
                :id="item.value"
                :name="item.label"
                :class="{
                  'is-selected':
                    formData.notify.notifyMonths &&
                    (formData.notify.notifyMonths.includes(item.value) ||
                      formData.notify.notifyMonths.includes(-1)),
                }"
              >
                <span>{{ item.label }}</span>
                <i
                  v-if="
                    formData.notify.notifyMonths &&
                      (formData.notify.notifyMonths.includes(item.value) ||
                        formData.notify.notifyMonths.includes(-1))
                  "
                  class="bk-option-icon bk-icon icon-check-1"
                ></i>
              </bk-option>
            </bk-select>
            <bk-select
              v-model="formData.notify.notifyWeekOfMonths"
              multiple
              style="width: 150px"
              @change="handleWeekOfMonthsSelect"
            >
              <bk-option
                v-for="(item, index) in weekOfMonthsMap"
                :id="item.value"
                :name="item.label"
                :key="index"
                :class="{
                  'is-selected':
                    formData.notify.notifyWeekOfMonths &&
                    (formData.notify.notifyWeekOfMonths.includes(item.value) ||
                      formData.notify.notifyWeekOfMonths.includes(-1)),
                }"
              >
                <span>{{ item.label }}</span>
                <i
                  v-if="
                    formData.notify.notifyWeekOfMonths &&
                      (formData.notify.notifyWeekOfMonths.includes(item.value) ||
                        formData.notify.notifyWeekOfMonths.includes(-1))
                  "
                  class="bk-option-icon bk-icon icon-check-1"
                ></i>
              </bk-option>
            </bk-select>
            <bk-select
              v-model="formData.notify.notifyDayOfWeeks"
              multiple
              style="width: 150px"
            >
              <bk-option
                v-for="(item, index) in dayOfWeeksMap"
                :id="item.value"
                :name="item.label"
                :key="index"
              ></bk-option>
            </bk-select>
          </bk-form-item>
          <bk-form-item :label="$t('通知人角色')">
            <bk-checkbox-group v-model="formData.notify.notifyReceiverTypes">
              <bk-checkbox value="ignore_author" class="mr40">{{
                $t('问题忽略人')
              }}</bk-checkbox>
              <bk-checkbox value="defect_author" class="mr40">{{
                $t('问题处理人')
              }}</bk-checkbox>
              <bk-checkbox value="task_creator">{{
                $t('任务创建人')
              }}</bk-checkbox>
            </bk-checkbox-group>
          </bk-form-item>
          <bk-form-item :label="$t('附加通知人')">
            <!-- <bk-input v-model="formData.notify.extReceiver"></bk-input> -->
            <UserSelector
              allow-create
              :value.sync="formData.notify.extReceiver"
            />
          </bk-form-item>
          <bk-form-item :label="$t('通知方式')">
            <bk-checkbox-group v-model="formData.notify.notifyTypes">
              <bk-checkbox value="rtx" class="mr40">{{
                $t('企业微信')
              }}</bk-checkbox>
              <bk-checkbox value="email">{{ $t('邮件') }}</bk-checkbox>
            </bk-checkbox-group>
          </bk-form-item>
        </template>
        <bk-form-item>
          <bk-button theme="primary" class="mr10" @click="handleConfirm">{{
            $t('确定')
          }}</bk-button>
          <bk-button @click="handleRouterBack">{{ $t('取消') }}</bk-button>
        </bk-form-item>
      </bk-form>
    </div>
  </div>
</template>

<script>
import { leaveConfirm } from '@/common/leave-confirm';
import { deepClone } from '@/common/util';
import UserSelector from '@/components/user-selector/index.vue';
import DEPLOY_ENV from '@/constants/env';

export default {
  components: {
    UserSelector,
  },
  props: {
    id: {
      type: Number,
      default: '',
    },
    entityId: {
      type: String,
      default: '',
    },
    isSysIgnore: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      isInnerSite: DEPLOY_ENV === 'tencent',
      ignoreInfo: {},
      isLoading: false,
      formData: {
        name: '',
        notify: {
          notifyMonths: [-1],
          notifyWeekOfMonths: [-1],
          notifyDayOfWeeks: [1],
          notifyReceiverTypes: [
            'ignore_author',
            'defect_author',
            'task_creator',
          ],
          extReceiver: [],
          notifyTypes: ['rtx', 'email'],
          everyWeek: false,
          everyMonth: true,
        },
      },
      weekOfMonthsMap: [
        { label: this.$t('每周'), value: -1 },
        { label: this.$t('第一个'), value: 1 },
        { label: this.$t('第二个'), value: 2 },
        { label: this.$t('第三个'), value: 3 },
        { label: this.$t('第四个'), value: 4 },
        { label: this.$t('第五个'), value: 5 },
      ],
      dayOfWeeksMap: [
        { label: this.$t('星期一'), value: 1 },
        { label: this.$t('星期二'), value: 2 },
        { label: this.$t('星期三'), value: 3 },
        { label: this.$t('星期四'), value: 4 },
        { label: this.$t('星期五'), value: 5 },
        { label: this.$t('星期六'), value: 6 },
        { label: this.$t('星期日'), value: 7 },
      ],
      monthsMap: [
        { label: this.$t('每月'), value: -1 },
        { label: this.$t('一月'), value: 1 },
        { label: this.$t('二月'), value: 2 },
        { label: this.$t('三月'), value: 3 },
        { label: this.$t('四月'), value: 4 },
        { label: this.$t('五月'), value: 5 },
        { label: this.$t('六月'), value: 6 },
        { label: this.$t('七月'), value: 7 },
        { label: this.$t('八月'), value: 8 },
        { label: this.$t('九月'), value: 9 },
        { label: this.$t('十月'), value: 10 },
        { label: this.$t('十一月'), value: 11 },
        { label: this.$t('十二月'), value: 12 },
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
      },
    };
  },
  computed: {
    title() {
      return this.entityId ? this.$t('编辑忽略配置') : this.$t('新增忽略配置');
    },
    langType() {
      return window.navigator.systemLanguage
        ? window.navigator.systemLanguage
        : window.navigator.language;
    },
    formLabelWidth() {
      return this.langType === 'zh-CN' ? 110 : 150;
    },
  },
  watch: {
    formData: {
      handler() {
        this.handleFormDataChange();
      },
      deep: true,
    },
    id: {
      handler(val) {
        if (val) {
          this.fetchIgnoreInfo();
        }
      },
      immediate: true,
    },
  },
  methods: {
    fetchIgnoreInfo() {
      this.isLoading = true;
      this.$store.dispatch('ignore/getIgnoreInfo', this.id).then((res) => {
        const { name, notify } = res.data;
        if (!notify.notifyMonths) notify.notifyMonths = [];
        if (!notify.notifyWeekOfMonths) notify.notifyWeekOfMonths = [];
        if (!notify.notifyDayOfWeeks) notify.notifyDayOfWeeks = [];
        this.formData.name = name;
        this.formData.notify = notify;
        if (notify.everyMonth) {
          this.formData.notify.notifyMonths = [-1];
        }
        if (notify.everyWeek) {
          this.formData.notify.notifyWeekOfMonths = [-1];
        }
        this.$nextTick(() => {
          global.changeAlert = false;
        });
        this.isLoading = false;
      });
    },
    handleFormDataChange() {
      global.changeAlert = true;
    },
    handleRouterBack() {
      leaveConfirm().then(() => {
        this.$router.push({
          name: 'ignoreList',
          query: {
            active: 'ignore-type',
          },
        });
      });
    },
    handleConfirm() {
      this.$refs.form.validate().then(() => {
        const curData = deepClone(this.formData);
        const {
          notifyMonths,
          notifyWeekOfMonths,
          notifyDayOfWeeks,
          notifyReceiverTypes,
          notifyTypes,
        } = curData.notify;
        let verify = false;
        if (
          (!notifyMonths.length
            && !notifyWeekOfMonths.length
            && !notifyDayOfWeeks.length)
          || (notifyMonths.length
            && notifyWeekOfMonths.length
            && notifyDayOfWeeks.length)
        ) {
          // 提醒日期三个都不选 或 三个都选择
          verify = true;
        } else {
          verify = false;
        }
        if (!verify) {
          this.$bkMessage({
            message: this.$t('请填写完整的提醒日期'),
            theme: 'error',
          });
          return;
        }
        if (notifyMonths.includes(-1)) {
          curData.notify.everyMonth = true;
          curData.notify.notifyMonths = [];
        } else {
          curData.notify.everyMonth = false;
        }

        if (notifyWeekOfMonths.includes(-1)) {
          curData.notify.everyWeek = true;
          curData.notify.notifyWeekOfMonths = [];
        } else {
          curData.notify.everyWeek = false;
        }

        if (
          !notifyReceiverTypes
          || !notifyReceiverTypes.length
          || !notifyTypes?.length
        ) {
          curData.notify.notifyMonths = [];
          curData.notify.notifyWeekOfMonths = [];
          curData.notify.notifyDayOfWeeks = [];
          curData.notify.everyMonth = false;
          curData.notify.everyWeek = false;
        }
        if (!this.id) {
          this.$store.dispatch('ignore/createIgnore', curData).then((res) => {
            if (res) {
              this.$bkMessage({
                message: this.$t('创建成功'),
                theme: 'success',
              });
              this.$router.push({
                name: 'ignoreList',
                query: {
                  active: 'ignore-type',
                },
              });
            }
          });
        } else {
          const params = {
            entityId: this.entityId,
            ...curData,
          };
          this.$store.dispatch('ignore/updateNotify', params).then((res) => {
            if (res) {
              this.$bkMessage({
                message: this.$t('更新成功'),
                theme: 'success',
              });
              this.$router.push({
                name: 'ignoreList',
              });
            }
          });
        }
      });
    },
    handleMonthsSelect(newVal, oldVal) {
      let type = '';
      let selectId = null;
      if (newVal.length > oldVal.length) {
        type = 'add';
        selectId = newVal.filter(id => !oldVal.includes(id));
      } else {
        type = 'delete';
        selectId = oldVal.filter(id => !newVal.includes(id));
      }
      selectId = selectId && selectId[0];
      if (type === 'add') {
        if (selectId === -1 || newVal.length === 12) {
          this.formData.notify.notifyMonths = [-1];
        } else if (newVal.includes(-1)) {
          this.formData.notify.notifyMonths = this.monthsMap
            .filter(i => !newVal.includes(i.value))
            .map(_i => _i.value);
        }
      } else {
        this.formData.notify.notifyMonths = newVal;
      }
    },

    handleWeekOfMonthsSelect(newVal, oldVal) {
      let type = '';
      let selectId = null;
      if (newVal.length > oldVal.length) {
        type = 'add';
        selectId = newVal.filter(id => !oldVal.includes(id));
      } else {
        type = 'delete';
        selectId = oldVal.filter(id => !newVal.includes(id));
      }
      selectId = selectId && selectId[0];
      if (type === 'add') {
        if (selectId === -1 || newVal.length === 5) {
          this.formData.notify.notifyWeekOfMonths = [-1];
        } else if (newVal.includes(-1)) {
          this.formData.notify.notifyWeekOfMonths = this.weekOfMonthsMap
            .filter(i => !newVal.includes(i.value))
            .map(_i => _i.value);
        } else {
          this.formData.notify.notifyWeekOfMonths = newVal;
        }
      }
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
    height: 500px;
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
