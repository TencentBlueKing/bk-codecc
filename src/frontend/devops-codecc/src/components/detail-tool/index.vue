<template>
  <div class="detail-card">
    <tool-status-card
      v-for="(item, index) in selectedTypeData"
      :key="index"
      :data="item"
    >
    </tool-status-card>
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
