<template>
  <div
    v-bkloading="{
      isLoading: !mainContentLoading && contentLoading,
      opacity: 0.3,
    }"
  >
    <div
      class="ccn-list"
      :class="{ 'project-defect': isProjectDefect }"
      v-if="
        taskDetail.enableToolList.find((item) => item.toolName === 'CCN') ||
          isProjectDefect
      "
    >
      <div class="breadcrumb">
        <div class="breadcrumb-name">
          <defect-panel v-if="isProjectDefect" />
          <bk-tab
            v-else
            :active.sync="active"
            :label-height="42"
            @tab-change="handleTableChange"
            type="unborder-card"
          >
            <bk-tab-panel
              v-for="(panel, index) in panels"
              v-bind="panel"
              :key="index"
            >
            </bk-tab-panel>
          </bk-tab>
          <div
            class="tab-extra-icon"
            :class="{ 'is-project': isProjectDefect }"
          >
            <span class="mr20">
              <bk-link theme="primary" @click="reload">
                <span class="f12">{{ $t('重置筛选条件') }}</span>
              </bk-link>
            </span>
            <span
              :class="{
                'filter-collapse-icon mr20': true,
                'mac-filter-search-icon': isMac,
              }"
            >
              <bk-popover
                ext-cls="handle-menu"
                ref="handleMenu"
                theme="light"
                placement="left-start"
                trigger="click"
              >
                <i
                  class="bk-icon codecc-icon icon-filter-collapse"
                  :class="
                    isSearchDropdown
                      ? 'icon-filter-collapse'
                      : 'icon-filter-expand'
                  "
                  v-bk-tooltips="
                    isSearchDropdown ? $t('收起筛选项') : $t('展开筛选项')
                  "
                  @click="toggleSearch"
                ></i>
              </bk-popover>
            </span>
            <span
              :class="{
                'filter-search-icon': true,
                'mac-filter-search-icon': isMac,
              }"
            >
              <bk-popover
                ext-cls="handle-menu"
                ref="handleMenu"
                theme="light"
                placement="left-start"
                trigger="click"
              >
                <i
                  class="bk-icon codecc-icon icon-filter-set"
                  v-bk-tooltips="$t('设置筛选条件')"
                ></i>
                <div slot="content">
                  <filter-search-option
                    :default-option="defaultOption"
                    :custom-option="customOption"
                    @selectAll="handleSelectAllSearchOption"
                    @confirm="handleConfirmSearchOption"
                  />
                </div>
              </bk-popover>
            </span>
            <span class="excel-icon pl20">
              <bk-button
                style="border: 0"
                v-if="exportLoading"
                icon="loading"
                :disabled="true"
                :title="$t('导出Excel')"
              ></bk-button>
              <span
                v-else
                class="codecc-icon icon-export-excel excel-download"
                @click="downloadExcel"
                v-bk-tooltips="$t('导出Excel')"
              ></span>
            </span>
          </div>
        </div>
      </div>
      <div class="main-container" ref="mainContainer">
        <div class="main-content-inner main-content-list">
          <bk-form
            :label-width="60"
            class="search-form main-form"
            :class="{ collapse: !isSearchDropdown }"
          >
            <container :class="['cc-container', { fold: !isSearchDropdown }]">
              <div class="cc-col" v-if="isProjectDefect">
                <bk-form-item :label="$t('任务')">
                  <bk-select searchable v-model="taskIdList" multiple>
                    <bk-option
                      v-for="item in taskList"
                      :key="item.taskId"
                      :id="item.taskId"
                      :name="item.nameCn"
                    >
                    </bk-option>
                  </bk-select>
                </bk-form-item>
              </div>
              <div class="cc-col">
                <bk-form-item :label="$t('处理人')">
                  <bk-tag-input
                    v-if="isProjectDefect"
                    v-model="searchParams.authorList"
                    :list="searchFormData.authorList"
                    :max-data="1"
                    :trigger="'focus'"
                    :allow-create="true"
                    :tpl="renderDisplayNameTagInputOption"
                    :tag-tpl="renderDisplayNameTagInputTag"
                  >
                  </bk-tag-input>

                  <bk-select v-else v-model="searchParams.author" searchable>
                    <template #trigger>
                      <SelectTrigger v-model="searchParams.author" />
                    </template>
                    <bk-option
                      v-for="author in searchFormData.authorList"
                      :key="author.id"
                      :id="author.id"
                      :name="author.name"
                    >
                      <bk-user-display-name :user-id="author.name"></bk-user-display-name>
                    </bk-option>
                  </bk-select>
                </bk-form-item>
              </div>
              <div class="cc-col" v-if="!isProjectDefect">
                <bk-form-item :label="$t('文件路径')" class="fixed-width">
                  <bk-dropdown-menu
                    @show="isFilePathDropdownShow = true"
                    @hide="isFilePathDropdownShow = false"
                    align="right"
                    trigger="click"
                    ref="filePathDropdown"
                  >
                    <bk-button type="primary" slot="dropdown-trigger">
                      <div
                        class="filepath-name"
                        :class="{ unselect: !searchFormData.filePathShow }"
                        :title="searchFormData.filePathShow"
                      >
                        {{
                          searchFormData.filePathShow
                            ? searchFormData.filePathShow
                            : $t('请选择')
                        }}
                      </div>
                      <i
                        :class="[
                          'bk-icon icon-angle-down',
                          { 'icon-flip': isFilePathDropdownShow },
                        ]"
                      ></i>
                    </bk-button>
                    <div
                      class="filepath-dropdown-content"
                      slot="dropdown-content"
                      @click="(e) => e.stopPropagation()"
                    >
                      <bk-tab
                        type="unborder-card"
                        class="create-tab"
                        :active="tabSelect"
                        @tab-change="changeTab"
                      >
                        <bk-tab-panel
                          v-for="(panel, index) in pathPanels"
                          v-bind="panel"
                          :key="index"
                        >
                        </bk-tab-panel>
                        <div
                          v-show="tabSelect === 'choose'"
                          class="create-tab-1"
                        >
                          <div>
                            <div class="content-hd">
                              <bk-input
                                v-model="searchInput"
                                class="search-input"
                                :clearable="true"
                                :placeholder="$t('搜索文件夹、问题路径名称')"
                                @input="handleFilePathSearch"
                              ></bk-input>
                            </div>
                            <div class="content-bd" v-if="treeList.length">
                              <bk-big-tree
                                ref="filePathTree"
                                height="340"
                                :style="treeWidth"
                                :options="{ idKey: 'treeId' }"
                                :show-checkbox="true"
                                :data="treeList"
                                :filter-method="filterMethod"
                                :expand-icon="'bk-icon icon-folder-open'"
                                :collapse-icon="'bk-icon icon-folder'"
                                :has-border="true"
                                :node-key="'name'"
                              >
                              </bk-big-tree>
                            </div>
                            <div class="content-empty" v-if="!treeList.length">
                              <empty size="small" :title="$t('无问题文件')" />
                            </div>
                          </div>
                        </div>
                        <div
                          v-show="tabSelect === 'input'"
                          class="create-tab-2"
                        >
                          <div class="input-info">
                            <div class="input-info-left">
                              <i class="bk-icon icon-info-circle-shape"></i>
                            </div>
                            <div class="input-info-right"></div>
                            {{ $t('搜索文件夹如P2PLive') }}<br />
                            {{ $t('搜索某类文件如P2PLive下') }}
                          </div>
                          <div class="input-paths">
                            <div
                              class="input-paths-item"
                              v-for="(path, index) in inputFileList"
                              :key="index"
                            >
                              <bk-input
                                :placeholder="$t('请输入')"
                                class="input-style"
                                v-model="inputFileList[index]"
                              ></bk-input>
                              <span class="input-paths-icon">
                                <i
                                  class="bk-icon icon-plus-circle-shape"
                                  @click="addPath(index)"
                                ></i>
                                <i
                                  class="bk-icon icon-minus-circle-shape"
                                  v-if="inputFileList.length > 1"
                                  @click="cutPath(index)"
                                ></i>
                              </span>
                            </div>
                          </div>
                        </div>
                      </bk-tab>
                      <div class="content-ft">
                        <bk-button
                          theme="primary"
                          @click="handleFilePathConfirmClick"
                        >{{ $t('确定') }}</bk-button
                        >
                        <bk-button @click="handleFilePathCancelClick">{{
                          $t('取消')
                        }}</bk-button>
                        <bk-button
                          class="clear-btn"
                          @click="handleFilePathClearClick"
                        >{{ $t('清空选择') }}</bk-button
                        >
                      </div>
                    </div>
                  </bk-dropdown-menu>
                </bk-form-item>
              </div>
              <div class="cc-col" v-show="allRenderColumnMap.daterange">
                <bk-form-item :label="$t('日期')">
                  <date-picker
                    :date-range="searchParams.daterange"
                    :handle-change="handleDateChange"
                    :status-union="searchParams.statusUnion"
                    :selected="dateType"
                  ></date-picker>
                </bk-form-item>
              </div>
              <div class="cc-col" v-if="!isProjectDefect">
                <bk-form-item :label="$t('快照')">
                  <bk-select
                    v-model="searchParams.buildId"
                    :clearable="true"
                    searchable
                  >
                    <bk-option
                      v-for="item in buildList"
                      :key="item.buildId"
                      :id="item.buildId"
                      :name="`#${item.buildNum} ${item.branch} ${
                        usersMap.get(item.buildUser)?.display_name || item.buildUser
                      } ${formatDate(item.buildTime) || ''}${$t('触发')}`"
                    >
                      <div
                        class="cc-ellipsis"
                        :title="`#${item.buildNum} ${item.branch} ${
                          usersMap.get(item.buildUser)?.display_name || item.buildUser
                        } ${formatDate(item.buildTime) || ''}${$t('触发')}`"
                      >
                        {{
                          `#${item.buildNum} ${item.branch} ${
                            usersMap.get(item.buildUser)?.display_name || item.buildUser
                          } ${
                            formatDate(item.buildTime) || ''
                          }${$t('触发')}`
                        }}
                      </div>
                    </bk-option>
                  </bk-select>
                </bk-form-item>
              </div>
              <div class="cc-col" v-if="allRenderColumnMap.status">
                <bk-form-item :label="$t('状态')">
                  <bk-select
                    ref="statusSelect"
                    searchable
                    multiple
                    v-model="searchParams.statusUnion"
                    :key="statusTreeKey"
                    :remote-method="handleStatusRemote"
                    :tag-fixed-height="false"
                    @tab-remove="handleStatusValuesChange"
                    @clear="handleStatusClear"
                  >
                    <bk-big-tree
                      :data="statusTreeData"
                      size="small"
                      show-checkbox
                      class="tree-select"
                      ref="statusTree"
                      v-if="hasCountData && hasIgnoreList"
                      :default-checked-nodes="searchParams.statusUnion"
                      :default-expand-all="!guideFlag"
                      @check-change="handleStatusCheckChange"
                    >
                      <div slot-scope="{ node, data }">
                        <div
                          v-if="
                            data.id === '4-24' && !guideFlag && hasIgnoreList
                          "
                        >
                          <bk-popover
                            ref="guidePopover"
                            placement="right-start"
                            :delay="2000"
                            theme="dot-menu light"
                          >
                            <span>{{ data.name }}</span>
                            <div class="guide-content" slot="content">
                              <div style="font-weight: bold; line-height: 22px">
                                {{ $t('【存量问题】可在这里查看') }}
                              </div>
                              <div>
                                {{ $t('针对特定忽略类型可') }}
                                <span
                                  theme="primary"
                                  class="set-tips"
                                  @click="handleSetReview"
                                >{{ $t('设置提醒') }}</span
                                >
                                {{ $t('，以便定期review和修复。') }}
                              </div>
                              <div class="guide-btn">
                                <span
                                  class="btn-item"
                                  @click="handleGuideNextStep"
                                >{{ $t('我知道了') }}</span
                                >
                              </div>
                            </div>
                          </bk-popover>
                        </div>
                        <div v-else>{{ data.name }}</div>
                      </div>
                    </bk-big-tree>
                  </bk-select>
                </bk-form-item>
              </div>
              <div class="cc-col-2" v-if="allRenderColumnMap.severity">
                <bk-form-item :label="$t('风险级别')">
                  <bk-checkbox-group
                    v-model="searchParams.severity"
                    class="checkbox-group"
                  >
                    <bk-checkbox
                      v-for="(name, value, index) in defectSeverityMap"
                      :value="Number(value)"
                      :key="index"
                    >
                      {{ name }}
                      <span v-if="!isProjectDefect"
                      >(<em
                        :class="[
                          'count',
                          `count-${['major', 'minor', 'info'][index]}`,
                        ]"
                      >{{ getDefectCountBySeverity(value) }}</em
                      >)</span
                      >
                    </bk-checkbox>
                    <bk-popover placement="top" width="220" class="popover">
                      <i class="codecc-icon icon-tips"></i>
                      <div slot="content">
                        <p>{{ $t('极高风险：复杂度>=60') }}</p>
                        <p>{{ $t('高风险：复杂度40-59') }}</p>
                        <p>{{ $t('中风险：复杂度20-39') }}</p>
                        <p>{{ $t('低风险：复杂度1-19') }}</p>
                        <p v-if="!isProjectDefect">
                          {{
                            $t(
                              '阈值被设置为20，列表中仅展示大于等于该阈值的函数',
                              { ccnThreshold: ccnThreshold }
                            )
                          }}
                        </p>
                      </div>
                    </bk-popover>
                  </bk-checkbox-group>
                </bk-form-item>
              </div>
            </container>
          </bk-form>

          <div class="cc-table">
            <div class="cc-selected">
              <span v-show="isSelectAll === 'Y'">{{
                $t('已选择x条,共y条', { x: totalCount, y: totalCount })
              }}</span>
              <span v-show="isSelectAll !== 'Y'">{{
                $t('已选择x条,共y条', { x: selectedLen, y: totalCount })
              }}</span>
            </div>
            <div v-if="isBatchOperationShow" class="cc-operate pb10">
              <div class="cc-operate-buttons">
                <bk-button
                  size="small"
                  ext-cls="cc-operate-button"
                  @click="handleMark(1, true)"
                  theme="primary"
                >{{ $t('标记处理') }}</bk-button
                >
                <bk-button
                  size="small"
                  ext-cls="cc-operate-button"
                  @click="handleAuthor(2)"
                  theme="primary"
                >{{ $t('分配') }}</bk-button
                >
                <bk-button
                  v-if="isInnerSite"
                  size="small"
                  ext-cls="cc-operate-button"
                  @click="handleCommit('commit', true)"
                  theme="primary"
                >{{ $t('提单') }}</bk-button
                >
                <bk-button
                  size="small"
                  ext-cls="cc-operate-button"
                  @click="handleIgnore('RevertIgnore', true)"
                  v-if="
                    !searchParams.status.length ||
                      searchParams.status.includes(4)
                  "
                  theme="primary"
                >
                  {{ $t('取消忽略') }}
                </bk-button>
                <bk-dropdown-menu
                  ext-cls="cc-operate-button"
                  ref="operateDropdown"
                  @show="isDropdownShow = true"
                  @hide="isDropdownShow = false"
                >
                  <bk-button size="small" slot="dropdown-trigger">
                    <span>{{ $t('更多') }}</span>
                    <i
                      :class="[
                        'bk-icon icon-angle-down',
                        { 'icon-flip': isDropdownShow },
                      ]"
                    ></i>
                  </bk-button>
                  <div class="handle-menu-tips" slot="dropdown-content">
                    <p class="entry-link" @click.stop="handleMark(0, true)">
                      {{ $t('取消标记') }}
                    </p>
                    <p
                      v-if="taskDetail.prohibitIgnore"
                      class="entry-link-allowed"
                      :title="
                        $t(
                          '已设置禁止页面忽略，可在代码行末或上一行使用注释忽略，例如// NOCC:rule1(ignore reason)'
                        )
                      "
                    >
                      {{ $t('忽略') }}
                    </p>
                    <p
                      v-else
                      class="entry-link"
                      @click.stop="handleIgnore('IgnoreDefect', true)"
                    >
                      {{ $t('忽略') }}
                    </p>
                  </div>
                </bk-dropdown-menu>
              </div>
            </div>

            <div class="cc-keyboard">
              <span>{{ $t('当前已支持键盘操作') }}</span>
              <bk-button
                text
                ext-cls="cc-button"
                @click="operateDialogVisible = true"
              >{{ $t('如何操作？') }}</bk-button
              >
            </div>

            <bk-table
              v-show="isFetched"
              class="file-list-table"
              ref="fileListTable"
              v-bkloading="{ isLoading: tableLoading, opacity: 0.6 }"
              :data="defectList"
              :row-class-name="handleRowClassName"
              :height="screenHeight"
              @row-click="handleFileListRowClick"
              @sort-change="handleSortChange"
              @selection-change="handleSelectionChange"
              @select-all="toSelectAll"
            >
              <bk-table-column
                :selectable="handleSelectable"
                type="selection"
                width="50"
                align="center"
              ></bk-table-column>
              <bk-table-column
                :label="$t('位置')"
                min-width="100"
                prop="filePath"
              >
                <template slot-scope="props">
                  <span
                    v-bk-tooltips="{
                      content: `${props.row.filePath}:${props.row.startLines}`,
                      delay: 600,
                    }"
                  >{{
                    `${getFileName(props.row.filePath)}:${
                      props.row.startLines
                    }`
                  }}</span
                  >
                </template>
              </bk-table-column>
              <bk-table-column
                show-overflow-tooltip
                :label="$t('函数名')"
                min-width="100"
                prop="functionName"
              >
                <template slot-scope="props">
                  <span>{{ props.row.functionName }}</span></template
                >
              </bk-table-column>
              <bk-table-column
                :label="$t('圈复杂度')"
                prop="ccn"
                sortable="custom"
              ></bk-table-column>
              <bk-table-column
                show-overflow-tooltip
                :label="$t('处理人')"
                prop="author"
              >
                <template slot-scope="props">
                  <div
                    @mouseenter="handleAuthorIndex(props.$index)"
                    @mouseleave="handleAuthorIndex(-1)"
                    @click.stop="
                      handleAuthor(
                        1,
                        props.row.entityId,
                        props.row.author,
                        props.row.defectId
                      )
                    "
                  >
                    <!-- <span>{{props.row.authorList && props.row.authorList.join(';')}}</span> -->
                    <bk-user-display-name :user-id="props.row.author"></bk-user-display-name>
                    <span
                      v-if="hoverAuthorIndex === props.$index"
                      class="bk-icon icon-edit2 fs18"
                    ></span>
                  </div>
                </template>
              </bk-table-column>
              <bk-table-column :label="$t('风险')" width="70" prop="riskFactor">
                <template slot-scope="props">
                  <span
                    :class="`color-${
                      { 1: 'major', 2: 'minor', 4: 'info' }[
                        props.row.riskFactor
                      ]
                    }`"
                  >{{ defectSeverityMap[props.row.riskFactor] }}</span
                  >
                </template>
              </bk-table-column>
              <bk-table-column
                prop="latestDateTime"
                sortable="custom"
                :label="$t('代码提交')"
              >
                <template slot-scope="props">
                  <span>{{
                    props.row.latestDateTime | formatDate('date')
                  }}</span>
                </template>
              </bk-table-column>
              <bk-table-column
                :label="$t('首次发现')"
                prop="createBuildNumber"
                sortable="custom"
                width="100"
              >
                <template slot-scope="props">
                  <span>{{
                    props.row.createBuildNumber
                      ? '#' + props.row.createBuildNumber
                      : '--'
                  }}</span>
                </template>
              </bk-table-column>
              <bk-table-column
                :label="$t('最新状态')"
                prop="status"
                width="125"
              >
                <template slot-scope="props">
                  <span class="mr5">{{
                    handleStatus(props.row.status, props.row.ignoreReasonType)
                  }}</span>
                  <span>
                    <span
                      v-if="props.row.status === 1 && props.row.mark === 1"
                      v-bk-tooltips="$t('已标记处理')"
                      class="codecc-icon icon-mark mr5"
                    ></span>
                    <span
                      v-if="props.row.status === 1 && props.row.markButNoFixed"
                      v-bk-tooltips="$t('标记处理后重新扫描仍为问题')"
                      class="codecc-icon icon-mark re-mark mr5"
                    ></span>
                    <span
                      v-if="
                        props.row.defectIssueInfoVO &&
                          props.row.defectIssueInfoVO.submitStatus &&
                          props.row.defectIssueInfoVO.submitStatus !== 4
                      "
                      v-bk-tooltips="$t('已提单')"
                      class="codecc-icon icon-tapd"
                    ></span>
                  </span>
                </template>
              </bk-table-column>
              <bk-table-column
                show-overflow-tooltip
                v-if="isProjectDefect"
                :label="$t('任务')"
                prop="task"
              >
                <template slot-scope="props">
                  <span
                    class="cc-link"
                    @click.stop="goToTask(props.row.taskId)"
                  >{{ props.row.taskNameCn || '--' }}</span
                  >
                </template>
              </bk-table-column>
              <bk-table-column :label="$t('操作')" width="60">
                <template slot-scope="props">
                  <!-- 已修复问题没有这些操作 -->
                  <span
                    v-if="!(props.row.status & 2)"
                    class="cc-operate-more"
                    @click.prevent.stop
                  >
                    <bk-popover
                      theme="light"
                      placement="bottom"
                      trigger="click"
                    >
                      <span class="bk-icon icon-more guide-icon"></span>
                      <div slot="content" class="handle-menu-tips txal">
                        <!-- 待修复问题的操作 -->
                        <template v-if="props.row.status === 1">
                          <p
                            v-if="props.row.mark"
                            class="entry-link"
                            @click.stop="
                              handleMark(0, false, props.row.entityId)
                            "
                          >
                            {{ $t('取消标记') }}
                          </p>
                          <p
                            v-else
                            class="entry-link"
                            @click.stop="
                              handleMark(1, false, props.row.entityId)
                            "
                          >
                            {{ $t('标记处理') }}
                          </p>
                        </template>
                        <!-- 已忽略问题的操作 -->
                        <p
                          v-if="
                            props.row.status & 4 &&
                              !props.row.ignoreCommentDefect
                          "
                          class="entry-link"
                          @click.stop="
                            handleRevertIgnoreAndMark(props.row.entityId)
                          "
                        >
                          {{ $t('取消忽略并标记处理') }}
                        </p>
                        <p
                          v-if="
                            props.row.status & 4 &&
                              !props.row.ignoreCommentDefect
                              && isInnerSite
                          "
                          class="entry-link"
                          @click.stop="
                            handleRevertIgnoreAndCommit(props.row.entityId)
                          "
                        >
                          {{ $t('取消忽略并提单') }}
                        </p>
                        <p
                          v-if="
                            props.row.status & 4 &&
                              props.row.ignoreCommentDefect
                          "
                          class="disabled"
                          :title="$t('注释忽略的问题不允许页面进行恢复操作')"
                        >
                          {{ $t('取消忽略') }}
                        </p>
                        <p
                          v-else-if="props.row.status & 4"
                          class="entry-link"
                          @click.stop="
                            handleIgnore(
                              'RevertIgnore',
                              false,
                              props.row.entityId
                            )
                          "
                        >
                          {{ $t('取消忽略') }}
                        </p>
                        <p
                          v-else-if="taskDetail.prohibitIgnore"
                          class="entry-link disabled"
                          :title="
                            $t(
                              '已设置禁止页面忽略，可在代码行末或上一行使用注释忽略，例如// NOCC:rule1(ignore reason)'
                            )
                          "
                        >
                          {{ $t('忽略函数') }}
                        </p>
                        <bk-popover
                          v-else
                          ref="guidePopover2"
                          placement="left"
                          theme="dot-menu light"
                          trigger="click"
                        >
                          <div>
                            <span class="guide-flag"></span>
                            <span
                              class="entry-link ignore-item"
                              @click.stop="
                                handleIgnore(
                                  'IgnoreDefect',
                                  false,
                                  props.row.entityId
                                )
                              "
                            >{{ $t('忽略函数') }}</span
                            >
                          </div>
                          <div class="guide-content" slot="content">
                            <div :style="lineHeight">
                              {{ $t('支持忽略无需处理或暂缓处理的问题。') }}
                              {{ $t('针对特定忽略类型可') }}
                              <span
                                theme="primary"
                                class="set-tips"
                                @click="handleTableSetReview"
                              >{{ $t('设置提醒') }}</span
                              >
                              {{ $t('，以便定期review和修复。') }}
                            </div>
                            <div class="guide-btn">
                              <span
                                class="btn-item"
                                @click="handleTableGuideNextStep"
                              >{{ $t('我知道了') }}</span
                              >
                            </div>
                          </div>
                        </bk-popover>
                        <p
                          v-if="
                            props.row.status & 4 &&
                              !props.row.ignoreCommentDefect
                          "
                          class="entry-link"
                          @click.stop="handleChangeIgnoreType(props.row)"
                        >
                          {{ $t('修改忽略类型') }}
                        </p>
                        <p
                          v-if="
                            props.row.status === 1 &&
                              !(
                                props.row.defectIssueInfoVO &&
                                props.row.defectIssueInfoVO.submitStatus &&
                                props.row.defectIssueInfoVO.submitStatus !== 4
                              )
                              && isInnerSite
                          "
                          class="entry-link"
                          @click.stop="
                            handleCommit('commit', false, props.row.entityId)
                          "
                        >
                          {{ $t('提单') }}
                        </p>
                      </div>
                    </bk-popover>
                  </span>
                </template>
              </bk-table-column>
              <div slot="append" v-show="isFileListLoadMore">
                <div class="table-append-loading">
                  {{
                    $t('正在加载第x-y个，请稍后···', {
                      x: nextPageStartNum,
                      y: nextPageEndNum,
                    })
                  }}
                </div>
              </div>
              <div slot="empty">
                <div class="codecc-table-empty-text">
                  <img src="../../images/empty.png" class="empty-img" />
                  <div>{{ $t('没有查询到数据') }}</div>
                </div>
              </div>
            </bk-table>
          </div>

          <bk-dialog
            v-model="defectDetailDialogVisible"
            ext-cls="file-detail-dialog"
            :fullscreen="isFullScreen"
            :position="{ top: `${isFullScreen ? 0 : 50}` }"
            :draggable="false"
            :show-footer="false"
            :close-icon="true"
            width="80%"
          >
            <div :class="['code-fullscreen', { 'full-active': isFullScreen }]">
              <i
                class="bk-icon toggle-full-icon"
                :class="
                  isFullScreen ? 'icon-un-full-screen' : 'icon-full-screen'
                "
                @click="isFullScreen = !isFullScreen"
              ></i>
              <div class="col-main">
                <div class="file-bar">
                  <div class="filemeta" v-if="currentLintFile">
                    <b class="filename" :title="currentLintFile.filePath">{{
                      `${lintDetail.fileName}:${currentLintFile.startLines}`
                    }}</b>
                    <span
                      :title="$t('复制问题所在文件位置')"
                      class="copy-icon"
                      @click.stop="copy(`${lintDetail.fileName}:${currentLintFile.startLines}`)">
                      <i class="codecc-icon icon-copy-line" style="font-size: 15px"></i>
                    </span>
                    <!-- <div class="filepath" :title="currentLintFile.filePath">{{$t('文件路径')}}：{{currentLintFile.filePath}}</div> -->
                    <bk-button
                      class="fr mr10"
                      theme="primary"
                      @click="scrollIntoView()"
                    >{{ $t('函数位置') }}</bk-button
                    >
                  </div>
                </div>
                <div
                  id="codeViewerInDialog"
                  :class="
                    isFullScreen ? 'full-code-viewer' : 'un-full-code-viewer'
                  "
                  @click="handleCodeViewerInDialogClick"
                ></div>
              </div>
              <div class="col-aside">
                <div class="operate-section">
                  <dl
                    class="basic-info"
                    :class="{ 'full-screen-info': isFullScreen }"
                    v-if="currentLintFile"
                  >
                    <div class="block">
                      <div class="item">
                        <span
                          class="fail mr5"
                          v-if="currentLintFile.status === 1"
                        >
                          <span class="cc-dot"></span>
                          <span
                            v-if="buildNum"
                            v-bk-tooltips="
                              `#${buildNum}待修复(当前分支最新构建#${
                                lintDetail.lastBuildNumOfSameBranch
                              }该问题为${
                                lintDetail.defectIsFixedOnLastBuildNumOfSameBranch
                                  ? '已修复'
                                  : '待修复'
                              })`
                            "
                          >
                            #{{ buildNum }}{{ $t('待修复') }}
                            <span style="color: #63656e"
                            >(#{{ lintDetail.lastBuildNumOfSameBranch
                            }}{{
                              lintDetail.defectIsFixedOnLastBuildNumOfSameBranch
                                ? $t('已修复')
                                : $t('待修复')
                            }})</span
                            >
                          </span>
                          <span v-else>{{ $t('待修复') }}</span>
                          <span
                            v-if="
                              currentLintFile.defectIssueInfoVO &&
                                currentLintFile.defectIssueInfoVO.submitStatus &&
                                currentLintFile.defectIssueInfoVO.submitStatus !==
                                4
                            "
                          >{{ $t('(已提单)') }}</span
                          >
                        </span>
                        <span
                          class="success mr5"
                          v-else-if="currentLintFile.status & 2"
                        ><span class="cc-dot"></span>{{ $t('已修复') }}</span
                        >
                        <span
                          class="warn mr5"
                          v-else-if="currentLintFile.status & 4"
                        ><span class="cc-dot"></span>{{ $t('已忽略') }}</span
                        >
                        <span>
                          <span
                            v-if="
                              currentLintFile.status === 1 &&
                                currentLintFile.mark === 1
                            "
                            v-bk-tooltips="$t('已标记处理')"
                            class="codecc-icon icon-mark mr5"
                          ></span>
                          <span
                            v-if="
                              currentLintFile.status === 1 &&
                                currentLintFile.markButNoFixed
                            "
                            v-bk-tooltips="$t('标记处理后重新扫描仍为问题')"
                            class="codecc-icon icon-mark re-mark mr5"
                          ></span>
                          <span
                            v-if="
                              currentLintFile.defectIssueInfoVO &&
                                currentLintFile.defectIssueInfoVO.submitStatus &&
                                currentLintFile.defectIssueInfoVO.submitStatus !==
                                4
                            "
                            v-bk-tooltips="$t('已提单')"
                            class="codecc-icon icon-tapd"
                          ></span>
                        </span>
                      </div>
                      <div v-if="currentLintFile.status === 1" class="item">
                        <bk-button
                          v-if="currentLintFile.mark"
                          class="item-button"
                          @click="
                            handleMark(0, false, currentLintFile.entityId)
                          "
                        >
                          {{ $t('取消标记') }}
                        </bk-button>
                        <bk-button
                          v-else
                          theme="primary"
                          class="item-button"
                          @click="
                            handleMark(1, false, currentLintFile.entityId)
                          "
                        >
                          {{ $t('标记处理') }}
                        </bk-button>
                      </div>
                      <div
                        v-if="
                          currentLintFile.status & 4 &&
                            !currentLintFile.ignoreCommentDefect
                        "
                      >
                        <div class="item">
                          <bk-button
                            class="item-button"
                            @click="
                              handleRevertIgnoreAndMark(
                                currentLintFile.entityId
                              )
                            "
                          >
                            {{ $t('取消忽略并标记处理') }}
                          </bk-button>
                        </div>
                        <div class="item" v-if="isInnerSite">
                          <bk-button
                            class="item-button"
                            @click="
                              handleRevertIgnoreAndCommit(
                                currentLintFile.entityId
                              )
                            "
                          >
                            {{ $t('取消忽略并提单') }}
                          </bk-button>
                        </div>
                      </div>
                      <div class="item">
                        <bk-button
                          v-if="
                            currentLintFile.status & 4 &&
                              currentLintFile.ignoreCommentDefect
                          "
                          class="item-button"
                          disabled
                          :title="$t('注释忽略的问题不允许页面进行恢复操作')"
                        >
                          {{ $t('取消忽略') }}
                        </bk-button>
                        <bk-button
                          v-else-if="currentLintFile.status & 4"
                          class="item-button"
                          @click="
                            handleIgnore(
                              'RevertIgnore',
                              false,
                              currentLintFile.entityId
                            )
                          "
                        >
                          {{ $t('取消忽略') }}
                        </bk-button>
                        <bk-button
                          v-else-if="taskDetail.prohibitIgnore"
                          disabled
                          class="item-button"
                          :title="
                            $t(
                              '已设置禁止页面忽略，可在代码行末或上一行使用注释忽略，例如// NOCC:rule1(ignore reason)'
                            )
                          "
                        >
                          {{ $t('忽略函数') }}
                        </bk-button>
                        <bk-button
                          v-else-if="!(currentLintFile.status & 2)"
                          class="item-button"
                          @click="
                            handleIgnore(
                              'IgnoreDefect',
                              false,
                              currentLintFile.entityId
                            )
                          "
                        >
                          {{ $t('忽略函数') }}
                        </bk-button>
                      </div>
                      <div class="item">
                        <bk-button
                          class="item-button"
                          @click="handleComment(currentLintFile.entityId)"
                        >
                          {{ $t('评论') }}
                        </bk-button>
                      </div>
                      <div
                        class="item"
                        v-if="
                          currentLintFile.status & 4 &&
                            !currentLintFile.ignoreCommentDefect
                        "
                      >
                        <bk-button
                          class="item-button"
                          @click="handleChangeIgnoreType(currentLintFile)"
                        >
                          {{ $t('修改忽略类型') }}
                        </bk-button>
                      </div>
                      <div
                        class="item"
                        v-if="
                          currentLintFile.status === 1 &&
                            !(
                              currentLintFile.defectIssueInfoVO &&
                              currentLintFile.defectIssueInfoVO.submitStatus &&
                              currentLintFile.defectIssueInfoVO.submitStatus !== 4
                            )
                            && isInnerSite
                        "
                      >
                        <bk-button
                          class="item-button"
                          @click="
                            handleCommit(
                              'commit',
                              false,
                              currentLintFile.entityId
                            )
                          "
                        >
                          {{ $t('提单') }}
                        </bk-button>
                      </div>
                    </div>
                    <div class="block">
                      <div class="item">
                        <dt>ID</dt>
                        <dd>{{ currentLintFile.id }}</dd>
                      </div>
                      <div class="item">
                        <dt>{{ $t('级别') }}</dt>
                        <dd>
                          {{ defectSeverityMap[currentLintFile.riskFactor] }}
                        </dd>
                      </div>
                    </div>
                    <div class="block">
                      <div class="item">
                        <dt>{{ $t('问题创建') }}</dt>
                        <dd class="small">
                          {{ currentLintFile.createTime | formatDate }} {{
                            currentLintFile.createBuildNumber
                              ? '#' + currentLintFile.createBuildNumber
                              : '--'
                          }}
                        </dd>
                      </div>
                      <div class="item">
                        <dt>
                          {{ $t('处理人') }}
                        </dt>
                        <dd>
                          <bk-user-display-name :user-id="currentLintFile.author"></bk-user-display-name>
                          <span
                            v-if="
                              currentLintFile.status & 1 ||
                                currentLintFile.status & 4
                            "
                            @click.stop="
                              handleAuthor(
                                1,
                                currentLintFile.entityId,
                                currentLintFile.author,
                                currentLintFile.defectId
                              )
                            "
                            class="curpt bk-icon icon-edit2 fs20"
                          >
                          </span>
                        </dd>
                      </div>
                      <div class="item" v-if="currentLintFile.status & 2">
                        <dt>{{ $t('修复时间') }}</dt>
                        <dd class="small">
                          {{ currentLintFile.fixedTime | formatDate }}
                        </dd>
                      </div>
                      <div class="item">
                        <dt>{{ $t('代码提交') }}</dt>
                        <dd class="small">
                          {{ currentLintFile.latestDateTime | formatDate }}
                        </dd>
                      </div>
                      <div class="item">
                        <dt>{{ $t('提交人') }}</dt>
                        <bk-user-display-name :user-id="currentLintFile.commitAuthor"></bk-user-display-name>
                      </div>
                    </div>
                    <div class="block" v-if="currentLintFile.status & 4">
                      <div class="item">
                        <dt>{{ $t('忽略时间') }}</dt>
                        <dd class="small">
                          {{ currentLintFile.ignoreTime | formatDate }}
                        </dd>
                      </div>
                      <div class="item">
                        <dt>{{ $t('忽略人') }}</dt>
                        <bk-user-display-name :user-id="currentLintFile.ignoreAuthor"></bk-user-display-name>
                      </div>
                      <div class="item disb">
                        <dt>{{ $t('忽略原因') }}</dt>
                        <dd>
                          {{
                            getIgnoreReasonByType(
                              currentLintFile.ignoreReasonType
                            )
                          }}
                          {{
                            currentLintFile.ignoreReason
                              ? '：' + currentLintFile.ignoreReason
                              : ''
                          }}
                        </dd>
                      </div>
                    </div>
                    <div class="block">
                      <div class="item">
                        <dt>{{ $t('函数名') }}</dt>
                        <dd>{{ currentLintFile.functionName }}</dd>
                      </div>
                      <div class="item">
                        <dt>{{ $t('圈复杂度') }}</dt>
                        <dd>{{ currentLintFile.ccn }}</dd>
                      </div>
                      <div class="item">
                        <dt>{{ $t('函数总行数') }}</dt>
                        <dd>{{ currentLintFile.totalLines }}</dd>
                      </div>
                      <div class="item">
                        <dt>{{ $t('函数起始行') }}</dt>
                        <dd>{{ currentLintFile.startLines }}</dd>
                      </div>
                    </div>
                    <div class="block">
                      <div class="item disb">
                        <dt>{{ $t('代码库路径') }}</dt>
                        <a target="_blank" :href="lintDetail.filePath">{{
                          lintDetail.filePath
                        }}</a>
                      </div>
                      <div class="item disb">
                        <dt>{{ $t('版本号') }}</dt>
                        <dd>{{ currentLintFile.revision }}</dd>
                      </div>
                    </div>
                    <!-- <div class="block">
                      <div class="item ignore">
                        <dt>{{$t('忽略问题')}}</dt>
                        <dd>{{$t('在函数头或函数内添加')}} // #lizard forgives</dd>
                      </div>
                    </div> -->
                  </dl>
                  <!-- <div class="toggle-file">
                    <bk-button theme="primary" style="width:200px;" @click="scrollIntoView">{{$t('回到函数位置')}}</bk-button>
                  </div> -->
                  <div class="toggle-file">
                    <bk-button
                      :disabled="fileIndex - 1 < 0"
                      @click="handleFileListRowClick(lintFileList[--fileIndex])"
                    >{{ $t('上一函数') }}</bk-button
                    >
                    <bk-button
                      :disabled="fileIndex + 1 >= totalCount"
                      @click="handleFileListRowClick(lintFileList[++fileIndex])"
                    >{{ $t('下一函数') }}</bk-button
                    >
                  </div>
                </div>
              </div>
            </div>
          </bk-dialog>
        </div>
      </div>
      <bk-dialog
        v-model="authorEditDialogVisible"
        width="560"
        theme="primary"
        header-position="left"
        :title="
          operateParams.changeAuthorType === 1
            ? $t('修改问题处理人')
            : $t('批量修改问题处理人')
        "
        :before-close="handleBeforeClose"
      >
        <div class="author-edit">
          <div class="tips" v-if="operateParams.changeAuthorType === 3">
            <i class="bk-icon icon-info-circle"></i
            >{{ $t('原处理人所有函数都将转给新处理人') }}
          </div>
          <bk-form
            :model="operateParams"
            :label-width="130"
            class="search-form"
          >
            <bk-form-item
              v-if="operateParams.changeAuthorType !== 2"
              property="sourceAuthor"
              :label="$t('原处理人')"
            >
              <bk-user-display-name :user-id="operateParams.sourceAuthor"></bk-user-display-name>
            </bk-form-item>
            <bk-form-item :label="$t('新处理人')">
              <UserSelector
                allow-create
                :value.sync="operateParams.targetAuthor"
                style="width: 290px"
              />
            </bk-form-item>
          </bk-form>
        </div>
        <div class="footer-wrapper" slot="footer">
          <bk-button
            type="button"
            theme="primary"
            :disabled="
              (operateParams.changeAuthorType === 3 &&
                !operateParams.sourceAuthor) ||
                !operateParams.targetAuthor.length
            "
            :loading="authorEditDialogLoading"
            @click.native="handleAuthorEditConfirm"
          >
            {{
              operateParams.changeAuthorType === 1 ? $t('确定') : $t('批量修改')
            }}
          </bk-button>
          <bk-button
            theme="primary"
            type="button"
            :disabled="authorEditDialogLoading"
            @click.native="handleCloseAuthorDialog"
          >
            {{ $t('取消') }}
          </bk-button>
        </div>
      </bk-dialog>
      <bk-dialog
        v-model="ignoreReasonDialogVisible"
        width="560"
        :position="ignoreDialogPositionConfig"
        theme="primary"
        header-position="left"
        :title="ignoreReasonDialogTitle"
        :before-close="handleBeforeClose"
        @cancel="handleIgnoreCancel"
      >
        <div class="reason-type-list">
          <div class="reason-type-header mb20">
            {{ $t('忽略类型') }}
            <span class="fr">
              <bk-button
                size="small"
                icon="plus"
                class="mr10"
                @click="handleSetReview"
              >{{ $t('新增类型') }}</bk-button
              >
              <bk-button size="small" @click="handelFetchIgnoreList"
              ><i class="codecc-icon icon-refresh-2"></i
              ></bk-button>
            </span>
          </div>
          <bk-form :model="operateParams" :label-width="0" class="search-form">
            <bk-form-item property="ignoreReason">
              <bk-radio-group
                v-model="operateParams.ignoreReasonType"
                class="ignore-list"
              >
                <bk-radio
                  class="cc-radio"
                  v-for="ignore in ignoreList"
                  :key="ignore.ignoreTypeId"
                  :value="ignore.ignoreTypeId"
                >
                  {{ ignore.name }}
                  <span v-if="ignore.ignoreTypeId === 42 && !isProjectDefect">
                    <a class="ml15" @click.stop="handleToPathShield">
                      {{ $t('按代码路径屏蔽') }}
                      <i class="codecc-icon icon-link"></i>
                    </a>
                  </span>
                  <span
                    v-else-if="
                      operateParams.ignoreReasonType === ignore.ignoreTypeId &&
                        ignore.notify.notifyDayOfWeeks.length
                    "
                    class="notify-tips"
                  >
                    <span
                      class="f12"
                      v-bk-tooltips="{
                        content:
                          formatTime(ignore.nextNotifyTime, 'M月d日') +
                          '（' +
                          handleGetNotifyDate(ignore.notify) +
                          '）' +
                          $t('提醒'),
                      }"
                    >
                      <i
                        class="codecc-icon icon-time"
                        style="color: #979ba5"
                      ></i>
                      {{ formatTime(ignore.nextNotifyTime, 'M月d日') }} （{{
                        handleGetNotifyDate(ignore.notify)
                      }}） {{ $t('提醒') }}
                    </span>
                  </span>
                </bk-radio>
              </bk-radio-group>
            </bk-form-item>
            <bk-form-item
              property="ignoreReason"
              :required="ignoreReasonRequired"
            >
              <span>{{ $t('忽略原因') }}</span>
              <bk-input
                :type="'textarea'"
                :maxlength="255"
                v-model="operateParams.ignoreReason"
              ></bk-input>
            </bk-form-item>
          </bk-form>
        </div>
        <div class="footer-wrapper" slot="footer">
          <bk-button
            theme="primary"
            :disabled="ignoreReasonAble"
            @click.native="handleIgnoreConfirm"
          >
            {{ $t('确定') }}
          </bk-button>
          <bk-button theme="primary" @click.native="handleIgnoreCancel">
            {{ $t('取消') }}
          </bk-button>
        </div>
      </bk-dialog>
      <bk-dialog
        v-model="commentDialogVisible"
        width="560"
        theme="primary"
        header-position="left"
        :before-close="handleBeforeClose"
        :title="$t('评论')"
      >
        <div class="pd10 pr50">
          <bk-form :model="commentParams" :label-width="30" class="search-form">
            <bk-form-item property="comment" :required="true">
              <bk-input
                :placeholder="$t('请输入你的评论内容')"
                :type="'textarea'"
                :maxlength="200"
                v-model="commentParams.comment"
              ></bk-input>
            </bk-form-item>
          </bk-form>
        </div>
        <div class="footer-wrapper" slot="footer">
          <bk-button
            theme="primary"
            :disabled="!commentParams.comment"
            @click.native="handleCommentConfirm"
          >
            {{ $t('确定') }}
          </bk-button>
          <bk-button
            theme="primary"
            @click.native="commentDialogVisible = false"
          >
            {{ $t('取消') }}
          </bk-button>
        </div>
      </bk-dialog>
      <bk-dialog
        v-model="operateDialogVisible"
        width="640"
        theme="primary"
        :position="{ top: 50, left: 5 }"
        :title="$t('现已支持键盘操作，提升操作效率')"
      >
        <div class="operate-txt operate-txt-1">1. {{ $t('列表') }}</div>
        <div>
          <img
            v-if="isEn"
            style="width: 592px"
            src="../../images/operate-1-en.png"
          />
          <img v-else style="width: 592px" src="../../images/operate-1.png" />
        </div>
        <div class="operate-txt operate-txt-2">2. {{ $t('问题详情') }}</div>
        <div>
          <img
            v-if="isEn"
            style="width: 592px"
            src="../../images/operate-2-en.png"
          />
          <img v-else style="width: 592px" src="../../images/operate-2.png" />
        </div>
        <div class="operate-footer" slot="footer">
          <bk-button
            theme="primary"
            @click.native="operateDialogVisible = false"
          >
            {{ $t('关闭') }}
          </bk-button>
        </div>
      </bk-dialog>
      <bk-dialog
        v-model="emptyDialogVisible"
        :theme="'primary'"
        :ok-text="$t('我知道了')"
      >
        {{
          $t(
            '当前项目由API创建，CodeCC任务数会十分庞大，暂不支持查看任务列表和问题列表。'
          )
        }}
      </bk-dialog>
    </div>
    <div class="ccn-list" v-else>
      <div class="main-container large border-none">
        <div class="no-task">
          <empty
            title=""
            :desc="
              $t(
                'CodeCC集成了圈复杂度工具，可以检测过于复杂的代码，复杂度越高代码存在缺陷的风险越大'
              )
            "
          >
            <template #action>
              <bk-button
                size="large"
                theme="primary"
                @click="addTool({ from: 'CCN' })"
              >{{ $t('配置规则集') }}</bk-button
              >
            </template>
          </empty>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="jsx">
import _ from 'lodash';
import { mapState } from 'vuex';
import { bus } from '@/common/bus';
import { getClosest, toggleClass, formatDiff, hasClass } from '@/common/util';
import util from '@/mixins/defect-list';
import displayNameTagInputTpl from '@/mixins/display-name-tag-input-tpl';
import CodeMirror from '@/common/codemirror';
import Empty from '@/components/empty';
import filterSearchOption from './filter-search-option';
import { format } from 'date-fns';
import DefectPanel from './components/defect-panel.vue';
// eslint-disable-next-line
import { export_json_to_excel } from '@/vendor/export2Excel';
import { language } from '../../i18n';
import DatePicker from '@/components/date-picker/index.vue';
import DEPLOY_ENV from '@/constants/env';
import UserSelector from '@/components/user-selector/index.vue';
import SelectTrigger from '@/components/select-trigger/index.vue';

// 搜索过滤项缓存
const CCN_SEARCH_OPTION_CACHE = 'search_option_columns_cnn';

export default {
  components: {
    DatePicker,
    Empty,
    filterSearchOption,
    DefectPanel,
    UserSelector,
    SelectTrigger,
  },
  mixins: [util, displayNameTagInputTpl],
  data() {
    const isProjectDefect = this.$route.name === 'project-ccn-list';
    this.getDefaultOption = () => (isProjectDefect
      ? [
        { id: 'task', name: this.$t('任务'), isChecked: true },
        { id: 'author', name: this.$t('处理人'), isChecked: true },
      ]
      : [
        { id: 'author', name: this.$t('处理人'), isChecked: true },
        { id: 'filePath', name: this.$t('文件路径'), isChecked: true },
        { id: 'buildId', name: this.$t('快照'), isChecked: true },
      ]);

    this.getCustomOption = function (val) {
      return [
        { id: 'daterange', name: this.$t('日期'), isChecked: val },
        { id: 'status', name: this.$t('状态'), isChecked: val },
        { id: 'severity', name: this.$t('风险级别'), isChecked: val },
      ];
    };

    const { query } = this.$route;
    const { taskId } = this.$route.params;
    let status = [1];
    let statusUnion = [1]; // 状态和忽略类型组合的新字段
    if (query.status) {
      status = this.numToArray(query.status);
      statusUnion = status.slice();
    }
    let ignoreReasonTypes = [];
    if (query.ignoreTypeId) {
      ignoreReasonTypes = query.ignoreTypeId.split(',').map(i => Number(i));
      const statusIndex = statusUnion.findIndex(item => item === 4);
      if (statusIndex !== -1) {
        statusUnion.splice(statusIndex, 1);
      }
      const typesArr = ignoreReasonTypes.map(item => `4-${item}`);
      statusUnion = statusUnion.concat(typesArr);
    }

    return {
      contentLoading: false,
      isInnerSite: DEPLOY_ENV === 'tencent',
      panels: [
        { name: 'defect', label: this.$t('风险函数') },
        { name: 'report', label: this.$t('数据报表') },
      ],
      defectSeverityMap: {
        1: this.$t('极高'),
        2: this.$t('高'),
        4: this.$t('中'),
        8: this.$t('低'),
      },
      toolId: 'CCN',
      listData: {
        defectList: {
          content: [],
          totalElements: 0,
        },
      },
      lintDetail: {},
      searchFormData: {
        checkerList: [],
        authorList: [],
        filePathTree: {},
        filePathShow: this.handleFileList(query.fileList).join(';'),
        superHighCount: 0,
        highCount: 0,
        mediumCount: 0,
        lowCount: 0,
      },
      statusFormData: {
        existCount: 0,
        fixCount: 0,
        ignoreCount: 0,
        maskCount: 0,
      },
      operateParams: {
        toolName: 'CCN',
        dimension: 'CCN',
        ignoreReasonType: '',
        ignoreReason: '',
        changeAuthorType: 1, // 1:单个修改处理人，2:批量修改处理人，3:固定修改处理人
        sourceAuthor: [],
        targetAuthor: [],
      },
      searchParams: {
        taskId: this.$route.params.taskId,
        toolName: 'CCN',
        toolNameList: ['CCN'],
        checker: query.checker || '',
        author: query.author,
        severity: this.numToArray(query.severity),
        status,
        statusUnion,
        buildId: query.buildId ? query.buildId : '',
        fileList: this.handleFileList(query.fileList),
        daterange: [query.startTime, query.endTime],
        sortField: query.sortField || 'ccn',
        sortType: 'DESC',
        ignoreReasonTypes,
        pageNum: 1,
        pageSize: 50,
        showTaskNameCn: isProjectDefect,
        authorList: query.author ? [query.author] : [],
      },
      defectDetailSearchParams: {
        sortField: '',
        sortType: '',
        pattern: '',
        filePath: '',
        entityId: undefined,
      },
      codeViewerInDialog: null,
      isFilePathDropdownShow: false,
      isFileListLoadMore: false,
      isDefectListLoadMore: false,
      defectDetailDialogVisible: false,
      authorEditDialogVisible: false,
      ignoreReasonDialogVisible: false,
      commentDialogVisible: false,
      isBatchOperationShow: false,
      isFullScreen: false,
      defectComment: '',
      pagination: {
        current: 1,
        count: 1,
        limit: 50,
      },
      totalCount: 0,
      fileIndex: 0,
      codeMirrorDefaultCfg: {
        lineNumbers: true,
        scrollbarStyle: 'simple',
        theme: 'summerfruit',
        lineWrapping: true,
        placeholder: this.emptyText,
        firstLineNumber: 1,
        readOnly: true,
      },
      show: false,
      searchInput: '',
      emptyText: this.$t('未选择文件'),
      newDefectJudgeTime: '',
      buildList: [],
      tableLoading: false,
      isSearchDropdown: true,
      lineAverageOpt: 10,
      screenHeight: 336,
      selectedLen: 0,
      isSelectAll: '',
      isSearch: false,
      hoverAuthorIndex: -1,
      commentList: [],
      commentParams: {
        fileId: '',
        toolName: 'CCN',
        defectId: '',
        commentId: '',
        singleCommentId: '',
        userName: this.$store.state.user.username,
        comment: '',
      },
      operateDialogVisible: false,
      isFetched: false,
      currentLintFile: {},
      exportLoading: false,
      ccnThreshold: 20,
      defaultOption: this.getDefaultOption(),
      customOption: this.getCustomOption(true),
      selectedOptionColumn: [],
      ignoreList: [], // 忽略类型列表,
      isAddingIgnore: false,
      monthsStrMap: {
        1: this.$t('一月'),
        2: this.$t('二月'),
        3: this.$t('三月'),
        4: this.$t('四月'),
        5: this.$t('五月'),
        6: this.$t('六月'),
        7: this.$t('七月'),
        8: this.$t('八月'),
        9: this.$t('九月'),
        10: this.$t('十月'),
        11: this.$t('十一月'),
        12: this.$t('十二月'),
      },
      weekOfMonthsStrMap: {
        1: this.$t('第一个星期'),
        2: this.$t('第二个星期'),
        3: this.$t('第三个星期'),
        4: this.$t('第四个星期'),
        5: this.$t('第五个星期'),
      },
      dayOfWeekMap: {
        1: this.$t('星期一'),
        2: this.$t('星期二'),
        3: this.$t('星期三'),
        4: this.$t('星期四'),
        5: this.$t('星期五'),
        6: this.$t('星期六'),
        7: this.$t('星期日'),
      },
      isRowChangeIgnoreType: false,
      toolName: 'CCN',
      toolNameList: ['CCN'],
      dimensionList: ['CCN'],
      guideFlag: false,
      hasCountData: false,
      hasIgnoreList: false,
      statusTreeKey: 1,
      isProjectDefect,
      taskIdList: isProjectDefect ? [] : [Number(taskId)],
      emptyDialogVisible: false,
      dateType: query.dateType || 'createTime',
    };
  },
  computed: {
    ...mapState('tool', {
      toolMap: 'mapList',
    }),
    ...mapState('task', {
      taskDetail: 'detail',
    }),
    ...mapState('project', {
      projectVisitable: 'visitable',
    }),
    ...mapState('displayname', {
      usersMap: 'usersMap',
    }),
    visitable() {
      return this.projectVisitable || !this.isProjectDefect;
    },
    projectId() {
      return this.$route.params.projectId;
    },
    taskId() {
      return this.$route.params.taskId;
    },
    userName() {
      return this.$store.state.user.username;
    },
    typeTips() {
      return this.$t('起始时间x之后产生的函数为新函数', {
        accessTime: this.newDefectJudgeTime,
      });
    },
    breadcrumb() {
      const { toolId } = this;
      let toolDisplayName = (this.toolMap[toolId] || {}).displayName || '';
      const names = [this.$route.meta.title || this.$t('风险函数')];
      if (toolDisplayName) {
        toolDisplayName = this.$t(`${toolDisplayName}`);
        names.unshift(toolDisplayName);
      }

      return { name: names.join(' / ') };
    },
    lintFileList() {
      return this.listData.defectList.content;
    },
    // currentLintFile () {
    //     return this.lintFileList[this.fileIndex]
    // },
    defectList() {
      this.setTableHeight();
      return this.listData.defectList.content;
    },
    newDefectCount() {
      const { newDefectCount } = this.listData;
      return newDefectCount > 100000 ? this.$t('10万+') : newDefectCount;
    },
    historyDefectCount() {
      const { historyDefectCount } = this.listData;
      return historyDefectCount > 100000
        ? this.$t('10万+')
        : historyDefectCount;
    },
    statusTreeData() {
      const { isProjectDefect } = this;
      const { existCount, fixCount, ignoreCount, maskCount } = this.statusFormData;
      const list = this.ignoreList.map(item => ({
        id: `4-${item.ignoreTypeId}`,
        name: `${this.$t('已忽略')}-${item.name}`,
      }));
      const statusList = [
        {
          id: 1,
          name: `${this.$t('待修复')}${
            isProjectDefect ? '' : `(${existCount || 0})`
          }`,
        },
        {
          id: 2,
          name: `${this.$t('已修复')}${
            isProjectDefect ? '' : `(${fixCount || 0})`
          }`,
        },
        {
          id: 4,
          name: `${this.$t('已忽略')}${
            isProjectDefect ? '' : `(${ignoreCount || 0})`
          }`,
          children: list,
        },
        {
          id: 8,
          name: `${this.$t('已屏蔽')}${
            isProjectDefect ? '' : `(${maskCount || 0})`
          }`,
        },
      ];
      this.statusTreeKey += 1;
      return statusList;
    },
    nextPageStartNum() {
      return (this.searchParams.pageNum - 1) * this.searchParams.pageSize + 1;
    },
    nextPageEndNum() {
      let nextPageEndNum = this.nextPageStartNum + this.searchParams.pageSize - 1;
      nextPageEndNum = this.totalCount < nextPageEndNum ? this.totalCount : nextPageEndNum;
      return nextPageEndNum;
    },
    ignoreReasonRequired() {
      return this.operateParams.ignoreReasonType === 4;
    },
    ignoreReasonAble() {
      if (
        this.operateParams.ignoreReasonType === 4
        && this.operateParams.ignoreReason === ''
      ) {
        return true;
      }
      return !this.operateParams.ignoreReasonType;
    },
    searchParamsWatch() {
      return JSON.parse(JSON.stringify(this.searchParams));
    },
    buildNum() {
      const { buildList } = this;
      const buildItem = buildList.find(item => item.buildId === this.searchParams.buildId)
        || {};
      return buildItem.buildNum;
    },
    allRenderColumnMap() {
      return this.selectedOptionColumn.reduce((result, item) => {
        result[item.id] = item.isChecked;
        return result;
      }, {});
    },
    isMac() {
      return /macintosh|mac os x/i.test(navigator.userAgent);
    },
    ignoreReasonDialogTitle() {
      if (this.isRowChangeIgnoreType) {
        return this.$t('忽略');
      }
      return (this.isSelectAll === 'Y' ? this.totalCount : this.selectedLen) > 1
        ? `${this.$t('忽略')}（
          ${this.$t('共x个问题', {
    num: this.isSelectAll === 'Y' ? this.totalCount : this.selectedLen,
  })}）`
        : this.$t('忽略');
    },
    ignoreDialogPositionConfig() {
      const { clientHeight } = document.body;
      const config = {
        top: '200',
      };
      if (clientHeight <= 1000 && clientHeight > 900) {
        config.top = '150';
      } else if (clientHeight <= 900) {
        config.top = '100';
      }
      return config;
    },
    isEn() {
      return language === 'en-US';
    },
    lineHeight() {
      return this.isEn ? 'line-height: 18px' : 'line-height: 22px';
    },
  },
  watch: {
    // 监听查询参数变化，则获取列表
    searchParamsWatch: {
      handler(newVal, oldVal) {
        if (_.isEqual(newVal, oldVal)) return;
        // 筛选状态，先特殊处理
        if (!_.isEqual(newVal.statusUnion, oldVal.statusUnion)) {
          const status = [];
          let ignoreTypes = [];
          const hasIgnore = newVal.statusUnion.includes(4);
          newVal.statusUnion.forEach((item) => {
            if (typeof item === 'string' && item.startsWith('4-')) {
              ignoreTypes.push(Number(item.replace('4-', '')));
            } else {
              status.push(item);
            }
          });
          if (newVal.statusUnion.includes(4)) {
            // 全选已忽略，就不用传忽略类型
            ignoreTypes = [];
          } else if (ignoreTypes.length && !status.includes(4)) {
            // 选了忽略类型，参数同时要选已忽略状态
            status.push(4);
          }
          this.$refs.statusTree.setChecked(newVal.statusUnion);
          if (newVal.statusUnion.includes(2)) {
            this.dateType = 'fixTime';
          } else {
            this.dateType = 'createTime';
          }
          this.searchParams.status = status;
          this.searchParams.ignoreReasonTypes = ignoreTypes;
          return;
        }
        if (this.isSearch) {
          this.tableLoading = true;
          this.fetchLintList(!this.pageChange)
            .then((list) => {
              if (this.pageChange) {
                // 将一页的数据追加到列表
                this.listData.defectList.content = this.listData.defectList.content.concat(list.defectList.content);

                // 隐藏加载条
                this.isFileListLoadMore = false;

                // 重置页码变更标记
                this.pageChange = false;
              } else {
                this.listData = { ...this.listData, ...list };
                this.totalCount = this.listData.defectList.totalElements;
                this.pagination.count = this.listData.defectList.totalElements;

                // 重置文件下的问题详情
                this.lintDetail = {};
              }
            })
            .finally(() => {
              this.addTableScrollEvent();
              this.tableLoading = false;
            });
        }
      },
      deep: true,
    },
    defectDetailSearchParams: {
      handler() {
        this.operateParams.ignoreReasonType = '';
        this.operateParams.ignoreReason = '';
        this.emptyText = this.$t('未选择文件');
        this.defectDetailDialogVisible = true;
        this.fetchLintDetail();
      },
      deep: true,
    },
    searchInput: {
      handler() {
        if (this.searchFormData.filePathTree.children) {
          if (this.searchInput) {
            // this.searchFormData.filePathTree.expanded = true
            this.openTree(this.searchFormData.filePathTree);
          } else {
            this.searchFormData.filePathTree.expanded = false;
          }
        }
      },
      deep: true,
    },
    defectDetailDialogVisible: {
      handler() {
        if (!this.defectDetailDialogVisible) {
          this.codeViewerInDialog.setValue('');
          this.codeViewerInDialog.setOption('firstLineNumber', 1);
        }
      },
      deep: true,
    },
    taskDetail: {
      handler(newVal) {
        if (newVal.enableToolList.find(item => item.toolName === 'CCN')) {
          this.$nextTick(() => {
            this.getQueryPreLineNum();
          });
        }
      },
      deep: true,
    },
    isSearchDropdown() {
      this.setTableHeight();
    },
    taskIdList(val) {
      this.init();
    },
    'searchParamsWatch.fileList'(val, oldVal) {
      if (_.isEqual(val, oldVal)) return;
      this.fetchSeverityParams();
      this.fetchStatusParams();
    },
  },
  created() {
    if (
      !this.taskDetail.nameEn
      || this.taskDetail.enableToolList.find(item => item.toolName === 'CCN')
    ) {
      this.init(true);
      this.isProjectDefect ? this.fetchTaskList() : this.getBuildList();
    }

    // 读取缓存搜索过滤项
    const columnsCache = JSON.parse(localStorage.getItem(CCN_SEARCH_OPTION_CACHE));
    if (columnsCache) {
      this.selectedOptionColumn = _.cloneDeep(columnsCache);
      this.customOption = columnsCache;
    } else {
      this.selectedOptionColumn = this.getCustomOption(true);
    }
    this.handelFetchIgnoreList();
    this.guideFlag = Boolean(localStorage.getItem('guideEnd') || '');
  },
  mounted() {
    // this.$nextTick(() => {
    //     this.getQueryPreLineNum()
    // })
    // 读取缓存中搜索项首次展示或收起
    const ccnSearchExpend = JSON.parse(window.localStorage.getItem('ccnSearchExpend'));
    ccnSearchExpend === null
      ? (this.isSearchDropdown = true)
      : (this.isSearchDropdown = ccnSearchExpend);
    window.addEventListener('resize', this.getQueryPreLineNum);
    this.openDetail();
    this.keyOperate();
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.getQueryPreLineNum);
    document.onkeydown = null;
  },
  methods: {
    async init(isInit) {
      this.getQueryPreLineNum();
      this.fetchSeverityParams();
      this.fetchStatusParams();
      isInit ? (this.contentLoading = true) : (this.fileLoading = true);
      await Promise.all([this.fetchLintList(), this.fetchLintParams()])
        .then(([list, params]) => {
          this.ccnThreshold = list.ccnThreshold;
          this.isSearch = true;
          if (isInit) {
            this.contentLoading = false;
            this.isFetched = true;
          } else {
            this.tableLoading = false;
          }
          if (list.tips) {
            this.handleNewBuildId(list.tips);
          }
          this.listData = { ...this.listData, ...list };
          this.totalCount = this.listData.defectList.totalElements;
          this.pagination.count = this.listData.defectList.totalElements;
          this.newDefectJudgeTime = list.newDefectJudgeTime
            ? this.formatTime(list.newDefectJudgeTime, 'yyyy-MM-dd')
            : '';
          this.addTableScrollEvent();

          // todo 给文件路径树加上icon
          function formatFilePath(filepath = {}) {
            if (filepath && filepath.children && filepath.children.length) {
              filepath.openedIcon = 'icon-folder-open';
              filepath.closedIcon = 'icon-folder';
              filepath.children.forEach(formatFilePath);
            } else {
              filepath.icon = 'icon-file';
            }
          }

          // 把当前登录用户插进去
          function addCurrentUser(authorList = [], user = '') {
            const newAuthorList = authorList.filter(item => item !== user);
            newAuthorList.unshift(user);
            return newAuthorList;
          }
          formatFilePath(params.filePathTree);
          const authorList = addCurrentUser(
            params.authorList,
            this.user.username,
          );
          params.authorList = authorList.map(item => ({
            id: item,
            name: item,
          }));

          this.searchFormData = Object.assign({}, this.searchFormData, params);
        })
        .catch((e) => {
          this.tableLoading = false;
          this.contentLoading = false;
          this.isFetched = true;
        });
    },
    fetchLintList(isReset = false) {
      if (this.visitable === false) {
        this.$nextTick(() => {
          this.emptyDialogVisible = true;
        });
        return;
      }
      // 非分页条件变化重置分页
      if (isReset) {
        this.searchParams.pageNum = 1;
        this.$refs.fileListTable.$refs.bodyWrapper.scrollTop = 0;
      }
      const params = this.getSearchParams();
      return this.$store.dispatch('defect/lintList', params);
    },
    async getBuildList() {
      if (this.visitable === false) return;
      const list = await this.$store.dispatch('defect/getBuildList', {
        taskId: this.$route.params.taskId,
      });
      // display name 处理
      const userIds = [...new Set(list.map(item => item.buildUser))];
      await this.$store.dispatch('displayname/batchGetDisplayName', userIds);
      this.buildList = list;
    },
    fetchLintParams() {
      if (this.visitable === false) return;
      const { buildId } = this.searchParams;
      const params = {
        toolId: 'CCN',
        buildId,
        toolNameList: ['CCN'],
        multiTaskQuery: this.isProjectDefect,
      };
      return this.isProjectDefect
        ? this.$store.dispatch('defect/lintOtherParams', params)
        : this.$store.dispatch('defect/lintParams', params);
    },
    fetchLintDetail() {
      const { pattern } = this.toolMap[this.toolId];
      const params = {
        ...this.searchParams,
        ...this.defectDetailSearchParams,
        pattern,
      };
      bus.$emit('show-app-loading');
      this.$store
        .dispatch('defect/lintDetail', params)
        .then((detail) => {
          if (detail.defectVO.ignoreReasonType) {
            this.operateParams.ignoreReasonType = detail.defectVO.ignoreReasonType;
          }
          if (detail.defectVO.ignoreReason) {
            this.operateParams.ignoreReason = detail.defectVO.ignoreReason;
          }
          if (detail.code === '2300005') {
            this.defectDetailDialogVisible = false;
            setTimeout(() => {
              this.$bkInfo({
                subHeader: this.$createElement(
                  'p',
                  {
                    style: {
                      fontSize: '20px',
                      lineHeight: '40px',
                    },
                  },
                  this.$t('无法获取问题的代码片段。请先将工蜂OAuth授权给蓝盾。'),
                ),
                confirmFn: () => {
                  this.$store
                    .dispatch('defect/oauthUrl', { toolName: this.toolId })
                    .then((res) => {
                      window.open(res, '_blank');
                    });
                },
              });
            }, 500);
          } else {
            this.lintDetail = {
              ...this.lintDetail,
              ...detail,
              codeComment: detail.codeComment,
            };

            this.currentLintFile = detail.defectVO || {};
            // 查询详情后，全屏显示问题
            this.handleCodeFullScreen();
          }
        })
        .finally(() => {
          bus.$emit('hide-app-loading');
        });
    },
    getDefectCountBySeverity(severity) {
      const severityFieldMap = {
        1: 'superHighCount',
        2: 'highCount',
        4: 'mediumCount',
        8: 'lowCount',
      };
      const count = this.searchFormData[severityFieldMap[severity]] || 0;
      return count > 100000 ? this.$t('10万+') : count;
    },
    async fetchSeverityParams() {
      if (this.visitable === false) return;
      const params = this.getSearchParams();
      params.statisticType = 'SEVERITY';
      const res = await this.$store.dispatch('defect/lintSearchParams', params);
      const { superHighCount, highCount, mediumCount, lowCount } = res;
      this.searchFormData = Object.assign(this.searchFormData, {
        superHighCount,
        highCount,
        mediumCount,
        lowCount,
      });
    },
    async fetchStatusParams() {
      if (this.visitable === false) return;
      const params = this.getSearchParams();
      params.statisticType = 'STATUS';
      const res = await this.$store.dispatch('defect/lintSearchParams', params);
      const { existCount, fixCount, ignoreCount, maskCount } = res;
      this.statusFormData = Object.assign({}, this.statusFormData, {
        existCount,
        fixCount,
        ignoreCount,
        maskCount,
      });
      this.hasCountData = true;
    },
    handleSortChange({ column, prop, order }) {
      const orders = { ascending: 'ASC', descending: 'DESC' };
      this.searchParams = {
        ...this.searchParams,
        ...{ pageNum: 1, sortField: prop, sortType: orders[order] },
      };
    },
    handleSelectionChange(selection) {
      this.selectedLen = selection.length || 0;
      this.isBatchOperationShow = Boolean(selection.length);
      // 如果长度是最长，那么就是Y，否则是N
      this.isSelectAll = (this.selectedLen && this.selectedLen === this.defectList.length) ? 'Y' : 'N';
    },
    toSelectAll() {
      this.isSelectAll = (this.selectedLen && this.selectedLen === this.defectList.length) ? 'Y' : 'N';
    },
    handleSelectable(row, index) {
      // return !(row.status & 2)
      return true;
    },
    handlePageChange(page) {
      this.pagination.current = page;
      this.searchParams = { ...this.searchParams, ...{ pageNum: page } };
    },
    handlePageLimitChange(currentLimit) {
      this.pagination.current = 1; // 切换分页大小时要回到第一页
      this.searchParams = {
        ...this.searchParams,
        ...{ pageNum: 1, pageSize: currentLimit },
      };
    },
    handleStatus(status, ignoreReasonType) {
      let key = 1;
      if (status === 1) {
        key = 1;
      } else if (status & 2) {
        key = 2;
      } else if (status & 4) {
        key = 4;
      } else if (status & 8 || status & 16) {
        // 8是路径屏蔽，16是规则屏蔽
        key = 8;
      }
      const statusMap = {
        1: this.$t('待修复'),
        2: this.$t('已修复'),
        4: this.$t('已忽略'),
        8: this.$t('已屏蔽'),
      };
      /**
         * submitStatus字段：
          1 - 排队中
          2 - 准备开始提单
          3 - 提单成功
          4 - 提单失败
        */
      // let issueStatus = ''
      // if (defectIssueInfoVO.submitStatus && defectIssueInfoVO.submitStatus !== 4) {
      //   issueStatus = this.$t('(已提单)')
      // }
      let ignoreStr = '';
      if (status & 4 && ignoreReasonType) {
        const ignoreName = this.ignoreList.find(item => item.ignoreTypeId === ignoreReasonType)?.name;
        ignoreName && (ignoreStr = `-${ignoreName}`);
      }
      return `${statusMap[key]}${ignoreStr}`;
    },
    handleMark(markFlag, batchFlag, entityId) {
      // markFlag 0: 取消标记, 1: 标记修改
      // batchFlag true: 批量操作
      let bizType = 'MarkDefect';
      let defectKeySet = [];
      if (batchFlag) {
        this.$refs.fileListTable.selection.forEach((item) => {
          defectKeySet.push(item.entityId);
        });
        if (markFlag) bizType = 'MarkDefect';
      } else {
        defectKeySet = [entityId];
      }
      const { taskIdList, toolNameList, dimensionList } = this;
      let data = {
        ...this.operateParams,
        bizType,
        defectKeySet,
        markFlag,
        taskIdList,
        toolNameList,
        dimensionList,
      };
      if (this.isSelectAll === 'Y') {
        data = {
          ...data,
          isSelectAll: 'Y',
          queryDefectCondition: JSON.stringify(this.getSearchParams()),
        };
      }
      this.tableLoading = true;
      this.$store
        .dispatch('defect/batchEdit', data)
        .then((res) => {
          if (res.code === '0') {
            let message = markFlag
              ? this.$t('标记为已处理成功')
              : this.$t('取消标记成功');
            if (batchFlag) {
              this.init();
              if (markFlag) {
                const list = res.data || [];
                let revertCount = 0;
                let markCount = 0;
                list.forEach((item) => {
                  if (item.bizType === 'RevertIgnore') {
                    revertCount = item.count;
                  } else if (item.bizType === 'MarkDefect') {
                    markCount = item.count;
                  }
                });
                const unfixedMarkCount = markCount - revertCount;
                message = '';
                if (unfixedMarkCount) message = this.$t('x个待修复问题标记为已处理成功。', {
                  unfixedMarkCount,
                });
                if (unfixedMarkCount && revertCount) message += ', ';
                if (revertCount) message += this.$t(
                  'x个已忽略问题取消忽略并标记为已处理成功。',
                  { revertCount },
                );
              }
            } else {
              this.listData.defectList.content.forEach((item) => {
                if (item.entityId === entityId) {
                  item.mark = markFlag;
                }
              });
              this.listData.defectList.content = this.listData.defectList.content.slice();
            }
            this.$bkMessage({
              theme: 'success',
              message,
            });
            if (this.defectDetailDialogVisible) this.fetchLintDetail();
          }
        })
        .catch((e) => {
          console.error(e);
        })
        .finally(() => {
          this.tableLoading = false;
        });
    },
    handleIgnore(ignoreType, batchFlag, entityId, defectId) {
      this.$refs.operateDropdown?.hide();
      this.operateParams.bizType = ignoreType;
      this.operateParams.batchFlag = batchFlag;
      if (batchFlag) {
        const defectKeySet = [];
        this.$refs.fileListTable.selection.forEach((item) => {
          defectKeySet.push(item.entityId);
        });
        this.operateParams.defectKeySet = defectKeySet;
      } else {
        this.operateParams.defectKeySet = [entityId];
      }
      if (ignoreType === 'RevertIgnore') {
        this.handleIgnoreConfirm();
      } else {
        window.changeAlert = false;
        this.ignoreReasonDialogVisible = true;
      }
    },
    handleIgnoreConfirm() {
      let data = this.operateParams;
      this.tableLoading = true;
      this.ignoreReasonDialogVisible = false;
      if (this.isSelectAll === 'Y') {
        data = {
          ...data,
          isSelectAll: 'Y',
          queryDefectCondition: JSON.stringify(this.getSearchParams()),
        };
      }
      const { taskIdList, toolNameList, dimensionList } = this;
      data = { ...data, taskIdList, toolNameList, dimensionList };
      this.isRowChangeIgnoreType = false;
      this.$store
        .dispatch('defect/batchEdit', data)
        .then((res) => {
          if (res.code === '0') {
            let message = '';
            if (this.operateParams.bizType === 'ChangeIgnoreType') {
              message = this.$t('修改忽略类型成功');
            } else {
              message = this.operateParams.bizType === 'IgnoreDefect'
                ? this.$t('忽略问题成功')
                : this.$t('恢复问题成功。该问题将重新在待修复列表中显示。');
            }
            this.$bkMessage({
              theme: 'success',
              message,
            });
            if (data.batchFlag) {
              this.init();
            } else {
              this.listData.defectList.content.forEach((item) => {
                if (item.entityId === data.defectKeySet[0]) {
                  if (this.operateParams.bizType === 'ChangeIgnoreType') {
                    item.ignoreReasonType = this.operateParams.ignoreReasonType;
                    item.ignoreReason = this.operateParams.ignoreReason;
                  } else if (this.operateParams.bizType === 'IgnoreDefect') {
                    item.status = 4;
                    item.ignoreReasonType = this.operateParams.ignoreReasonType;
                    item.ignoreReason = this.operateParams.ignoreReason;
                  } else {
                    item.status = 1;
                  }
                }
              });
              this.listData.defectList.content = this.listData.defectList.content.slice();
              // this.init()
            }
            this.operateParams.ignoreReason = '';
            this.operateParams.ignoreReasonType = '';
            if (this.defectDetailDialogVisible) this.fetchLintDetail();
            this.operateParams.ignoreReason = '';
            // this.defectDetailDialogVisible = false
          }
        })
        .catch((e) => {
          console.error(e);
        })
        .finally(() => {
          this.tableLoading = false;
          this.isAddingIgnore = false;
          this.fetchSeverityParams();
          this.fetchStatusParams();
          this.handleClearWindowAlert();
        });
    },
    handleIgnoreCancel() {
      this.operateParams.ignoreReason = '';
      this.operateParams.ignoreReasonType = '';
      this.ignoreReasonDialogVisible = false;
      this.isAddingIgnore = false;
      this.handleClearWindowAlert();
    },
    handleComment(id) {
      // 暂不做评论修
      // const hasComment = this.commentList.find(val => val.userName === this.userName)
      // this.commentParams.comment = hasComment ? hasComment.comment : ''
      // this.commentParams.singleCommentId = hasComment ? hasComment.singleCommentId : ''

      this.commentParams.comment = '';
      this.commentParams.defectId = id;
      this.commentParams.commentId = this.lintDetail.codeComment
        ? this.lintDetail.codeComment.entityId
        : '';
      window.changeAlert = false;
      this.commentDialogVisible = true;
    },
    handleFileListRowClick(row, event, column) {
      this.fileIndex = this.defectList.findIndex(file => file.entityId === row.entityId);
      // 筛选后，问题详情为空，此时要把参数强制置空，不然点击文件不能触发请求
      if (!this.lintDetail.lintDefectList) {
        this.defectDetailSearchParams.entityId = '';
      }
      this.defectDetailSearchParams.entityId = row.entityId;
      this.defectDetailSearchParams.filePath = row.filePath;
      this.$refs.fileListTable.clearSelection();
      this.screenScroll();
    },
    handleAuthorIndex(index) {
      this.hoverAuthorIndex = index;
    },
    handleAuthor(changeAuthorType, entityId, author, defectId) {
      this.authorEditDialogVisible = true;
      window.changeAlert = false;
      this.operateParams.changeAuthorType = changeAuthorType;
      if (author !== undefined) {
        this.operateParams.sourceAuthor = [author];
      }
      this.operateParams.defectKeySet = [entityId];
    },
    handleCodeFullScreen(showDefectDetail) {
      // setTimeout(() => {
      //     const width = 700 - document.getElementsByClassName('filename')[0].offsetWidth
      //     if (document.getElementsByClassName('filepath')[0]) {
      //         document.getElementsByClassName('filepath')[0].style.width = width + 'px'
      //     }
      // }, 0)

      if (!this.codeViewerInDialog) {
        const codeMirrorConfig = {
          ...this.codeMirrorDefaultCfg,
          ...{ autoRefresh: true },
        };
        this.codeViewerInDialog = CodeMirror(
          document.getElementById('codeViewerInDialog'),
          codeMirrorConfig,
        );

        this.codeViewerInDialog.on('update', () => {});
      }
      this.updateCodeViewer(this.codeViewerInDialog, showDefectDetail);
      this.codeViewerInDialog.refresh();
      setTimeout(this.scrollIntoView, 250);
    },
    // 代码展示相关
    updateCodeViewer(codeViewer, showDefectDetail) {
      if (!this.lintDetail.fileContent) {
        this.emptyText = this.$t('文件内容为空');
        return;
      }
      const { fileName, fileContent, trimBeginLine, codeComment } = this.lintDetail;
      const { mode } = CodeMirror.findModeByFileName(fileName);
      this.commentList = codeComment ? codeComment.commentList : [];
      import(`codemirror/mode/${mode}/${mode}.js`).then((m) => {
        codeViewer.setOption('mode', mode);
      });
      codeViewer.setValue(fileContent);
      codeViewer.setOption(
        'firstLineNumber',
        trimBeginLine === 0 ? 1 : trimBeginLine,
      );

      this.buildLintHints(codeViewer, showDefectDetail);
    },
    // 创建问题提示块
    async buildLintHints(codeViewer, showDefectDetail) {
      let checkerComment = '';
      const { trimBeginLine } = this.lintDetail;
      const { ccn, startLines, endLines, entityId } = this.currentLintFile;
      const { ccnThreshold } = this;
      const hints = document.createElement('div');
      const checkerDetail = `
                    <div>
                        <p>${this.$t('如果多个函数存在相同代码路径片段，可以尝试以下技巧：')}</p>
                        <p>${this.$t('技巧名称：提炼函数')}</p>
                        <p>${this.$t('具体方法：将相同的代码片段独立成函数，并在之前的提取位置上条用该函数')}</p>
                        <p>${this.$t('示例代码：')}</p>
                        <pre>void Example(int val){
    if(val &lt; MAX_VAL){
        val = MAX_VAL;
    }
    for(int i = 0; i &lt; val; i++){
        doSomething(i);
    }
}</pre>
            <p>${this.$t('可以提炼成两个函数')}</p>
            <pre>int getValidVal(int val){
    if(val &lt; MAX_VAL){
        return MAX_VAL;
    }
    return val;
    }

    void Example(int val){
    val = getValidVal(val);
    for(int i = 0; i &lt; val; i++){
        doSomething(i);
    }
}</pre>
                    </div>`;

      if (this.commentList.length) {
        const userIds = [...new Set(this.commentList.map(item => item.userName))];
        const dataMap = await this.$store.dispatch('displayname/batchGetDisplayName', userIds);
        for (let i = 0; i < this.commentList.length; i++) {
          checkerComment += `
                            <p class="comment-item">
                                <span class="info">
                                    <i class="codecc-icon icon-user-fill"></i>
                                    <span>
                                      ${dataMap.get(this.commentList[i].userName)?.display_name || this.commentList[i].userName}
                                    </span>
                                    <span title="${
  this.commentList[i].comment
}">${this.commentList[i].comment}</span>
                                </span>
                                <span class="handle">
                                    <span>${this.formatCommentTime(this.commentList[i].commentTime)}</span>
                                    <i class="bk-icon icon-delete"
                                    data-comment="${
  this.commentList[i].comment
}"
                                    data-entityid="${entityId}"
                                    data-type="comment-${
  this.commentList[i].singleCommentId
}">
                                    </i>
                                </span>
                            </p>`;
        }
      } else checkerComment = '';

      hints.innerHTML = `
                    <div class="lint-info">
                        <div class="lint-info-main">
                            <i class="lint-icon bk-icon icon-right-shape"></i>
                            <p>${this.$t(
    '圈复杂度为，超过圈复杂度规则的阈值xx，请进行函数功能拆分降低代码复杂度。',
    { ccn, ccnThreshold },
  )}</p>
                        </div>
                        <div class="checker-detail">${checkerDetail}</div>
                        ${
  checkerComment
    ? `<div class="checker-comment">${checkerComment}</div>`
    : ''
}
                    </div>
                `;

      hints.className = showDefectDetail ? 'lint-hints active' : 'lint-hints';
      codeViewer.addLineWidget(startLines - trimBeginLine, hints, {
        coverGutter: false,
        noHScroll: false,
        above: true,
      });
      for (
        let i = startLines - trimBeginLine;
        i <= endLines - trimBeginLine;
        i++
      ) {
        codeViewer.addLineClass(i, 'wrap', 'lint-hints-wrap main ccn');
      }
      setTimeout(this.scrollIntoView, 1);
    },
    // 默认滚动到问题位置
    scrollIntoView() {
      const { trimBeginLine } = this.lintDetail;
      const codeViewer = this.codeViewerInDialog;
      const startLines = this.currentLintFile.startLines - 1;
      const { top } = codeViewer.charCoords(
        { line: startLines - trimBeginLine, ch: 0 },
        'local',
      );
      const lineHeight = codeViewer.defaultTextHeight();
      codeViewer.scrollTo(0, top - 5 * lineHeight);
    },
    handleCodeViewerInDialogClick(event, eventSource) {
      this.codeViewerClick(event, 'dialog-code');
    },
    codeViewerClick(event, eventSource) {
      const lintHints = getClosest(event.target, '.lint-hints');
      const lintInfo = getClosest(event.target, '.icon-right-shape');
      const commentCon = getClosest(event.target, '.checker-comment');
      const delHandle = getClosest(event.target, '.icon-delete');
      // 如果点击的是lint问题区域，展开修复建议
      // if (lintHints && !commentCon) {
      //   toggleClass(lintHints, 'active')
      // }
      if (lintInfo) {
        const showDefectDetail = hasClass(lintHints, 'active');
        this.handleCodeFullScreen(!showDefectDetail);
      }
      // 如果点击的是删除评论
      if (delHandle) {
        const that = this;
        this.$bkInfo({
          title: this.$t('删除评论'),
          subTitle: this.$t('确定要删除该条评论吗？'),
          maskClose: true,
          confirmFn() {
            const commentStr = delHandle.getAttribute('data-comment');
            const defectEntityId = delHandle.getAttribute('data-entityid');
            const delData = delHandle.getAttribute('data-type');
            const singleCommentId = delData.split('-').pop();
            that.deleteComment(singleCommentId, defectEntityId, commentStr);
          },
        });
      }
    },
    // 删除评论
    deleteComment(id, defectEntityId, commentStr) {
      const params = {
        commentId: this.lintDetail.codeComment.entityId,
        singleCommentId: id,
        defectEntityId,
        commentStr,
        toolName: 'CCN',
      };
      this.$store.dispatch('defect/deleteComment', params).then((res) => {
        if (res.code === '0') {
          this.$bkMessage({
            theme: 'success',
            message: this.$t('删除成功'),
          });
          this.commentParams.comment = '';
          this.fetchLintDetail();
        }
      });
    },
    // 处理人修改
    handleAuthorEdit() {
      this.$router.push({
        name: 'task-settings-trigger',
      });
    },
    handleFilePathCancelClick() {
      const { filePathDropdown } = this.$refs;
      filePathDropdown.hide();
    },
    openSlider() {
      this.show = true;
    },
    numToArray(num, arr = [1, 2, 4, 8]) {
      let filterArr = arr.filter(x => x & num);
      filterArr = filterArr.length ? filterArr : arr;
      return filterArr;
    },
    openTree(arr) {
      if (arr.children) {
        arr.expanded = true;
        arr.children.forEach((item) => {
          this.openTree(item);
        });
      }
    },
    toLogs() {
      this.$router.push({
        name: 'task-settings-trigger',
      });
    },
    keyOperate() {
      const vm = this;
      document.onkeydown = keyDown;
      function keyDown(event) {
        const e = event || window.event;
        if (e.target.nodeName !== 'BODY') return;
        switch (e.keyCode) {
          case 13: // enter
            // e.path.length < 5 防止规则等搜索条件里面的回车触发打开详情
            if (!vm.defectDetailDialogVisible && !vm.authorEditDialogVisible) vm.keyEnter();
            break;
          case 27: // esc
            if (vm.defectDetailDialogVisible) vm.defectDetailDialogVisible = false;
            break;
          case 37: // left
            if (vm.defectDetailDialogVisible && vm.fileIndex > 0) {
              vm.handleFileListRowClick(vm.defectList[(vm.fileIndex -= 1)]);
            } else if (!vm.defectDetailDialogVisible && vm.fileIndex > 0) {
              vm.fileIndex -= 1;
            }
            break;
          case 38: // up
            if (!vm.defectDetailDialogVisible && vm.fileIndex > 0) {
              vm.fileIndex -= 1;
            } else if (vm.defectDetailDialogVisible && vm.fileIndex > 0) {
              vm.handleFileListRowClick(vm.defectList[(vm.fileIndex -= 1)]);
              return false;
            }
            break;
          case 39: // right
            if (
              vm.defectDetailDialogVisible
              && vm.fileIndex < vm.defectList.length - 1
            ) {
              vm.handleFileListRowClick(vm.defectList[(vm.fileIndex += 1)]);
            } else if (
              !vm.defectDetailDialogVisible
              && vm.fileIndex < vm.defectList.length - 1
            ) {
              vm.fileIndex += 1;
            }
            break;
          case 40: // down
            if (
              !vm.defectDetailDialogVisible
              && vm.fileIndex < vm.defectList.length - 1
            ) {
              vm.fileIndex += 1;
            } else if (
              vm.defectDetailDialogVisible
              && vm.fileIndex < vm.defectList.length - 1
            ) {
              vm.handleFileListRowClick(vm.defectList[(vm.fileIndex += 1)]);
              return false;
            }
            break;
        }
      }
    },
    addTableScrollEvent() {
      this.$nextTick(() => {
        // 滚动加载
        if (this.$refs.fileListTable) {
          const tableBodyWrapper = this.$refs.fileListTable.$refs.bodyWrapper;

          // 列表滚动加载
          tableBodyWrapper.addEventListener('scroll', (event) => {
            const dom = event.target;
            // 总页数
            const { totalPages } = this.listData.defectList;
            // 当前页码
            const currentPageNum = this.searchParams.pageNum;
            // 是否滚动到底部
            const hasScrolledToBottom = dom.scrollTop + dom.offsetHeight + 100 > dom.scrollHeight;

            // 触发翻页加载
            if (
              hasScrolledToBottom
              && currentPageNum + 1 <= totalPages
              && this.isFileListLoadMore === false
            ) {
              // 显示加载条
              this.isFileListLoadMore = true;
              // 变更页码触发查询
              this.searchParams.pageNum += 1;
              // 标记为页面变更查询
              this.pageChange = true;
            }
          });
        }
      });
    },
    keyEnter() {
      const row = this.defectList[this.fileIndex];
      if (!this.lintDetail.lintDefectList) {
        this.defectDetailSearchParams.entityId = '';
      }
      this.defectDetailSearchParams.entityId = row.entityId;
      this.defectDetailSearchParams.filePath = row.filePath;
    },
    formatDate(dateNum, time) {
      if (!dateNum) return '--';
      return time
        ? format(dateNum, 'HH:mm:ss')
        : format(dateNum, 'yyyy-MM-dd HH:mm:ss');
    },
    screenScroll() {
      this.$nextTick(() => {
        if (this.$refs.fileListTable.$refs.bodyWrapper) {
          const children = this.$refs.fileListTable.$refs.bodyWrapper;
          const height = this.fileIndex > 3 ? (this.fileIndex - 3) * 42 : 0;
          children.scrollTo({
            top: height,
            behavior: 'smooth',
          });
        }
      }, 0);
    },
    setTableHeight() {
      this.$nextTick(() => {
        let smallHeight = 0;
        let largeHeight = 0;
        let tableHeight = 0;
        const i = this.listData.defectList.content.length || 0;
        if (this.$refs.fileListTable) {
          const $main = document.getElementsByClassName('main-form');
          smallHeight = $main.length > 0 ? $main[0].clientHeight : 0;
          largeHeight = this.$refs.mainContainer
            ? this.$refs.mainContainer.clientHeight
            : 0;
          tableHeight = this.$refs.fileListTable.$el.clientHeight;
        }
        this.screenHeight = i * 42 > tableHeight ? largeHeight - smallHeight - 62 : i * 42 + 43;
        this.screenHeight = this.screenHeight === 43 ? 336 : this.screenHeight;
      });
    },
    toggleSearch() {
      this.isSearchDropdown = !this.isSearchDropdown;
      window.localStorage.setItem(
        'ccnSearchExpend',
        JSON.stringify(this.isSearchDropdown),
      );
      this.getQueryPreLineNum();
    },
    getQueryPreLineNum() {
      let bodyWidth = document.body.offsetWidth;
      if (bodyWidth < 1280) bodyWidth -= 10; // 有滚动条要减去滚动条宽度
      const containerW = bodyWidth - 292; // 搜索栏宽度
      const childW = 379; // 单个搜素宽度
      // const containerW = document.getElementsByClassName('search-form')[0].offsetWidth
      // const childW = document.getElementsByClassName('cc-col')[0].offsetWidth
      const average = Math.floor(containerW / childW);
      this.lineAverageOpt = average;
    },
    getFileName(path) {
      return path.split('/').pop();
    },
    handleAuthorEditConfirm() {
      let data = this.operateParams;
      const sourceAuthor = data.sourceAuthor
        ? new Set(data.sourceAuthor)
        : new Set();
      if (data.changeAuthorType === 2) {
        const defectKeySet = [];
        this.$refs.fileListTable.selection.forEach((item) => {
          defectKeySet.push(item.entityId);
          sourceAuthor.add(item.author);
        });
        this.operateParams.defectKeySet = defectKeySet;
        data.defectKeySet = defectKeySet;
      }
      data.bizType = 'AssignDefect';
      data.sourceAuthor = Array.from(sourceAuthor);
      data.newAuthor = data.targetAuthor;
      if (this.isSelectAll === 'Y') {
        data = {
          ...data,
          isSelectAll: 'Y',
          queryDefectCondition: JSON.stringify(this.getSearchParams()),
        };
      }
      const { taskIdList, toolNameList, dimensionList } = this;
      data = { ...data, taskIdList, toolNameList, dimensionList };
      const dispatchUrl = data.changeAuthorType === 3 ? 'defect/authorEdit' : 'defect/batchEdit';
      this.authorEditDialogVisible = false;
      this.tableLoading = true;
      this.$store
        .dispatch(dispatchUrl, data)
        .then((res) => {
          if (res.code === '0') {
            this.$bkMessage({
              theme: 'success',
              message: this.$t('修改成功'),
            });
            if (data.changeAuthorType === 1) {
              this.listData.defectList.content.forEach((item) => {
                if (item.entityId === data.defectKeySet[0]) {
                  item.author = data.newAuthor.join(';');
                }
              });
              this.listData.defectList.content = this.listData.defectList.content.slice();
            } else {
              this.init();
            }
            if (this.defectDetailDialogVisible) this.fetchLintDetail();
            this.operateParams.sourceAuthor = [];
            this.operateParams.targetAuthor = [];
          }
        })
        .catch((e) => {
          console.error(e);
        })
        .finally(() => {
          this.tableLoading = false;
          this.handleClearWindowAlert();
        });
    },
    handleCommentConfirm() {
      this.commentDialogVisible = false;
      // 暂不做修改评论
      // const url = this.commentParams.singleCommentId ? 'defect/updateComment' : 'defect/commentDefect'

      const url = 'defect/commentDefect';
      bus.$emit('show-app-loading');
      this.$store.dispatch(url, this.commentParams)
        .then((res) => {
          if (res.code === '0') {
            this.$bkMessage({
              theme: 'success',
              message: this.$t('评论成功'),
            });
            this.commentParams.comment = '';
            this.fetchLintDetail();
          }
        })
        .finally(() => {
          bus.$emit('hide-app-loading');
        });
    },
    formatCommentTime(time) {
      return formatDiff(time);
    },
    handleRowClassName({ row, rowIndex }) {
      let rowClass = 'list-row';
      if (this.fileIndex === rowIndex) rowClass += ' current-row';
      return rowClass;
    },
    getIgnoreReasonByType(type) {
      const typeMap = this.ignoreList.reduce((result, item) => {
        result[item.ignoreTypeId] = item.name;
        return result;
      }, {});
      return typeMap[type];
    },
    openDetail() {
      const id = this.$route.query.entityId;
      if (id) {
        setTimeout(() => {
          if (!this.toolMap[this.toolId]) {
            this.openDetail();
          } else {
            this.defectDetailSearchParams.entityId = id;
            this.defectDetailSearchParams.filePath = this.$route.query.filePath;
          }
        }, 500);
      }
    },
    getSearchParams() {
      const params = { ...this.searchParams };

      const { daterange, authorList } = this.searchParams;
      const author = this.isProjectDefect
        ? authorList[0]
        : this.searchParams.author;
      const multiTaskQuery = this.isProjectDefect;
      const startTime = this.dateType === 'createTime' ? 'startCreateTime' : 'startFixTime';
      const endTime = this.dateType === 'createTime' ? 'endCreateTime' : 'endFixTime';
      params[startTime] = daterange[0] || '';
      params[endTime] = daterange[1] || '';
      params.taskIdList = this.taskIdList;
      return { ...params, author, multiTaskQuery };
    },
    /**
     * 重置搜索过滤项
     */
    handleSelectAllSearchOption() {
      const isSelectAll = this.customOption.every(item => item.isChecked);
      this.customOption = this.getCustomOption(!isSelectAll);
      // this.$refs.handleMenu.instance.hide()
      // this.customOption = this.getCustomOption(true)
      // this.selectedOptionColumn = _.cloneDeep(this.customOption)
      // localStorage.setItem(CCN_SEARCH_OPTION_CACHE, JSON.stringify(this.selectedOptionColumn))
    },

    /**
     * 确认搜索过滤项
     */
    handleConfirmSearchOption() {
      this.$refs.handleMenu.instance.hide();
      this.selectedOptionColumn = _.cloneDeep(this.customOption);
      localStorage.setItem(
        CCN_SEARCH_OPTION_CACHE,
        JSON.stringify(this.selectedOptionColumn),
      );
      this.setTableHeight();
    },
    async downloadExcel() {
      if (this.tableLoading) {
        this.$bkMessage({
          message: this.$t('问题列表加载中，请等待列表加载完再尝试导出。'),
        });
        return;
      }
      const params = this.getSearchParams();
      params.pageSize = 300000;
      if (this.totalCount > 300000) {
        this.$bkMessage({
          message: this.$t('当前问题数已超过30万个，无法直接导出excel，请筛选后再尝试导出。'),
        });
        return;
      }
      try {
        this.exportLoading = true;
        const res = await this.$store.dispatch('defect/lintList', params);
        const list = res && res.defectList && res.defectList.content;
        // display name 处理
        const userIds = [...new Set(list.map(item => item.author))];
        const dataMap = await this.$store.dispatch('displayname/batchGetDisplayName', userIds);
        for (const row of list) {
          row.author = dataMap.get(row.author)?.display_name || row.author;
        }
        this.generateExcel(list);
      } catch (err) {
        console.error(err);
      } finally {
        this.exportLoading = false;
      }
    },
    generateExcel(list = []) {
      const { isProjectDefect } = this;
      const exHeader = isProjectDefect ? [this.$t('任务')] : [];
      const exVal = isProjectDefect ? ['taskNameCn'] : [];
      const tHeader = [
        this.$t('entityId'),
        this.$t('位置'),
        this.$t('路径'),
        this.$t('函数名'),
        this.$t('圈复杂度'),
        this.$t('处理人'),
        this.$t('风险'),
        this.$t('代码提交'),
        this.$t('问题创建'),
        this.$t('修复日期'),
        this.$t('忽略日期'),
        this.$t('首次发现'),
        this.$t('最新状态'),
        ...exHeader,
      ];
      const filterVal = [
        'entityId',
        'fileName',
        'filePath',
        'functionName',
        'ccn',
        'author',
        'riskFactor',
        'latestDateTime',
        'createTime',
        'fixedTime',
        'ignoreTime',
        'createBuildNumber',
        'status',
        ...exVal,
      ];
      const data = this.formatJson(filterVal, list);
      // eslint-disable-next-line
      const prefix = isProjectDefect
        ? `${this.projectId}`
        : `${this.taskDetail.nameCn}-${this.taskDetail.taskId}-${this.toolId}-`;
      const title = `${prefix}${this.$t('风险函数')}-${new Date().toISOString()}`;
      export_json_to_excel(tHeader, data, title);
    },
    formatJson(filterVal, list) {
      return list.map(item => filterVal.map((j) => {
        if (j === 'fileName') {
          return `${this.getFileName(item.filePath)}:${item.startLines}`;
        }
        if (j === 'riskFactor') {
          return this.defectSeverityMap[item.riskFactor];
        }
        if (
          j === 'latestDateTime'
            || j === 'createTime'
            || j === 'fixedTime'
            || j === 'ignoreTime'
        ) {
          return this.formatTime(item[j], 'yyyy-MM-dd HH:mm:ss');
        }
        if (j === 'createBuildNumber') {
          return `#${item.createBuildNumber}`;
        }
        if (j === 'status') {
          return this.handleStatus(item.status, item.ignoreReasonType);
        }
        if (j === 'startLines') {
          return '';
        }
        return item[j];
      }));
    },
    handelFetchIgnoreList() {
      if (this.visitable === false) return;
      this.$store.dispatch('ignore/fetchIgnoreList').then((res) => {
        this.ignoreList = res.data;
        this.hasIgnoreList = true;
        setTimeout(this.showIgnoreTypeSelect, 1200);
        // this.showIgnoreTypeSelect()
      });
    },
    handleGuideNextStep() {
      this.$nextTick(() => {
        this.$refs.guidePopover?.hideHandler();
        this.handleNextGuide();
        localStorage.setItem('guideEnd', true);
        this.guideFlag = true;
      });
    },
    handleSetReview() {
      this.$refs.guidePopover?.hideHandler();
      let prefix = `${location.host}`;
      if (window.self !== window.top) {
        prefix = `${window.DEVOPS_SITE_URL}/console`;
      }
      const route = this.$router.resolve({
        name: 'ignoreList',
      });
      window.open(prefix + route.href, '_blank');
      this.handleGuideNextStep();
    },
    handleTableSetReview() {
      let prefix = `${location.host}`;
      if (window.self !== window.top) {
        prefix = `${window.DEVOPS_SITE_URL}/console`;
      }
      const route = this.$router.resolve({
        name: 'ignoreList',
      });
      window.open(prefix + route.href, '_blank');
      localStorage.setItem('guide2End', true);
      document.body.click();
    },
    handleTableGuideNextStep() {
      document.body.click();
      localStorage.setItem('guide2End', true);
    },
    handleNextGuide() {
      if (!localStorage.getItem('guide2End')) {
        const index = this.defectList.findIndex(item => item.status === 1);
        document.getElementsByClassName('guide-icon')[index]?.click();
        setTimeout(() => {
          document.getElementsByClassName('guide-flag')[0]?.click();
        }, 200);
      }
    },
    handleToIgnoreList() {
      if (!this.isAddingIgnore) {
        this.handleSetReview();
        this.isAddingIgnore = true;
      } else {
        this.isAddingIgnore = false;
        this.handelFetchIgnoreList();
      }
    },
    handleGetNotifyDate(notify) {
      let str = '';
      if (notify.notifyDayOfWeeks && notify.notifyDayOfWeeks.length) {
        const {
          notifyMonths,
          notifyWeekOfMonths,
          notifyDayOfWeeks,
          everyMonth,
          everyWeek,
        } = notify;
        const monthsTextMap = notifyMonths.map(i => this.monthsStrMap[i]);
        const weekOfMonthTextMap = notifyWeekOfMonths.map(i => this.weekOfMonthsStrMap[i]);
        const dayTextMap = notifyDayOfWeeks.map(i => this.dayOfWeekMap[i]);
        if (everyMonth && everyWeek && notifyDayOfWeeks.length === 7) {
          str = this.$t('每天');
        } else if (everyMonth && everyWeek && notifyDayOfWeeks.length) {
          str = this.$t('每个月的每周的') + dayTextMap.join('、');
        } else if (
          everyMonth
          && notifyWeekOfMonths.length
          && notifyDayOfWeeks.length
        ) {
          str = this.$t('每个月的')
            + weekOfMonthTextMap.join('、')
            + this.$t('的')
            + dayTextMap.join('、');
        } else if (
          notifyMonths.length
          && everyWeek
          && notifyDayOfWeeks.length
        ) {
          str = this.$t('每年的')
            + monthsTextMap.join('、')
            + this.$t('每周的')
            + dayTextMap.join('、');
        } else if (
          notifyMonths.length
          && notifyWeekOfMonths.length
          && notifyDayOfWeeks.length
        ) {
          str = this.$t('每年的')
            + monthsTextMap.join('、')
            + this.$t('的')
            + weekOfMonthTextMap.join('、')
            + this.$t('的')
            + dayTextMap.join('、');
        }
      }
      return str;
    },
    /**
     * 取消忽略并标记处理
     */
    async handleRevertIgnoreAndMark(entityId) {
      await this.handleIgnore('RevertIgnore', false, entityId);
      setTimeout(() => {
        this.handleMark(1, false, entityId);
      }, 500);
    },
    /**
     * 取消忽略并提单
     */
    async handleRevertIgnoreAndCommit(entityId) {
      await this.handleIgnore('RevertIgnore', false, entityId);
      setTimeout(() => {
        this.handleCommit('commit', false, entityId);
      }, 500);
    },
    /**
     * 修改忽略类型
     */
    handleChangeIgnoreType(row) {
      this.isSelectAll = 'N';
      this.isRowChangeIgnoreType = true;
      this.operateParams.ignoreReason = row.ignoreReason;
      this.operateParams.ignoreReasonType = row.ignoreReasonType;
      this.handleIgnore('ChangeIgnoreType', false, row.entityId);
    },
    /**
     * 跳转至设置-路径屏蔽
     */
    handleToPathShield() {
      const routeParams = { ...this.$route.params };
      const { href } = this.$router.resolve({
        name: 'task-settings',
        params: routeParams,
        query: {
          panel: 'ignore',
        },
      });
      window.open(href, '_blank');
    },
    async fetchTaskList() {
      this.taskList = (await this.$store.dispatch('defect/getTaskList')) || [];
    },
    copy(file) {
      const input = document.createElement('input');
      document.body.appendChild(input);
      input.setAttribute('value', file);
      input.select();
      document.execCommand('copy');
      document.body.removeChild(input);
      this.$bkMessage({ theme: 'success', message: this.$t('复制成功') });
    },
    handleCloseAuthorDialog() {
      this.authorEditDialogVisible = false;
      this.handleClearWindowAlert();
    },
  },
};
</script>

