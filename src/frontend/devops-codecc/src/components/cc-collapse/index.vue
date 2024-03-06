<template>
  <div class="cc-collapse">
    <bk-collapse v-model="activeName">
      <bk-collapse-item
        v-for="item in searchFormat"
        :key="item.name"
        :name="item.name"
      >
        {{ nameMap(item.name) }}
        <div slot="content">
          <cc-collapse-item
            :need-search="false"
            :id="item.name"
            :data="item.checkerCountList"
            :max-length="item.maxLength"
            :selected="selectedList[item.name]"
            @handleSelect="handleSelect"
          >
          </cc-collapse-item>
        </div>
      </bk-collapse-item>
    </bk-collapse>
  </div>
</template>

<script>
import { mapState } from 'vuex';
import ccCollapseItem from '@/components/cc-collapse-item';

export default {
  name: 'CcCollapse',
  components: {
    ccCollapseItem,
  },
  props: {
    isCheckerSet: {
      type: Boolean,
      default: false,
    },
    search: {
      type: Array,
      default: [],
    },
    activeName: {
      type: Array,
      default: [],
    },
    updateActiveName: {
      type: Function,
    },
  },
  data() {
    return {
      selectedList: {},
    };
  },
  computed: {
    ...mapState('tool', {
      toolMap: 'mapList',
    }),
    searchFormat() {
      const list = this.search.filter(item => item.name !== 'total');
      const { selectedList } = this;
      const severityMap = [
        '',
        this.$t('严重'),
        this.$t('一般'),
        '',
        this.$t('提示'),
      ];
      const editableMap = {
        false: this.$t('不可修改'),
        true: this.$t('可修改'),
      };
      list.forEach((item) => {
        if (item.name === 'toolName') {
          item.checkerCountList.forEach((count) => {
            count.name = this.toolMap[count.key] && this.toolMap[count.key].displayName;
          });
        } else if (item.name === 'severity') {
          item.checkerCountList.forEach((count) => {
            count.name = severityMap[count.key];
          });
        } else if (item.name === 'editable') {
          item.checkerCountList.forEach((count) => {
            count.name = editableMap[count.key];
          });
        }
        item.selectedList = selectedList[item.name] || [];
      });
      return list;
    },
    checkerSetQueryStorage() {
      return localStorage.getItem('checkerSetQueryObj')
        ? JSON.parse(localStorage.getItem('checkerSetQueryObj'))
        : {};
    },
  },
  watch: {
    activeName(newVal) {
      this.$emit('updateActiveName', newVal);
    },
  },
  created() {
    if (this.isCheckerSet && Object.keys(this.checkerSetQueryStorage).length) {
      this.selectedList = this.checkerSetQueryStorage;
      this.$emit('handleSelect', this.selectedList);
    }
  },
  methods: {
    nameMap(name) {
      const checkerMap = {
        checkerLanguage: this.$t('适用语言'),
        checkerCategory: this.$t('类别'),
        toolName: this.$t('multi.工具'),
        tag: this.$t('multi.标签'),
        severity: this.$t('严重级别'),
        editable: this.$t('参数策略'),
        checkerRecommend: this.$t('来源'),
        checkerSetSelected: this.$t('使用状态'),
      };
      const checkerSetMap = {
        checkerSetLanguage: this.$t('适用语言'),
        checkerSetCategory: this.$t('类别'),
        toolName: this.$t('multi.工具'),
        checkerSetSource: this.$t('来源'),
      };
      return this.isCheckerSet ? checkerSetMap[name] : checkerMap[name];
    },
    handleSelect(value, id) {
      let selected = this.selectedList[id] || [];
      if (selected.includes(value)) {
        selected = selected.filter(item => item !== value);
      } else {
        selected.push(value);
      }
      const selectedList = {};
      selectedList[id] = selected;
      this.selectedList = Object.assign({}, this.selectedList, selectedList);
      this.$emit('handleSelect', this.selectedList);
    },
    handleClear() {
      this.selectedList = {};
    },
  },
};
</script>

<style lang="postcss" scoped>
>>> .bk-collapse-item .bk-collapse-item-header {
  color: #313238;
}
</style>
