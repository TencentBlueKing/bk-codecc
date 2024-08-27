<template>
  <bk-dialog
    v-model="visible"
    ext-cls="install-more-dialog"
    width="650px"
    :theme="'primary'"
    :close-icon="false"
    :before-close="handleClose"
    :position="{ top: positionTop }"
  >
    <div
      class="main-content"
      v-bkloading="{ isLoading: loading, opacity: 0.3 }"
    >
      <div class="info-header">
        <span
        >{{ $t('选择规则集')
        }}<i
          class="bk-icon icon-refresh checkerset-fresh"
          :class="fetchingList ? 'spin-icon' : ''"
          @click="refresh"
        /></span>
        <div class="handle-option">
          <bk-select
            class="search-select"
            v-model="language"
            multiple
            style="width: 120px"
            :placeholder="$t('请选择')"
          >
            <bk-option
              v-for="option in codeLangs"
              :key="option.displayName"
              :id="option.displayName"
              :name="option.displayName"
            >
            </bk-option>
          </bk-select>
          <bk-input
            class="search-input"
            :placeholder="$t('快速搜索')"
            :clearable="true"
            :right-icon="'bk-icon icon-search'"
            v-model="keyWord"
            @input="handleClear"
            @enter="handleKeyWordSearch"
          >
          </bk-input>
          <i class="bk-icon icon-close" @click="closeDialog" />
        </div>
      </div>
      <bk-tab
        class="checkerset-tab"
        :label-height="42"
        size="small"
        ref="tab"
        :active.sync="classifyCode"
        type="unborder-card"
      >
        <bk-tab-panel
          class="checkerset-panel"
          ref="checkersetPanel"
          v-for="classify in classifyCodeList"
          :key="classify.enName"
          :name="classify.enName"
          :label="isEn ? classify.enName : classify.cnName"
          render-directive="if"
        >
          <section ref="checkersetList">
            <div
              :class="[
                'info-card',
                {
                  disabled:
                    checkerSet.codeLangList.length > 1 ||
                    !checkerSet.codeLangList.includes(curLang),
                  selected: checkIsSelected(checkerSet.checkerSetId),
                },
              ]"
              v-for="(checkerSet, index) in checkerSetList"
              :key="index"
            >
              <div
                :class="[
                  'checkerset-icon',
                  getIconColorClass(checkerSet.checkerSetId),
                ]"
              >
                {{ (checkerSet.checkerSetName || '')[0] }}
              </div>
              <div class="info-content">
                <p class="checkerset-main">
                  <span class="name" :title="checkerSet.checkerSetName">{{
                    checkerSet.checkerSetName
                  }}</span>
                  <span
                    v-if="
                      ['DEFAULT', 'RECOMMEND'].includes(
                        checkerSet.checkerSetSource
                      )
                    "
                    :class="[
                      'use-mark',
                      {
                        preferred: checkerSet.checkerSetSource === 'DEFAULT',
                        recommend: checkerSet.checkerSetSource === 'RECOMMEND',
                      },
                    ]"
                  >{{
                    $t(
                      checkerSet.checkerSetSource === 'DEFAULT'
                        ? '精选'
                        : '推荐'
                    )
                  }}</span
                  >
                  <span
                    class="language"
                    :title="getCodeLang(checkerSet.codeLang)"
                  >{{ getCodeLang(checkerSet.codeLang) }}</span
                  >
                </p>
                <p class="checkerset-desc" :title="checkerSet.description">
                  {{ checkerSet.description || $t('暂无描述') }}
                </p>
                <p class="other-msg">
                  <span>{{ $t('由x发布', { name: checkerSet.creator }) }}</span>
                  <span>{{
                    $t('共x条规则', { sum: checkerSet.checkerCount || 0 })
                  }}</span>
                </p>
              </div>
              <div
                class="info-operate"
                @mouseenter="currentHoverItem = index"
                @mouseleave="currentHoverItem = -1"
              >
                <bk-button
                  size="small"
                  class="handle-btn"
                  v-bk-tooltips="
                    getToolTips(
                      checkerSet.codeLangList.length > 1,
                      !checkerSet.codeLangList.includes(curLang)
                    )
                  "
                  :class="[
                    checkerSet.codeLangList.length > 1 ||
                      !checkerSet.codeLangList.includes(curLang)
                      ? 'disable-btn'
                      : 'enable-btn',
                  ]"
                  :theme="
                    classifyCode === 'store' && !checkerSet.projectInstalled
                      ? 'primary'
                      : 'default'
                  "
                  @click="
                    handleOption(
                      checkerSet.codeLangList.length > 1 ||
                        !checkerSet.codeLangList.includes(curLang),
                      classifyCode === 'store' && !checkerSet.projectInstalled,
                      checkerSet,
                      checkIsSelected(checkerSet.checkerSetId)
                    )
                  "
                >{{ getSelectText(checkerSet, index) }}</bk-button
                >
              </div>
            </div>
          </section>
          <div v-if="!checkerSetList.length">
            <div class="codecc-table-empty-text">
              <img src="@/images/empty.png" class="empty-img" />
              <div>{{ $t('暂无数据') }}</div>
            </div>
          </div>
        </bk-tab-panel>
        <template slot="setting">
          <a :href="linkUrl" target="_blank" class="codecc-link">{{
            $t('创建规则集')
          }}</a>
        </template>
      </bk-tab>
    </div>
    <div slot="footer">
      <bk-button :theme="'default'" @click="closeDialog">{{
        $t('关闭')
      }}</bk-button>
    </div>
  </bk-dialog>
