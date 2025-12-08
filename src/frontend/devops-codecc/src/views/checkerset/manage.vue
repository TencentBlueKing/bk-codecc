<template>
  <div class="checkerset-manage">
    <!-- <header>
            <span class="breadcrumb-txt breadcrumb-back" @click="$router.go(-1)">规则集</span>
            <i class="bk-icon icon-angle-right fs12"></i>
            <span class="breadcrumb-txt breadcrumb-name">管理规则集</span>
        </header> -->
    <div class="checkerset-info" v-if="detailInfo.checkerSetId">
      <span class="breadcrumb-txt" @click="back"
      ><i class="bk-icon icon-angle-left"></i>{{ $t('返回') }}</span
      >
      <div class="col-item checkerset-name" :title="detailInfo.checkerSetName">
        {{ detailInfo.checkerSetName }}
      </div>
      <div
        class="col-item checkerset-desc"
        v-if="detailInfo.description"
        :title="detailInfo.description"
      >
        {{ detailInfo.description }}
      </div>
      <div class="col-item code-lang">
        <span>{{ $t('语言') }}</span> {{ getCodeLang(detailInfo.codeLang) }}
      </div>
      <div class="col-item">
        <span
          class="checker-tag"
          v-for="(category, index) in detailInfo.catagories"
          :key="index"
        >{{ isEn ? category.enName : category.cnName }}</span
        >
      </div>
      <span class="codecc-icon icon-edit" @click="edit"></span>
    </div>
    <checker-list
      ref="checkerList"
      v-if="detailInfo.codeLang"
      :is-config="true"
      :has-no-permission="hasNoPermission"
      :checkerset-conf="checkersetConf"
      :handle-select-rules="handleSelectRules"
      :selected-conf="selectedRuleList"
      :save-conf="saveConf"
      :local-rule-param="localRuleParam"
      :update-conf-parame="updateConfParame"
    ></checker-list>
    <create
      :visible.sync="sliderVisible"
      :is-edit="true"
      :has-permission="!hasNoPermission"
      :has-detail="true"
      :edit-obj="editObj"
      :refresh-detail="getCheckersetDetail"
    ></create>
  </div>
</template>

<script>
import { mapState } from 'vuex';
import checkerList from '../checker/list';
import create from './create';
import { language } from '../../i18n';

