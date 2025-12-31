<template>
  <bk-user-selector
    :model-value="selectedUsers"
    :multiple="multiple"
    :api-base-url="apiBaseUrl"
    :tenant-id="tenantId"
    :placeholder="placeholder"
    :allow-create="allowCreate"
    :disabled="disabled"
    @change="handleSelectedChange">
  </bk-user-selector>
</template>

<script>
import { mapState } from 'vuex';
import BkUserSelector from '@blueking/bk-user-selector/vue2';
import '@blueking/bk-user-selector/vue2/vue2.css';
export default {
  components: {
    BkUserSelector,
  },
  props: {
    value: {
      type: String,
    },
    multiple: {
      type: Boolean,
      default: true,
    },
    placeholder: {
      type: String,
      default: '',
    },
    allowCreate: {
      type: Boolean,
      default: false,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      apiBaseUrl: window.BK_API_TENANT_BASE_URL,
      selectedUsers: this.value,
    };
  },
  computed: {
    ...mapState('displayname', {
      tenantId: 'tenantId',
    }),
  },
  watch: {
    value(newValue) {
      this.selectedUsers = newValue;
    },
  },
  methods: {
    handleSelectedChange(val) {
      this.$emit('update:value', val);
    },
  },
};
</script>
