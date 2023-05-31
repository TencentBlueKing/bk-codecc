<template>
  <div v-bkloading="{ isLoading: !mainContentLoading && contentLoading, opacity: 0.3 }">
    <section class="defect-list" :class="{ 'project-defect': isProjectDefect }"
             v-if="taskDetail.enableToolList.find(item => item.toolName !== 'CCN' && item.toolName !== 'DUPC') || isFromOverview || isProjectDefect">
      <div class="breadcrumb">
        <div class="breadcrumb-name">
          <defect-panel v-if="isProjectDefect" />
          <chart-panel
            v-else
            :tool-name="toolNameStr"
            :tool-list="toolList" />
          <div class="tab-extra-icon" :class="{ 'is-project': isProjectDefect }">
            <span class="mr20">
              <bk-link theme="primary" @click="reload">
                <span class="f12">{{ $t('重置筛选条件') }}</span>
              </bk-link>
            </span>
            <span class="filter-collapse-icon mr20" :class="{ 'mac-filter-search-icon': isMac }">
              <bk-popover ext-cls="handle-menu" ref="handleMenu" theme="light" placement="left-start" trigger="click">
                <i class="bk-icon codecc-icon icon-filter-collapse"
                   :class="isSearchDropdown ? 'icon-filter-collapse' : 'icon-filter-expand'"
                   v-bk-tooltips="isSearchDropdown ? $t('收起筛选项') : $t('展开筛选项')"
                   @click="toggleSearch"></i>
              </bk-popover>
            </span>
            <span class="filter-search-icon" :class="{ 'mac-filter-search-icon': isMac }">
              <bk-popover ext-cls="handle-menu" ref="handleMenu" theme="light" placement="left-start" trigger="click">
                <i class="bk-icon codecc-icon icon-filter-set" v-bk-tooltips="$t('设置筛选条件')"></i>
                <div slot="content">
                  <filter-search-option
                    :default-option="defaultOption"
                    :custom-option="customOption"
                    @selectAll="handleSelectAllSearchOption"
                    @confirm="handleConfirmSearchOption" />
                </div>
              </bk-popover>
            </span>
            <span class="excel-icon pl20">
              <bk-button style="border: 0" v-if="exportLoading" icon="loading" :disabled="true" :title="$t('导出Excel')"></bk-button>
              <span v-else class="codecc-icon icon-export-excel excel-download" @click="downloadExcel" v-bk-tooltips="$t('导出Excel')"></span>
            </span>
          </div>
        </div>
      </div>

      <div class="main-container" ref="mainContainer">
        <div class="main-content-inner main-content-list">
          <div class="search-content-inner" :class="{ 'collapse': !isSearchDropdown }">
            <bk-form ref="bkForm" :label-width="65" class="search-form main-form">
              <container class="cc-container">
                <div class="cc-col" v-if="isProjectDefect">
                  <bk-form-item :label="$t('任务')">
                    <bk-select searchable v-model="taskIdList" multiple>
                      <bk-option
                        v-for="item in taskList"
                        :key="item.taskId"
                        :id="item.taskId"
                        :name="item.nameCn">
                      </bk-option>
                    </bk-select>
                  </bk-form-item>
                </div>
                <div class="cc-col">
                  <bk-form-item :label="$t('维度')">
                    <bk-select v-model="dimension" multiple>
                      <bk-option
                        v-for="item in dimenList"
                        :key="item.key"
                        :id="item.key"
                        :name="item.name">
                      </bk-option>
                    </bk-select>
                  </bk-form-item>
                </div>
                <div class="cc-col">
                  <bk-form-item :label="$t('工具')">
                    <bk-select multiple v-model="searchParams.toolName">
                      <bk-option
                        v-for="option in toolList"
                        :key="option.toolName"
                        :id="option.toolName"
                        :name="option.toolDisplayName">
                      </bk-option>
                    </bk-select>
                  </bk-form-item>
                </div>
                <div class="cc-col">
                  <bk-form-item :label="$t('规则集')">
                    <bk-select v-model="searchParams.checkerSet" searchable>
                      <bk-option
                        v-for="checkerSet in searchFormData.checkerSetList"
                        :key="checkerSet.checkerSetId"
                        :id="checkerSet.checkerSetId"
                        :name="checkerSet.checkerSetName">
                      </bk-option>
                    </bk-select>
                  </bk-form-item>
                </div>
                <div class="cc-col" v-show="allRenderColumnMap.checker">
                  <bk-form-item :label="$t('规则')">
                    <bk-select v-model="searchParams.checker" searchable :loading="selectLoading.otherParamsLoading">
                      <bk-option-group
                        v-for="group in searchFormData.checkerList"
                        :name="group.typeName"
                        :show-count="false"
                        :key="group">
                        <bk-option
                          v-for="checker in group.checkers"
                          :key="checker"
                          :id="checker"
                          :name="checker">
                        </bk-option>
                      </bk-option-group>
                    </bk-select>
                  </bk-form-item>
                </div>
                <div class="cc-col" v-show="allRenderColumnMap.author">
                  <bk-form-item :label="$t('处理人')">
                    <bk-select v-model="searchParams.author" searchable :loading="selectLoading.otherParamsLoading">
                      <bk-option
                        v-for="author in searchFormData.authorList"
                        :key="author"
                        :id="author"
                        :name="author">
                      </bk-option>
                    </bk-select>
                    <bk-button v-if="!isProjectDefect" @click="toChangeMember" :title="$t('批量修改问题处理人')" :text="true" class="change-handler">
                      <i class="codecc-icon icon-handler-2"></i>
                    </bk-button>
                  </bk-form-item>
                </div>
                <div class="cc-col" v-show="allRenderColumnMap.daterange">
                  <bk-form-item :label="$t('日期')">
                    <bk-date-picker v-model="searchParams.daterange" type="daterange"></bk-date-picker>
                  </bk-form-item>
                </div>
                <div class="cc-col" v-if="!isProjectDefect" v-show="allRenderColumnMap.path">
                  <bk-form-item :label="$t('路径')" class="fixed-width">
                    <bk-select v-if="selectLoading.otherParamsLoading" :loading="true"></bk-select>
                    <bk-dropdown-menu v-else @show="isFilePathDropdownShow = true" @hide="isFilePathDropdownShow = false" align="left" trigger="click" ref="filePathDropdown">
                      <bk-button type="primary" slot="dropdown-trigger">
                        <div style="font-size: 12px" class="filepath-name" :class="{ 'unselect': !searchFormData.filePathShow }" :title="searchFormData.filePathShow">{{searchFormData.filePathShow ? searchFormData.filePathShow : $t('请选择')}}</div>
                        <i :class="['bk-icon icon-angle-down', { 'icon-flip': isFilePathDropdownShow }]" style="color: #979ba5; position: absolute; right: -2px"></i>
                      </bk-button>
                      <div class="filepath-dropdown-content" slot="dropdown-content" @click="e => e.stopPropagation()">
                        <bk-tab type="unborder-card" class="create-tab" :active="tabSelect" @tab-change="changeTab">
                          <bk-tab-panel
                            v-for="(panel, index) in pathPanels"
                            v-bind="panel"
                            :key="index">
                          </bk-tab-panel>
                          <div v-show="tabSelect === 'choose'" class="create-tab-1">
                            <div>
                              <div class="content-hd">
                                <bk-input v-model="searchInput" class="search-input" :clearable="true" :placeholder="$t('搜索文件夹、问题路径名称')" @input="handleFilePathSearch"></bk-input>
                              </div>
                              <div class="content-bd" v-if="treeList.length">
                                <bk-big-tree
                                  ref="filePathTree"
                                  height="340"
                                  :style="treeWidth"
                                  :options="{ 'idKey': 'treeId' }"
                                  :show-checkbox="true"
                                  :data="treeList"
                                  :filter-method="filterMethod"
                                  :expand-icon="'bk-icon icon-folder-open'"
                                  :collapse-icon="'bk-icon icon-folder'"
                                  :has-border="true"
                                  :node-key="'name'">
                                </bk-big-tree>
                              </div>
                              <div class="content-empty" v-if="!treeList.length">
                                <empty size="small" :title="$t('无问题文件')" />
                              </div>
                            </div>
                          </div>
                          <div v-show="tabSelect === 'input'" class="create-tab-2">
                            <div class="input-info">
                              <div class="input-info-left"><i class="bk-icon icon-info-circle-shape"></i></div>
                              <div class="input-info-right"></div>
                              {{$t('搜索文件夹如P2PLive')}}<br />
                              {{$t('搜索某类文件如P2PLive下')}}
                            </div>
                            <div class="input-paths">
                              <div class="input-paths-item" v-for="(path, index) in inputFileList" :key="index">
                                <bk-input :placeholder="$t('请输入')" class="input-style" v-model="inputFileList[index]"></bk-input>
                                <span class="input-paths-icon">
                                  <i class="bk-icon icon-plus-circle-shape" @click="addPath(index)"></i>
                                  <i class="bk-icon icon-minus-circle-shape" v-if="inputFileList.length > 1" @click="cutPath(index)"></i>
                                </span>
                              </div>
                            </div>
                          </div>
                        </bk-tab>
                        <div class="content-ft">
                          <bk-button theme="primary" @click="handleFilePathConfirmClick">{{$t('确定')}}</bk-button>
                          <bk-button @click="handleFilePathCancelClick">{{$t('取消')}}</bk-button>
                          <bk-button class="clear-btn" @click="handleFilePathClearClick">{{$t('清空选择')}}</bk-button>
                        </div>
                      </div>
                    </bk-dropdown-menu>
                  </bk-form-item>
                </div>
                <div class="cc-col" v-if="!isProjectDefect">
                  <bk-form-item :label="$t('快照')">
                    <bk-select v-model="searchParams.buildId" :clearable="true" searchable :loading="selectLoading.buildListLoading">
                      <bk-option
                        v-for="item in buildList"
                        :key="item.buildId"
                        :id="item.buildId"
                        :name="`#${item.buildNum} ${item.branch} ${item.buildUser} ${formatDate(item.buildTime) || ''}${$t('触发')}`">
                        <div class="cc-ellipsis" :title="`#${item.buildNum} ${item.branch} ${item.buildUser} ${formatDate(item.buildTime) || ''}${$t('触发')}`">
                          {{`#${item.buildNum} ${item.branch} ${item.buildUser} ${formatDate(item.buildTime) || ''}${$t('触发')}`}}
                        </div>
                      </bk-option>
                    </bk-select>
                  </bk-form-item>
                </div>
                <div class="cc-col" v-show="allRenderColumnMap.status">
                  <bk-form-item :label="$t('状态')">
                    <bk-select
                      ref="statusSelect"
                      searchable
                      multiple
                      v-model="searchParams.statusUnion"
                      :key="statusTreeKey"
                      :remote-method="handleStatusRemote"
                      :tag-fixed-height="false"
                      :loading="selectLoading.statusLoading"
                      @tab-remove="handleStatusValuesChange"
                      @clear="handleStatusClear">
                      <bk-big-tree
                        :data="statusTreeData"
                        size="small"
                        show-checkbox
                        class="tree-select"
                        ref="statusTree"
                        v-if="hasCountData && hasIgnoreList"
                        :default-checked-nodes="searchParams.statusUnion"
                        :default-expand-all="!guideFlag"
                        @check-change="handleStatusCheckChange">
                        <div slot-scope="{ node, data }">
                          <div v-if="data.id === '4-24' && hasIgnoreList">
                            <bk-popover ref="guidePopover" :disabled="guideFlag" placement="right-start" :delay="2000" theme="dot-menu light">
                              <span>{{ data.name }}</span>
                              <div class="guide-content" slot="content">
                                <div style="line-height: 22px; font-weight: bold;">{{ $t('【存量问题】可在这里查看') }}</div>
                                <div>
                                  {{ $t('针对特定忽略类型可') }}
                                  <span theme="primary" class="set-tips" @click="handleSetReview">{{ $t('设置提醒') }}</span>
                                  {{ $t('，以便定期review和修复。') }}
                                </div>
                                <div class="guide-btn">
                                  <span class="btn-item" @click="handleGuideNextStep">{{ $t('知道了') }}</span>
                                </div>
                              </div>
                            </bk-popover>
                          </div>
                          <div v-else>{{data.name}}</div>
                        </div>
                      </bk-big-tree>
                    </bk-select>
                  </bk-form-item>
                </div>
                <div class="cc-col" v-show="allRenderColumnMap.severity">
                  <bk-form-item :label="$t('级别')">
                    <bk-checkbox-group v-model="searchParams.severity" class="checkbox-group">
                      <bk-checkbox
                        v-for="(value, key, index) in defectSeverityMap"
                        :value="Number(key)"
                        :key="index">
                        {{value}}(<em :class="['count', `count-${['major', 'minor', 'info'][index]}`]">{{getDefectCountBySeverity(key)}}</em>)
                      </bk-checkbox>
                    </bk-checkbox-group>
                  </bk-form-item>
                </div>
              </container>
            </bk-form>
          </div>
          <div class="cc-table" ref="ccTable" v-bkloading="{ isLoading: tableLoading && !defectDetailDialogVisiable, opacity: 0.6 }">
            <div class="cc-selected">
              <span v-if="searchParams.clusterType === 'file'">
                {{$t('已选择w文件(x问题)，共y文件(z问题)', {
                  w: isSelectAll === 'Y' ? totalCount : selectedLen,
                  x: selectedDefectCount,
                  y: totalCount,
                  z: totalDefectCount
                })}}
              </span>
              <span v-else>{{$t('已选择x问题，共y问题', { x: isSelectAll === 'Y' ? totalCount : selectedLen, y: totalCount })}}</span>
              <span v-if="gatherFile.fileCount" class="extra-file" @click="goToLog">
                {{$t('另有x个大文件y问题', { x: gatherFile.fileCount, y: gatherFile.defectCount })}}
                <bk-popover placement="top" width="300" class="popover">
                  <i class="codecc-icon icon-tips"></i>
                  <div slot="content">
                    {{$t('对于部分大文件产生的海量告警，CodeCC已将其归档。可前往工具分析记录下载文件查看详情')}}
                  </div>
                </bk-popover>
              </span>
            </div>
            <div v-if="isBatchOperationShow" class="cc-operate pb10">
              <div class="cc-operate-buttons">
                <bk-button size="small" ext-cls="cc-operate-button" @click="handleMark(1, true)" theme="primary">{{$t('标记处理')}}</bk-button>
                <bk-button size="small" ext-cls="cc-operate-button" v-if="searchParams.clusterType === 'defect'" @click="handleAuthor(2)" theme="primary">{{$t('分配')}}</bk-button>
                <!-- <bk-button size="small" ext-cls="cc-operate-button" @click="handleCommit('commit', true)" theme="primary">{{$t('提单')}}</bk-button> -->
                <bk-button size="small" ext-cls="cc-operate-button" @click="handleIgnore('RevertIgnore', true)" v-if="!searchParams.status.length || searchParams.status.includes(4)" theme="primary">
                  {{$t('取消忽略')}}
                </bk-button>
                <bk-dropdown-menu ext-cls="cc-operate-button" ref="operateDropdown" v-if="searchParams.clusterType === 'defect'" @show="isDropdownShow = true" @hide="isDropdownShow = false">
                  <bk-button size="small" slot="dropdown-trigger">
                    <span>{{$t('更多')}}</span>
                    <i :class="['bk-icon icon-angle-down', { 'icon-flip': isDropdownShow }]"></i>
                  </bk-button>
                  <div class="handle-menu-tips" slot="dropdown-content">
                    <p class="entry-link" @click.stop="handleMark(0, true)">
                      {{$t('取消标记')}}
                    </p>
                    <p v-if="taskDetail.prohibitIgnore" class="entry-link-allowed" @click.stop="handleIgnore('IgnoreDefect', true)" :title="$t('已设置禁止页面忽略，可在代码行末或上一行使用注释忽略，例如// NOCC:rule1(ignore reason)')">
                      {{$t('忽略')}}
                    </p>
                    <p v-else class="entry-link" @click.stop="handleIgnore('IgnoreDefect', true)">
                      {{$t('忽略')}}
                    </p>
                  </div>
                </bk-dropdown-menu>
              </div>
            </div>
            <div class="cc-keyboard">
              <span>{{$t('当前已支持键盘操作')}}</span>
              <bk-button text ext-cls="cc-button" @click="operateDialogVisiable = true">{{$t('如何操作？')}}</bk-button>
            </div>
            <table-defect
              v-show="isFetched"
              ref="table"
              :list="defectList"
              :prohibit-ignore="taskDetail.prohibitIgnore"
              :screen-height="screenHeight"
              :guide-flag="guideFlag"
              :file-index="fileIndex"
              :handle-mark="handleMark"
              :handle-ignore="handleIgnore"
              :handle-commit="handleCommit"
              :handle-author="handleAuthor"
              :handle-sort-change="handleSortChange"
              :handle-selection-change="handleSelectionChange"
              :to-select-all="toSelectAll"
              :handle-file-list-row-click="handleFileListRowClick"
              :is-file-list-load-more="isFileListLoadMore"
              :next-page-start-num="nextPageStartNum"
              :next-page-end-num="nextPageEndNum"
              :handle-revert-ignore-and-mark="handleRevertIgnoreAndMark"
              :handle-revert-ignore-and-commit="handleRevertIgnoreAndCommit"
              :is-project-defect="isProjectDefect"
              :handle-status="handleStatus"
              :handle-change-ignore-type="handleChangeIgnoreType">
            </table-defect>
          </div>

          <bk-dialog
            v-model="defectDetailDialogVisiable"
            :ext-cls="'file-detail-dialog'"
            :fullscreen="isFullScreen"
            :position="{ top: `${isFullScreen ? 0 : 50}` }"
            :draggable="false"
            :mask-close="false"
            :show-footer="false"
            :close-icon="false"
            width="80%">
            <detail
              ref="detail"
              :type="searchParams.clusterType"
              :list="defectList"
              :ignore-list="ignoreList"
              :prohibit-ignore="taskDetail.prohibitIgnore"
              :is-loading.sync="detailLoading"
              :is-full-screen.sync="isFullScreen"
              :visiable="defectDetailDialogVisiable"
              :file-index="fileIndex"
              :entity-id="defectDetailSearchParams.entityId"
              :total-count="totalCount"
              :current-file="currentFile"
              :handle-mark="handleMark"
              :handle-coment="handleComent"
              :handle-commit="handleCommit"
              :delete-comment="deleteComment"
              :handle-ignore="handleIgnore"
              :handle-author="handleAuthor"
              :build-num="buildNum"
              :lint-detail="lintDetail"
              :handle-file-list-row-click="handleFileListRowClick"
              :is-file-list-load-more="isFileListLoadMore"
              :next-page-start-num="nextPageStartNum"
              :next-page-end-num="nextPageEndNum"
              :handle-revert-ignore-and-mark="handleRevertIgnoreAndMark"
              :handle-revert-ignore-and-commit="handleRevertIgnoreAndCommit"
              :handle-change-ignore-type="handleChangeIgnoreType"
              :is-project-defect="isProjectDefect"
              @scrollLoadMore="scrollLoadMore"
              @closeDetail="defectDetailDialogVisiable = false">
            </detail>
          </bk-dialog>

        </div>
      </div>
      <bk-dialog
        v-model="authorEditDialogVisiable"
        width="560"
        theme="primary"
        :mask-close="false"
        header-position="left"
        :title="operateParams.changeAuthorType === 1 ? $t('修改问题处理人') : $t('批量修改问题处理人')">
        <div class="author-edit">
          <div class="tips" v-if="operateParams.changeAuthorType === 3"><i class="bk-icon icon-info-circle"></i>{{$t('原处理人所有函数都将转给新处理人')}}</div>
          <bk-form :model="operateParams" :label-width="130" class="search-form">
            <bk-form-item v-if="operateParams.changeAuthorType !== 2"
                          property="sourceAuthor"
                          :label="$t('原处理人')">
              <bk-input v-model="operateParams.sourceAuthor" :disabled="operateParams.changeAuthorType === 1" style="width: 290px;"></bk-input>
            </bk-form-item>
            <bk-form-item :label="$t('新处理人')">
              <bk-tag-input allow-create :max-data="1" v-model="operateParams.targetAuthor" style="width: 290px;"></bk-tag-input>
            </bk-form-item>
          </bk-form>
        </div>
        <div slot="footer">
          <bk-button
            type="button"
            theme="primary"
            :disabled="(operateParams.changeAuthorType === 3) || !operateParams.targetAuthor"
            :loading="authorEditDialogLoading"
            @click.native="handleAuthorEditConfirm">
            {{operateParams.changeAuthorType === 1 ? $t('确定') : $t('批量修改')}}
          </bk-button>
          <bk-button
            theme="primary"
            type="button"
            :disabled="authorEditDialogLoading"
            @click.native="authorEditDialogVisiable = false">
            {{$t('取消')}}
          </bk-button>
        </div>
      </bk-dialog>

      <bk-dialog
        v-model="ignoreReasonDialogVisiable"
        width="560"
        :position="ignoreDialogPositionConfig"
        theme="primary"
        :mask-close="false"
        header-position="left"
        :title="ignoreReasonDialogTitle"
        @cancel="handleIgnoreCancel">
        <div class="reason-type-list">
          <div class="reason-type-header mb20">
            {{ $t('忽略类型') }}
            <span class="fr">
              <bk-button size="small" icon="plus" class="mr10" @click="handleSetReview">{{$t('新增类型')}}</bk-button>
              <bk-button size="small" @click="handelFetchIgnoreList"><i class="codecc-icon icon-refresh-2"></i></bk-button>
            </span>
          </div>
          <bk-form :model="operateParams" :label-width="0" class="search-form">
            <bk-form-item property="ignoreReason">
              <bk-radio-group v-model="operateParams.ignoreReasonType" class="ignore-list">
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
                  <span v-else-if="operateParams.ignoreReasonType === ignore.ignoreTypeId && ignore.notify.notifyDayOfWeeks.length" class="notify-tips">
                    <span class="f12" v-bk-tooltips="{ content: formatTime(ignore.nextNotifyTime, 'M月D日') + '（' + handleGetNotifyDate(ignore.notify) + '）' + $t('提醒') }">
                      <i class="codecc-icon icon-time" style="color: #979ba5;"></i>
                      {{ formatTime(ignore.nextNotifyTime, 'M月D日') }} （{{ handleGetNotifyDate(ignore.notify) }}） {{ $t('提醒') }}
                    </span>
                  </span>
                </bk-radio>
              </bk-radio-group>
            </bk-form-item>
            <bk-form-item property="ignoreReason" :required="ignoreReasonRequired">
              <span>{{ $t('忽略原因') }}</span>
              <bk-input :type="'textarea'" :maxlength="255" v-model="operateParams.ignoreReason"></bk-input>
            </bk-form-item>
          </bk-form>
        </div>
        <div slot="footer">
          <bk-button
            theme="primary"
            :disabled="ignoreReasonAble"
            @click.native="handleIgnoreConfirm"
          >
            {{$t('确定')}}
          </bk-button>
          <bk-button
            theme="primary"
            @click.native="handleIgnoreCancel">
            {{$t('取消')}}
          </bk-button>
        </div>
      </bk-dialog>
      <operate-dialog :visiable.sync="operateDialogVisiable"></operate-dialog>
      <Record :visiable.sync="show" :data="this.$route.name" />
      <bk-dialog
        v-model="changeHandlerVisiable"
        theme="primary"
        :mask-close="false"
        :title="$t('跳转提示')">
        <span>{{$t('配置处理人转换，可将原处理人当前和未来的问题都分配给新处理人。')}}</span>
        <div style="padding-bottom: 19px;"></div>
        <bk-checkbox
          :true-value="true"
          :false-value="false"
          v-model="memberNeverShow"
          :value="true">
          {{$t('不再提示')}}
        </bk-checkbox>
        <template slot="footer">
          <bk-button theme="primary" @click="toLogs()">{{$t('去配置')}}</bk-button>
          <bk-button @click="changeHandlerVisiable = false">{{$t('取消')}}</bk-button>
        </template>
      </bk-dialog>
      <bk-dialog
        v-model="commentDialogVisiable"
        width="560"
        theme="primary"
        :mask-close="false"
        header-position="left"
        :title="$t('问题评论')">
        <div class="pd10 pr50">
          <bk-form :model="commentParams" :label-width="30" class="search-form">
            <bk-form-item property="comment" :required="true">
              <bk-input :placeholder="$t('请输入你的评论内容')" :type="'textarea'" :maxlength="200" v-model="commentParams.comment"></bk-input>
            </bk-form-item>
          </bk-form>
        </div>
        <div slot="footer">
          <bk-button
            theme="primary"
            :disabled="!commentParams.comment"
            @click.native="handleCommentConfirm"
          >
            {{$t('确定')}}
          </bk-button>
          <bk-button
            theme="primary"
            @click.native="commentDialogVisiable = false"
          >
            {{$t('取消')}}
          </bk-button>
        </div>
      </bk-dialog>
    </section>
    <div class="defect-list" v-else>
      <div class="main-container large boder-none">
        <div class="no-task">
          <empty title="" :desc="$t('CodeCC集成了十余款工具，支持检查代码缺陷、安全漏洞、代码规范等问题')">
            <template v-slot:action>
              <bk-button size="large" theme="primary" @click="addTool({ from: 'lint' })">{{$t('配置规则集')}}</bk-button>
            </template>
          </empty>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  import _ from 'lodash'
  import { bus } from '@/common/bus'
  import { format } from 'date-fns'
  import { mapGetters, mapState } from 'vuex'
  import { getClosest, toggleClass } from '@/common/util'
  // eslint-disable-next-line
  import { export_json_to_excel } from 'vendor/export2Excel'
  import util from '@/mixins/defect-list'
  import defectCache from '@/mixins/defect-cache'
  import detail from './detail'
  import tableDefect from './table-defect'
  import Empty from '@/components/empty'
  import Record from '@/components/operate-record/index'
  import filterSearchOption from './filter-search-option'
  import OperateDialog from '@/components/operate-dialog'
  import ChartPanel from './components/chart-panel.vue'
  import DefectPanel from './components/defect-panel.vue'
  // import CodeMirror from '@/common/codemirror'

  // 搜索过滤项缓存
  const SEARCH_OPTION_CACHE = 'search_option_columns_defect'

  export default {
    components: {
      Record,
      Empty,
      tableDefect,
      detail,
      filterSearchOption,
      OperateDialog,
      ChartPanel,
      DefectPanel,
    },
    mixins: [util, defectCache],
    data() {
      const isProjectDefect = this.$route.name === 'project-defect-list'
      this.getDefaultOption = () => (
        isProjectDefect
          ? [
            { id: 'task', name: this.$t('任务'), isChecked: true },
            { id: 'dimension', name: this.$t('维度'), isChecked: true },
            { id: 'toolName', name: this.$t('工具'), isChecked: true },
            { id: 'checkerSet', name: this.$t('规则集'), isChecked: true },
          ]
          : [
            { id: 'dimension', name: this.$t('维度'), isChecked: true },
            { id: 'toolName', name: this.$t('工具'), isChecked: true },
            { id: 'checkerSet', name: this.$t('规则集'), isChecked: true },
            { id: 'buildId', name: this.$t('快照'), isChecked: true },
          ]
      )

      this.getCustomOption = val => (
        isProjectDefect
          ? [
            { id: 'checker', name: this.$t('规则'), isChecked: val },
            { id: 'author', name: this.$t('处理人'), isChecked: val },
            { id: 'daterange', name: this.$t('日期'), isChecked: val },
            { id: 'status', name: this.$t('状态'), isChecked: val },
            { id: 'severity', name: this.$t('级别'), isChecked: val },
          ]
          : [
            { id: 'checker', name: this.$t('规则'), isChecked: val },
            { id: 'author', name: this.$t('处理人'), isChecked: val },
            { id: 'daterange', name: this.$t('日期'), isChecked: val },
            { id: 'path', name: this.$t('路径'), isChecked: val },
            { id: 'status', name: this.$t('状态'), isChecked: val },
            { id: 'severity', name: this.$t('级别'), isChecked: val },
          ])

      const { query } = this.$route
      const { toolId, taskId } = this.$route.params
      let status = [1]
      let statusUnion = [1] // 状态和忽略类型组合的新字段
      if (query.status) {
        status = this.numToArray(query.status)
        statusUnion = status.slice()
      }
      let ignoreReasonTypes = []
      if (query.ignoreTypeId) {
        ignoreReasonTypes = query.ignoreTypeId.split(',').map(i => Number(i))
        const statusIndex = statusUnion.findIndex(item => item === 4)
        if (statusIndex !== -1) {
          statusUnion.splice(statusIndex, 1)
        }
        const typesArr = ignoreReasonTypes.map(item => `4-${item}`)
        statusUnion = statusUnion.concat(typesArr)
      }

      return {
        taskId,
        contentLoading: false,
        detailLoading: false,
        toolPattern: 'LINT',
        panels: [
          { name: 'defect', label: this.$t('问题管理') },
          { name: 'report', label: this.$t('数据报表') },
        ],
        tableLoading: false,
        isSearch: false,
        defectSeverityMap: {
          1: this.$t('严重'),
          2: this.$t('一般'),
          4: this.$t('提示'),
        },
        defectSeverityDetailMap: {
          1: this.$t('严重'),
          2: this.$t('一般'),
          3: this.$t('提示'),
        },
        defectCountMap: {
          1: 'seriousCount',
          2: 'normalCount',
          4: 'promptCount',
        },
        dimension: query.dimension ? query.dimension.split(',') : query.dimension,
        listData: {
          defectList: {
            records: [],
            count: 0,
          },
        },
        lintDetail: {
          lintDefectDetailVO: {},
        },
        searchFormData: {
          authorList: [],
          checkerList: [],
          checkerSetList: [{}],
          filePathTree: {},
          filePathShow: this.handleFileList(query.fileList).join(';'),
          existCount: 0, // 待修复
          fixCount: 0, // 已修复
          ignoreCount: 0, // 已忽略
          maskCount: 0, // 已屏蔽
          newCount: 0,
          historyCount: 0,
          seriousCount: 0,
          normalCount: 0,
          promptCount: 0,
        },
        searchParams: {
          taskId: this.$route.params.taskId,
          toolName: toolId ? toolId.split(',') : toolId,
          dimension: query.dimension || '',
          checker: query.checker || '',
          checkerSet: query.checkerSet || '',
          author: query.author,
          severity: this.numToArray(query.severity),
          status,
          statusUnion,
          buildId: query.buildId ? query.buildId : '',
          fileList: this.handleFileList(query.fileList),
          daterange: [query.startTime, query.endTime],
          clusterType: query.clusterType || 'defect',
          sortField: query.sortField || 'fileName',
          sortType: 'ASC',
          ignoreReasonTypes,
          pageNum: 1,
          pageSize: 100,
          showTaskNameCn: isProjectDefect,
        },
        defectDetailSearchParams: {
          sortField: '',
          sortType: '',
          pattern: '',
          filePath: '',
          toolName: toolId || '',
          entityId: undefined,
        },
        isFilePathDropdownShow: false,
        isFileListLoadMore: false,
        isDefectListLoadMore: false,
        defectDetailDialogVisiable: false,
        authorEditDialogVisiable: false,
        ignoreReasonDialogVisiable: false,
        commentDialogVisiable: false,
        pagination: {
          current: 1,
          count: 1,
          limit: 50,
        },
        totalCount: 0,
        fileIndex: 0,
        selectedDefectCount: 0,
        totalDefectCount: 0,
        show: false,
        isBatchOperationShow: false,
        searchInput: '',
        emptyText: this.$t('未选择文件'),
        operateParams: {
          toolName: toolId,
          dimension: query.dimension || '',
          ignoreReasonType: '',
          ignoreReason: '',
          changeAuthorType: 1, // 1:单个修改处理人，2:批量修改处理人，3:固定修改处理人
          sourceAuthor: [],
          targetAuthor: [],
        },
        isDropdownShow: false,
        operateDialogVisiable: false,
        changeHandlerVisiable: false,
        screenHeight: 336,
        selectedLen: 0,
        memberNeverShow: false,
        newDefectJudgeTime: '',
        buildList: [],
        isSelectAll: '',
        isFullScreen: true,
        isFetched: false,
        commentParams: {
          toolName: toolId,
          defectId: '',
          commentId: '',
          singleCommentId: '',
          userName: this.$store.state.user.username,
          comment: '',
        },
        gatherFile: {},
        exportLoading: false,
        selectLoading: {
          otherParamsLoading: false,
          statusLoading: false,
          buildListLoading: false,
        },
        defaultOption: this.getDefaultOption(),
        customOption: this.getCustomOption(true),
        selectedOptionColumn: [],
        ignoreList: [],
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
        isShowDetailDialog: true,
        isRowChangeIgnoreType: false,
        guideFlag: false,
        isSearchDropdown: true,
        hasCountData: false,
        hasIgnoreList: false,
        statusTreeKey: 1,
        taskIdList: isProjectDefect ? [] : [Number(taskId)],
        isProjectDefect,
      }
    },
    computed: {
      ...mapGetters(['mainContentLoading']),
      ...mapState('tool', {
        toolMap: 'mapList',
      }),
      ...mapState('task', {
        taskDetail: 'detail',
      }),
      dimensionStr() {
        const { dimension = '' } = this
        return typeof dimension === 'string' ? dimension : dimension.join(',')
      },
      toolNameStr() {
        const { toolName = '' } = this.searchParams
        return typeof toolName === 'string' ? toolName : toolName.join(',')
      },
      toolName() {
        return this.searchParams.toolName || []
      },
      defectList() {
        return this.listData.defectList.records
      },
      currentFile() {
        return this.lintDetail.lintDefectDetailVO
      },
      statusTreeData() {
        const { existCount, fixCount, ignoreCount, maskCount } = this.searchFormData
        const list = this.ignoreList.map(item => (
          { id: `4-${item.ignoreTypeId}`, name: `${this.$t('已忽略')}-${item.name}` }
        ))
        const statusList = [
          {
            id: 1,
            name: `${this.$t('待修复')}(${existCount || 0})`,
          },
          {
            id: 2,
            name: `${this.$t('已修复')}(${fixCount || 0})`,
          },
          {
            id: 4,
            name: `${this.$t('已忽略')}(${ignoreCount || 0})`,
            children: list,
          },
          {
            id: 8,
            name: `${this.$t('已屏蔽')}(${maskCount || 0})`,
          },
        ]
        this.statusTreeKey += 1
        return statusList
      },
      nextPageStartNum() {
        return ((this.searchParams.pageNum - 1) * this.searchParams.pageSize) + 1
      },
      nextPageEndNum() {
        let nextPageEndNum = this.searchParams.pageNum * this.searchParams.pageSize
        nextPageEndNum = this.totalCount < nextPageEndNum ? this.totalCount : nextPageEndNum
        return nextPageEndNum
      },
      ignoreReasonRequired() {
        return this.operateParams.ignoreReasonType === 4
      },
      ignoreReasonAble() {
        if (this.operateParams.ignoreReasonType === 4 && this.operateParams.ignoreReason === '') {
          return true
        }
        return !this.operateParams.ignoreReasonType
      },
      searchParamsWatch() {
        return JSON.parse(JSON.stringify(this.searchParams))
      },
      cacheConfig() {
        const cacheKey = this.searchParams.clusterType === 'file' ? 'entityId' : 'defectId'
        return { cacheKey }
      },
      statusRelevantParams() {
        const { checkerSet, checker, author, fileList, buildId, daterange } = this.searchParams
        return { checkerSet, checker, author, fileList, buildId, daterange }
      },
      severityRelevantParams() {
        const { status } = this.searchParams
        return { ...this.statusRelevantParams, status }
      },
      buildNum() {
        const { buildList } = this
        const buildItem = buildList?.find(item => item.buildId === this.searchParams.buildId) || {}
        return buildItem.buildNum
      },
      allRenderColumnMap() {
        return this.selectedOptionColumn.reduce((result, item) => {
          result[item.id] = item.isChecked
          return result
        }, {})
      },
      isMac() {
        return /macintosh|mac os x/i.test(navigator.userAgent)
      },
      ignoreReasonDialogTitle() {
        if (this.isRowChangeIgnoreType) {
          return this.$t('忽略')
        }
        return (this.isSelectAll === 'Y' ? this.totalCount : this.selectedLen) > 1
          ? `${this.$t('忽略')}（
          ${this.$t('共x个问题', { num: this.isSelectAll === 'Y' ? this.totalCount : this.selectedLen })}）` : this.$t('忽略')
      },
      ignoreDialogPositionConfig() {
        const { clientHeight } = document.body
        const config = {
          top: '200',
        }
        if (clientHeight <= 1000 && clientHeight > 900) {
          config.top = '150'
        } else if (clientHeight <= 900) {
          config.top = '100'
        }
        return config
      },
    },
    watch: {
      // 监听查询参数变化，则获取列表
      searchParamsWatch: {
        handler(newVal, oldVal) {
          // 切换快照时，重新获取工具列表
          if (newVal.buildId !== oldVal.buildId) {
            this.fetchListTool()
          }
          // 比如在第二页，筛选条件发生变化，要回到第一页
          if (newVal.pageNum !== 1 && newVal.pageNum === oldVal.pageNum) {
            this.searchParams.pageNum = 1
            this.$refs.table.$refs.fileListTable.$refs.bodyWrapper.scrollTo(0, 0)
            return
          }

          // 筛选状态，先特殊处理
          if (!_.isEqual(newVal.statusUnion, oldVal.statusUnion)) {
            const status = []
            let ignoreTypes = []
            const hasIgnore = newVal.statusUnion.includes(4)
            newVal.statusUnion.forEach((item) => {
              if (typeof(item) === 'string' && item.startsWith('4-')) {
                ignoreTypes.push(Number(item.replace('4-', '')))
              } else {
                status.push(item)
              }
            })
            if (newVal.statusUnion.includes(4)) { // 全选已忽略，就不用传忽略类型
              ignoreTypes = []
            } else if (ignoreTypes.length && !status.includes(4)) { // 选了忽略类型，参数同时要选已忽略状态
              status.push(4)
            }
            this.searchParams.status = status
            this.searchParams.ignoreReasonTypes = ignoreTypes
            return
          }
          // 切换文件后，如果状态还没修改为只选择待修复，不请求后台
          if (newVal.clusterType === 'file' && !(newVal.status.length === 1 && newVal.status[0] === 1)) {
            return
          }
          if (this.isSearch) {
            this.fetchList()
          }
        },
        deep: true,
      },
      defectDetailSearchParams: {
        handler(val) {
          this.clearAllInterval()
          const cacheId = val[this.cacheConfig.cacheKey]
          this.defectDetailDialogVisiable = this.isShowDetailDialog
          this.emptyText = this.$t('未选择文件')
          this.detailLoading = true
          if (this.defectCache[cacheId]) {
            this.lintDetail = this.defectCache[cacheId]
            this.handleCodeFullScreen()
          }
          this.operateParams.ignoreReasonType = ''
          this.operateParams.ignoreReason = ''
          this.fetchLintDetail()
          this.preloadCache(this.defectList, this.cacheConfig)
        },
        deep: true,
      },
      searchInput: {
        handler(val) {
          if (this.searchFormData.filePathTree.children) {
            if (val) {
              // this.searchFormData.filePathTree.expanded = true
              this.openTree(this.searchFormData.filePathTree)
            } else {
              this.searchFormData.filePathTree.expanded = false
            }
          }
        },
        deep: true,
      },
      changeHandlerVisiable: {
        handler() {
          if (!this.changeHandlerVisiable) {
            window.localStorage.setItem('memberNeverShow', JSON.stringify(this.memberNeverShow))
          }
        },
        deep: true,
      },
      'searchParams.clusterType'(newVal) {
        this.isSelectAll = 'N'
        this.isBatchOperationShow = false
        if (newVal === 'file' && !(this.searchParams.status.length === 1 && this.searchParams.status[0] === 1)) {
          this.searchParams.status = [1]
        }
        if (newVal === 'file') {
          this.searchParams.sortField = 'fileName'
        }
        this.fileIndex = 0
        this.selectedLen = 0
        this.selectedDefectCount = 0
      },
      defectList(val, oldVal) {
        this.preloadCache(val, this.cacheConfig)
        this.setTableHeight()
        if (val.length < this.fileIndex) {
          this.fileIndex = 0
        }
      },
      'searchParams.status'(val, oldVal) {
        this.fetchOtherParams()
      },
      'searchParams.checkerSet'(val, oldVal) {
        this.fetchOtherParams()
      },
      'searchParams.ignoreReasonTypes'() {
        this.fetchStatusParams()
        this.fetchSeverityParams()
      },
      statusRelevantParams(val, oldVal) {
        this.fetchStatusParams()
      },
      severityRelevantParams(val, oldVal) {
        this.fetchSeverityParams()
      },
      'searchParams.toolName'(val, oldVal) {
        this.fetchCheckerSetParams()
        this.initParams()
      },
      defectDetailDialogVisiable() {
        setTimeout(() => {
          if (document.querySelector('.current-row')) {
            document.querySelector('.current-row').scrollIntoView({ block: 'center' })
          }
        })
      },
      dimension(val) {
        this.searchParams.dimension = val
        this.fetchCheckerSetParams()
        this.initParams()
        this.fetchListTool()
      },
      taskIdList(val) {
        this.fetchCheckerSetParams()
        this.initParams()
        this.fetchListTool()
        this.fetchList()
      },
    },
    created() {
      if (!this.taskDetail.nameEn || this.taskDetail.enableToolList
        .find(item => item.toolName !== 'CCN' && item.toolName !== 'DUPC')) {
        this.init(true)
        this.fetchOtherParams()
        this.isProjectDefect ? this.fetchTaskList() : this.fetchBuildList()
      }
      this.handelFetchIgnoreList()
      if (this.isFromOverview) this.isFetched = true

      // 读取缓存搜索过滤项
      const columnsCache = JSON.parse(localStorage.getItem(SEARCH_OPTION_CACHE))
      const defaultOptionColumn = this.getCustomOption(true)
      if (columnsCache) {
        defaultOptionColumn.forEach((item) => {
          item.isChecked = columnsCache.find(column => column.id === item.id)?.isChecked
        })
        this.customOption = defaultOptionColumn
      }
      this.selectedOptionColumn = _.cloneDeep(defaultOptionColumn)
      this.guideFlag = Boolean(localStorage.getItem('guideEnd') || '')
    },
    mounted() {
      const memberNeverShow = JSON.parse(window.localStorage.getItem('memberNeverShow'))
      memberNeverShow === null
        ? this.memberNeverShow = false
        : this.memberNeverShow = memberNeverShow
      // 读取缓存中搜索项首次展示或收起
      const lintSearchExpend = JSON.parse(window.localStorage.getItem('lintSearchExpend'))
      lintSearchExpend === null
        ? this.isSearchDropdown = true
        : this.isSearchDropdown = lintSearchExpend
      this.openDetail()
      this.keyOperate()
    },
    beforeDestroy() {
      document.onkeydown = null
    },
    methods: {
      fetchList() {
        this.tableLoading = true
        // this.fetchSearchList()
        this.fetchLintList().then((list) => {
          if (this.pageChange) {
            // 将一页的数据追加到列表
            this.listData.defectList.records = this.listData.defectList.records.concat(list.defectList.records)

            // 隐藏加载条
            this.isFileListLoadMore = false

            // 重置页码变更标记
            this.pageChange = false
          } else {
            this.listData = { ...this.listData, ...list }
            this.totalCount = this.listData.defectList.count
            this.pagination.count = this.listData.defectList.count
            // 重置文件下的问题详情
            this.lintDetail = {}
            if (list.tips) {
              this.handleNewBuildId(list.tips)
            }
          }
        })
          .finally(() => {
            // this.fileIndex = 0
            this.addTableScrollEvent()
            this.tableLoading = false
          })
      },
      /**
       * 重置搜索过滤项
       */
      handleSelectAllSearchOption() {
        const isSelectAll = this.customOption.every(item => item.isChecked)
        this.customOption = this.getCustomOption(!isSelectAll)
        // this.$refs.handleMenu.instance.hide()
        // this.selectedOptionColumn = _.cloneDeep(this.customOption)
        // localStorage.setItem(SEARCH_OPTION_CACHE, JSON.stringify(this.selectedOptionColumn))
        // this.setTableHeight()
      },

      /**
       * 确认搜索过滤项
       */
      handleConfirmSearchOption() {
        this.$refs.handleMenu.instance.hide()
        this.selectedOptionColumn = _.cloneDeep(this.customOption)
        localStorage.setItem(SEARCH_OPTION_CACHE, JSON.stringify(this.selectedOptionColumn))
        this.setTableHeight()
      },

      downloadExcel() {
        const params = this.getSearchParams()
        const { clusterType } = this.searchParams
        params.pageSize = 300000
        if (this.totalCount > 300000) {
          this.$bkMessage({
            message: this.$t('当前问题数已超过30万个，无法直接导出excel，请筛选后再尝试导出。'),
          })
          return
        }
        this.exportLoading = true
        this.$store.dispatch('defect/lintList', params).then((res) => {
          const listKey = clusterType === 'defect' ? 'defectList' : 'fileList'
          const list = res && res[listKey] && res[listKey].records
          this.generateExcel(list, clusterType)
        })
          .finally(() => {
            this.exportLoading = false
          })
      },
      generateExcel(list = [], clusterType) {
        const { isProjectDefect } = this
        const exHeader = isProjectDefect ? [this.$t('任务')] : []
        const exVal = isProjectDefect ? ['taskNameCn'] : []
        const tHeader = [
          this.$t('序号'),
          this.$t('entityId'),
          this.$t('位置'),
          this.$t('路径'),
          this.$t('规则'),
          this.$t('工具'),
          this.$t('维度'),
          this.$t('问题描述'),
          this.$t('处理人'),
          this.$t('级别'),
          this.$t('提交日期'),
          this.$t('创建日期'),
          this.$t('修复日期'),
          this.$t('忽略日期'),
          this.$t('忽略人'),
          this.$t('忽略类型'),
          this.$t('忽略原因'),
          this.$t('首次发现'),
          this.$t('最新状态'),
          ...exHeader,
        ]
        const filterVal = [
          'index',
          'entityId',
          'fileName',
          'filePath',
          'checker',
          'toolName',
          'dimension',
          'message',
          'author',
          'severity',
          'lineUpdateTime',
          'createTime',
          'fixedTime',
          'ignoreTime',
          'ignoreAuthor',
          'ignoreReasonType',
          'ignoreReason',
          'createBuildNumber',
          'status',
          ...exVal,
        ]
        const data = this.formatJson(filterVal, list, clusterType)
        // eslint-disable-next-line
        const prefix = isProjectDefect ? `${this.$route.params.projectId}` : `${this.taskDetail.nameCn}-${this.taskDetail.taskId}-${this.dimensionStr}-`
        const title = `${prefix}${this.$t('问题')}-${new Date().toISOString()}`
        export_json_to_excel(tHeader, data, title)
      },
      // 处理状态
      handleStatus(status, ignoreReasonType) {
        let key = 1
        if (status === 1) {
          key = 1
        } else if (status & 2) {
          key = 2
        } else if (status & 4) {
          key = 4
        } else if (status & 8 || status & 16) { // 8是路径屏蔽，16是规则屏蔽
          key = 8
        }
        const statusMap = {
          1: this.$t('待修复'),
          2: this.$t('已修复'),
          4: this.$t('已忽略'),
          8: this.$t('已屏蔽'),
        }
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
        let ignoreStr = ''
        if (status & 4 && ignoreReasonType) {
          const ignoreName = this.ignoreList.find(item => item.ignoreTypeId === ignoreReasonType)?.name
          ignoreName && (ignoreStr = `-${ignoreName}`)
        }
        return `${statusMap[key]}${ignoreStr}`
      },
      // 处理表格数据
      formatJson(filterVal, list, clusterType) {
        let index = 0
        return list.map(item => filterVal.map((j) => {
          if (j === 'index') {
            return index += 1
          } if (j === 'fileName' && clusterType === 'defect') {
            return `${item.fileName}:${item.lineNum}`
          } if (j === 'severity') {
            return this.defectSeverityMap[item.severity]
          } if (j === 'lineUpdateTime' || j === 'fileUpdateTime'
            || j === 'createTime' || j === 'fixedTime' || j === 'ignoreTime') {
            return this.formatTime(item[j], 'YYYY-MM-DD HH:mm:ss')
          } if (j === 'createBuildNumber') {
            return `#${item.createBuildNumber}`
          } if (j === 'status') {
            return this.handleStatus(item.status, item.ignoreReasonType)
          } if (j === 'checkerList') {
            return item[j] && item[j].length
          } if (j === 'authorList') {
            return item[j] && item[j].join(';')
          } if (j === 'severityList') {
            return item[j] && item[j].map(i => this.defectSeverityMap[i]).join('、')
          } if (j === 'ignoreReasonType') {
            if (item[j] === 1) {
              return this.$t('工具误报')
            } if (item[j] === 2) {
              return this.$t('设计如此')
            } if (item[j] === 4) {
              return this.$t('其他')
            }
            return ''
          } if (j === 'dimension') {
            const { dimension } = this.searchParams
            let dimensionName = ''
            this.dimenList.forEach((i) => {
              if (dimension === i.key) {
                dimensionName = i.name
              }
            })
            return dimensionName
          }
          return item[j]
        }))
      },
      async init(isInit) {
        isInit ? this.contentLoading = true : this.fileLoading = true
        // const list = await this.fetchLintList()
        // this.listData = { ...this.listData, ...list }
        // this.totalCount = this.listData.defectList.count
        // this.pagination.count = this.listData.defectList.count
        this.isSearch = true
        this.addTableScrollEvent()
        if (isInit) {
          this.handleGatherFile()
          this.fetchCheckerSetParams()
          this.contentLoading = false
          this.isFetched = true
        } else {
          this.tableLoading = false
        }
      },
      initParams() {
        this.fetchSeverityParams()
        this.fetchStatusParams()
        this.fetchOtherParams()
      },
      async fetchBuildList() {
        this.selectLoading.buildListLoading = true
        this.buildList = await this.$store.dispatch('defect/getBuildList', { taskId: this.taskId })
        this.selectLoading.buildListLoading = false
      },
      getSearchParams() {
        const { daterange } = this.searchParams
        const startCreateTime = this.formatTime(daterange[0], 'YYYY-MM-DD')
        const endCreateTime = this.formatTime(daterange[1], 'YYYY-MM-DD')
        const { dimension, toolName, taskIdList } = this
        const { isSelectAll } = this
        const checkerSet = this.searchFormData.checkerSetList
          .find(checkerSet => checkerSet.checkerSetId === this.searchParams.checkerSet)
        const params = {
          ...this.searchParams,
          dimensionList: dimension,
          toolNameList: toolName,
          startCreateTime,
          endCreateTime,
          isSelectAll,
          checkerSet,
          taskIdList,
        }
        return params
      },
      async fetchLintList() {
        const params = this.getSearchParams()
        const res = await this.$store.dispatch('defect/lintList', params)
        if (!res) return []
        if (res.fileList) {
          res.defectList = res.fileList
          delete res.fileList
        }
        return res
      },
      // async fetchSearchList () {
      //     const params = this.getSearchParams()
      //     const res = await this.$store.dispatch('defect/lintSearchParams', params)
      //     this.newDefectJudgeTime = res.newDefectJudgeTime ? this.formatTime(res.newDefectJudgeTime, 'YYYY-MM-DD') : ''
      //     this.searchFormData = Object.assign(this.searchFormData, res)
      //     this.getDefectCount(res)
      // },
      async fetchSeverityParams() {
        const params = this.getSearchParams()
        params.statisticType = 'SEVERITY'
        const res = await this.$store.dispatch('defect/lintSearchParams', params)
        const { newDefectJudgeTime, seriousCount, normalCount, promptCount } = res
        this.newDefectJudgeTime = newDefectJudgeTime ? this.formatTime(newDefectJudgeTime, 'YYYY-MM-DD') : ''
        this.searchFormData = Object.assign(this.searchFormData, { seriousCount, normalCount, promptCount })
        this.hasCountData = true
        this.getDefectCount(res)
      },
      async fetchCheckerSetParams() {
        const { dimension, toolName, taskIdList, searchParams: { buildId } } = this
        const params = {
          taskIdList,
          toolNameList: toolName,
          dimensionList: dimension,
          buildId,
        }
        const res = await this.$store.dispatch('checkerset/listForDefect', params)
        const checkerSetList = res.filter(checkerSet => checkerSet.taskUsing)
        this.searchFormData = Object.assign(this.searchFormData, { checkerSetList })
      },
      async fetchStatusParams() {
        this.selectLoading.statusLoading = true
        const params = this.getSearchParams()
        params.statisticType = 'STATUS'
        const res = await this.$store.dispatch('defect/lintSearchParams', params)
        const { existCount, fixCount, ignoreCount, maskCount } = res
        this.searchFormData = Object.assign(this.searchFormData, { existCount, fixCount, ignoreCount, maskCount })
        this.selectLoading.statusLoading = false
      },
      async fetchOtherParams() {
        this.selectLoading.otherParamsLoading = true
        const { status, checkerSet, buildId } = this.searchParams
        const { taskIdList, dimension, toolName } = this
        const params = {
          dimensionList: dimension,
          toolNameList: toolName,
          statusList: status,
          checkerSet,
          buildId,
          taskIdList,
        }
        const res = await this.$store.dispatch('defect/lintOtherParams', params)
        const { authorList = [], checkerList = [], filePathTree = {} } = res
        const newAuthorList = authorList.filter(item => item !== this.user.username)
        newAuthorList.unshift(this.user.username)
        this.searchFormData = Object.assign(
          this.searchFormData,
          { authorList: newAuthorList, checkerList, filePathTree },
        )
        this.selectLoading.otherParamsLoading = false
      },
      async handleGatherFile() {
        const { buildId } = this.searchParams
        const { dimension, toolName, taskIdList } = this
        this.gatherFile = await this.$store.dispatch(
          'defect/gatherFile',
          { taskIdList, toolNameList: toolName, dimensionList: dimension, buildId },
        ) || {}
      },
      fetchLintDetail(type, extraParams = {}) {
        const params = {
          ...this.searchParams,
          ...this.defectDetailSearchParams,
          pattern: this.toolPattern,
          ...extraParams,
          dimension: this.dimensionStr,
        }
        params.fileList = [params.filePath]
        this.$store.dispatch('defect/lintDetail', params).then((detail) => {
          if (detail.lintDefectDetailVO.ignoreReasonType) {
            this.operateParams.ignoreReasonType = detail.lintDefectDetailVO.ignoreReasonType
          }
          if (detail.lintDefectDetailVO.ignoreReason) {
            this.operateParams.ignoreReason = detail.lintDefectDetailVO.ignoreReason
          }
          if (detail.fileName) {
            if (!extraParams.entityId) {
              this.lintDetail = detail

              // 查询详情后，全屏显示问题
              this.handleCodeFullScreen(type)
              const cacheConfig = { ...this.cacheConfig, index: this.fileIndex }
              this.preloadCache(this.defectList, cacheConfig)
            }
            const updateCacheKey = params[this.cacheConfig.cacheKey]
            this.updateCache(updateCacheKey, detail)
            if (this.isOpenDetail) {
              this.handleInitFileIndex()
              this.isOpenDetail = false
            }
          } else if (detail.response) {
            this.cacheConfig.length = 0
            this.preloadCache(this.defectList, this.cacheConfig)
          } else if (detail.code === '2300005') {
            this.defectDetailDialogVisiable = false
            setTimeout(() => {
              this.$bkInfo({
                subHeader: this.$createElement('p', {
                  style: {
                    fontSize: '20px',
                    lineHeight: '40px',
                  },
                }, this.$t('无法获取问题的代码片段。请先将工蜂OAuth授权给蓝盾。')),
                confirmFn: () => {
                  this.$store.dispatch('defect/oauthUrl', { toolName: this.defectDetailSearchParams.toolName })
                    .then((res) => {
                      window.open(res, '_blank')
                    })
                },
              })
            }, 500)
          }
        })
          .finally(() => {
            this.detailLoading = false
          })
      },
      keyOperate() {
        const vm = this
        document.onkeydown = keyDown
        function keyDown(event) {
          const e = event || window.event
          if (e.target.nodeName !== 'BODY') return
          switch (e.code) {
            case 'Enter': // enter
              // e.path.length < 5 防止规则等搜索条件里面的回车触发打开详情
              if (!vm.defectDetailDialogVisiable && !vm.authorEditDialogVisiable) vm.keyEnter()
              break
            case 'Escape': // esc
              if (vm.defectDetailDialogVisiable) vm.defectDetailDialogVisiable = false
              break
            case 'ArrowLeft': // left
            case 'ArrowUp': // up
            case 'KeyW': // w
              if (e.shiftKey) {
                if (vm.defectDetailDialogVisiable) {
                  vm.$refs.detail.traceUp()
                }
              } else {
                if (vm.fileIndex > 0) {
                  if (vm.defectDetailDialogVisiable) {
                    vm.handleFileListRowClick(vm.defectList[vm.fileIndex -= 1])
                  } else {
                    vm.fileIndex -= 1
                  }
                  vm.screenScroll()
                }
              }
              break
            case 'ArrowRight': // right
            case 'ArrowDown': // down
            case 'KeyS': // s
              if (e.shiftKey) {
                if (vm.defectDetailDialogVisiable) {
                  vm.$refs.detail.traceDown()
                }
              } else {
                if (vm.fileIndex < vm.defectList.length - 1) {
                  if (vm.defectDetailDialogVisiable) {
                    vm.handleFileListRowClick(vm.defectList[vm.fileIndex += 1])
                  } else {
                    vm.fileIndex += 1
                  }
                  vm.screenScroll()
                }
              }
              break
          }
        }
      },
      addTableScrollEvent() {
        this.$nextTick(() => {
          // 滚动加载
          if (this.$refs.table && this.$refs.table.$refs.fileListTable) {
            const tableBodyWrapper = this.$refs.table.$refs.fileListTable.$refs.bodyWrapper

            // 问题文件列表滚动加载
            tableBodyWrapper.addEventListener('scroll', (event) => {
              const dom = event.target
              // 是否滚动到底部
              const hasScrolledToBottom = dom.scrollTop + dom.offsetHeight + 100 > dom.scrollHeight

              // 触发翻页加载
              if (hasScrolledToBottom) {
                this.scrollLoadMore()
              }
            })
          }
        })
      },
      getDefectCountBySeverity(severity) {
        const severityFieldMap = {
          1: 'seriousCount',
          2: 'normalCount',
          4: 'promptCount',
        }
        const count = this.searchFormData[severityFieldMap[severity]] || 0
        return count > 100000 ? this.$t('10万+') : count
      },
      getDefectCountByType(type) {
        const tpyeFieldMap = {
          1: 'newCount',
          2: 'historyCount',
        }
        const count = this.searchFormData[tpyeFieldMap[type]] || 0
        return count > 100000 ? this.$t('10万+') : count
      },
      handleSortChange({ column, prop, order }) {
        const orders = { ascending: 'ASC', descending: 'DESC' }
        this.searchParams = { ...this.searchParams, pageNum: 1, sortField: prop, sortType: orders[order] }
      },
      handlePageChange(page) {
        this.pagination.current = page
        this.searchParams = { ...this.searchParams, pageNum: page }
      },
      handlePageLimitChange(currentLimit) {
        this.pagination.current = 1 // 切换分页大小时要回到第一页
        this.searchParams = { ...this.searchParams, pageNum: 1, pageSize: currentLimit }
      },
      handleSelectionChange(selection) {
        this.selectedLen = selection.length || 0
        this.selectedDefectCount = 0
        selection.forEach((val) => {
          if (this.selectedLen === this.defectList.length) {
            this.selectedDefectCount = this.totalDefectCount
          } else this.selectedDefectCount += val.defectCount
        })
        this.isBatchOperationShow = Boolean(selection.length)
        // 如果长度是最长，那么就是Y，否则是N
        this.isSelectAll = this.selectedLen === this.defectList.length ? 'Y' : 'N'
      },
      handleFileListRowClick(row) {
        if (row.entityId) {
          this.fileIndex = this.defectList.findIndex(file => file.entityId === row.entityId)
        } else {
          this.fileIndex = this.defectList.findIndex(file => file.filePath === row.filePath)
        }
        this.defectDetailDialogVisiable = this.isShowDetailDialog
        // 要把参数强制先置空，不然不能触发请求
        this.defectDetailSearchParams.entityId = ''
        this.defectDetailSearchParams.entityId = row.entityId
        this.defectDetailSearchParams.filePath = row.filePath || row.fileName
        this.defectDetailSearchParams.defectId = row.defectId
        this.defectDetailSearchParams.toolName = row.toolName
        this.operateParams.toolName = row.toolName
        this.commentParams.toolName = row.toolName
        this.$refs.table.$refs.fileListTable.clearSelection()
      },
      keyEnter() {
        const row = this.defectList[this.fileIndex]
        this.defectDetailSearchParams.entityId = ''
        this.defectDetailSearchParams.entityId = row.entityId
        this.defectDetailSearchParams.defectId = row.defectId
        this.defectDetailSearchParams.filePath = row.filePath
      },
      handleCodeFullScreen(type) {
        this.$nextTick(() => {
          this.$refs.detail.handleCodeFullScreen(type)
          if (!type) this.$refs.detail.locateFirst()
        })
      },
      codeViewerClick(event, eventSource) {
        const lintHints = getClosest(event.target, '.lint-hints')

        // 如果点击的是lint问题区域，展开修复建议
        if (lintHints) {
          toggleClass(lintHints, 'active')
        }
      },

      // 文件路径相关交互
      handleFilePathSearch(val) {
        this.$refs.filePathTree.filter(val)
      },
      handleFilePathCancelClick() {
        this.$refs.filePathDropdown.hide()
      },
      openSlider() {
        this.show = true
      },
      numToArray(num, arr = [1, 2, 4, 8]) {
        let filterArr = arr.filter(x => x & num)
        filterArr = filterArr.length ? filterArr : arr
        return filterArr
      },
      openTree(arr) {
        if (arr.children) {
          arr.expanded = true
          arr.children.forEach((item) => {
            this.openTree(item)
          })
        }
      },
      handleSelectable(row, index) {
        return !(row.status & 2)
      },
      handleMark(markFlag, batchFlag, entityId) {
        // markFlag 0: 取消标记, 1: 标记修改
        // batchFlag true: 批量操作
        let bizType = 'MarkDefect'
        let defectKeySet = []
        if (batchFlag) {
          this.$refs.table.$refs.fileListTable.selection.forEach((item) => {
            defectKeySet.push(item.entityId)
          })
          if (markFlag) bizType = 'RevertIgnore|MarkDefect'
        } else {
          defectKeySet = [entityId]
        }
        const dimension = this.dimensionStr
        const { taskIdList, toolName: toolNameList, dimension: dimensionList } = this
        let data = { ...this.operateParams, bizType, defectKeySet, markFlag, taskIdList, toolNameList, dimensionList }
        if (this.isSelectAll === 'Y') {
          data = { ...data, isSelectAll: 'Y', queryDefectCondition: JSON.stringify(this.getSearchParams()) }
        }
        this.tableLoading = true
        this.$store.dispatch('defect/batchEdit', data).then((res) => {
          if (res.code === '0') {
            let message = markFlag ? this.$t('标记为已处理成功') : this.$t('取消标记成功')
            if (batchFlag) {
              this.fetchList()
              if (markFlag) {
                const list = res.data || []
                let revertCount = 0
                let markCount = 0
                list.forEach((item) => {
                  if (item.bizType === 'RevertIgnore') {
                    revertCount = item.count
                  } else if (item.bizType === 'MarkDefect') {
                    markCount = item.count
                  }
                })
                const unfixedMarkCount = markCount - revertCount
                message = ''
                if (markCount && unfixedMarkCount) message = this.$t('x个待修复问题标记为已处理成功', { unfixedMarkCount })
                if (markCount && unfixedMarkCount && revertCount) message += ', '
                if (revertCount) message += this.$t('x个已忽略问题取消忽略并标记为已处理成功', { revertCount })
              }
            } else {
              this.listData.defectList.records.forEach((item) => {
                if (item.entityId === entityId) {
                  item.mark = markFlag
                }
              })
              this.listData.defectList.records = this.listData.defectList.records.slice()
            }
            if (this.defectDetailDialogVisiable) {
              this.fetchLintDetail('scroll')
            }
            this.$bkMessage({
              theme: 'success',
              message,
            })
          }
        })
          .catch((e) => {
            console.error(e)
          })
          .finally(() => {
            this.tableLoading = false
          })
      },
      handleComent(entityId, commentId) {
        this.commentParams = { ...this.commentParams, defectId: entityId, comment: '' }
        if (commentId) {
          this.commentParams.commentId = commentId
        } else if (this.searchParams.clusterType === 'defect') {
          this.commentParams.commentId = this.lintDetail.lintDefectDetailVO.codeComment
            ? this.lintDetail.lintDefectDetailVO.codeComment.entityId : ''
        } else {
          this.commentParams.commentId = ''
        }
        this.commentDialogVisiable = true
      },
      handleCommentConfirm() {
        this.commentDialogVisiable = false
        // 暂不做修改评论
        // const url = this.commentParams.singleCommentId ? 'defect/updateComment' : 'defect/commentDefect'

        const url = 'defect/commentDefect'
        this.detailLoading = true
        this.commentParams.nameCn = this.taskDetail.nameCn
        this.commentParams.fileName = this.lintDetail.fileName
        this.commentParams.checker = this.lintDetail.lintDefectDetailVO.checker
        this.$store.dispatch(url, this.commentParams).then((res) => {
          if (res.code === '0') {
            this.$bkMessage({
              theme: 'success',
              message: this.$t('评论问题成功'),
            })
            this.fetchLintDetail('scroll')
          }
        })
          .finally(() => {
            this.detailLoading = false
          })
      },
      deleteComment(commentId, singleCommentId, defectEntityId, commentStr) {
        const params = {
          commentId,
          singleCommentId,
          defectEntityId,
          commentStr,
          toolName: this.defectDetailSearchParams.toolName,
        }
        this.$store.dispatch('defect/deleteComment', params).then((res) => {
          if (res.code === '0') {
            this.$bkMessage({
              theme: 'success',
              message: this.$t('删除成功'),
            })
            this.fetchLintDetail('scroll')
          }
        })
      },
      handleAuthor(changeAuthorType, entityId, author) {
        this.authorEditDialogVisiable = true
        this.operateParams.changeAuthorType = changeAuthorType
        if (author !== undefined) {
          this.operateParams.sourceAuthor = author
        }
        this.operateParams.defectKeySet = [entityId]
      },
      // 处理人修改
      handleAuthorEditConfirm() {
        let data = this.operateParams
        const sourceAuthor = data.sourceAuthor ? new Set(data.sourceAuthor) : new Set()
        if (data.changeAuthorType === 2) {
          const defectKeySet = []
          this.$refs.table.$refs.fileListTable.selection.forEach((item) => {
            defectKeySet.push(item.entityId)
            item.author && sourceAuthor.add(...item.author)
          })
          this.operateParams.defectKeySet = defectKeySet
          data.defectKeySet = defectKeySet
        }
        data.bizType = 'AssignDefect'

        data.sourceAuthor = Array.from(sourceAuthor)
        data.newAuthor = data.targetAuthor
        if (this.isSelectAll === 'Y') {
          data = { ...data, isSelectAll: 'Y', queryDefectCondition: JSON.stringify(this.getSearchParams()) }
        }
        const { taskIdList, toolName: toolNameList, dimension: dimensionList } = this
        data = { ...data, taskIdList, toolNameList, dimensionList }
        const dispatchUrl = data.changeAuthorType === 3 ? 'defect/authorEdit' : 'defect/batchEdit'
        this.authorEditDialogVisiable = false
        this.tableLoading = true
        this.$store.dispatch(dispatchUrl, data).then((res) => {
          if (res.code === '0') {
            this.$bkMessage({
              theme: 'success',
              message: this.$t('修改处理人成功'),
            })
            this.operateParams.targetAuthor = []
            this.operateParams.sourceAuthor = []
            if (data.changeAuthorType === 1) {
              this.listData.defectList.records.forEach((item) => {
                if (item.entityId === data.defectKeySet[0]) {
                  item.author = data.newAuthor
                }
              })
              this.listData.defectList.records = this.listData.defectList.records.slice()
            } else {
              this.fetchList()
            }
            this.initParams()
            if (this.defectDetailDialogVisiable) {
              this.fetchLintDetail('scroll')
            }
          }
        })
          .catch((e) => {
            console.error(e)
          })
          .finally(() => {
            this.tableLoading = false
          })
      },
      handleIgnore(ignoreType, batchFlag, entityId, filePath) {
        this.$refs.operateDropdown?.hide()
        if (this.taskDetail.prohibitIgnore) return
        this.operateParams.fileList = [filePath]
        this.operateParams.bizType = ignoreType
        this.operateParams.batchFlag = batchFlag
        if (batchFlag) {
          const defectKeySet = []
          const fileList = []
          this.$refs.table.$refs.fileListTable.selection.forEach((item) => {
            defectKeySet.push(item.entityId)
            fileList.push(item.filePath)
          })
          this.operateParams.defectKeySet = defectKeySet
          this.operateParams.fileList = fileList
        } else {
          this.operateParams.defectKeySet = [entityId]
        }
        if (ignoreType === 'RevertIgnore') {
          this.handleIgnoreConfirm()
        } else {
          this.ignoreReasonDialogVisiable = true
        }
      },
      handleIgnoreConfirm() {
        let data = this.operateParams
        this.tableLoading = true
        this.ignoreReasonDialogVisiable = false
        if (this.isSelectAll === 'Y') {
          data = { ...data, isSelectAll: 'Y', queryDefectCondition: JSON.stringify(this.getSearchParams()) }
        } else if (this.searchParams.clusterType === 'file' && !this.operateParams.defectKeySet[0]) {
          const searchParams = JSON.parse(JSON.stringify(this.searchParams))
          searchParams.clusterType = 'defect'
          searchParams.pattern = 'LINT'
          searchParams.fileList = this.operateParams.fileList
          data = { ...data, isSelectAll: 'Y', queryDefectCondition: JSON.stringify(searchParams) }
          data.defectKeySet = []
        }
        const { taskIdList, toolName: toolNameList, dimension: dimensionList } = this
        data = { ...data, taskIdList, toolNameList, dimensionList }
        this.isRowChangeIgnoreType = false
        this.isShowDetailDialog = true
        this.$store.dispatch('defect/batchEdit', data).then((res) => {
          if (res.code === '0') {
            let message = ''
            if (this.operateParams.bizType === 'ChangeIgnoreType') {
              message = this.$t('修改忽略类型成功')
            } else {
              message = this.operateParams.bizType === 'IgnoreDefect'
                ? this.$t('忽略问题成功') : this.$t('恢复问题成功。该问题将重新在待修复列表中显示。')
            }
            this.$bkMessage({
              theme: 'success',
              message,
            })
            if (!data.batchFlag) {
              const index = this.listData.defectList.records.findIndex(item => item.entityId === data.defectKeySet[0])
              if (index < 0) {
                this.defectDetailDialogVisiable = false
              } else {
                this.listData.defectList.records.forEach((item) => {
                  if (item.entityId === data.defectKeySet[0]) {
                    if (this.operateParams.bizType === 'ChangeIgnoreType') {
                      item.ignoreReasonType = this.operateParams.ignoreReasonType
                      item.ignoreReason = this.operateParams.ignoreReason
                    } else if (this.operateParams.bizType === 'IgnoreDefect') {
                      item.status = 4
                      item.ignoreReasonType = this.operateParams.ignoreReasonType
                      item.ignoreReason = this.operateParams.ignoreReason
                    } else {
                      item.status = 1
                    }
                  }
                })
                this.listData.defectList.records = this.listData.defectList.records.slice()
              }
            } else {
              this.fetchList()
            }
            this.initParams()


            this.operateParams.ignoreReason = ''
            this.operateParams.ignoreReasonType = ''
            if (this.defectDetailDialogVisiable) {
              this.handleFileListRowClick(this.defectList[this.fileIndex])
            }
          }
        })
          .catch((e) => {
            console.error(e)
          })
          .finally(() => {
            this.tableLoading = false
          })
      },
      handleIgnoreCancel() {
        this.operateParams.ignoreReason = ''
        this.operateParams.ignoreReasonType = ''
        this.ignoreReasonDialogVisiable = false
        this.isShowDetailDialog = true
      },
      toLogs() {
        this.changeHandlerVisiable = false
        this.$router.push({
          name: 'task-settings-trigger',
        })
      },
      screenScroll() {
        this.$nextTick(() => {
          if (this.$refs.table && this.$refs.table.$refs.fileListTable
            && this.$refs.table.$refs.fileListTable.$refs.bodyWrapper) {
            const childrens = this.$refs.table.$refs.fileListTable.$refs.bodyWrapper
            const height = this.fileIndex > 3 ? (this.fileIndex - 3) * 42 : 0
            childrens.scrollTo({
              top: height,
              behavior: 'smooth',
            })
          }
        })
      },
      handleAuthorEdit() {
        this.$router.push({
          name: 'task-settings-trigger',
        })
      },
      toChangeMember() {
        if (this.memberNeverShow) {
          this.$router.push({
            name: 'task-settings-trigger',
          })
        } else {
          this.changeHandlerVisiable = true
        }
      },
      formatDate(dateNum, time) {
        return time ? format(dateNum, 'HH:mm:ss') : format(dateNum, 'YYYY-MM-DD HH:mm:ss')
      },
      toSelectAll() {
        this.isSelectAll = this.selectedLen === this.defectList.length ? 'Y' : 'N'
      },
      toggleSearch() {
        this.isSearchDropdown = !this.isSearchDropdown
        window.localStorage.setItem('lintSearchExpend', JSON.stringify(this.isSearchDropdown))
        this.setTableHeight()
      },
      // 根据ID打开详情
      openDetail() {
        const { entityId } = this.$route.query
        const { filePath } = this.$route.query
        if (entityId || filePath) {
          setTimeout(() => {
            this.isOpenDetail = true
            this.defectDetailSearchParams.entityId = entityId
          }, 1000)
        }
      },
      setTableHeight() {
        this.$nextTick(() => {
          let smallHeight = 0
          let largeHeight = 0
          let tableHeight = 0
          const i = this.listData.defectList.records.length || 0
          if (this.$refs.table && this.$refs.table.$refs.fileListTable) {
            const $main = document.getElementsByClassName('main-form')
            smallHeight = $main.length > 0 ? $main[0].clientHeight : 0
            largeHeight = this.$refs.mainContainer ? this.$refs.mainContainer.clientHeight : 0
            tableHeight = this.$refs.table.$refs.fileListTable.$el.scrollHeight
            this.screenHeight = i * 60 > tableHeight ? largeHeight - smallHeight - 73 : (i * 60) + 61
            this.screenHeight = this.screenHeight === 61 ? 336 : this.screenHeight
          }
        })
      },
      getDefectCount(list) {
        this.totalDefectCount = 0
        this.searchParams.severity.forEach((item) => {
          this.totalDefectCount += list[this.defectCountMap[item]]
        })
      },
      goToLog() {
        this.$router.push({ name: 'task-detail-log' })
      },
      /**
       * 滚动加载
       */
      scrollLoadMore() {
        // 总页数
        const { totalPages } = this.listData.defectList
        // 当前页码
        const currentPageNum = this.searchParams.pageNum
        if (currentPageNum + 1 <= totalPages && this.isFileListLoadMore === false) {
          // 显示加载条
          this.isFileListLoadMore = true
          // 变更页码触发查询
          this.searchParams.pageNum += 1
          // 标记为页面变更查询
          this.pageChange = true
        }
      },
      /**
       * 问题跳转打开详情时，匹配到list里具体问题
       */
      handleInitFileIndex() {
        const { entityId } = this.$route.query
        if (entityId) {
          const index = this.defectList.findIndex(item => item.entityId === entityId)
          let defect = this.currentFile
          if (index !== -1) {
            [defect] = this.listData.defectList.records.splice(index, 1)
          }
          this.listData.defectList.records.unshift(defect)
        }
      },
      handelFetchIgnoreList() {
        this.$store.dispatch('ignore/fetchIgnoreList').then((res) => {
          this.ignoreList = res.data
          this.hasIgnoreList = true
          setTimeout(this.showIgnoreTypeSelect, 1200)
          // this.showIgnoreTypeSelect()
        })
      },
      handleGuideNextStep() {
        this.$nextTick(() => {
          this.$refs.guidePopover?.hideHandler()
          bus.$emit('handleNextGuide')
          localStorage.setItem('guideEnd', true)
          this.guideFlag = true
        })
      },
      handleSetReview() {
        this.$refs.guidePopover?.hideHandler()
        let prefix = `${location.host}`
        if (window.self !== window.top) {
          prefix = `${window.DEVOPS_SITE_URL}/console`
        }
        const route = this.$router.resolve({
          name: 'ignoreList',
        })
        window.open(prefix + route.href, '_blank')
        this.handleGuideNextStep()
      },
      handleGetNotifyDate(notify) {
        let str = ''
        if (notify.notifyDayOfWeeks && notify.notifyDayOfWeeks.length) {
          const { notifyMonths, notifyWeekOfMonths, notifyDayOfWeeks, everyMonth, everyWeek } = notify
          const monthsTextMap = notifyMonths.map(i => this.monthsStrMap[i])
          const weekOfMonthTextMap = notifyWeekOfMonths.map(i => this.weekOfMonthsStrMap[i])
          const dayTextMap = notifyDayOfWeeks.map(i => this.dayOfWeekMap[i])
          if (everyMonth && everyWeek && notifyDayOfWeeks.length === 7) {
            str = this.$t('每天')
          } else if (everyMonth && everyWeek && notifyDayOfWeeks.length) {
            str = this.$t('每个月的每周的') + dayTextMap.join('、')
          } else if (everyMonth && notifyWeekOfMonths.length && notifyDayOfWeeks.length) {
            str = this.$t('每个月的') + weekOfMonthTextMap.join('、') + this.$t('的') + dayTextMap.join('、')
          } else if (notifyMonths.length && everyWeek && notifyDayOfWeeks.length) {
            str = this.$t('每年的') + monthsTextMap.join('、') + this.$t('每周的') + dayTextMap.join('、')
          } else if (notifyMonths.length && notifyWeekOfMonths.length && notifyDayOfWeeks.length) {
            str =  this.$t('每年的') + monthsTextMap.join('、') +  this.$t('的')
              + weekOfMonthTextMap.join('、') + this.$t('的') + dayTextMap.join('、')
          }
        }
        return str
      },
      /**
       * 取消忽略并标记处理
       */
      async handleRevertIgnoreAndMark(entityId) {
        await this.handleIgnore('RevertIgnore', false, entityId)
        setTimeout(() => {
          this.handleMark(1, false, entityId)
        }, 500)
      },
      /**
       * 取消忽略并提单
       */
      async handleRevertIgnoreAndCommit(entityId) {
        await this.handleIgnore('RevertIgnore', false, entityId)
        setTimeout(() => {
          this.handleCommit('commit', false, entityId)
        }, 500)
      },
      /**
       * 修改忽略类型
       */
      handleChangeIgnoreType(row, show) {
        this.isSelectAll = 'N'
        this.isRowChangeIgnoreType = true
        this.isShowDetailDialog = show
        // this.handleFileListRowClick(row)
        this.operateParams.ignoreReasonType = row.ignoreReasonType
        this.operateParams.ignoreReason = row.ignoreReason
        this.handleIgnore('ChangeIgnoreType', false, row.entityId)
      },
      /**
       * 跳转至设置-路径屏蔽
       */
      handleToPathShield() {
        const routeParams = { ...this.$route.params }
        const { href } = this.$router.resolve({
          name: 'task-settings',
          params: routeParams,
          query: {
            panel: 'ignore',
          },
        })
        window.open(href, '_blank')
      },
      async fetchTaskList() {
        this.taskList = await this.$store.dispatch('defect/getTaskList') || []
      },
    },
  }
