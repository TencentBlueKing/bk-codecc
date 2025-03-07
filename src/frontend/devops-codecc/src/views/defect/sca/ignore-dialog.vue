<template>
  <bk-dialog
    width="560"
    theme="primary"
    header-position="left"
    v-model="isShow"
    :title="title"
    :position="ignoreDialogPositionConfig"
    @before-close="handleBeforeClose"
    @cancel="handleHide"
  >
    <div class="reason-type-list">
      <div class="reason-type-header mb20">
        {{ $t('忽略类型') }}
        <span class="fr">
          <bk-button
            size="small"
            icon="plus"
            class="mr10"
            @click="handleSetReview"
          >{{ $t('新增类型') }}</bk-button
          >
          <bk-button size="small" @click="handleRefresh">
            <i class="codecc-icon icon-refresh-2"></i>
          </bk-button>
        </span>
      </div>
      <bk-form
        :model="form"
        :label-width="0"
        class="search-form"
        v-bkloading="{ isLoading: isLoading, opacity: 0.3 }">
        <bk-form-item property="ignoreReason">
          <!-- 忽略类型单选 -->
          <bk-radio-group v-model="form.ignoreReasonType" class="ignore-list">
            <div v-for="ignore in ignoreList" :key="ignore.ignoreTypeId">
              <bk-radio class="cc-radio" :value="ignore.ignoreTypeId" @change="() => handleChangeRadio(ignore)">
                {{ ignore.name }}
                <!-- 路径屏蔽 -->
                <span v-if="showPathShield(ignore)">
                  <a class="ml15" @click.stop="handleToPathShield">
                    {{ $t('按代码路径屏蔽') }}
                    <i class="codecc-icon icon-link"></i>
                  </a>
                </span>
                <!-- 忽略提醒 -->
                <span v-else-if="showNotify(ignore)" class="notify-tips">
                  <span
                    class="f12"
                    v-bk-tooltips="{ content: getNotifyText(ignore.nextNotifyTime, ignore.notify) }"
                  >
                    <i class="codecc-icon icon-time text-[#979ba5]"></i>
                    {{ getNotifyText(ignore.nextNotifyTime, ignore.notify) }}
                  </span>
                </span>
              </bk-radio>
              <FoldAlert
                v-if="form.ignoreReasonType === ignore.ignoreTypeId"
                :approver-list="approverList"
                :title="$t('x个问题忽略该类型需进行审批', [preIgnoreApproval.count])"
                :data-list="preIgnoreApproval.defectList" />
            </div>
          </bk-radio-group>
        </bk-form-item>
        <bk-form-item property="ignoreReason" :required="ignoreReasonRequired">
          <span>{{ $t('忽略原因') }}</span>
          <bk-input
            :type="'textarea'"
            :maxlength="255"
            v-model="form.ignoreReason"
            @change="handleIgnoreReasonChange">
          </bk-input>
        </bk-form-item>
      </bk-form>
    </div>
    <div class="footer-wrapper" slot="footer">
      <bk-button theme="primary" :disabled="ignoreReasonAble" @click.native="handleConfirm">
        {{ $t('确定') }}
      </bk-button>
      <bk-button theme="primary" @click.native="handleHide">
        {{ $t('取消') }}
      </bk-button>
    </div>
  </bk-dialog>
</template>

<script>
import { format } from 'date-fns';
import FoldAlert from '../components/fold-alert.vue';

export default {
  comments: {
    FoldAlert,
  },
  props: {
    title: {
      type: String,
      default: '',
    },
    ignoreReasonRequired: {
      type: Boolean,
    },
    ignoreReasonAble: {
      type: Boolean,
    },
    approverList: {
      type: Array,
    },
    preIgnoreApproval: {
      type: Object,
    },
    ignoreList: {
      type: Array,
    },
    isLoading: {
      type: Boolean,
    },
    handleSetReview: {
      type: Function,
    },
    ignoreReasonType: {
      type: String,
    },
    ignoreReason: {
      type: String,
    },
  },
  data() {
    return {
      isShow: false,
      form: {
        ignoreReasonType: this.$props.ignoreReasonType || '',
        ignoreReason: this.$props.ignoreReason || '',
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
    ignoreDialogPositionConfig() {
      const { clientHeight } = document.body;
      const config = {
        top: '200',
      };
      if (clientHeight <= 1000 && clientHeight > 900) {
        config.top = '150';
      } else if (clientHeight <= 900) {
        config.top = '100';
      }
      return config;
    },
  },
  watch: {
    ignoreReasonType(newVal) {
      this.form.ignoreReasonType = newVal;
    },
    ignoreReason(newVal) {
      this.form.ignoreReason = newVal;
    },
  },
  methods: {
    handleShow() {
      this.isShow = true;
    },
    handleHide() {
      this.isShow = false;
      this.form.ignoreReason = '';
      this.form.ignoreReasonType = '';
    },
    handleBeforeClose() {
      this.$emit('beforeClose');
    },
    handleConfirm() {
      this.$emit('confirm');
    },
    handleChangeRadio(val) {
      this.$emit('ignoreReasonTypeChange', val);
    },
    handleIgnoreReasonChange(val) {
      this.$emit('ignoreReasonChange', val);
    },
    handleRefresh() {
      this.$emit('refresh');
    },
    // 跳转至设置-路径屏蔽
    handleToPathShield() {
      const routeParams = { ...this.$route.params };
      const { href } = this.$router.resolve({
        name: 'task-settings',
        params: routeParams,
        query: {
          panel: 'ignore',
        },
      });
      window.open(href, '_blank');
    },
    // 生成忽略提醒文本
    handleGetNotifyDate(notify) {
      let str = '';
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
        } else if (everyMonth && notifyWeekOfMonths.length && notifyDayOfWeeks.length) {
          str = this.$t('每个月的')
            + weekOfMonthTextMap.join('、')
            + this.$t('的')
            + dayTextMap.join('、');
        } else if (notifyMonths.length && everyWeek && notifyDayOfWeeks.length) {
          str = this.$t('每年的')
            + monthsTextMap.join('、')
            + this.$t('每周的')
            + dayTextMap.join('、');
        } else if (notifyMonths.length && notifyWeekOfMonths.length && notifyDayOfWeeks.length) {
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
    formatTime(date, token, options = {}) {
      return date ? format(Number(date), token, options) : '';
    },
    getNotifyText(time, notify) {
      return `${this.formatTime(time, 'M月d日')}（${
        this.handleGetNotifyDate(notify)}）${this.$t('提醒')}`;
    },
    showNotify(ignore) {
      return this.form.ignoreReasonType === ignore.ignoreTypeId
      && ignore.notify.notifyDayOfWeeks.length;
    },
    showPathShield(ignore) {
      return ignore.ignoreTypeId === 42;
    },
  },
};
</script>

<style lang="postcss" scoped>
.search-form.main-form.collapse {
  height: 48px;
  overflow: hidden;
}

.reason-type-list {
  padding: 20px;
  background: #fafbfd;

  .ignore-list {
    max-height: 250px;
    padding-left: 2px;
    overflow-y: scroll;
  }

  .reason-type-header {
    line-height: 26px;
  }
}

.notify-tips {
  display: inline-block;
  width: 360px;
  margin-left: 15px;
  overflow: hidden;
  color: #979ba5;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1%;
}

.cc-radio {
  display: block;
  padding-bottom: 15px;
}
</style>
