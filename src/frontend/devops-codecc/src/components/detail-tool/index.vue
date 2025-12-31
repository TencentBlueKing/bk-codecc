<template>
  <div class="detail-card">
    <bk-exception
      v-if="selectedTypeData.length === 0"
      type="empty"
      scene="part"
    />
    <template v-else>
      <tool-status-card
        v-for="(item, index) in selectedTypeData"
        :key="index"
        :data="item"
      >
      </tool-status-card>
    </template>
  </div>
</template>
<script>
import toolStatusCard from '@/components/tool-status-card';

export default {
  components: {
    toolStatusCard,
  },
  props: {
    selectedTypeData: {
      type: Array,
      default: [],
    },
  },
};
</script>
