<template>
  <div>
    <div
      v-if="!isEditable || projectId === 'CUSTOMPROJ_PCG_RD'"
      class="from-pipeline"
    >
      <div class="to-pipeline">
        <span v-if="projectId === 'CUSTOMPROJ_PCG_RD'">{{
          $t('扫描触发配置由PCG EP度量平台自动生成')
        }}</span>
        <span v-else
        >{{ $t('修改扫描触发配置，请前往流水线') }}
          <a @click="handleToPipeline" href="javascript:;">{{
            $t('立即前往>>')
          }}</a></span
        >
      </div>
      <div>
        <span class="pipeline-label"
        >{{ $t('扫描方式') }}
          <bk-popover>
            <i class="codecc-icon icon-tips"></i>
            <div slot="content">
              <div>
                {{
                  $t(
                    '快速全量扫描：首次会扫描全部文件。后续扫描未变更文件会复用之前的扫描结果，提升扫描速度。'
                  )
                }}
              </div>
              <div>
                {{
                  $t(
                    '全量扫描：每次都扫描全部文件。Klocwork、Pinpoint、Gometalinter、重复率仅支持该扫描方式。'
                  )
                }}
              </div>
              <div>
                {{ $t('差异扫描：扫描当前工作空间分支与对比分支的差异文件。') }}
              </div>
              <div>
                {{
                  $t(
                    'MR/PR扫描：扫描MR/PR的源分支与目标分支的差异，默认为差异行，可选择差异文件。源分支代码需拉取到工作空间。MR/PR扫描不支持手动执行或流水线重试。'
                  )
                }}
                <a v-if="isInnerSite" target="_blank" :href="handleMrpr">{{ $t('了解更多') }}</a>
              </div>
            </div>
          </bk-popover>
        </span>
        <span>
          <span class="fs14" v-if="taskDetail.scanType === 1">{{
            $t('快速全量扫描')
          }}</span>
          <span class="fs14" v-else-if="taskDetail.scanType === 0">{{
            $t('全量扫描')
          }}</span>
          <span class="fs14" v-else-if="taskDetail.scanType === 6">{{
            $t('差异扫描')
          }}</span>
          <span
            class="fs14"
            v-else-if="taskDetail.scanType === 2 || taskDetail.scanType === 5"
          >{{ $t('MR/PR扫描') }}</span
          >
        </span>
      </div>
      <div>
        <span class="pipeline-label">{{ $t('触发方式') }}</span>
        <span class="fs14">{{ formatExecuteDate || '--' }}</span>
        <span class="fs14">{{ taskDetail.executeTime }}</span>
      </div>
      <div>
        <span class="pipeline-label">{{ $t('处理人转换') }}</span>
        <div v-if="authorList" class="handler-replace">
          <div v-for="(item, index) in authorList" :key="index">
            <span class="fs14">{{ item.targetAuthor.join() || '--' }}</span>
            <span class="fs14 ml10" v-if="item.targetAuthor.join()">{{
              $t('(原处理人 ') + item.sourceAuthor + ')'
            }}</span>
          </div>
        </div>
      </div>
      <div>
        <span class="pipeline-label">{{ $t('禁止页面忽略问题') }}</span>
        <span>
          <span class="fs14" v-if="taskDetail.prohibitIgnore">{{
            $t('禁止页面忽略')
          }}</span>
          <span class="fs14" v-else>{{ $t('允许页面忽略') }}</span>
        </span>
      </div>
    </div>
    <div v-else>
      <bk-form
        :label-width="190"
        :model="taskDetail"
        onkeydown="if(event.keyCode==13){return false;}"
      >
        <template v-if="taskDetail.createFrom !== 'gongfeng_scan'">
          <div class="settings-header">
            <b class="settings-header-title">{{ $t('扫描方式') }}</b>
            <span class="fs12 ml5">{{
              $t('支持除Gometalinter、重复率之外所有工具')
            }}</span>
          </div>
          <div class="settings-body">
            <bk-form-item :label-width="110">
              <bk-radio-group v-model="taskDetail.scanType">
                <bk-radio :value="1" class="pr30">{{
                  $t('快速全量扫描')
                }}</bk-radio>
                <bk-radio :value="0" class="pr30">{{
                  $t('全量扫描')
                }}</bk-radio>
              </bk-radio-group>
            </bk-form-item>
          </div>
          <div class="settings-header">
            <b
              v-if="taskDetail.createFrom === 'bs_pipeline'"
              class="settings-header-title"
            >{{ $t('触发方式') }}</b
            >
            <b v-else class="settings-header-title">{{ $t('定时触发') }}</b>
          </div>

          <div
            class="settings-body"
            v-if="taskDetail.createFrom !== 'bs_pipeline'"
          >
            <bk-form-item :label-width="110">
              <!-- 周选择器 -->
              <div>
                <ul>
                  <li
                    :class="
                      taskDetail.executeDate &&
                        taskDetail.executeDate.includes(week.id)
                        ? 'active'
                        : ''
                    "
                    @click="selectedWeek(week.id)"
                    class="settings-trigger-week"
                    v-for="week in weekList"
                    :key="week.label"
                  >
                    {{ week.name }}
                  </li>
                </ul>
              </div>
              <!-- /周选择器 -->
            </bk-form-item>
            <bk-form-item :label-width="110" property="time">
              <bk-time-picker
                style="width: 293px"
                v-model="taskDetail.executeTime"
                :placeholder="$t('选择时间')"
                :format="'HH:mm'"
              >
              </bk-time-picker>
            </bk-form-item>
          </div>
          <div class="settings-body" v-else>
            <bk-form-item :label-width="110">
              <span class="fs14"
              >{{ $t('请前往流水线修改触发方式。') }}
                <a @click="handleToPipeline" href="javascript:;">{{
                  $t('立即前往>>')
                }}</a></span
              >
            </bk-form-item>
          </div>
        </template>

        <div class="settings-header">
          <b class="settings-header-title">{{ $t('处理人转换') }}</b>
          <span class="fs12 pl10">{{
            $t('各工具原处理人的问题都将自动转给新处理人')
          }}</span>
        </div>
        <div class="settings-body">
          <bk-form-item
            :label-width="110"
            class="input"
            v-for="(item, index) in authorList"
            :key="index"
          >
            <bk-input v-model="item.sourceAuthor"></bk-input>
            <!-- <bk-input class="compile-version" v-model="item.targetAuthor" :placeholder="'新处理人'"></bk-input> -->
            <bk-tag-input
              allow-create
              v-if="IS_ENV_TAI || !isInnerSite"
              class="compile-version"
              v-model="item.targetAuthor"
              :placeholder="$t('新处理人')"
            ></bk-tag-input>
            <bk-user-selector
              v-else
              class="compile-version"
              :api="userApiUrl"
              name="targetAuthor"
              :placeholder="$t('新处理人')"
              v-model="item.targetAuthor"
            ></bk-user-selector>
            <div class="tool-icon">
              <i
                class="bk-icon icon-plus"
                @click="addTool(index)"
                v-if="index === authorList.length - 1"
              ></i>
              <i
                class="bk-icon icon-close"
                @click="deleteTool(index)"
                v-if="authorList.length > 1"
              ></i>
            </div>
          </bk-form-item>
        </div>
        <div class="settings-header">
          <b class="settings-header-title">{{ $t('禁止页面忽略问题') }}</b>
          <span class="fs12 pl10">
            {{
              $t(
                '禁止直接从页面忽略问题，可在代码行末或上一行使用注释忽略，例如// NOCC:rule1(ignore reason)。'
              )
            }}
            <a v-if="isInnerSite" @click="handleToProhibit" href="javascript:;">{{
              $t('了解详情>>')
            }}</a>
          </span>
        </div>
        <div class="settings-body">
          <bk-form-item :label-width="110">
            <bk-radio-group v-model="prohibitIgnore">
              <bk-radio :value="false" class="pr30">{{
                $t('允许页面忽略')
              }}</bk-radio>
              <bk-radio :value="true">{{ $t('禁止页面忽略') }}</bk-radio>
            </bk-radio-group>
          </bk-form-item>
          <bk-form-item :label-width="110">
            <span v-if="!isEditable" class="fs14"
            >{{ $t('请前往流水线修改。') }}
              <a @click="handleToPipeline" href="javascript:;">{{
                $t('立即前往>>')
              }}</a></span
            >
            <bk-button
              v-else-if="isRbac === true"
              key="save"
              v-perm="{
                hasPermission: true,
                disablePermissionApi: false,
                permissionData: {
                  projectId: projectId,
                  resourceType: 'codecc_task',
                  resourceCode: taskId,
                  action: 'codecc_task_setting',
                },
              }"
              theme="primary"
              @click="saveHandler"
            >
              {{ $t('保存') }}
            </bk-button>
            <bk-button
              v-else
              :disabled="ableHandlerConversion()"
              theme="primary"
              @click="saveHandler"
            >{{ $t('保存') }}</bk-button
            >
          </bk-form-item>
        </div>
      </bk-form>
    </div>
  </div>
