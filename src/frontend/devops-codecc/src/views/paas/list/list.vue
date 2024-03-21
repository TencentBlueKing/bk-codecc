
<template>
  <div class="table-list">
    <div class="total-count">{{ $t('共{0}问题', [totalCount]) }}</div>
    <bk-table
      size="medium"
      ref="table"
      class="table-list-content"
      :max-height="maxHeight"
      :data="tableList"
      :row-class-name="handleRowClassName"
      :scroll-loading="bottomLoadingOptions"
      v-bkloading="{ isLoading: listLoading }"
      @scroll-end="scrollLoadMore"
      @sort-change="handleSortChange"
      @row-click="handleRowClick">
      <bk-table-column :label="$t('项目')" prop="projectName" min-width="100"></bk-table-column>
      <bk-table-column :label="$t('任务')" show-overflow-tooltip prop="taskNameCn" min-width="100"></bk-table-column>
      <bk-table-column :label="$t('问题位置')" prop="repo" show-overflow-tooltip min-width="300">
        <template slot-scope="props">
          <span>{{ props.row.fileLink + ":" + props.row.lineNum }}</span>
        </template>
      </bk-table-column>
      <bk-table-column :label="$t('规则')" show-overflow-tooltip prop="checker" min-width="100"></bk-table-column>
      <bk-table-column :label="$t('规则发布者')" prop="publisher" min-width="100"></bk-table-column>
      <bk-table-column :label="$t('级别')" prop="severity">
        <template slot-scope="props">
          <span :class="`severity-${props.row.severity}`">{{ severityMap[props.row.severity] }}</span>
        </template>
      </bk-table-column>
      <bk-table-column :label="$t('忽略人')" prop="ignoreAuthor"></bk-table-column>
      <bk-table-column :label="$t('忽略时间')" :sortable="'custom'" prop="ignoreTime" min-width="100">
        <template slot-scope="props">
          <span>{{ props.row.ignoreTime | formatDate('date') }}</span>
        </template>
      </bk-table-column>
      <bk-table-column :label="$t('忽略原因')" prop="ignoreReason" show-overflow-tooltip min-width="100">
        <template slot-scope="props">
          <span>
            {{ getIgnoreReasonByType(props.row.ignoreReasonType) }}
            {{ props.row.ignoreReason ? ': ' + props.row.ignoreReason : '' }}
          </span>
        </template>
      </bk-table-column>
      <bk-table-column :label="$t('处理状态')" prop="processProgress" min-width="100">
        <template slot-scope="props">
          <span class="dot" :class="`status-${props.row.processProgress}`"></span>
          <span>{{ statusMap[props.row.processProgress] }}</span>
          <bk-icon
            v-if="props.row.processProgress === 2 || props.row.processProgress === 3"
            v-bk-tooltips="{
              allowHTML: false,
              content: reasonMap[props.row.processReasonType]
                + `${props.row.processReason ? ': ' + props.row.processReason : ''}`
            }"
            type="info-circle" />
          <bk-icon
            v-else-if="props.row.processProgress === 1 && (props.row.processReason || props.row.issueLink) "
            v-bk-tooltips="{
              allowHTML: false,
              content: props.row.processReason +
                `${props.row.processReason && props.row.issueLink ? ':' : ''}`
                + props.row.issueLink
            }"
            type="info-circle" />
        </template>
      </bk-table-column>
      <bk-table-column :label="$t('操作')">
        <template slot-scope="props">
          <bk-popover
            theme="light"
            placement="bottom">
            <div class="dropdown-trigger-text">
              <span>{{ $t('处理') }}</span>
              <i class="bk-icon icon-angle-down"></i>
            </div>
            <ul class="bk-dropdown-list" slot="content">
              <li
                v-for="item in statusList"
                :key="item.id"
                @click.stop="handleDefect(item.id, props.row)">
                <a class="dropdown-list-item">{{ item.name }}</a>
              </li>
            </ul>
          </bk-popover>
        </template>
      </bk-table-column>
    </bk-table>
    <process ref="process"></process>
    <bk-dialog
      v-model="defectDetailDialogVisible"
      :ext-cls="'file-detail-dialog'"
      :fullscreen="true"
      :position="{ top: 0 }"
      :draggable="false"
      :mask-close="false"
      :show-footer="false"
      :close-icon="false"
      width="80%"
    >
      <detail
        ref="detail"
        :is-paas="true"
        :row="row"
        :type="'defect'"
        :list="tableList"
        :is-loading.sync="detailLoading"
        :is-full-screen="true"
        :visible="defectDetailDialogVisible"
        :file-index="fileIndex"
        :total-count="totalCount"
        :ignore-list="ignoreList"
        :current-file="currentFile"
        :handle-mark="handleMark"
        :build-num="buildNum"
        :lint-detail="lintDetail"
        :handle-file-list-row-click="handleRowClick"
        :is-file-list-load-more="isFileListLoadMore"
        :next-page-start-num="nextPageStartNum"
        :next-page-end-num="nextPageEndNum"
        :is-project-defect="isProjectDefect"
        @scrollLoadMore="scrollLoadMore"
        @closeDetail="defectDetailDialogVisible = false"
      >
      </detail>
    </bk-dialog>
  </div>
