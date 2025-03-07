<template>
  <section class="content">
    <div class="time-content">
      <bk-select
        v-model="days"
        :clearable="false"
        style="width: 120px;">
        <bk-option
          v-for="option in dayList"
          :key="option.id"
          :id="option.id"
          :name="option.name">
        </bk-option>
      </bk-select>
      <span class="summary">
        <span>{{ $t('工具误报{0}个', [statistics.total || 0]) }},&nbsp;</span>
        <span>{{ $t('已确认{0}个', [statistics.confirmed || 0]) }},&nbsp;</span>
        <span>{{ $t('待确认') }}
          <span style="color: #FF9C01;"> {{ statistics.unconfirmed || 0 }} </span>
          <span>{{ $t('个') }},&nbsp;</span>
        </span>
        <span>{{ $t('涉及{0}个代码库', [statistics.repos || 0]) }}</span>
      </span>
    </div>
    <div class="search-content" id="search-content">
      <section class="search-list">
        <span class="search-name">{{ $t('规则名称') }}</span>
        <bk-select
          class="search-select"
          searchable
          multiple
          v-model="searchValue.checkerNames">
          <bk-option
            v-for="option in optional.checkers || []"
            :key="option"
            :id="option"
            :name="option">
          </bk-option>
        </bk-select>
      </section>

      <section class="search-list">
        <span class="search-name">{{ $t('规则标签') }}</span>
        <bk-select
          class="search-select"
          searchable
          multiple
          v-model="searchValue.checkerTags">
          <bk-option
            v-for="option in optional.tags || []"
            :key="option"
            :id="option"
            :name="option">
          </bk-option>
        </bk-select>
      </section>

      <section class="search-list">
        <span class="search-name">{{ $t('严重级别') }}</span>
        <bk-checkbox-group
          class="search-select"
          v-model="searchValue.severities">
          <bk-checkbox
            v-for="severity in severityList"
            :value="severity.id"
            :key="severity.id">
            {{ severity.name }}
          </bk-checkbox>
        </bk-checkbox-group>
      </section>

      <section class="search-list">
        <span class="search-name">{{ $t('规则发布者') }}</span>
        <bk-select
          class="search-select"
          searchable
          multiple
          v-model="searchValue.publishers">
          <bk-option
            v-for="option in optional.publishers || []"
            :key="option"
            :id="option"
            :name="option">
          </bk-option>
        </bk-select>
      </section>

      <section class="search-list">
        <span class="search-name">{{ $t('来源') }}</span>
        <bk-select
          class="search-select"
          multiple
          v-model="searchValue.createFroms">
          <bk-option
            v-for="option in fromList"
            :key="option.id"
            :id="option.id"
            :name="option.name">
          </bk-option>
        </bk-select>
      </section>

      <section class="search-list">
        <span class="search-name">{{ $t('组织') }}</span>
        <org ref="orgTreeRef" class="search-select" v-model="organizationIdList"></org>
      </section>

      <section class="search-list">
        <span class="search-name">{{ $t('忽略时间') }}</span>
        <bk-date-picker v-model="daterange" class="search-select" :type="'daterange'"></bk-date-picker>
      </section>

      <section class="search-list">
        <span class="search-name">{{ $t('处理状态') }}</span>
        <bk-select
          class="search-select"
          multiple
          v-model="searchValue.processProgresses">
          <bk-option
            v-for="option in statusList"
            :key="option.id"
            :id="option.id"
            :name="option.name">
          </bk-option>
        </bk-select>
      </section>
    </div>
  </section>
</template>

<script>
import { bus } from '@/common/bus';
import Org from './org.vue';

