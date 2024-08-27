<template>
  <section class="list">
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
};
</script>

<style lang="postcss" scoped>
.list {
  min-height: 100vh;
  padding: 24px;
  color: #63656E;
  background: #F5F6FA;
}
</style>
