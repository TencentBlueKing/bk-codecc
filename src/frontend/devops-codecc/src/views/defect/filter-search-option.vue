<template>
  <section>
    <div class="default-option">
      <div class="header">{{ $t('系统预置筛选') }}</div>
      <div class="options">
        <div class="option-item" v-for="option in defaultOption" :key="option.id">
          <bk-checkbox v-model="option.isChecked" disabled></bk-checkbox>
          {{ option.name }}
        </div>
      </div>
    </div>
    <div class="custom-option">
      <div class="header">{{ $t('自定义筛选') }}</div>
      <div class="options">
        <div class="option-item" v-for="option in customOption" :key="option.id">
          <bk-checkbox v-model="option.isChecked"></bk-checkbox>
          {{ option.name }}
        </div>
      </div>
    </div>
    <div class="option-footer">
      <span class="btn" @click="handleSelectAll">{{ isSelectAll ? $t('取消全选') : $t('全选') }}</span>
      <span class="btn" @click="handleConfirm">{{ $t('确认') }}</span>
    </div>
  </section>
</template>

<script>
  export default {
    props: {
      defaultOption: {
        type: Array,
        default: () => [],
      },
      customOption: {
        type: Array,
        default: () => [],
      },
    },
    computed: {
      isSelectAll() {
        return this.customOption.every(item => item.isChecked)
      },
    },
    methods: {
      handleConfirm() {
        this.$emit('confirm')
      },
      handleSelectAll() {
        this.$emit('selectAll')
      },
    },
  }
</script>

<style lang="postcss" scoped>
  .custom-option,
  .default-option {
    width: 100px;
    z-index: 9999;
    .header {
        text-align: center;
        font-size: 12px;
        color: #979BA5;
        padding: 8px 0;
    }
    .option-item {
      height: 30px;
      line-height: 30px;;
      padding-left: 13px;
      color: #63656E;
      font-size: 12px;
      &:hover {
        background-color: #EAF3FF;
      }
    }
  }
  .option-footer {
    display: flex;
    justify-content: space-evenly;
    height: 30px;
    line-height: 30px;
    text-align: center;
    border-top: 1px solid #F0F1F5;
    .btn {
      color: #3A84FF;
      cursor: pointer;
    }
  }
</style>