<style>
@import url('./codemirror.css');
</style>

<style lang="postcss" scoped>
@import url('../../css/variable.css');
@import url('../../css/mixins.css');
@import url('./defect-list.css');

.ccn-list {
  margin: 16px 20px 0 16px;

  &.project-defect {
    margin: 0 40px;
    border: 1px solid $borderColor;

    .main-container {
      padding: 16px 20px 0 16px;
    }
  }
}

.breadcrumb {
  padding: 0 !important;

  .breadcrumb-name {
    background: white;
  }
}

.main-container {
  /* padding: 20px 33px 0!important;
        margin: 0 -13px!important; */

  /* border-top: 1px solid #dcdee5; */
  margin: 0 !important;
  background: white;
}

.cc-selected {
  float: left;
  height: 42px;
  padding-right: 10px;
  font-size: 12px;
  line-height: 32px;
  color: #333;
}

.file-list-table {
  >>> .list-row {
    cursor: pointer;

    &.grey-row {
      color: #c3cdd7;

      .color-major,
      .color-minor,
      .color-info {
        color: #c3cdd7;
      }
    }
  }
}

>>> .checkbox-group {
  font-size: 14px;

  .popover {
    position: relative;
    top: 1px;
    font-size: 16px;
  }

  .bk-checkbox-text {
    font-size: 12px;
  }
}

