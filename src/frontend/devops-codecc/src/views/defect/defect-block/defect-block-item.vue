<template>
  <div class="item" :class="{ 'active': active }" @click="handleFileListRowClick(defect)">
    <div class="item-ellipsis item-message" :title="defect.message || defect.displayCategory">
      <i class="bk-icon icon-exclamation-circle-shape" :class="[`type-${defect.severity}`]"></i>
      {{defect.message || defect.displayCategory}}
    </div>
    <div class="item-ellipsis"
         :title="`${defect.checker || defect.checkerName}`">
      <span>{{defect.checker || defect.checkerName}}</span>
    </div>
    <div class="item-file"
         :title="`${defect.fileName}:${defect.lineNum || defect.lineNumber}`">
      <span class="item-ellipsis item-file-name">{{defect.fileName}}</span>
      <span>:{{defect.lineNum || defect.lineNumber}}</span>
      <span :title="$t('复制问题所在文件位置')" class="copy-icon" @click.stop="copy(`${defect.fileName}:${defect.lineNum || defect.lineNumber}`)"><i class="codecc-icon icon-copy-line"></i></span>
    </div>
    <div class="item-light" v-for="(instances, i) in defectInstances" :key="instances" @click.stop>
      <div class="item-times" v-if="defectInstances.length > 1">{{$t('第x次，共出现y次', { num: i + 1,count: defectInstances.length })}}</div>
      <div class="item-trace" v-for="trace in instances.traces"
           :key="trace">
        <div class="item-ellipsis item-trace"
             :class="{ active: traceActiveId === trace.id || (traceActiveId === '' && trace.main) }"
             @click.stop="clickTrace(trace)">
          <span class="item-index" :class="{ main: trace.main }">{{trace.index && trace.kind !== 'MULTI' ? trace.index : ''}}</span>
          <i class="bk-icon"
             v-if="trace.linkTrace && trace.linkTrace.length"
             :class="[trace.expanded ? 'icon-down-shape' : 'icon-right-shape']"
             @click.stop="updateExanded(trace, !trace.expanded)"></i>
          <span v-if="trace.kind === 'REMEDIATION'" :title="trace.message"><bk-icon type="exclamation-circle" /> {{$t('修复建议')}}</span>
          <span v-else :title="trace.message">{{trace.tag || trace.message}}</span>
        </div>
        <link-trace v-show="trace.expanded" :list="trace.linkTrace || []" :active-id="traceActiveId" @clickTrace="clickTrace"></link-trace>
      </div>
      <divider class="item-divider" v-if="defectInstances.length > 1 && i < defectInstances.length - 1"></divider>
    </div>
  </div>
</template>

<script>
  import LinkTrace from './link-trace'
  export default {
    name: 'defect-block-item',
    components: {
      LinkTrace,
    },
    props: {
      active: Boolean,
      defect: Object,
      defectInstances: Array,
      handleFileListRowClick: Function,
      traceActiveId: String,
    },
    data() {
      return {
        traceTimeIndex: 0,
      }
    },
    methods: {
      clickTrace(trace) {
        this.$emit('clickTrace', trace)
      },
      updateExanded(trace, expanded) {
        this.defectInstances.forEach((instance) => {
          instance.traces.forEach((item) => {
            if (item.id === trace.id) {
              item.expanded = expanded
            }
          })
        })
        this.$forceUpdate()
      },
      copy(file) {
        const input = document.createElement('input')
        document.body.appendChild(input)
        input.setAttribute('value', file)
        input.select()
        document.execCommand('copy')
        document.body.removeChild(input)
        this.$bkMessage({ theme: 'success', message: '复制成功' })
      },
    },
  }
</script>

<style lang="postcss" scoped>
  .item {
    font-size: 14px;
    /* font-weight: bold; */
    color: #313238;
    line-height: 24px;
    width: 312px;
    border: 1px solid #dcdee5;
    border-radius: 2px;
    padding: 8px;
    margin-left: 8px;
    margin-bottom: 8px;
    cursor: pointer;
    background: #fff;
    &.active {
      background: #ffeded;
      border: 1px solid #ffd2de;
      box-shadow: 0 2px 4px 0 #f6dada;
    }
  }
  .item-light {
    font-weight: normal;
    color: #63656e;
  }
  .item-times {
    color: #979ba5;
  }
  .type-1 {
    color: #ff5656;
  }
  .type-2 {
    color: #ff9c01;
  }
  .type-3,.type-4 {
    color: #54cad1;
  }
  .item-file {
    display: -webkit-box;
    &:hover {
      .copy-icon {
        display: -webkit-box;
      }
    }
    .copy-icon {
      display: none;
      color: #979ba5;
      margin-top: -3px;
      padding-left: 5px;
      cursor: pointer;
      .icon-copy-line {
        position: relative;
        top: 1px;
      }
    }
  }
  .item-file-name {
    max-width: calc(100% - 50px);
  }
  .item-ellipsis {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: normal;
    word-break: break-all;
    display: -webkit-box;
    -webkit-line-clamp: 1;
    -webkit-box-orient: vertical;
    &.item-message {
      -webkit-line-clamp: 2;
    }
  }
  .item-trace {
    line-height: 30px;
    &.active {
      color: #3a84ff;
      background: #e1ecff;
    }
  }
  .item-index {
    display: inline-block;
    width: 22px;
    line-height: 14px;
    text-align: center;
    color: #3a84ff;
    background: #edf4ff;
    border-radius: 2px;
    &.main {
      background: #609cfe;
      color: #fff;
    }
  }
  .item-divider {
    position: relative;
    display: block;
    width: 100%;
    height: 0px;
    margin: 8px 0px;
    vertical-align: middle;
    border-bottom: 1px solid #dcdee5;
  }
</style>
