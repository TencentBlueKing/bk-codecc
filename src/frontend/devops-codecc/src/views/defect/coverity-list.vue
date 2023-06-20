<template>
  <div v-bkloading="{ isLoading: !mainContentLoading && contentLoading, opacity: 0.3 }">
    <section class="coverity-list"
             v-if="taskDetail.enableToolList.find(item => item.toolName === 'COVERITY' || item.toolName === 'KLOCWORK' || item.toolName === 'PINPOINT')
               || isFromOverview">
      <div class="breadcrumb">
        <div class="breadcrumb-name">
          <bk-tab :active.sync="active" :label-height="42" @tab-change="handleTableChange" type="unborder-card">
            <bk-tab-panel
              v-for="(panel, index) in panels"
              v-bind="panel"
              :key="index">
            </bk-tab-panel>
          </bk-tab>
          <span :class="{ 'filter-search-icon': true, 'mac-filter-search-icon': isMac }">
            <bk-popover ext-cls="handle-menu" ref="handleMenu" theme="light" placement="left-start" trigger="click">
              <i class="bk-icon codecc-icon icon-shaixuan" v-bk-tooltips="$t('设置筛选条件')"></i>
              <div slot="content">
                <filter-search-option
                  :default-option="defaultOption"
                  :custom-option="customOption"
                  @selectAll="handleSelectAllSearchOption"
                  @confirm="handleConfirmSearchOption" />
              </div>
            </bk-popover>
          </span>
          <span class="excel-icon">
            <bk-button style="border: 0" v-if="exportLoading" icon="loading" :disabled="true" :title="$t('导出Excel')"></bk-button>
            <span v-else class="codecc-icon icon-export-excel excel-download" @click="downloadExcel" v-bk-tooltips="$t('导出Excel')"></span>
          </span>
        </div>
      </div>

      <div class="main-container" ref="mainContainer">
        <div class="main-content-inner main-content-list">
          <div class="search-content-inner">
            <bk-form ref="bkForm" :label-width="60" class="search-form main-form">
              <container class="cc-container">
                <div class="cc-col">
                  <bk-form-item :label="$t('维度')">
                    <bk-select v-model="dimension" @selected="handleSelectDimen" :clearable="false">
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
                    <bk-select v-model="searchParams.toolName">
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
                  <bk-form-item :label="$t('single.规则集')">
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
                <div class="cc-col" v-show="allRenderColumnMap.checker && (lineAverageOpt >= 4 || isSearchDropdown)">
                  <bk-form-item :label="$t('规则')">
                    <bk-select v-model="searchParams.checker" searchable :loading="selectLoading.otherParamsLoading">
                      <bk-option
                        v-for="(value, key) in listData.checkerMap"
                        :key="key"
                        :id="key"
                        :name="`${key}（${value}）`"
                      >
                      </bk-option>
                    </bk-select>
                  </bk-form-item>
                </div>
                <div class="cc-col" v-show="allRenderColumnMap.author && (lineAverageOpt >= 5 || isSearchDropdown)">
                  <bk-form-item :label="$t('处理人')">
                    <bk-select v-model="searchParams.author" searchable :loading="selectLoading.otherParamsLoading">
                      <bk-option
                        v-for="(value, key) in listData.authorMap"
                        :key="key"
                        :id="key"
                        :name="`${key} (${value})`"
                      >
                      </bk-option>
                    </bk-select>
                    <bk-button @click="toChangeMember" :title="$t('批量修改问题处理人')" :text="true" class="change-handler">
                      <i class="codecc-icon icon-handler-2"></i>
                    </bk-button>
                  </bk-form-item>
                </div>
                <div class="cc-col" v-show="allRenderColumnMap.daterange && (lineAverageOpt >= 6 || isSearchDropdown)">
                  <bk-form-item :label="$t('日期')">
                    <date-picker :date-range="searchParams.daterange" :handle-change="handleDateChange" :selected="dateType"></date-picker>
                  </bk-form-item>
                </div>
                <div class="cc-col" v-show="allRenderColumnMap.path && (lineAverageOpt >= 7 || isSearchDropdown)">
                  <bk-form-item :label="$t('路径')">
                    <bk-select v-if="selectLoading.otherParamsLoading" :loading="true"></bk-select>
                    <bk-dropdown-menu
                      v-else
                      trigger="click"
                      ref="filePathDropdown"
                      @show="isFilePathDropdownShow = true"
                      @hide="isFilePathDropdownShow = false"
                      :align="left"
                    >
                      <bk-button type="primary" slot="dropdown-trigger">
                        <div class="filepath-name"
                             :class="{ 'unselect': !searchFormData.filePathShow }"
                             :title="searchFormData.filePathShow"
                        >
                          {{searchFormData.filePathShow ? searchFormData.filePathShow : $t('请选择')}}
                        </div>
                        <i :class="['bk-icon icon-angle-down', { 'icon-flip': isFilePathDropdownShow }]" style="color: #979ba5; position: absolute; right: -2px"></i>
                      </bk-button>
                      <div class="filepath-dropdown-content" slot="dropdown-content" @click.stop>
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
                <div class="cc-col">
                  <bk-form-item :label="$t('快照')">
                    <bk-select v-model="searchParams.buildId" :clearable="true" searchable :loading="selectLoading.buildListLoading">
                      <bk-option
                        v-for="item in buildList"
                        :key="item.buildId"
                        :id="item.buildId"
                        :name="`#${item.buildNum} ${item.branch} ${item.buildUser} ${formatDate(item.buildTime) || ''}触发`">
                        <div class="cc-ellipsis" :title="`#${item.buildNum} ${item.branch} ${item.buildUser} ${formatDate(item.buildTime) || ''}触发`">
                          {{`#${item.buildNum} ${item.branch} ${item.buildUser} ${formatDate(item.buildTime) || ''}触发`}}
                        </div>
                      </bk-option>
                    </bk-select>
                  </bk-form-item>
                </div>
                <div class="cc-col" v-show="allRenderColumnMap.status && (lineAverageOpt >= 8 || isSearchDropdown)">
                  <bk-form-item :label="$t('状态')">
                    <bk-select multiple v-model="searchParams.status" clearable searchable :loading="selectLoading.otherParamsLoading">
                      <bk-option
                        v-for="(value, key) in statusTypeMap"
                        :key="Number(key)"
                        :id="Number(key)"
                        :name="value"
                      >
                      </bk-option>
                    </bk-select>
                  </bk-form-item>
                </div>
                <div class="cc-col" v-show="allRenderColumnMap.ignoreType && (lineAverageOpt >= 9 || isSearchDropdown)">
                  <bk-form-item :label="$t('忽略类型')">
                    <bk-select
                      v-model="searchParams.ignoreReasonTypes"
                      :disabled="ignoreTypeDisabled && !!guideFlag"
                      multiple
                      searchable
                      ref="ignoreTypeSelect">
                      <bk-option
                        v-for="(ignore, index) in ignoreList"
                        :key="index"
                        :id="ignore.ignoreTypeId"
                        :name="ignore.name"
                        :class="{ 'guide-item': ignore.ignoreTypeId === 24 && !guideFlag }"
                      >
                        <div v-if="ignore.ignoreTypeId === 24 && !guideFlag">
                          <bk-popover ref="guidePopover" placement="right-start" theme="dot-menu light">
                            <span>{{ ignore.name }}</span>
                            <div class="guide-content" slot="content">
                              <div>{{ $t('【存量问题】可在这里查看') }}</div>
                              <div>{{ $t('对特定忽略类型可设置提醒，以便定期review和修复。') }}</div>
                              <div class="guide-btn">
                                <span class="btn-item mr5" @click="handleSetReview">{{ $t('设置提醒') }}</span>
                                <span class="btn-item" @click="handleGuideNextStep">{{ $t('知道了') }}</span>
                              </div>
                            </div>
                          </bk-popover>
                        </div>
                      </bk-option>
                    </bk-select>
                  </bk-form-item>
                </div>
                <div class="cc-col" v-show="allRenderColumnMap.severity && (lineAverageOpt >= 10 || isSearchDropdown)">
                  <bk-form-item :label="$t('级别')">
                    <bk-checkbox-group v-model="searchParams.severity" class="checkbox-group">
                      <bk-checkbox
                        v-for="(value, key, index) in defectSeverityMap"
                        :value="Number(key)"
                        :key="index"
                      >
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
              <span v-show="isSelectAll === 'Y'">{{$t('已选择x条,共y条', { x: totalCount, y: totalCount })}}</span>
              <span v-show="isSelectAll !== 'Y'">{{$t('已选择x条,共y条', { x: selectedLen, y: totalCount })}}</span>
            </div>
            <p class="search-more-option" v-if="!isBatchOperationShow">
              <i :class="['bk-icon codecc-icon icon-codecc-arrow', { 'icon-flip': isSearchDropdown }]"
                 @click.stop="toggleSearch">
              </i>
            </p>
            <div v-if="isBatchOperationShow" class="cc-operate pb10">
              <div class="cc-operate-buttons">
                <bk-dropdown-menu ref="operateDropdown" @show="isDropdownShow = true" @hide="isDropdownShow = false">
                  <bk-button size="small" slot="dropdown-trigger">
                    <span>{{$t('标记')}}</span>
                    <i :class="['bk-icon icon-angle-down', { 'icon-flip': isDropdownShow }]"></i>
                  </bk-button>
                  <div class="handle-menu-tips" slot="dropdown-content">
                    <p class="entry-link" @click.stop="handleMark(1, true)">
                      {{$t('标记处理')}}
                    </p>
                    <p class="entry-link" @click.stop="handleMark(0, true)">
                      {{$t('取消标记')}}
                    </p>
                  </div>
                </bk-dropdown-menu>
                <bk-button size="small" ext-cls="cc-operate-button" @click="handleAuthor(2)" theme="primary">{{$t('分配')}}</bk-button>
                <bk-button size="small" ext-cls="cc-operate-button" @click="handleIgnore('IgnoreDefect', true)" theme="primary">{{$t('忽略')}}</bk-button>
                <bk-button size="small" ext-cls="cc-operate-button" @click="handleIgnore('RevertIgnore', true)" v-if="!searchParams.status.length || searchParams.status.includes(4)" theme="primary">
                  {{$t('取消忽略')}}
                </bk-button>
                <bk-button size="small" ext-cls="cc-operate-button" @click="handleCommit('commit', true)" theme="primary">{{$t('提单')}}</bk-button>
              </div>
            </div>
            <div class="cc-keyboard">
              <span>{{$t('当前已支持键盘操作')}}</span>
              <bk-button text ext-cls="cc-button" @click="operateDialogVisiable = true">{{$t('如何操作？')}}</bk-button>
            </div>
            <bk-table
              v-show="isFetched"
              class="file-list-table"
              row-class-name="list-row"
              ref="fileListTable"
              v-bkloading="{ isLoading: fileLoading, opacity: 0.6 }"
              :height="screenHeight"
              :data="defectList"
              @row-click="handleFileListRowClick"
              @sort-change="handleSortChange"
              @selection-change="handleSelectionChange"
              @select-all="toSelectAll"
            >
              <bk-table-column :selectable="handleSelectable" type="selection" width="60" align="center">
              </bk-table-column>
              <bk-table-column width="15" class-name="mark-row">
                <template slot-scope="props">
                  <span v-if="props.row.status === 1 && props.row.mark" class="cc-icon-mark"></span>
                </template>
              </bk-table-column>
              <!-- <bk-table-column :label="$t('ID')" prop="id" sortable="custom" width="75"></bk-table-column> -->
              <bk-table-column :label="$t('文件名')" prop="fileName" sortable="custom">
                <template slot-scope="props">
                  <span v-bk-tooltips="props.row.filePath">{{props.row.fileName}}</span>
                </template>
              </bk-table-column>
              <bk-table-column :label="$t('行号')" prop="lineNum" width="70"></bk-table-column>
              <bk-table-column v-if="false" :label="$t('版本号')" prop="revision" width="70"></bk-table-column>
              <bk-table-column :label="$t('规则')" prop="checker"></bk-table-column>
              <bk-table-column :label="$t('规则类型')" prop="displayCategory"></bk-table-column>
              <bk-table-column :label="$t('问题描述')" prop="message">
                <template slot-scope="props">
                  <span :title="props.row.message || props.row.displayType">{{props.row.message || props.row.displayType}}</span>
                </template>
              </bk-table-column>
              <bk-table-column :label="$t('处理人')" prop="authorList" min-width="70">
                <template slot-scope="props">
                  <div
                    v-if="props.row.status === 1"
                    @mouseenter="handleAuthorIndex(props.$index)"
                    @mouseleave="handleAuthorIndex(-1)"
                    @click.stop="handleAuthor(1, props.row.entityId, props.row.authorList && props.row.authorList)">
                    <span>{{props.row.authorList && props.row.authorList.join(';')}}</span>
                    <span v-if="hoverAuthorIndex === props.$index" class="bk-icon icon-edit2 fs18"></span>
                  </div>
                  <div v-else>
                    <span>
                      {{props.row.authorList && props.row.authorList.join(';')}}
                    </span>
                  </div>
                </template>
              </bk-table-column>
              <bk-table-column :label="$t('级别')" prop="severity" sortable="custom" width="80">
                <template slot-scope="props">
                  <span :class="`color-${{ 1: 'major', 2: 'minor', 4: 'info' }[props.row.severity]}`">{{defectSeverityMap[props.row.severity]}}</span>
                </template>
              </bk-table-column>
              <bk-table-column
                prop="createTime"
                sortable="custom"
                width="110"
                :label="$t('创建日期')"
                :formatter="(row, column, cellValue, index) => formatTime(cellValue, 'YYYY-MM-DD')">
              </bk-table-column>
              <bk-table-column :label="$t('首次发现')" prop="createBuildNumber" sortable="custom" width="100">
                <template slot-scope="props">
                  <span>{{props.row.createBuildNumber ? '#' + props.row.createBuildNumber : '--'}}</span>
                </template>
              </bk-table-column>
              <bk-table-column :label="$t('状态')" prop="status" width="110">
                <template slot-scope="props">
                  <span>{{handleStatus(props.row.status, props.row.defectIssueInfoVO)}}</span>
                </template>
              </bk-table-column>
              <bk-table-column :label="$t('操作')" width="60">
                <template slot-scope="props">
                  <!-- 已修复问题没有这些操作 -->
                  <span v-if="!(props.row.status & 2)" class="cc-operate-more" @click.prevent.stop>
                    <bk-popover theme="light" placement="bottom" trigger="click">
                      <span class="bk-icon icon-more guide-icon"></span>
                      <div slot="content" class="handle-menu-tips txal">
                        <!-- 待修复问题的操作 -->
                        <template v-if="props.row.status === 1">
                          <p v-if="props.row.mark" class="entry-link" @click.stop="handleMark(0, false, props.row.entityId)">
                            {{$t('取消标记')}}
                          </p>
                          <p v-else class="entry-link" @click.stop="handleMark(1, false, props.row.entityId)">
                            {{$t('标记处理')}}
                          </p>
                        </template>
                        <!-- 已忽略问题的操作 -->
                        <p v-if="props.row.status & 4" class="entry-link" @click="handleRevertIgnoreAndMark(props.row.entityId)">
                          {{$t('取消忽略并标记处理')}}
                        </p>
                        <p v-if="props.row.status & 4" class="entry-link" @click="handleRevertIgnoreAndCommit(props.row.entityId)">
                          {{$t('取消忽略并提单')}}
                        </p>
                        <p v-if="props.row.status & 4" class="entry-link" @click.stop="handleIgnore('RevertIgnore', false, props.row.entityId)">
                          {{$t('取消忽略')}}
                        </p>
                        <bk-popover v-else ref="guidePopover2" placement="left" theme="dot-menu light" trigger="click">
                          <div>
                            <span class="guide-flag"></span>
                            <span class="entry-link ignore-item" @click.stop="handleIgnore('IgnoreDefect', false, props.row.entityId)">{{$t('忽略问题')}}</span>
                          </div>
                          <div class="guide-content" slot="content">
                            <div>{{ $t('支持忽略无需处理或暂缓处理的问题。') }}</div>
                            <div>{{ $t('针对特定忽略类型可设置提醒，以便定期review和修复。') }}</div>
                            <div class="guide-btn">
                              <span class="btn-item mr5" @click="handleTableSetReview">{{ $t('设置提醒') }}</span>
                              <span class="btn-item" @click="handleTableGuideNextStep">{{ $t('知道了') }}</span>
                            </div>
                          </div>
                        </bk-popover>
                        <p v-if="props.row.status & 4" class="entry-link" @click.stop="handleChangeIgnoreType(props.row)">
                          {{$t('修改忽略类型')}}
                        </p>
                        <p v-if="props.row.status === 1 && !(props.row.defectIssueInfoVO.submitStatus && props.row.defectIssueInfoVO.submitStatus !== 4)"
                           class="entry-link"
                           @click.stop="handleCommit('commit', false, props.row.entityId)">
                          {{$t('提单')}}
                        </p>
                      </div>
                    </bk-popover>
                  </span>
                </template>
              </bk-table-column>
              <div slot="append" v-show="isFileListLoadMore">
                <div class="table-append-loading">
                  {{$t('正在加载第x-y个，请稍后···', { x: nextPageStartNum, y: nextPageEndNum })}}
                </div>
              </div>
              <div slot="empty">
                <div class="codecc-table-empty-text">
                  <img src="../../images/empty.png" class="empty-img">
                  <div>{{$t('暂无数据')}}</div>
                </div>
              </div>
            </bk-table>
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
            width="80%"
          >
            <div v-bkloading="{ isLoading: detailLoading, opacity: 0.6 }" :class="['code-fullscreen', { 'full-active': isFullScreen }]">

              <div class="col-aside-left">
                <div class="col-aside-left-header">
                  <bk-icon @click="defectDetailDialogVisiable = false" class="arrows-left" type="arrows-left" />
                  <div class="header-index">{{fileIndex + 1}}/{{totalCount}}</div>
                  <i class="codecc-icon icon-share" v-bk-tooltips="$t('分享此问题')" @click="shareVisiable = true"></i>
                  <i class="codecc-icon icon-operate" v-bk-tooltips="$t('键盘操作指引')" @click="operateDialogVisiable = true"></i>
                  <div class="dialog-block" v-if="shareVisiable">
                    <div class="share-header">{{$t('分享此问题')}}</div>
                    <i class="bk-icon icon-close" @click="shareVisiable = false"></i>
                    <div class="share-content" @click="shareDefect">
                      <i class="codecc-icon icon-link-2"></i>
                      <span>{{$t('复制链接')}}</span>
                    </div>
                  </div>
                </div>
                <defect-block
                  :list="defectList"
                  :defect-index="fileIndex"
                  :handle-file-list-row-click="handleFileListRowClick"
                  :is-file-list-load-more="isFileListLoadMore"
                  :defect-instances="detailLoading ? [] : (defectInstances || [])"
                  :trace-active-id="traceActiveId"
                  :next-page-start-num="nextPageStartNum"
                  :next-page-end-num="nextPageEndNum"
                  @clickTrace="clickTrace"
                  @scrollLoadMore="scrollLoadMore">
                </defect-block>
              </div>

              <div class="col-main">
                <b class="filename" :title="(currentTrace || {}).filePath">{{((currentTrace || {}).filePath || '').split('/').pop()}}</b>
                <div id="codeViewerInDialog" :class="isFullScreen ? 'full-code-viewer' : 'un-full-code-viewer'" @click="handleCodeViewerInDialogClick"></div>
              </div>

              <div class="col-aside">
                <div class="operate-section">
                  <div class="basic-info" :class="{ 'full-screen-info': isFullScreen }" v-if="currentFile">
                    <div class="block">
                      <div class="item disb">
                        <span class="fail" :class="{ 'cc-status': currentFile.mark }" v-if="currentFile.status === 1">
                          <span class="cc-dot"></span>
                          <span v-if="buildNum" v-bk-tooltips="`#${buildNum}待修复(当前分支最新构建#${lintDetail.lastBuildNumOfSameBranch}该问题为${lintDetail.defectIsFixedOnLastBuildNumOfSameBranch ? '已修复' : '待修复'})`">
                            #{{buildNum}}{{$t('待修复')}}
                            <span style="color: #63656e">(#{{lintDetail.lastBuildNumOfSameBranch}}{{lintDetail.defectIsFixedOnLastBuildNumOfSameBranch ? $t('已修复') : $t('待修复')}})</span>
                          </span>
                          <span v-else>{{$t('待修复')}}</span>
                          <span v-if="currentFile.defectIssueInfoVO.submitStatus && currentFile.defectIssueInfoVO.submitStatus !== 4">{{$t('(已提单)')}}</span>
                        </span>
                        <span class="success" v-else-if="currentFile.status & 2"><span class="cc-dot"></span>{{$t('已修复')}}</span>
                        <span class="warn" v-else-if="currentFile.status & 4"><span class="cc-dot"></span>{{$t('已忽略')}}</span>
                        <span v-if="currentFile.status === 1 && currentFile.mark" class="cc-mark disb">
                          <span class="cc-icon-mark"></span>
                          <span>{{$t('已标记处理')}}</span>
                        </span>
                      </div>
                      <div v-if="currentFile.status === 1" class="item">
                        <bk-button v-if="currentFile.mark" class="item-button" @click="handleMark(0, false, currentFile.entityId)">
                          {{$t('取消标记')}}
                        </bk-button>
                        <bk-button v-else theme="primary" class="item-button" @click="handleMark(1, false, currentFile.entityId)">
                          {{$t('标记处理')}}
                        </bk-button>
                      </div>
                      <div v-if="currentFile.status & 4">
                        <div class="item">
                          <bk-button class="item-button" @click="handleRevertIgnoreAndMark(currentFile.entityId)">
                            {{$t('取消忽略并标记处理')}}
                          </bk-button>
                        </div>
                        <div class="item">
                          <bk-button class="item-button" @click="handleRevertIgnoreAndCommit(currentFile.entityId)">
                            {{$t('取消忽略并提单')}}
                          </bk-button>
                        </div>
                      </div>
                      <div class="item">
                        <bk-button v-if="currentFile.status & 4" class="item-button" @click="handleIgnore('RevertIgnore', false, currentFile.entityId)">
                          {{$t('取消忽略')}}
                        </bk-button>
                        <bk-button v-else-if="!(currentFile.status & 2)" class="item-button" @click="handleIgnore('IgnoreDefect', false, currentFile.entityId)">
                          {{$t('忽略问题')}}
                        </bk-button>
                      </div>
                      <div class="item" v-if="currentFile.status & 4">
                        <bk-button class="item-button" @click="handleChangeIgnoreType(currentFile)">
                          {{$t('修改忽略类型')}}
                        </bk-button>
                      </div>
                      <div class="item">
                        <bk-button
                          v-if="currentFile.status === 1 && !(currentFile.defectIssueInfoVO.submitStatus && currentFile.defectIssueInfoVO.submitStatus !== 4)"
                          class="item-button"
                          @click="handleCommit('commit', false, currentFile.entityId)">
                          {{$t('提单')}}
                        </bk-button>
                      </div>
                    </div>
                    <div class="block">
                      <div class="item">
                        <dt>{{$t('ID')}}</dt>
                        <dd>{{currentFile.id}}</dd>
                      </div>
                      <div class="item">
                        <dt>{{$t('级别')}}</dt>
                        <dd>{{defectSeverityMap[currentFile.severity]}}</dd>
                      </div>
                    </div>
                    <div class="block">
                      <div class="item">
                        <dt>{{$t('创建时间')}}</dt>
                        <dd>{{formatTime(currentFile.createTime, 'YYYY-MM-DD')}}</dd>
                      </div>
                      <div class="item" v-if="currentFile.status & 2">
                        <dt>{{$t('修复时间')}}</dt>
                        <dd>{{formatTime(currentFile.fixedTime, 'YYYY-MM-DD')}}</dd>
                      </div>
                      <div class="item">
                        <dt v-if="currentFile.status === 1" class="curpt" @click.stop="handleAuthor(1, currentFile.entityId, currentFile.authorList)">
                          {{$t('处理人')}}
                          <span class="bk-icon icon-edit2 fs20"></span>
                        </dt>
                        <dt v-else>
                          {{$t('处理人')}}
                        </dt>
                        <dd>{{currentFile.authorList && currentFile.authorList.join(';')}}</dd>
                      </div>
                    </div>
                    <div class="block" v-if="currentFile.status & 4">
                      <div class="item">
                        <dt>{{$t('忽略时间')}}</dt>
                        <dd>{{formatTime(currentFile.ignoreTime, 'YYYY-MM-DD')}}</dd>
                      </div>
                      <div class="item">
                        <dt>{{$t('忽略人')}}</dt>
                        <dd>{{currentFile.ignoreAuthor}}</dd>
                      </div>
                      <div class="item disb">
                        <dt>{{$t('忽略原因')}}</dt>
                        <dd>{{getIgnoreReasonByType(currentFile.ignoreReasonType)}}
                          {{currentFile.ignoreReason ? '：' + currentFile.ignoreReason : ''}}
                        </dd>
                      </div>
                    </div>
                    <div class="block">
                      <div class="item disb">
                        <dt>{{$t('工具')}}</dt>
                        <dd>{{(toolMap[currentFile.toolName] || {}).displayName || ''}}</dd>
                      </div>
                      <div class="item disb">
                        <dt>{{$t('规则')}}</dt>
                        <dd>{{currentFile.checker}}</dd>
                      </div>
                    </div>
                    <div class="block">
                      <div class="item disb">
                        <dt>{{$t('代码库路径')}}</dt>
                        <dd>{{(currentFile.filePath || '')}}</dd>
                      </div>
                      <div class="item disb">
                        <dt>{{$t('版本号')}}</dt>
                        <dd>{{currentFile.revision || '--'}}</dd>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </bk-dialog>

        </div>
      </div>
      <bk-dialog
        v-model="authorEditDialogVisiable"
        width="560"
        theme="primary"
        :mask-close="false"
        header-position="left"
        :title="operateParams.changeAuthorType === 1 ? $t('修改问题处理人') : $t('批量修改问题处理人')"
      >
        <div class="author-edit">
          <div class="tips" v-if="operateParams.changeAuthorType === 3"><i class="bk-icon icon-info-circle"></i>{{$t('原处理人所有函数都将转给新处理人')}}</div>
          <bk-form :model="operateParams" :label-width="130" class="search-form">
            <bk-form-item v-if="operateParams.changeAuthorType !== 2"
                          property="sourceAuthor"
                          :label="$t('原处理人')">
              <bk-input v-model="operateParams.sourceAuthor" :disabled="operateParams.changeAuthorType === 1" style="width: 290px;"></bk-input>
            </bk-form-item>
            <bk-form-item :label="$t('新处理人')">
              <bk-tag-input allow-create v-model="operateParams.targetAuthor" style="width: 290px;"></bk-tag-input>
            </bk-form-item>
          </bk-form>
        </div>
        <div slot="footer">
          <bk-button
            type="button"
            theme="primary"
            :disabled="(operateParams.changeAuthorType === 3 && !operateParams.sourceAuthor) || !operateParams.targetAuthor"
            :loading="authorEditDialogLoading"
            @click.native="handleAuthorEditConfirm"
          >
            {{$t('确定')}}
          </bk-button>
          <bk-button
            theme="primary"
            type="button"
            :disabled="authorEditDialogLoading"
            @click.native="authorEditDialogVisiable = false"
          >
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
          <div class="mb20">{{ $t('忽略类型') }}</div>
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
                  <span v-if="ignore.ignoreTypeId === 42">
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
          <div v-if="!isAddingIgnore" style="position: absolute; bottom: 18px;" @click="handleToIgnoreList">
            <bk-button text>{{ $t('缺少合适忽略类型？') }}</bk-button>
          </div>
          <div v-else style="position: absolute; bottom: 18px; cursor: pointer;" @click="handleToIgnoreList">
            <bk-button text>{{ $t('新增忽略类型点此刷新') }}</bk-button>
            <i class="codecc-icon icon-refresh-2 f12 ml5" style="color: #3A84FF; position: relative; bottom: 1px;"></i>
          </div>
          <bk-button
            theme="primary"
            :disabled="ignoreReasonAble"
            @click.native="handleIgnoreConfirm"
          >
            {{$t('确定')}}
          </bk-button>
          <bk-button
            theme="primary"
            @click.native="handleIgnoreCancel"
          >
            {{$t('取消')}}
          </bk-button>
        </div>
      </bk-dialog>
      <operate-dialog :visiable.sync="operateDialogVisiable"></operate-dialog>
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
          {{this.$t('不再提示')}}
        </bk-checkbox>
        <template slot="footer">
          <bk-button theme="primary" @click="toLogs()">{{$t('去配置')}}</bk-button>
          <bk-button @click="changeHandlerVisiable = false">{{$t('取消')}}</bk-button>
        </template>
      </bk-dialog>
    </section>
    <div class="coverity-list" v-else>
      <div class="main-container large boder-none">
        <div class="no-task">
          <empty title="" :desc="$t('CodeCC集成了十余款工具，支持检查代码缺陷、安全漏洞、代码规范等问题')">
            <template v-slot:action>
              <bk-button size="large" theme="primary" @click="addTool({ from: 'cov' })">{{$t('配置规则集')}}</bk-button>
            </template>
          </empty>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  import _ from 'lodash'
  import { mapState } from 'vuex'
  import { bus } from '@/common/bus'
  import { getClosest, addClass, hasClass } from '@/common/util'
  import util from '@/mixins/defect-list'
  import defectCache from '@/mixins/defect-cache'
  import CodeMirror from '@/common/codemirror'
  import Empty from '@/components/empty'
  import DatePicker from '@/components/date-picker/index'
  import filterSearchOption from './filter-search-option'
  import { format } from 'date-fns'
  // eslint-disable-next-line
  import { export_json_to_excel } from 'vendor/export2Excel'
  import DefectBlock from './defect-block/defect-block'
  import OperateDialog from '@/components/operate-dialog'

  // 搜索过滤项缓存
  const COVERITY_SEARCH_OPTION_CACHE = 'search_option_columns_coverity'

  export default {
    components: {
      Empty,
      DatePicker,
      filterSearchOption,
      DefectBlock,
      OperateDialog,
    },
    mixins: [util, defectCache],
    data() {
      this.getDefaultOption = () => ([
        { id: 'dimension', name: this.$t('维度'), isChecked: true },
        { id: 'toolName', name: this.$t('工具'), isChecked: true },
        { id: 'checkerSet', name: this.$t('single.规则集'), isChecked: true },
        { id: 'buildId', name: this.$t('快照'), isChecked: true },
      ])

      this.getCustomOption = function (val) {
        return [
          { id: 'checker', name: this.$t('规则'), isChecked: val },
          { id: 'author', name: this.$t('处理人'), isChecked: val },
          { id: 'daterange', name: this.$t('日期'), isChecked: val },
          { id: 'path', name: this.$t('路径'), isChecked: val },
          { id: 'status', name: this.$t('状态'), isChecked: val },
          { id: 'ignoreType', name: this.$t('忽略类型'), isChecked: val },
          { id: 'severity', name: this.$t('级别'), isChecked: val },
        ]
      }
      const { query } = this.$route
      const { toolId } = this.$route.params
      let status = [1]
      if (query.status) {
        status = this.numToArray(query.status)
      }
      let ignoreReasonTypes = []
      if (query.ignoreTypeId) {
        ignoreReasonTypes = query.ignoreTypeId.split(',').map(i => Number(i))
      }

      return {
        contentLoading: false,
        detailLoading: false,
        panels: [
          { name: 'defect', label: this.$t('问题管理') },
          { name: 'report', label: this.$t('数据报表') },
        ],
        tableLoading: false,
        fileLoading: false,
        isSearch: false,
        defectSeverityMap: {
          1: this.$t('严重'),
          2: this.$t('一般'),
          4: this.$t('提示'),
        },
        toolPattern: '',
        dimension: query.dimension || '',
        listData: {
          defectList: {
            content: [],
            totalElements: 0,
          },
        },
        lintDetail: {},
        searchFormData: {
          checkerSetList: [{}],
          filePathTree: {},
          filePathShow: this.handleFileList(query.fileList).join(';'),
        },
        searchParams: {
          taskId: this.$route.params.taskId,
          dimension: query.dimension || '',
          toolName: toolId || '',
          checker: query.checker || '',
          checkerSet: query.checkerSet || '',
          author: query.author,
          severity: this.numToArray(query.severity),
          status,
          buildId: query.buildId ? query.buildId : '',
          fileList: this.handleFileList(query.fileList),
          daterange: [query.startTime, query.endTime],
          sortField: query.sortField || 'severity',
          sortType: 'DESC',
          ignoreReasonTypes,
          pageNum: 1,
          pageSize: 100,
        },
        defectDetailSearchParams: {
          sortField: '',
          sortType: '',
          pattern: '',
          filePath: '',
          toolName: toolId || '',
          entityId: undefined,
        },
        codeViewerInDialog: null,
        isFilePathDropdownShow: false,
        isFileListLoadMore: false,
        isDefectListLoadMore: false,
        defectDetailDialogVisiable: false,
        authorEditDialogVisiable: false,
        ignoreReasonDialogVisiable: false,
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
        isBatchOperationShow: false,
        searchInput: '',
        emptyText: this.$t('未选择文件'),
        hoverAuthorIndex: -1,
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
        detailContent: this.$t('待补充...'),
        memberNeverShow: false,
        newDefectJudgeTime: '',
        buildList: [],
        isSelectAll: '',
        dateType: query.dateType || 'createTime',
        cacheConfig: {
          length: 0,
        },
        isFetched: false,
        lineAverageOpt: 10,
        isSearchDropdown: true,
        isFullScreen: true,
        exportLoading: false,
        selectLoading: {
          otherParamsLoading: false,
          buildListLoading: false,
        },
        defaultOption: this.getDefaultOption(),
        customOption: this.getCustomOption(true),
        selectedOptionColumn: [],
        shareVisiable: false,
        traceDataList: [], // trace 平铺列表
        currentTrace: null,
        currentTraceIndex: 0,
        traceActiveId: '',
        startLine: 1,
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
        guideFlag: false,
      }
    },
    computed: {
      ...mapState('tool', {
        toolMap: 'mapList',
      }),
      ...mapState('task', {
        taskDetail: 'detail',
      }),
      typeTips() {
        return this.$t('起始时间x之后产生的问题为新问题', { accessTime: this.newDefectJudgeTime })
      },
      defectList(val) {
        return this.listData.defectList.content
      },
      currentFile() {
        return { ...this.lintDetail.defectDetailVO }
        // return this.defectList[this.fileIndex]
      },
      statusTypeMap() {
        const { existCount, fixCount, ignoreCount, maskCount } = this.listData
        return {
          1: `${this.$t('待修复')}(${existCount || 0})`,
          2: `${this.$t('已修复')}(${fixCount || 0})`,
          4: `${this.$t('已忽略')}(${ignoreCount || 0})`,
          8: `${this.$t('已屏蔽')}(${maskCount || 0})`,
        }
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
      buildNum() {
        const { buildList } = this
        const buildItem = buildList.find(item => item.buildId === this.searchParams.buildId) || {}
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
      defectInstances() {
        const { defectInstances = [] } = this.currentFile
        const traceDataList = []
        let deep = 1
        const handleLinkTrace = function (linkTrace = [], id, index) {
          linkTrace.forEach((link, linkIndex) => {
            const linkId = `${id}-${linkIndex}`
            const newIndex = `${index}.${link.traceNum}`
            traceDataList.push(link)
            link.id = linkId
            link.index = newIndex
            // 目前只需要展示2层linkTrace
            if (deep < 2) {
              deep += 1
              handleLinkTrace(link.linkTrace, linkId, newIndex)
            } else {
              link.linkTrace = []
            }
          })
        }
        /**
         * 把示例集中放到最后
         * @param {*} list
         */
        const sortExample = function (list) {
          const normal = []
          const multi = []
          list.forEach((item) => {
            if (item.kind === 'MULTI') {
              multi.push(item)
            } else {
              normal.push(item)
            }
          })
          return [...normal, ...multi]
        }
        defectInstances.forEach((instance, instanceIndex) => {
          instance.traces = sortExample(instance.traces)
          instance.traces.forEach((trace, traceIndex) => {
            const id = `${instanceIndex}-${traceIndex}`
            traceDataList.push(trace)
            trace.id = id
            trace.index = trace.traceNum
            trace.expanded = !!trace.main
            deep = 1
            handleLinkTrace(trace.linkTrace, id, trace.traceNum)
          })
        })
        this.traceDataList = traceDataList
        return defectInstances
      },
      ignoreReasonDialogTitle() {
        if (this.isRowChangeIgnoreType) {
          return this.$t('忽略')
        }
        return (this.isSelectAll === 'Y' ? this.totalCount : this.selectedLen) > 1
          ? `${this.$t('忽略')}（
          ${this.$t('共x个问题', { num: this.isSelectAll === 'Y' ? this.totalCount : this.selectedLen })}）`
          : this.$t('忽略')
      },
      dimensionStr() {
        const { dimension = '' } = this
        return typeof dimension === 'string' ? dimension : dimension.join(',')
      },
      toolNameStr() {
        const { toolName = '' } = this.searchParams
        return typeof toolName === 'string' ? toolName : toolName.join(',')
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
            this.$refs.fileListTable.$refs.bodyWrapper.scrollTo(0, 0)
            return
          }
          if (this.isSearch) {
            this.tableLoading = true
            this.fetchLintList().then((list) => {
              if (this.pageChange) {
                // 将一页的数据追加到列表
                this.listData.defectList.content = this.listData.defectList.content
                  .concat(list.defectList.content)

                // 隐藏加载条
                this.isFileListLoadMore = false

                // 重置页码变更标记
                this.pageChange = false
              } else {
                this.listData = { ...this.listData, ...list }
                this.totalCount = this.listData.defectList.totalElements
                this.pagination.count = this.listData.defectList.totalElements
                // 重置文件下的问题详情
                this.lintDetail = {}
              }
            })
              .finally(() => {
                // this.fileIndex = 0
                this.addTableScrollEvent()
                this.tableLoading = false
              })
          }
        },
        deep: true,
      },
      defectDetailSearchParams: {
        handler(val) {
          this.emptyText = this.$t('未选择文件')
          this.detailLoading = true
          this.operateParams.ignoreReason = ''
          this.operateParams.ignoreReasonType = ''
          // bus.$emit('show-app-loading')
          this.defectDetailDialogVisiable = true
          // const { entityId } = val
          // if (this.defectCache[entityId]) {
          //   this.detailContent = this.checkerContentCache && this.checkerContentCache[this.checkerKey]
          //     && this.checkerContentCache[this.checkerKey].codeExample
          //   this.lintDetail = this.defectCache[entityId]
          //   this.handleCodeFullScreen()
          //   this.fetchLintDetail()
          // } else {
          //   this.fetchLintDetail('first')
          // }
          this.fetchLintDetail('first')
          // this.preloadCache(this.defectList, this.cacheConfig)
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
      defectDetailDialogVisiable(val) {
        if (!val) {
          this.codeViewerInDialog.setValue('')
          this.codeViewerInDialog.setOption('firstLineNumber', 1)
        }
      },
      changeHandlerVisiable: {
        handler() {
          if (!this.changeHandlerVisiable) {
            window.localStorage.setItem('memberNeverShow', JSON.stringify(this.memberNeverShow))
          }
        },
        deep: true,
      },
      taskDetail: {
        handler(newVal) {
          this.$nextTick(() => {
            this.getQueryPreLineNum()
          })
        },
        deep: true,
      },
      defectList(val, oldVal) {
        this.preloadCache(val, this.cacheConfig)
        this.setTableHeight()
      },
      'defectDetailSearchParams.toolName'(val, oldVal) {
        if (val) {
          this.toolPattern = this.toolMap[val].pattern
        }
      },
      listData(val) {
        if (val.authorMap) {
          const user = this.user.username
          const authorValue = val.authorMap[user] || 0
          this.listData.authorMap[user] = authorValue
        }
      },
    },
    created() {
      if (!this.taskDetail.nameEn
        || this.taskDetail.enableToolList.find(item => item.toolName === 'COVERITY'
          || item.toolName === 'KLOCWORK' || item.toolName === 'PINPOINT')) {
        this.init(true)
      }
      if (this.isFromOverview) this.isFetched = true

      // 读取缓存搜索过滤项
      const columnsCache = JSON.parse(localStorage.getItem(COVERITY_SEARCH_OPTION_CACHE))
      if (columnsCache) {
        this.selectedOptionColumn = _.cloneDeep(columnsCache)
        this.customOption = columnsCache
      } else {
        this.selectedOptionColumn = this.getCustomOption(true)
      }
      this.guideFlag = localStorage.getItem('guideEnd') || ''
    },
    async mounted() {
      const memberNeverShow = JSON.parse(window.localStorage.getItem('memberNeverShow'))
      memberNeverShow === null
        ? this.memberNeverShow = false
        : this.memberNeverShow = memberNeverShow
      // 读取缓存中搜索项首次展示或收起
      const lintSearchExpend = JSON.parse(window.localStorage.getItem('lintSearchExpend'))
      lintSearchExpend === null
        ? this.isSearchDropdown = true
        : this.isSearchDropdown = lintSearchExpend
      window.addEventListener('resize', this.getQueryPreLineNum)
      this.openDetail()
      this.keyOperate()
      this.handelFetchIgnoreList()
    },
    beforeDestroy() {
      window.removeEventListener('resize', this.getQueryPreLineNum)
      document.onkeydown = null
    },
    methods: {
      async init(isInit) {
        this.getQueryPreLineNum()
        isInit ? this.contentLoading = true : this.fileLoading = true
        this.selectLoading.otherParamsLoading = true
        const list = await this.fetchLintList()
        if (list.tips) {
          this.handleNewBuildId(list.tips)
        }

        this.selectLoading.buildListLoading = true
        this.buildList = await this.$store.dispatch(
          'defect/getBuildList',
          { taskId: this.$route.params.taskId },
        )
        this.selectLoading.buildListLoading = false

        this.newDefectJudgeTime = (list.newDefectJudgeTime
          ? this.formatTime(list.newDefectJudgeTime, 'YYYY-MM-DD')
          : '')
        this.listData = { ...this.listData, ...list }
        this.totalCount = this.listData.defectList.totalElements
        this.pagination.count = this.listData.defectList.totalElements
        if (isInit) {
          this.fetchCheckerSetParams()
          this.contentLoading = false
          this.isFetched = true
        } else {
          this.tableLoading = false
        }
        this.isSearch = true
        this.addTableScrollEvent()
        const { filePathTree } = list

        this.searchFormData = { ...this.searchFormData, filePathTree }
        this.selectLoading.otherParamsLoading = false

        this.showIgnoreTypeSelect()
      },
      async fetchCheckerSetParams() {
        const params = {
          taskId: this.$route.params.taskId,
          toolName: this.searchParams.toolName,
          dimension: this.dimension,
        }
        const res = await this.$store.dispatch('checkerset/listForDefect', params)
        const checkerSetList = res.filter(checkerSet => checkerSet.taskUsing)
        this.searchFormData = Object.assign(this.searchFormData, { checkerSetList })
      },
      fetchLintList() {
        const params = this.getSearchParams()

        return this.$store.dispatch('defect/lintList', params)
      },
      async fetchLintDetail(type, extraParams = {}) {
        const params = {
          ...this.searchParams,
          ...this.defectDetailSearchParams,
          pattern: this.toolPattern,
          ...extraParams,
        }
        const detail = await this.$store.dispatch('defect/lintDetail', params)
        if (detail.defectDetailVO.ignoreReasonType) {
          this.operateParams.ignoreReasonType = detail.defectDetailVO.ignoreReasonType
        }
        if (detail.defectDetailVO.ignoreReason) {
          this.operateParams.ignoreReason = detail.defectDetailVO.ignoreReason
        }
        if (detail.defectDetailVO) {
          // 获取规则详情
          const checkerContent = await this.getWarnContent(detail.defectDetailVO.checker) || {}
          if (!this.checkerContentCache) {
            this.checkerContentCache = {}
          }
          this.checkerContentCache[this.checkerKey] = checkerContent
          this.detailContent = checkerContent.codeExample
          if (!extraParams.entityId) {
            this.lintDetail = detail
            // 把main trace设为当前trace
            const tracesList = (this.defectInstances[0] && this.defectInstances[0].traces) || [{}]
            this.currentTrace = tracesList.find(item => item.main) || tracesList[tracesList.length - 1]
            this.traceActiveId = this.currentTrace.id
            this.currentTraceIndex = this.traceDataList.findIndex(item => item.id === this.currentTrace.id)
            this.preLineNum = undefined

            // 查询详情后，全屏显示问题
            if (type === 'first') {
              this.handleCodeFullScreen()
            }
            const cacheConfig = { ...this.cacheConfig, index: this.fileIndex }
            this.preloadCache(this.defectList, cacheConfig)
          }
          const updateCacheKey = params.entityId
          this.updateCache(updateCacheKey, detail)
          if (this.isOpenDetail) {
            this.handleInitFileIndex()
            this.isOpenDetail = false
          }
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
                this.$store.dispatch('defect/oauthUrl', { toolName: this.searchParams.toolName }).then((res) => {
                  window.open(res, '_blank')
                })
              },
            })
          }, 500)
        } else if (detail.response) {
          this.cacheConfig.length = 0
          this.preloadCache(this.defectList, this.cacheConfig)
          this.clear()
        }
        this.detailLoading = false
      },
      getWarnContent(checkerKey) {
        if (this.checkerContentCache && this.checkerContentCache[this.checkerKey]) {
          return this.checkerContentCache[this.checkerKey]
        }
        return this.$store.dispatch(
          'defect/getWarnContent',
          { toolName: this.defectDetailSearchParams.toolName, checkerKey: this.checkerKey || checkerKey },
        )
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
              if (!vm.defectDetailDialogVisiable && !vm.authorEditDialogVisiable && e.path.length < 5) {
                vm.keyEnter()
              }
              break
            case 'Escape': // esc
              if (vm.defectDetailDialogVisiable) vm.defectDetailDialogVisiable = false
              break
            case 'ArrowLeft': // left
            case 'ArrowUp': // up
            case 'KeyW': // w
              if (e.shiftKey) {
                if (vm.defectDetailDialogVisiable && vm.currentTraceIndex) {
                  vm.currentTraceIndex -= 1
                  const trace = vm.traceDataList[vm.currentTraceIndex]
                  vm.clickTrace(trace)
                }
              } else {
                if (vm.fileIndex > 0) {
                  if (vm.defectDetailDialogVisiable) {
                    vm.handleFileListRowClick(vm.defectList[vm.fileIndex -= 1])
                  } else {
                    vm.fileIndex -= 1
                  }
                  vm.addCurrentRowClass()
                  vm.screenScroll()
                }
              }
              break
            case 'ArrowRight': // right
            case 'ArrowDown': // down
            case 'KeyS': // s
              if (e.shiftKey) {
                if (vm.defectDetailDialogVisiable && vm.currentTraceIndex < vm.traceDataList.length - 1) {
                  vm.currentTraceIndex += 1
                  const trace = vm.traceDataList[vm.currentTraceIndex]
                  vm.clickTrace(trace)
                }
              } else {
                if (vm.fileIndex < vm.defectList.length - 1) {
                  if (vm.defectDetailDialogVisiable) {
                    vm.handleFileListRowClick(vm.defectList[vm.fileIndex += 1])
                  } else {
                    vm.fileIndex += 1
                  }
                  vm.addCurrentRowClass()
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
          this.fileLoading = false
          this.addCurrentRowClass()
          if (this.$refs.fileListTable) {
            const tableBodyWrapper = this.$refs.fileListTable.$refs.bodyWrapper

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
        const count = this.listData[severityFieldMap[severity]] || 0
        return count > 100000 ? this.$t('10万+') : count
      },
      getDefectCountByType(type) {
        const tpyeFieldMap = {
          1: 'newCount',
          2: 'historyCount',
        }
        const count = this.listData[tpyeFieldMap[type]] || 0
        return count > 100000 ? this.$t('10万+') : count
      },
      getIgnoreReasonByType(type) {
        const typeMap = this.ignoreList.reduce((result, item) => {
          result[item.ignoreTypeId] = item.name
          return result
        }, {})
        return typeMap[type]
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
        this.isBatchOperationShow = Boolean(selection.length)
        // 如果长度是最长，那么就是Y，否则是N
        this.isSelectAll = this.selectedLen === this.defectList.length ? 'Y' : 'N'
      },
      handleFileListRowClick(row, event, column) {
        this.checkerKey = row.checker
        // this.$store.dispatch('defect/getWarnContent', { toolName: this.toolId, checkerKey: row.checker }).then(res => {
        //     this.detailContent = res.codeExample
        // })
        this.fileIndex = this.defectList.findIndex(file => file.entityId === row.entityId)
        // 筛选后，问题详情为空，此时要把参数强制置空，不然点击文件不能触发请求
        if (!this.lintDetail.lintDefectList) {
          this.defectDetailSearchParams.entityId = ''
        }
        this.defectDetailSearchParams.entityId = row.entityId
        this.defectDetailSearchParams.filePath = row.filePath
        this.defectDetailSearchParams.toolName = row.toolName
        this.addCurrentRowClass()
        this.screenScroll()
      },
      keyEnter() {
        this.checkerKey = this.defectList[this.fileIndex].checker
        // this.checkerContentCache
        // this.$store.dispatch('defect/getWarnContent', { toolName: this.toolId, checkerKey: this.defectList[this.fileIndex].checker }).then(res => {
        //     this.detailContent = res.codeExample
        // })
        const row = this.defectList[this.fileIndex]
        if (!this.lintDetail.lintDefectList) {
          this.defectDetailSearchParams.entityId = ''
        }
        this.defectDetailSearchParams.entityId = row.entityId
        this.defectDetailSearchParams.filePath = row.filePath
      },
      // 表格行加当前高亮样式
      addCurrentRowClass() {
        const defectListTable = this.$refs.fileListTable
        if (defectListTable) {
          const defectListTableBodyWrapper = defectListTable.$refs.bodyWrapper
          const rows = defectListTableBodyWrapper.querySelectorAll('tr')
          const currentRow = rows[this.fileIndex]
          if (rows.length && currentRow) {
            rows.forEach(el => el.classList.remove('current-row'))
            addClass(currentRow, 'current-row')
          }
        }
      },
      handleCodeFullScreen(showDefectDetail) {
        if (!this.codeViewerInDialog) {
          setTimeout(() => {
            const codeMirrorConfig = {
              ...this.codeMirrorDefaultCfg,
              autoRefresh: true,
            }
            this.codeViewerInDialog = CodeMirror(
              document.getElementById('codeViewerInDialog'),
              codeMirrorConfig,
            )

            this.codeViewerInDialog.on('update', () => {})
            this.updateCodeViewer(this.codeViewerInDialog)
            this.codeViewerInDialog.refresh()
            setTimeout(this.scrollIntoView, 10)
            if (!window.localStorage.getItem('opreate-keyboard-20220411')) {
              this.operateDialogVisiable = true
            }
            return false
          }, 250)
        }
        this.updateCodeViewer(this.codeViewerInDialog, showDefectDetail)
        setTimeout(this.scrollIntoView, 10)
      },
      // 代码展示相关
      updateCodeViewer(codeViewer, showDefectDetail) {
        if (!codeViewer) return
        const { fileMd5 } = this.currentTrace
        const { contents, startLine, filePath } = this.currentFile.fileInfoMap[fileMd5]
        if (!contents) {
          this.emptyText = this.$t('文件内容为空')
          return
        }
        this.currentFile.startLine = startLine
        const codeMirrorMode = CodeMirror.findModeByFileName(filePath)
        if (codeMirrorMode && codeMirrorMode.mode) {
          const { mode } = codeMirrorMode
          import(`codemirror/mode/${mode}/${mode}.js`).then((m) => {
              codeViewer.setOption('mode', mode)
          })
        }
        if (codeViewer) {
          codeViewer.setValue(contents)
          codeViewer.setOption('firstLineNumber', startLine === 0 ? 1 : startLine)
          this.buildLintHints(codeViewer, startLine, showDefectDetail)
        }
      },
      // 创建问题提示块
      buildLintHints(codeViewer, startLine, showDefectDetail) {
        const { id, fileMd5 } = this.currentTrace
        const idList = id.split('-')
        let defectList = this.defectInstances[idList[0]].traces
        if (idList.length > 2) {
          for (let i = 1; i < idList.length - 1; i++) {
            defectList = defectList[idList[i]].linkTrace
          }
        }
        const { detailContent } = this
        this.currentFile.locatedIndex = 0
        defectList.forEach((defect) => {
          if (defect.fileMd5 !== fileMd5) return
          const { index, lineNum, message, main, kind } = defect
          const { displayType, checker } = this.currentFile
          const messageDom = document.createElement('div')
          messageDom.className = 'checker-detail'
          messageDom.style.maxHeight = '300px'
          messageDom.style.overflow = 'auto'
          messageDom.innerHTML = detailContent || this.$t('待补充...')

          const checkerDom = document.createElement('p')
          checkerDom.innerText = `${index && kind !== 'MULTI' ? `${index}.` : ''}${message || ''}`
          const hints = document.createElement('div')
          if (main) {
            hints.innerHTML = `
                            <div class="lint-info-main">
                                <i class="lint-icon bk-icon icon-right-shape"></i>
                                <p>${checkerDom.innerText}</p>
                                <p>${displayType}(${checker})</p>
                            </div>
                            <div>${messageDom.outerHTML}</div>
                        `
            codeViewer.addLineClass(lineNum - startLine, 'wrap', 'lint-hints-wrap main')
          } else {
            hints.innerHTML = `
                            <div class="lint-info">
                                <p>${checkerDom.outerHTML}</p>
                            </div>
                        `
            codeViewer.addLineClass(lineNum - startLine, 'wrap', 'lint-hints-wrap')
          }
          hints.className = showDefectDetail ? `lint-hints active ${kind}` : `lint-hints ${kind}`
          codeViewer.addLineWidget(lineNum - startLine, hints, {
            coverGutter: false,
            noHScroll: false,
            above: true,
          })
        })
        this.scrollIntoView()
      },
      // 问题上下文
      scrollTrace() {
        const { lineNum } = this.currentTrace
        const { startLine } = this.currentFile
        const codeViewer = this.codeViewerInDialog
        if (!codeViewer || !lineNum) return false
        if (this.preLineNum) {
          codeViewer.removeLineClass(this.preLineNum - startLine, 'wrap', 'defect-trace')
        }
        codeViewer.addLineClass(lineNum - startLine, 'wrap', 'defect-trace')
        this.scrollIntoView(lineNum)
      },
      // 默认滚动到问题位置
      scrollIntoView(number) {
        const codeViewer = this.codeViewerInDialog
        if (!codeViewer || !codeViewer.getScrollerElement()) return false
        const { startLine = 0 } = this.currentFile
        const middleHeight = codeViewer.getScrollerElement().offsetHeight / 2
        const lineHeight = codeViewer.defaultTextHeight()
        const { lineNum = number } = this.currentTrace
        if (!number) {
          codeViewer.removeLineClass(lineNum - startLine, 'wrap', 'defect-trace')
        }
        setTimeout(() => {
          codeViewer.scrollIntoView({ line: lineNum - startLine, ch: 0 }, middleHeight - lineHeight)
          // bus.$emit('hide-app-loading')
          this.detailLoading = false
        }, 1)
      },
      handleCodeViewerInDialogClick(event, eventSource) {
        this.codeViewerClick(event, 'dialog-code')
      },
      codeViewerClick(event, eventSource) {
        const lintInfo = getClosest(event.target, '.icon-right-shape')

        // 如果点击的是lint问题区域，展开修复建议
        if (lintInfo) {
          const lintHints = getClosest(lintInfo, '.lint-hints')
          const showDefectDetail = hasClass(lintHints, 'active')
          this.handleCodeFullScreen(!showDefectDetail)
          // toggleClass(lintHints, 'active')
        }
      },
      handleFilePathCancelClick() {
        const { filePathDropdown } = this.$refs
        filePathDropdown.hide()
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
      handleStatus(status, defectIssueInfoVO = {}) {
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
        let issueStatus = ''
        if (defectIssueInfoVO.submitStatus && defectIssueInfoVO.submitStatus !== 4) {
          issueStatus = this.$t('(已提单)')
        }
        return `${statusMap[key]}${issueStatus}`
      },
      handleSelectable(row, index) {
        return !(row.status & 2)
      },
      handleMark(markFlag, batchFlag, entityId) {
        // markFlag 0: 取消标记, 1: 标记修改
        // batchFlag true: 批量操作
        let defectKeySet = []
        if (batchFlag) {
          defectKeySet = this.$refs.fileListTable.selection.map(item => item.entityId)
        } else {
          defectKeySet = [entityId]
        }
        const bizType = 'MarkDefect'
        let data = { ...this.operateParams, bizType, defectKeySet, markFlag }
        if (this.isSelectAll === 'Y') {
          data = { ...data, isSelectAll: 'Y', queryDefectCondition: JSON.stringify(this.getSearchParams()) }
        }
        this.tableLoading = true
        this.$store.dispatch('defect/batchEdit', data).then((res) => {
          if (res.code === '0') {
            this.$bkMessage({
              theme: 'success',
              message: markFlag
                ? this.$t('标记为已处理成功') : this.$t('取消标记成功'),
            })
            if (batchFlag) {
              this.init()
            } else {
              this.listData.defectList.content.forEach((item) => {
                if (item.entityId === entityId) {
                  item.mark = markFlag
                }
              })
              this.listData.defectList.content = this.listData.defectList.content.slice()
            }
            if (this.defectDetailDialogVisiable) this.fetchLintDetail()
          }
        })
          .catch((e) => {
            console.error(e)
          })
          .finally(() => {
            this.tableLoading = false
          })
      },
      handleAuthorIndex(index) {
        this.hoverAuthorIndex = index
      },
      handleAuthor(changeAuthorType, id, author) {
        this.authorEditDialogVisiable = true
        this.operateParams.changeAuthorType = changeAuthorType
        this.operateParams.sourceAuthor = author
        this.operateParams.defectKeySet = [id]
      },
      // 处理人修改
      handleAuthorEditConfirm() {
        let data = this.operateParams
        const sourceAuthor = data.sourceAuthor ? new Set(data.sourceAuthor) : new Set()
        if (data.changeAuthorType === 2) {
          const defectKeySet = []
          this.$refs.fileListTable.selection.forEach((item) => {
            defectKeySet.push(item.entityId)
            item.authorList && sourceAuthor.add(...item.authorList)
          })
          data.defectKeySet = defectKeySet
          // data.defectKeySet = this.$refs.fileListTable.selection.map(item => item.entityId)
        }
        data.bizType = 'AssignDefect'
        data.sourceAuthor = Array.from(sourceAuthor)
        data.newAuthor = data.targetAuthor
        if (this.isSelectAll === 'Y') {
          data = { ...data, isSelectAll: 'Y', queryDefectCondition: JSON.stringify(this.getSearchParams()) }
        }
        const dispatchUrl = data.changeAuthorType === 3 ? 'defect/authorEdit' : 'defect/batchEdit'
        this.authorEditDialogVisiable = false
        this.tableLoading = true
        this.$store.dispatch(dispatchUrl, data).then((res) => {
          if (res.code === '0') {
            this.$bkMessage({
              theme: 'success',
              message: this.$t('修改处理人成功'),
            })
            this.operateParams.sourceAuthor = []
            this.operateParams.targetAuthor = []
            if (data.changeAuthorType === 1) {
              this.listData.defectList.content.forEach((item) => {
                if (item.entityId === data.defectKeySet[0]) {
                  item.authorList = data.newAuthor
                }
              })
              this.listData.defectList.content = this.listData.defectList.content.slice()
            } else {
              this.init()
            }
            if (this.defectDetailDialogVisiable) this.fetchLintDetail()
          }
        })
          .catch((e) => {
            console.error(e)
          })
          .finally(() => {
            this.tableLoading = false
          })
      },
      handleIgnore(ignoreType, batchFlag, id) {
        this.$refs.operateDropdown?.hide()
        this.operateParams.bizType = ignoreType
        this.operateParams.batchFlag = batchFlag
        if (batchFlag) {
          this.operateParams.defectKeySet = this.$refs.fileListTable.selection.map(item => item.entityId)
        } else {
          this.operateParams.defectKeySet = [id]
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
        }
        this.isRowChangeIgnoreType = false
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
            if (this.operateParams.batchFlag) {
              this.init()
            } else {
              if (this.operateParams.bizType !== 'ChangeIgnoreType') {
                this.listData.defectList.content.forEach((item) => {
                  if (item.entityId === data.defectKeySet[0]) {
                    item.status = 4
                  }
                })
                this.listData.defectList.content = this.listData.defectList.content.slice()
                // const index = this.listData.defectList.content
                //   .findIndex(item => item.entityId === this.operateParams.defectKeySet[0])
                // this.listData.defectList.content.splice(index, 1)
              }
              this.init()
            }
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
            this.isAddingIgnore = false
          })
      },
      handleIgnoreCancel() {
        this.operateParams.ignoreReason = ''
        this.operateParams.ignoreReasonType = ''
        this.ignoreReasonDialogVisiable = false
        this.isAddingIgnore = false
      },
      toLogs() {
        this.changeHandlerVisiable = false
        this.$router.push({
          name: 'task-settings-trigger',
        })
      },
      screenScroll() {
        this.$nextTick(() => {
          if (this.$refs.fileListTable.$refs.bodyWrapper) {
            const childrens = this.$refs.fileListTable.$refs.bodyWrapper
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
      openDetail() {
        const { entityId } = this.$route.query
        const { toolName } = this.searchParams
        if (entityId && toolName) {
          setTimeout(() => {
            if (!this.toolMap[toolName]) {
              this.openDetail()
            } else {
              this.isOpenDetail = true
              this.defectDetailSearchParams.entityId = entityId
            }
          }, 1000)
        }
      },
      toggleSearch() {
        this.isSearchDropdown = !this.isSearchDropdown
        window.localStorage.setItem('lintSearchExpend', JSON.stringify(this.isSearchDropdown))
        this.getQueryPreLineNum()
        this.setTableHeight()
      },
      getQueryPreLineNum() {
        let bodyWidth = document.body.offsetWidth
        if (bodyWidth < 1280) bodyWidth -= 10 // 有滚动条要减去滚动条宽度
        const containerW = bodyWidth - 292 // 搜索栏宽度
        const childW = 379 // 单个搜素宽度
        // const containerW = document.getElementsByClassName('search-form')[0].offsetWidth
        // const childW = document.getElementsByClassName('cc-col')[0].offsetWidth
        const average = Math.floor(containerW / childW)
        this.lineAverageOpt = average
      },
      setTableHeight() {
        setTimeout(() => {
          let smallHeight = 0
          let largeHeight = 0
          let tableHeight = 0
          const i = this.listData.defectList.content.length || 0
          if (this.$refs.fileListTable) {
            const $main = document.getElementsByClassName('main-form')
            smallHeight = $main.length > 0 ? $main[0].clientHeight : 0
            largeHeight = this.$refs.mainContainer ? this.$refs.mainContainer.clientHeight : 0
            tableHeight = this.$refs.fileListTable.$el.offsetHeight
          }
          this.screenHeight = i * 42 > tableHeight ? largeHeight - smallHeight - 73 : (i * 42) + 43
          this.screenHeight = this.screenHeight === 43 ? 336 : this.screenHeight
        }, 100)
      },
      setFullScreen() {
        this.isFullScreen = !this.isFullScreen
      },
      getSearchParams() {
        const { daterange } = this.searchParams
        const { isSelectAll } = this
        const checkerSet = this.searchFormData.checkerSetList
          .find(checkerSet => checkerSet.checkerSetId === this.searchParams.checkerSet)
        const params = { ...this.searchParams, isSelectAll, checkerSet }
        const startTime = this.dateType === 'createTime' ? 'startCreateTime' : 'startFixTime'
        const endTime = this.dateType === 'createTime' ? 'endCreateTime' : 'endFixTime'

        params[startTime] = daterange[0] || ''
        params[endTime] = daterange[1] || ''
        params.dimension = this.dimensionStr

        return params
      },
      /**
       * 重置搜索过滤项
       */
      handleSelectAllSearchOption() {
        this.$refs.handleMenu.instance.hide()
        this.customOption = this.getCustomOption(true)
        this.selectedOptionColumn = _.cloneDeep(this.customOption)
        localStorage.setItem(COVERITY_SEARCH_OPTION_CACHE, JSON.stringify(this.selectedOptionColumn))
        this.setTableHeight()
      },
      /**
       * 确认搜索过滤项
       */
      handleConfirmSearchOption() {
        this.$refs.handleMenu.instance.hide()
        this.selectedOptionColumn = _.cloneDeep(this.customOption)
        localStorage.setItem(COVERITY_SEARCH_OPTION_CACHE, JSON.stringify(this.selectedOptionColumn))
        this.setTableHeight()
      },
      downloadExcel() {
        const params = this.getSearchParams()
        params.pageSize = 300000
        if (this.totalCount > 300000) {
          this.$bkMessage({
            message: this.$t('当前问题数已超过30万个，暂时不支持导出excel，请筛选后再尝试导出。'),
          })
          return
        }
        this.exportLoading = true
        this.$store.dispatch('defect/lintList', params).then((res) => {
          const list = res && res.defectList && res.defectList.content
          this.generateExcel(list)
        })
          .finally(() => {
            this.exportLoading = false
          })
      },
      generateExcel(list = []) {
        const tHeader = [this.$t('序号'),
                         this.$t('entityId'),
                         this.$t('ID'),
                         this.$t('文件名称'),
                         this.$t('行号'),
                         this.$t('版本号'),
                         this.$t('路径'),
                         this.$t('规则'),
                         this.$t('规则类型'),
                         this.$t('类型子类'),
                         this.$t('处理人'),
                         this.$t('级别'),
                         this.$t('创建日期'),
                         this.$t('修复日期'),
                         this.$t('忽略日期'),
                         this.$t('最新状态'),
                         this.$t('首次发现')]
        const filterVal = ['index',
                           'entityId',
                           'id',
                           'fileName',
                           'lineNum',
                           'revision',
                           'filePath',
                           'checker',
                           'displayCategory',
                           'displayType',
                           'authorList',
                           'severity',
                           'createTime',
                           'fixedTime',
                           'ignoreTime',
                           'status',
                           'createBuildNumber']
        const data = this.formatJson(filterVal, list)
        // eslint-disable-next-line
        const title = `${this.taskDetail.nameCn}-${this.taskDetail.taskId}-${this.dimension}-${this.$t('问题')}-${new Date().toISOString()}`
        export_json_to_excel(tHeader, data, title)
      },
      // 处理表格数据
      formatJson(filterVal, list) {
        let index = 1
        return list.map(item => filterVal.map((j) => {
          if (j === 'index') {
            return index += 1
          } if (j === 'severity') {
            return this.defectSeverityMap[item.severity]
          } if (j === 'authorList') {
            return item.authorList.toString()
          } if (j === 'createTime' || j === 'fixedTime' || j === 'ignoreTime') {
            return this.formatTime(item[j], 'YYYY-MM-DD HH:mm:ss')
          } if (j === 'createBuildNumber') {
            return `#${item.createBuildNumber}`
          } if (j === 'status') {
            return this.handleStatus(item.status, item.defectIssueInfoVO)
          }
          return item[j]
        }))
      },
      /**
       * 分享问题链接
       */
      shareDefect() {
        const { projectId, taskId } = this.$route.params
        const { toolName, entityId, status } = this.currentFile
        let prefix = `${location.protocol}//${location.host}`
        if (window.self !== window.top) {
          prefix = `${location.protocol}${window.DEVOPS_SITE_URL}/console`
        }
        const url = `${prefix}/codecc/${projectId}/task/${taskId}/defect/compile/${toolName}/list
?entityId=${entityId}&status=${status}`
        const input = document.createElement('input')
        document.body.appendChild(input)
        input.setAttribute('value', url)
        input.select()
        document.execCommand('copy')
        document.body.removeChild(input)
        this.$bkMessage({ theme: 'success', message: '链接已复制到粘贴板' })
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
      clickTrace(trace) {
        const { fileMd5, lineNum, id } = this.currentTrace || {}
        this.currentTraceIndex = this.traceDataList.findIndex(item => item.id === trace.id)
        this.currentTrace = trace
        this.traceActiveId = trace.id
        this.defectInstances.forEach((instance) => {
          instance.traces.forEach((item) => {
            if (item.id === trace.id.split('-').slice(0, 2)
              .join('-')) {
              item.expanded = true
            }
          })
        })
        if (fileMd5 !== trace.fileMd5
          || id.length !== trace.id.length
          || id.split('-').shift() !== trace.id.split('-').shift()) {
          this.updateCodeViewer(this.codeViewerInDialog)
          this.preLineNum = undefined
        } else {
          this.preLineNum = lineNum
        }
        this.scrollTrace()
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
            [defect] = this.listData.defectList.content.splice(index, 1)
          }
          this.listData.defectList.content.unshift(defect)
        }
      },
      handelFetchIgnoreList() {
        this.$store.dispatch('ignore/fetchIgnoreList').then((res) => {
          this.ignoreList = res.data
        })
      },
      handleGuideNextStep() {
        this.$nextTick(() => {
          this.$refs.guidePopover[0].hideHandler()
          this.handleNextGuide()
          localStorage.setItem('guideEnd', true)
          this.guideFlag = true
        })
      },
      handleSetReview() {
        let prefix = `${location.protocol}//${location.host}`
        if (window.self !== window.top) {
          prefix = `${location.protocol}${window.DEVOPS_SITE_URL}/console`
        }
        const route = this.$router.resolve({
          name: 'ignoreList',
        })
        window.open(prefix + route.href, '_blank')
        this.handleGuideNextStep()
      },
      handleTableSetReview() {
        let prefix = `${location.protocol}//${location.host}`
        if (window.self !== window.top) {
          prefix = `${location.protocol}${window.DEVOPS_SITE_URL}/console`
        }
        const route = this.$router.resolve({
          name: 'ignoreList',
        })
        window.open(prefix + route.href, '_blank')
        document.body.click()
      },
      handleTableGuideNextStep() {
        document.body.click()
        localStorage.setItem('guideEnd', true)
      },
      handleNextGuide() {
        if (!this.guideFlag) {
          document.getElementsByClassName('guide-icon')[0] && document.getElementsByClassName('guide-icon')[0].click()
          setTimeout(() => {
            document.getElementsByClassName('guide-flag')[0] && document.getElementsByClassName('guide-flag')[0].click()
          }, 200)
        }
      },
      handleToIgnoreList() {
        if (!this.isAddingIgnore) {
          this.handleSetReview()
          this.isAddingIgnore = true
        } else {
          this.handelFetchIgnoreList()
          this.isAddingIgnore = false
        }
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
      handleChangeIgnoreType(row) {
        this.isSelectAll = 'N'
        this.isRowChangeIgnoreType = true
        this.operateParams.ignoreReason = row.ignoreReason
        this.operateParams.ignoreReasonType = row.ignoreReasonType
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
    },
  }
</script>

<style>
    @import "./codemirror.css";
</style>

<style lang="postcss" scoped>
    @import "../../css/variable.css";
    @import "../../css/mixins.css";
    @import "./defect-list.css";

    .coverity-list {
      padding: 16px 20px 0px 16px;
    }
    .breadcrumb {
      padding: 0px !important;
      .breadcrumb-name {
        background: white;
      }
    }
    .main-container {
      /* padding: 20px 33px 0!important;
        margin: 0 -13px!important; */
      /* border-top: 1px solid #dcdee5; */
      margin: 0px !important;
      background: white;
      .change-handler {
        position: relative;
        top: -32px;
        left: 310px;
      }
      .codecc-icon {
        font-size: 14px;
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
    .file-list-table {
      >>> .list-row {
        cursor: pointer;
        &.grey-row {
          color: #c3cdd7;
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
        height: 370px;
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
    .code-fullscreen {
      display: flex;
      .col-main {
        flex: 1;
        max-width: calc(100% - 585px);
        margin-top: 47px;
      }
      .col-aside {
        flex: none;
        width: 240px;
        background: #f5f7fa;
        padding: 12px 20px;
        margin-top: 47px;
      }
      .file-bar {
        height: 42px;
        .filemeta {
          display: inline-block;
          margin-top: -2px;
          font-size: 12px;
          border-left: 4px solid #3a84ff;
          padding-left: 8px;
          .filename {
            font-size: 16px;
          }
          .filepath {
            width: 700px;
            display: inline-block;
            vertical-align: bottom;
            margin-left: 8px;
            line-height: 24px;

            @mixin ellipsis;
          }
        }
        .times {
          padding-right: 6px;
          line-height: 30px;
        }
        .bk-select.times-select {
          >>>.bk-select-name {
            padding: 0 26px 0 10px;
          }
        }
      }
      .operate-section {
        height: 100%;
      }
      .basic-info {
        max-height: calc(100vh - 240px);
        overflow-y: scroll;
        margin-right: -29px;
        padding-right: 20px;
        &.full-screen-info {
          max-height: calc(100vh - 71px);
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
            padding-bottom: 50px;
          }
          .item {
            display: flex;
            padding: 5px 0;
            dt {
              width: 90px;
              flex: none;
            }
            dd {
              flex: 1;
              color: #313238;
              word-break: break-all;
            }
            a {
              color: #313238;
              word-break: break-all;
            }
            .item-button {
              width: 200px;
            }
          }
        }
      }
    }
    #codeViewerInDialog {
      font-size: 14px;
      width: 100%;
      border: 1px solid #eee;
      border-left: 0;
      border-right: 0;
    }
    .un-full-code-viewer {
      height: calc(100vh - 200px);
    }
    .full-code-viewer {
      height: calc(100vh - 47px);
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
    .handle-menu-tips {
      text-align: center;
      .entry-link {
        padding: 4px 0;
        font-size: 12px;
        cursor: pointer;
        color: $fontWeightColor;
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
      background: url(../../images/mark.svg) no-repeat;
      height: 14px;
      width: 14px;
      margin-bottom: -2px;
    }
    .cc-status {
      width: 60px;
    }
    .cc-mark {
      width: 114px;
      background: white;
      border-radius: 12px;
      padding: 0 8px;
      line-height: 23px;
      height: 23px;
      margin-top: 8px;
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
    .table-append-loading {
      text-align: center;
      padding: 12px 0;
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
    .main-container::-webkit-scrollbar {
      width: 0;
    }
    .toggle-full-icon {
      position: absolute;
      top: -22px;
      right: 10px;
      color: #979ba5;
      cursor: pointer;
      &.icon-un-full-screen {
        top: 8px;
      }
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
    .col-aside-left {
      flex: none;
      width: 336px;
      background: #f5f7fa;
      .col-aside-left-header {
        height: 48px;
        border-bottom: 1px solid #dcdee5;
        .arrows-left {
          position: absolute;
          font-size: 36px!important;
          line-height: 48px;
          cursor: pointer;
          z-index: 9999;
        }
        .header-index {
          display: inline-block;
          text-align: center;
          width: 100%;
          line-height: 48px;
          font-weight: bold;
        }
      }
    }
    .filename {
      position: absolute;
      top: 0;
      padding-left: 24px;
      font-size: 16px;
      line-height: 48px;
    }
    .icon-share, .icon-operate {
      position: absolute;
      top: 0;
      right: 24px;
      font-size: 16px;
      cursor: pointer;
      line-height: 48px;
      &:hover {
        color: #3a84ff;
      }
    }
    .icon-operate {
      right: 60px;
    }
    .dialog-block {
      position: absolute;
      right: 24px;
      width: 272px;
      height: 150px;
      background: #fff;
      box-shadow: 0 4px 12px 0 rgba(0,0,0,0.20);
      border-radius: 2px;
      z-index: 99;
      .share-header {
        font-size: 20px;
        color: #313238;
        line-height: 28px;
        padding: 18px 0 10px 24px;
      }
      .icon-close {
        position: absolute;
        right: 10px;
        top: 10px;
        font-size: 28px;
        cursor: pointer;
      }
      .share-content {
        width: 200px;
        height: 32px;
        background: #fff;
        box-shadow: 0 2px 6px 0 rgba(0,0,0,0.10);
        border-radius: 2px;
        line-height: 32px;
        margin: 16px 36px 0;
        padding-left: 10px;
        cursor: pointer;
        &:hover {
          color: #3a84ff;
        }
      }
    }
    >>>.file-detail-dialog.bk-dialog-wrapper .bk-dialog-body {
      padding: 0;
    }
    ::v-deep .bk-dialog-header {
      padding: 3px 24px 0px !important;
    }
</style>
<style lang="postcss">
    .tippy-tooltip.dot-menu-theme {
      padding: 10px;
      width: 280px !important;
      height: 100px;
      background-color: #3b91fb;
      color: #fff;
      .guide-btn {
        position: absolute;
        right: 18px;
        bottom: 10px;
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