</template>

<script>
import { mapState } from 'vuex';
import DEPLOY_ENV from '@/constants/env';
import BkUserSelector from '@blueking/user-selector';

export default {
  components: {
    BkUserSelector,
  },
  data() {
    return {
      weekList: [
        {
          id: '1',
          name: this.$t('一'),
          label: 'Mon',
        },
        {
          id: '2',
          name: this.$t('二'),
          label: 'Tues',
        },
        {
          id: '3',
          name: this.$t('三'),
          label: 'Wed',
        },
        {
          id: '4',
          name: this.$t('四'),
          label: 'Thur',
        },
        {
          id: '5',
          name: this.$t('五'),
          label: 'Fri',
        },
        {
          id: '6',
          name: this.$t('六'),
          label: 'Sat',
        },
        {
          id: '7',
          name: this.$t('日'),
          label: 'Sun',
        },
      ],
      authorList: [{ sourceAuthor: '', targetAuthor: [] }],
      date: '',
      prohibitIgnore: false,
      weekForm: [
        this.$t('周一'),
        this.$t('周二'),
        this.$t('周三'),
        this.$t('周四'),
        this.$t('周五'),
        this.$t('周六'),
        this.$t('周日'),
      ],
      isInnerSite: DEPLOY_ENV === 'tencent',
      IS_ENV_TAI: window.IS_ENV_TAI,
      userApiUrl: window.USER_API_URL,
    };
  },
  computed: {
    ...mapState('task', {
      taskDetail: 'detail',
    }),
    ...mapState(['isRbac']),
    isEditable() {
      // 最老的v1插件atomCode为空，但createFrom === 'bs_pipeline', 也可编辑
      return (
        !this.taskDetail.atomCode
        || this.taskDetail.createFrom !== 'bs_pipeline'
        || this.taskDetail.createFrom === 'gongfeng_scan'
        || this.taskDetail.createFrom === 'api_trigger'
      );
    },
    fromDate() {
      return this.taskDetail.newDefectJudge
        ? this.taskDetail.newDefectJudge.fromDate
        : '';
    },
    formatExecuteDate() {
      const execute = this.taskDetail.executeDate
        ? this.taskDetail.executeDate.sort()
        : [];
      if (execute.length) {
        let str = '每';
        Object.keys(execute).forEach((key) => {
          str += this.weekForm[execute[key] - 1];
          if (key < execute.length - 1) {
            str += '、';
          }
        });
        return str;
      }
      return '';
    },
    handleMrpr() {
      return window.IWIKI_MRPR_SCAN;
    },
    projectId() {
      return this.$route.params.projectId;
    },
    taskId() {
      return this.$route.params.taskId;
    },
  },
  watch: {
    taskDetail(val, oldVal) {
      this.prohibitIgnore = !!val.prohibitIgnore;
    },
  },
  created() {
    this.init();
  },
  methods: {
    init() {
      this.$store.dispatch('defect/getTransferAuthorList').then((res) => {
        this.authorList = res.transferAuthorList
          ? this.formatterAuthor(res.transferAuthorList)
          : [{ sourceAuthor: '', targetAuthor: [] }];
        this.date = this.fromDate;
      });
    },
    saveTime() {
      const { taskId } = this.$route.params;
      const { scanType, executeDate = [], executeTime = '' } = this.taskDetail;
      const data = {
        taskId,
        scanType,
        prohibitIgnore: this.prohibitIgnore,
        timeAnalysisConfig: { executeDate, executeTime },
        newDefectJudge: { judgeBy: 1, fromDate: this.date },
      };
      this.$store.dispatch('task/trigger', data)
        .then((res) => {
          if (res === true) {
            this.$bkMessage({ theme: 'success', message: this.$t('保存成功') });
          }
          this.$store.dispatch('task/detail', { showLoading: true });
        })
        .catch(() => {
          this.$bkMessage({ theme: 'error', message: this.$t('保存失败') });
          this.$store.dispatch('task/detail', { showLoading: true });
        });
    },
    // 保存选择的周
    selectedWeek(id) {
      if (!this.taskDetail.executeDate) this.$set(this.taskDetail, 'executeDate', []);
      if (!this.taskDetail.executeDate.includes(id)) {
        this.taskDetail.executeDate.push(id);
      } else if (this.taskDetail.executeDate.includes(id)) {
        const i = this.taskDetail.executeDate.indexOf(id);
        this.taskDetail.executeDate.splice(i, 1);
      }
    },
    handleTimeChange(date) {
      this.date = date;
    },
    addTool() {
      this.authorList.push({
        sourceAuthor: '',
        targetAuthor: [],
      });
    },
    deleteTool(index) {
      if (this.authorList.length > 1) {
        this.authorList.splice(index, 1);
      }
    },
    formatterAuthor(authorObj, type) {
      return authorObj.map(({ sourceAuthor, targetAuthor }) => ({
        sourceAuthor: sourceAuthor || '',
        targetAuthor: Array.isArray(targetAuthor)
          ? targetAuthor.join(',')
          : targetAuthor.split(','),
      }));
    },
    saveHandlerConversion() {
      const transferAuthorList = this.formatterAuthor(this.authorList);
      const { taskId } = this.$route.params;
      const { scanType } = this.taskDetail;
      let { executeDate, executeTime } = this.taskDetail;
      executeDate = executeDate === undefined ? [] : executeDate;
      executeTime = executeTime === undefined ? '' : executeTime;
      const newDefectJudge = { judgeBy: 1, fromDate: this.date };
      const data = {
        taskId,
        scanType,
        timeAnalysisConfig: { executeDate, executeTime },
        transferAuthorList,
        newDefectJudge,
      };
      this.$store
        .dispatch('task/trigger', data)
        .then((res) => {
          if (res === true) {
            this.$bkMessage({ theme: 'success', message: this.$t('保存成功') });
            this.$store.dispatch('task/detail', { showLoading: true });
          }
        })
        .catch((e) => {
          this.$bkMessage({ theme: 'error', message: this.$t('保存失败') });
          this.$store.dispatch('task/detail', { showLoading: true });
        });
    },
    ableHandlerConversion() {
      if (
        this.authorList.length === 1
        && this.authorList[0].sourceAuthor === ''
        && !this.authorList[0].targetAuthor.length
      ) {
        return false;
      }
      for (const element of this.authorList) {
        if (element.sourceAuthor === '' || !element.targetAuthor.length) {
          return true;
        }
      }
      return false;
    },
    saveHandler() {
      if (
        this.authorList.length === 1
        && this.authorList[0].sourceAuthor === ''
        && !this.authorList[0].targetAuthor.length
      ) {
        return this.saveTime();
      }
      return this.saveHandlerConversion();
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
    handleToProhibit() {
      window.open(`${window.IWIKI_PROHIBIT}`, '_blank');
    },
  },
};
</script>

<style lang="postcss" scoped>
@import url('../../css/variable.css');

/* 标题与分隔线 start */
.settings-header {
  margin: 19px 0 19px 45px;

  .settings-header-title {
    display: inline-block;
    font-size: 14px;
    color: #63656e;
    text-align: left;
  }
}

/* 标题与分隔线 end */
.settings-body {
  padding-bottom: 20px;
  font-size: 14px;
  border-bottom: 1px solid $bgHoverColor;

  &:last-of-type {
    border-bottom: 0;
  }

  /* 星期列表 start */
  .settings-trigger-week {
    display: inline-block;
    width: 40px;
    height: 32px;
    margin-right: 8px;
    line-height: 32px;
    text-align: center;
    cursor: pointer;
    border: 1px solid $itemBorderColor;
    border-radius: 2px;
  }

  /* 星期列表 end */
  .active {
    color: $goingColor;
    border: 1px solid $goingColor;
  }

  .save-button {
    width: 86px;
  }

  .date-picker {
    margin-right: 15px;
  }

  .input {
    width: 300px;
    height: 32px;

    .compile-version {
      position: relative;
      top: -32px;
      left: 115%;
      width: 190px;
    }

    .compile-version::before {
      position: absolute;
      top: 0;
      left: -24px;
      font-size: 14px;
      line-height: 32px;
      color: #c4c6cc;
      content: '>>';
    }

    .tool-icon {
      position: relative;
      top: -72px;
      left: 220%;

      .bk-icon {
        font-size: 20px;
        cursor: pointer;
      }
    }
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
    width: 130px;
    height: 46px;
    font-size: 14px;
    font-weight: 600;
    line-height: 14px;
    text-align: left;
  }

  .handler-replace {
    display: inline-block;
    vertical-align: top;
  }
}
</style>