>>> .bk-date-picker {
  width: 298px;
}

.filepath-dropdown-content {
  color: #737987;

  .content-hd {
    margin: 0 16px 16px;
  }

  .content-bd {
    width: 480px;
    height: 370px;
    margin: 16px;
    overflow: auto;
  }

  .content-ft {
    position: relative;
    padding: 12px 0;
    text-align: center;
    border-top: 1px solid #ded8d8;

    .clear-btn {
      position: absolute;
      right: 8px;
    }
  }

  >>> .bk-tree .node-icon {
    margin: 0 4px;
  }

  >>> .bk-tree .tree-drag-node .tree-expanded-icon {
    margin: 0 4px;
  }
}

.filepath-name {
  display: inline-block;
  float: left;
  width: 200px;
  text-align: left;

  @mixin ellipsis;

  & + .bk-icon {
    right: 10px;
  }
}

.icon-mark {
  color: #53cad1;

  &.re-mark {
    color: #facc48;
  }
}

.code-fullscreen {
  display: flex;

  &.full-active {
    padding-top: 30px;
  }

  .toggle-full-icon {
    position: absolute;
    top: 11px;
    right: 38px;
    color: #979ba5;
    cursor: pointer;
  }

  .col-main {
    flex: 1;
    max-width: calc(100% - 250px);
  }

  .col-aside {
    width: 240px;
    padding: 4px 20px;
    margin-left: 16px;
    background: #f0f1f5;
    flex: none;
  }

  .file-bar {
    height: 36px;
    margin-top: 4px;

    .filemeta {
      position: relative;
      top: -4px;
      padding-left: 8px;
      font-size: 12px;
      white-space: nowrap;
      border-left: 4px solid #3a84ff;

      .copy-icon {
        cursor: pointer;
      }

      .filename {
        font-size: 16px;
      }

      .filepath {
        display: inline-block;
        width: 700px;
        margin-left: 8px;
        line-height: 24px;
        vertical-align: bottom;

        @mixin ellipsis;
      }
    }
  }

  .toggle-file {
    display: flex;
    margin: 10px 0;
    text-align: center;
    justify-content: space-between;
  }

  .operate-section {
    height: 100%;
  }

  .basic-info {
    height: calc(100% - 60px);
    max-height: calc(100vh - 190px);
    padding-right: 20px;
    margin-right: -29px;
    overflow-y: scroll;

    &.full-screen-info {
      max-height: calc(100vh - 120px);
    }

    .title {
      font-size: 14px;
      color: #313238;
    }

    .block {
      padding: 5px 0;
      border-bottom: 1px dashed #c4c6cc;

      &:last-of-type {
        border-bottom: 0;
      }

      .item {
        display: flex;
        padding: 5px 0;

        dt {
          width: 90px;
          flex: none;
        }

        dd {
          color: #313238;
          word-break: break-all;

          &.small {
            width: 80px;
          }
        }

        a {
          color: #313238;
          word-break: break-all;
        }

        &.ignore {
          display: block;
        }

        .item-button {
          width: 200px;
        }
      }

      .cc-mark {
        width: 114px;
        height: 23px;
        padding: 0 8px;
        margin-left: 27px;
        line-height: 23px;
        background: white;
        border-radius: 12px;
      }
    }
  }
}

