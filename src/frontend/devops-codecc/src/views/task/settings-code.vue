<template>
  <div
    class="main-content-inner main-content-form"
    :class="editDisabled ? 'from-pipeline' : ''"
  >
    <div v-if="editDisabled" :model="formData">
      <div class="to-pipeline">
        <span v-if="projectId === 'CUSTOMPROJ_PCG_RD'">{{
          $t('基础信息配置由PCG EP度量平台自动生成')
        }}</span>
        <span
          v-else-if="
            taskDetail.createFrom === 'gongfeng_scan' ||
              taskDetail.createFrom === 'api_trigger'
          "
        >{{ $t('基础信息配置由CodeCC开源扫描集群自动生成') }}</span
        >
        <span v-else
        >{{ $t('修改基础信息配置，请前往流水线') }}
          <a @click="handleToPipeline" href="javascript:;">{{
            $t('立即前往>>')
          }}</a></span
        >
      </div>
      <div class="disf">
        <span class="pipeline-label disf">{{ $t('任务名称') }}</span>
        <span class="fs14">{{ formData.nameCn }}</span>
      </div>
      <div class="disf">
        <span class="pipeline-label disf">{{ $t('附加标识') }}</span>
        <span class="fs14">{{ formData.multiPipelineMark }}</span>
      </div>
      <div class="disf">
        <div class="pipeline-label disf"><span>{{ $t('任务语言') }}</span></div>
        <div class="inner-height"><span
          :class="index === 0 ? '' : 'lang'"
          class="fs14"
          v-for="(lang, index) in formatLang(taskDetail.codeLang)"
          :key="lang"
        >{{ lang }}</span></div>
      </div>
      <bk-form :label-width="130">
        <div v-for="toolParam in toolConfigParams" :key="toolParam.name">
          <tool-params-form
            v-for="param in toolParam.paramsList"
            :key="param.key"
            :param="param"
            :tool="toolParam.name"
          >
          </tool-params-form>
        </div>
      </bk-form>
      <tool-config-form
        class="form-edit"
        scenes="manage-edit"
        :code-message="codeMessage"
        :tools="configData"
        :code-lang="formData.codeLang"
        @saveBasic="submitData"
        @handleFactorChange="handleFactorChange"
      >
      </tool-config-form>
      <div class="disf">
        <span class="pipeline-label disf">{{ $t('创建人') }}</span>
        <span class="fs14">{{ formData.createdBy }}</span>
      </div>
      <div class="disf">
        <span class="pipeline-label disf">{{ $t('创建时间') }}</span>
        <span class="fs14">{{ formData.createdDate | formatDate }}</span>
      </div>
      <div class="disf">
        <span class="pipeline-label disf">{{ $t('创建来源') }}</span>
        <span class="fs14">{{ formatCreateFrom(formData.taskType) }}</span>
      </div>
    </div>
    <div v-else>
      <bk-form :label-width="130" :model="formData" ref="basicInfo">
        <bk-form-item
          :label="$t('任务名称')"
          :required="true"
          :rules="formRules.nameCn"
          property="nameCn"
        >
          <bk-input v-model.trim="formData.nameCn"></bk-input>
        </bk-form-item>
        <!-- <bk-form-item :label="$t('英文ID')" property="taskDetail.nameEn">
                    <bk-input v-model="formData.nameEn" readonly></bk-input>
                </bk-form-item> -->
        <bk-form-item
          :label="$t('任务语言')"
          class="pb15"
          :required="true"
          :rules="formRules.codeLang"
          property="codeLang"
        >
          <bk-checkbox-group
            v-model="formData.codeLang"
            @change="handleLangChange"
            class="checkbox-lang"
          >
            <bk-checkbox
              v-for="lang in toolMeta.LANG"
              :key="lang.key"
              :value="parseInt(lang.key)"
              class="item fs12"
            >{{ lang.fullName }}</bk-checkbox
            >
          </bk-checkbox-group>
        </bk-form-item>
        <bk-form :label-width="130">
          <div v-for="toolParam in toolConfigParams" :key="toolParam.name">
            <tool-params-form
              v-for="param in toolParam.paramsList"
              :key="param.key"
              :param="param"
              :tool="toolParam.name"
            >
            </tool-params-form>
          </div>
        </bk-form>
        <bk-form-item label-width="0" class="cc-code-message">
          <tool-config-form
            class="form-edit"
            scenes="manage-edit"
            :code-message="codeMessage"
            :tools="configData"
            :code-lang="formData.codeLang"
            :is-tool-manage="taskDetail.createFrom === 'bs_pipeline'"
            @saveBasic="submitData"
            @handleFactorChange="handleFactorChange"
          >
          </tool-config-form>
        </bk-form-item>
      </bk-form>
    </div>
  </div>
