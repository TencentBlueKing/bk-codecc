<template>
  <div v-show="isShow" class="content" v-bkloading="{ isLoading: loading }">
    <div class="render-content" id="suggestion-content">
      <render-markdown :mds="mds" :copy-text="copyText" />
    </div>
    <bk-icon class="suggestion-close" @click="handleClick" type="close" />
    <div class="footer">
      <span
        class="footer-icon mr10"
        :class="{ active: likeList.includes(user.username) }"
        v-bk-tooltips="likeList.join(',')"
        @click="handleLike(true)">
        <span class="codecc-icon icon-like"></span>
        <span class="number">{{ likeList.length }}</span>
      </span>
      <span
        class="footer-icon unlike mr20"
        :class="{ active: unlikeList.includes(user.username) }"
        v-bk-tooltips="unlikeList.join(',')"
        @click="handleLike(false)">
        <span class="codecc-icon icon-like unlike"></span>
        <span class="number">{{ unlikeList.length }}</span>
      </span>
      <span class="footer-icon" @click="getSuggestion(true)">
        <span class="codecc-icon icon-refresh-2"></span>
        {{ $t('重新生成') }}
      </span>
    </div>
  </div>
</template>

<script>
import RenderMarkdown from './render-markdown.jsx';
import DefectWebSocket from '@/common/defectWebSocket';
import { isJSON } from '@/common/util';
import { mapGetters } from 'vuex';

export default {
  name: 'AiSuggestion',
  components: {
    RenderMarkdown,
  },
  props: {
    currentFile: {
      type: Object,
      default: () => ({}),
    },
    closeAiSuggestion: {
      type: Function,
      default: () => {},
    },
  },
  data() {
    return {
      isShow: true,
      loading: true,
      defectWS: null,
      suggestion: '',
      likeList: [],
      unlikeList: [],
    };
  },
  computed: {
    ...mapGetters(['user']),
    mds() {
      return this.splitMd(this.suggestion);
    },
  },
  created() {
    this.getEvaluate();
  },
  mounted() {
    this.initWebSocket();
  },
  beforeDestroy() {
    this.defectWS.disconnect();
  },
  methods: {
    getEvaluate() {
      const { entityId } = this.currentFile;
      this.$store.dispatch('defect/getEvaluate', {
        id: entityId,
      }).then((res) => {
        this.likeList = res.goodEvaluates || [];
        this.unlikeList = res.badEvaluates || [];
      });
    },
    handleClick() {
      this.isShow = false;
      this.closeAiSuggestion();
    },
    // 复制到剪切板
    copyText(text) {
      const input = document.createElement('textarea');
      input.style.position = 'fixed';
      input.style.opacity = 0;
      input.value = text;
      document.body.appendChild(input);
      input.select();
      document.execCommand('Copy');
      document.body.removeChild(input);
      this.$bkMessage({
        theme: 'success',
        message: this.$t('代码已复制'),
      });
    },
    splitMd(md) {
      const mdArr = [];
      const reg = /```[\s\S]*?```/g;
      const codeArr = md.match(reg);
      if (codeArr) {
        codeArr.forEach((code) => {
          const index = md.indexOf(code);
          if (index > 0) {
            mdArr.push({
              type: 'text',
              value: md.substring(0, index),
            });
          }
          const [, language, codeValue] = /(?:```)([a-zA-Z-_]*)[\s]([\s\S]+?)(?:[\s]```)/.exec(code) || [];
          // 去掉```，获取代码块
          mdArr.push({
            type: 'code',
            value: code,
            code: codeValue,
          });
          md = md.substring(index + code.length);
        });
      }
      if (md) {
        mdArr.push({
          type: 'text',
          value: md,
        });
      }
      return mdArr;
    },

    initWebSocket() {
      this.defectWS = new DefectWebSocket();
      const { entityId } = this.currentFile;
      const subscribe = `/topic/defect/suggestion/${entityId}`;

      this.defectWS.connect(this.getSuggestion, subscribe, {
        success: (res) => {
          this.handleMessage(res.body);
        },
        error: message => console.error(message),
      });
    },
    handleMessage(suggestion) {
      try {
        const values = suggestion.split(/(?<=})\s*(?={"result":true,)/);
        values.forEach((value) => {
          const item = value.trim();
          if (isJSON(item)) {
            const {
              result,
              data,
              message,
            } = JSON.parse(item);
            if (result === true) {
              if (!data.llm.includes('hunyuan')) {
                const llm = data.llm.split('.')[0].toUpperCase();
                this.suggestion.replace('混元大模型', `${llm}大模型`);
              }
              const choice = data?.result?.choices?.[0];
              if (!choice) {
                console.log('模型调用失败，返回对象为空');
              } else if (choice.finish_reason) {
                // console.log('模型调用完成');
                // console.log(this.suggestion);
                this.scrollBottom();
              } else if (choice.delta.content) {
                this.suggestion += choice.delta.content;
                // console.log(choice.delta.content);
                this.scrollBottom();
                this.loading = false;
              } else {
                console.log('模型调用失败，返回对象为空');
              }
            }
          }
        });
      } catch (error) {
        console.log('parse error', error);
      }
    },
    getSuggestion(flushCache = false) {
      this.loading = true;
      this.suggestion = this.$t('**混元大模型**：');
      const { toolName, entityId, filePath, taskId } = this.currentFile;
      const params = {
        toolName,
        entityId,
        filePath,
        pattern: 'LINT',
        stream: true,
        flushCache,
        uid: this.user.username,
        projectId: this.$route.params.projectId,
        taskId,
      };
      this.defectWS.sendMessage(params);
      if (flushCache) {
        this.likeList = [];
        this.unlikeList = [];
      }
    },
    scrollBottom() {
      this.$nextTick(() => {
        const content = document.getElementById('suggestion-content');
        if (content) {
          content.scrollTo({
            top: content.scrollHeight,
            behavior: 'smooth',
          });
        }
      });
    },
    handleLike(like) {
      const list = like ? this.likeList : this.unlikeList;
      const otherList = like ? this.unlikeList : this.likeList;
      if (list.includes(this.user.username)) {
        list.splice(list.indexOf(this.user.username), 1);
      } else {
        list.push(this.user.username);
        otherList.splice(otherList.indexOf(this.user.username), 1);
      }
      this.$store.dispatch('defect/postEvaluate', {
        defectId: this.currentFile.entityId,
        goodEvaluates: this.likeList,
        badEvaluates: this.unlikeList,
      });
    },
  },
};
</script>

