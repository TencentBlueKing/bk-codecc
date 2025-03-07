<template>
  <div class="main-content-outer">
    <div class="new-task">
      <div class="new-task-main">
        <div class="main-top">
          <span
            class="cc-link-primary pr20 f14"
            @click="$router.push({ name: 'task-list' })"
          ><i class="bk-icon icon-angle-left f20"></i>{{ $t('返回') }}</span
          >
          {{ isTestTask ? $t('新建测试任务') : $t('新建任务') }}
        </div>
        <h2 class="main-title"></h2>
        <div class="new-task-content">
          <div class="step-basic">
            <bk-form
              class="pb20"
              :label-width="130"
              :model="formData"
              ref="basicForm"
            >
              <bk-form-item
                :label="$t('任务名称')"
                :required="true"
                :rules="formRules.nameCn"
                property="nameCn"
              >
                <bk-input v-model.trim="formData.nameCn"></bk-input>
              </bk-form-item>
              <bk-form-item
                :label="$t('任务语言')"
                :required="true"
                property="codeLang"
                :rules="formRules.codeLang"
              >
                <span v-if="isTestTask" class="text-[12px]">
                  {{ codeLangDisplay.join('; ') }}
                </span>
                <bk-checkbox-group
                  v-else
                  v-model="formData.codeLang"
                  @change="handleLangChange"
                  class="checkbox-lang"
                >
                  <bk-checkbox
                    v-for="lang in toolMeta.LANG"
                    :key="lang.key"
                    :value="parseInt(lang.key, 10)"
                    class="item fs12"
                  >{{ lang.fullName }}</bk-checkbox
                  >
                </bk-checkbox-group>
              </bk-form-item>
              <bk-form-item
                :label="$t('multi.规则集')"
                v-if="isTestTask"
              >
                <div v-for="item in checkerSetList" :key="item.entityId" class="text-[12px]">
                  {{ item.checkerSetName }}
                </div>
              </bk-form-item>
              <bk-form-item
                :label="$t('multi.规则集')"
                style="height: 50px"
                v-if="Object.keys(checkersetMap).length"
              >
                <span
                  class="select-tool cc-ellipsis"
                  :title="toolCnList.join('、')"
                  v-if="toolCnList.length"
                >{{ $t('涉及工具') }} {{ toolCnList.join('、') }}</span
                >
              </bk-form-item>
              <checkerset-select
                ref="checkerSetList"
                :data="checkersetMap"
                @handleToolChange="handleToolChange"
              >
              </checkerset-select>
            </bk-form>
            <bk-form :label-width="130">
              <div
                v-for="toolParam in toolConfigParams"
                class="pb20"
                :key="toolParam.name"
              >
                <tool-params-form
                  v-for="param in toolParam.paramsList"
                  :key="param.key"
                  :param="param"
                  :tool="toolParam.name"
                  @handleFactorChange="handleFactorChange"
                >
                </tool-params-form>
              </div>
            </bk-form>
            <tool-config-form
              class="form-add"
              scenes="register-add"
              :tools="toolList"
              :code-lang="formData.codeLang"
              ref="toolConfigForm"
            />
          </div>
        </div>

        <div class="new-task-footer">
          <bk-button
            theme="primary"
            :loading="buttonLoading"
            @click="handleCompleteClick"
          >{{ $t('完成') }}</bk-button
          >
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapState } from 'vuex';
import ToolConfigForm from '@/components/tool-config-form';
import ToolParamsForm from '@/components/tool-params-form';
import checkersetSelect from '@/components/checkerset-select';

