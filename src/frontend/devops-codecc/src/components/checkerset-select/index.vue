<template>
  <div>
    <bk-form :label-width="130" ref="checkerset">
      <bk-form-item
        v-for="(value, key) in data"
        :key="key"
        :required="true"
        :label="key"
        :property="key"
      >
        <div class="rule-set-input" @click="openSelect(key)">
          <p class="rule-set-value" :title="getValueShow(renderList[key])">
            {{ getValueShow(renderList[key]) }}
          </p>
          <span
            class="placeholder"
            v-if="!selectSets[key] || !selectSets[key].length"
          >{{ $t('请选择') }}</span
          >
          <span
            class="bk-select-clear bk-icon icon-close-circle-shape"
            @click.stop="handleClear(selectSets[key], key)"
          ></span>
        </div>
      </bk-form-item>
    </bk-form>
    <rule-set-dialog
      :visible.sync="dialogVisible"
      :cur-lang="dialogKey"
      :selected-list="renderList[dialogKey]"
      :default-lang="[dialogKey]"
      :handle-select="handleSelect"
    ></rule-set-dialog>
  </div>
</template>

<script>
import RuleSetDialog from './RuleSetDialog';
import { mapState } from 'vuex';
export default {
  name: 'CheckersetSelect',
  components: {
    RuleSetDialog,
  },
  props: {
    data: {
      type: Object,
      default: {},
    },
  },
  data() {
    return {
      selectSets: {},
      dialogVisible: false,
      renderList: {},
      dialogKey: '',
    };
  },
  computed: {
    ...mapState('project', ['projectInfo']),
  },
  watch: {
    data(data) {
      const { selectSets } = this;
      const isIegProj = this.projectInfo?.bgId === '956';
      const bkCheckCheckerSets = ['bkcheck_default_java', 'bkcheck_default_lua', 'bkcheck_default_csharp', 'bkcheck_default_test'];
      Object.keys(data).forEach((key) => {
        if (!selectSets[key] || !selectSets[key].length) {
          const list = [];
          this.renderList[key] = [];
          Object.values(data[key]).forEach((value) => {
            value.forEach((item) => {
              if (item.defaultCheckerSet || (isIegProj && bkCheckCheckerSets.includes(item.checkerSetId))) {
                list.push(item.checkerSetId);
                this.renderList[key].push(item);
              }
            });
          });
          selectSets[key] = list;
        }
      });
      this.handleChange();
      this.selectSets = { ...selectSets };
    },
  },
  methods: {
    handleChange(value, options) {
      const checkerset = this.getCheckerset();
      const toolStr = checkerset.map(item => item.toolList).join();
      const toolList = Array.from(new Set(toolStr.split(','))).filter(item => item);
      this.$emit('handleToolChange', toolList);
    },
    getCheckerset() {
      let checkerset = [];
      Object.keys(this.renderList).forEach((key) => {
        if (this.data[key]) {
          checkerset = checkerset.concat(this.renderList[key]);
        }
      });
      return checkerset;
    },
    handleValidate() {
      let hasError = false;
      const { formItems } = this.$refs.checkerset;
      const { selectSets } = this;
      Object.keys(this.data).forEach((key) => {
        const formItem = formItems.find(item => item.label === key);
        if (!selectSets[key] || !selectSets[key].length) {
          formItem.validator.state = 'error';
          formItem.validator.content = this.$t('必填项');
          hasError = true;
        }
      });
      // for (const key in this.data) {
      //   const formItem = formItems.find(item => item.label === key)
      //   if (!selectSets[key] || !selectSets[key].length) {
      //     formItem.validator.state = 'error'
      //     formItem.validator.content = '必填项'
      //     hasError = true
      //   }
      // }
      return !hasError;
    },
    openSelect(key) {
      this.$refs.checkerset.formItems
        .find(item => item.label === key)
        .clearError();
      this.dialogKey = key;
      this.dialogVisible = true;
    },
    handleClear(val, key) {
      this.$refs.checkerset.formItems
        .find(item => item.label === key)
        .clearError();
      this.selectSets[key] = [];
      this.renderList[key] = [];
      // this.$emit('input', [])
      this.handleChange();
    },
    getValueShow(list = []) {
      const nameList = list.map(val => val.checkerSetName);
      return nameList.join(',');
    },
    handleSelect(checkerSet, isCancel, key) {
      if (isCancel) {
        this.renderList[key] = this.renderList[key].filter(item => item.checkerSetId !== checkerSet.checkerSetId);
      } else {
        this.renderList[key].push(checkerSet);
        this.renderList[key] = this.renderList[key].filter(item => item);
      }

      const newVal = this.renderList[key].map(item => item.checkerSetId);
      // this.$emit('input', newVal)
      this.selectSets[key] = newVal;
      this.handleChange();
    },
  },
};
</script>

<style lang="postcss" scoped>
.checker-label {
  position: relative;
  top: -10px;
  display: inline-block;
  width: 116px;
  padding-right: 16px;
  font-size: 12px;
  line-height: 32px;
  text-align: right;
}

.checker-select {
  display: inline-block;
  width: 567px;
}

.dib {
  display: inline-block;
}

.checker-option-content {
  height: 32px;
}

.checker-option {
  width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  &.checker-name {
    width: 190px;
  }
}

>>> .bk-form .bk-label {
  font-size: 12px;
  font-weight: 400 !important;
}

.tag {
  display: inline-block;
  padding: 2px 8px;
  line-height: 18px;
  background: #c9dffa;
  border-radius: 2px;
}

.bk-selector-create-item {
  cursor: pointer;
}

.rule-set-input {
  position: relative;
  height: 32px;
  padding: 0 20px 0 10px;
  overflow: hidden;
  font-size: 12px;
  line-height: 30px;
  color: #63656e;
  cursor: pointer;
  border: 1px solid #c4c6cc;
  border-radius: 2px;

  .rule-set-value {
    margin: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .placeholder {
    color: #c9d2db;
  }

  .bk-select-clear {
    position: absolute;
    top: 8px;
    right: 6px;
    z-index: 100;
    display: none;
    font-size: 14px;
    color: #c4c6cc;
    text-align: center;

    &:hover {
      color: #979ba5;
    }
  }

  &:hover {
    .bk-select-clear {
      display: inline-block;
    }
  }
}
</style>
