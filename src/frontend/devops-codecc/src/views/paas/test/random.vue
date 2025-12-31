<template>
  <div class="paas-test bg-white p-[24px]">
    <div v-if="!isCompiled" class="flex flex-row space-x-4">
      <section class="basis-1/2 px-[16px] py-[12px]">
        <p class="pb-[24px]">
          <span class="text-[#313238] text-[14px] font-bold">{{ $t('测试规模') }}</span>
        </p>
        <bk-form label-width="95">
          <bk-form-item required :label="$t('代码库大小')">
            <bk-select v-model="repoScaleId" :disabled="!!process" :placeholder="$t('请选择')">
              <bk-option
                v-for="option in repoScaleList"
                :key="option.entityId"
                :id="option.entityId"
                :name="option.name">
              </bk-option>
            </bk-select>
          </bk-form-item>
          <bk-form-item required :label="$t('代码库数量')">
            <bk-input type="number" :disabled="!!process" :min="0" :max="50" v-model="need" :placeholder="$t('请输入')">
            </bk-input>
          </bk-form-item>
          <bk-form-item required :label="$t('测试任务')">
            <section class="max-h-[160px] overflow-auto">
              <div
                v-for="item in checkerSetList"
                :key="item.checkerSetId"
                class="flex flex-row space-x-4 mb-[8px]">
                <bk-input class="basis-1/3" v-model="item.checkerSetLang" disabled>
                  <template slot="prepend">
                    <div class="group-text">{{ $t('语言') }}</div>
                  </template>
                </bk-input>
                <bk-input class="basis-2/3" v-model="item.checkerSetName" disabled>
                  <template slot="prepend">
                    <div class="group-text">{{ $t('规则集') }}</div>
                  </template>
                </bk-input>
              </div>
            </section>

          </bk-form-item>
          <bk-form-item>
            <bk-button
              v-if="process !== 'success' && process !== 'fail'"
              theme="primary"
              :disabled="!repoScaleId || !need || !!process"
              @click="startTest">
              {{ process === 'testing' ? $t('测试中...') : $t('开始测试') }}
            </bk-button>
            <span v-if="process === 'success'">
              <bk-icon class="text-[14px] text-[#2DCB56] pl-[15px]" type="check-circle-shape" />
              <span class="text-[12px]">{{ $t('测试已通过') }}</span>
            </span>
            <span v-if="process === 'fail'">
              <bk-popconfirm
                :content="$t('结束后，整个流程会置为失败。')"
                width="288"
                @confirm="skipTest('finish')">
                <bk-button class="ml-[15px]">{{ $t('结束') }}</bk-button>
              </bk-popconfirm>
              <bk-popconfirm
                :content="$t('随机测试的指标未达到建议值，是否确认跳过？跳过后，整个流程会置为成功。')"
                width="288"
                @confirm="skipTest('success')">
                <bk-button class="ml-[15px]">{{ $t('跳过') }}</bk-button>
              </bk-popconfirm>
              <bk-icon class="text-[14px] text-[#FF5A5A] pl-[15px]" type="close-circle-shape" />
              <span class="text-[12px]">{{ $t('测试未通过') }}</span>
            </span>
          </bk-form-item>
        </bk-form>
      </section>
      <section class="basis-1/2 px-[16px] py-[12px] bg-[#F5F7FA] text-[12px]">
        <result ref="resultRef" :stage="2" @updateResult="updateResult"></result>
      </section>
    </div>
    <div v-else class="min-h-[430px]">
      <bk-exception type="empty">{{ $t('编译型工具暂不支持随机测试') }}</bk-exception>
    </div>
  </div>
</template>

<script>
import Result from './result.vue';

export default {
  components: {
    bkSelect,
    bkOption,
    Result,
  },
  data() {
    return {
      isCompiled: false,
      repoScaleId: '',
      need: 10,
      repoScaleList: [],
      checkerSetList: [],
      result: {},
      process: '', // 测试进度，testing, success, fail
    };
  },
  watch: {
    process(val) {
      console.log('🚀 ~ process ~ val:', val);
      // 如果测试结束，则发送消息给父级, 只有成功才自动发送，失败要手动发送
      if (val === 'success') {
        window.parent.postMessage({
          type: 'design-test',
          data: val,
        }, '*');
      }
    },
  },
  beforeCreate() {
    this.$store.dispatch('test/getRepoScaleList').then((res) => {
      this.repoScaleList = res;
    });
    this.$store.dispatch('test/getToolList', this.$route.params.toolName).then((res) => {
      this.checkerSetList = res?.checkerSetList || [];
    });
  },
  methods: {
    startTest() {
      this.$store.dispatch('test/startRandomTest', {
        version: this.$route.query.version,
        toolName: this.$route.params.toolName,
        need: this.need,
        repoScaleId: this.repoScaleId,
      }).then((res) => {
        if (res.data) {
          this.$refs.resultRef.init();
        } else {
          this.$bkMessage({
            theme: 'error',
            message: res.message || this.$t('开始测试失败'),
          });
        }
      });
    },
    updateResult(result) {
      this.result = result;
      if (result.repoCount) {
        const { repoCount, repoScaleId, taskCount, failCount, successCount, isPass } = result;
        this.need = repoCount;
        this.repoScaleId = repoScaleId;
        if (taskCount === failCount + successCount) {
          if (failCount || !isPass) {
            this.process = 'fail';
          } else {
            this.process = 'success';
          }
        } else {
          this.process = 'testing';
        }
      }
    },
    skipTest(process) {
      this.process = process;
      // 如果失败，且点击“结束”，则发送消息给父级，置为失败
      if (process === 'finish') {
        window.parent.postMessage({
          type: 'design-test',
          data: 'fail',
        }, '*');
      }
    },
  },
};
</script>

<style>
#app {
  min-width: 960px;
}
</style>
<style lang="postcss" scoped>
.paas-test {
  >>> .bk-link .bk-link-text {
    font-size: 12px;
  }
}
</style>
