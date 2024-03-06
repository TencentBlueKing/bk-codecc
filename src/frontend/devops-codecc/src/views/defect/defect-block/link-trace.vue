<template>
  <div class="ml30">
    <div v-for="trace in list" :key="trace.id">
      <div
        class="item-trace"
        :class="{ active: activeId === trace.id }"
        @click.stop="clickTrace(trace)"
      >
        <span class="item-index">{{ trace.index }}</span>
        <span :title="trace.message">{{ trace.tag || trace.message }}</span>
      </div>
      <link-trace
        :list="trace.linkTrace"
        :active-id="activeId"
        @clickTrace="clickTrace"
      ></link-trace>
    </div>
  </div>
</template>

<script>
import LinkTrace from './link-trace';
export default {
  name: 'LinkTrace',
  components: {
    LinkTrace,
  },
  props: {
    list: Array,
    activeId: String,
  },
  data() {
    return {};
  },
  methods: {
    clickTrace(trace) {
      this.$emit('clickTrace', trace);
    },
  },
};
</script>

<style lang="postcss" scoped>
.item-trace {
  line-height: 30px;
  word-break: break-all;

  &.active {
    color: #3a84ff;
    background: #e1ecff;
  }
}
</style>