</template>
<script>
import { mapState } from 'vuex';
import Process from './process.vue';
import Detail from '@/views/defect/detail.vue';
import { throttle } from 'lodash';

export default {
  components: {
    Process,
    Detail,
  },
  data() {
    return {
      severityMap: {
        1: this.$t('严重'),
        2: this.$t('一般'),
        3: this.$t('提示'),
      },
      statusList: [
        {
          id: 1,
          name: this.$t('已优化工具'),
        },
        {
          id: 2,
          name: this.$t('非工具原因'),
        },
        {
          id: 3,
          name: this.$t('其他'),
        },
      ],
      statusMap: {
        0: this.$t('待处理'),
        1: this.$t('已优化工具'),
        2: this.$t('非工具原因'),
        3: this.$t('其他'),
      },
      reasonMap: {
        1: this.$t('用户误操作'),
        2: this.$t('用户不配合'),
        3: this.$t('无法查看问题代码'),
        4: this.$t('受限于技术架构'),
        5: this.$t('修复成本过高'),
        6: this.$t('其他'),
      },
      ignoreList: [],
      defectDetailDialogVisible: false,
      detailLoading: false,
      lintDetail: {},
      currentFile: {},
      fileIndex: 0,
      row: {},
      firstRender: true,
      maxHeight: window.innerHeight - 288,
      bottomLoadingOptions: {
        size: 'small',
        isLoading: false,
      },
      currentPage: 1,
    };
  },
  computed: {
    ...mapState('paas', ['list', 'totalCount', 'listLoading']),
    tableList() {
      if (this.$route.query.entityId && this.firstRender && this.list.length) {
        this.firstRender = false;
        const index = this.list.findIndex(item => item.defectId === this.$route.query.entityId);
        if (index > 0) {
          this.fileIndex = index;
        }
      }
      return this.list;
    },
    nextPageStartNum() {
      return this.currentPage * 100 + 1;
    },
    nextPageEndNum() {
      return this.totalCount < (this.currentPage + 1) * 100 ? this.totalCount : (this.currentPage + 1) * 100;
    },
  },
  beforeCreate() {
    this.$store.dispatch('ignore/fetchIgnoreList').then((res) => {
      this.ignoreList = res.data;
    });
  },
  mounted() {
    this.keyOperate();
    if (this.$route.query.entityId) {
      this.defectDetailDialogVisible = true;
      this.getDetail(this.$route.query.entityId);
    }
    const calcHeight = throttle(() => {
      this.maxHeight = window.innerHeight - 198 - document.getElementById('search-content').offsetHeight;
    }, 800);
    window.addEventListener('resize', calcHeight);
    this.$nextTick(() => {
      this.maxHeight = window.innerHeight - 198 - document.getElementById('search-content').offsetHeight;
    });
  },
  methods: {
    handleDefect(id, row) {
      this.$refs.process.handleDefect(id, row);
    },
    async handleSortChange({ column, prop, order }) {
      const orders = { ascending: 'ASC', descending: 'DESC' };
      const sortParams = {
        sortField: prop,
        sortType: orders[order],
      };
      await this.$parent.$refs.search.getList(false, sortParams);
    },
    handleRowClick(row) {
      this.defectDetailDialogVisible = true;
      this.row = row;
      this.fileIndex = this.tableList.findIndex(item => item.defectId === row.defectId);
      this.getDetail(row.defectId);
    },
    getDetail(id) {
      this.detailLoading = true;
      const payload = {
        entityId: id,
        toolName: this.$route.params.toolName,
        dimension: '',
        pattern: 'LINT',
      };
      this.$store.dispatch('paas/getDetail', payload).then((res) => {
        this.lintDetail = res;
        this.currentFile = res.lintDefectDetailVO;
        this.$nextTick(() => {
          this.$refs.detail.handleCodeFullScreen();
          this.detailLoading = false;
        });
      });
    },
    async scrollLoadMore() {
      this.bottomLoadingOptions.isLoading = true;
      this.bottomLoadingOptions.text = this.$t(
        '正在加载第x-y个，请稍后···',
        {
          x: this.nextPageStartNum,
          y: this.nextPageEndNum,
        },
      );
      await this.$parent.$refs.search.getList(true);
      this.bottomLoadingOptions.isLoading = false;
      this.currentPage += 1;
    },
    handleRowClassName({ row, rowIndex }) {
      let rowClass = 'list-row';
      if (this.fileIndex === rowIndex) rowClass += ' current-row';
      return rowClass;
    },
    keyEnter() {
      const row = this.tableList[this.fileIndex];
      this.handleRowClick(row);
    },
    keyOperate() {
      document.onkeydown = (event) => {
        const e = event || window.event;
        if (e.target.nodeName !== 'BODY') return;
        switch (e.code) {
          case 'Enter': // enter
            // e.path.length < 5 防止规则等搜索条件里面的回车触发打开详情
            if (!this.defectDetailDialogVisible && !this.authorEditDialogVisible) this.keyEnter();
            break;
          case 'Escape': // esc
            if (this.defectDetailDialogVisible) this.defectDetailDialogVisible = false;
            break;
          case 'ArrowLeft': // left
          case 'ArrowUp': // up
          case 'KeyW': // w
            if (e.shiftKey) {
              if (this.defectDetailDialogVisible) {
                this.$refs.detail.traceUp();
              }
            } else {
              if (this.fileIndex > 0) {
                if (this.defectDetailDialogVisible) {
                  this.handleRowClick(this.tableList[(this.fileIndex -= 1)]);
                } else {
                  this.fileIndex -= 1;
                }
                this.screenScroll();
              }
            }
            break;
          case 'ArrowRight': // right
          case 'ArrowDown': // down
          case 'KeyS': // s
            if (e.shiftKey) {
              if (this.defectDetailDialogVisible) {
                this.$refs.detail.traceDown();
              }
            } else {
              if (this.fileIndex < this.tableList.length - 1) {
                if (this.defectDetailDialogVisible) {
                  this.handleRowClick(this.tableList[(this.fileIndex += 1)]);
                } else {
                  this.fileIndex += 1;
                }
                this.screenScroll();
              }
            }
            break;
        }
      };
    },
    screenScroll() {
      this.$nextTick(() => {
        if (
          this.$refs.table
          && this.$refs.table.$refs.fileListTable
          && this.$refs.table.$refs.fileListTable.$refs.bodyWrapper
        ) {
          const children = this.$refs.table.$refs.fileListTable.$refs.bodyWrapper;
          const height = this.fileIndex > 3 ? (this.fileIndex - 3) * 42 : 0;
          children.scrollTo({
            top: height,
            behavior: 'smooth',
          });
        }
      });
    },
    getIgnoreReasonByType(type) {
      const typeMap = this.ignoreList.reduce((result, item) => {
        result[item.ignoreTypeId] = item.name;
        return result;
      }, {});
      return typeMap[type];
    },
  },
};
</script>


