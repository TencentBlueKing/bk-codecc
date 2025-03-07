<template>
  <bk-dropdown-menu
    class="dropdown-menu"
    trigger="click"
    ref="dropdown"
    @show="isDropdownShow = true"
    @hide="isDropdownShow = false">
    <bk-button
      slot="dropdown-trigger"
      class="dropdown-trigger"
      :class="{ 'unselect': !value.length }"
    >
      <div @mouseenter="isHoverShow = true" @mouseleave="isHoverShow = false">
        <div class="selected-name" :title="names">{{ names }}</div>
        <i
          v-if="!isHoverShow || !value.length"
          :class="[
            'bk-icon icon-angle-down dropdown-icon',
            { 'icon-flip': isDropdownShow },
          ]"></i>
        <i v-else class="bk-icon icon-close-circle-shape dropdown-icon" @click.stop="handleClear"></i>
      </div>
    </bk-button>
    <div slot="dropdown-content" class="dropdown-content" @click.stop>
      <bk-input behavior="simplicity" :left-icon="'bk-icon icon-search'" v-model="searchVal"></bk-input>
      <bk-big-tree
        class="tree-content"
        :data="treeData"
        show-checkbox
        ref="tree"
        :default-checked-keys="ids"
        :default-checked-nodes="defaultNodes"
        @check-change="handleCheckChange"
      ></bk-big-tree>
    </div>
  </bk-dropdown-menu>
</template>

<script>
export default {
  name: 'OrgTree',
  props: {
    value: {
      type: Array,
      default: () => [],
    },
  },
  data() {
    return {
      ids: [],
      isDropdownShow: false,
      isHoverShow: false,
      searchVal: '',
      treeData: [],
      defaultNodes: []
    };
  },
  computed: {
    computedIds() {
      const uniqueIds = new Set();
      this.ids.forEach((itemId) => {
        const node = this.$refs.tree.getNodeById(itemId);
        const hasParents = node.parents && node.parents.length > 0;
        // 有parents，说明是子节点，找到第一个选中的父节点
        if (hasParents) {
          const firstCheckedParent = node.parents.find(parent => parent.checked);
          const parentId = firstCheckedParent ? firstCheckedParent.id : node.id;
          uniqueIds.add(parentId);
        } else {
          uniqueIds.add(node.id);
        }
      });
      return Array.from(uniqueIds);
    },
    names() {
      const ids = this.computedIds.map(id => this.getIdMap(id));
      this.$emit('input', ids);
      return this.computedIds.map(id => this.getNameById(id)).join(',') || this.$t('请选择');
    },
  },
  watch: {
    searchVal(val) {
      this.$refs.tree && this.$refs.tree.filter(val);
    },
  },
  created() {
    this.ids = [...this.value];
    this.getDept();
  },
  methods: {
    handleCheckChange(id, checked) {
      this.ids = [...id];
    },
    getDept() {
      this.$store.dispatch('getDeptTree').then((res) => {
        this.treeData = res.treeData || [];
      });
    },
    getIdMap(id) {
      const node = this.$refs.tree.getNodeById(id);
      const parents = node.parents?.map(parent => parent.id) ?? [];
      return [...parents, node.id];
    },
    getNameById(id) {
      const node = this.$refs.tree.getNodeById(id);
      const parentNames = node.parents?.map(parent => parent.name) ?? [];
      return [...parentNames, node.name].join('/');
    },
    handleClear() {
      this.ids = [];
      this.$emit('input', []);
      this.$refs.tree && this.$refs.tree.removeChecked({ emitEvent: false });
    },
    showDropdownByIds(ids) {
      // 展开菜单
      this.$refs.dropdown.show()
      // 获取部门data
      this.$store.dispatch('getDeptTree').then((res) => {
        this.treeData = res.treeData || [];
        // 用异步方法给bk-big-tree赋值
        this.$refs.tree.setData(this.treeData)
        // 设置默认勾选值
        this.defaultNodes = ids
        // 手动触发更新names
        this.handleCheckChange(ids)
      });
    }
  },
};
</script>

<style lang="postcss" scoped>
.dropdown-trigger {
  width: 100%;
  padding-left: 10px;
  text-align: left;

  .selected-name {
    display: inline-block;
    max-width: 180px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &.unselect {
    color: #c3cdd7;
  }

  .dropdown-icon {
    position: absolute;
    top: 7px;
    right: 2px;
    color: #979ba5;
  }

  .icon-close-circle-shape {
    right: 3px;
    font-size: 14px;
    color: #c4c6cc;
    transition: none;
  }
}

.dropdown-content {
  width: 300px;
}

.tree-content {
  max-height: 300px;
}
</style>
