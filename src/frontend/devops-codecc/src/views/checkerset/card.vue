<template>
  <div
    class="checkerset-card"
    :class="{
      enable: isEnable || isFlash,
      disable: from === 'task' && !isEnable,
    }"
  >
    <span
      :title="$t('默认规则集将会在创建任务时被自动开启')"
      class="codecc-icon icon-default-eng default-mark"
      v-if="checkerset.defaultCheckerSet"
    ></span>
    <section class="checkerset-info">
      <p class="checkerset-name">
        <a
          class="name-content"
          :title="checkerset.checkerSetName"
          @click="handleManage(checkerset, 'edit')"
        >
          <span>{{ checkerset.checkerSetName }}</span>
        </a>
        <span
          v-if="['DEFAULT', 'RECOMMEND'].includes(checkerset.checkerSetSource)"
          :class="[
            'use-mark',
            {
              preferred: checkerset.checkerSetSource === 'DEFAULT',
              recommend: checkerset.checkerSetSource === 'RECOMMEND',
            },
          ]"
        >{{
          $t(checkerset.checkerSetSource === 'DEFAULT' ? '精选' : '推荐')
        }}</span
        >
        <span
          :title="$t('复制规则集ID')"
          class="copy-icon"
          @click="copyCheckerset(checkerset.checkerSetId)"
        ><i class="codecc-icon icon-copy-line"></i
        ></span>
      </p>
      <p class="checkerset-id" :title="checkerset.description">
        {{ checkerset.description }}
      </p>
      <div class="dn">
        <div id="ccn-tips">
          <div class="pb10">
            {{ $t('点击规则集名称，可前往添加圈复杂度和重复率规则') }}
          </div>
          <div style="text-align: right">
            <bk-button
              theme="primary"
              size="small"
              @click="confirm('ccn-tips')"
            >{{ $t('我知道了') }}</bk-button
            >
          </div>
        </div>
        <div id="new-tips">
          <div class="pb10">
            {{ $t('点击规则集名称可查看其所包含的规则，并进行编辑') }}
          </div>
          <div style="text-align: right">
            <bk-button
              theme="primary"
              size="small"
              @click="confirm('new-tips')"
            >{{ $t('我知道了') }}</bk-button
            >
          </div>
        </div>
      </div>
    </section>
    <section class="item-language">
      <p class="col-value" :title="codeLang">{{ codeLang }}</p>
      <label class="col-label">{{ $t('语言') }}</label>
    </section>
    <section class="item-rule-count">
      <p class="col-value">
        {{ checkerset.checkerProps ? checkerset.checkerProps.length : 0 }}
      </p>
      <label class="col-label">{{ $t('规则数') }}</label>
    </section>
    <section class="item-tool-count">
      <p class="col-value" :title="formateTool(checkerset.toolList)">
        {{ checkerset.toolList ? checkerset.toolList.length : 0 }}
      </p>
      <label class="col-label">{{ $t('工具数') }}</label>
    </section>
    <section class="item-creator">
      <p class="col-value" :title="checkerset.creator">
        {{ checkerset.creator }}
      </p>
      <label class="col-label">{{ $t('发布者') }}</label>
    </section>
    <section class="item-labels">
      <label
        v-for="(category, categoryIndex) in checkerset.catagories"
        :key="categoryIndex"
        v-if="checkerset.catagories && categoryIndex <= 3"
      >{{ isEn ? category.enName : category.cnName }}</label
      >
      <bk-popover
        placement="bottom-top"
        v-if="checkerset.catagories && checkerset.catagories.length > 3"
      >
        <label class="more-label">...</label>
        <div slot="content" class="menu-content">
          <span
            v-for="(category, categoryIndex) in checkerset.catagories"
            :key="categoryIndex"
          >{{ isEn ? category.enName : category.cnName }}
            <span
              v-if="categoryIndex < checkerset.catagories.length - 1"
              class="cut-line"
            >|</span
            >
          </span>
        </div>
      </bk-popover>
    </section>
    <section class="task-used-count" v-if="from !== 'task'">
      <i
        :class="{ 'status-mark': true, 'using-status': checkerset.taskUsage }"
        v-bk-tooltips="useContentPrompt"
      ></i>
      <span v-if="!isSmallScreen">
        <label>{{ checkerset.taskUsage ? $t('使用中') : $t('空闲中') }}</label>
        <span class="value">({{ checkerset.taskUsage || 0 }})</span>
      </span>
    </section>
    <section class="item-version" v-if="from !== 'task'">
      <p class="current-version" @click="showMenu">
        <i class="last-new-icon"></i>{{ displayVersion }}
      </p>
      <bk-popover
        :ext-cls="'version-menu'"
        ref="versionList"
        theme="light"
        placement="bottom-top"
        trigger="click"
        :on-hide="hideFn"
        :on-show="showFn"
      >
        <i
          :class="{
            'bk-icon': true,
            'icon-down-shape': true,
            active: showVersionList,
          }"
        ></i>
        <div slot="content" class="menu-content">
          <p
            v-for="(version, versionIndex) in checkerset.versionList"
            :key="versionIndex"
            @click="changeVersion(version.version)"
          >
            {{ version.displayName }}
            <i
              class="bk-icon codecc-icon icon-pic-label-new"
              v-if="versionIndex === 1"
              style="color: red"
            ></i>
          </p>
        </div>
      </bk-popover>
    </section>
    <section class="item-version" v-else>
      <p class="current-version">{{ displayVersion }}</p>
    </section>
    <section class="handle-option" v-if="from !== 'task'">
      <bk-popover
        :ext-cls="'handle-menu'"
        ref="handleMenu"
        theme="light"
        placement="bottom-top"
        trigger="click"
      >
        <i class="bk-icon icon-more"></i>
        <div slot="content" class="menu-content">
          <p
            v-for="(handle, handleIndex) in handleList"
            :key="handleIndex"
            v-if="handle.isEnable"
            @click.stop="handleOption(checkerset, handle.name)"
          >
            {{ handle.label }}
          </p>
        </div>
      </bk-popover>
    </section>
    <section class="handle-switch" v-if="from === 'task' && !isNewAtom">
      <bk-switcher
        size="large"
        theme="primary"
        v-model="isEnable"
        :disabled="!isEnable && checkerset.codeLangList > 1"
        @click.native="handleClick(isEnable)"
      >
      </bk-switcher>
    </section>
  </div>
