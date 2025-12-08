<template>
  <bk-dialog
    v-model="visible"
    width="640"
    theme="primary"
    header-position="left"
    footer-position="center"
    :position="{ top: 50, left: 5 }"
    :title="$t(title)"
  >
    <template v-if="$slots.header" #header>
      <slot name="header"></slot>
    </template>
    <div class="operate-txt operate-txt-1">1. {{ $t('列表') }}</div>
    <div
      class="flex justify-between items-center text-[#979BA5] mt-[8px] p-[16px]
        !text-[12px] border-[#DCDEE5] border-[1px] border-solid">
      <div>
        <div>
          <img :src="KEY_W" class="w-[28px] h-[16px] inline-block mr-[12px]" />
          <span>{{ $t('或') }}</span>
          <img :src="KEY_UP" class="w-[28px] h-[16px] inline-block mx-[12px]" />
          <span>{{ $t('向上 切换问题') }}</span>
        </div>
        <div>
          <img :src="KEY_S" class="w-[28px] h-[16px] inline-block mr-[12px]" />
          <span>{{ $t('或') }}</span>
          <img :src="KEY_DOWN" class="w-[28px] h-[16px] inline-block mx-[12px]" />
          <span>{{ $t('向下 切换问题') }}</span>
        </div>
      </div>
      <bk-divider direction="vertical" class="!h-[29px]"></bk-divider>
      <div>
        <img :src="KEY_ENTER" class="w-[64px] h-[24px] inline-block mr-[16px]" />
        <span>{{ $t('Enter 键进入问题详情') }}</span>
      </div>
    </div>
    <div class="flex justify-center bg-[#EAEBF0] p-[8px]">
      <img :src="defectListImg" class="w-[574px]" />
    </div>
    <div class="operate-txt operate-txt-2 mt-[16px]" v-if="showDetail">2. {{ $t('问题详情') }}</div>
    <div
      v-if="showDetail"
      class="flex justify-between items-center text-[#979BA5] mt-[8px] p-[16px]
        !text-[12px] border-[#DCDEE5] border-[1px] border-solid">
      <div>
        <div>
          <img :src="KEY_SHIFT" class="w-[41px] h-[16px] inline-block mr-[8px]" />
          <span>+</span>
          <img :src="KEY_W" class="w-[28px] h-[16px] inline-block mx-[8px]" />
          <span>{{ $t('或') }}</span>
          <img :src="KEY_UP" class="w-[28px] h-[16px] inline-block mx-[8px]" />
          <span>{{ $t('向上 切换步骤') }}</span>
        </div>
        <div>
          <img :src="KEY_SHIFT" class="w-[41px] h-[16px] inline-block mr-[8px]" />
          <span>+</span>
          <img :src="KEY_S" class="w-[28px] h-[16px] inline-block mx-[8px]" />
          <span>{{ $t('或') }}</span>
          <img :src="KEY_DOWN" class="w-[28px] h-[16px] inline-block mx-[8px]" />
          <span>{{ $t('向下 切换步骤') }}</span>
        </div>
      </div>
      <bk-divider direction="vertical" class="!h-[29px]"></bk-divider>
      <div>
        <img :src="KEY_ESC" class="w-[64px] h-[24px] inline-block mr-[16px]" />
        <span>{{ $t('Esc 键关闭问题详情') }}</span>
      </div>
    </div>
    <div class="flex justify-center bg-[#EAEBF0] p-[8px]">
      <img :src="defectDetailImg" class="w-[574px]" />
    </div>
    <div class="operate-footer" slot="footer">
      <bk-button theme="primary" @click.native="handleClick">
        {{ $t('关闭') }}
      </bk-button>
    </div>
  </bk-dialog>
</template>

<script>
import { language } from '../../i18n';
import KEY_W from '../../images/operate-tips/button/w.png';
import KEY_S from '../../images/operate-tips/button/s.png';
import KEY_SHIFT from '../../images/operate-tips/button/shift.png';
import KEY_ENTER from '../../images/operate-tips/button/defect-enter.png';
import KEY_ESC from '../../images/operate-tips/button/defect-esc.png';
import KEY_UP from '../../images/operate-tips/button/up.png';
import KEY_DOWN from '../../images/operate-tips/button/down.png';
import zhDefectList from '../../images/operate-tips/zh/defect-list.png';
import zhDefectDetail from '../../images/operate-tips/zh/defect-detail.png';
import enDefectList from '../../images/operate-tips/en/defect-list.png';
import enDefectDetail from '../../images/operate-tips/en/defect-detail.png';
import jaDefectList from '../../images/operate-tips/ja/defect-list.png';
import jaDefectDetail from '../../images/operate-tips/ja/defect-detail.png';

export default {
  name: 'OperateDialog',
  props: {
    visible: Boolean,
    showDetail: {
      type: Boolean,
      default: true,
    },
    title: {
      type: String,
      default: '键盘操作指引',
    },
  },
  data() {
    return {
      KEY_W,
      KEY_S,
      KEY_SHIFT,
      KEY_ENTER,
      KEY_ESC,
      KEY_UP,
      KEY_DOWN,
      zhDefectList,
      zhDefectDetail,
      enDefectList,
      enDefectDetail,
      jaDefectList,
      jaDefectDetail,
      langKey: {
        'zh-CN': 'zh',
        'en-US': 'en',
        'ja-JP': 'ja',
      },
    };
  },
  computed: {
    defectListImg() {
      const curLang = this.langKey[language] || 'zh';
      switch (curLang) {
        case 'zh':
          return zhDefectList;
        case 'en':
          return enDefectList;
        case 'ja':
          return jaDefectList;
        default:
          return zhDefectList;
      };
    },
    defectDetailImg() {
      const curLang = this.langKey[language] || 'zh';
      switch (curLang) {
        case 'zh':
          return zhDefectDetail;
        case 'en':
          return enDefectDetail;
        case 'ja':
          return jaDefectDetail;
        default:
          return zhDefectDetail;
      };
    },
  },
  watch: {
    visible(value) {
      if (!value) {
        window.localStorage.setItem('opreate-keyboard-20220411', '1');
      }
      this.$emit('update:visible', value);
    },
  },
  methods: {
    handleClick() {
      this.$emit('update:visible', false);
    },
  },
};
</script>
