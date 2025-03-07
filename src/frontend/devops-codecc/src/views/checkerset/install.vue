<template>
  <bk-dialog
    v-model="visible"
    width="650px"
    ext-cls="install-more-dialog"
    :position="{ top: positionTop }"
    :theme="'primary'"
    :close-icon="false"
  >
    <div
      class="main-content"
      v-bkloading="{ isLoading: loading, opacity: 0.3 }"
    >
      <div class="info-header">
        <span
        >{{ $t('更多规则集')
        }}<i
          class="bk-icon icon-refresh checkerset-fresh"
          :class="fetchingList ? 'spin-icon' : ''"
          @click="refresh"
        /></span>
        <bk-select
          class="search-select"
          v-model="language"
          multiple
          style="width: 120px"
          :placeholder="$t('选择语言')"
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
      </div>
      <bk-tab
        class="checkerset-tab"
        size="small"
        ref="tab"
        :active.sync="classifyCode"
        type="unborder-card"
      >
        <bk-tab-panel
          class="checkerset-panel"
          ref="checkersetPanel"
          v-for="classify in classifyCodeList"
          :key="classify.keyName"
          :name="classify.keyName"
          :label="classify.cnName"
          render-directive="if"
        >
          <section ref="checkersetList">
            <div
              class="info-card"
              v-for="(checkerSet, index) in checkerSetList"
              :key="index"
              @mouseover="currentHoverItem = index"
              @mouseout="currentHoverItem = -1"
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
                  {{ $t(checkerSet.description || '暂无描述') }}
                </p>
                <p class="other-msg">
                  <span>
                    {{ $t('由x发布', { name: checkerSet.creator }) }}
                  </span>
                  <span>
                    {{ $t('共x条规则', { sum: checkerSet.checkerCount || 0 }) }}
                  </span>
                </p>
              </div>
              <div class="info-operate">
                <bk-button
                  :theme="!checkerSet.projectInstalled ? 'primary' : 'default'"
                  size="small"
                  class="install-btn"
                  :disabled="checkerSet.projectInstalled"
                  @click="install(checkerSet)"
                >{{
                  $t(checkerSet.projectInstalled ? '已安装' : '安装')
                }}</bk-button
                >
              </div>
            </div>
          </section>
          <div v-if="!checkerSetList.length">
            <div class="codecc-table-empty-text">
              <img src="../../images/empty.png" class="empty-img" />
              <div>{{ $t('暂无数据') }}</div>
            </div>
          </div>
        </bk-tab-panel>
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

export default {
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    refreshList: Function,
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
        pageSize: 20,
      },
      classifyCode: 'all',
      classifyCodeList: [{ cnName: this.$t('所有'), enName: 'all', keyName: 'all' }],
      checkerSetList: [],
      codeLangs: [],
    };
  },
  computed: {
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
    positionTop() {
      const top = (window.innerHeight - 693) / 2;
      return top > 0 ? top : 0;
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
          pageSize: 20,
        };
        this.classifyCode = 'all';
        this.requestList(true);
        this.addScrollLoadMore();
      } else {
        this.classifyCode = '';
      }
    },
    classifyCode(newVal) {
      if (this.visible && !this.isOpen) {
        this.removeScrollLoadMore();
        this.params.pageNum = 1;
        this.params.checkerSetCategory = ['all'].includes(newVal)
          ? []
          : [newVal];
        this.requestList(true);
        this.addScrollLoadMore();
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
  created() {
    this.getFormParams();
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
    async getFormParams() {
      const res = await this.$store.dispatch('checkerset/params');
      this.classifyCodeList = [...this.classifyCodeList, ...res.catatories];
      this.codeLangs = res.codeLangs;
    },
    async requestList(isInit, params = this.params) {
      this.loading = true;
      this.isLoadingMore = true;
      params.projectInstalled = this.classifyCode === 'store' ? false : undefined;
      const res = await this.$store
        .dispatch('checkerset/otherList', params)
        .finally(() => {
          this.loading = false;
          this.isOpen = false;
        });
      this.checkerSetList = this.pageChange
        ? this.checkerSetList.concat(res.content)
        : res.content;
      this.loadEnd = res.last;
      this.pageChange = false;
      this.isLoadingMore = false;
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
    refresh() {
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
    resetInsatllStatus(checkerSetId) {
      this.checkerSetList = this.checkerSetList.map(checker => ({
        ...checker,
        projectInstalled:
          checker.checkerSetId === checkerSetId
            ? true
            : checker.projectInstalled,
      }));
    },
    install(checkerSet) {
      const params = {
        type: 'PROJECT',
        projectId: this.projectId,
        checkerSetId: checkerSet.checkerSetId,
        version: checkerSet.version,
      };
      this.$store
        .dispatch('checkerset/install', params)
        .then((res) => {
          if (res.code === '0') {
            this.$bkMessage({ theme: 'success', message: this.$t('安装成功') });
            this.refreshList();
            this.resetInsatllStatus(checkerSet.checkerSetId);
          }
        })
        .catch((e) => {
          console.error(e);
        });
    },
  },
};
</script>
<style lang="postcss">
.install-more-dialog {
  .bk-dialog-tool {
    display: none;
  }

  .bk-dialog-body {
    padding: 20px 24px 16px 32px;
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
    color: #3c96ff;
    cursor: pointer;

    &.spin-icon {
      color: #c3cdd7;
    }
  }

  .search-input {
    width: 180px;
  }

  .search-select {
    width: 120px;
    margin-left: 150px;
  }

  .checkerset-tab {
    height: calc(100% - 40px);
    margin-top: 10px;
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
    height: 80px;
    padding: 0 10px 0 8px;
    margin: 0 0 6px;
    align-items: center;

    &:first-child {
      margin-top: 18px;
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

      ,
      &.c5 {
        background: #f787d9;
      }

      ,
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

      .language {
        display: inline;
        max-width: 300px;
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

    .install-btn {
      font-weight: normal;
      line-height: 22px;
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
}
</style>
