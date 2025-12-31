<template>
  <ul class="tabs-list">
    <li
      v-for="tab in list"
      :key="tab.name"
      class="tab-item"
      :class="tab.name === active && 'active-item'"
      @click="handleTabChange(tab.name)"
    >
      <span>{{ tab.label }}</span>
    </li>
  </ul>
</template>

<script>
export default {
  props: {
    list: {
      type: Array,
      default: () => [],
    },
    value: {
      type: String,
      default: '',
    },
  },
  data() {
    return {};
  },
  computed: {
    active() {
      return this.value || '';
    },
  },
  methods: {
    handleTabChange(newVal) {
      this.$emit('update:value', newVal);
    },
  },
};
</script>
<style lang="postcss" scoped>
.tabs-list {
  position: relative;
  display: flex;
  font-size: 14px;
  font-weight: bold;
  align-items: center;

  &::after {
    position: absolute;
    bottom: 0;
    left: 0;
    width: 100%;
    height: 1px;
    background-color: #e5e7eb;
    content: '';
  }

  .tab-item {
    position: relative;
    display: flex;
    height: 42px;
    padding: 0 16px;
    line-height: 14px;
    color: #63656e;
    cursor: pointer;
    align-items: center;

    &:not(:last-child)::before {
      position: absolute;
      top: 12px;
      right: 0;
      width: 1px;
      height: 18px;
      margin-left: 8px;
      background-color: #e5e7eb;
      content: '';
    }
  }

  .active-item {
    position: relative;
    color: #3a84ff;

    &::after {
      position: absolute;
      bottom: 0;
      left: 0;
      width: 100%;
      height: 3px;
      background-color: #3a84ff;
      content: '';
    }
  }
}
</style>
