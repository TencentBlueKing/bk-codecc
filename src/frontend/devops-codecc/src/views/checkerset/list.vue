<template>
  <div class="cc-checkerset">
    <div class="cc-side">
      <div class="cc-filter">
        <span class="cc-filter-txt">{{ $t('过滤器') }}</span>
        <span class="cc-filter-clear fr" @click="handleClear">
          <i class="codecc-icon icon-filter-2 cc-link"></i>
        </span>
      </div>
      <bk-input
        class="checker-search"
        :placeholder="$t('请输入，按enter键搜索')"
        :clearable="true"
        :right-icon="'bk-icon icon-search'"
        v-model="keyWord"
        @enter="handleKeyWordSearch"
        @clear="handleKeyWordSearch"
      >
      </bk-input>
      <div class="cc-filter-content">
        <cc-collapse
          ref="searchParams"
          :is-checker-set="true"
          :search="search"
          :active-name="activeName"
          @updateActiveName="updateActiveName"
          @handleSelect="handleSelect"
        >
        </cc-collapse>
      </div>
    </div>
    <div class="cc-main" v-bkloading="{ isLoading: pageLoading, opacity: 0.6 }">
      <div class="list-tool-bar">
        <section class="bar-info">
          <bk-button
            icon="plus"
            theme="primary"
            @click="handleCreate"
            v-if="isRbac === true"
            key="create"
            v-perm="{
              hasPermission: true,
              disablePermissionApi: false,
              permissionData: {
                projectId: projectId,
                resourceType: 'codecc_rule_set',
                resourceCode: projectId,
                action: 'codecc_rule_set_create',
              },
            }"
          >
            {{ $t('创建规则集') }}
          </bk-button>
          <bk-button v-else icon="plus" theme="primary" @click="handleCreate">{{
            $t('创建规则集')
          }}</bk-button>
          <bk-button
            :text="true"
            icon-right="icon-plus-circle"
            @click="installMore"
          >{{ $t('更多规则集') }}</bk-button
          >
        </section>
        <span class="total-count"
        >{{ $t('共') }}
          <span v-if="checkersetList">{{ checkersetList.length }}</span>
          {{ $t('个规则集') }}</span
        >
      </div>
      <main class="checkerset-content" v-show="isFetched">
        <bk-virtual-scroll
          v-if="isFetched && checkersetList.length"
          ref="domVirtualScroll"
          class="dom-virtual-scroll"
          :item-height="100"
        >
          <template slot-scope="item">
            <card
              :checkerset="item.data"
              :permission-list="permissionList"
              :handle-manage="handleCheckerset"
            >
            </card>
          </template>
        </bk-virtual-scroll>
        <div v-if="!checkersetList.length">
          <div class="codecc-table-empty-text">
            <img src="../../images/empty.png" class="empty-img" />
            <div>{{ $t('暂无数据') }}</div>
          </div>
        </div>
      </main>
    </div>
    <bk-dialog
      v-model="delVisible"
      :theme="'primary'"
      @cancel="delVisible = false"
      @confirm="handleDelete"
      :title="
        curHandleItem.projectId === projectId
          ? $t('删除规则集')
          : $t('卸载规则集')
      "
    >
      <span v-if="curHandleItem.projectId === projectId">{{
        $t(
          '删除【x】规则集后无法恢复。本项目中将无法使用该规则集，其他项目中若已安装该规则集仍可继续使用。',
          [curHandleItem.checkerSetName]
        )
      }}</span>
      <span v-else>{{
        $t(
          '卸载【x】规则集后，本项目将无法使用该规则集。可通过“更多规则集”重新安装。',
          [curHandleItem.checkerSetName]
        )
      }}</span>
    </bk-dialog>
    <create
      :visible.sync="sliderVisible"
      :is-edit.sync="isEdit"
      :edit-obj="editObj"
      :refresh-detail="refreshList"
    ></create>
    <install
      :visible.sync="dialogVisible"
      :refresh-list="refreshList"
    ></install>
  </div>
</template>

<script>
import { mapState } from 'vuex';
import ccCollapse from '@/components/cc-collapse';
import card from './card';
import create from './create';
import install from './install';