export default {
  components: { checkerList, create },
  props: {
    checkersetId: {
      type: String,
      default: '',
    },
    version: {
      type: String,
      default: '',
    },
    isFromSettings: {
      type: Boolean,
      default: false,
    },
    updateCheckerList: {
      type: Function,
    },
  },
  data() {
    return {
      sliderVisible: false,
      permissionList: [],
      selectedRuleList: [],
      localRuleParam: [],
      detailInfo: {},
      editObj: {},
    };
  },
  computed: {
    ...mapState(['toolMeta']),
    projectId() {
      return this.$route.params.projectId;
    },
    isPertainProject() {
      return (
        this.detailInfo.projectId
        && this.detailInfo.projectId === this.projectId
      );
    },
    hasNoPermission() {
      return (
        this.detailInfo.checkerSetId
        && ((this.detailInfo.legacy && this.detailInfo.codeLangList.length > 1) // 单语言可编辑
          || !this.isPertainProject
          || !this.permissionList.some(item => ['MANAGER', 'CREATOR'].includes(item)))
      );
    },
    checkersetConf() {
      if (this.detailInfo.checkerSetId) {
        const conf = {
          codeLang: this.detailInfo.codeLangList,
          checkerSetId: this.detailInfo.checkerSetId,
          version: this.detailInfo.version,
          legacy: this.detailInfo.legacy,
          codeLangList: this.detailInfo.codeLangList,
          toolList: this.detailInfo.toolList,
          sourceProjectId: this.detailInfo.projectId,
        };
        return conf;
      }
      return {};
    },
    isEn() {
      return language === 'en-US';
    },
  },
  mounted() {
    console.log(language);
    this.checkPermission();
    this.getCheckersetDetail();
  },
  methods: {
    async getCheckersetDetail() {
      const checkersetId = this.checkersetId || this.$route.params.checkersetId;
      const version = this.version || this.$route.params.version;
      const params = { checkersetId, version };
      const res = await this.$store.dispatch('checkerset/detail', params);
      this.detailInfo = res;
      this.selectedRuleList = this.detailInfo.checkerProps
        ? this.detailInfo.checkerProps
        : [];
      if (this.selectedRuleList.length) {
        this.detailInfo.checkerProps.forEach((item) => {
          let temp = {};
          if (item.props) {
            temp = {
              ...item,
              propName: JSON.parse(item.props)[0].propName,
              propValue: JSON.parse(item.props)[0].propValue,
            };
            this.localRuleParam.push(temp);
          }
        });
      }
    },
    async checkPermission() {
      const params = {
        projectId: this.projectId,
        user: this.$store.state.user.username,
        checkerSetId: this.checkersetId || this.$route.params.checkersetId,
      };
      const res = await this.$store.dispatch('checkerset/permission', params);
      this.permissionList = res.data;
    },
    back() {
      if (this.isFromSettings) {
        this.$emit('closeDialog');
      } else {
        this.$router.push({ name: 'checkerset-list' });
      }
    },
    edit() {
      const catagories = this.detailInfo.catagories.map(category => category.enName);
      this.editObj = { ...this.detailInfo, catagories };
      this.sliderVisible = true;
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
    updateConfParame(list) {
      this.localRuleParam = list;
    },
    handleSelectRules(data, isChecked, isBatch) {
      if (isBatch) {
        const validArr = data.filter(val => val);
        if (isChecked) {
          validArr.forEach((val) => {
            this.selectedRuleList.push(val);
          });
          const obj = {};
          this.selectedRuleList = this.selectedRuleList.reduce((cur, next) => {
            if (!obj[`${next.checkerKey}-${next.toolName}`]) {
              obj[`${next.checkerKey}-${next.toolName}`] = true;
              cur.push(next);
            }
            return cur;
          }, []);
        } else validArr.forEach(val => this.selectedRuleList.splice(
          this.selectedRuleList.findIndex(item => item.checkerKey === val.checkerKey
                  && item.toolName === val.toolName),
          1,
        ));
      } else {
        if (isChecked) {
          this.selectedRuleList.push(data);
        } else this.selectedRuleList.splice(
          this.selectedRuleList.findIndex(item => item.checkerKey === data.checkerKey
                && item.toolName === data.toolName),
          1,
        );
      }
    },
    async submit() {
      const version = this.version || this.$route.params.version;
      const checkersetId = this.checkersetId || this.$route.params.checkersetId;
      const params = { checkersetId, version };
      let hasGrayChecker = false;
      let hasTestChecker = false;
      const checkerProps = this.selectedRuleList.map((checker) => {
        const temp = {
          toolName: checker.toolName,
          checkerKey: checker.checkerKey,
        };
        if (checker.checkerVersion === -2) {
          hasGrayChecker = true;
        }
        if (checker.checkerVersion === -1) {
          hasTestChecker = true;
        }
        const matchItem = this.localRuleParam.findIndex(item => item.checkerKey === checker.checkerKey);
        if (matchItem > -1) {
          temp.props = this.localRuleParam[matchItem].props;
        } else {
          temp.props = checker.props;
        }
        return temp;
      });
      Object.assign(params, { checkerProps });
      bus.$emit('show-app-loading');
      await this.$store.dispatch('checkerset/save', params).then((res) => {
        if (res.code === '0') {
          let nextVersion = this.detailInfo.version;
          let nextVersionText = `V${nextVersion}`;
          if (nextVersion === -2 || hasGrayChecker) {
            nextVersion = -2;
            nextVersionText = this.$t('灰度');
          } else if (nextVersion === -1 || hasTestChecker) {
            nextVersion = -1;
            nextVersionText = this.$t('测试');
          } else if (this.detailInfo.initCheckers || this.detailInfo.legacy) {
            nextVersion += 1;
            nextVersionText = `V${nextVersion}`;
          }

          this.$bkMessage({
            theme: 'success',
            message: this.$t('规则配置已保存至{0}版本', [nextVersionText]),
          });
          // this.getCheckersetDetail()
          if (this.isFromSettings) {
            this.updateCheckerList();
          } else if (this.detailInfo.initCheckers || hasGrayChecker || hasTestChecker) {
            const link = {
              name: 'checkerset-manage',
              params: {
                projectId: this.projectId,
                checkersetId: this.detailInfo.checkerSetId,
                version: nextVersion,
              },
            };
            this.$router.push(link);
          } else {
            this.getCheckersetDetail();
          }
        }
      })
        .finally(() => {
          bus.$emit('hide-app-loading');
        });
    },
    saveConf() {
      if (this.selectedRuleList.length && !this.hasNoPermission) {
        let nextVersion = this.detailInfo.version;
        if (nextVersion === -2) {
          nextVersion = this.$t('灰度');
        } else if (nextVersion === -1) {
          nextVersion = this.$t('测试');
        } else if (this.detailInfo.initCheckers || this.detailInfo.legacy) {
          nextVersion = `V${nextVersion + 1}`;
        } else {
          nextVersion = `V${nextVersion}`;
        }
        this.$bkInfo({
          title: this.$t('确认'),
          subTitle: this.$t('是否保存规则配置，并创建新的规则集版本{0}？', [
            nextVersion,
          ]),
          maskClose: true,
          confirmFn: () => {
            this.submit();
          },
        });
      }
    },
  },
};
</script>

<style lang="postcss" scoped>
.checkerset-manage {
  padding: 0 40px;

  header {
    padding-bottom: 8px;
    margin-bottom: 10px;
    font-size: 14px;
    border-bottom: 1px solid #dcdee5;

    .breadcrumb-back {
      color: #3a84ff;
      cursor: pointer;
    }
  }

  .breadcrumb-txt {
    margin-right: 20px;
    color: #3a84ff;
    cursor: pointer;

    .bk-icon {
      position: relative;
      top: 1px;
      font-size: 20px;
      font-weight: 600;
    }
  }

  .checkerset-info {
    display: flex;
    align-items: center;
    font-size: 14px;
    color: #63656d;
  }

  .checkerset-name {
    max-width: 196px;
    margin-right: 14px;
    overflow: hidden;
    font-size: 16px;
    color: #333;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .checkerset-desc,
  .code-lang {
    padding-right: 16px;
    margin-right: 16px;
    border-right: 1px solid #dcdee5;
  }

  .checkerset-desc {
    max-width: 196px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .checker-tag {
    display: inline-block;
    padding: 2px 8px;
    margin-left: 4px;
    font-size: 12px;
    background: #c9dffa;
    border-radius: 2px;
  }

  .icon-edit {
    margin-left: 32px;
    font-size: 16px;
    cursor: pointer;

    &:hover {
      color: #3a84ff;
    }
  }

  .disable-edit {
    color: #dcdee5;

    &:hover {
      color: #dcdee5;
    }
  }

  .cc-checkers {
    height: calc(100% - 50px);
    padding: 12px 0;
  }
}
</style>
