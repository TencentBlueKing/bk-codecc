<template>
  <div class="cc-ignore main-content-outer main-content-ignore">
    <bk-tab :active.sync="active" type="card-tab" style="margin-top: 20px;" @tab-change="handleTabChange">
      <bk-tab-panel name="ignore-type" :label="$t('忽略类型')">
        <ignore-type />
      </bk-tab-panel>
      <bk-tab-panel v-if="isInnerSite" name="ignore-approval" :label="$t('忽略审批')">
        <ignore-approval />
      </bk-tab-panel>
    </bk-tab>
  </div>
</template>

<script>
import IgnoreType from './ignore-type.vue';
import IgnoreApproval from './ignore-approval.vue';
import router from '@/router';
import DEPLOY_ENV from '@/constants/env';

export default {
  components: {
    IgnoreType,
    IgnoreApproval,
  },
  data() {
    return {
      active: this.$route.query?.active || 'ignore-type',
      isInnerSite: DEPLOY_ENV === 'tencent',
    };
  },
  methods: {
    handleTabChange(value) {
      router.replace({
        query: {
          active: value,
        },
      });
    },
  },
};
</script>

<style lang="postcss" scoped>
::v-deep .bk-tab-section {
  background-color: #fff;
}

.main-content-outer {
  width: 1236px;
}
</style>
