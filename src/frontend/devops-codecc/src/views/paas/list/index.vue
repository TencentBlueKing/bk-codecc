<template>
  <section class="list" :style="{ padding: isIframe ? '0' : '24px' }">
    <search ref="search"></search>
    <table-list></table-list>
  </section>
</template>

<script>
import Search from './search.vue';
import TableList from './list.vue';

export default {
  name: 'List',
  components: {
    Search,
    TableList,
  },
  computed: {
    isIframe() {
      return window.self !== window.top;
    },
  },
};
</script>

<style lang="postcss" scoped>
.list {
  min-height: 100vh;
  color: #63656E;
  background: #F5F6FA;
}
</style>