<style lang="postcss" scoped>
.content {
  margin-right: 10px;
  margin-bottom: 10px;
  font-size: 14px;
  border: 1px solid #FCC8BE;
}

.render-content {
  max-height: 240px;
  min-height: 100px;
  overflow: auto;
  background: url("../../images/ai.svg") no-repeat;
  background-attachment: local;
  background-color: rgb(255 255 255 / 90%);
  background-size: 30px;

  &::-webkit-scrollbar {
    background-color: rgb(235 210 209 / 40%);
  }
}

.suggestion-close {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 9999;
  margin: 6px;
  font-size: 20px !important;
  cursor: pointer;
}

>>> .v-note-wrapper {
  min-height: 0;
  border: none;
}

>>> .suggestion-code {
  position: relative;

  .suggestion-icon-copy {
    position: absolute;
    top: 5px;
    right: 30px;
    z-index: 9999;
    margin: 6px;
    color: #3a84ff;
    cursor: pointer;
  }
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

.footer {
  display: flex;
  height: 50px;
  justify-content: center;
  align-items: center;

  .footer-icon {
    display: inline-block;
    padding: 2px 10px;
    font-size: 12px;
    line-height: 20px;
    color: #63656E;
    cursor: pointer;
    background: #EAEBF0;
    border: 1px solid #DCDEE5;
    border-radius: 12px;

    &.active, &:hover {
      color: #3A84FF;
      background-color: #E1ECFF;
      border-color: #A3C5FD;

      &.unlike {
        color: #ff9b03;
        background-color: #FFE8C3;
        border-color: #FFD695;
      }
    }
  }

  .icon-like {
    &.unlike {
      display: inline-block;
      rotate: 180deg;
    }
  }
}
</style>
