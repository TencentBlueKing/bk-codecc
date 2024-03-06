<template>
  <div class="record-list" v-if="records.length">
    <div class="search-area">
      <input v-bk-focus="1" v-model.trim="searchValue" />
      <i class="bk-icon icon-search"></i>
    </div>
    <ul>
      <li
        v-for="item in filterRecords"
        :title="item[paramName]"
        :key="item[paramId]"
        :class="{ active: activeId === item[paramId] }"
        @click.stop="handleRecordClick(item)"
      >
        {{ item[paramName] }}
      </li>
    </ul>
  </div>
</template>

<script>
export default {
  name: 'CrumbRecords',
  props: {
    records: {
      type: Array,
      default: [],
    },
    handleRecordClick: {
      type: Function,
    },
    activeId: {
      type: String,
      default: '',
    },
    paramId: {
      type: String,
      default: 'id',
    },
    paramName: {
      type: String,
      default: 'name',
    },
  },
  data() {
    return {
      searchValue: '',
    };
  },
  computed: {
    filterRecords() {
      return this.records.filter((item) => {
        const lcName = item[this.paramName]
          ? item[this.paramName].toLowerCase()
          : '';
        const lcSearchValue = this.searchValue.toLowerCase();
        return lcName.indexOf(lcSearchValue) > -1;
      });
    },
  },
};
</script>

<style lang="postcss" scoped>
.record-list {
  position: absolute;
  top: 52px;
  left: 142px;
  z-index: 100;
  width: 222px;
  background: white;
  border-radius: 2px;
  box-shadow: 0 0 8px 1px rgb(0 0 0 / 10%);

  .search-area {
    position: relative;
    padding: 5px;
    line-height: 32px;
    cursor: default;
    border-bottom: 1px solid #e5e5e5;

    input {
      width: 100%;
      height: 32px;
      padding: 10px;
      font-size: 14px;
      line-height: 32px;
      color: #63656e;
      background-color: #fafbfd;
      border: 1px solid #dde4eb;
      border-radius: 2px;
      outline: none;
      box-shadow: none;
    }

    > i.icon-search {
      position: absolute;
      top: 5px;
      right: 14px;
      height: 30px;
      line-height: 30px;
      color: #ccc;
    }
  }

  ul {
    max-height: 360px;
    overflow: auto;

    > li {
      width: 100%;
      height: 42px;
      padding: 0 10px;
      overflow: hidden;
      font-size: 14px;
      line-height: 42px;
      text-align: left;
      text-overflow: ellipsis;
      white-space: nowrap;
      cursor: pointer;
      background-color: #fff;
      border-right: #c3cdd7;
      border-left: #c3cdd7;

      /* @include ellipsis(); */

      &:hover,
      &.active {
        color: #3c96ff;
        background-color: #eef6fe;
      }
    }
  }
}
</style>
