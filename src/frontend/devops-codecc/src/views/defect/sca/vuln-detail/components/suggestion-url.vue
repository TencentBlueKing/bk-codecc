<template>
  <ul class="suggestion-url">
    <li v-for="link in list" :key="link.name">
      <bk-link
        class="suggestion-link"
        :href="link.url"
        target="_blank">
        {{ link.url }}
      </bk-link>
    </li>
    <bk-button
      v-if="source.length > maxShowRow"
      text
      class="fold-button"
      theme="primary"
      @click="handleToggle">
      {{ toggleText }}
    </bk-button>
  </ul>
</template>

<script>
export default {
  props: {
    source: {
      type: Array,
    },
  },
  data() {
    return {
      isFold: true,
      maxShowRow: 6,
    };
  },
  computed: {
    toggleText() {
      return this.isFold ? this.$t('查看全部') : this.$t('收起');
    },
    list() {
      if (this.isFold) {
        return this.source.slice(0, this.maxShowRow);
      }
      return this.source;
    },
  },
  methods: {
    handleToggle() {
      this.isFold = !this.isFold;
    },
  },
};
</script>

<style lang="postcss" scoped>
.suggestion-url {
  li {
    padding: 12px 0;
    list-style: inside;
    border-bottom: 1px solid #EFF1F4;

    &::marker {
      color: #000;
    }

    .suggestion-link {
      color: #3D9EDF;
    }
  }

  .fold-button {
    margin-top: 12px;
  }
}
</style>
