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
        <org class="search-select" v-model="searchValue.organizationIds"></org>
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
    bus.$on('refresh-paas-count', () => {
      this.getStatistics();
    });
  },
  methods: {
    init() {
      this.getStatistics();
      this.getOptional();
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
