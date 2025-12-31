<template>
  <div>
    <div
      class="disf"
      v-if="
        (taskDetail.atomCode && taskDetail.createFrom === 'bs_pipeline') ||
          taskDetail.createFrom === 'gongfeng_scan' ||
          taskDetail.createFrom === 'api_trigger'
      "
    >
      <span class="pipeline-label">{{ param.labelName }}</span>
      <span class="fs14">{{ param.varDefault }}</span>
    </div>
    <div v-else>
      <bk-form-item :label="$t(param.labelName)" class="pb20">
        <bk-input
          v-bk-tooltips="{
            content: param.varTips,
            width: isToolManage ? 400 : 300,
            placement: isToolManage ? 'bottom' : 'right',
          }"
          @change="handleChange"
          v-model.trim="param.varDefault"
          style="width: 450px"
          v-if="param.varType === 'STRING'"
        >
        </bk-input>

        <bk-input
          type="number"
          @change="handleChange"
          v-model.trim="param.varDefault"
          style="width: 450px"
          v-if="param.varType === 'NUMBER'"
        >
        </bk-input>

        <bk-switcher
            @change="handleChange"
            v-model="param.varDefault"
            v-else-if="param.varType === 'BOOLEAN'"
        >
        </bk-switcher>

        <bk-radio-group
          @change="handleChange"
          v-else-if="param.varType === 'RADIO'"
          v-model="param.varDefault"
          class="radio-param"
        >
          <bk-radio
            v-for="(option, index) in param.varOptionList"
            :value="option.id"
            :key="index"
            class="item pr20"
          >{{ option.name }}</bk-radio
          >
        </bk-radio-group>

        <bk-checkbox-group
          @change="handleMultipleChange"
          v-model="param.varDefault"
          v-else-if="param.varType === 'CHECKBOX'"
          class="checkbox-param"
        >
          <bk-checkbox
            v-for="(option, index) in param.varOptionList"
            :value="option.id"
            :key="index"
            class="item"
          >{{ option.name }}</bk-checkbox
          >
        </bk-checkbox-group>

        <bk-row v-for="(item, index) in macroList"
                :key="item"
                v-if="['MultipleInput', 'ITEM_EDIT'].includes(param.varType)">
          <bk-col class="macro-section">
            <bk-input :placeholder="param.varType === 'MultipleInput' ? '-D表示宏定义，-U表示取消宏定义' : ''"
                      style="width: 300px;"
                      @blur="multipleInputChange(index)"
                      v-model="compile.size[index]">
            </bk-input>
          </bk-col>
          <div class="tool-icon">
            <i class="bk-icon icon-plus" @click="addTool(index)" v-if="index === macroList.length - 1"></i>
            <i class="bk-icon icon-close" @click="deleteTool(index)" v-if="macroList.length > 1 "></i>
          </div>
        </bk-row>
      </bk-form-item>
    </div>
  </div>
</template>

<script>
import { mapState } from 'vuex';

export default {
  name: 'ToolParamsForm',
  props: {
    param: {
      type: Object,
    },
    tool: {
      type: String,
    },
    isToolManage: {
      type: Boolean,
      default: true,
    },
  },
  data() {
    return {
      formRules: {
        inputValue: [
          {
            max: 50,
            message: this.$t('不能多于x个字符', { num: 50 }),
            trigger: 'blur',
          },
        ],
        chooseValue: [
          {
            required: true,
            message: this.$t('必填项'),
            trigger: 'change',
          },
        ],
      },
      compile: {
        size: []
      },
      macroList: [
        {}
      ],
    };
  },
  computed: {
    ...mapState('task', {
      taskDetail: 'detail',
    }),
  },
  created() {
    // 处理多选项框输入，解析到列表，ITEM_EDIT是插件后统一的格式，平台兼容保持一致
    if (['MultipleInput', 'ITEM_EDIT'].includes(this.param.varType)) {
      if (this.param.varDefault?.length) {
        this.compile.size = this.param.varDefault;
      } else { // 防止非数组默认值改变类型兜底
        this.compile.size = [""];
      }
      this.macroList = this.compile.size.slice();
      this.handleChangeByVarType(this.macroList);
    }
  },
  methods: {
    // bkcheck宏定义在插件有特殊操作，平台创建还是json数组处理格式。
    handleChange(value) {
      const factor = {};
      factor[this.param.varName] = value;
      this.$emit('handleFactorChange', factor, this.tool);
    },
    handleMultipleChange(value) {
      // 空值检查（兼容param对象未定义的情况）
      if (!this.param?.varName) return
      // 新工具复选都是逗号分隔字符串格式
      const commaSeparatedString = Array.isArray(value)
          ? value.join(',')
          : (value ?? '');
      this.$emit('handleFactorChange', {
        [this.param.varName]: commaSeparatedString  // 直接计算属性名
      }, this.tool);
    },
    // 添加分级列表
    addTool(index) {
      if (this.compile.size[index]) {
        this.macroList.push("");
        this.compile.size.push("")
        this.param.varDefault = this.macroList;
      }
    },
    // 删除分级列表
    deleteTool(index) {
      if (this.macroList.length > 1) {
        this.macroList.splice(index, 1);
        this.compile.size.splice(index, 1);
      } else if (this.macroList.length === 1) {
        this.macroList = [""];
        this.compile.size = [""];
      }
      this.param.varDefault = this.macroList;
    },
    multipleInputChange(index) {
      this.macroList[index] = this.compile.size[index];
      this.param.varDefault = this.macroList;
      this.handleChangeByVarType(this.macroList);
    },
    // MultipleInput为以前兼容的旧格式传参， ITEM_EDIT跟插件格式保持一致按照逗号字符串传参
    handleChangeByVarType(macroList) {
      if (this.param.varType === 'MultipleInput') {
        this.handleChange(macroList);
      } else {
        this.handleMultipleChange(macroList);
      }
    }
  },
};
</script>

<style lang="postcss" scoped>
.pipeline-label {
  display: inline-block;
  width: 104px;
  height: 46px;
  font-size: 14px;
  font-weight: 600;
  line-height: 14px;
  text-align: left;
}
.radio-param {
  .item {
    margin-right: 16px;
  }
}
.macro-section {
  height: 45px;
}
.checkbox-param {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;

  .item {
    margin-right: 12px;
    margin-bottom: 8px;
    line-height: 25px;

    input[type="checkbox"] {
      margin-right: 10px;
    }
  }

  .ace-wrapper {
    padding: 5px 8px;
  }
}

</style>
