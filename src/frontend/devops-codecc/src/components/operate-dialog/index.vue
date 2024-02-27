<template>
  <bk-dialog
    v-model="visible"
    width="640"
    theme="primary"
    header-position="left"
    footer-position="center"
    :position="{ top: 50, left: 5 }"
    :title="$t('键盘操作指引')"
  >
    <div class="operate-txt operate-txt-1">1. {{ $t('列表') }}</div>
    <div>
      <img
        v-if="isEn"
        style="width: 592px"
        src="../../images/operate-1-en.png"
      />
      <img v-else style="width: 592px" src="../../images/operate-1.png" />
    </div>
    <div class="operate-txt operate-txt-2">2. {{ $t('问题详情') }}</div>
    <div>
      <img
        v-if="isEn"
        style="width: 592px"
        src="../../images/operate-2-en.png"
      />
      <img v-else style="width: 592px" src="../../images/operate-2.png" />
    </div>
    <div class="operate-footer" slot="footer">
      <bk-button theme="primary" @click.native="handleClick">
        {{ $t('关闭') }}
      </bk-button>
    </div>
  </bk-dialog>
</template>

<script>
import { language } from '../../i18n';

export default {
  name: 'OperateDialog',
  props: {
    visible: Boolean,
  },
  data() {
    return {};
  },
  computed: {
    isEn() {
      return language === 'en';
    },
  },
  watch: {
    visible(value) {
      if (!value) {
        window.localStorage.setItem('opreate-keyboard-20220411', '1');
      }
      this.$emit('update:visible', value);
    },
  },
  methods: {
    handleClick() {
      this.$emit('update:visible', false);
    },
  },
};
</script>
