<template>
  <div v-bkloading="{ isLoading: !mainContentLoading && contentLoading, opacity: 0.3 }">
    <div class="dupc-list" v-if="taskDetail.enableToolList.find(item => item.toolName === 'DUPC')">
      <div class="breadcrumb">
        <div class="breadcrumb-name">
          <bk-tab :active.sync="active" :label-height="42" @tab-change="handleTableChange" type="unborder-card">
            <bk-tab-panel
              v-for="(panel, index) in panels"
              v-bind="panel"
              :key="index">
            </bk-tab-panel>
          </bk-tab>
          <div class="tab-extra-icon is-ccn">
            <!-- <span :class="{ 'filter-search-icon': true, 'mac-filter-search-icon': isMac }">
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
            </span> -->
            <span class="excel-icon pl20">
              <bk-button style="border: 0" v-if="exportLoading" icon="loading" :disabled="true" :title="$t('导出Excel')"></bk-button>
              <span v-else class="codecc-icon icon-export-excel excel-download" @click="downloadExcel" v-bk-tooltips="$t('导出Excel')"></span>
            </span>
          </div>
        </div>
      </div>
      <div class="main-container">
        <div class="main-content-inner main-content-list">
          <bk-form :label-width="60" class="search-form">
            <container class="cc-container">
              <div class="cc-col">
                <bk-form-item :label="$t('处理人')">
                  <bk-select v-model="searchParams.author" searchable>
                    <bk-option
                      v-for="(author, index) in searchFormData.authorList"
                      :key="index"
                      :id="author"
                      :name="author"
                    >
                    </bk-option>
                  </bk-select>
                </bk-form-item>
              </div>
              <div class="cc-col">
                <bk-form-item :label="$t('文件路径')" class="fixed-width">
                  <bk-dropdown-menu @show="isFilePathDropdownShow = true" @hide="isFilePathDropdownShow = false" :align="isSmallScreen ? 'left' : 'right'" trigger="click" ref="filePathDropdown">
                    <bk-button type="primary" slot="dropdown-trigger">
                      <div style="font-size: 12px" class="filepath-name" :class="{ 'unselect': !searchFormData.filePathShow }" :title="searchFormData.filePathShow">{{searchFormData.filePathShow ? searchFormData.filePathShow : $t('请选择')}}</div>
                      <i :class="['bk-icon icon-angle-down', { 'icon-flip': isFilePathDropdownShow }]"></i>
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
                            {{ $t('搜索某类文件如P2PLive下') }}
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
              <div class="cc-col" v-show="allRenderColumnMap.severity">
                <bk-form-item :label="$t('风险级别')">
                  <bk-checkbox-group v-model="searchParams.severity" class="checkbox-group">
                    <bk-checkbox
                      v-for="(name, value, index) in defectSeverityMap"
                      :value="Number(value)"
                      :key="index"
                    >
                      {{name}}(<em :class="['count', `count-${['major', 'minor', 'info'][index]}`]">{{getDefectCountBySeverity(value)}}</em>)
                    </bk-checkbox>
                    <bk-popover placement="top" width="220" class="popover">
                      <i class="codecc-icon icon-tips"></i>
                      <div slot="content">
                        <p>{{$t('极高风险(>=20%)')}}</p>
                        <p>{{$t('高风险11%20%')}}</p>
                        <p>{{$t('中风险5%11%')}}</p>
                        <p>{{$t('低风险0%5%')}}</p>
                        <p>{{$t('低风险文件不在列表中显示')}}</p>
                      </div>
                    </bk-popover>
                  </bk-checkbox-group>
                </bk-form-item>
              </div>
            </container>
          </bk-form>

          <bk-table
            v-show="isFetched"
            class="file-list-table"
            ref="fileListTable"
            v-bkloading="{ isLoading: tableLoading, opacity: 0.6 }"
            :data="defectList"
            :pagination="pagination"
            :row-class-name="handleRowClassName"
            @page-change="handlePageChange"
            @page-limit-change="handlePageLimitChange"
            @row-click="handleListRowClick"
            @sort-change="handleSortChange"
          >
            <bk-table-column type="index" :label="$t('序号')" align="center" width="70"></bk-table-column>
            <bk-table-column :label="$t('文件名')" prop="fileName">
              <template slot-scope="props">
                <span v-bk-tooltips="{ content: props.row.filePath, delay: 600 }">{{props.row.fileName}}</span>
              </template>
            </bk-table-column>
            <bk-table-column :label="$t('重复块数')" prop="blockNum"></bk-table-column>
            <bk-table-column :label="$t('重复行数')" prop="dupLines" sortable="custom" label-class-name="col-sort-label" class-name="col-sort"></bk-table-column>
            <bk-table-column :label="$t('函数总行数')" prop="totalLines"></bk-table-column>
            <bk-table-column :label="$t('重复率')" prop="dupRate" sortable="custom">
              <template slot-scope="props">
                <span>{{props.row.dupRate}}</span>
              </template>
            </bk-table-column>
            <bk-table-column :label="$t('相关作者')" prop="authorList">
              <template slot-scope="props">
                <span v-bk-tooltips="props.row.authorList">{{props.row.authorList}}</span>
              </template>
            </bk-table-column>
            <bk-table-column :label="$t('风险')" prop="riskFactor" :render-header="renderHeader">
              <template slot-scope="props">
                <span :class="`color-${{ 1: 'major', 2: 'minor', 4: 'info' }[props.row.riskFactor]}`">{{defectSeverityMap[props.row.riskFactor]}}</span>
              </template>
            </bk-table-column>
            <bk-table-column :label="$t('提交时间')" prop="fileChangeTime" sortable="custom">
              <template slot-scope="props">
                <span>{{formatTime(props.row.fileChangeTime, 'YYYY-MM-DD')}}</span>
              </template>
            </bk-table-column>
            <div slot="empty">
              <div class="codecc-table-empty-text">
                <img src="../../images/empty.png" class="empty-img">
                <div>{{$t('暂无数据')}}</div>
              </div>
            </div>
          </bk-table>
        </div>
      </div>
    </div>
    <div class="dupc-list" v-else>
      <div class="main-container large boder-none">
        <div class="no-task">
          <empty title="" :desc="$t('CodeCC集成了重复率工具，可以发现冗余和重复代码，以便代码抽象和重构')">
            <template v-slot:action>
              <bk-button size="large" theme="primary" @click="addTool({ from: 'DUPC' })">{{$t('配置规则集')}}</bk-button>
            </template>
          </empty>
        </div>
      </div>
    </div>
    <bk-dialog
      v-model="detailVisiable"
      :fullscreen="true"
      :show-mask="false"
      :show-footer="false">
      <dupc-detail
        :entity-id="entityId"
        :file-path="filePath"
      ></dupc-detail>
    </bk-dialog>
  </div>
