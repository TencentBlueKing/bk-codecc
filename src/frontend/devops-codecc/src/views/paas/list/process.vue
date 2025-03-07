<template>
  <bk-dialog
    :title="statusMap[formData.processProgress]"
    header-position="left"
    v-model="isProcessShow"
    :confirm-fn="handleConfirm"
  >
    <p v-if="formData.processProgress === 1" style="color: #979ba5;">{{ $t('后续用户将不再出现此类误报') }}</p>
    <bk-form ref="form" :model="formData" :rules="rules" class="process-form" form-type="vertical">
      <template v-if="formData.processProgress === 1 || formData.processProgress === 4">
        <bk-form-item property="processReason" :label="$t('补充说明')">
          <bk-input type="textarea" :maxlength="255" v-model="formData.processReason"></bk-input>
        </bk-form-item>
        <bk-form-item property="issueLink" :label="$t('单据链接')">
          <bk-input v-model="formData.issueLink" :placeholder="$t('请输入TAPD/Github Issue链接')"></bk-input>
        </bk-form-item>
      </template>
      <template v-else>
        <bk-form-item property="processReasonType" :label="$t('类型')" required>
          <bk-radio-group v-model="formData.processReasonType">
            <bk-radio
              v-for="item in formData.processProgress === 2 ? noToolReasonList : otherReasonList"
              :key="item.id"
              :id="item.id"
              :value="item.id">
              {{ item.name }}
            </bk-radio>
          </bk-radio-group>
        </bk-form-item>
        <bk-form-item property="processReason" :label="$t('原因')" :required="formData.processReasonType === 6">
          <bk-input type="textarea" :maxlength="255" v-model="formData.processReason"></bk-input>
        </bk-form-item>
      </template>
    </bk-form>
  </bk-dialog>
</template>

<script>
import { bus } from '@/common/bus';
import { mapState } from 'vuex';

export default {
  name: 'Process',
  data() {
    return {
      row: {},
      isProcessShow: false,
      statusMap: {
        1: this.$t('已优化工具'),
        2: this.$t('非工具原因'),
        4: this.$t('跟进中'),
        3: this.$t('其他'),
      },
      /**
       * 误报的处理原因类型:
       * 1. 用户误操作
       * 2. 用户不配合
       * 3. 无法查看问题代码
       * 4. 受限于技术架构
       * 5. 修复成本过高
       * 6. 其他
       * 其中, 1 是 "非工具原因" 的误报的原因, 2-5 是 "其他" 的误报的原因, 6 是通用的原因.
       */
      noToolReasonList: [
        {
          id: 1,
          name: this.$t('用户误操作'),
        },
        {
          id: 6,
          name: this.$t('其他'),
        },
      ],
      otherReasonList: [
        {
          id: 2,
          name: this.$t('用户不配合'),
        },
        {
          id: 3,
          name: this.$t('无法查看问题代码'),
        },
        {
          id: 4,
          name: this.$t('受限于技术架构'),
        },
        {
          id: 5,
          name: this.$t('修复成本过高'),
        },
        {
          id: 6,
          name: this.$t('其他'),
        },
      ],
      formData: {
        processProgress: 3,
        processReasonType: 6,
        processReason: '',
        issueLink: '',
      },
    };
  },
  computed: {
    rules() {
      return {
        processReason: [
          {
            required: this.formData.processProgress !== 1 && this.formData.processReasonType === 6,
            message: this.$t('请输入原因'),
            trigger: 'blur',
          },
        ],
      };
    },
  },
  watch: {
    formData: {
      handler(val) {
        this.$nextTick(() => {
          this.$refs.form.clearError();
        });
      },
      deep: true,
    },
  },
  methods: {
    handleDefect(id, row) {
      this.row = row;
      this.formData.processProgress = id;
      this.formData.processReason = row.processReason;
      this.formData.issueLink = row.issueLink;
      this.processProgress = id;
      if (id === 0) {
        this.processDefect(this.row);
        return;
      }
      this.isProcessShow = true;
    },
    handleConfirm() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          this.processDefect(this.row);
        }
      });
    },
    processDefect(row) {
      const payload = {
        toolName: this.$route.params.toolName,
        entityId: this.row.entityId,
        processProgress: this.formData.processProgress,
        processReasonType: this.formData.processReasonType,
        processReason: this.formData.processReason,
        issueLink: this.formData.issueLink,
      };
      row.processProgress = null;
      row.modify = true;
      this.$store.dispatch('paas/processDefect', payload)
        .then((res) => {
          this.isProcessShow = false;
          if (res.data) {
            this.$bkMessage({
              theme: 'success',
              message: this.$t('操作成功'),
            });
            bus.$emit('refresh-paas-count');
            row.processProgress = payload.processProgress;
            row.processReasonType = payload.processReasonType;
            row.processReason = payload.processReason;
            row.issueLink = payload.issueLink;
            row.modify = false;
          } else {
            this.$bkMessage({
              theme: 'error',
              message: this.$t('操作失败'),
            });
          }
        });
    },
  },
};
</script>

<style lang="postcss" scoped>
.process-form {
  margin-top: 12px;

  >>> .bk-form-radio {
    display: block;
    height: 32px;
    font-size: 12px;
    line-height: 32px;
  }
}

::v-deep .bk-dialog-header {
  padding: 3px 24px 0 !important;
}
</style>