#codeViewerInDialog {
  width: 100%;
  font-size: 14px;
  border: 1px solid #eee;
  border-right: 0;
  border-left: 0;
}

.un-full-code-viewer {
  height: calc(100vh - 164px);
}

.full-code-viewer {
  height: calc(100vh - 100px);
}

.reason-type-list {
  padding: 20px;
  background: #fafbfd;

  .ignore-list {
    max-height: 250px;
    padding-left: 2px;
    overflow-y: scroll;
  }

  .reason-type-header {
    line-height: 26px;
  }
}

/deep/ .bk-radio-text {
  display: inline-flex;
  width: 95%;
}

.notify-tips {
  display: inline-block;
  width: 360px;
  margin-left: 15px;
  overflow: hidden;
  color: #979ba5;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1%;
}

>>> .bk-label {
  font-size: 12px;
}

.table-append-loading {
  padding: 12px 0;
  text-align: center;
}

.handle-menu-tips {
  text-align: center;

  .entry-link {
    padding: 4px 0;
    font-size: 12px;
    color: $fontWeightColor;
    cursor: pointer;

    &.disabled {
      color: $borderColor;
      cursor: not-allowed;

      &:hover {
        color: $borderColor;

        > a {
          color: $borderColor;
        }
      }
    }

    > a {
      color: $fontWeightColor;
    }

    &:hover {
      color: $primaryColor;

      > a {
        color: $primaryColor;
      }
    }
  }

  .entry-link-allowed {
    padding: 4px 0;
    font-size: 12px;
    color: $fontWeightColor;
    cursor: not-allowed;

    &:hover {
      color: $borderColor;

      > a {
        color: $borderColor;
      }
    }
  }
}

