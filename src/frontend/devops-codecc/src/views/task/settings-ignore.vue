<template>
  <div>
    <bk-tab
      class="settings-ignore-tab"
      :label-height="42"
      :active.sync="active"
      type="unborder-card"
      @tab-change="changeTab"
    >
      <!-- <template slot="setting" v-if="taskDetail.createFrom === 'gongfeng_scan'">
        <span class="f12">
          <i class="bk-icon icon-exclamation-circle-shape red"></i>
          {{$t('安全漏洞工具依然会检查屏蔽路径的代码')}}
        </span>
      </template> -->
      <bk-tab-panel
        v-for="(panel, index) in options"
        :key="index"
        v-bind="panel"
      >
      </bk-tab-panel>
      <!-- 添加路径组件 -->
      <div class="path-list" v-if="customExist">
        <span v-if="projectId === 'CUSTOMPROJ_PCG_RD'" class="link-text">{{
          $t('由PCG EP度量平台创建的任务，暂不支持在页面自定义屏蔽路径')
        }}</span>
        <span v-else-if="!isEditable" class="link-text"
        >{{ $t('修改路径屏蔽，请前往流水线') }}
          <a @click="handleToPipeline" href="javascript:;">{{
            $t('立即前往>>')
          }}</a></span
        >
        <div
          class="settings-ignore-header"
          v-if="customExist && isEditable && projectId !== 'CUSTOMPROJ_PCG_RD'"
        >
          <bk-button
            v-if="customExist && isRbac === true"
            key="add"
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
            @click="newVisible"
            size="small"
            icon="plus"
            :theme="'primary'"
            :title="$t('添加路径')"
          >
            {{ $t('添加路径') }}
          </bk-button>
          <bk-button
            v-else-if="customExist"
            @click="newVisible"
            size="small"
            icon="plus"
            :theme="'primary'"
            :title="$t('添加路径')"
          >{{ $t('添加路径') }}</bk-button
          >
          <span class="header-tab-right">{{
            $t('添加后的代码路径将不会产生问题')
          }}</span>
        </div>
        <div
          class="custom-path"
          v-for="(customPath, index) in customList"
          :key="index"
        >
          {{ customPath }}
          <div
            class="del-style"
            v-if="isEditable"
            @click="delCustomPath(customPath)"
          >
            {{ $t('删除') }}
          </div>
        </div>
      </div>
      <!-- /添加路径组件 -->
      <!-- 系统默认组件 -->
      <div class="path-list" v-if="defaultExist">
        <div class="settings-ignore-header" v-if="defaultExist">
          <bk-button
            v-if="defaultExist"
            @click="inputVisible"
            icon="plus"
            :theme="'primary'"
            :title="$t('默认设置')"
          >{{ $t('默认设置') }}</bk-button
          >
          <span class="header-tab-right">{{
            $t('添加后的代码路径将不会产生问题')
          }}</span>
        </div>
        <div
          class="default-path"
          v-for="(defaultPath, index) in defaultSelectList"
          :key="index"
        >
          {{ defaultPath }}
          <div class="del-style" @click="delDefaultPath(defaultPath)">
            {{ $t('移除') }}
          </div>
        </div>
      </div>
      <!-- /系统默认组件 -->
      <!-- yml -->
      <div class="mt10" v-if="ymlExist">
        <span class="yaml-desc">{{
          $t('在代码仓库根目录下的codeyml设置屏蔽路径后')
        }}</span>
        <Ace
          lang="yaml"
          theme="idle_fingers"
          :read-only="true"
          v-model="ymlContent"
          height="300"
          width="100%"
        >
        </Ace>
      </div>
      <!-- /yml -->
      <div class="no-path" v-if="customNone">
        <empty
          size="small"
          title=""
          :desc="$t('添加后的代码路径将被屏蔽，不再有问题等产生')"
        >
          <template #action>
            <span v-if="projectId === 'CUSTOMPROJ_PCG_RD'" class="fs12">{{
              $t('由PCG EP度量平台创建的任务，暂不支持在页面自定义屏蔽路径')
            }}</span>
            <span v-else-if="!isEditable" class="fs12"
            >{{ $t('修改路径屏蔽，请前往流水线') }}
              <a @click="handleToPipeline" href="javascript:;">{{
                $t('立即前往>>')
              }}</a></span
            >
            <bk-button
              v-else-if="isRbac === true"
              key="add-path"
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
              @click="newVisible"
            >
              {{ $t('添加路径') }}
            </bk-button>
            <bk-button v-else theme="primary" @click="newVisible">{{
              $t('添加路径')
            }}</bk-button>
          </template>
        </empty>
      </div>
      <div class="no-path" v-if="defaultNone">
        <empty
          size="small"
          title=""
          :desc="$t('添加后的代码路径将被屏蔽，不再有问题等产生')"
        >
          <template #action>
            <bk-button theme="primary" @click="inputVisible">{{
              $t('添加路径')
            }}</bk-button>
          </template>
        </empty>
      </div>
    </bk-tab>
    <bk-dialog
      v-model="delVisible"
      :theme="'primary'"
      @cancel="delVisible = false"
      @confirm="handleDelete(pathName)"
      :title="$t('确认')"
    >
      <span style="word-break: break-all">{{
        $t('确认要删除吗', { pathName })
      }}</span>
    </bk-dialog>
    <SettingsIgnoreNew
      v-if="isEditable"
      :visible="isCreateShow"
      @visibleChange="updateCreateVisible"
    />
    <SettingsIgnoreInput
      :selected="defaultSelectList"
      :list="defaultList"
      :visible="isInputShow"
      @visibleChange="updateInputVisible"
    />
  </div>
