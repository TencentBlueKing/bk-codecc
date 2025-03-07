<template>
  <bk-select
    :disabled="false"
    v-model="value"
    style="width: 100px;"
    ext-cls="version-select"
    ext-popover-cls="select-popover-custom"
    :clearable="true"
  >
    <bk-option
      v-for="option in list"
      :key="option"
      :id="option"
      :name="option">
    </bk-option>
  </bk-select>
</template>
<script>

export default {
  props: {
    stage: {
      type: Number,
      default: 1,
    },
  },
  data() {
    return {
      value: 1,
      list: [],
    };
  },
  watch: {
    value(val) {
      this.$emit('change', val);
    },
  },
  created() {
    this.$store.dispatch('test/listVersion', { toolName: this.$route.params.toolName, stage: this.stage })
      .then((res) => {
        this.list = res.filter(item => item !== this.$route.query.version) || [];
        this.value = this.list[0] ?? '';
      });
  },
};
</script>

<style lang="postcss" scoped>
.version-select {
  width: 120px;
  height: 22px;
  line-height: 22px;

  >>> .bk-select-angle {
    top: 0;
  }

  >>> .bk-select-clear {
    top: 4px;
  }

  >>> .bk-select-name {
    height: 20px;
  }
}
</style>
