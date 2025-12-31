<template>
  <div class="suggestion-table">
    <table
      v-for="(item, index) in tableData"
      :key="index">
      <tr>
        <td>{{ $t('影响软件包') }}</td>
        <td>{{ item?.packageName || '--' }}</td>
      </tr>
      <tr>
        <td>{{ $t('影响版本') }}</td>
        <td>
          <div class="influence-version" v-for="v in item.versions" :key="v.version">
            {{ v.version }}
            <div class="vertical-line"></div>
          </div>
        </td>
      </tr>
      <tr>
        <td>{{ $t('修复建议') }}</td>
        <td>{{ item?.fixAdvice || '--' }}</td>
      </tr>
    </table>
    <bk-button
      v-if="affectedPackages.length > maxShowRow"
      text
      theme="primary"
      @click="handleToggle">
      {{ toggleText }}
    </bk-button>
  </div>
</template>

<script>
export default {
  props: {
    affectedPackages: {
      type: Array,
    },
  },
  data() {
    return {
      isFold: true,
      maxShowRow: 2,
    };
  },
  computed: {
    toggleText() {
      return this.isFold ? this.$t('查看全部') : this.$t('收起');
    },
    tableData() {
      if (this.isFold) {
        return this.affectedPackages.slice(0, this.maxShowRow);
      }
      return this.affectedPackages;
    },
  },
  methods: {
    handleToggle() {
      this.isFold = !this.isFold;
    },
  },
};
</script>

<style lang="postcss" scoped>
.suggestion-table {
  margin: 24px 0;

  table {
    width: 100%;
    margin-bottom: 12px;
    color: #000;
    border-collapse: collapse;
    border-radius: 24px;

    tr {
      td {
        width: auto;
        padding: 12px;
        background-color: #FFF;
        border: 1px solid #EFEFEF;

        .influence-version {
          display: inline-block;

          .vertical-line {
            display: inline-block;
            width: 1px;
            height: 16px;
            margin: 0 15px;
            line-height: 26px;
            vertical-align: middle;
            cursor: auto;
            background: #f3f3f4;
          }
        }
      }

      td:nth-child(1) {
        width: 120px;
        background-color: #FAFAFA;
      }
    }
  }
}
</style>