</script>

<style>
    @import "./codemirror.css";
</style>

<style lang="postcss" scoped>
    @import "./../../css/variable.css";
    @import "./../../css/mixins.css";
    @import "./defect-list.css";

    .defect-list {
      margin: 16px 20px 0px 16px;
      &.project-defect {
        margin: 0 40px;
        border: 1px solid $borderColor;
        .main-container {
          padding: 16px 20px 0 16px;
        }
      }
    }
    .breadcrumb {
      padding: 0px !important;
      .breadcrumb-name {
        background: white;
      }
    }
    .main-container {
      margin: 0px !important;
      background: white;
      .change-handler {
        position: relative;
        top: -32px;
        left: 305px;
      }
      .codecc-icon {
        /* font-size: 14px; */
      }
      .icon-empty {
        font-size: 50px;
      }
      .bk-button-text {
        color: #63656e;
        :hover {
          color: #699df4;
        }
      }
    }
    >>>.checkbox-group {
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
    .bk-form-radio {
      margin-right: 15px;
      >>>.bk-radio-text {
        font-size: 12px;
      }
    }
    .cc-table {
      padding: 0px 15px 15px 15px;
      background: #fff;
      .cc-operate {
        display: inline-block;
        .cc-operate-buttons {
          display: flex;
          .cc-operate-button {
            margin-left: 10px;
          }
        }
      }
      .cc-selected {
        float: left;
        height: 42px;
        font-size: 12px;
        line-height: 26px;
        color: #333;
        padding-right: 10px;
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
      .cc-operate-more {
        >>>.icon-more {
          font-size: 20px;
        }
      }
    }
    .filepath-dropdown-content {
      color: #737987;
      .content-hd {
        margin: 0 16px 16px;
      }
      .content-bd {
        width: 480px;
        height: 350px;
        margin: 16px;
        overflow: auto;
      }
      .content-ft {
        border-top: 1px solid #ded8d8;
        text-align: center;
        padding: 12px 0;
        position: relative;
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
      width: 200px;
      text-align: left;
      display: inline-block;

      @mixin ellipsis;
      &+.bk-icon {
        right: 10px;
      }
    }
    .author-edit {
      padding: 34px 18px 11px;
      .tips {
        position: absolute;
        top: 66px;
        left: 23px;
        color: #979ba5;
        .bk-icon {
          margin-right: 2px;
          color: #ffd695;
        }
      }
    }
    .reason-type-list {
      padding: 20px;
      background: #FAFBFD;
      .ignore-list {
        max-height: 250px;
        overflow-y: scroll;
        padding-left: 2px;
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
      flex: 1%;
      margin-left: 15px;
      color:#979BA5;
      display: inline-block;
      width: 360px;
      text-overflow: ellipsis;
      overflow: hidden;
      white-space: nowrap;
    }
    >>>.bk-date-picker {
      width: 300px;
    }
    .cc-radio {
      display: block;
      padding-bottom: 15px;
    }
    .cc-icon-mark {
      display: inline-block;
      background: url(./../../images/mark.svg) no-repeat;
      height: 14px;
      width: 14px;
      margin-bottom: -2px;
    }
    .cc-mark {
      width: 114px;
      background: white;
      border-radius: 12px;
      padding: 0 8px;
      line-height: 23px;
      margin-left: 27px;
    }
    .defect-type-tips {
      top: 5px;
    }
    >>>.bk-label {
      font-size: 12px;
    }
    .operate-footer {
      text-align: center;
    }
    >>>.bk-table {
      .mark-row {
        .cell {
          padding: 0;
        }
      }
    }
    >>>.bk-option-content-default {
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
    }
    .handle-menu-tips {
      text-align: center;
      .entry-link {
        padding: 4px 0;
        font-size: 12px;
        cursor: pointer;
        color: $fontWeightColor;
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
        cursor: not-allowed;
        color: $fontWeightColor;
        &:hover {
          color: $borderColor;
          > a {
            color: $borderColor;
          }
        }
      }
    }
    .main-container::-webkit-scrollbar {
      width: 0;
    }
    .excel-download {
      line-height: 32px;
      cursor: pointer;
      padding-right: 10px;
      &:hover {
        color: #3a84ff;
      }
    }
    >>>.bk-button .bk-icon {
      .loading {
        color: #3a92ff;
      }
    }
    .operate-txt {
      line-height: 32px;
      &.operate-txt-1 {
        margin-top: -10px;
      }
      &.operate-txt-2 {
        margin-top: 10px;
      }
    }
    >>>.file-detail-dialog {
      .bk-dialog-body {
        padding: 0;
      }
    }
    >>>.CodeMirror-linewidget .lint-hints .lint-icon {
      top: 4px;
    }
    ::v-deep .bk-dialog-header {
      padding: 3px 24px 0px !important;
    }
</style>
<style lang="postcss">
    .file-detail-dialog {
      .bk-dialog {
        min-width: 1010px;
      }
    }
    .guide-item {
      background-color: #eaf3ff;
      color: #3a84ff;
    }
    .tippy-tooltip.dot-menu-theme {
      padding: 10px;
      width: 280px !important;
      height: 100px;
      background-color: #3b91fb;
      color: #fff;
      .guide-btn {
        position: absolute;
        right: 18px;
        bottom: 15px;
      }
      .btn-item {
        background-color: #fff;
        color: #3b91fb;
        padding: 2px 5px;
        border-radius: 3px;
        cursor: pointer;
      }
    }
</style>
