<template>
  <div class="nav-top">
    <router-link
      tag="div"
      :to="{ name: 'task-list', params: $route.params }"
      class="logo"
    >
      <img class="codecc-logo" :src="codecc" :alt="$t('腾讯代码分析')" />
      <h1 class="app-name">{{ $t('腾讯代码分析') }}</h1>
    </router-link>
    <div class="toggle-project">
      <bk-select
        v-model="projectId"
        class="project-select"
        @change="handleProjectChange"
        searchable
        :clearable="false"
      >
        <bk-option
          v-for="(option, index) in projectList"
          :key="index"
          :id="option.projectCode"
          :name="option.projectName"
        >
        </bk-option>
      </bk-select>
    </div>
    <div class="profile">
      <bk-dropdown-menu
        @show="dropdownShow"
        @hide="dropdownHide"
        align="right"
        ref="dropdown"
        class="dropdown-user"
      >
        <div slot="dropdown-trigger" class="menu-trigger">
          <span>{{ user.username }}</span>
          <i
            :class="[
              'bk-icon icon-angle-down',
              { 'icon-flip': isDropdownShow },
            ]"
          ></i>
        </div>
        <div slot="dropdown-content">
          <dl class="menu-content">
            <dt class="avatar-name">
              <span class="chinesename">{{ user.username }}</span>
            </dt>
            <dd class="feature-link-wrap">
              <ul class="feature-links">
                <li class="link-item">
                  <a :href="projectManageUrl" @click="triggerHandler">{{
                    $t('任务管理')
                  }}</a>
                </li>
              </ul>
            </dd>
          </dl>
        </div>
      </bk-dropdown-menu>
    </div>
  </div>
</template>

<script>
import { mapGetters, mapState } from 'vuex';
import codecc from '@/images/codecc.svg';

export default {
  data() {
    return {
      isDropdownShow: false,
      projectManageUrl: `${window.DEVOPS_SITE_URL}/console/pm`,
      codecc,
    };
  },
  computed: {
    ...mapGetters(['user']),
    ...mapState('project', {
      projectList: 'list',
    }),
    projectId() {
      return this.$route.params.projectId;
    },
  },
  methods: {
    dropdownShow() {
      this.isDropdownShow = true;
    },
    dropdownHide() {
      this.isDropdownShow = false;
    },
    triggerHandler() {
      this.$refs.dropdown.hide();
    },
    handleProjectChange(projectId) {
      this.$router.push({
        name: 'task-list',
        params: { ...this.$route.params, projectId },
      });
    },
  },
};
</script>

<style lang="postcss">
.nav-top {
  display: flex;
  height: 50px;
  padding: 0 16px;
  background: #191929;
  align-items: center;

  a {
    color: #63656e;
    text-decoration: none;
  }

  a:hover {
    color: #3a84ff;
  }

  .logo {
    width: 280px;
    flex: none;
    cursor: pointer;

    .codecc-logo {
      float: left;
      width: 36px;
      height: 36px;
      margin-right: 13px;
    }
  }

  .toggle-project {
    flex: 1;

    .project-select {
      width: 250px;
      color: #979ba5;
      background-color: #191929;
      border-color: #2a2a42;

      &:hover,
      &.active,
      &.is-focus {
        background-color: black;
        border-color: #2a2a42 !important;
        box-shadow: none;
      }

      .bk-select-dropdown .bk-tooltip-ref {
        outline: none;
      }
    }
  }

  .profile {
    flex: 1;
    display: flex;
    justify-content: flex-end;
    align-items: center;

    .help {
      margin-right: 24px;
    }

    .dropdown-user {
      .menu-trigger {
        cursor: pointer;
      }

      .menu-content {
        width: 212px;

        .feature-link-wrap {
          padding: 8px 16px;
        }
      }
    }

    .avatar-name {
      display: flex;
      align-items: center;
      padding: 12px 16px;
      border-bottom: 1px solid #dde4eb;

      .chinesename {
        padding-left: 12px;
      }
    }

    .avatar {
      width: 32px;
      height: 32px;
      overflow: hidden;
      border-radius: 50%;

      img {
        max-width: 100%;
      }
    }

    .feature-links {
      font-size: 14px;

      .link-item {
        margin: 4px 0;
      }
    }
  }
}

.app-name {
  padding: 6px;
  margin: 0;
  font-size: 16px;
  font-weight: normal;
}
</style>