</template>

<script>
import { mapState } from 'vuex';
import { language } from '../../i18n';

export default {
  props: {
    checkerset: {
      type: Object,
      default() {
        return {};
      },
    },
    from: {
      type: String,
      default: '',
    },
    permissionList: Array,
    handleManage: Function,
    isEnable: {
      type: Boolean,
      default: false,
    },
    isNewAtom: {
      type: Boolean,
      default: false,
    },
    hasCcn: {
      type: Boolean,
      default: false,
    },
    hasNew: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      showVersionList: false,
      isSmallScreen: document.body.clientWidth <= 1280,
      ccnTipsHtmlConfig: {
        allowHtml: true,
        width: 240,
        theme: 'light',
        trigger: 'click',
        showOnInit: true,
        content: '#ccn-tips',
      },
      newTipsHtmlConfig: {
        allowHtml: true,
        width: 240,
        theme: 'light',
        trigger: 'click',
        showOnInit: true,
        content: '#new-tips',
      },
      isFlash: false,
      newTipsClass: true,
    };
  },
  computed: {
    ...mapState(['toolMeta']),
    ...mapState('tool', {
      toolMap: 'mapList',
    }),
    projectId() {
      return this.$route.params.projectId;
    },
    taskId() {
      return this.$route.params.taskId;
    },
    isCreator() {
      return (
        this.checkerset.creator
        && this.checkerset.creator === this.$store.state.user.username
      );
    },
    isManager() {
      return this.permissionList.includes('MANAGER');
    },
    isPertainProject() {
      return (
        this.checkerset.projectId
        && this.checkerset.projectId === this.projectId
      );
    },
    useContentPrompt() {
      return this.isSmallScreen
        ? {
          theme: 'light',
          content: this.checkerset.taskUsage
            ? this.$t('使用中 (x)', [this.checkerset.taskUsage])
            : this.$t('空闲中 (0)'),
          delay: 300,
        }
        : '';
    },
    handleList() {
      return [
        {
          name: 'edit',
          label: this.$t('编辑'),
          isEnable:
            !(
              this.checkerset.legacy && this.checkerset.codeLangList.length > 1
            ) // 单语言旧可编辑
            && this.isPertainProject
            && (this.isCreator || this.isManager),
        },
        {
          name: 'copy',
          label: this.$t('复制'),
          isEnable: !(
            this.checkerset.legacy && this.checkerset.codeLangList.length > 1
          ),
        },
        {
          name: 'setDefault',
          label: `${
            this.checkerset.defaultCheckerSet
              ? this.$t('取消')
              : this.$t('设为')
          } ${this.$t('默认')}`,
          isEnable: !(
            this.checkerset.legacy && this.checkerset.codeLangList.length > 1
          ),
        },
        {
          name: 'setPublish',
          label: `${this.$t('设为')}${
            this.checkerset.scope === 1 ? this.$t('私密') : this.$t('公开')
          }`,
          isEnable:
            !this.checkerset.legacy
            && this.isPertainProject
            && (this.isCreator || this.isManager),
        },
        {
          name: 'delete',
          label: this.isPertainProject ? this.$t('删除') : this.$t('卸载'),
          isEnable:
            (this.isPertainProject && (this.isCreator || this.isManager))
            || !this.isPertainProject,
        },
      ];
    },
    codeLang() {
      const names = this.toolMeta.LANG.map((lang) => {
        if (lang.key & this.checkerset.codeLang) {
          return lang.name;
        }
        return false;
      }).filter(name => name);
      return names.join('、');
    },
    displayVersion() {
      if (this.checkerset.versionList && this.checkerset.versionList.length) {
        const index = this.checkerset.versionList.findIndex(val => val.version === this.checkerset.version);
        return (
          this.checkerset.versionList[index]
          && this.checkerset.versionList[index].displayName
        );
      }
      return `V${this.checkerset.version}`;
    },
    hasCcnTips() {
      return this.hasCcn && !localStorage.getItem('ccn-tips');
    },
    hasNewTips() {
      return this.hasNew && !localStorage.getItem('new-tips');
    },
    isEn() {
      return language === 'en';
    },
  },
  mounted() {
    window.addEventListener('resize', () => {
      this.isSmallScreen = document.body.clientWidth <= 1280;
    });
    this.flash();
  },
  methods: {
    handleOption(checkerset, type) {
      this.$refs.handleMenu.instance.hide();
      this.handleManage(checkerset, type);
    },
    changeVersion(version) {
      this.$refs.versionList.instance.hide();
      if (version !== this.checkerset.version) {
        this.handleManage(this.checkerset, 'version', version);
      }
    },
    showMenu() {
      this.showVersionList = true;
      this.$refs.versionList.instance.show();
    },
    hideFn() {
      this.showVersionList = false;
    },
    showFn() {
      setTimeout(() => {
        this.showVersionList = true;
      }, 10);
    },
    handleClick(value) {
      const isEnable = !value;
      this.isEnable = isEnable;
      if (isEnable) {
        // 启用状态，要停用
        const { projectId, taskId, checkerset } = this;
        const { checkerSetId } = checkerset;
        const params = { projectId, checkerSetId, discardFromTask: taskId };
        this.$store
          .dispatch('checkerset/manage', params)
          .then((res) => {
            if (res.code === '0') {
              this.$bkMessage({
                theme: 'success',
                message: this.$t('操作成功'),
              });
              this.checkerset.taskUsing = false;
            }
          })
          .catch((e) => {
            console.error(e);
          });
      } else {
        // 要启用
        const { projectId, taskId, checkerset } = this;
        const type = 'TASK';
        const { checkerSetId } = checkerset;
        const params = { projectId, taskId, type, checkerSetId };
        this.$store
          .dispatch('checkerset/install', params)
          .then((res) => {
            if (res.code === '0') {
              this.$bkMessage({
                theme: 'success',
                message: this.$t('操作成功'),
              });
              this.checkerset.taskUsing = true;
            }
          })
          .catch((e) => {
            console.error(e);
          });
      }
    },
    formateTool(list = []) {
      const newList = list.map(item => this.toolMap[item] && this.toolMap[item].displayName);
      return newList.join('、');
    },
    confirm(item) {
      localStorage.setItem(item, 1);
      this.newTipsClass = false;
      document.getElementById('ccn-tips').click();
    },
    flash() {
      if (this.hasCcnTips && !this.isEnable) {
        setTimeout(() => {
          this.isFlash = true;
        }, 100);
        setTimeout(() => {
          this.isFlash = false;
        }, 600);
      }
    },
    goToNew() {
      this.$router.push({ name: 'task-new' });
    },
    handleHref(checkerset) {
      const link = {
        name: 'checkerset-manage',
        params: {
          projectId: this.projectId,
          checkersetId: checkerset.checkerSetId,
          version: checkerset.version,
        },
      };
      return this.$router.resolve(link).href;
    },
    copyCheckerset(checkersetId) {
      const input = document.createElement('input');
      document.body.appendChild(input);
      input.setAttribute('value', checkersetId);
      input.select();
      document.execCommand('copy');
      document.body.removeChild(input);
      this.$bkMessage({
        theme: 'success',
        message: this.$t('已复制规则集ID【x】至剪贴板', [checkersetId]),
      });
    },
  },
};
</script>