</template>

<script>
import { mapState } from 'vuex';
import SettingsIgnoreNew from '@/components/settings-ignore-new/index';
import SettingsIgnoreInput from '@/components/settings-ignore-new/input';
import Empty from '@/components/empty';
import Ace from '@/components/ace-editor';

export default {
  components: {
    SettingsIgnoreNew,
    SettingsIgnoreInput,
    Empty,
    Ace,
  },
  data() {
    return {
      options: [
        { name: 'custom', label: this.$t('自定义') },
        { name: 'default', label: this.$t('系统默认') },
        { name: 'yml', label: 'YAML' },
      ],
      select: 'custom',
      tabSelect: 'custom',
      isCreateShow: false,
      isInputShow: false,
      delVisible: false,
      pathName: '',
      ymlContent: '',
    };
  },
  computed: {
    ...mapState('task', {
      taskIgnore: 'ignore',
      taskDetail: 'detail',
    }),
    ...mapState(['isRbac']),
    taskId() {
      return this.$route.params.taskId;
    },
    isEditable() {
      return (
        !this.taskDetail.atomCode
        || this.taskDetail.createFrom !== 'bs_pipeline'
        || this.taskDetail.createFrom === 'gongfeng_scan'
        || this.taskDetail.createFrom === 'api_trigger'
      );
    },
    customNone() {
      return this.tabSelect === 'custom' && this.customList.length === 0;
    },
    defaultNone() {
      return (
        this.tabSelect === 'default' && this.defaultSelectList.length === 0
      );
    },
    customExist() {
      return this.tabSelect === 'custom' && this.customList.length !== 0;
    },
    defaultExist() {
      return (
        this.tabSelect === 'default' && this.defaultSelectList.length !== 0
      );
    },
    ymlExist() {
      return this.tabSelect === 'yml';
    },
    customList() {
      const customList = [];
      if (this.taskIgnore) {
        Object.assign(customList, this.taskIgnore.filterPaths);
        return customList;
      }
      return customList;
    },
    defaultList() {
      const defaultList = [];
      if (this.taskIgnore) {
        Object.assign(defaultList, this.taskIgnore.defaultFilterPath);
        return defaultList;
      }
      return defaultList;
    },
    defaultSelectList() {
      const defaultSelectList = [];
      if (this.taskIgnore) {
        Object.assign(defaultSelectList, this.taskIgnore.defaultAddPaths);
        return defaultSelectList;
      }
      return defaultSelectList;
    },
    projectId() {
      return this.$route.params.projectId;
    },
  },
  created() {
    this.$store.dispatch('task/ignore', this.taskId);
    this.$store.dispatch('task/getYml', this.taskId).then((res) => {
      this.ymlContent = `source:
    # ${this.$t('文件或目录使用绝对路径，绝对路径按代码库根目录计算，以/开头。')}
    # ${this.$t('提供产品代码库中编写的测试代码存放目录或文件名格式，以便代码检查时进行排除处理')}
    # ${this.$t('不要使用.*/等正则表达式屏蔽掉所有代码，会使得代码存在风险，还会导致工具扫不到任何代码而失败')}
    test_source:
        #${this.$t('用于匹配文件; 匹配方式为正则表达式，例如')}[".*/java/test/.*", ".*/test.java"]
        filepath_regex: ${this.formate(res.testSourceFilterPath)}
    # ${this.$t('提供产品代码库中工具或框架自动生成的且在代码库中的代码，没有可为空。以便代码检查时进行排除处理。')}
    auto_generate_source:
        # ${this.$t('自动生成代码文件的正则表达式，若无统一标识格式，可以指定具体目录，样例可参考test_source举例')}
        filepath_regex: ${this.formate(res.autoGenFilterPath)}
    # ${this.$t('提供产品代码库中直接以源码形式存在的第三方代码目录或代码文件名的正则表达。')}
    # ${this.$t('此处备注的第三方代码将在代码检查时进行排除，若代码库中不存在需要排除的第三方代码，该项配置标识可为空')}
    third_party_source:
        #${this.$t('第三方代码文件的正则表达式，若无统一标识格式，可以指定具体目录，样例可参考test_source举例')}
        filepath_regex: ${this.formate(res.thirdPartyFilterPath)}`;
    });
  },
  methods: {
    // 切换tab页面
    changeTab(name) {
      this.tabSelect = name;
    },
    // 打开添加路径组件
    newVisible() {
      this.isCreateShow = true;
    },
    // 打开添加系统默认组件
    inputVisible() {
      this.isInputShow = true;
    },
    delCustomPath(path) {
      this.pathName = path;
      this.delVisible = true;
    },
    delDefaultPath(path) {
      this.pathName = path;
      this.delVisible = true;
    },
    // 弹出组件的显示关闭状态与方法
    updateCreateVisible(visible) {
      this.isCreateShow = visible;
    },
    updateInputVisible(visible) {
      this.isInputShow = visible;
    },
    handleDelete(name) {
      const params = {
        taskId: '',
        path: '',
        pathType: '',
      };
      if (this.tabSelect === 'default') {
        params.taskId = this.taskId;
        params.path = name;
        params.pathType = 'DEFAULT';
      } else if (this.tabSelect === 'custom') {
        params.taskId = this.taskId;
        params.path = name;
        params.pathType = 'CUSTOM';
      }
      this.$store
        .dispatch('task/deleteIgnore', { params })
        .then((res) => {
          if (res === true) {
            this.$bkMessage({ theme: 'success', message: this.$t('删除成功') });
            this.$store.dispatch('task/ignore', this.taskId);
            this.$store.dispatch('task/ignoreTree');
          }
        })
        .catch((e) => {
          this.$bkMessage({ theme: 'error', message: this.$t('删除失败') });
          this.$store.dispatch('task/ignoreTree');
        });
      this.delVisible = false;
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
    formate(arr = []) {
      let str = '';
      if (arr.length > 0 && arr[0]) {
        for (let index = 0; index < arr.length; index++) {
          const e = arr[index];
          const beginStr = index === 0 ? '["' : '"';
          const endStr = index === arr.length - 1 ? '"]' : '", ';
          str = `${str}${beginStr}${e}${endStr}`;
        }
      }
      return str;
    },
  },
};
</script>

<style lang="postcss" scoped>
@import url('../../css/variable.css');

.settings-ignore-tab {
  margin: -17px -5px;
  border-top: 0;

  /* 路径列表 start */
  .path-list {
    padding-top: 5px;

    .custom-path,
    .default-path {
      padding: 8px 0;
      font-size: 14px;
      border-bottom: 1px solid $borderColor;

      .del-style {
        float: right;
        padding-right: 14px;
        font-size: 14px;
        color: $goingColor;
        cursor: pointer;
      }

      &:hover {
        background: $bgHoverColor;
      }
    }
  }

  .no-path {
    margin-top: 48px;
  }

  /* 路径列表 end */
  >>> .bk-tab-section {
    padding: 0 6px;
  }

  /* 组件按钮 start */
  .settings-ignore-header {
    line-height: 40px;

    .header-tab-left {
      display: inline-block;
      padding: 0 5px;
      font-size: 14px;
      cursor: pointer;
    }

    .active {
      padding-bottom: 6px;
      color: $goingColor;
      border-bottom: 2px solid $goingColor;
    }

    .header-tab-right {
      padding-left: 12px;
      font-size: 12px;
      color: $fontLightColor;
    }

    .header-tab-middle {
      padding-left: 18px;
      font-size: 12px;
      color: $goingColor;

      >>> .bk-button.bk-button-small {
        padding: 0 6px;
      }
    }
  }

  /* 组件按钮 end */
}

/* 覆盖组件样式 start */

/* >>>.settings-ignore-tab {
      .bk-tab-header {
        background-color: #fff;
        border: 0;
        background-image: linear-gradient(transparent 42px,#dcdee5 0);
        .bk-tab-label-wrapper {
          .bk-tab-label-list {
            .bk-tab-label-item {
              border: 0;
              min-width: 62px;
            }
            .bk-tab-label-item.active {
              color: $goingColor;
              font-weight: bold;
            }
            .bk-tab-label-item.active:after {
              background-color: $goingColor;
              height: 2px;
              top: auto;
              bottom: 0;
            }
          }
        }
      }
    }
    >>>.bk-tab.bk-tab-card .bk-tab-label-list {
      border: 0;
      .bk-tab-label-item.is-first {
        border: 0;
      }
    }
    >>>.bk-tab-section {
      border: 0;
    } */

/* 覆盖组件样式 end */
.link-text {
  font-size: 12px;
  line-height: 40px;

  a {
    margin-left: 12px;
  }
}

.fs12 {
  a {
    margin-left: 12px;
  }
}

.yaml-desc {
  display: inline-block;
  padding: 10px 0;
  font-size: 12px;
}

.red {
  color: red;
}
</style>