export default {
  name: 'Search',
  components: {
    Org,
  },
  data() {
    return {
      days: '30',
      statistics: {},
      searchValue: {
        checkerNames: [],
        checkerTags: [],
        severities: [1, 2, 3],
        publishers: [],
        createFroms: [],
        organizationIds: [],
        startDate: '',
        endDate: '',
        processProgresses: [],
      },
      organizationIdList: [],
      treeData: [],
      typeIdMap: {},
      daterange: [],
      optional: {},
      fromList: [
        {
          id: 'bs_codecc',
          name: this.$t('CodeCC服务'),
        },
        {
          id: 'bs_pipeline',
          name: this.$t('流水线'),
        },
        {
          id: 'api_trigger',
          name: this.$t('OpenAPI'),
        },
        {
          id: 'gongfeng_scan',
          name: this.$t('开源/闭源扫描'),
        },
      ],
      dayList: [
        {
          id: '1',
          name: this.$t('最近{0}天', [1]),
        },
        {
          id: '3',
          name: this.$t('最近{0}天', [3]),
        },
        {
          id: '7',
          name: this.$t('最近{0}天', [7]),
        },
        {
          id: '15',
          name: this.$t('最近{0}天', [15]),
        },
        {
          id: '30',
          name: this.$t('最近{0}天', [30]),
        },
        {
          id: '60',
          name: this.$t('最近{0}天', [60]),
        },
        {
          id: '90',
          name: this.$t('最近{0}天', [90]),
        },
        {
          id: '180',
          name: this.$t('最近半年'),
        },
        {
          id: '365',
          name: this.$t('最近一年'),
        },
      ],
      statusList: [
        {
          id: 0,
          name: this.$t('待处理'),
        },
        {
          id: 4,
          name: this.$t('跟进中'),
        },
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
      severityList: [
        {
          id: 1,
          name: this.$t('严重'),
        },
        {
          id: 2,
          name: this.$t('一般'),
        },
        {
          id: 3,
          name: this.$t('提示'),
        },
      ],
      listData: [],
      sortParams: {
        sortField: 'ignoreTime',
        sortType: 'DESC',
      },
    };
  },
  watch: {
    days() {
      this.getStatistics();
      this.getList();
      this.getCount();
    },
    organizationIdList(selectList) {
      // 后端按照 BG、部门、中心 顺序处理数组。过滤事业线的id
      let scopes = []
      // 遍历组件传来的组织架构数组
      selectList.forEach(organize => {
        let organization = []
        // 6:BG、1:部门、7:中心
        // 后端按照数组顺序解析组织架构
        organize.forEach(id => {
          const type = this.typeIdMap.get(id);
          if (type === '6' || type === '1' || type === '7') {
            organization.push(parseInt(id));
          }
        });
        scopes.push(organization)
      })
      this.searchValue.organizationIds = scopes
    },
    searchValue: {
      handler() {
        this.getList();
        this.getCount();
      },
      deep: true,
    },
    daterange() {
      [this.searchValue.startDate, this.searchValue.endDate] = this.daterange;
    },
  },
  created() {
    this.init();
  },
  mounted() {
    this.orgTreeRef = this.$refs.orgTreeRef;
    bus.$on('refresh-paas-count', () => {
      this.getStatistics();
    });
  },
  methods: {
    init() {
      this.getRouteQuery();
      this.getStatistics();
      this.getOptional();
      this.getOrganizationType();
    },
    getStatistics() {
      const { toolName } = this.$route.params;
      this.$store.dispatch('paas/getStatistics', {
        toolName,
        days: this.days,
      }).then((res) => {
        this.statistics = res;
      });
    },
    getOptional() {
      const { toolName } = this.$route.params;
      this.$store.dispatch('paas/getOptional', {
        toolName,
      }).then((res) => {
        this.optional = res;
      });
    },
    getCount() {
      this.$store.dispatch('paas/getCount', {
        toolName: this.$route.params.toolName,
        days: this.days,
        params: this.searchValue,
      });
    },
    getList(isLoadMore, sortParams) {
      let lastInd = '';
      if (isLoadMore) {
        lastInd = this.listData[this.listData.length - 1]?.entityId;
        if (!lastInd) {
          return;
        }
      }
      if (sortParams) {
        this.sortParams = Object.assign({}, this.sortParams, sortParams);
      }
      const { toolName } = this.$route.params;
      return this.$store.dispatch('paas/getList', {
        toolName,
        days: this.days,
        params: this.searchValue,
        lastInd,
        ...this.sortParams,
      }).then((res) => {
        this.listData = res;
      });
    },
    async getRouteQuery() {
      // 从路由路径中提取查询参数
      const { bgId, startDate, endDate, deptIds } = this.$route.query;
      // 检查是否存在必要的查询参数
      if (bgId !== undefined) {
        // 设置搜索值和日期范围
        this.searchValue.startDate = startDate || '';
        this.searchValue.endDate = endDate || '';
        this.daterange = [this.searchValue.startDate, this.searchValue.endDate];
        // 准备部门 ID 数组
        let deptIdArray = deptIds;
        if (deptIds !== undefined && !Array.isArray(deptIds)) {
          deptIdArray = [deptIds];
        }
        // 根据部门 ID 显示下拉框
        this.$nextTick(() => {
          this.$refs.orgTreeRef.showDropdownByIds(deptIdArray || [bgId]);
        });
      }
    },
    async getOrganizationType() {
      await this.$store.dispatch('getDeptTree').then((res) => {
        this.treeData = res.treeData || [];
      });
      this.typeIdMap = this.buildNodeMap(this.treeData)
    },

    buildNodeMap(treeData, nodeMap = new Map()) {
      for (const node of treeData) {
        // 将当前节点的 id 和 typeId 存入 nodeMap
        nodeMap.set(node.id, node.typeId);

        // 如果当前节点有子节点,则递归遍历子节点
        if (node.children) {
          this.buildNodeMap(node.children, nodeMap);
        }
      }
      return nodeMap;
    }
  },
};
</script>

<style lang="postcss" scoped>
.content {
  font-size: 12px;
}

.time-content {
  display: flex;
  padding: 12px 24px;
  margin-bottom: 16px;
  background: #fff;
  align-items: center;
}

.search-content {
  display: flex;
  padding-top: 12px;
  background: #fff;
  flex-wrap: wrap;

  .search-list {
    display: flex;
    align-items: center;
    width: 304px;
    padding: 6px 0;
    margin-left: 20px;
  }
}

.search-name {
  width: 60px;
  margin-right: 20px;
  text-align: right;
}

.search-select {
  flex-grow: 1;
  width: 220px;
}

.bk-form-checkbox {
  margin-right: 20px;
}

.summary {
  margin-left: 16px;
}

>>> .bk-form-checkbox .bk-checkbox-text {
  font-size: 12px;
}
</style>