export default {
  components: {
    ccCollapse,
    card,
    create,
    install,
  },
  data() {
    return {
      activeName: [],
      search: [],
      editObj: {},
      selectParams: {},
      curHandleItem: '',
      delVisible: false,
      sliderVisible: false,
      dialogVisible: false,
      isEdit: false,
      keyWord: undefined,
      checkersetList: [],
      permissionList: [],
      pageLoading: false,
      isFetched: false,
    };
  },
  computed: {
    ...mapState(['toolMeta', 'isRbac']),
    projectId() {
      return this.$route.params.projectId;
    },
    queryOptStorage() {
      return localStorage.getItem('checkerSetQueryObj')
        ? JSON.parse(localStorage.getItem('checkerSetQueryObj'))
        : {};
    },
  },
  watch: {
    selectParams: {
      handler(value) {
        this.initSearch(false);
        this.fetchList(false, value);
        this.setLocalQueryOpt(value);
      },
      deep: true,
    },
    checkersetList(value) {
      this.addVirtualScroll();
    },
  },
  created() {
    this.checkPermission();
    this.initSearch(true);
    this.fetchList(true);
  },
  mounted() {
    if (this.$route.query.installMore) this.dialogVisible = true;
    if (this.$route.hash === '#new') {
      setTimeout(() => {
        this.handleCreate();
      }, 800);
    }
  },
  methods: {
    async checkPermission() {
      const params = {
        projectId: this.projectId,
        user: this.$store.state.user.username,
      };
      const res = await this.$store.dispatch('checkerset/permission', params);
      this.permissionList = res.data;
    },
    async initSearch(isInit) {
      const data = Object.assign(this.selectParams, {
        projectId: this.projectId,
      });
      const res = await this.$store.dispatch('checkerset/count', data);
      this.search = res;
      if (isInit) {
        this.$nextTick(() => {
          if (Object.keys(this.queryOptStorage).length) {
            this.activeName = Object.keys(this.queryOptStorage);
          } else {
            this.activeName = ['checkerSetLanguage', 'checkerSetCategory'];
          }
        });
      }
    },
    async fetchList(isInit, selectParams = this.selectParams) {
      this.pageLoading = true;
      const params = Object.assign(selectParams, { projectId: this.projectId });
      const res = await this.$store.dispatch('checkerset/list', params);
      this.pageLoading = false;
      if (isInit) {
        if (!Object.keys(this.queryOptStorage).length) {
          this.checkersetList = res || [];
        }
        this.isFetched = true;
        this.addVirtualScroll();
      } else {
        this.checkersetList = res || [];
      }
    },
    setLocalQueryOpt(query) {
      const checkerSetQueryObj = {};
      Object.keys(query).forEach((item) => {
        if (!['projectId', 'keyWord'].includes(item) && query[item].length) {
          checkerSetQueryObj[item] = query[item];
        }
      });
      localStorage.setItem(
        'checkerSetQueryObj',
        JSON.stringify(checkerSetQueryObj),
      );
    },
    handleSelect(value) {
      this.selectParams = Object.assign({}, this.selectParams, value);
    },
    handleKeyWordSearch(value) {
      this.selectParams = Object.assign({}, this.selectParams, {
        keyWord: value,
      });
    },
    handleClear() {
      this.keyWord = '';
      this.selectParams = {};
      this.$refs.searchParams.handleClear();
    },
    handleCreate() {
      this.sliderVisible = true;
    },
    installMore() {
      this.dialogVisible = true;
    },
    refreshList() {
      this.initSearch(false);
      this.fetchList(false);
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
    async handleDelete() {
      const params = {
        projectId: this.projectId,
        checkerSetId: this.curHandleItem.checkerSetId,
      };
      this.curHandleItem.projectId === this.projectId
        ? (params.deleteCheckerSet = true)
        : (params.uninstallCheckerSet = true);
      this.handleManage(params);
    },
    async setDefault(checkerset) {
      const params = {
        projectId: this.projectId,
        checkerSetId: checkerset.checkerSetId,
        defaultCheckerSet: !checkerset.defaultCheckerSet,
      };
      this.handleManage(params);
    },
    async setScope(checkerset) {
      const params = {
        projectId: this.projectId,
        checkerSetId: checkerset.checkerSetId,
        scope: checkerset.scope === 1 ? 2 : 1,
      };
      this.handleManage(params);
    },
    handleManage(params) {
      this.$store
        .dispatch('checkerset/manage', params)
        .then((res) => {
          if (res.code === '0') {
            this.$bkMessage({ theme: 'success', message: this.$t('操作成功') });
            this.initSearch(false);
            this.fetchList(false);
          }
        })
        .catch((e) => {
          console.error(e);
        });
    },
    handleCheckerset(checkerset, type, version) {
      if (type === 'edit') {
        const link = {
          name: 'checkerset-manage',
          params: {
            projectId: this.projectId,
            checkersetId: checkerset.checkerSetId,
            version: checkerset.version,
          },
        };
        this.$router.push(link);
      } else if (type === 'copy') {
        const catagories = checkerset.catagories.map(category => category.enName);
        const { checkerSetName, checkerSetId, codeLang, description } = checkerset;
        this.editObj = {
          checkerSetName: `${checkerSetName}_copy`,
          checkerSetId: `${checkerSetId}_copy`,
          description,
          codeLang,
          catagories,
          baseCheckerSetId: checkerSetId,
          baseCheckerSetVersion: checkerset.version,
        };
        this.isEdit = true;
        this.handleCreate();
      } else if (type === 'delete') {
        this.curHandleItem = checkerset;
        this.delVisible = true;
      } else if (['setDefault', 'setPublish'].includes(type)) {
        const that = this;
        let titleTxt;
        let subTitleTxt;
        if (type === 'setDefault') {
          titleTxt = !checkerset.defaultCheckerSet
            ? this.$t('设置规则集为默认')
            : this.$t('取消规则集为默认');
          subTitleTxt = !checkerset.defaultCheckerSet
            ? this.$t(
              '设置【x】规则集为默认后，接入y语言任务时该规则集将会被自动选中。',
              [
                checkerset.checkerSetName,
                this.getCodeLang(checkerset.codeLang),
              ],
            )
            : this.$t(
              '取消【x】规则集为默认后，接入y语言任务时该规则集将不会被自动选中。',
              [
                checkerset.checkerSetName,
                this.getCodeLang(checkerset.codeLang),
              ],
            );
        } else {
          titleTxt = checkerset.scope === 2
            ? this.$t('设为公开规则集')
            : this.$t('设为私密规则集');
          subTitleTxt = checkerset.scope === 2
            ? this.$t(
              '将【x】规则集设为公开后，其他项目可以安装并使用该规则集。',
              [checkerset.checkerSetName],
            )
            : this.$t(
              '将【x】规则集设为私密后，其他项目不可以安装且使用该规则集。',
              [checkerset.checkerSetName],
            );
        }
        this.$bkInfo({
          title: titleTxt,
          subTitle: subTitleTxt,
          maskClose: true,
          confirmFn(name) {
            type === 'setDefault'
              ? that.setDefault(checkerset)
              : that.setScope(checkerset);
          },
        });
      } else if (type === 'version') {
        const params = {
          projectId: this.projectId,
          checkerSetId: checkerset.checkerSetId,
          versionSwitchTo: version,
        };
        this.handleManage(params);
      }
    },
    updateActiveName(activeName) {
      this.activeName = activeName;
    },
    addVirtualScroll() {
      setTimeout(() => {
        if (this.checkersetList.length && this.$refs.domVirtualScroll) {
          this.$refs.domVirtualScroll.scrollPageByIndex(0);
          this.$refs.domVirtualScroll.setListData(this.checkersetList);
          this.$refs.domVirtualScroll.getListData();
        }
      });
    },
  },
};
</script>

<style lang="postcss" scoped>
.cc-checkerset {
  display: flex;
  height: 100%;
  min-width: 1244px;
  padding: 0 40px;

  .cc-side {
    display: block;
    height: 100%;
    background: #fff;
    border: 1px solid #dcdee5;
  }

  .cc-side {
    width: 240px;
    padding: 0 16px;
    margin-right: 16px;

    .cc-filter {
      height: 52px;
      line-height: 52px;
      border-bottom: 1px solid #dcdee5;

      .cc-filter-txt {
        font-size: 14px;
        color: #333;
      }

      .cc-filter-select {
        float: right;
      }

      .cc-filter-clear {
        position: relative;
        float: right;
        padding-left: 10px;
        cursor: pointer;

        /* &::before {
                        content: "";
                        position: absolute;
                        width: 1px;
                        height: 18px;
                        background-color: #dcdee5;
                        left: 0;
                        top: 18px;
                    } */
      }
    }

    .cc-filter-content {
      max-height: calc(100% - 108px);
      margin: 0 -10px;
      overflow-y: scroll;

      &::-webkit-scrollbar {
        width: 4px;
      }

      &::-webkit-scrollbar-thumb {
        background-color: #d4dae3;
        border-radius: 13px;
      }
    }

    .checker-search {
      padding: 8px 0;
    }
  }

  .cc-main {
    display: block;
    width: calc(100% - 250px);
    height: 100%;
  }

  .list-tool-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 12px;
    margin-bottom: 12px;
    border-bottom: 1px solid #dcdee5;
  }

  .bar-info {
    button:last-child {
      margin-left: 14px;
    }
  }

  .total-count {
    font-size: 12px;
    color: #737987;
  }

  .codecc-table-empty-text {
    padding-top: 200px;
    text-align: center;
  }

  .checkerset-content {
    height: calc(100% - 40px);
    overflow: auto;

    &::-webkit-scrollbar {
      width: 4px;
    }

    &::-webkit-scrollbar-thumb {
      background-color: #d4dae3;
      border-radius: 13px;
    }
  }

  >>> .bk-button-text .bk-icon {
    &.icon-plus-circle {
      top: 0;
    }
  }
}
</style>
