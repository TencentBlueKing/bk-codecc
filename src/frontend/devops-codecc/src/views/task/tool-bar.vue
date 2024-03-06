<template>
  <div class="task-list-tool-bar">
    <main class="tool-bar-content">
      <section class="bar-info">
        <span
          v-if="createFromGongfeng || createFromStream"
          v-bk-tooltips="toolTipsConfig"
        >
          <bk-button
            :disabled="createFromGongfeng || createFromStream"
            icon="plus"
            theme="primary"
          >{{ $t('新增任务') }}</bk-button
          >
          <div id="go-pipeline">
            <p v-if="createFromGongfeng">
              {{ $t('开源扫描CodeCC不支持新建任务，请前往') }}
              <a :href="pipelineSite" class="curpt" target="_blank">{{
                $t('蓝盾')
              }}</a>
            </p>
            <p v-if="createFromStream">
              {{ $t('Stream CodeCC不支持直接新建任务，请前往') }}
              <a :href="streamSite" class="curpt" target="_blank">Stream</a>
            </p>
          </div>
        </span>
        <span v-else>
          <bk-button
            v-if="isRbac === true"
            key="createTask"
            v-perm="{
              disablePermissionApi: false,
              permissionData: {
                projectId: projectId,
                resourceType: 'codecc_task',
                resourceCode: projectId,
                action: 'codecc_task_create',
              },
            }"
            icon="plus"
            theme="primary"
            @click="$router.push({ name: 'task-new' })"
          >{{ $t('新增任务') }}</bk-button
          >
          <bk-button
            v-else
            icon="plus"
            theme="primary"
            @click="$router.push({ name: 'task-new' })"
          >{{ $t('新增任务') }}</bk-button
          >
        </span>
        <span class="task-total-count">{{
          $t('共x个任务', { num: taskCount })
        }}</span>
      </section>
      <section class="bar-handle">
        <i
          class="icon codecc-icon icon-filter"
          v-bk-tooltips="$t('筛选')"
          @click="isFilter = !isFilter"
        ></i>
        <div v-bk-clickoutside="hideFeedBackMenu" style="display: inline">
          <i
            class="icon codecc-icon icon-sort"
            v-bk-tooltips="$t('排序')"
            @click.stop="toggleFeedBackMenu"
          ></i>
          <ul class="feedback-menu" v-show="showOrderType">
            <li v-for="(order, index) in orderList" :key="`order${index}`">
              <a @click.stop="changeOrderType(order.id)">{{ order.name }}</a>
            </li>
          </ul>
        </div>
      </section>
    </main>
    <div class="task-filter-bar" v-if="isFilter">
      <div class="mr40">
        <bk-radio-group size="small" v-model="searchInfo.showDisabledTask">
          <bk-radio-button :value="false">{{ $t('已启用任务') }}</bk-radio-button>
          <bk-radio-button :value="true">{{ $t('已停用任务') }}</bk-radio-button>
        </bk-radio-group>
      </div>
      <div class="task-filter-select" v-if="!searchInfo.showDisabledTask">
        <label>{{ $t('状态') }}</label>
        <bk-select
          v-model="searchInfo.taskStatusList"
          style="width: 240px"
          multiple
        >
          <bk-option
            v-for="option in statusList"
            :key="option.id"
            :id="option.id"
            :name="option.name"
          >
          </bk-option>
        </bk-select>
      </div>
      <!-- <div class="task-filter-select">
                <label>{{$t('代码库')}}</label>
                <bk-select v-model="searchInfo.codelib" style="width: 240px;" searchable>
                    <bk-option v-for="option in codelibList"
                        :key="option.id"
                        :id="option.id"
                        :name="option.name">
                    </bk-option>
                </bk-select>
            </div> -->
      <div class="task-filter-select" v-if="!searchInfo.showDisabledTask">
        <label>{{ $t('任务来源') }}</label>
        <bk-select v-model="searchInfo.taskSource" style="width: 240px">
          <bk-option
            v-for="option in sourceList"
            :key="option.id"
            :id="option.id"
            :name="option.name"
          >
          </bk-option>
        </bk-select>
      </div>
    </div>
  </div>
</template>

