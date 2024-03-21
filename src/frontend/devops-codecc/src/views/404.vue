<template>
  <div class="exception-wrap">
    <bk-exception class="exception-wrap-item" type="404">
      <span>{{ $t('页面不存在') }}</span>
      <div class="text-wrap">
        <span class="text-btn" @click="refreshPage">{{ $t('刷新页面') }}</span>
        <span class="text-btn" @click="backHome">{{ $t('返回首页') }}</span>
      </div>
    </bk-exception>
  </div>
</template>

<script>
export default {
  name: 'Exception',
  methods: {
    refreshPage() {
      window.location.reload();
    },
    backHome() {
      this.$router.push('/');
    },
  },
};
</script>

<style lang="postcss" scoped>
.exception-wrap .exception-wrap-item {
  padding-top: 60px;
  margin: 10px;
}

.exception-wrap .exception-wrap-item .text-wrap {
  display: flex;
  margin-top: 24px;
  font-size: 14px;
  color: #3A84FF;
  align-items: center;
  justify-content: center;
}

.exception-wrap .exception-wrap-item .text-subtitle {
  margin-top: 14px;
  font-size: 14px;
  color: #979BA5;
  text-align: center;
}

.text-wrap .text-btn {
  margin: 0 5px;
}

.text-wrap .text-btn:hover {
  cursor: pointer;
}
</style>