</template>

<script>
import { mapState } from 'vuex';
import ToolConfigForm from '@/components/tool-config-form';
import ToolParamsForm from '@/components/tool-params-form';

export default {
  components: {
    ToolConfigForm,
    ToolParamsForm,
  },
  data() {
    return {
      formData: {},
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
          {
            max: 50,
            message: this.$t('不能多于x个字符', { num: 50 }),
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
      paramsValue: {},
      buttonLoading: false,
      codeMessage: {},
    };
  },
  computed: {
    ...mapState(['toolMeta']),
    ...mapState('tool', {
      toolMap: 'mapList',
    }),
    ...mapState('task', {
      taskDetail: 'detail',
    }),
    configData() {
      const configData = [];
      Object.values(this.taskDetail.enableToolList).forEach((value) => {
        configData.push(value.toolName);
      });
      return configData;
    },
    toolConfigParams() {
      const toolConfigParams = [];
      const toolParamList = ['PYLINT', 'GOML', 'BKCHECK'];
      Object.keys(this.toolMap).forEach((key) => {
        const toolParams = this.taskDetail.enableToolList.find(item => item.toolName === key);
        if (toolParamList.includes(key) && toolParams) {
          try {
            const paramJson = toolParams.paramJson && JSON.parse(toolParams.paramJson);
            let paramsList = this.toolMap[key] && JSON.parse(this.toolMap[key].params);
            paramsList = paramsList.map((item) => {
              const { varName } = item;
              if (paramJson && paramJson[varName]) {
                item.varDefault = JSON.parse(paramJson[varName]);
              }
              return item;
            });
            this.toolMap[key].paramsList = paramsList;
            // 如果是bkcheck第一个参数是MetricsUri平台不需要展示，需要给清除
            if (key === 'BKCHECK') {
              this.toolMap[key].paramsList.shift();
            }
            toolConfigParams.push(this.toolMap[key]);
          } catch (error) {
            // console.error(error);
          }
        }
      });
      return toolConfigParams;
    },
    editDisabled() {
      // 最老的v1插件atomCode为空，但createFrom === 'bs_pipeline', 也可编辑
      return (
        (this.taskDetail.atomCode
          && this.taskDetail.createFrom === 'bs_pipeline')
        || this.taskDetail.createFrom === 'gongfeng_scan'
        || this.taskDetail.createFrom === 'api_trigger'
      );
    },
    projectId() {
      return this.$route.params.projectId;
    },
  },
  watch: {
    formData: {
      handler() {
        this.handleFormDataChange();
      },
      deep: true,
    },
  },
  created() {
    this.init();
  },
  methods: {
    handleFormDataChange() {
      window.changeAlert = true;
    },
    formatLang(num) {
      return this.toolMeta.LANG.map(lang => (lang.key & num ? lang.name : '')).filter(name => name);
    },
    async init() {
      const params = { taskId: this.$route.params.taskId, showLoading: true };
      const res = await this.$store.dispatch('task/basicInfo', params);
      if (!this.toolMeta.LANG.length) {
        const res = await this.$store.dispatch('getToolMeta');
        this.LANG = res.LANG;
      }
      res.codeLang = this.toolMeta.LANG.map(lang => lang.key & res.codeLang).filter(lang => lang > 0);
      this.formData = res;
      this.codeMessage = await this.$store.dispatch('task/getCodeMessage');
      this.$nextTick(() => {
        window.changeAlert = false;
      });
    },
    handleLangChange(newValue) {
      const newCodeLang = newValue || this.formData.codeLang;
      const [formItem0, formItem] = this.$refs.basicInfo.formItems;
      if (newCodeLang.length) {
        formItem.clearValidator();
      } else {
        const { validator } = formItem;
        const msg = this.$t('请选择至少一种任务语言');
        setTimeout(() => {
          validator.state = 'error';
          validator.content = msg;
        }, 100);
      }
    },
    submitData() {
      this.handleLangChange();
      const { formItems } = this.$refs.basicInfo;
      const devopsToolParams = this.getParamsValue();
      let hasError = false;
      for (let index = 0; index < formItems.length; index++) {
        if (
          formItems[index].validator
          && formItems[index].validator.state === 'error'
        ) hasError = true;
      }

      if (!hasError) {
        this.buttonLoading = true;
        this.$refs.basicInfo.validate().then(
          (validator) => {
            const params = {
              taskId: this.$route.params.taskId,
              nameCn: this.formData.nameCn,
              codeLang: String(this.formData.codeLang.reduce((n1, n2) => n1 + n2, 0)),
              devopsToolParams,
            };
            this.$store
              .dispatch('task/updateBasicInfo', params)
              .then((res) => {
                if (res === true) {
                  this.$bkMessage({
                    theme: 'success',
                    message: this.$t('修改成功'),
                  });
                  this.$nextTick(() => {
                    window.changeAlert = false;
                  });
                }
              })
              .catch((e) => {
                console.error(e);
              })
              .finally(() => {
                this.buttonLoading = false;
              });
          },
          (validator) => {
            // console.log(validator)
          },
        );
      }
    },
    handleFactorChange(factor, toolName) {
      this.paramsValue[toolName] = Object.assign(
        {},
        this.paramsValue[toolName],
        factor,
      );
    },
    getParamsValue() {
      return this.toolConfigParams.flatMap(({ name: toolName, paramsList }) => paramsList.map(({ varName, varDefault = '' }) => {
        let chooseValue = (this.paramsValue[toolName]?.[varName]) ?? varDefault;
        chooseValue = Array.isArray(chooseValue) ? JSON.stringify(chooseValue) : String(chooseValue);
        return { toolName, varName, chooseValue };
      }));
    },
    handleToPipeline() {
      if (/^git_\d+$/.test(this.projectId)) {
        window.open(
          `${window.STREAM_SITE_URL}/pipeline/${this.taskDetail.pipelineId}
#${this.taskDetail.projectName}`,
          '_blank',
        );
      } else {
        window.open(
          `${window.DEVOPS_SITE_URL}/console/pipeline/${this.taskDetail.projectId}/
${this.taskDetail.pipelineId}/edit#${this.taskDetail.atomCode}`,
          '_blank',
        );
      }
    },
    formatCreateFrom(createFrom) {
      const fromMap = {
        bs_pipeline: this.$t('流水线'),
        timing_scan: this.$t('开源治理'),
        api_trigger: this.$t('API'),
        bs_codecc: this.$t('自建任务'),
      };
      return fromMap[createFrom];
    },
  },
};
</script>

<style lang="postcss" scoped>
>>> .bk-form-input[readonly] {
  background: transparent !important;
  border: 0 0;
}

>>> .bk-form .bk-form-content {
  width: 620px;
}

.inner-height {
  width: 88%;
  height: 60px;
}

.checkbox-lang {
  .item {
    width: 155px;
    line-height: 30px;
  }
}

.bk-form-checkbox.fs12 {
  >>> .bk-checkbox-text {
    font-size: 12px;
  }
}

.from-pipeline {
  padding: 0 35px 0 20px;

  .to-pipeline {
    height: 32px;
    margin-bottom: 20px;
    font-size: 12px;
    border-bottom: 1px solid #e3e3e3;

    a {
      margin-left: 12px;
    }
  }

  .pipeline-label {
    display: inline-block;
    width: 104px;

    /* line-height: 14px; */
    height: 46px;
    font-size: 14px;
    font-weight: 600;
    text-align: left;
  }

  .lang::before {
    top: 12px;
    right: 0;

    /* width: 30px; */
    height: 12px;
    padding: 0 10px;
    color: #d8d8d8;
    content: ' | ';
  }
}

>>> .bk-form-label {
  text-align: left;
}

>>> .cc-code-message > .bk-label {
  min-height: 0;
  line-height: 0;
}
</style>
