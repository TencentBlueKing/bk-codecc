<template>
  <div :class="['empty', `empty-${size}`]">
    <img src="../../images/empty.png" class="empty-img" />
    <div class="title">{{ title }}</div>
    <div class="desc" v-if="desc">{{ desc }}</div>
    <template v-if="$slots.action">
      <div class="action">
        <slot name="action"></slot>
      </div>
    </template>
    <template>
      <slot></slot>
    </template>
  </div>
</template>

<script>
export default {
  name: 'Empty',
  props: {
    title: {
      type: String,
      default: 'Empty',
    },
    desc: {
      type: String,
      default: '',
    },
    size: {
      type: String,
      default: 'normal',
      validator(value) {
        if (['normal', 'small'].indexOf(value) === -1) {
          console.error(`size property is not valid: '${value}'`);
          return false;
        }
        return true;
      },
    },
  },
  data() {
    return {};
  },
};
</script>

<style lang="postcss">
.empty {
  text-align: center;

  .icon {
    margin: 0 auto;
    background: url('../../images/empty.png') no-repeat 50% 0;
    background-size: contain;
  }

  &.empty-normal {
    .icon {
      width: 91px;
      height: 57px;
    }
  }

  &.empty-small {
    .icon {
      width: 64px;
      height: 40px;
    }

    .title {
      margin-top: 0;
      font-size: 12px;
    }
  }

  .title {
    margin-top: 16px;
    font-size: 20px;
    color: #333;
  }

  .desc {
    margin: 8px 0;
    font-size: 14px;
  }

  .action {
    margin-top: 12px;
  }

  .empty-img {
    width: 220px;

    /* height: 56px; */
  }
}
</style>
