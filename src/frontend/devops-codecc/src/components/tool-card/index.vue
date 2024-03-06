<template>
  <div
    :class="['tool-card', { picked, disabled }]"
    :source="source"
    :type="type"
    @click="handleClick"
  >
    <div class="tool-logo">
      <div class="tool-img"><img :src="toolData.logo" /></div>
      <div class="tool-name">
        <em :title="$t(`${toolData.displayName}`)">{{
          $t(`${toolData.displayName}`)
        }}</em>
      </div>
    </div>
    <div class="tool-desc">
      <div class="desc">
        <div class="tool-lang" :title="supportLangs">
          {{ $t('支持语言') }} {{ supportLangs }}
        </div>
        <div class="tool-summary" :title="toolData.description">
          {{ toolData.description }}
        </div>
      </div>
    </div>
    <!-- <corner :text="$t('推荐')" theme="warning" v-if="toolData.recommend" /> -->
  </div>
</template>

<script>
import { mapState } from 'vuex';
// import Corner from '@/components/corner'

export default {
  name: 'ToolCard',
  components: {
    // Corner
  },
  props: {
    tool: {
      type: Object,
      default() {
        return {};
      },
    },
    picked: {
      type: Boolean,
      default: false,
    },
    type: {
      type: String,
      default: 'pick',
      validator(value) {
        if (['pick', 'manage'].indexOf(value) === -1) {
          console.error(`type property is not valid: '${value}'`);
          return false;
        }
        return true;
      },
    },
    source: {
      type: String,
      default: 'new',
    },
  },
  data() {
    return {
      statusEnabled: !this.disabled,
    };
  },
  computed: {
    ...mapState(['toolMeta']),
    ...mapState('task', {
      taskDetail: 'detail',
    }),
    ...mapState('tool', {
      toolMap: 'mapList',
    }),
    toolData() {
      // 兼容已有工具和更多工具数据结构，已有工具的数据结构中没有logo等字段值
      // 因此在这里统一成原始工具列表中的工具数据
      return this.toolMap[this.tool.toolName || this.tool.name] || {};
    },
    supportLangs() {
      const toolLang = this.toolData.lang;
      const names = this.toolMeta.LANG.map((lang) => {
        if (lang.key & toolLang) {
          return lang.name;
        }
        return false;
      }).filter(name => name);
      return names.join('、');
    },
    isAdded() {
      return this.tool.taskId > 0;
    },
    hasRules() {
      let hasRules = false;
      hasRules = this.tool.toolName !== 'DUPC' && this.tool.toolName !== 'CCN';
      return hasRules;
    },
    disabled() {
      // 已停用工具
      this.statusEnabled = !(this.isAdded && this.tool.followStatus === 6);
      return this.isAdded && this.tool.followStatus === 6;
    },
    statusSwitcherDisabled() {
      return this.statusEnabled && this.taskDetail.enableToolList.length === 1;
    },
    statusSwitcherTitle() {
      let title = this.statusEnabled ? this.$t('停用') : this.$t('启用');
      if (this.statusSwitcherDisabled) {
        title = this.$t('不能停用所有工具');
      }
      return title;
    },
  },
  methods: {
    handleClick(e) {
      if (this.type === 'pick') {
        this.picked = !this.picked;
      }
      this.$emit('click', e, {
        name: this.toolData.name,
        picked: this.picked,
        disabled: this.disabled,
        source: this.source,
      });
    },
    toRules() {
      if (this.statusEnabled) {
        const { params } = this.$route;
        params.toolId = this.toolData.name;
        this.$router.push({
          name: 'task-settings-checkerset',
          params,
        });
      }
    },
  },
};
</script>

<style lang="postcss">
@import url('../../css/mixins.css');

.tool-card {
  position: relative;
  float: left;
  width: 485px;
  height: 174px;
  border: 1px solid #d1e5f2;
  border-radius: 2px;

  &[type='pick'] {
    cursor: pointer;
  }

  &.picked {
    background: #fbfdff;
    border-color: #3a84ff;

    @mixin triangle-check-bottom-right 11, #3a84ff;
  }

  .tool-logo {
    float: left;
    width: 126px;
    padding: 40px 5px;
    text-align: center;

    .tool-img {
      height: 64px;
      line-height: 0;

      img {
        max-width: 64px;
      }
    }

    .tool-name {
      overflow: hidden;
      color: #46c2c7;
      text-align: center;
      text-overflow: ellipsis;
      white-space: nowrap;

      em {
        font-size: 18px;
        font-style: normal;
      }
    }
  }

  .tool-desc {
    padding: 20px 21px 20px 0;
    overflow: hidden;
    font-size: 14px;

    .desc {
      height: 115px;
    }

    .tool-lang {
      padding: 4px;
      margin-top: 4px;
      font-weight: 600;
      color: #63656e;
      background: #e8f5fd;

      @mixin text-ellipsis 2;
    }

    .tool-summary {
      padding-top: 16px;
      line-height: 26px;
      color: #63656e;

      @mixin text-ellipsis 3;
    }

    .action-bar {
      text-align: right;

      .is-checked {
        background-color: #3a84ff;
      }

      .disabled {
        color: #b2c2dc;
        cursor: no-drop;
      }
    }
  }
}
</style>
