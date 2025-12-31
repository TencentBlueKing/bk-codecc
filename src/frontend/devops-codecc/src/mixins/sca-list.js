import { mapGetters, mapState } from 'vuex';
import { format } from 'date-fns';
import { language } from '../i18n';
import { export_json_to_excel } from '@/vendor/export2Excel';

export default {
  data() {
    const isFromOverview = this.$route.query.from === 'overview';
    const isProjectDefect = this.$route.name === 'project-defect-list';
    return {
      isFromOverview,
      isProjectDefect,
      isFullScreen: true,
      // enableToolName: 'PECKER_SCA',
      // toolId: 'PECKER_SCA',
      screenHeight: 336,
      panels: [
        { name: 'sca-pkg', label: `${this.$t('组件')} (0)` },
        { name: 'sca-vuln', label: `${this.$t('漏洞')} (0)` },
        { name: 'sca-lic', label: `${this.$t('许可证')} (0)` },
      ],
      basicSearchParams: {
        // data
        taskIdList: [Number(this.$route.params.taskId)],
        // toolNameList: ['PECKER_SCA'],
        dimensionList: ['SCA'],
        pattern: 'SCA',
        // query
        sortField: 'SEVERITY', // 按严重程度排序
        sortType: 'ASC', // 排序方式
      },
      severityFilters: [
        {
          text: this.$t('未知'),
          value: 0,
        },
        {
          text: this.$t('高危'),
          value: 1,
        },
        {
          text: this.$t('中危'),
          value: 2,
        },
        {
          text: this.$t('低危'),
          value: 4,
        },
      ],
      pkgSeverityMap: {
        0: this.$t('未知'),
        1: this.$t('高'),
        2: this.$t('中'),
        4: this.$t('低'),
      },
      pkgSeverityTextMap: {
        unknownCount: this.$t('未知'),
        seriousCount: this.$t('高'),
        normalCount: this.$t('中'),
        promptCount: this.$t('低'),
      },
      pkgSeverityFiledMap: {
        0: 'unknownCount',
        1: 'seriousCount',
        2: 'normalCount',
        4: 'promptCount',
      },
      vulnSeverityMap: {
        0: this.$t('未知'),
        1: this.$t('高'),
        2: this.$t('中'),
        4: this.$t('低'),
      },
      vulnSeverityTextMap: {
        unknownCount: this.$t('未知'),
        seriousCount: this.$t('高'),
        normalCount: this.$t('中'),
        promptCount: this.$t('低'),
      },
      vulnSeverityFiledMap: {
        0: 'unknownCount',
        1: 'seriousCount',
        2: 'normalCount',
        4: 'promptCount',
      },
      tabMap: {
        packageCount: 'sca-pkg',
        newVulCount: 'sca-vuln',
        licenseCount: 'sca-lic',
      },
    };
  },
  computed: {
    ...mapState('project', {
      projectVisitable: 'visitable',
    }),
    visitable() {
      return this.projectVisitable || !this.isProjectDefect;
    },
    ...mapGetters(['mainContentLoading']),
    ...mapState('task', {
      taskDetail: 'detail',
    }),
    dimensionStr() {
      return this.searchParams.scaDimensionList.join(',');
    },
    statusTreeData() {
      const { existCount, fixCount, ignoreCount, maskCount } = this.searchFormData;
      const list = this.ignoreList.map(item => ({
        id: `4-${item.ignoreTypeId}`,
        name: `${this.$t('已忽略')}-${item.name}`,
      }));
      const statusList = [
        {
          id: 1,
          name: `${this.$t('待修复')}${`(${existCount || 0})`}`,
        },
        {
          id: 2,
          name: `${this.$t('已修复')}${`(${fixCount || 0})`}`,
        },
        {
          id: 4,
          name: `${this.$t('已忽略')}${`(${ignoreCount || 0})`}`,
          children: list,
        },
        {
          id: 8,
          name: `${this.$t('已屏蔽')}${`(${maskCount || 0})`}`,
        },
      ];
      this.statusTreeKey += 1;
      return statusList;
    },
    isEn() {
      return language === 'en-US';
    },
    enableSCA() {
      return this.taskDetail.enableToolList.find(item => item.toolPattern === 'SCA');
    },
  },
  created() {
    // 初始化tab数量
    this.fetchTabPanelsCount();
  },
  methods: {
    severityFilterMethod(value, row, column) {
      const { property } = column;
      return row[property] === value;
    },
    // 初始化tab上的组件、漏洞、许可证数量
    async fetchTabPanelsCount() {
      const params = {
        taskId: this.$route.params.taskId,
        orderBy: 'dimension',
        buildNum: 'latest',
      };
      try {
        const res = await this.$store.dispatch('task/overView', params);
        if (res && res.lastClusterResultList) {
          const tabCountList = this.resolvePanelsCountData(res.lastClusterResultList);
          if (tabCountList.length !== 0) {
            for (const o of tabCountList) {
              const key = o[0];
              const count = o[1];
              const tabIndex = this.panels.findIndex(tab => tab.name === this.tabMap[key]);
              this.panels[tabIndex].label = this.panels[tabIndex].label.replace('0', count);
            }
          }
        }
      } catch (err) {
        console.error(err);
      }
    },
    // 根据返回值筛选SCA数据，并根据tabMap返回对应数量
    resolvePanelsCountData(lastClusterResultList) {
      const curBaseClusterResultVO = lastClusterResultList.find(item => item.baseClusterResultVO.type === 'SCA');
      if (curBaseClusterResultVO) {
        const tabCountList = Object.entries(curBaseClusterResultVO.baseClusterResultVO)
          .filter(item => Object.keys(this.tabMap).includes(item[0]));
        return tabCountList;
      }
      return null;
    },
    // 切换tab
    handleTabChange(value) {
      this.$router.push({
        name: `defect-${value}-list`,
        query: this.$route.query,
      });
    },
    // 配置规则集
    addTool(query) {
      if (this.taskDetail.projectId.startsWith('git_')) {
        this.$bkInfo({
          title: this.$t('配置规则集'),
          subTitle: this.$t('此代码检查任务为Stream创建，规则集需前往Stream配置。'),
          maskClose: true,
          confirmFn: (name) => {
            window.open(
              `${window.STREAM_SITE_URL}/pipeline/${this.taskDetail.pipelineId}
    #${this.taskDetail.projectName}`,
              '_blank',
            );
          },
        });
      } else if (this.taskDetail.createFrom.indexOf('pipeline') !== -1) {
        this.$bkInfo({
          title: this.$t('配置规则集'),
          subTitle: this.$t('此代码检查任务为流水线创建，规则集需前往相应流水线配置。'),
          maskClose: true,
          confirmFn: (name) => {
            window.open(
              `${window.DEVOPS_SITE_URL}/console/pipeline/${this.taskDetail.projectId}
/${this.taskDetail.pipelineId}/edit#${this.taskDetail.atomCode}`,
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
    formatDate(dateNum, time) {
      if (!dateNum) return '--';
      return time
        ? format(dateNum, 'yyyy-MM-dd')
        : format(dateNum, 'yyyy-MM-dd HH:mm:ss');
    },
    // 处理query提供的风险等级，并返回数组
    numToArray(num, arr = [0, 1, 2, 4]) {
      let filterArr = arr.filter(x => x & num);
      filterArr = filterArr.length ? filterArr : arr;
      return filterArr;
    },
    // 处理并展示风险等级文字
    getPkgCountBySeverity(severity) {
      const count = this.searchFormData[this.pkgSeverityFiledMap[severity]] || 0;
      return count > 100000 ? this.$t('10万+') : count;
    },
    // 处理并展示风险等级文字
    getVulnCountBySeverity(severity) {
      const count = this.searchFormData[this.vulnSeverityFiledMap[severity]] || 0;
      return count > 100000 ? this.$t('10万+') : count;
    },
    // 设置表头及表数据字段
    generateExcel(headerProps, list = []) {
      const tHeader = Object.values(headerProps);
      const filterVal = Object.keys(headerProps);

      const data = this.formatJson(filterVal, list);
      // eslint-disable-next-line
      const prefix = `${this.taskDetail.nameCn}-${this.taskDetail.taskId}-${this.dimensionStr}-`;
      const title = `${prefix}${this.$t('问题')}-${new Date().toISOString()}`;
      export_json_to_excel(tHeader, data, title);
    },
  },
};
