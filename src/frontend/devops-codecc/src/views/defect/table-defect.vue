<template>
  <bk-table
    class="file-list-table"
    ref="fileListTable"
    v-bkloading="{ isLoading: fileLoading, opacity: 0.6 }"
    :height="screenHeight"
    :data="list"
    :row-class-name="handleRowClassName"
    @row-click="handleFileListRowClick"
    @sort-change="handleSortChange"
    @selection-change="handleSelectionChange"
    @select-all="toSelectAll"
  >
    <bk-table-column
      :selectable="handleSelectable"
      type="selection"
      width="60"
      align="center"
    >
    </bk-table-column>
    <!-- <bk-table-column width="15" class-name="mark-row">
      <template slot-scope="props">
        <span v-if="props.row.status === 1 && props.row.mark === 1" v-bk-tooltips="$t('已标记处理')" class="codecc-icon icon-mark"></span>
        <span v-if="props.row.status === 1 && props.row.mark === 2" v-bk-tooltips="$t('标记处理后重新扫描仍为问题')" class="codecc-icon icon-mark re-mark"></span>
      </template>
    </bk-table-column> -->
    <!-- <bk-table-column :label="$t('ID')" prop="defectId" sortable="custom"></bk-table-column> -->
    <!-- <bk-table-column :label="$t('位置')" prop="fileName" sortable="custom">
      <template slot-scope="props">
        <span v-bk-tooltips="{ content: props.row.filePath + ':' + props.row.lineNum, delay: 600 }">{{props.row.fileName}}:{{props.row.lineNum}}</span>
      </template>
    </bk-table-column> -->
    <bk-table-column :label="$t('文件名')" prop="fileName" sortable="custom">
      <template slot-scope="props">
        <span v-bk-tooltips="{ content: props.row.filePath, delay: 600 }">{{
          props.row.fileName
        }}</span>
      </template>
    </bk-table-column>
    <bk-table-column
      :label="$t('行号')"
      prop="lineNum"
      width="70"
    ></bk-table-column>
    <bk-table-column
      show-overflow-tooltip
      :label="$t('规则')"
      prop="checker"
    ></bk-table-column>
    <bk-table-column
      show-overflow-tooltip
      :label="$t('问题描述')"
      prop="message"
      min-width="120"
    ></bk-table-column>
    <!-- <bk-table-column :label="$t('类型子类')" prop="displayType"></bk-table-column> -->
    <bk-table-column
      show-overflow-tooltip
      :label="$t('处理人')"
      prop="author"
      min-width="70"
    >
      <template slot-scope="props">
        <div
          v-if="props.row.status & 1 || props.row.status & 4"
          @mouseenter="handleAuthorIndex(props.$index)"
          @mouseleave="handleAuthorIndex(-1)"
          @click.stop="handleAuthor(1, props.row.entityId, props.row.author)"
        >
          <span>{{ array2Str(props.row.author) }}</span>
          <span
            v-if="hoverAuthorIndex === props.$index"
            class="bk-icon icon-edit2 fs18"
          ></span>
        </div>
        <div v-else>
          <span>
            {{ array2Str(props.row.author) }}
          </span>
        </div>
      </template>
    </bk-table-column>
    <bk-table-column :label="$t('级别')" prop="severity" width="80">
      <template slot-scope="props">
        <span
          :class="`color-${
            { 1: 'major', 2: 'minor', 4: 'info' }[props.row.severity]
          }`"
        >{{ defectSeverityMap[props.row.severity] }}</span
        >
      </template>
    </bk-table-column>
    <bk-table-column
      prop="lineUpdateTime"
      width="110"
      :sortable="isProjectDefect ? false : 'custom'"
      :label="$t('代码提交')"
    >
      <template slot-scope="props">
        <span>{{ props.row.lineUpdateTime | formatDate('date') }}</span>
      </template>
    </bk-table-column>
    <bk-table-column
      :sortable="isProjectDefect ? false : 'custom'"
      :label="$t('首次发现')"
      prop="createBuildNumber"
      width="100"
    >
      <template slot-scope="props">
        <span>{{
          props.row.createBuildNumber ? '#' + props.row.createBuildNumber : '--'
        }}</span>
      </template>
    </bk-table-column>
    <bk-table-column :label="$t('分支')" prop="branch" width="110" show-overflow-tooltip>
      <template slot-scope="props">
        <span>{{ props.row.branch ? props.row.branch : '--' }}</span>
      </template>
    </bk-table-column>
    <bk-table-column :label="$t('状态')" prop="status" width="110">
      <template slot-scope="props">
        <div>
          {{ handleStatus(props.row.status, props.row.ignoreReasonType) }}
        </div>
        <div>
          <span
            v-if="props.row.status === 1 && props.row.mark === 1"
            v-bk-tooltips="$t('已标记处理')"
            class="codecc-icon icon-mark"
          ></span>
          <span
            v-if="props.row.status === 1 && props.row.markButNoFixed"
            v-bk-tooltips="$t('标记处理后重新扫描仍为问题')"
            class="codecc-icon icon-mark re-mark"
          ></span>
          <bk-popover
            v-if="
              props.row.defectIssueInfoVO &&
                props.row.defectIssueInfoVO.submitStatus &&
                props.row.defectIssueInfoVO.submitStatus !== 4
            "
            :disabled="!props.row.defectIssueInfoVO.submitIssueUrl"
            placement="bottom">
            <span class="codecc-icon icon-yititapddan"></span>
            <bk-button
              v-if="props.row.defectIssueInfoVO.submitIssueUrl"
              slot="content"
              text theme="primary"
              class="text-[12px]"
              @click="goToTAPD(props.row.defectIssueInfoVO.submitIssueUrl)">
              {{ $t('查看TAPD单据') }}
            </bk-button>
          </bk-popover>
          <img
            v-if="props.row.hasCodeComment"
            :src="commented"
            v-bk-tooltips="$t('已评论')"
            class="img-icon" />
          <img
            v-if="
              props.row.ignoreApprovalStatus &&
                (props.row.ignoreApprovalStatus === 1 || props.row.ignoreApprovalStatus === 2)
            "
            :src="ignoreApprovalIng"
            v-bk-tooltips="$t('忽略审批中')"
            class="img-icon" />
          <img
            v-if="
              props.row.ignoreApprovalStatus &&
                props.row.ignoreApprovalStatus === 3
            "
            :src="ignoreApprovalPass"
            v-bk-tooltips="$t('忽略审批通过')"
            class="img-icon" />
          <img
            v-if="
              props.row.ignoreApprovalStatus &&
                props.row.ignoreApprovalStatus === 4
            "
            :src="ignoreApprovalRefuse"
            v-bk-tooltips="$t('忽略审批被拒')"
            class="img-icon" />
        </div>
      </template>
    </bk-table-column>
    <bk-table-column
      show-overflow-tooltip
      v-if="isProjectDefect"
      :label="$t('任务')"
      prop="task"
    >
      <template slot-scope="props">
        <span class="cc-link" @click.stop="goToTask(props.row.taskId)">{{
          props.row.taskNameCn || '--'
        }}</span>
      </template>
    </bk-table-column>
    <bk-table-column :label="$t('操作')" width="60">
      <template slot-scope="props">
        <!-- 已修复问题没有这些操作 -->
        <span
          v-if="!(props.row.status & 2)"
          class="cc-operate-more"
          @click.prevent.stop
        >
          <bk-popover z-index="99" theme="light" placement="bottom" trigger="click">
            <span class="bk-icon icon-more guide-icon" v-if="!(props.row.status & 8 || props.row.status & 16)"></span>
            <div slot="content" class="handle-menu-tips txal">
              <!-- 待修复问题的操作 -->
              <template v-if="props.row.status === 1">
                <p
                  v-if="props.row.mark"
                  class="entry-link"
                  @click.stop="handleMark(0, false, props.row.entityId)"
                >
                  {{ $t('取消标记') }}
                </p>
                <p
                  v-else
                  class="entry-link"
                  @click.stop="handleMark(1, false, props.row.entityId)"
                >
                  {{ $t('标记处理') }}
                </p>
              </template>
              <!-- 已忽略问题的操作 -->
              <p
                v-if="props.row.status & 4 && props.row.ignoreCommentDefect"
                class="disabled"
                :title="$t('注释忽略的问题不允许页面进行恢复操作')"
              >
                {{ $t('取消忽略') }}
              </p>
              <p
                v-else-if="props.row.status & 4"
                class="entry-link"
                @click.stop="
                  handleIgnore('RevertIgnore', false, props.row.entityId)
                "
              >
                {{ $t('取消忽略') }}
              </p>
              <p
                v-if="props.row.status & 4 && !props.row.ignoreCommentDefect"
                class="entry-link"
                @click.stop="handleRevertIgnoreAndMark(props.row.entityId)"
              >
                {{ $t('取消忽略并标记处理') }}
              </p>
              <p
                v-if="props.row.status & 4 && !props.row.ignoreCommentDefect && isInnerSite"
                class="entry-link"
                @click.stop="handleRevertIgnoreAndCommit(props.row.entityId)"
              >
                {{ $t('取消忽略并提单') }}
              </p>
              <p
                v-else-if="prohibitIgnore"
                class="entry-link disabled"
                :title="
                  $t(
                    '已设置禁止页面忽略，可在代码行末或上一行使用注释忽略，例如// NOCC:rule1(ignore reason)'
                  )
                "
              >
                {{ $t('忽略问题') }}
              </p>
              <bk-popover
                v-else
                z-index="99"
                ref="guidePopover2"
                placement="left"
                theme="dot-menu light"
                trigger="click"
              >
                <div>
                  <span class="guide-flag"></span>
                  <span
                    :class="[
                      'entry-link',
                      'ignore-item',
                      [1, 2].includes(props.row.ignoreApprovalStatus) ? 'disabled' : ''
                    ]"
                    @click.stop="handleIgnore(
                      'IgnoreApproval|IgnoreDefect',
                      false,
                      props.row.entityId,
                      null,
                      props.row.ignoreApprovalStatus
                    )"
                  >{{ $t('忽略问题') }}</span
                  >
                </div>
                <div class="guide-content" slot="content">
                  <div :style="lineHeight">
                    {{ $t('支持忽略无需处理或暂缓处理的问题。') }}
                    {{ $t('针对特定忽略类型可') }}
                    <span
                      theme="primary"
                      class="set-tips"
                      @click="handleTableSetReview"
                    >{{ $t('设置提醒') }}</span
                    >
                    {{ $t('，以便定期review和修复。') }}
                  </div>
                  <div class="guide-btn">
                    <span class="btn-item" @click="handleTableGuideNextStep">{{
                      $t('我知道了')
                    }}</span>
                  </div>
                </div>
              </bk-popover>
              <p
                v-if="props.row.status & 4 && !props.row.ignoreCommentDefect"
                class="entry-link"
                @click.stop="handleChangeIgnoreType(props.row, false)"
              >
                {{ $t('修改忽略类型') }}
              </p>
              <p
                v-if="
                  props.row.status === 1 &&
                    !(
                      props.row.defectIssueInfoVO &&
                      props.row.defectIssueInfoVO.submitStatus &&
                      props.row.defectIssueInfoVO.submitStatus !== 4
                    )
                    &&
                    isInnerSite
                "
                class="entry-link"
                @click.stop="handleCommit('commit', false, props.row.entityId)"
              >
                {{ $t('提单') }}
              </p>
            </div>
          </bk-popover>
        </span>
      </template>
    </bk-table-column>
    <div slot="append" v-show="isFileListLoadMore">
      <div class="table-append-loading">
        {{
          $t('正在加载第x-y个，请稍后···', {
            x: nextPageStartNum,
            y: nextPageEndNum,
          })
        }}
      </div>
    </div>
    <div slot="empty">
      <div class="codecc-table-empty-text">
        <img src="../../images/empty.png" class="empty-img" />
        <div>{{ $t('没有查询到数据') }}</div>
      </div>
    </div>
  </bk-table>
</template>

<script>
import defectTable from '@/mixins/defect-table';
import { array2Str } from '@/common/util';
import { language } from '../../i18n';
import DEPLOY_ENV from '@/constants/env';
import ignoreApprovalPass from '@/images/ignore-approval-pass.svg';
import ignoreApprovalRefuse from '@/images/ignore-approval-refuse.svg';
import ignoreApprovalIng from '@/images/ignore-approval-ing.svg';
import commented from '@/images/commented.svg';

export default {
  mixins: [defectTable],
  data() {
    return {
      ignoreApprovalPass,
      ignoreApprovalRefuse,
      ignoreApprovalIng,
      commented,
      array2Str,
      isInnerSite: DEPLOY_ENV === 'tencent',
    };
  },
  computed: {
    isEn() {
      return language === 'en-US';
    },
    lineHeight() {
      return this.isEn ? 'line-height: 18px' : 'line-height: 22px';
    },
  },
};
</script>

<style scoped lang="postcss">
@import url('../../css/variable.css');

.file-list-table {
  >>> .list-row {
    cursor: pointer;

    &.grey-row {
      color: #c3cdd7;

      .color-major,
      .color-minor,
      .color-info {
        color: #c3cdd7;
      }
    }

    .cell {
      -webkit-line-clamp: 2;
    }
  }

  >>> td{
    height: 60px;
  }
}

.cc-operate-more {
  >>> .icon-more {
    font-size: 20px;
  }
}

.handle-menu-tips {
  text-align: center;

  .entry-link {
    padding: 4px 0;
    font-size: 12px;
    color: $fontWeightColor;
    cursor: pointer;

    &.disabled {
      color: $borderColor;
      cursor: not-allowed;

      &:hover {
        color: $borderColor;

        > a {
          color: $borderColor;
        }
      }
    }

    > a {
      color: $fontWeightColor;
    }

    &:hover {
      color: $primaryColor;

      > a {
        color: $primaryColor;
      }
    }
  }
}

.icon-mark {
  color: #53cad1;

  &.re-mark {
    color: #facc48;
  }
}

.icon-yititapddan {
  font-size: 14px;
  color: #3A84FF;
}

.img-icon {
  display: inline-block;
  width: 14px;
  margin-top: -1px;
  margin-right: -1px;
}

>>> .bk-table {
  .mark-row {
    .cell {
      padding: 0;
    }
  }
}

.table-append-loading {
  padding: 12px 0;
  text-align: center;
}

.set-tips {
  color: #fff;
  text-decoration: underline;
  cursor: pointer;

  &:hover {
    color: #1768ef;
  }

  >>> .bk-link-text {
    font-size: 12px;
  }
}
</style>
