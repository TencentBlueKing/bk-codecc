<template>
  <div v-show="isShow" class="content" @scroll.stop>
    <div>
      <mavon-editor
        default-open="preview"
        :value="checkerDesc"
        :subfield="false"
        :toolbars-flag="false"
        preview-background="rgb(246 237 236)"
        :box-shadow="false"
        :editable="false" />
      <br />
      <div v-if="errExample">
        <div>{{ $t('错误示例') }}</div>
        <br />
        <mavon-editor
          default-open="preview"
          :value="errExample"
          :subfield="false"
          :toolbars-flag="false"
          preview-background="rgb(246 237 236)"
          :box-shadow="false"
          :editable="false" />
      </div>
      <div v-if="rightExample">
        <div>{{ $t('正确示例') }}</div>
        <br />
        <mavon-editor
          default-open="preview"
          :value="rightExample"
          :subfield="false"
          :toolbars-flag="false"
          preview-background="rgb(246 237 236)"
          :box-shadow="false"
          :editable="false" />
      </div>
    </div>
  </div>
</template>

<script>
import { mavonEditor } from 'mavon-editor';
import 'mavon-editor/dist/css/index.css';

export default {
  name: 'CheckerDetail',
  components: {
    mavonEditor,
  },
  props: {
    checkerDetail: {
      type: Object,
      default: () => ({}),
    },
  },
  data() {
    return {
      isShow: true,
      checkerDesc: '',
      errExample: '',
      rightExample: '',
    };
  },
  watch: {
    checkerDetail: {
      handler() {
        const { checkerDesc, errExample, rightExample, checkerLanguage } = this.checkerDetail;
        this.checkerDesc = checkerDesc;
        const lang = checkerLanguage?.[0];
        this.errExample = errExample ? `\`\`\`${lang}\n${errExample}\n\`\`\`` : '';
        this.rightExample = rightExample ? `\`\`\`${lang}\n${rightExample}\n\`\`\`` : '';
      },
      deep: true,
      immediate: true,
    },
  },
};
</script>

<style lang="postcss" scoped>

.content {
  position: relative;

  /* max-height: 240px;
  min-height: 100px; */
  padding: 16px;
  margin-right: 10px;
  margin-bottom: 10px;
  overflow: auto;
  font-size: 14px;

  >>> .pre-code {
    padding: 10px;
    overflow: auto;
    border: 1px solid #dcdee5;
  }
}

.suggestion-close {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 9999;
  font-size: 20px !important;
}

>>> .v-note-wrapper {
  min-height: 0;
  border: none;
}


>>> .markdown-body pre {
  padding: 0 !important;
}

>>> .markdown-body {
  font-size: 14px;
}

>>> .v-note-wrapper {
  z-index: 99;
  background-color: transparent;
}

>>> .v-note-wrapper .v-note-panel .v-note-show .v-show-content {
  padding: 0;
}
</style>
