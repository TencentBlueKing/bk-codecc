<template>
  <bk-tab
    :active.sync="currentNavTab"
    :label-height="42"
    @tab-change="handleTableChange"
    type="unborder-card"
  >
    <bk-tab-panel v-for="(panel, index) in panels" v-bind="panel" :key="index">
    </bk-tab-panel>
  </bk-tab>
</template>

<script>
export default {
  props: {},
  data() {
    return {
      panels: [
        { name: 'defect', label: this.$t('代码问题') },
        { name: 'ccn', label: this.$t('圈复杂度') },
      ],
    };
  },
  computed: {
    currentNavTab() {
      const routeName = this.$route.name;
      const navMap = {
        'project-defect-list': 'defect',
        'project-ccn-list': 'ccn',
      };
      return navMap[routeName];
    },
  },
  methods: {
    handleTableChange(value) {
      if (value === 'defect') {
        this.$router.push({
          name: 'project-defect-list',
          query: { author: this.$store.state.user.username },
        });
      } else if (value === 'ccn') {
        this.$router.push({
          name: 'project-ccn-list',
          query: { author: this.$store.state.user.username },
        });
      }
    },
  },
};
</script>

<style></style>