<style lang="postcss">
@import url('../../css/variable.css');

.checkerset-card {
  position: relative;
  display: flex;
  width: 100%;
  height: 84px;
  padding: 8px 24px 8px 40px;
  margin-bottom: 16px;
  background-color: #fff;
  border: 1px solid $borderColor;
  border-radius: 2px;
  align-items: center;

  @media screen and (width <= 1280px) {
    .item-version {
      flex: 0.7;
    }

    .task-used-count {
      flex: none;
      margin-left: 12px;
    }
  }

  @media screen and (width <= 1366px) {
    .item-language,
    .item-rule-count,
    .item-tool-count {
      flex: 0.6;
    }

    .item-version {
      padding-right: 10px;
    }

    .handle-option {
      flex: 0.3;
      text-align: center;
    }
  }

  &.enable {
    background: #edf5ff;
    border: 1px solid #dae1f6;
  }

  &.disable {
    .default-mark {
      color: #c4c6cc;
    }
  }

  section {
    flex: 1;
    overflow: hidden;
  }

  .default-mark {
    position: absolute;
    top: 0;
    left: -1px;
    z-index: 1;
    font-size: 16px;
    color: #7572dc;
  }

  .checkerset-info {
    flex: 2;
    width: 100%;
    min-width: 256px;
    padding-right: 12px;
    border-right: 1px solid #eff1f1;
  }

  .checkerset-name,
  .checkerset-id {
    font-size: 18px;
    color: #63656e;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .checkerset-name {
    display: flex;
    align-items: center;

    &:hover {
      .copy-icon {
        display: block;
      }
    }

    .copy-icon {
      display: none;
      padding-left: 5px;
      margin-top: -3px;
      color: #979ba5;
      cursor: pointer;
    }
  }

  .name-content {
    overflow: hidden;
    color: #63656e;
    text-overflow: ellipsis;
    white-space: nowrap;
    cursor: pointer;

    &:hover {
      color: #3a84ff;
    }
  }

  .use-mark {
    display: inline-block;
    height: 20px;
    padding: 0 10px;
    margin-left: 8px;
    font-size: 12px;
    border-radius: 2px;

    &.preferred {
      color: rgb(53 99 22 / 80%);
      background-color: rgb(134 223 38 / 30%);
      border: 1px solid rgb(102 197 1 / 30%);
    }

    &.recommend {
      color: rgb(61 76 138 / 80%);
      background-color: rgb(211 224 255 / 30%);
      border: 1px solid rgb(187 204 244 / 30%);
    }
  }

  .checkerset-id {
    margin-top: 6px;
    overflow: hidden;
    font-size: 12px;
    color: #c3cdd7;
  }

  .item-language,
  .item-rule-count,
  .item-tool-count,
  .item-creator {
    padding: 0 16px;
    font-size: 14px;
    color: #63656e;
    text-align: center;
  }

  .item-creator {
    flex: 1.2;
  }

  .col-value {
    max-width: 100%;
    margin-bottom: 4px;
    overflow: hidden;
    font-size: 16px;
    color: #313238;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .col-label {
    font-size: 12px;
    color: #63656d;
  }

  .item-labels {
    flex: 1;
    min-width: 90px;
    text-align: center;

    label {
      display: inline-block;
      padding: 2px 6px;
      margin-top: 4px;
      margin-left: 4px;
      font-size: 12px;
      color: #737987;
      background-color: #c9dffa;
      border-radius: 2px;
    }

    .more-label {
      padding: 2px 6px;
      margin-left: 0;
    }
  }

  .task-used-count {
    position: relative;
    font-size: 12px;
    color: #999;
    text-align: center;
    flex: 1.1;
  }

  .status-mark {
    display: inline-table;
    width: 6px;
    height: 6px;
    margin-right: 6px;
    background-color: #7572dc;
    border-radius: 50%;

    &.using-status {
      background-color: #2dcb56;
    }
  }

  .item-version {
    position: relative;
    display: flex;
    min-width: 52px;
    padding-right: 26px;
    font-size: 14px;
    color: #63656e;
    justify-content: flex-end;
    flex: 0.8;
  }

  .current-version {
    position: relative;
    padding-right: 4px;
    cursor: pointer;
  }

  .last-new-icon {
    position: absolute;
    top: -6px;
    left: -10px;
    width: 6px;
    height: 6px;
    background-color: $iconFailColor;
    border-radius: 50%;
  }

  .icon-down-shape {
    display: inline-block;
    font-size: 12px;
    transition: all 0.3s ease;

    &.active {
      transform: rotate(180deg);
    }
  }

  .handle-option {
    flex: 0.4;
    text-align: center;
  }

  .handle-switch {
    flex: 0.5;
    text-align: center;
  }

  .bk-icon:hover {
    color: $goingColor;
    cursor: pointer;
  }
}

.version-menu,
.handle-menu,
.task-menu {
  .tippy-tooltip {
    padding: 0;
  }
}

.menu-content {
  max-height: 140px;
  padding: 4px 0;
  overflow: auto;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-thumb {
    background-color: #d4dae3;
    border-radius: 13px;
  }

  p {
    min-width: 100px;
    padding: 0 12px;
    margin: 2px auto;
    line-height: 24px;
    color: #63656e;
    cursor: pointer;

    &:hover {
      color: #3a84ff;
      background-color: #c9dffa;
    }
  }

  .cut-line {
    margin: 0 6px 0 2px;
  }
}

.task-menu .menu-content p {
  min-width: 200px;
  line-height: 18px;
  color: #fff;
}

.new-tips {
  color: #3a84ff;
}
</style>
