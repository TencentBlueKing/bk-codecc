<template>
  <div>{{ formatSeconds(animateTime) }}</div>
</template>

<script>
import { formatSeconds } from '@/common/util';
export default {
  name: 'TickTime',
  props: {
    time: {
      type: [String, Number],
      default: 0,
    },
    startTime: {
      type: [String, Number],
      default: Date.now(),
    },
  },
  data() {
    return {
      formatSeconds,
      setIntervalId: null,
      animateTime: Number(this.time),
    };
  },
  created() {
    this.addTime();
  },
  beforeDestroy() {
    clearInterval(this.setIntervalId);
  },
  methods: {
    addTime() {
      const calcTime = Date.now() - this.startTime;
      this.animateTime = calcTime > this.time ? calcTime : this.time;
      this.setIntervalId = setInterval(() => {
        this.animateTime = this.animateTime + 1000;
      }, 1000);
    },
  },
};
</script>

<style></style>
