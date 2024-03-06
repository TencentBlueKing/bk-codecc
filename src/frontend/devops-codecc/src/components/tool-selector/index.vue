<template>
  <ul>
    <li
      v-for="item in list"
      :class="picked && picked.includes(item.id) ? 'active' : ''"
      @click="selectedItem(item.id)"
      class="settings-trigger-item"
      :key="item.id"
    >
      <img class="item-pic" :src="item.logo" />
      <p :title="item.name">{{ item.name }}</p>
    </li>
  </ul>
</template>

<script>
export default {
  name: 'WeekSelector',
  props: {
    picked: {
      type: Array,
      default: () => [],
    },
    list: {
      type: Array,
      default: () => [],
    },
  },
  data() {
    return {};
  },
  computed: {},
  methods: {
    selectedItem(id) {
      this.$emit('select', id);
    },
  },
};
</script>

<style lang="postcss" scoped>
@import url('../../css/variable.css');

.settings-trigger-item {
  position: relative;
  display: inline-block;
  width: 89px;
  height: 87px;
  margin-right: 8px;
  margin-bottom: 8px;
  font-size: 12px;
  line-height: 12px;
  text-align: center;
  cursor: pointer;
  background-color: #f7f9fb;
  border: 1px solid #d9e2e8;
  border-radius: 2px;

  .item-pic {
    width: 64px;
    height: 64px;
  }

  p {
    height: 14px;
    overflow: hidden;
    font-size: 14px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.active {
  color: $goingColor;
  border: 1px solid $goingColor;

  &::before {
    position: absolute;
    right: -1px;
    bottom: -1px;
    width: 0;
    height: 0;
    border-color: transparent transparent #3a84ff;
    border-style: solid;
    border-width: 0 0 14px 20px;
    content: '';
  }

  &::after {
    position: absolute;
    right: 0;
    bottom: -2px;
    font-family: bk-icons-linear !important;
    font-size: 12px;
    font-weight: bold;
    color: #fff;
    content: '\e977';
  }
}
</style>
