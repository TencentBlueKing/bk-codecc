<template>
  <bk-dialog v-model="visible" :title="title" width="800px" padding="30px 15px" z-index="500" ref="dialog" @cancel="handelClose()" :header-position="'left'">
    <hr style=" width: 103.2%; margin-top: -5px;color: #D3D3D3">
    <bk-form :label-width="100" :model="formData" :rules="rules" ref="formData" style="margin-top: 27px">
      <bk-form-item :label="$t('规则名称')" :required="true" :property="'checkerName'" error-display-type="normal">
        <bk-input
          :disabled="isCheckerName"
          v-model.trim="formData.checkerName"
          :placeholder="$t('仅支持英文大小写，例如{0}', ['AndroidSandBoxPath'])">
        </bk-input>
      </bk-form-item>
      <bk-form-item :label="$t('适用语言')" :required="true" :property="'checkerLanguage'" error-display-type="normal">
        <bk-select
          :z-index="3001"
          v-model="formData.checkerLanguage"
          show-select-all
          multiple
          display-tag>
          <bk-option
            v-for="codeLang in codeLangs"
            :key="codeLang.displayName"
            :id="codeLang.displayName"
            :name="codeLang.displayName">
          </bk-option>
        </bk-select>
      </bk-form-item>
      <bk-form-item :label="$t('类别')" :required="true" :property="'checkerCategory'" error-display-type="normal">
        <bk-select
          :disabled="false"
          v-model="formData.checkerCategory"
          ext-cls="select-custom"
          ext-popover-cls="select-popover-custom">
          <bk-option
            v-for="option in checkerCategoryList"
            :key="option.code"
            :id="option.code"
            :name="option.name">
          </bk-option>
        </bk-select>
      </bk-form-item>
      <bk-form-item :label="$t('严重级别')" :required="true" :property="'severity'" error-display-type="normal">
        <bk-select
          :disabled="false"
          v-model="formData.severity"
          ext-cls="select-custom"
          ext-popover-cls="select-popover-custom"
        >
          <bk-option
            v-for="option in severityList"
            :key="option.id"
            :id="option.id"
            :name="option.name">
          </bk-option>
        </bk-select>
      </bk-form-item>
      <bk-form-item :label="$t('工具')">
        <bk-select
          disabled
          v-model="toolName"
          ext-cls="select-custom"
          ext-popover-cls="select-popover-custom">
          <bk-option
            v-for="option in toolList"
            :key="option.id"
            :id="option.id"
            :name="option.displayName">
          </bk-option>
        </bk-select>
      </bk-form-item>
      <bk-form-item :label="$t('标签')" :required="true" :property="'checkerTag'" error-display-type="normal">
        <bk-tag-input
          v-model="formData.checkerTag"
          :placeholder="$t('支持输入多个标签，用回车键间隔，例如{0}', ['Android、UE、Unity3D'])"
          :list="checkerTagList"
          :allow-create="allowCreate"
          :allow-auto-match="false"
          :free-paste="true"
          :has-delete-icon="hasDeleteIcon">
          @change="tagChange">
        </bk-tag-input>
      </bk-form-item>
      <bk-form-item :label="$t('参数')">
        <div style=" display: flex;margin-left: 10px; flex-direction: column; gap: 1px;">
          <bk-form-item label-width="50" label-right-align label="regex:">
            <bk-input
              v-model.trim="paramData.regex"
              :placeholder="$t('请输入需要匹配出问题代码的正则表达式，例如：{0}', ['\'\\\\sgoto\\\\s\''])">
            </bk-input>
          </bk-form-item>
          <bk-form-item label-width="50" label-right-align label="msg:" style="margin-top: 2px;">
            <bk-input
              v-model.trim="paramData.msg"
              :placeholder="$t('请输入匹配出问题代码后给用户的情况，例如：禁止使用{0}', ['goto'])">
            </bk-input>
          </bk-form-item>
        </div>
      </bk-form-item>
      <bk-form-item :label="$t('描述')">
        <bk-input
          v-model.trim="formData.checkerDesc"
          maxlength="180"
          :placeholder="$t('请输入')"
          :type="'textarea'">
        </bk-input>
      </bk-form-item>
      <bk-form-item :label="$t('详细说明')">
        <bk-input
          v-model.trim="formData.checkerDescModel"
          maxlength="180"
          :placeholder="$t('请输入')"
          :type="'textarea'">
        </bk-input>
      </bk-form-item>
      <bk-form-item :label="$t('错误示例')">
        <bk-input
          v-model.trim="formData.errExample"
          :type="'textarea'"
          :placeholder="$t('请输入')">
        </bk-input>
      </bk-form-item>
      <bk-form-item :label="$t('正确示例')">
        <bk-input
          v-model.trim="formData.codeExample"
          :type="'textarea'"
          :placeholder="$t('请输入')">
        </bk-input>
      </bk-form-item>
      <bk-form-item :label="$t('可见范围')">
        <span>{{ projectInfo.project_name }}</span>
      </bk-form-item>
    </bk-form>
    <div slot="footer" class="dialog-footer">
      <bk-button :loading="isLoading" size="small" theme="primary" @click="submitForm"> {{ $t('确 认') }} </bk-button>
      <bk-button size="small" @click="handelClose()">{{ $t('取 消') }}</bk-button>
    </div>
  </bk-dialog>