</template>

<script>
  import _ from 'lodash'
  import { mapState } from 'vuex'
  import util from '@/mixins/defect-list'
  import dupcDetail from './dupc-detail'
  import Empty from '@/components/empty'
  // import filterSearchOption from './filter-search-option'
  // eslint-disable-next-line
  import { export_json_to_excel } from 'vendor/export2Excel'

  // 搜索过滤项缓存
  const DUPC_SEARCH_OPTION_CACHE = 'dupc_search_option_columns'

  export default {
    components: {
      dupcDetail,
      Empty,
      // filterSearchOption,
    },
    mixins: [util],
    data() {
      this.getDefaultOption = () => ([
        { id: 'author', name: this.$t('处理人'), isChecked: true },
        { id: 'filePath', name: this.$t('文件路径'), isChecked: true },
      ])

      this.getCustomOption = function (val) {
        return [
          { id: 'severity', name: this.$t('风险级别'), isChecked: val },
        ]
      }
      const { query } = this.$route

      return {
        contentLoading: false,
        panels: [
          { name: 'defect', label: this.$t('重复文件') },
          { name: 'report', label: this.$t('数据报表') },
        ],
        defectSeverityMap: {
          1: this.$t('极高'),
          2: this.$t('高'),
          4: this.$t('中'),
        },
        defectTypeMap: {
          1: this.$t('新文件'),
          2: this.$t('存量文件'),
        },
        newDefectJudgeTime: '',
        searchParams: {
          taskId: this.$route.params.taskId,
          toolName: 'DUPC',
          taskIdList: [this.$route.params.taskId],
          toolNameList: ['DUPC'],
          checker: query.checker || '',
          author: query.author,
          severity: this.numToArray(query.severity),
          defectType: this.numToArray(query.defectType, [1, 2]),
          fileList: this.handleFileList(query.fileList),
          sortField: query.sortField || 'dupLines',
          sortType: 'DESC',
          pageNum: 1,
          pageSize: 50,
        },
        searchFormData: {
          checkerList: [],
          authorList: [],
          filePathTree: {},
          filePathShow: this.handleFileList(query.fileList).join(';'),
        },
        pagination: {
          current: 1,
          count: 1,
          limit: 50,
        },
        listData: {
          defectList: {
            content: [],
            totalElements: 0,
          },
          superHighCount: 0,
          highCount: 0,
          mediumCount: 0,
          newCount: 0,
          historyCount: 0,
        },
        isFilePathDropdownShow: false,
        toolId: 'DUPC',
        editor: null,
        show: false,
        isSmallScreen: document.body.clientWidth < 1360,
        searchInput: '',
        detailVisiable: false,
        entityId: '',
        filePath: '',
        fileIndex: 0,
        tableLoading: false,
        isFetched: false,
        exportLoading: false,
        defaultOption: this.getDefaultOption(),
        customOption: this.getCustomOption(true),
        selectedOptionColumn: [],
        toolName: 'DUPC',
      }
    },
    computed: {
      ...mapState([
        'toolMeta',
      ]),
      ...mapState('tool', {
        toolMap: 'mapList',
      }),
      ...mapState('task', {
        taskDetail: 'detail',
      }),
      ...mapState('defect', [
        'detail',
      ]),
      projectId() {
        return this.$route.params.projectId
      },
      taskId() {
        return this.$route.params.taskId
      },
      breadcrumb() {
        const { toolId } = this
        let toolDisplayName = (this.toolMap[toolId] || {}).displayName || ''
        const names = [this.$route.meta.title || this.$t('重复文件')]
        if (toolDisplayName) {
          toolDisplayName = this.$t(`${toolDisplayName}`)
          names.unshift(toolDisplayName)
        }

        return { name: names.join(' / ') }
      },
      defectList() {
        return this.listData.defectList.content
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
      searchParams: {
        handler() {
          this.tableLoading = true
          this.fetchLintList().then((list) => {
            this.fileIndex = 0
            this.listData = { ...this.listData, ...list }
            this.pagination.count = this.listData.defectList.totalElements
          })
            .finally(() => {
              this.tableLoading = false
            })
        },
        deep: true,
      },
      searchInput: {
        handler() {
          if (this.searchFormData.filePathTree.children) {
            if (this.searchInput) {
              // this.searchFormData.filePathTree.expanded = true
              this.openTree(this.searchFormData.filePathTree)
            } else {
              this.searchFormData.filePathTree.expanded = false
            }
          }
        },
        deep: true,
      },
    },
    created() {
      if (!this.taskDetail.nameEn || this.taskDetail.enableToolList.find(item => item.toolName === 'DUPC')) {
        this.init()
      }

      // 读取缓存搜索过滤项
      const columnsCache = JSON.parse(localStorage.getItem(DUPC_SEARCH_OPTION_CACHE))
      if (columnsCache) {
        this.selectedOptionColumn = _.cloneDeep(columnsCache)
        this.customOption = columnsCache
      } else {
        this.selectedOptionColumn = this.getCustomOption(true)
      }
    },
    mounted() {
      window.addEventListener('resize', () => {
        this.isSmallScreen = document.body.clientWidth < 1360
      })
      this.openDetail()
      this.keyOperate()
    },
    beforeDestroy() {
      document.onkeydown = null
    },
    methods: {
      async init() {
        this.contentLoading = true
        await Promise.all([
          this.fetchLintList(),
          this.fetchLintParams(),
        ]).then(([list, params]) => {
          this.isFetched = true
          this.listData = list
          this.formatFilePath(params.filePathTree)
          this.newDefectJudgeTime = list.newDefectJudgeTime
            ? this.formatTime(list.newDefectJudgeTime, 'YYYY-MM-DD') : ''

          // 把当前登录用户插进去
          function addCurrentUser(authorList = [], user = '') {
            const newAuthorList = authorList.filter(item => item !== user)
            newAuthorList.unshift(user)
            return newAuthorList
          }
          params.authorList = addCurrentUser(params.authorList, this.user.username)
          this.searchFormData = Object.assign({}, this.searchFormData, params)
          this.pagination.count = this.listData.defectList.totalElements
        })
          .finally(() => {
            this.contentLoading = false
          })
      },
      // 给文件路径树加上icon
      formatFilePath(filepath) {
        if (filepath.children && filepath.children.length) {
          filepath.openedIcon = 'icon-folder-open'
          filepath.closedIcon = 'icon-folder'
          filepath.children.forEach(this.formatFilePath)
        } else {
          filepath.icon = 'icon-file'
        }
      },
      // 获取问题列表
      fetchLintList() {
        return this.$store.dispatch('defect/lintList', this.searchParams)
      },
      // 获取问题筛选参数
      fetchLintParams() {
        const { params } = this.$route
        params.toolId = 'DUPC'
        return this.$store.dispatch('defect/lintParams', params)
      },
      handleListRowClick(row, event, column) {
        this.fileIndex = this.defectList.findIndex(file => file.entityId === row.entityId)
        const { entityId, filePath } = row
        this.entityId = entityId
        this.filePath = filePath
        this.detailVisiable = true
        // const query = { entityId, filePath }
        // const resolved = this.$router.resolve({
        //     name: 'defect-dupc-detail',
        //     params: this.$route.params,
        //     query
        // })
        // const href = `${window.DEVOPS_SITE_URL}/console${resolved.href}`
        // window.open(href, '_blank')
      },
      renderHeader(h, data) {
        const extreHigh = this.$t('极高风险(>=20%)')
        const high = this.$t('高风险11%20%')
        const medium = this.$t('中风险5%11%')
        const low = this.$t('低风险0%5%')
        const tips = this.$t('低风险文件不在列表中显示')
        const directive = {
          name: 'bkTooltips',
          content: `<p>${extreHigh}</p><p>${high}</p><p>${medium}</p><p>${low}</p><p>${tips}</p>`,
          placement: 'right',
        }
        return <span class="custom-header-cell" v-bk-tooltips={ directive }>{ data.column.label }</span>
      },
      // 问题列表排序
      handleSortChange({ column, prop, order }) {
        const orders = { ascending: 'ASC', descending: 'DESC' }
        this.searchParams = { ...this.searchParams, ...{ pageNum: 1, sortField: prop, sortType: orders[order] } }
      },
      handlePageChange(page) {
        this.pagination.current = page
        this.searchParams = { ...this.searchParams, ...{ pageNum: page } }
      },
      handlePageLimitChange(currentLimit) {
        this.pagination.current = 1 // 切换分页大小时要回到第一页
        this.searchParams = { ...this.searchParams, ...{ pageNum: 1, pageSize: currentLimit } }
      },
      getDefectCountBySeverity(severity) {
        const severityFieldMap = {
          1: 'superHighCount',
          2: 'highCount',
          4: 'mediumCount',
        }
        const count = this.listData[severityFieldMap[severity]]
        return count > 100000 ? this.$t('10万+') : count
      },
      handleFilePathCancelClick() {
        const { filePathDropdown } = this.$refs
        filePathDropdown.hide()
      },
      openSlider() {
        this.show = true
      },
      numToArray(num, arr = [1, 2, 4]) {
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
      keyOperate() {
        const vm = this
        document.onkeydown = keyDown
        function keyDown(event) {
          const e = event || window.event
          if (e.target.nodeName !== 'BODY') return
          switch (e.code) {
            case 'Enter': // enter
              // e.path.length < 5 防止规则等搜索条件里面的回车触发打开详情
              if (!vm.detailVisiable && e.path.length < 5) vm.keyEnter()
              break
            case 'Escape': // esc
              vm.detailVisiable = false
              break
            case 'ArrowLeft': // left
              if (vm.fileIndex > 0) {
                if (vm.detailVisiable) {
                  vm.handleListRowClick(vm.defectList[vm.fileIndex -= 1])
                } else {
                  vm.fileIndex -= 1
                }
                vm.screenScroll()
              }
              break
            case 'ArrowUp': // up
              if (vm.fileIndex > 0) {
                if (vm.detailVisiable) {
                  vm.handleListRowClick(vm.defectList[vm.fileIndex -= 1])
                } else {
                  vm.fileIndex -= 1
                }
                vm.screenScroll()
              }
              break
            case 'ArrowRight': // right
              if (vm.fileIndex < vm.defectList.length - 1) {
                if (vm.detailVisiable) {
                  vm.handleListRowClick(vm.defectList[vm.fileIndex += 1])
                } else {
                  vm.fileIndex += 1
                }
                vm.screenScroll()
              }
              break
            case 'ArrowDown': // down
              if (vm.fileIndex < vm.defectList.length - 1) {
                if (vm.detailVisiable) {
                  vm.handleListRowClick(vm.defectList[vm.fileIndex += 1])
                } else {
                  vm.fileIndex += 1
                }
                vm.screenScroll()
              }
              break
          }
        }
      },
      keyEnter() {
        const row = this.defectList[this.fileIndex]
        this.entityId = row.entityId
        this.filePath = row.filePath
        this.detailVisiable = true
      },
      screenScroll() {
        const [container] = document.getElementsByClassName('main-container')
        if (container) {
          const height = this.fileIndex > 3 ? (this.fileIndex - 3) * 42 : 0
          container.scrollTo({
            top: height,
          })
        }
      },
      handleRowClassName({ row, rowIndex }) {
        return this.fileIndex === rowIndex ? 'list-row current-row' : 'list-row'
      },
      openDetail() {
        const id = this.$route.query.entityId
        if (id) {
          setTimeout(() => {
            if (!this.toolMap[this.toolId]) {
              this.openDetail()
            } else {
              this.detailVisiable = true
              this.entityId = id
              this.filePath = this.$route.query.filePath
            }
          }, 500)
        }
      },
      getSearchParams() {
        const params = { ...this.searchParams }
        return params
      },

      /**
       * 重置搜索过滤项
      */
      handleSelectAllSearchOption() {
        this.$refs.handleMenu.instance.hide()
        this.customOption = this.getCustomOption(true)
        this.selectedOptionColumn = _.cloneDeep(this.customOption)
        localStorage.setItem(DUPC_SEARCH_OPTION_CACHE, JSON.stringify(this.selectedOptionColumn))
      },

      /**
       * 确认搜索过滤项
       */
      handleConfirmSearchOption() {
        this.$refs.handleMenu.instance.hide()
        this.selectedOptionColumn = _.cloneDeep(this.customOption)
        localStorage.setItem(DUPC_SEARCH_OPTION_CACHE, JSON.stringify(this.selectedOptionColumn))
      },
      downloadExcel() {
        const params = this.getSearchParams()
        params.pageSize = 300000
        if (this.totalCount > 300000) {
          this.$bkMessage({
            message: this.$t('当前问题数已超过30万个，无法直接导出excel，请筛选后再尝试导出。'),
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
                         this.$t('文件名'),
                         this.$t('路径'),
                         this.$t('重复块数'),
                         this.$t('重复行数'),
                         this.$t('函数总行数'),
                         this.$t('重复率'),
                         this.$t('相关作者'),
                         this.$t('风险'),
                         this.$t('提交时间')]
        const filterVal = ['index',
                           'fileName',
                           'filePath',
                           'blockNum',
                           'dupLines',
                           'totalLines',
                           'dupRate',
                           'authorList',
                           'riskFactor',
                           'fileChangeTime']
        const data = this.formatJson(filterVal, list)
        // eslint-disable-next-line
        const title = `${this.taskDetail.nameCn}-${this.taskDetail.taskId}-${this.toolId}-${this.$t('重复文件')}-${new Date().toISOString()}`
        export_json_to_excel(tHeader, data, title)
      },
      formatJson(filterVal, list) {
        let index = 1
        return list.map(item => filterVal.map((j) => {
          if (j === 'index') {
            return index += 1
          } if (j === 'riskFactor') {
            return this.defectSeverityMap[item.riskFactor]
          } if (j === 'fileChangeTime') {
            return this.formatTime(item.fileChangeTime, 'YYYY-MM-DD')
          }
          return item[j]
        }))
      },
      getDefectCountByType(type) {
        const tpyeFieldMap = {
          1: 'newCount',
          2: 'historyCount',
        }
        const count = this.listData[tpyeFieldMap[type]] || 0
        return count > 100000 ? this.$t('10万+') : count
      },
      toLogs() {
        this.$router.push({
          name: 'task-settings-trigger',
        })
      },
    },
  }
</script>

<style>
    @import "./codemirror.css";
</style>

<style lang="postcss" scoped>
    @import "../../css/mixins.css";
    @import "./defect-list.css";

    .dupc-list {
      padding: 16px 20px 0px 16px;
    }
    .breadcrumb {
      padding: 0px!important;
      .breadcrumb-name {
        background: white;
      }
    }
    .main-container {
      /* padding: 20px 33px 0!important;
        margin: 0 -13px!important; */
      /* border-top: 1px solid #dcdee5; */
      margin: 0px!important;
      background: white;
      .search-form {
        >>>.bk-label {
          font-size: 12px;
        }
        >>>.bk-select {
          font-size: 12px!important;
        }
      }
    }
    .file-list-table {
      >>> .list-row {
        cursor: pointer;
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
      float: left;

      @mixin ellipsis;
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
</style>
