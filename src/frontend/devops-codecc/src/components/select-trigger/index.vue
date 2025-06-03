<template>
  <div class="container">
    <template v-if="isHasData">
      <bk-user-display-name :user-id="currentValue"></bk-user-display-name>
      <i class="codecc-icon icon-close-circle-fill close" @click="handleClearValue"></i>
    </template>
    <span v-else class="select-trigger-placeholder">{{ $t('请选择') }}</span>
  </div>
</template>

<script>
export default {
  props: {
    value: {
      type: String,
    },
  },
  data() {
    return {
      currentValue: this.value,
    };
  },
  computed: {
    isHasData() {
      return this.currentValue !== ''
        && this.currentValue !== undefined
        && this.currentValue !== null
        && this.currentValue?.length !== 0;
    },
  },
  watch: {
    value(newValue) {
      this.currentValue = newValue;
    },
    currentValue(newVal) {
      this.$emit('input', newVal);
      this.$emit('update:value', newVal);
    },
  },
  methods: {
    handleClearValue() {
      this.currentValue = '';
    },
  },
};
</script>

<style lang="postcss" scoped>
.container {
  padding-inline: 10px;
  display: flex;
  align-items: center;
  min-height: 32px;

  &:hover {
    .close {
      position: absolute;
      right: 6px;
      display: unset;
      color: #c4c6cc;

      &:hover {
        color: #979ba5;
      }
    }
  }

  .close {
    display: none;
  }

  .select-trigger-placeholder {
    color: #c3cdd7;
  }
}

</style>