<script>
import { mapState } from 'vuex';
export default {
  props: {
    taskCount: {
      type: Number,
      default: 0,
    },
    searchInfo: {
      type: Object,
      default: {},
    },
  },
  data() {
    return {
      isFilter: true,
      showOrderType: false,
      statusList: [
        { id: 'SUCCESS', name: this.$t('成功') },
        { id: 'FAIL', name: this.$t('失败') },
        { id: 'WAITING', name: this.$t('待分析') },
        { id: 'ANALYSING', name: this.$t('分析中') },
        // { id: 'DISABLED', name: '已停用' },
      ],
      codelibList: [],
      orderList: [
        { id: 'SIMPLIFIED_PINYIN', name: `${this.$t('按名称')} a-Z` },
        { id: 'CREATE_DATE', name: this.$t('按创建时间') },
        { id: 'LAST_EXECUTE_DATE', name: this.$t('按最近执行时间') },
      ],
      sourceList: [
        { id: 'bs_pipeline', name: this.$t('流水线') },
        { id: 'bs_codecc', name: this.$t('自建任务') },
      ],
      toolTipsConfig: {
        allowHtml: true,
        content: '#go-pipeline',
      },
      pipelineSite: `${window.DEVOPS_SITE_URL}/console/pipeline`,
      streamSite: window.STREAM_SITE_URL,
    };
  },
  computed: {
    projectId() {
      return this.$route.params.projectId;
    },
    // CODE_开头为工蜂开源扫描任务
    createFromGongfeng() {
      return /^CODE_\d+$/.test(this.projectId);
    },
    // git_开头为stream扫描任务
    createFromStream() {
      return /^git_\d+$/.test(this.projectId);
    },
    ...mapState(['isRbac']),
  },
  methods: {
    hideFeedBackMenu() {
      this.showOrderType = false;
    },
    toggleFeedBackMenu() {
      this.showOrderType = !this.showOrderType;
    },
    changeOrderType(val) {
      this.showOrderType = false;
      this.$emit('changeOrder', val);
    },
  },
};
</script>

<style lang="postcss" scoped>
@import url('../../css/variable.css');

.task-list-tool-bar {
  margin-bottom: 16px;

  .tool-bar-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .task-total-count {
    margin-left: 18px;
    font-size: 12px;
    color: $itemBorderColor;
  }

  .bar-handle {
    position: relative;

    .codecc-icon {
      font-size: 22px;
      color: $fontLightColor;
      cursor: pointer;

      &:first-child {
        margin-right: 6px;
      }

      &:hover {
        color: $goingColor;
      }
    }

    .icon-sort {
      font-size: 20px;
    }
  }

  .task-filter-bar {
    display: flex;
    margin-top: 16px;
  }

  .task-filter-select {
    display: flex;
    align-items: center;
    margin-right: 40px;

    label {
      margin-right: 16px;
      font-size: 12px;
    }
  }

  .task-filter-switcher {
    display: flex;
    align-items: center;
    margin-left: auto;

    label {
      margin-right: 16px;
      margin-left: 0;
      font-size: 12px;
    }
  }

  .task-disabled-label {
    margin-left: 18px;
    font-size: 12px;
    color: #a4a6a8;
  }

  .feedback-menu {
    position: absolute;
    top: 36px;
    right: -8px;
    z-index: 3;
    background-color: white;
    border: 1px solid $itemBorderColor;
    border-radius: 2px;
    box-shadow: 0 3px 6px rgb(51 60 72 / 12%);

    &::before {
      position: absolute;
      top: -5px;
      right: 18px;
      width: 8px;
      height: 8px;
      background: white;
      border: 1px solid $itemBorderColor;
      border-right: 0;
      border-bottom: 0;
      content: '';
      transform: rotate(45deg);
    }

    li {
      line-height: 32px;
      text-align: center;
      border-bottom: 1px solid $itemBorderColor;

      &:last-child {
        border: 0;
      }

      a {
        padding: 0 14px;
        font-size: 14px;
        line-height: 30px;
        color: $fontWeightColor;
        white-space: nowrap;
        cursor: pointer;

        &:hover {
          color: $primaryColor;
        }
      }
    }
  }
}
</style>
