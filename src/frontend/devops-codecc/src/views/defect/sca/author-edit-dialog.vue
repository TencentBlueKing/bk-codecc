<template>
  <bk-dialog
    v-model="isShow"
    width="560"
    theme="primary"
    header-position="left"
    :title="dialogTitle"
    :before-close="handleBeforeClose"
  >
    <div class="author-edit">
      <div class="tips" v-if="changeAuthorType === 3">
        <i class="bk-icon icon-info-circle"></i
        >{{ $t('原处理人所有函数都将转给新处理人') }}
      </div>
      <bk-form
        :model="form"
        :label-width="130"
        class="search-form"
      >
        <bk-form-item
          v-if="changeAuthorType !== 2"
          property="sourceAuthor"
          :label="$t('原处理人')"
        >
          <bk-input
            class="input-width"
            v-model="form.sourceAuthor"
            :disabled="changeAuthorType === 1"
            @change="handleSourceAuthorChange"
          ></bk-input>
        </bk-form-item>

        <bk-form-item :label="$t('新处理人')">
          <UserSelector
            allow-create
            class="input-width"
            :value.sync="form.targetAuthor"
            @update:value="handleTargetAuthorChange"
          />
        </bk-form-item>
      </bk-form>
    </div>
    <div class="footer-wrapper" slot="footer">
      <bk-button
        type="button"
        theme="primary"
        :disabled="disabledConfirm"
        :loading="confirmLoading"
        @click.native="handleConfirm"
      >
        {{ confirmBtnText }}
      </bk-button>
      <bk-button
        theme="primary"
        type="button"
        :disabled="confirmLoading"
        @click="isShow = false"
      >
        {{ $t('取消') }}
      </bk-button>
    </div>
  </bk-dialog>
</template>

<script>
import UserSelector from '@/components/user-selector/index.vue';
export default {
  components: {
    UserSelector,
  },
  props: {
    changeAuthorType: {
      type: Number,
    },
    sourceAuthor: {
      type: String,
    },
  },
  data() {
    return {
      isShow: false,
      confirmLoading: false,
      form: {
        sourceAuthor: [],
        targetAuthor: [],
      },
    };
  },
  computed: {
    dialogTitle() {
      return this.$props.changeAuthorType === 1 ? this.$t('修改问题处理人') : this.$t('批量修改问题处理人');
    },
    confirmBtnText() {
      return this.$props.changeAuthorType === 1 ? this.$t('确定') : this.$t('批量修改');
    },
    disabledConfirm() {
      return this.$props.changeAuthorType === 3 || !this.form.targetAuthor.length;
    },
  },
  watch: {
    sourceAuthor(val) {
      this.form.sourceAuthor = val;
    },
  },
  methods: {
    handleBeforeClose() {
      this.$emit('beforeClose');
    },
    handleConfirm() {
      this.form.sourceAuthor = [];
      this.form.targetAuthor = [];
      this.$emit('confirm', this.form);
    },
    handleShow() {
      this.isShow = true;
    },
    handleHide() {
      this.isShow = false;
    },
    handleTargetAuthorChange(val) {
      this.$emit('targetAuthorChange', val);
    },
    handleSourceAuthorChange(val) {
      this.$emit('sourceAuthorChange', val);
    },
  },
};
</script>

<style lang="postcss" scoped>
.input-width {
  width: 290px;
}

.search-form.main-form.collapse {
  height: 48px;
  overflow: hidden;
}

.author-edit {
  padding: 34px 18px 11px;

  .tips {
    position: absolute;
    top: 66px;
    left: 23px;
    color: #979ba5;

    .bk-icon {
      margin-right: 2px;
      color: #ffd695;
    }
  }
}
</style>
