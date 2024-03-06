<template>
  <bk-dialog
    v-model="visible"
    width="480px"
    @after-leave="setItem"
    :theme="'primary'"
  >
    <template slot="header">
      <div>
        <img class="header" src="../../images/group-2.svg" />
      </div>
    </template>
    {{ $t('恭喜你已经切换至V2版本，立即触发一次新的分析吧!') }}
    <div style="padding-bottom: 19px"></div>
    <bk-checkbox
      :true-value="true"
      :false-value="false"
      v-model="neverShow"
      :value="true"
    >
      {{ $t('不再提示') }}
    </bk-checkbox>
    <template slot="footer">
      <bk-button :theme="'primary'" @click="newAnalyze">{{
        $t('立即检查')
      }}</bk-button>
      <bk-button :theme="'default'" @click="closeDialog">{{
        $t('我知道了')
      }}</bk-button>
    </template>
  </bk-dialog>
</template>

<script>
export default {
  name: 'NewAnalyze',
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    neverShow: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {};
  },
  computed: {},
  watch: {
    neverShow: {
      handler() {
        this.$emit('changeItem', this.neverShow);
      },
      deep: true,
    },
  },
  methods: {
    closeDialog() {
      this.$emit('update:visible', false);
    },
    setItem() {
      window.localStorage.setItem('neverShow', JSON.stringify(this.neverShow));
    },
    newAnalyze() {
      this.$emit('newAnalyze');
    },
  },
};
</script>
<style scoped>
.header {
  position: relative;
  right: 24px;
  bottom: 33px;
  margin-bottom: -48px;
}
</style>