.bk-date-picker {
  width: 300px;
}

>>> .bk-table {
  .mark-row {
    .cell {
      padding: 0;
    }
  }
}

.cc-radio {
  display: block;
  padding-bottom: 15px;
}

.cc-keyboard {
  float: right;
  height: 42px;
  font-size: 12px;
  line-height: 26px;
  color: #333;

  .cc-button {
    font-size: 12px;
    color: #699df4;
  }
}

.bk-form-radio {
  margin-right: 15px;

  >>> .bk-radio-text {
    font-size: 12px;
  }
}

.cc-table {
  position: relative;

  .cc-operate {
    display: inline-block;

    .cc-operate-buttons {
      display: flex;

      .cc-operate-button {
        margin-left: 10px;
      }
    }
  }
}

.operate-footer {
  text-align: center;
}

.main-container::-webkit-scrollbar {
  width: 0;
}

.excel-download {
  padding-right: 10px;
  line-height: 32px;
  cursor: pointer;

  &:hover {
    color: #3a84ff;
  }
}

>>> .bk-button .bk-icon {
  .loading {
    color: #3a92ff;
  }
}

::v-deep .bk-dialog-header {
  padding: 3px 24px 0 !important;
}
</style>
<style lang="postcss">
.file-detail-dialog {
  .bk-dialog {
    min-width: 960px;
  }
}

.guide-item {
  color: #3a84ff;
  background-color: #eaf3ff;
}

.tippy-tooltip.dot-menu-theme {
  width: 280px !important;
  height: 100px;
  padding: 10px;
  color: #fff;
  background-color: #3b91fb;

  .guide-btn {
    position: absolute;
    right: 18px;
    bottom: 15px;
  }

  .btn-item {
    padding: 2px 5px;
    color: #3b91fb;
    cursor: pointer;
    background-color: #fff;
    border-radius: 3px;
  }
}

.search-form.main-form.collapse {
  height: 48px;
  overflow: hidden;
}
</style>
