<template>
  <div class="date-picker-dropdown" v-bk-clickoutside="hiddenDropdown">
    <bk-button type="primary" class="select-button" @click="toggleShow">
      <div
        class="data-range-input"
        :class="{ unselect: !localVal }"
        :title="localVal"
      >
        {{ localVal ? localVal : $t('请选择') }}
      </div>
      <i :class="['bk-icon icon-angle-down', { 'icon-flip': visible }]"></i>
    </bk-button>
    <div class="picker-dropdown-content" v-if="visible">
      <div class="bk-button-group">
        <bk-button
          v-for="(item, index) in filterTypeList"
          :key="index"
          :class="selected === item.key ? 'is-selected' : ''"
          @click="selectType(item.key)"
        >{{ item.name }}</bk-button
        >
      </div>
      <bk-date-picker
        class=""
        v-model="localDaterange"
        type="daterange"
      ></bk-date-picker>
      <div class="content-ft">
        <bk-button theme="primary" @click="handleConfirm">{{
          $t('确定')
        }}</bk-button>
        <bk-button @click="hiddenDropdown">{{ $t('取消') }}</bk-button>
        <bk-button class="clear-btn" @click="handleClear">{{
          $t('清空选择')
        }}</bk-button>
      </div>
    </div>
  </div>
</template>

<script>
import { format } from 'date-fns';

export default {
  props: {
    dateRange: {
      type: String,
      default: '',
    },
    selected: {
      type: String,
      default: 'createTime',
    },
    statusUnion: {
      type: Array,
      default: [],
    },
    handleChange: Function,
  },
  data() {
    return {
      localDaterange: [],
      visible: false,
      filterTypeList: [
        { key: 'createTime', name: this.$t('提交日期') },
        { key: 'fixTime', name: this.$t('修复日期') },
      ],
    };
  },
  computed: {
    localVal() {
      let result = '';
      if (
        this.dateRange.length
        && Object.keys(this.dateRange).every(item => this.dateRange[item])
      ) {
        result = `${
          this.selected === 'createTime'
            ? this.$t('提交日期')
            : this.$t('修复日期')
        }：${this.dateRange.join(' - ')}`;
      }
      return result;
    },
  },
  methods: {
    toggleShow() {
      this.visible = !this.visible;
    },
    hiddenDropdown() {
      this.visible = false;
    },
    selectType(type) {
      this.selected = type;
      this.localDaterange = [];
    },
    handleConfirm() {
      if (
        Object.keys(this.localDaterange).every(item => this.localDaterange[item])
      ) {
        const startCreateTime = this.formatTime(
          this.localDaterange[0],
          'yyyy-MM-dd',
        );
        const endCreateTime = this.formatTime(
          this.localDaterange[1],
          'yyyy-MM-dd',
        );
        const target = [startCreateTime, endCreateTime];
        this.handleChange(target, this.selected);
      } else {
        this.handleChange([], this.selected);
      }
      // 勾选
      if (this.selected === 'fixTime') {
        const that = this;
        if (!this.statusUnion.includes(2)) {
          const subTitleTxt = this.$t('只有已修复问题包含修复日期字段。筛选条件暂未选择“已修复”状态，是否自动选中？');
          this.$bkInfo({
            subTitle: subTitleTxt,
            maskClose: true,
            confirmFn: async () => {
              that.statusUnion.push(2);
            },
          });
        }
      }
      this.hiddenDropdown();
    },
    handleClear() {
      this.localDaterange = [];
      this.handleChange([], this.selected);
      this.hiddenDropdown();
    },
    formatTime(date, token, options = {}) {
      return date ? format(Number(date), token, options) : '';
    },
  },
};
</script>

<style lang="postcss">
@import url('../../css/mixins.css');

.date-picker-dropdown {
  position: absolute;
  z-index: 99;
  width: 100%;
  border-radius: 2px;

  .picker-dropdown-content {
    width: 360px;
    margin-top: 3px;
    background-color: #fff;
    border: 1px solid #dcdee5;
    border-radius: 2px;
    box-shadow: 0 2px 6px rgb(51 60 72 / 10%);
  }

  .select-button {
    width: 100%;
  }

  .data-range-input {
    display: inline-block;
    float: left;
    font-size: 12px;
    text-align: left;

    @mixin ellipsis;

    & + .bk-icon {
      position: absolute;
      top: 7px;
      right: 0;
    }

    &.unselect {
      margin-left: -4px;
      font-size: 12px;
      color: #c3cdd7;
    }
  }

  .icon-flip {
    transform: rotate(180deg);
    transition: all 0.2s ease;
  }

  .bk-date-picker {
    margin: 0 20px 20px;
  }

  .bk-button-group {
    margin: 20px;
  }

  .content-ft {
    position: relative;
    padding: 12px 0;
    text-align: center;
    border-top: 1px solid #ded8d8;

    .clear-btn {
      position: absolute;
      right: 8px;
    }
  }
}
</style>