</template>

<script>
import { mapState } from 'vuex';
import { leaveConfirm } from '../../common/leave-confirm';
import { language } from '../../i18n';

export default {
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    curLang: {
      type: String,
      default: '',
    },
    defaultLang: {
      type: Array,
      default: () => [],
    },
    selectedList: {
      type: Array,
      default: () => [],
    },
    handleSelect: Function,
  },
  data() {
    return {
      fetchingList: false,
      loading: false,
      loadEnd: false,
      pageChange: false,
      isLoadingMore: false,
      isOpen: false,
      currentHoverItem: -1,
      keyWord: '',
      language: [],
      params: {
        quickSearch: '',
        checkerSetCategory: [],
        checkerSetLanguage: [],
        pageNum: 1,
        pageSize: 10000,
      },
      classifyCode: 'all',
      checkerSetList: [],
    };
  },
  computed: {
    ...mapState('checkerset', {
      categoryList: 'categoryList',
      checkerSetLanguage: 'checkerSetLanguage',
      codeLangs: 'codeLangs',
    }),
    ...mapState(['toolMeta']),
    projectId() {
      return this.$route.params.projectId;
    },
    renderList() {
      let target = [];
      if (this.classifyCode === 'all') {
        target = [...this.checkerSetList];
      } else {
        target = this.checkerSetList.filter(item => item.catagories.some(val => val.enName === this.classifyCode));
      }
      return target || [];
    },
    linkUrl() {
      return `${window.DEVOPS_SITE_URL}/console/codecc/${this.projectId}/checkerset/list#new`;
    },
    classifyCodeList() {
      if (this.categoryList.length) {
        return [
          { cnName: this.$t('所有'), enName: 'all' },
          ...this.categoryList,
          { cnName: this.$t('研发商店'), enName: 'store' },
        ];
      }
      return [
        { cnName: this.$t('所有'), enName: 'all' },
        { cnName: this.$t('研发商店'), enName: 'store' },
      ];
    },
    positionTop() {
      const top = (window.innerHeight - 693) / 2;
      return top > 0 ? top : 0;
    },
    isEn() {
      return language === 'en-US';
    },
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        this.isOpen = true;
        this.keyWord = '';
        this.language = [];
        this.params = {
          quickSearch: '',
          checkerSetCategory: [],
          checkerSetLanguage: [],
          pageNum: 1,
          pageSize: 10000,
        };
        this.classifyCode = 'all';
        // 兼容语言
        if (
          this.codeLangs.findIndex(val => val.displayName === this.defaultLang[0]) < 0
        ) {
          this.language = [];
          this.params.checkerSetLanguage = [];
        } else {
          this.language = this.defaultLang;
          this.params.checkerSetLanguage = this.defaultLang;
        }
        this.requestList(true);
        // this.addScrollLoadMore()
      } else {
        this.classifyCode = '';
      }
    },
    classifyCode(newVal) {
      if (this.visible && !this.isOpen) {
        this.removeScrollLoadMore();
        this.params.pageNum = 1;
        this.params.checkerSetCategory = ['all', 'store'].includes(newVal)
          ? []
          : [newVal];
        this.requestList(true);
        // this.addScrollLoadMore()
      }
    },
    language(newVal) {
      if (!this.isOpen) {
        this.pageChange = false;
        this.params.checkerSetLanguage = newVal;
        this.resetScroll();
        this.requestList(false, this.params);
      }
    },
  },
  mounted() {
    // this.addScrollLoadMore()
  },
  beforeDestroy() {
    this.removeScrollLoadMore();
  },
  methods: {
    closeDialog() {
      this.$emit('update:visible', false);
    },
    async requestList(isInit, params = this.params) {
      this.loading = true;
      this.isLoadingMore = true;
      params.projectInstalled = this.classifyCode !== 'store' ? true : undefined;
      const res = await this.$store
        .dispatch('checkerset/otherList', params)
        .finally(() => {
          this.loading = false;
          this.isOpen = false;
        });
      // this.checkerSetList = this.pageChange ? this.checkerSetList.concat(res.content) : res.content
      this.checkerSetList = res.content;
      this.loadEnd = res.last;
      this.pageChange = false;
      this.isLoadingMore = false;
    },
    checkIsSelected(checkerSetId) {
      return this.selectedList.some(item => item.checkerSetId === checkerSetId);
    },
    getSelectText(checkerSet, index) {
      let txt = '';
      if (this.classifyCode === 'store' && !checkerSet.projectInstalled) {
        txt = this.$t('安装');
      } else {
        if (this.checkIsSelected(checkerSet.checkerSetId)) {
          txt = this.currentHoverItem === index
            ? this.$t('取消选中')
            : this.$t('已选中');
        } else {
          txt = this.$t('选择');
        }
      }
      return txt;
    },
    getIconColorClass(checkerSetId) {
      return checkerSetId ? `c${(checkerSetId[0].charCodeAt() % 6) + 1}` : 'c1';
    },
    getCodeLang(codeLang) {
      const names = this.toolMeta.LANG.map((lang) => {
        if (lang.key & codeLang) {
          return lang.name;
        }
        return false;
      }).filter(name => name);
      return names.join('、');
    },
    handleOption(isDisabled, isInstall, checkerSet, isCancel) {
      window.changeAlert = true;
      if (!isDisabled) {
        if (isInstall) this.install(checkerSet);
        else this.handleSelect(checkerSet, isCancel, this.curLang);
      }
    },
    handleKeyWordSearch(value) {
      this.keyWord = value.trim();
      this.params.quickSearch = this.keyWord;
      this.resetScroll();
      this.requestList(false, this.params);
    },
    handleClear(str) {
      if (str === '') {
        this.keyValue = '';
        this.handleKeyWordSearch('');
      }
    },
    async refresh() {
      if (this.keyWord === '') {
        this.requestList(true);
      } else {
        this.keyValue = this.keyValue.trim();
        const params = { quickSearch: this.keyWord };
        this.requestList(true, params);
      }
    },
    resetScroll() {
      const target = document.querySelector('.checkerset-panel');
      if (target) target.scrollTop = 0;
      this.params.pageNum = 1;
    },
    scrollLoadMore(event) {
      const { target } = event;
      const bottomDis = target.scrollHeight - target.clientHeight - target.scrollTop;
      if (bottomDis < 10 && !this.loadEnd && !this.isLoadingMore) {
        this.params.pageNum += 1;
        this.pageChange = true;
        this.requestList(false, this.params);
      }
    },
    addScrollLoadMore() {
      this.$nextTick(() => {
        const mainBody = document.querySelector('.checkerset-panel');
        if (mainBody) mainBody.addEventListener('scroll', this.scrollLoadMore, {
          passive: true,
        });
      });
    },
    removeScrollLoadMore() {
      const mainBody = document.querySelector('.checkerset-panel');
      if (mainBody) mainBody.removeEventListener('scroll', this.scrollLoadMore, {
        passive: true,
      });
    },
    install(checkerSet) {
      const params = {
        type: 'PROJECT',
        projectId: this.projectId,
        checkerSetId: checkerSet.checkerSetId,
        version: checkerSet.version,
      };
      this.loading = true;
      this.$store
        .dispatch('checkerset/install', params)
        .then((res) => {
          if (res.code === '0') {
            this.$bkMessage({ theme: 'success', message: this.$t('安装成功') });
            this.refresh();
          }
        })
        .catch((e) => {
          this.$bkMessage({
            message: this.$t('安装失败'),
            theme: 'error',
          });
        });
    },
    getToolTips(hasMultiLang, notCurLang) {
      if (hasMultiLang) {
        return this.$t('该规则集不适用于当前插件');
      }
      if (notCurLang) {
        return this.$t('该规则集不适用于当前插件已选择的语言');
      }
      return { disabled: true };
    },
    handleClose() {
      return leaveConfirm();
    },
  },
};
</script>
<style lang="postcss">
.install-more-dialog {
  border: 1px solid #ebf0f5;

  p {
    padding: 0;
    margin: 0;
  }

  .bk-dialog-tool {
    display: none;
  }

  .bk-dialog .bk-dialog-body {
    padding: 8px 16px 0;
  }

  .main-content {
    height: 600px;
  }

  .info-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    span {
      font-size: 14px;
      color: #222;
    }
  }

  .checkerset-fresh {
    position: relative;
    display: inline-block;
    padding: 4px;
    margin-left: 3px;
    font-size: 14px;

    /* top: 2px; */
    color: #3c96ff;
    cursor: pointer;

    &.spin-icon {
      color: #c3cdd7;
    }
  }

  .handle-option {
    display: flex;

    .icon-close {
      z-index: 3000;
      margin-left: 10px;
      font-size: 28px;
      color: #979ba5;
      cursor: pointer;
    }
  }

  .search-input {
    width: 120px;
  }

  .search-select {
    width: 100px;
    margin-right: 10px;
  }

  .checkerset-tab {
    height: calc(100% - 40px);
    overflow: hidden;
    font-size: 12px;
    font-weight: 500;
    border: 0;

    div.bk-tab-section {
      height: calc(100% - 42px);
      padding: 0;
      overflow-y: hidden;

      .bk-tab-content {
        height: 100%;
        overflow: auto;

        &::-webkit-scrollbar {
          width: 6px;
        }
      }
    }

    .bk-tab-header {
      .bk-tab-label-wrapper {
        .bk-tab-label-list {
          .bk-tab-label-item {
            min-width: auto;
            padding: 0 15px;

            .bk-tab-label {
              font-size: 12px;
              color: #63656e;

              &.active {
                font-weight: bold;
              }
            }
          }
        }
      }
    }
  }

  .info-card {
    display: flex;
    height: 66px;
    padding: 0 10px 0 8px;
    margin: 0 0 6px;
    align-items: center;

    &:first-child {
      margin-top: 12px;
    }

    .checkerset-icon {
      width: 48px;
      height: 48px;
      margin-right: 14px;
      font-size: 24px;
      font-weight: bold;
      line-height: 48px;
      color: #fff;
      text-align: center;
      border-radius: 8px;

      &.c1 {
        background: #37dab9;
      }

      &.c2 {
        background: #7f6efa;
      }

      &.c3 {
        background: #ffca2b;
      }

      &.c4 {
        background: #fe8f65;
      }

      &.c5 {
        background: #f787d9;
      }

      &.c6 {
        background: #5e7bff;
      }
    }

    .logo {
      width: 50px;
      height: 50px;
      margin-right: 15px;
      font-size: 50px;
      line-height: 50px;
      color: #c3cdd7;
    }

    .checkerset-main {
      display: flex;
      align-items: center;
      line-height: 14px;
    }

    .info-content {
      padding: 24px 0 20px;
      padding-right: 10px;
      overflow: hidden;
      font-size: 14px;
      color: #4a4a4a;
      flex: 1;
      flex-direction: column;
      justify-content: space-between;

      .name {
        max-width: 240px;
        overflow: hidden;
        font-size: 14px;
        font-weight: bold;
        color: #222;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .use-mark {
        display: inline-block;
        height: 20px;
        padding: 2px 10px;
        margin-left: 8px;
        font-size: 12px;
        white-space: nowrap;
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

      .language {
        display: inline;
        max-width: 240px;
        padding-left: 8px;
        margin-left: 8px;
        overflow: hidden;
        font-size: 12px;
        color: #63656e;
        text-overflow: ellipsis;
        white-space: nowrap;
        border-left: 1px solid #d8d8d8;
      }
    }

    .checkerset-desc {
      position: relative;
      top: 2px;
      width: 100%;
      overflow: hidden;
      font-size: 12px;
      font-weight: normal;
      color: #666;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .other-msg {
      font-size: 12px;
      color: #bbb;

      span:first-child {
        display: inline-block;
        width: 120px;
      }

      span:last-child {
        margin-left: 10px;
      }
    }

    .handle-btn {
      width: 74px;
      font-weight: normal;
      line-height: 22px;

      &.enable-btn:hover {
        color: white;
        background-color: #3a84ff;
      }

      &.disable-btn {
        color: #c4c6cc;
        cursor: not-allowed;
        background-color: #fff;
        border-color: #dcdee5;
      }
    }

    &.disabled .info-content {
      .name,
      .language,
      .checkerset-desc,
      .other-msg {
        color: #c3cdd7;
      }
    }

    &.selected {
      background-color: #e9f4ff;
    }
  }

  .info-card:hover {
    background-color: #f3f7fe;
  }

  .search-result {
    height: calc(100% - 60px);
    margin-top: 20px;
    overflow: auto;

    .info-card:first-child {
      margin-top: 0;
    }

    &::-webkit-scrollbar {
      width: 6px;
    }
  }

  .codecc-table-empty-text {
    margin-top: 180px;
    text-align: center;
  }

  .codecc-link {
    position: relative;
    top: 4px;
    padding-right: 12px;
    color: #3a84ff;
    text-decoration: none;
    cursor: pointer;
  }
}
</style>
