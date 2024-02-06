<template>
  <bk-tab
    :active.sync="active"
    :label-height="42"
    @tab-change="handleTableChange"
    type="unborder-card"
  >
    <bk-tab-panel v-for="(panel, index) in panels" v-bind="panel" :key="index">
    </bk-tab-panel>
  </bk-tab>
</template>

<script>
import { mapState } from 'vuex';
export default {
  props: {
    toolName: {
      type: String,
      default: '',
    },
    toolList: {
      type: Array,
      default: [],
    },
  },
  data() {
    return {
      active: 'defect',
      panels: [
        { name: 'defect', label: this.$t('问题管理') },
        { name: 'report', label: this.$t('数据报表') },
      ],
    };
  },
  computed: {
    ...mapState('tool', {
      toolMap: 'mapList',
    }),
  },
  methods: {
    handleTableChange(value) {
      let { toolName } = this;
      if (!toolName || toolName.includes(',')) {
        toolName = this.toolList[0]?.toolName;
      }
      const toolPattern = this.toolMap[toolName].pattern.toLocaleLowerCase();
      const params = { ...this.$route.params, toolId: toolName };
      if (value === 'defect') {
        this.$router.push({
          name: `defect-${toolPattern}-list`,
          params,
        });
      } else {
        this.$router.push({
          name: `defect-${toolPattern}-charts`,
          params,
        });
      }
    },
  },
};
</script>

<style></style>