export default {
  components: {
    ToolConfigForm,
    ToolParamsForm,
    checkersetSelect,
  },
  data() {
    return {
      formRules: {
        nameCn: [
          {
            required: true,
            message: this.$t('必填项'),
            trigger: 'blur',
          },
          {
            regex: /^[\u4e00-\u9fa5_a-zA-Z0-9]+$/,
            message: this.$t('需由中文、字母、数字或下划线组成'),
            trigger: 'blur',
          },
        ],
        codeLang: [
          {
            required: true,
            message: this.$t('必填项'),
            trigger: 'change',
          },
        ],
      },
      formData: {
        nameCn: '',
        codeLang: [],
      },
      toolList: [],
      checkerset: {},
      checkersetMap: {},
      formValidator: {},
      buttonLoading: false,
      paramsValue: {},
      checkerSetList: [],
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
    toolConfigParams() {
      const toolConfigParams = [];
      const toolParamList = ['GOML', 'BKCHECK'];
      Object.keys(this.toolMap).forEach((key) => {
        if (toolParamList.includes(key) && this.toolList.includes(key)) {
          try {
            this.toolMap[key].paramsList = this.toolMap[key] && JSON.parse(this.toolMap[key].params);
            // 如果是bkcheck第一个参数是MetricsUri平台不需要展示，需要给清除
            if (key === 'BKCHECK') {
              this.toolMap[key].paramsList.shift()
            }
            // 如果paramsList里面varType是bool类型，需要把varDefault的字符串转成bool类型
            for (const param of this.toolMap[key].paramsList) {
              if (param.varType === 'BOOLEAN') {
                param.varDefault = JSON.parse(param.varDefault)
              }
            }
            toolConfigParams.push(this.toolMap[key]);
          } catch (error) {
            console.error(error);
          }
        }
      });
      return toolConfigParams;
    },
    toolCnList() {
      return this.toolList.map(item => this.toolMap[item] && this.toolMap[item].displayName);
    },
    isTestTask() {
      return this.$route.query.isTestTask;
    },
    codeLangDisplay() {
      return this.formData.codeLang.map((lang) => {
        const { fullName } = this.toolMeta.LANG.find(item => Number(item.key) === lang);
        return fullName;
      });
    },
  },
  created() {
    this.init();
  },
  methods: {
    init() {
      this.$store.dispatch('checkerset/params');
      this.$store.dispatch('checkerset/count', { projectId: this.projectId });
      this.$store.commit('task/updateDetail', {});
      this.$store.dispatch('checkerset/categoryList').then((res) => {
        this.checkerset = res;
      });
      this.$store.dispatch('project/getProjectInfo');
      if (this.isTestTask && this.$route.query.toolName) {
        this.$store.dispatch('test/getToolList', this.$route.query.toolName).then((res) => {
          console.log('🚀 ~ this.$store.dispatch ~ res:', res);
          this.checkerSetList = res?.checkerSetList || [];
          const language = Array.from(new Set(this.checkerSetList.map(item => item.codeLang)));
          console.log('🚀 ~ this.$store.dispatch ~ language:', language);
          this.formData.codeLang = language;
        });
      }
    },
    handleLangChange(newValue) {
      const [formItem0, formItem] = this.$refs.basicForm.formItems;
      const newCodeLang = newValue || this.formData.codeLang;
      const validator = {
        state: newCodeLang.length > 0,
        content: this.$t('请选择至少一种任务语言'),
        formItem,
        field: 'codeLang',
      };
      this.hintFormItem(validator);

      const { codeLang } = this.formData;
      const checkersetMap = {};
      codeLang.forEach((item) => {
        const { name } = this.toolMeta.LANG.find(lang => Number(lang.key) === item);
        const checkerset = { ...this.checkerset };
        Object.keys(checkerset).forEach((key) => {
          checkerset[key] = checkerset[key].filter(checker => checker.codeLang & item);
        });
        const list = checkerset;
        checkersetMap[name] = list;
      });
      this.$refs.checkerSetList.handleChange();
      this.checkersetMap = checkersetMap;
    },
    hintFormItem(validator) {
      const { state, formItem, field, content } = validator;
      this.formValidator[field] = validator;
      if (state) {
        formItem.clearValidator();
      } else {
        const { validator } = formItem;
        setTimeout(() => {
          validator.state = 'error';
          validator.content = content;
        }, 100);
      }
    },
    handleToolChange(toolList) {
      this.toolList = toolList;
    },
    handleCompleteClick() {
      if (!this.isTestTask) {
        if (!this.$refs.checkerSetList.handleValidate()) return false; // 规则集验证
        this.handleLangChange();
      }
      this.$refs.basicForm.validate().then(
        (validator) => {
          let hasError = false;
          Object.keys(this.formValidator).forEach((key) => {
            const validator = this.formValidator[key];
            if (!validator.state) {
              this.hintFormItem(validator);
              hasError = true;
            }
          });
          if (hasError) {
            return false;
          }
          this.$refs.toolConfigForm.$refs.codeForm.validate().then(
            (validator) => {
              this.submitData();
            },
            validator => false,
          );
        },
        validator => false,
      );
    },
    submitData() {
      const codeData = this.$refs.toolConfigForm.getSubmitData();
      const checkerSetList = this.$refs.checkerSetList.getCheckerset();
      const devopsToolParams = this.getParamsValue();
      const codeLang = String(this.formData.codeLang.reduce((n1, n2) => n1 + n2, 0));
      let postData = {
        ...this.formData,
        ...codeData,
        checkerSetList,
        codeLang,
        devopsToolParams,
      };
      if (this.isTestTask) {
        postData = {
          ...postData,
          testStage: 1,
          checkerSetList: this.checkerSetList,
          testTool: this.$route.query.toolName,
          testVersion: this.$route.query.version,
        };
      }
      this.buttonLoading = true;
      const url = this.isTestTask ? 'task/addTestTool' : 'task/addTool';
      this.$store
        .dispatch(url, postData)
        .then((res) => {
          // 成功则进入一下步
          this.$router.push({
            name: 'task-detail',
            params: { ...this.$route.params, taskId: res.data.taskId },
            query: { buildNum: 'latest' },
          });
        })
        .catch((e) => {
          console.error(e);
        })
        .finally(() => {
          this.buttonLoading = false;
        });
    },
    getParamsValue() {
      return this.toolConfigParams.flatMap(({ name: toolName, paramsList }) =>
          paramsList.map(({ varName, varDefault = '' }) => {
            let chooseValue = (this.paramsValue[toolName]?.[varName]) ?? varDefault;
            chooseValue = Array.isArray(chooseValue) ? JSON.stringify(chooseValue) : String(chooseValue);
            return { toolName, varName, chooseValue };
          })
      );
    },
    handleFactorChange(factor, toolName) {
      this.paramsValue[toolName] = Object.assign(
        {},
        this.paramsValue[toolName],
        factor,
      );
    },
  },
};
</script>

<style lang="postcss" scoped>
.main-top {
  height: 56px;
  padding: 0 30px;
  line-height: 56px;
  background: #fafbfd;
  border-bottom: 1px solid #dcdee5;
}

.new-task-main {
  --asideHorizontalPadding: 32px;

  display: block;
  height: calc(100% - 140px);
  min-height: 600px;
  margin-bottom: 60px;
  background: #fff;
  border-radius: 2px;
  box-shadow: 0 2px 12px 0 hsl(0deg 0% 87% / 50%),
    0 2px 13px 0 hsl(0deg 0% 87% / 50%);

  .new-task-aside {
    padding: 28px var(--asideHorizontalPadding);
    background: #fafbfd;
    border-right: 1px solid #dcdee5;
    flex: none;
  }

  .new-task-content {
    width: 800px;
    padding: 12px;
    margin: auto;
    overflow: hidden;
  }
}

.step-basic {
  .bk-form {
    width: 90%;
  }
}

.checkbox-lang {
  .item {
    width: 140px;
    line-height: 30px;
  }
}

.bk-form-checkbox.fs12 {
  >>> .bk-checkbox-text {
    font-size: 12px;
  }
}

.new-task-footer {
  padding-bottom: 20px;
  margin-top: 20px;
  text-align: center;
}

>>> .bk-form .bk-label {
  font-weight: bold;
}

.select-tool {
  display: inline-block;
  max-width: 568px;
  font-size: 12px;
  line-height: 31px;
  color: #999;
}

.icon-angle-left {
  position: relative;
  top: 2px;
}
</style>
