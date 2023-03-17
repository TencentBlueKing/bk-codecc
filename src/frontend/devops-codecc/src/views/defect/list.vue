<template>
  <div v-bkloading="{ isLoading: !mainContentLoading && contentLoading, opacity: 0.3 }">
    <section class="coverity-list"
             v-if="taskDetail.enableToolList.find(item => item.toolName !== 'CCN' && item.toolName !== 'DUPC') || isFromOverview">
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
              <i class="bk-icon codecc-icon icon-shaixuan" v-bk-tooltips="$t('筛选项设置')"></i>
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
                    <bk-select v-model="dimension" :multiple="isMigration" @selected="handleSelectDimen">
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
                    <bk-select :multiple="isMigration" v-model="searchParams.toolName">
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
                    <bk-button @click="toChangeMember" :title="$t('批量修改问题处理人')" :text="true" class="change-handler">
                      <i class="codecc-icon icon-handler-2"></i>
                    </bk-button>
                  </bk-form-item>
                </div>
                <div class="cc-col" v-show="allRenderColumnMap.daterange">
                  <bk-form-item :label="$t('日期')">
                    <bk-date-picker v-model="searchParams.daterange" type="daterange"></bk-date-picker>
                  </bk-form-item>
                </div>
                <div class="cc-col" v-show="allRenderColumnMap.path">
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
                <div class="cc-col" v-show="allRenderColumnMap.status">
                  <bk-form-item :label="$t('状态')">
                    <bk-select multiple v-model="searchParams.status" clearable searchable :loading="selectLoading.statusLoading">
                      <bk-option
                        v-for="(value, key) in statusTypeMap"
                        :key="Number(key)"
                        :id="Number(key)"
                        :disabled="searchParams.clusterType === 'file' && Number(key) !== 1"
                        :name="value">
                        <span v-bk-tooltips="searchParams.clusterType === 'file' && Number(key) !== 1 ? '仅支持按问题聚类方式查看' : ''">{{value}}</span>
                      </bk-option>
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
                <div class="cc-col" v-show="allRenderColumnMap.defectType">
                  <bk-form-item :label="$t('时期')">
                    <bk-checkbox-group v-model="searchParams.defectType" class="checkbox-group">
                      <bk-checkbox
                        v-for="(value, key, index) in defectTypeMap"
                        :value="Number(key)"
                        :key="index">
                        {{value}}({{getDefectCountByType(key)}})
                      </bk-checkbox>
                      <bk-popover placement="top" width="220" class="popover">
                        <i class="codecc-icon icon-tips"></i>
                        <div slot="content">
                          {{typeTips}}
                          <a href="javascript:;" @click="toLogs">{{$t('前往设置')}}>></a>
                        </div>
                      </bk-popover>
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
                <bk-button size="small" ext-cls="cc-operate-button" @click="handleCommit('commit', true)" theme="primary">{{$t('提单')}}</bk-button>
                <bk-button size="small" ext-cls="cc-operate-button" @click="handleIgnore('RevertIgnore', true)" v-if="!searchParams.status.length || searchParams.status.includes(4)" theme="primary">
                  {{$t('恢复忽略')}}
                </bk-button>
                <bk-dropdown-menu ext-cls="cc-operate-button" v-if="searchParams.clusterType === 'defect'" @show="isDropdownShow = true" @hide="isDropdownShow = false">
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
            <table-file
              v-if="searchParams.clusterType === 'file'"
              v-show="isFetched"
              ref="table"
              :list="defectList"
              :prohibit-ignore="taskDetail.prohibitIgnore"
              :screen-height="screenHeight"
              :file-index="fileIndex"
              :handle-mark="handleMark"
              :handle-ignore="handleIgnore"
              :handle-commit="handleCommit"
              :handle-sort-change="handleSortChange"
              :handle-selection-change="handleSelectionChange"
              :to-select-all="toSelectAll"
              :handle-file-list-row-click="handleFileListRowClick">
            </table-file>
            <table-defect
              v-else
              v-show="isFetched"
              ref="table"
              :list="defectList"
              :prohibit-ignore="taskDetail.prohibitIgnore"
              :screen-height="screenHeight"
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
              :next-page-end-num="nextPageEndNum">
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
              <bk-member-selector :max-data="1" v-model="operateParams.targetAuthor" style="width: 290px;"></bk-member-selector>
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
        theme="primary"
        :mask-close="false"
        header-position="left"
        :title="operateParams.batchFlag ? $t('选择问题忽略原因，共x个问题', { num: selectedDefectCount || (isSelectAll === 'Y' ? totalCount : selectedLen) }) : $t('选择问题忽略原因')">
        <div class="pd10 pr50">
          <bk-form :model="operateParams" :label-width="30" class="search-form">
            <bk-form-item property="ignoreReason">
              <bk-radio-group v-model="operateParams.ignoreReasonType">
                <bk-radio class="cc-radio" :value="1">{{$t('检查工具误报')}}</bk-radio>
                <bk-radio class="cc-radio" :value="2">{{$t('设计如此')}}</bk-radio>
                <bk-radio class="cc-radio" :value="4">{{$t('其他')}}</bk-radio>
              </bk-radio-group>
            </bk-form-item>
            <bk-form-item property="ignoreReason" :required="ignoreReasonRequired">
              <bk-input :type="'textarea'" :maxlength="255" v-model="operateParams.ignoreReason"></bk-input>
            </bk-form-item>
          </bk-form>
        </div>
        <div slot="footer">
          <bk-button
            theme="primary"
            :disabled="ignoreReasonAble"
            @click.native="handleIgnoreConfirm">
            {{operateParams.batchFlag ? $t('批量忽略') : $t('确定')}}
          </bk-button>
          <bk-button
            theme="primary"
            @click.native="ignoreReasonDialogVisiable = false">
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
          {{this.$t('不再提示')}}
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
    <div class="coverity-list" v-else>
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
  import { format } from 'date-fns'
  import { mapGetters, mapState } from 'vuex'
  import { getClosest, toggleClass } from '@/common/util'
  // eslint-disable-next-line
  import { export_json_to_excel } from 'vendor/export2Excel'
  import util from '@/mixins/defect-list'
  import defectCache from '@/mixins/defect-cache'
  import detail from './detail'
  import tableFile from './table-file'
  import tableDefect from './table-defect'
  import Empty from '@/components/empty'
  import Record from '@/components/operate-record/index'
  import filterSearchOption from './filter-search-option'
  import OperateDialog from '@/components/operate-dialog'
  // import CodeMirror from '@/common/codemirror'

  // 搜索过滤项缓存
  const SEARCH_OPTION_CACHE = 'search_option_columns_defect'

  export default {
    components: {
      Record,
      Empty,
      tableFile,
      tableDefect,
      detail,
      filterSearchOption,
      OperateDialog,
    },
    mixins: [util, defectCache],
    data() {
      this.getDefaultOption = () => ([
        { id: 'dimension', name: this.$t('维度'), isChecked: true },
        { id: 'toolName', name: this.$t('工具'), isChecked: true },
        { id: 'checkerSet', name: this.$t('规则集'), isChecked: true },
        { id: 'buildId', name: this.$t('快照'), isChecked: true },
      ])

      this.getCustomOption = function (val) {
        return [
          { id: 'checker', name: this.$t('规则'), isChecked: val },
          { id: 'author', name: this.$t('处理人'), isChecked: val },
          { id: 'daterange', name: this.$t('日期'), isChecked: val },
          { id: 'path', name: this.$t('路径'), isChecked: val },
          { id: 'status', name: this.$t('状态'), isChecked: val },
          { id: 'severity', name: this.$t('级别'), isChecked: val },
          { id: 'defectType', name: this.$t('时期'), isChecked: val },
        ]
      }

      const { query } = this.$route
      const { toolId } = this.$route.params
      let status = [1]
      if (query.status) {
        status = this.numToArray(query.status)
      }
      const isMigration = !!this.$store.state.task.detail.dataMigrationSuccessful

      return {
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
        defectTypeMap: {
          1: this.$t('新问题'),
          2: this.$t('存量问题'),
        },
        dimension: query.dimension && isMigration ? query.dimension.split(',') : query.dimension,
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
          toolName: toolId && isMigration ? toolId.split(',') : toolId,
          dimension: query.dimension || '',
          checker: query.checker || '',
          checkerSet: query.checkerSet || '',
          author: query.author,
          severity: this.numToArray(query.severity),
          defectType: this.numToArray(query.defectType, [1, 2]),
          status,
          buildId: query.buildId ? query.buildId : '',
          fileList: this.handleFileList(query.fileList),
          daterange: [query.startTime, query.endTime],
          clusterType: query.clusterType || 'defect',
          sortField: query.sortField || 'fileName',
          sortType: 'ASC',
          pageNum: 1,
          pageSize: 100,
          dataMigrationSuccessful: isMigration,
        },
        defectDetailSearchParams: {
          sortField: '',
          sortType: '',
          pattern: '',
          filePath: '',
          toolName: toolId || '',
          entityId: undefined,
          dataMigrationSuccessful: isMigration,
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
          dataMigrationSuccessful: isMigration,
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
        lineAverageOpt: 10,
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
      typeTips() {
        return this.$t('起始时间x之后产生的问题为新问题', { accessTime: this.newDefectJudgeTime })
      },
      // breadcrumb() {
      //   const { toolId } = this
      //   let toolDisplayName = (this.toolMap[toolId] || {}).displayName || ''
      //   const names = [this.$route.meta.title || this.$t('问题管理')]
      //   if (toolDisplayName) {
      //     toolDisplayName = this.$t(`${toolDisplayName}`)
      //     names.unshift(toolDisplayName)
      //   }

      //   return { name: names.join(' / ') }
      // },
      isMigration() {
        return !!this.$store.state.task.detail.dataMigrationSuccessful
      },
      dimensionStr() {
        const { dimension = '' } = this
        return typeof dimension === 'string' ? dimension : dimension.join(',')
      },
      toolNameStr() {
        const { toolName = '' } = this.searchParams
        return typeof toolName === 'string' ? toolName : toolName.join(',')
      },
      defectList() {
        return this.listData.defectList.records
      },
      currentFile() {
        return this.lintDetail.lintDefectDetailVO
      },
      statusTypeMap() {
        const { existCount, fixCount, ignoreCount, maskCount } = this.searchFormData
        return {
          1: `${this.$t('待修复')} (${existCount || 0})`,
          2: `${this.$t('已修复')} (${fixCount || 0})`,
          4: `${this.$t('已忽略')} (${ignoreCount || 0})`,
          8: `${this.$t('已屏蔽')} (${maskCount || 0})`,
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
          this.defectDetailDialogVisiable = true
          this.emptyText = this.$t('未选择文件')
          this.detailLoading = true
          if (this.defectCache[cacheId]) {
            this.lintDetail = this.defectCache[cacheId]
            this.handleCodeFullScreen()
          }
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
      taskDetail: {
        handler(newVal) {
          if (newVal.enableToolList.find(item => item.toolName !== 'CCN' && item.toolName !== 'DUPC')) {
            this.$nextTick(() => {
              this.getQueryPreLineNum()
            })
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
      statusRelevantParams(val, oldVal) {
        this.fetchStatusParams()
      },
      severityRelevantParams(val, oldVal) {
        this.fetchSeverityParams()
        this.fetchDefectTypeParams()
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
        if (this.isMigration) {
          this.searchParams.dimension = val
          this.fetchCheckerSetParams()
          this.initParams()
          this.fetchListTool()
        }
      },
    },
    created() {
      if (!this.taskDetail.nameEn || this.taskDetail.enableToolList
        .find(item => item.toolName !== 'CCN' && item.toolName !== 'DUPC')) {
        this.init(true)
        this.fetchBuildList()
        this.fetchOtherParams()
      }
      if (this.isFromOverview) this.isFetched = true

      // 读取缓存搜索过滤项
      const columnsCache = JSON.parse(localStorage.getItem(SEARCH_OPTION_CACHE))
      if (columnsCache) {
        this.selectedOptionColumn = _.cloneDeep(columnsCache)
        this.customOption = columnsCache
      } else {
        this.selectedOptionColumn = this.getCustomOption(true)
      }
    },
    mounted() {
      const memberNeverShow = JSON.parse(window.localStorage.getItem('memberNeverShow'))
      memberNeverShow === null
        ? this.memberNeverShow = false
        : this.memberNeverShow = memberNeverShow
      window.addEventListener('resize', this.getQueryPreLineNum)
      this.openDetail()
      this.keyOperate()
    },
    beforeDestroy() {
      window.removeEventListener('resize', this.getQueryPreLineNum)
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
        this.$refs.handleMenu.instance.hide()
        this.customOption = this.getCustomOption(true)
        this.selectedOptionColumn = _.cloneDeep(this.customOption)
        localStorage.setItem(SEARCH_OPTION_CACHE, JSON.stringify(this.selectedOptionColumn))
        this.setTableHeight()
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
        const tHeader = clusterType === 'defect'
          ? [
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
          ]
          : [
            this.$t('序号'),
            this.$t('文件名称'),
            this.$t('问题数'),
            this.$t('规则数'),
            this.$t('级别'),
            this.$t('处理人'),
            this.$t('所属路径'),
            this.$t('提交日期'),
          ]
        const filterVal = clusterType === 'defect'
          ? [
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
          ]
          : [
            'index',
            'fileName',
            'defectCount',
            'checkerList',
            'severityList',
            'authorList',
            'filePath',
            'fileUpdateTime',
          ]
        const data = this.formatJson(filterVal, list, clusterType)
        // eslint-disable-next-line
        const title = `${this.taskDetail.nameCn}-${this.taskDetail.taskId}-${this.dimensionStr}-${this.$t('问题')}-${new Date().toISOString()}`
        export_json_to_excel(tHeader, data, title)
      },
      // 处理状态
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
            return this.handleStatus(item.status, item.defectIssueInfoVO)
          } if (j === 'checkerList') {
            return item[j] && item[j].length
          } if (j === 'authorList') {
            return item[j] && item[j].join(';')
          } if (j === 'severityList') {
            return item[j] && item[j].map(i => this.defectSeverityMap[i]).join('、')
          } if (j === 'ignoreReasonType') {
            if (item[j] === 1) {
              return '工具误报'
            } if (item[j] === 2) {
              return '设计如此'
            } if (item[j] === 4) {
              return '其他'
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
        this.getQueryPreLineNum()
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
        this.fetchDefectTypeParams()
        this.fetchStatusParams()
        this.fetchOtherParams()
      },
      async fetchBuildList() {
        this.selectLoading.buildListLoading = true
        this.buildList = await this.$store.dispatch('defect/getBuildList', { taskId: this.$route.params.taskId })
        this.selectLoading.buildListLoading = false
      },
      getSearchParams() {
        const { daterange } = this.searchParams
        const startCreateTime = this.formatTime(daterange[0], 'YYYY-MM-DD')
        const endCreateTime = this.formatTime(daterange[1], 'YYYY-MM-DD')
        const dimension = this.dimensionStr
        const toolName = this.toolNameStr
        const { isSelectAll } = this
        const checkerSet = this.searchFormData.checkerSetList
          .find(checkerSet => checkerSet.checkerSetId === this.searchParams.checkerSet)
        const params = {
          ...this.searchParams,
          dimension,
          toolName,
          startCreateTime,
          endCreateTime,
          isSelectAll,
          checkerSet,
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
        this.getDefectCount(res)
      },
      async fetchCheckerSetParams() {
        const { dimensionStr, toolNameStr, isMigration, searchParams: { buildId } } = this
        const params = {
          taskId: this.$route.params.taskId,
          toolName: toolNameStr,
          dimension: dimensionStr,
          buildId,
          dataMigrationSuccessful: isMigration,
        }
        const res = await this.$store.dispatch('checkerset/listForDefect', params)
        const checkerSetList = res.filter(checkerSet => checkerSet.taskUsing)
        this.searchFormData = Object.assign(this.searchFormData, { checkerSetList })
      },
      async fetchDefectTypeParams() {
        const params = this.getSearchParams()
        params.statisticType = 'DEFECT_TYPE'
        const res = await this.$store.dispatch('defect/lintSearchParams', params)
        const { newCount, historyCount } = res
        this.searchFormData = Object.assign(this.searchFormData, { newCount, historyCount })
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
        const params = {
          dimension: this.dimensionStr,
          toolName: this.toolNameStr,
          status,
          checkerSet,
          buildId,
          dataMigrationSuccessful: this.isMigration,
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
        const { taskId } = this.$route.params
        const { buildId } = this.searchParams
        const { dimensionStr, toolNameStr, isMigration } = this
        this.gatherFile = await this.$store.dispatch(
          'defect/gatherFile',
          { taskId, toolName: toolNameStr, dimension: dimensionStr, buildId, dataMigrationSuccessful: isMigration },
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
              if (!vm.defectDetailDialogVisiable && !vm.authorEditDialogVisiable && e.path.length < 5) vm.keyEnter()
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
        let defectKeySet = []
        if (batchFlag) {
          this.$refs.table.$refs.fileListTable.selection.forEach((item) => {
            defectKeySet.push(item.entityId)
          })
        } else {
          defectKeySet = [entityId]
        }
        const bizType = 'MarkDefect'
        const dimension = this.dimensionStr
        let data = { ...this.operateParams, bizType, defectKeySet, markFlag, dimension }
        if (this.isSelectAll === 'Y') {
          data = { ...data, isSelectAll: 'Y', queryDefectCondition: JSON.stringify(this.getSearchParams()) }
        }
        this.tableLoading = true
        this.$store.dispatch('defect/batchEdit', data).then((res) => {
          if (res.code === '0') {
            this.$bkMessage({
              theme: 'success',
              message: markFlag
                ? this.$t('标记为已处理成功。若下次检查仍为问题将突出显示。') : this.$t('取消标记成功'),
            })
            if (batchFlag) {
              this.fetchList()
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
        this.$store.dispatch('defect/batchEdit', data).then((res) => {
          if (res.code === '0') {
            this.$bkMessage({
              theme: 'success',
              message: this.operateParams.bizType === 'IgnoreDefect'
                ? this.$t('忽略问题成功。该问题将不会在待修复列表中显示。') : this.$t('恢复问题成功。该问题将重新在待修复列表中显示。'),
            })
            if (data.batchFlag) {
              this.fetchList()
            } else if (!this.searchParams.status.includes(4)) { // 状态不包含已忽略，列表去掉已忽略
              const index = this.listData.defectList.records.findIndex(item => item.entityId === data.defectKeySet[0])
              this.listData.defectList.records.splice(index, 1)
              this.totalCount -= 1
            }
            this.initParams()

            this.operateParams.ignoreReason = ''
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
      // 根据ID打开详情
      openDetail() {
        const { entityId } = this.$route.query
        const { filePath } = this.$route.query
        if (entityId || filePath) {
          setTimeout(() => {
            const toolName = this.toolNameStr || ''
            if (!toolName || toolName.includes(',')) return
            if (!this.toolMap[toolName]) {
              this.openDetail()
            } else if (filePath) {
              this.searchParams.clusterType = 'file'
              this.defectDetailSearchParams.clusterType = 'file'
              this.defectDetailSearchParams.filePath = filePath
            } else {
              this.isOpenDetail = true
              this.defectDetailSearchParams.entityId = entityId
            }
          }, 1000)
        }
      },
      setTableHeight() {
        setTimeout(() => {
          let smallHeight = 0
          let largeHeight = 0
          let tableHeight = 0
          const i = this.listData.defectList.records.length || 0
          if (this.$refs.table && this.$refs.table.$refs.fileListTable) {
            const $main = document.getElementsByClassName('main-form')
            smallHeight = $main.length > 0 ? $main[0].clientHeight : 0
            largeHeight = this.$refs.mainContainer ? this.$refs.mainContainer.clientHeight : 0
            tableHeight = this.$refs.table.$refs.fileListTable.$el.scrollHeight
            this.screenHeight = i * 42 > tableHeight ? largeHeight - smallHeight - 73 : (i * 42) + 43
            this.screenHeight = this.screenHeight === 43 ? 336 : this.screenHeight
          }
        }, 0)
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
        line-height: 30px;
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
</style>
<style lang="postcss">
    .file-detail-dialog {
      .bk-dialog {
        min-width: 1010px;
      }
    }
</style>
