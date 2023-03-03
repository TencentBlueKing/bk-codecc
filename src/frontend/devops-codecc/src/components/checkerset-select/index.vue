<template>
  <div>
    <bk-form :label-width="130" ref="checkerset">
      <bk-form-item v-for="(value, key) in data" :key="key" :required="true" :label="key" :property="key">
        <div class="rule-set-input" @click="openSelect(key)">
          <p class="rule-set-value" :title="getValueShow(renderList[key])">{{ getValueShow(renderList[key]) }}</p>
          <span class="placeholder" v-if="!selectSets[key] || !selectSets[key].length">{{$t('请选择')}}</span>
          <span class="bk-select-clear bk-icon icon-close-circle-shape" @click.stop="handleClear(selectSets[key], key)"></span>
        </div>
      </bk-form-item>
    </bk-form>
    <rule-set-dialog
      :visiable.sync="dialogVisiable"
      :cur-lang="dialogKey"
      :selected-list="renderList[dialogKey]"
      :default-lang="[dialogKey]"
      :handle-select="handleSelect"
    ></rule-set-dialog>
  </div>
</template>

<script>
  import RuleSetDialog from './RuleSetDialog'
  export default {
    name: 'checkerset-select',
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
        dialogVisiable: false,
        renderList: {},
        dialogKey: '',
      }
    },
    watch: {
      data(data) {
        const { selectSets } = this
        Object.keys(data).forEach((key) => {
          if (!selectSets[key] || !selectSets[key].length) {
            const list = []
            this.renderList[key] = []
            Object.values(data[key]).forEach((value) => {
              value.forEach((item) => {
                if (item.defaultCheckerSet) {
                  list.push(item.checkerSetId)
                  this.renderList[key].push(item)
                }
              })
            })
            selectSets[key] = list
          }
        })
        this.handleChange()
        this.selectSets = { ...selectSets }
      },
    },
    methods: {
      handleChange(value, options) {
        const checkerset = this.getCheckerset()
        const toolStr = checkerset.map(item => item.toolList).join()
        const toolList = Array.from(new Set(toolStr.split(','))).filter(item => item)
        this.$emit('handleToolChange', toolList)
      },
      getCheckerset() {
        let checkerset = []
        Object.keys(this.renderList).forEach((key) => {
          if (this.data[key]) {
            checkerset = checkerset.concat(this.renderList[key])
          }
        })
        return checkerset
      },
      handleValidate() {
        let hasError = false
        const { formItems } = this.$refs.checkerset
        const { selectSets } = this
        Object.keys(this.data).forEach((key) => {
          const formItem = formItems.find(item => item.label === key)
          if (!selectSets[key] || !selectSets[key].length) {
            formItem.validator.state = 'error'
            formItem.validator.content = '必填项'
            hasError = true
          }
        })
        // for (const key in this.data) {
        //   const formItem = formItems.find(item => item.label === key)
        //   if (!selectSets[key] || !selectSets[key].length) {
        //     formItem.validator.state = 'error'
        //     formItem.validator.content = '必填项'
        //     hasError = true
        //   }
        // }
        return !hasError
      },
      openSelect(key) {
        this.$refs.checkerset.formItems.find(item => item.label === key).clearError()
        this.dialogKey = key
        this.dialogVisiable = true
      },
      handleClear(val, key) {
        this.$refs.checkerset.formItems.find(item => item.label === key).clearError()
        this.selectSets[key] = []
        this.renderList[key] = []
        // this.$emit('input', [])
        this.handleChange()
      },
      getValueShow(list = []) {
        const nameList = list.map(val => val.checkerSetName)
        return nameList.join(',')
      },
      handleSelect(checkerSet, isCancel, key) {
        if (isCancel) {
          this.renderList[key] = this.renderList[key].filter(item => item.checkerSetId !== checkerSet.checkerSetId)
        } else {
          this.renderList[key].push(checkerSet)
          this.renderList[key] = this.renderList[key].filter(item => item)
        }

        const newVal = this.renderList[key].map(item => item.checkerSetId)
        // this.$emit('input', newVal)
        this.selectSets[key] = newVal
        this.handleChange()
      },
    },
  }
</script>

<style lang="postcss" scoped>
    .checker-label {
      font-size: 12px;
      width: 116px;
      text-align: right;
      padding-right: 16px;
      display: inline-block;
      line-height: 32px;
      position: relative;
      top: -10px;
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
      font-weight: 400 !important;
      font-size: 12px;
    }
    .tag {
      background: #c9dffa;
      border-radius: 2px;
      padding: 2px 8px;
      display: inline-block;
      line-height: 18px;
    }
    .bk-selector-create-item {
      cursor: pointer;
    }
    .rule-set-input {
      position: relative;
      padding: 0 20px 0 10px;
      border: 1px solid #c4c6cc;
      border-radius: 2px;
      height: 32px;
      line-height: 30px;
      color: #63656e;
      cursor: pointer;
      font-size: 12px;
      overflow: hidden;
      .rule-set-value {
        margin: 0;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
      .placeholder {
        color: #c9d2db;
      }
      .bk-select-clear {
        display: none;
        position: absolute;
        right: 6px;
        top: 8px;
        text-align: center;
        font-size: 14px;
        z-index: 100;
        color: #c4c6cc;
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