<style>
@import url('@/views/defect/codemirror.css');
</style>

<style lang="postcss" scoped>
.table-list {
  padding: 24px;
  background: #fff;

  >>> .list-row {
    cursor: pointer;
  }


  >>> .cell {
    -webkit-line-clamp: 2;
  }
}

.bk-dropdown-list {
  margin: -5px -14px;
  cursor: pointer;

  .dropdown-list-item {
    display: block;
    padding: 0 16px;
    line-height: 32px;
    color: #63656e;
    cursor: pointer;

    &:hover {
      color: #3a84ff;
      background-color: #f0f1f5;
    }
  }
}

.dropdown-trigger-text {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  height: 32px;
  min-width: 68px;
  color: #3a84ff;
  cursor: pointer;

  &:hover {
    .icon-angle-down {
      transform: rotate(180deg);
    }
  }
}

.dropdown-trigger-text .bk-icon {
  font-size: 22px;
  transition: all .2s ease;
  transform-origin: center;
}

>>> .bk-table-fixed-right tr.bk-table-row-last td.is-last {
  border-bottom: 1px solid #dfe0e5;
}

.severity-1 {
  color: #EA3636;
}

.severity-2 {
  color: #FF9C01;
}

.severity-3 {
  color: #699DF4;
}

.dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-right: 4px;
  background: #F0F1F5;
  border: 1px solid #C4C6CC;
  border-radius: 50%;
}

.status-0 {
  background: #FFE8C3;
  border: 1px solid #FF9C01;
}

.status-1 {
  background: #E5F6EA;
  border: 1px solid #3FC06D;
}

.file-detail-dialog {
  .bk-dialog {
    min-width: 1010px;
  }
}

>>> .file-detail-dialog {
  .bk-dialog-body {
    padding: 0;
  }
}

>>> .CodeMirror-linewidget .lint-hints .lint-icon {
  top: 7px;
}

::v-deep .bk-dialog-header {
  padding: 3px 24px 0 !important;
}

.total-count {
  margin-bottom: 10px;
  font-size: 12px;
  line-height: 16px;

  /* color: #979BA5; */
}
</style>

