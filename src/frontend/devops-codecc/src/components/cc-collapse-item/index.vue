<template>
  <div class="cc-collapse-item">
    <bk-input
      class="collapse-search"
      :placeholder="$t('搜索')"
      :clearable="true"
      :right-icon="'bk-icon icon-search'"
      v-if="needSearch"
      v-model="value"
    >
    </bk-input>
    <ul>
      <li
        class="collapse-li"
        v-for="item in list"
        :key="item.key"
        :class="{
          'is-selected': selected.includes(item.key),
        }"
        @click="handleClick(item.key, id)"
      >
        <span
          :class="['severity-label', `severity-${item.key}`]"
          :title="item.name || item.key"
        >{{ item.name || item.key }}</span
        >
        <span>{{ item.count }}</span>
      </li>
      <template v-if="hasMore">
        <li
          v-if="hasMoreFlag"
          class="collapse-li cc-click"
          @click="hasMoreFlag = false"
        >
          {{ $t('更多') }}...
        </li>
        <li v-else class="collapse-li cc-click" @click="hasMoreFlag = true">
          {{ $t('收起') }}
        </li>
      </template>
    </ul>
  </div>
</template>

<script>
export default {
  name: 'CcCollapseItem',
  props: {
    id: {
      type: String,
      required: true,
    },
    needSearch: {
      type: Boolean,
      default: false,
    },
    data: {
      type: Array,
      default: [],
    },
    maxLength: {
      type: Number,
      default: 7,
    },
    selected: {
      type: Array,
      default: [],
    },
    redPoint: {
      type: Array,
      default: [],
    },
  },
  data() {
    return {
      searchValue: '',
      hasMoreFlag: true,
    };
  },
  computed: {
    list() {
      const { data, maxLength, hasMoreFlag } = this;
      let list = data;
      if (maxLength && hasMoreFlag) {
        list = data.slice(0, maxLength);
      }
      return list;
    },
    filterRecords() {
      return this.records.filter((item) => {
        const lcName = item[this.paramName]
          ? item[this.paramName].toLowerCase()
          : '';
        const lcSearchValue = this.searchValue.toLowerCase();
        return lcName.indexOf(lcSearchValue) > -1;
      });
    },
    hasMore() {
      return this.data.length > 7;
    },
  },
  created() {
    const hasLatterIndex = this.selected.some(item => this.data.findIndex(val => val.key === item) > this.maxLength);
    if (hasLatterIndex) this.hasMoreFlag = false;
  },
  methods: {
    handleClick(value, id) {
      this.$emit('handleSelect', value, id);
    },
  },
};
</script>

<style lang="postcss" scoped>
.collapse-search {
  padding: 3px 0;
}

.collapse-li {
  display: flex;
  height: 30px;
  padding: 0 5px;
  margin: 2px 0 2px 8px;
  font-size: 12px;
  line-height: 30px;
  color: #63656d;
  cursor: pointer;
  justify-content: space-between;

  &.cc-click {
    color: #3c96ff;
  }

  &:hover {
    /* color: #3a84ff; */
    background: #f0f1f5;
  }

  &.is-selected {
    background: #e1ecff;
  }

  .severity-label {
    overflow: hidden;
    text-overflow: ellipsis;
  }
}
</style>
