import { format } from 'date-fns';
import { mapState } from 'vuex';
import { leaveConfirm } from '../common/leave-confirm';

export default {
  data() {
    const { query } = this.$route;
    return {
      pathPanels: [
        { name: 'choose', label: this.$t('选择路径') },
        { name: 'input', label: this.$t('手动输入') },
      ],
      tabSelect: query.fileList ? 'input' : 'choose',
      inputFileList: query.fileList
        ? this.handleFileList(query.fileList)
        : [''],
      dimenList: [],
      dimenListFetched: false,
      isFromOverview: this.$route.query.from === 'overview',
      toolList: [], // 搜索栏工具列表
    };
  },
  inject: ['reload'],
  watch: {
    toolMap(val) {
      // 处理toolMap加载慢情况
      if (!this.dimension && this.searchParams.toolName) {
        this.fetchListTool();
      }
    },
    'operateParams.targetAuthor'() {
      window.changeAlert = true;
    },
    'operateParams.ignoreReasonType'() {
      window.changeAlert = true;
    },
    'operateParams.ignoreReason'() {
      window.changeAlert = true;
    },
    'commentParams.comment'() {
      window.changeAlert = true;
    },
  },
  computed: {
    ...mapState(['user']),
    ...mapState(['toolMeta']),
    ...mapState('tool', {
      toolMap: 'mapList',
    }),
    treeList() {
      return this.searchFormData.filePathTree
        && this.searchFormData.filePathTree.name
        ? [this.searchFormData.filePathTree]
        : [];
    },
    treeWidth() {
      function getLevel(data) {
        let res = 0;
        function loopGetLevel(data, level = res) {
          if (Array.isArray(data)) {
            for (let i = 0; i < data.length; i++) {
              if (data[i].children) {
                loopGetLevel(data[i].children, level + 1);
              } else {
                res = level + 1 > res ? level + 1 : res;
              }
            }
          }
        }
        loopGetLevel(data);
        return res;
      }
      const level = getLevel(this.treeList);
      const width = level < 10 ? 480 : 480 + 30 * (level - 9);
      return `width: ${width}px`;
    },
    ignoreTypeDisabled() {
      return !this.searchParams.status.includes(4);
    },
    ignoreToolTips() {
      return this.ignoreTypeDisabled
        ? this.$t('忽略类型筛选仅对已忽略问题生效。请先选择“已忽略”问题状态')
        : '';
    },
  },
  created() {
    this.fetchDimension();
  },
  mounted() {
    this.openCheckerset();
  },
  methods: {
    formatTime(date, token, options = {}) {
      return date ? format(Number(date), token, options) : '';
    },
    getTreeData() {
      const dirstrs = [];
      this.$refs.filePathTree.checked.forEach((item) => {
        const checkeditem = this.$refs.filePathTree.getNodeById(item);
        if (checkeditem.parent) {
          if (checkeditem.parent.parent && checkeditem.parent.indeterminate) {
            const fullPath = this.getFullPath(checkeditem).join('/');
            if (checkeditem.children.length) {
              dirstrs.push(`.*/${fullPath}/.*`);
            } else {
              dirstrs.push(`.*/${fullPath}`);
            }
          } else if (!checkeditem.parent.parent) {
            // 当为第二层时候，此时是虚拟层，需要传第三层内容给后台
            checkeditem.children.forEach((child) => {
              const fullPath = this.getFullPath(child).join('/');
              if (child.children.length) {
                dirstrs.push(`.*/${fullPath}/.*`);
              } else {
                dirstrs.push(`.*/${fullPath}`);
              }
            });
          }
        }
      });
      this.searchParams.fileList = dirstrs;
      return dirstrs;
    },
    getFullPath(item) {
      let fullPath = [item.name];
      const getPath = function (node, path = []) {
        if (node.parent) {
          path.unshift(node.parent.name);
          fullPath.unshift(node.parent.name);
          getPath(node.parent, path);
        }
      };
      getPath(item);
      fullPath = fullPath.slice(2);
      return fullPath;
    },
    handleFilePathClearClick() {
      const { filePathDropdown } = this.$refs;
      this.$refs.filePathTree && this.$refs.filePathTree.removeChecked();
      this.inputFileList = [''];
      this.searchFormData.filePathShow = '';
      this.searchParams.fileList = [];
      filePathDropdown.hide();
    },
    // 文件路径相关交互
    handleFilePathSearch(val) {
      this.$refs.filePathTree.filter(val);
    },
    // 路径过滤函数
    filterMethod(keyword, node) {
      return node.name.toLowerCase().indexOf(keyword.toLowerCase()) > -1;
    },
    handleSelectTool(toolName) {
      const tool = this.taskDetail.enableToolList.find(item => item.toolName === toolName);
      const toolPattern = tool.toolPattern.toLocaleLowerCase();
      this.$router.push({
        name: `defect-${toolPattern}-list`,
        params: { ...this.$route.params, toolId: toolName },
      });
    },
    handleTableChange(value) {
      let toolName = this.toolNameStr || this.toolName;
      if (!toolName || toolName.includes(',')) {
        toolName = this.toolList[0] && this.toolList[0].toolName;
      }
      const toolPattern = this.toolMap[toolName].pattern.toLocaleLowerCase();
      const params = { ...this.$route.params, toolId: toolName };
      const { query } = this.$route;
      if (value === 'defect') {
        this.$router.push({
          name: `defect-${toolPattern}-list`,
          params,
          query,
        });
      } else {
        this.$router.push({
          name: `defect-${toolPattern}-charts`,
          params,
          query,
        });
      }
    },
    addTool(query) {
      if (this.taskDetail.createFrom.indexOf('pipeline') !== -1) {
        const that = this;
        this.$bkInfo({
          title: this.$t('配置规则集'),
          subTitle: this.$t('此代码检查任务为流水线创建，规则集需前往相应流水线配置。'),
          maskClose: true,
          confirmFn(name) {
            window.open(
              `${window.DEVOPS_SITE_URL}/console/pipeline/${that.taskDetail.projectId}
/${that.taskDetail.pipelineId}/edit#${that.taskDetail.atomCode}`,
              '_blank',
            );
          },
        });
      } else {
        // this.$router.push({ name: 'task-settings-checkerset', query })
        const { from } = query;
        if (from === 'cov' || from === 'lint') {
          this.$router.push({ name: 'task-settings-checkerset', query });
          return;
        }
        const params = { toolName: from };
        this.$store.dispatch('checkerset/config', params).then((res) => {
          if (res.code === '0') {
            if (res.data === true) {
              setTimeout(() => {
                this.$store.dispatch('task/detail').then((res) => {
                  this.init(true);
                });
              }, 200);
            } else {
              this.$bkMessage({
                theme: 'error',
                message: this.$t('规则集安装失败'),
              });
            }
          } else {
            this.$bkMessage({
              theme: 'error',
              message: res.message || this.$t('规则集安装失败'),
            });
          }
        });
      }
    },
    handleDateChange(date, type) {
      this.searchParams.daterange = date;
      this.dateType = type;
    },
    changeTab(name) {
      this.tabSelect = name;
    },
    // 添加input框
    addPath(index) {
      this.inputFileList.push('');
    },
    cutPath(index) {
      if (this.inputFileList.length > 1) {
        this.inputFileList.splice(index, 1);
      }
    },
    handleFilePathConfirmClick() {
      if (this.tabSelect === 'choose') {
        const filePath = this.getTreeData();
        this.searchFormData.filePathShow = filePath.join(';');
      } else if (this.tabSelect === 'input') {
        const inputFileList = this.inputFileList.filter(item => item).slice();
        this.searchParams.fileList = inputFileList;
        this.searchFormData.filePathShow = inputFileList.join(';');
      }

      this.$refs.filePathDropdown.hide();
    },
    fetchDimension() {
      if (this.visitable === false) return;
      this.$store
        .dispatch('defect/getDimension', { taskIdList: this.taskIdList })
        .then((res) => {
          const list = res.filter(item => item.key !== 'CCN' && item.key !== 'DUPC' && item.key !== 'CLOC');
          this.dimenList = list;
          this.dimenListFetched = true;
          if (!this.dimension && this.searchParams.toolName && this.toolMap) {
            // 从工具过来
            this.fetchListTool();
          } else {
            this.fetchListTool();
          }
        });
    },
    // 从总览页面过来的链接，如果没有相关工具，提示跳转到规则集页面
    openCheckerset() {
      setTimeout(() => {
        // 要等维度信息加载完
        if (!this.dimenListFetched) {
          this.openCheckerset();
        } else if (
          this.$route.query.isOpenScan !== true
          && this.isFromOverview
          && !this.dimenList.find(item => this.dimensionStr.includes(item.key))
          && this.toolId !== 'CCN'
          && this.toolId !== 'DUPC'
        ) {
          const isFromPipeline = this.taskDetail.createFrom.indexOf('pipeline') !== -1;
          const title = isFromPipeline ? this.$t('前往流水线') : '';
          this.$bkInfo({
            title: this.$t('前往配置'),
            subTitle: this.$t('暂无此维度规则，请先x配置相关的规则集。', [
              title,
            ]),
            confirmFn: () => {
              if (isFromPipeline) {
                window.open(
                  `${window.DEVOPS_SITE_URL}/console/pipeline/${this.taskDetail.projectId}
/${this.taskDetail.pipelineId}/edit#${this.taskDetail.atomCode}`,
                  '_blank',
                );
              } else {
                this.$router.push({ name: 'task-settings-checkerset' });
              }
            },
          });
        }
      }, 500);
    },
    // 快照升级后，旧快照筛选不出来，需要提示用户
    handleNewBuildId(tips) {
      setTimeout(() => {
        this.$bkInfo({
          title: this.$t('立即检查'),
          subTitle: tips,
          confirmFn: this.triggerAnalyze,
        });
      }, 300);
    },
    triggerAnalyze() {
      setTimeout(() => {
        if (this.taskDetail.createFrom.indexOf('pipeline') !== -1) {
          const { projectId, pipelineId } = this.taskDetail;
          this.$bkInfo({
            title: this.$t('立即检查'),
            subTitle: this.$t('此代码检查任务需要到流水线启动，是否前往流水线？'),
            confirmFn(name) {
              window.open(
                `${window.DEVOPS_SITE_URL}/console/pipeline/${projectId}/${pipelineId}/edit`,
                '_blank',
              );
            },
          });
        } else {
          this.$store.dispatch('task/triggerAnalyze');
        }
      }, 300);
    },
    handleRepoList(list = [], repoId, branch) {
      const [repo] = list;
      if (repo) {
        return `${repo.repoId}@${repo.branch}`;
      }
      if (repoId) {
        return `${repoId}@${branch}`;
      }
      return '';
    },
    handleCommit(type, batchFlag, entityId, filePath) {
      if (type === 'commit') {
        const bizType = 'IssueDefect';
        const {
          toolNameStr,
          dimensionStr,
          toolName,
          dimension,
          taskIdList,
          toolNameList,
        } = this;
        const defectKeySet = [];
        if (batchFlag) {
          const table = this.$refs.fileListTable
            || this.$refs.table.$refs.fileListTable
            || {};
          const selection = table.selection || [];
          selection.forEach((item) => {
            defectKeySet.push(item.entityId);
          });
        } else {
          defectKeySet.push(entityId);
        }
        let payload = {
          toolName: toolNameStr || toolName,
          dimension: dimensionStr || dimension,
          taskIdList,
          toolNameList: toolNameList || toolName,
          dimensionList: dimension,
          defectKeySet,
          bizType,
        };
        if (this.isSelectAll === 'Y') {
          payload = {
            ...payload,
            isSelectAll: 'Y',
            queryDefectCondition: JSON.stringify(this.getSearchParams()),
          };
        }
        this.$store.dispatch('defect/defectCommit', payload).then((res) => {
          const h = this.$createElement;
          if (res.data.submitSuccess) {
            const { submitIssueList = [] } = res.data;
            this.$bkMessage({
              theme: 'success',
              message: h('p', {}, [
                this.$t('待修复问题提单至'),
                h(
                  'a',
                  {
                    attrs: {
                      href: submitIssueList[0]?.sysHomeUrl,
                      target: '_blank',
                    },
                    style: {
                      cursor: 'pointer',
                    },
                  },
                  submitIssueList[0]?.sysNameCn,
                ),
                this.$t('成功'),
              ]),
            });

            if (batchFlag) {
              if (this.toolName === 'CCN') {
                this.init();
              } else {
                this.fetchList();
              }
            } else {
              let list = this.listData.defectList.records;
              if (this.toolName === 'CCN') {
                list = this.listData.defectList.content;
              }
              list.forEach((item) => {
                if (item.entityId === entityId) {
                  item.defectIssueInfoVO.submitStatus = 1;
                }
              });
              this.listData.defectList.records = list.slice();
            }
            if (this.defectDetailDialogVisible) {
              this.fetchLintDetail('scroll');
            }
          } else {
            if (this.isProjectDefect) {
              const { failTaskList = [] } = res.data;
              const vnodeList = failTaskList.slice(0, 5).map((item, index) => h(
                'a',
                {
                  on: {
                    click: () => goToTaskSetting(item.taskId),
                  },
                  style: {
                    cursor: 'pointer',
                  },
                },
                index === failTaskList.length - 1 || index > 3
                  ? item.nameCn
                  : `${item.nameCn}、`,
              ));
              this.$bkInfo({
                title: this.$t('请先设置问题提单'),
                container: this.$refs.mainContainer,
                okText: this.$t('我授权好了'),
                subHeader: h(
                  'p',
                  {
                    style: {
                      'font-size': '14px',
                    },
                  },
                  [
                    h(
                      'p',
                      {},
                      this.$t(
                        '请先前往以下x个任务的设置-问题提单，将你的相关TAPD项目OAuth授权给蓝盾CodeCC：',
                        [failTaskList.length],
                      ),
                    ),
                    ...vnodeList,
                  ],
                ),
              });

              const goToTaskSetting = (id) => {
                let prefix = `${location.host}`;
                if (window.self !== window.top) {
                  prefix = `${window.DEVOPS_SITE_URL}/console`;
                }
                const route = this.$router.resolve({
                  name: 'task-settings-issue',
                  params: { taskId: id },
                });
                window.open(prefix + route.href, '_blank');
              };
            } else {
              this.$bkInfo({
                title: this.$t('请先设置问题提单'),
                subTitle: this.$t('请先前往设置-问题提单，将你的相关TAPD项目OAuth授权给蓝盾CodeCC。'),
                okText: this.$t('确定'),
                confirmFn: () => {
                  this.$router.push({ name: 'task-settings-issue' });
                },
              });
            }
          }
        });
      }
    },
    handleFileList(fileList) {
      if (fileList) {
        const list = fileList.split(';');
        return list.map(item => (item.startsWith('/') || item.startsWith('.') || item.startsWith('\\')
          ? `.*${item}`
          : `.*/${item}`));
      }
      return [];
    },
    /** 获取工具列表 */
    fetchListTool() {
      if (this.visitable === false) return;
      const { name } = this.$route;
      if (
        name === 'defect-ccn-list'
        || name === 'defect-dupc-list'
        || name === 'project-ccn-list'
      ) return;
      const {
        searchParams: { buildId },
        dimension,
        taskIdList,
      } = this;
      const params = {
        buildId,
        dimensionList: dimension,
        taskIdList,
      };
      this.$store.dispatch('defect/listToolName', params).then((res) => {
        this.toolList = res;
      });
    },
    /** 新手引导 */
    showIgnoreTypeSelect() {
      if (!this.guideFlag && !this.$route.query.entityId) {
        this.$nextTick(() => {
          if (this.$refs.statusSelect) {
            this.$refs.statusSelect?.show();
            setTimeout(() => {
              this.$refs.guidePopover.showHandler();
            }, 1000);
          } else {
            setTimeout(() => {
              this.$refs.statusSelect?.show();
              setTimeout(() => {
                this.$refs.guidePopover.showHandler();
              }, 1000);
            }, 1000);
          }
        });
      } else if (
        !localStorage.getItem('guide2End')
        && !this.$route.query.entityId
      ) {
        setTimeout(() => {
          this.handleGuideNextStep();
        }, 500);
      }
    },
    handleStatusRemote(keyword) {
      this.$refs.statusTree && this.$refs.statusTree.filter(keyword);
    },
    handleStatusCheckChange(id, checked) {
      let list = id;
      if (id.includes(4)) {
        list = id.filter(item => !/^4-/.test(item));
      }
      this.searchParams.statusUnion = [...list];
    },
    handleStatusValuesChange(options) {
      this.$refs.statusTree
        && this.$refs.statusTree.setChecked(options.id, {
          emitEvent: true,
          checked: false,
        });
    },
    handleStatusClear() {
      this.$refs.statusTree
        && this.$refs.statusTree.removeChecked({ emitEvent: false });
    },
    goToTask(taskId) {
      this.$router.push({
        name: 'task-detail',
        params: {
          taskId,
        },
      });
    },
    handleBeforeClose() {
      return leaveConfirm();
    },
  },
};