</template>

<script>
import { mapState } from 'vuex';

export default {
  // name: 'registerGrayProject',
  props: {
    refreshDetail: Function,
  },
  data() {
    return {
      toolName: 'RegexScan',
      formData: {
        checkerName: '',
        checkerDesc: '',
        checkerLanguage: [],
        severity: '',
        checkerCategory: '',
        checkerTag: [],
        checkerRecommend: 'USER_DEFINED',
        checkerProps: [],
        editable: true,
        errExample: '',
        codeExample: '',
        checkGranularity: 'SINGLE_LINE',
        checkerDescModel: '',
      },
      isCheckerName: false,
      isEdit: false,
      title: '',
      visible: false,
      isLoading: false,
      initialLanguages: [], // 初始化可选的语言
      checkerCategoryList: [
        { code: 'CODE_DEFECT', name: this.$t('代码缺陷') },
        { code: 'CODE_FORMAT', name: this.$t('代码规范') },
        { code: 'SECURITY_RISK', name: this.$t('安全漏洞') },
        { code: 'COMPLEXITY', name: this.$t('圈复杂度') },
        { code: 'DUPLICATE', name: this.$t('重复率') },
      ],
      severityList: [
        { id: 1, name: this.$t('严重') },
        { id: 2, name: this.$t('一般') },
        { id: 3, name: this.$t('提示') },
      ],
      codeLangs: [],
      allowCreate: true,
      hasDeleteIcon: true,
      checkerTagList: [
        { id: '潜在错误', name: '潜在错误' },
        { id: '程序卡死', name: '程序卡死' },
      ],
      paramData: {
        regex: '',
        msg: '',
      },
      toolList: [
        { id: 'RegexScan', displayName: 'RegexScan' },
      ],
      rules: {
        checkerName: [
          {
            required: true,
            message: this.$t('规则名称不能为空'),
            trigger: 'blur',
          },          {
            regex: /^[a-zA-Z][a-zA-Z_-]*$/,
            message: this.$t('只能包含字母、下划线和中划线，且字母开头'),
            trigger: 'blur',
          },
        ],
        checkerDesc: [
          {
            required: true,
            message: this.$t('规则描述不能为空'),
            trigger: 'blur',
          },
        ],
        checkerLanguage: [
          {
            required: true,
            message: this.$t('代码语言不能为空'),
            trigger: 'blur',
          },
          {
            validator: this.validateLanguages,
            message: this.$t('可以增加语言不能减少'),
            trigger: 'blur',
          },
        ],
        severity: [
          {
            required: true,
            message: this.$t('严重级别不能为空'),
            trigger: 'blur',
          },
        ],
        checkerCategory: [
          {
            required: true,
            message: this.$t('规则类型不能为空'),
            trigger: 'blur',
          },
        ],
        checkerTag: [
          {
            required: true,
            message: this.$t('规则标签不能为空'),
            trigger: 'blur',
          },
        ],
      },
    };
  },
  computed: {
    ...mapState('project', ['projectInfo']),
  },
  created() {
    this.getFormParams();
    this.$store.dispatch('project/getProjectInfo');
  },
  methods: {
    async updateCheckerCustom() {
      const params = this.createParams(true);
      this.$store
        .dispatch('checker/updateCustomeChecker', params)
        .then((res) => {
          if (res.code === '0') {
            this.$bkMessage({
              theme: 'success',
              message: '修改正则规则成功',
              offsetY: 80,
            });
            this.isLoading = false;
            this.handelClose();
          }
        })
        .catch((e) => {
          console.error(e);
        });
    },
    async addCheckerCustom() {
      const params = this.createParams(false);
      this.$store.dispatch('checker/create', params)
        .then((res) => {
          if (res.code === '0') {
            this.$bkMessage({
              theme: 'success',
              message: this.$t('创建正则规则成功'),
              offsetY: 80,
            });
            this.isLoading = false;
            this.handelClose();
          }
        })
        .catch((e) => {
          console.error(e);
        });
    },
    submitForm() {
      this.$refs['formData'].validate().then((validator) => {
        if (this.isEdit) {
          this.updateCheckerCustom();
        } else {
          this.addCheckerCustom();
        }
      }, (validator) => {
        console.log('error submit!!');
        return false;
      });
    },
    handelClose() {
      this.clearForm();
      this.refreshDetail();
      this.$refs['formData'].clearError();
    },
    clearForm() {
      const initialFormData = {
        checkerName: '',
        checkerDesc: '',
        checkerLanguage: [],
        severity: '',
        checkerCategory: '',
        checkerTag: [],
        checkerRecommend: 'USER_DEFINED',
        checkerProps: [],
        editable: true,
        errExample: '',
        codeExample: '',
        checkGranularity: 'SINGLE_LINE',
        checkerDescModel: '',
      };
      this.visible = false;
      Object.assign(this.formData, initialFormData);
      this.paramData.regex = '';
      this.paramData.msg = '';
      this.initialLanguages = [];
      this.isEdit = false;
    },
    async getFormParams() {
      const res = await this.$store.dispatch('checkerset/params');
      const paramsMap = ['codeLangs'];
      paramsMap.forEach((item) => {
        this[item] = res[item];
      });
    },
    allCodeLangSetiing() {
      this.codeLangs.forEach((it) => {
        this.formData.checkerLanguage.push(it.displayName);
      });
      this.initialLanguages = this.formData.checkerLanguage;
    },
    validateLanguages(val) {
      if (this.isEdit) {
        const allLanguagesValid = this.initialLanguages.every(lang => val.includes(lang));
        return allLanguagesValid;
      }
      return true;
    },
    createParams(isModified) {
      const { regex, msg } = this.paramData;
      const { checkerName, codeExample, ...restFormData } = this.formData; // 解构 formData

      const checkerDetailVOList = [{
        ...restFormData, // 合并其他属性
        checkerKey: checkerName,
        rightExample: codeExample,
        checkerName,
        codeExample,
        checkerProps: [
          { propName: 'regex', propValue: regex },
          { propName: 'msg', propValue: msg },
        ],
      }];
      const params = {};
      if (isModified) {
        return {
          ...checkerDetailVOList[0],
          toolName: this.toolName,
        };
      }
      return {
        toolName: this.toolName,
        checkerDetailVOList,
      };
    },
  },
};
</script>

<style scoped>
.input-suffix {
  position: absolute;
  right: 20px;
  color: #999;
  pointer-events: none; /* 防止点击后缀 */
}
</style>
