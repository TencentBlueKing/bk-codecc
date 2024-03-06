/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to
 * use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * 公共常量类
 *
 * @version V1.0
 * @date 2019/5/1
 */
public interface ComConstants {

    String SYSTEM_USER = "system";

    /**
     * 按处理人统计的当前遗留待修复告警的图表合计节点的名称
     */
    String TOTAL_CHART_NODE = "Total";

    /**
     * 是否中断后再执行的标志位，如果任务已经在执行，是否中断后再执行，0（不中断），1（中断），为空默认不中断
     */
    Integer ABORT_ANALYSIS_TASK_FLAG = 1;

    /**
     * PreCI规则集KEY
     */
    String PRECI_CHECKER_SET = "PRECI_CHECKER_SET";

    /**
     * PreCI-SCC自动语言判断
     */
    String PRECI_SCC_LANG_FILTER = "PRECI_SCC_LANG_FILTER";

    /**
     * 默认过滤路径类型
     */
    String PATH_TYPE_DEFAULT = "DEFAULT";

    /**
     * CODE_YML过滤路径类型
     */
    String PATH_TYPE_CODE_YML = "CODE_YML";

    /**
     * BizService的bean名（PatternBizTypeBizService）的后缀名,比如：COVERITYBatchMarkDefectBizService
     */
    String BIZ_SERVICE_POSTFIX = "BizService";

    /**
     * 通用Processor类名（CommonBatchBizTypeProcessorImpl）的前缀名,比如：COVERITYBatchMarkDefectProcessorImpl
     */
    String BATCH_PROCESSOR_INFIX = "Batch";

    /**
     * 通用BizService类名（CommonBizTypeBizServiceImpl）的前缀名
     */
    String COMMON_BIZ_SERVICE_PREFIX = "Common";
    /**
     * 项目已接入工具的名称之间的分隔符
     */
    String TOOL_NAMES_SEPARATOR = ",";

    /**
     * 分号分隔符
     */
    String SEPARATOR_SEMICOLON = ":";

    /**
     * GOML工具特殊参数rel_path
     */
    String PARAMJSON_KEY_REL_PATH = "rel_path";
    /**
     * GOML工具特殊参数go_path
     */
    String PARAMJSON_KEY_GO_PATH = "go_path";

    /**
     * 合计
     */
    String SUM = "合计";

    /**
     * 严重程度类别：严重（1），一般（2），提示（4）
     */
    int SERIOUS = 1;
    int NORMAL = 2;
    int PROMPT = 4;

    /**
     * 天数
     */
    int DAY_FOURTEEN = 14;
    int DAY_THIRTY = 30;
    int DAY_THIRTYONE = 31;

    /**
     * 数据库中表示缺陷严重程度为提示的值为3
     */
    int PROMPT_IN_DB = 3;

    /**
     * 常用整数
     */
    long COMMON_NUM_10000L = 10000L;
    long COMMON_NUM_1000L = 1000L;
    long COMMON_NUM_1L = 1L;
    long COMMON_NUM_10L = 10L;

    long COMMON_NUM_100L = 100L;
    long COMMON_NUM_0L = 0L;
    float COMMON_NUM_0F = 0F;
    double COMMON_NUM_0D = 0.0D;
    int COMMON_NUM_5000 = 5000;

    /**
     * ci新建任务前缀
     */
    String PIPELINE_ENNAME_PREFIX = "DEVOPS";

    String GONGFENG_ENNAME_PREFIX = "CODE";

    /**
     * CodeCC服务新建任务前缀
     */
    String CODECC_ENNAME_PREFIX = "CODECC";

    /**
     * 旧的CodeCC服务新建任务前缀
     */
    String OLD_CODECC_ENNAME_PREFIX = "LD_";

    /**
     * codecc平台转bs平台元数据类型 语言类型
     */
    String METADATA_TYPE_LANG = "LANG";
    /**
     * 其他項目語言
     */
    String OTHERS_PROJECT_LANGUAGE = "OTHERS";
    /**
     * 字符串定界符
     */
    String STRING_DELIMITER = "|";
    /**
     * 字符串分隔符
     */
    String STRING_SPLIT = ",";
    /**
     * 字符串标识符
     */
    String STRING_TIPS = "-";
    /**
     * 字符串前后缀
     */
    String STRING_PREFIX_OR_SUFFIX = "";
    /**
     * 字符串前后缀
     */
    String STRING_NULL_ARRAY = "[]";

    String DEFECT_STATUS_CLOSED = "closed";

    /**
     * 灰度任务前缀
     */
    String GARY_TASK = "GRAY_TASK_POOL";

    /**
     * 日期常量
     */
    String DATE_TODAY = "today";
    String DATE_YESTERDAY = "yesterday";
    String DATE_MONDAY = "monday";
    String DATE_LAST_MONDAY = "lastMonday";

    /**
     * 风险系数的配置的key前缀,
     * 比如：圈复杂度的风险系数，RISK_FACTOR_CONFIG:CCN hash
     */
    String PREFIX_RISK_FACTOR_CONFIG = "CONFIG_RISK_FACTOR";

    /**
     * 工具的顺序表
     */
    String KEY_TOOL_ORDER = "TOOL_ORDER";

    /**
     * 语言的顺序表
     */
    String KEY_LANG_ORDER = "LANG_ORDER";

    /**
     * 默认过滤路径
     */
    String KEY_DEFAULT_FILTER_PATH = "DEFAULT_FILTER_PATH";

    /**
     * 工具语言字段
     */
    String KEY_CODE_LANG = "LANG";

    /**
     * 过滤配置
     */
    String KEY_FILTER_CONFIG = "FILTER_CONFIG";

    /**
     * appCode映射
     */
    String KEY_APP_CODE_MAPPING = "APP_CODE_MAPPING";

    /**
     * 屏蔽用户成员名单
     */
    String KEY_EXCLUDE_USER_LIST = "EXCLUDE_USER_MEMBER";

    /**
     * 管理员名单
     */
    String KEY_ADMIN_MEMBER = "ADMIN_MEMBER";

    /**
     * OP管理员名单
     */
    String KEY_OP_ADMIN_MEMBER = "OP_ADMIN_MEMBER";

    /**
     * 流水线任务限制
     */
    String KEY_PIPELINE_TASK_LIMIT = "PIPELINE_TASK_LIMIT";

    /**
     * 流水线任务限制-默认Key
     */
    String DEFAULT_PIPELINE_TASK_LIMIT_KEY = "DEFAULT";

    /**
     * 流水线任务限制-默认Key
     */
    Integer DEFAULT_PIPELINE_TASK_LIMIT_VALUE = 50;

    /**
     * 工具对应的规范规则集ID的配置
     */
    String STANDARD_CHECKER_SET_ID = "STANDARD_CHECKER_SET_ID";

    /**
     * 定时任务初始化preci_user_daily表时,时间参数的范围
     */
    String PRECI_USER_CRON_JOB_TIME_RANGE = "PRECI_USER_CRON_JOB_TIME_RANGE";

    /**
     * 分号
     */
    String SEMICOLON = ";";

    /**
     * 逗号
     */
    String COMMA = ",";
    /**
     * ------------------------操作历史记录操作类型------------------
     */
    String REGISTER_TOOL = "register_tool";
    String MODIFY_INFO = "modify_info";
    String ENABLE_ACTION = "enable_action";
    String DISABLE_ACTION = "diable_action";
    String OPEN_CHECKER = "open_checker";
    String CLOSE_CHECKER = "close_checker";
    String TRIGGER_ANALYSIS = "trigger_analysis";
    String AUTHOR_TRANSFER = "author_transfer";
    // 批处理告警
    String BATCH_DEFECT = "batch_defect";
    // 忽略问题
    String DEFECT_IGNORE = "defect_ignore";
    // 恢复忽略
    String REVERT_IGNORE = "revert_ignore";
    // 标记问题
    String DEFECT_MARKED = "defect_marked";
    // 取消标记
    String DEFECT_UNMARKED = "defect_unmarked";
    // 告警处理人分配
    String ASSIGN_DEFECT = "assign_defect";
    // 添加代码评论
    String CODE_COMMENT_ADD = "code_comment_add";
    // 删除代码评论
    String CODE_COMMENT_DEL = "code_comment_del";
    // 告警提单
    String ISSUE_DEFECT = "issue_defect";
    // 更改人员权限
    String SETTINGS_AUTHORITY = "settings_authority";
    /**
     * ------------------------操作历史记录操作功能id------------------
     */
    //注册工具
    String FUNC_REGISTER_TOOL = "register_tool";
    //修改任务信息
    String FUNC_TASK_INFO = "task_info";
    //切换任务状态
    String FUNC_TASK_SWITCH = "task_switch";
    //切换工具状态
    String FUNC_TOOL_SWITCH = "tool_switch";
    //任务代码库更新
    String FUNC_CODE_REPOSITORY = "task_code";
    //规则配置
    String FUNC_CHECKER_CONFIG = "checker_config";
    //触发立即分析
    String FUNC_TRIGGER_ANALYSIS = "trigger_analysis";
    //定时扫描修改
    String FUNC_SCAN_SCHEDULE = "scan_schedule";
    //过滤路径
    String FUNC_FILTER_PATH = "filter_path";
    //告警管理
    String FUNC_DEFECT_MANAGE = "defect_manage";
    // 批处理告警
    String FUNC_BATCH_DEFECT = "batch_defect";
    // 忽略问题
    String FUNC_DEFECT_IGNORE = "defect_ignore";
    // 恢复忽略
    String FUNC_REVERT_IGNORE = "revert_ignore";
    // 标记问题
    String FUNC_DEFECT_MARKED = "defect_marked";
    // 取消标记
    String FUNC_DEFECT_UNMARKED = "defect_unmarked";
    // 告警处理人分配
    String FUNC_ASSIGN_DEFECT = "assign_defect";
    // 新增代码评论
    String FUNC_CODE_COMMENT_ADD = "code_comment_add";
    // 删除代码评论
    String FUNC_CODE_COMMENT_DEL = "code_comment_del";
    // 告警提单
    String FUNC_ISSUE_DEFECT = "issue_defect";
    // 更改人员权限
    String FUNC_SETTINGS_AUTHORITY = "settings_authority";
    /**
     * ----------------------------end----------------------------
     */

    /*-------------------------------Accept-Language-----------------------*/
    String ZH_CN = "ZH-CN";
    /**
     * Node规则包
     */
    String NODE = "NODE";
    /**
     * 风格规则包
     */
    String STYLISTIC = "STYLISTIC";
    /**
     * 严格模式包
     */
    String STRICT_MODE = "STRICT_MODE";
    /**
     * 逻辑规则包
     */
    String LOGICA = "LOGICAL";
    /**
     * 默认规则包
     */
    String DEFAULT = "DEFAULT";
    /**
     * 腾讯开源包
     */
    String TOSA = "TOSA";
    /**
     * 变量规则包
     */
    String VARIABLE = "VARIABLE";
    /**
     * ES6规则包
     */
    String ES6 = "ES6";
    /**
     * 最佳实践包
     */
    String BEST_PRACTICES = "BEST_PRACTICES";
    /**
     * 头文件规则包
     */
    String HEADER_FILE = "HEADER_FILE";
    /**
     * 系统API包
     */
    String SYS_API = "SYS_API";
    /**
     * OneSDK规则包
     */
    String ONESDK = "ONESDK";
    /**
     * 安全规则包
     */
    String SECURITY = "SECURITY";
    /**
     * 命名规范包
     */
    String NAMING = "NAMING";
    /**
     * 注释规则包
     */
    String COMMENT = "COMMENT";
    /**
     * 格式规范包
     */
    String FORMAT = "FORMAT";
    /**
     * ESLINT参数名 - eslint_rc
     */
    String PARAM_ESLINT_RC = "eslint_rc";
    /**
     * GOML参数名 - go_path
     */
    String PARAM_GOML_GO_PATH = "go_path";
    /**
     * ----------------------------end----------------------------
     */
    /**
     * GOML参数名 - rel_path
     */
    String PARAM_GOML_REL_PATH = "rel_path";
    /**
     * PYLINT参数名 - py_version
     */
    String PARAM_PYLINT_PY_VERSION = "py_version";
    /**
     * SPOTBUGS参数名 - script_type
     */
    String PARAM_SPOTBUGS_SCRIPT_TYPE = "script_type";
    /**
     * SPOTBUGS参数名 - script_content
     */
    String PARAM_SPOTBUGS_SCRIPT_CONTENT = "script_content";
    /**
     * PHPCS参数名 - script_type
     */
    String PARAM_PHPCS_XX = "script_type";
    /**
     * 圈复杂度阈值
     */
    String KEY_CCN_THRESHOLD = "ccn_threshold";
    /**
     * 默认圈复杂度阈值
     */
    int DEFAULT_CCN_THRESHOLD = 20;
    /**
     * PHPCS规范
     */
    String KEY_PHPCS_STANDARD = "phpcs_standard";
    /**
     * 下划线
     */
    String KEY_UNDERLINE = "_";
    String BLUEKING_LANGUAGE = "blueking_language";

    /**
     * 一键开启规则集key
     */
    String ONCE_CHECKER_SET_KEY = "ONCE_CHECKER_SET_KEY";


    String GRAY_PROJECT_PREFIX = "GRAY_TASK_POOL_";

    /**
     * 每日分析代码行统计
     */
    String TOTAL_BLANK = "totalBlank";
    String TOTAL_COMMENT = "totalComment";
    String TOTAL_CODE = "totalCode";
    List<String> MASK_STATUS = Arrays.asList("8", "16", "10", "12", "14", "18", "20", "22", "24", "26", "28", "30");

    String GONGFENG_PROJECT_ID_PREFIX = "CODE_";
    String CUSTOMPROJ_ID_PREFIX = "CUSTOMPROJ_";

    /**
     * 腾讯内部开源预发布规则开始结束时间
     */
    String PRE_PROD_TENCENT_OPENSOURCE_CHECKER_SET_TIME_GAP = "PRE_PROD_TENCENT_OPENSOURCE_CHECKER_SET_TIME_GAP";
    /**
     * 腾讯内部开源生产规则开始结束时间
     */
    String PROD_TENCENT_OPENSOURCE_CHECKER_SET_TIME_GAP = "PROD_TENCENT_OPENSOURCE_CHECKER_SET_TIME_GAP";
    /**
     * 腾讯外网开源预发布规则开始结束时间
     */
    String PRE_PROD_TENCENT_COMMUNITY_OPENSOURCE_CHECKER_SET_TIME_GAP
            = "PRE_PROD_TENCENT_COMMUNITY_OPENSOURCE_CHECKER_SET_TIME_GAP";
    /**
     * 腾讯外网开源生产规则开始结束时间
     */
    String PROD_TENCENT_COMMUNITY_OPENSOURCE_CHECKER_SET_TIME_GAP
            = "PROD_TENCENT_COMMUNITY_OPENSOURCE_CHECKER_SET_TIME_GAP";
    /**
     * 腾讯epc开源预发布规则开始结束时间
     */
    String PRE_PROD_TENCENT_EPC_OPENSOURCE_CHECKER_SET_TIME_GAP
            = "PRE_PROD_TENCENT_EPC_OPENSOURCE_CHECKER_SET_TIME_GAP";
    /**
     * 腾讯epc开源生产规则开始结束时间
     */
    String PROD_TENCENT_EPC_OPENSOURCE_CHECKER_SET_TIME_GAP = "PROD_TENCENT_EPC_OPENSOURCE_CHECKER_SET_TIME_GAP";
    String MAX_BUILD_LIST_SIZE = "MAX_BUILD_LIST_SIZE";

    /**
     * 分析结果commit到platform的锁的超时时间，设置为30分钟
     */
    int COMMIT_PLATFORM_LOCK_EXPIRY_TIME_MILLIS = 30 * 60 * 1000;

    /**
     * 自定义redis mq的锁的超时时间，设置为5秒
     */
    int REDIS_MQ_KEY_LOCK_EXPIRY_TIME_MILLIS = 5 * 1000;

    /**
     * 获取锁的超时时间，设置为10秒
     */
    int ACQUIRY_LOCK_EXPIRY_TIME_MILLIS = 10 * 1000;

    /**
     * 后台保存构建记录的最大条数
     * 定时清理构建任务根据此值来决定保留多少条构建记录
     */
    String MAX_SAVE_BUILD_NUM = "MAX_SAVE_BUILD_NUM";
    String CLEAN_CONSUMER_END_TIME = "CLEAN_CONSUMER_END_TIME";
    String CLEAN_NODE_THREAD_NUM = "CLEAN_NODE_THREAD_NUM";
    String CLEAN_TASK_STATUS = "CLEAN_TASK_STATUS";
    String CLEAN_TASK_WHITE_LIST = "CLEAN_TASK_WHITE_LIST";
    /**
     * 工具许可项目白名单(即只有指定的项目才能使用该工具，用于某些收费工具对特定项目使用)
     * {
     * "param_code" : "COVERITY", // 工具名
     * "param_name" : "工具许可项目白名单(即只有指定的项目才能使用该工具，用于某些收费工具对特定项目使用)",
     * "param_type" : "TOOL_LICENSE_WHITE_LIST",
     * "param_value" : "austin", // 项目ID，用逗号分割
     * "create_date" : NumberLong(1661241724935),
     * "created_by" : "austinshen",
     * "updated_date" : NumberLong(1661241724935),
     * "updated_by" : "austinshen"
     * }
     */
    String TOOL_LICENSE_WHITE_LIST = "TOOL_LICENSE_WHITE_LIST";
    /**
     * 数据迁移开关，单任务模式
     */
    String DATA_MIGRATION_SWITCH_SINGLE_MODE = "DATA_MIGRATION_SWITCH_SINGLE_MODE";
    /**
     * 数据迁移开关，批量迁移模式
     */
    String DATA_MIGRATION_SWITCH_BATCH_MODE = "DATA_MIGRATION_SWITCH_BATCH_MODE";
    /**
     * 批量迁移时的虚拟构建Id
     */
    String DATA_MIGRATION_VIRTUAL_BUILD_ID = "DATA_MIGRATION_VIRTUAL_BUILD_ID";

    /**
     * 查询的时候limit
     */
    int QUERY_LIMIT_ONE = 1;

    /**
     * 公共的查询分页大小
     */
    int COMMON_PAGE_SIZE = 1000;

    /**
     * 公共的查询分页大小
     */
    int SMALL_PAGE_SIZE = 100;

    /**
     * 公共的查询分页大小
     */
    int COMMON_BATCH_PAGE_SIZE = 3_0000;

    /**
     * 重复率提单分页大小
     */
    int DUPC_DEFECT_COMMIT_BATCH_PAGE_SIZE = 2_5000;

    /**
     * 重复率block_list字段大小限制
     */
    int DUPC_DEFECT_BLOCK_LIST_LIMIT = 100;

    int SCM_FILE_INFO_CACHE_BATCH_PAGE_SIZE = 5000;

    /**
     * 延迟消息的公共延迟时间（MS）
     */
    int COMMON_MSG_DELAY = 5_000;

    /**
     * 插件统计中查询任务列表分页大小
     */
    int COMMON_NUM_10000 = 10000;

    String DEFAULT_LANDUN_WORKSPACE = "/data/landun/workspace";

    /**
     * 展示规则key字符多少长度
     */
    int SHOW_CHECKER_KEY_LENGTH = 200;

    /**
     * 冷热分离，降冷开关
     */

    String SWITCH_FOR_DATA_SEPARATION_COOL_DOWN = "SWITCH_FOR_DATA_SEPARATION_COOL_DOWN";

    /**
     * 冷热分离，加热开关
     */
    String SWITCH_FOR_DATA_SEPARATION_WARM_UP = "SWITCH_FOR_DATA_SEPARATION_WARM_UP";

    /**
     * 业务类型
     */
    enum BusinessType {
        /**
         * 注册接入新工具
         */
        REGISTER_TOOL("RegisterTool"),

        /**
         * 告警查询
         */
        QUERY_WARNING("QueryWarning"),

        /**
         * 数据报表
         */
        DATA_REPORT("DataReport"),

        /**
         * 分析记录上报
         */
        ANALYZE_TASK("AnalyzeTask"),

        /**
         * 获取分析记录
         */
        GET_TASKLOG("GetTaskLog"),

        /**
         * 路径屏蔽
         */
        FILTER_PATH("FilterPath"),

        /**
         * 工具侧上报告警数据
         */
        UPLOAD_DEFECT("UploadDefect"),

        /**
         * 忽略告警
         */
        IGNORE_DEFECT("IgnoreDefect"),

        /**
         * 恢复忽略告警
         */
        REVERT_IGNORE("RevertIgnore"),

        /**
         * 改变忽略类型
         */
        CHANGE_IGNORE_TYPE("ChangeIgnoreType"),

        /**
         * 告警处理人分配
         */
        ASSIGN_DEFECT("AssignDefect"),

        /**
         * 告警处理人转换
         */
        AUTHOR_TRANS("AuthorTrans"),

        /**
         * 告警标志修改
         */
        MARK_DEFECT("MarkDefect"),

        /**
         * 查询规则包
         */
        QUERY_PKG("QueryCheckerPkg"),

        /**
         * 配置规则包
         */
        CONFIG_PKG("ConfigCheckerPkg"),

        /**
         * 查询分析统计结果
         */
        QUERY_STATISTIC("QueryStatistic"),

        /**
         * 树形服务
         */
        TREE_SERVICE("Tree"),

        /**
         * 分析结束生成报告
         */
        CHECK_REPORT("CheckerReport"),

        /**
         * 告警生成
         */
        DEFECT_OPERATE("DefectOperate"),

        /**
         * OP告警数据
         */
        DEFECT_DATA("DefectData"),

        /**
         * 告警提单
         */
        ISSUE_DEFECT("IssueDefect");

        private String value;

        BusinessType(String value) {
            this.value = value;
        }

        public String value() {
            return this.value;
        }
    }

    /**
     * 工具类型
     */
    enum Tool {
        COVERITY,
        KLOCWORK,
        PINPOINT,
        SENSITIVE,
        HORUSPY,
        WOODPECKER_SENSITIVE,
        RIPS,
        CPPLINT,
        CHECKSTYLE,
        ESLINT,
        STYLECOP,
        GOML,
        DETEKT,
        PHPCS,
        PYLINT,
        OCCHECK,
        CCN,
        DUPC,
        TSCLUA,
        SPOTBUGS,
        CLOC,
        FLAKE8,
        CLOJURE,
        IP_CHECK,
        GITHUBSTATISTIC,
        SCC,
        BLACKDUCK,
        WOODPECKER_COMMITSCAN,

        BKCHECK;
    }

    /**
     * 工具当前的状态/当前步骤的状态
     */
    enum StepStatus {
        SUCC(0),
        FAIL(1),

        /**
         * 只有步骤为AUTH(-2)：代码托管帐号鉴权，stepStatus = 0表示正常，1表示帐号密码过期，2表示代码没更新,3表示正在鉴权
         */
        NO_CHANGE(2),

        /**
         * 只有步骤为AUTH(-2)：代码托管帐号鉴权，stepStatus = 0表示正常，1表示帐号密码过期，2表示代码没更新,3表示正在鉴权
         */
        AUTH_ING(3);

        private int stepStatus;

        StepStatus(int stepStatus) {
            this.stepStatus = stepStatus;
        }

        public int value() {
            return this.stepStatus;
        }
    }

    /**
     * 上报分析步骤的状态标记,包括成功、失败、进行中、中断
     */
    enum StepFlag {
        SUCC(1),
        FAIL(2),
        PROCESSING(3),
        ABORT(4);

        private int stepFlag;

        StepFlag(int stepStatus) {
            this.stepFlag = stepStatus;
        }

        public int value() {
            return this.stepFlag;
        }
    }

    /**
     * 项目接入多工具步骤
     */
    enum Step4MutliTool {
        /**
         * 步骤：代码托管帐号鉴权，stepStatus = 0表示正常，1表示帐号密码过期，2表示代码没更新,3表示正在鉴权
         *
         * @date 2017/9/13
         * @version V2.4
         */
        AUTH(-2),

        /**
         * 申请中，该状态已经废弃
         */
        APPLYING(-1),

        /**
         * 接入完成
         */
        READY(0),

        /**
         * 排队状态
         */
        QUEUE(1),

        /**
         * 代码下载
         */
        DOWNLOAD(2),

        /**
         * 代码扫描
         */
        SCAN(3),

        /**
         * 代码缺陷提交
         */
        COMMIT(4),

        /**
         * 分析完成
         */
        COMPLETE(5);

        private int value;

        Step4MutliTool(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }
    }

    /**
     * 项目接入Coverity工具步骤
     */
    enum Step4Cov {
        /**
         * 申请中，该状态已经废弃
         */
        APPLYING(-1),

        /**
         * 接入完成
         */
        READY(0),

        /**
         * 上传
         */
        UPLOAD(1),

        /**
         * 排队状态
         */
        QUEUE(2),

        /**
         * 分析中
         */
        ANALYZE(3),

        /**
         * 工具缺陷提交自带platform
         */
        COMMIT(4),

        /**
         * 将告警从platform同步到codecc
         */
        DEFECT_SYNS(5),

        /**
         * 分析完成
         */
        COMPLETE(6);

        private int value;

        Step4Cov(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }
    }

    /**
     * 文件类型
     */
    enum FileType {
        NEW(1),
        HISTORY(2),
        FIXED(4),
        IGNORE(8);

        private int value;

        FileType(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public String stringValue() {
            return String.valueOf(value);
        }
    }

    /**
     * 规则包分类，默认规则包-0；安全规则包-1；内存规则包-2；编译警告包-3；
     * 系统API包-4；性能问题包-5；表达式问题包-6；可疑问题包-7；定制规则包-8；ONESDK规范规则包-9
     * 腾讯开源包-10；
     */
    enum CheckerPkgKind {
        DEFAULT("0"),
        SECURITY("1"),
        MEMORY("2"),
        PERFORMANCE("3"),
        COMPILE("4"),
        SYS_API("5"),
        EXPRESSION("6"),
        POSSIBLE("7"),
        CUSTOM("8"),
        LOGICAL("9"),
        STYLISTIC("10"),
        BEST_PRACTICES("11"),
        HEADER_FILE("12"),
        ES6("13"),
        NODE("14"),
        VARIABLE("15"),
        STRICT_MODE("16"),
        FORMAT("17"),
        NAMING("18"),
        COMMENT("19"),
        KING_KONG("20"),
        TOSA("21");

        private String value;

        CheckerPkgKind(String value) {
            this.value = value;
        }

        public static String getValueByName(String name) {
            if (values() != null) {
                for (CheckerPkgKind checkerPkgKind : values()) {
                    if (checkerPkgKind.name().equalsIgnoreCase(name)) {
                        return checkerPkgKind.value;
                    }
                }
            }
            return null;
        }

        public String value() {
            return value;
        }
    }

    /**
     * 任务语言
     */
    enum CodeLang {
        C_SHARP(1L, "C#", "C#"),
        C_CPP(2L, "C/C++", "C/C++"),
        JAVA(4L, "JAVA", "JAVA"),
        PHP(8L, "PHP", "PHP"),
        OC(16L, "Objective-C/C++", "OC/OC++"),
        PYTHON(32L, "Python", "Python"),
        JS(64L, "JavaScript", "JS"),
        RUBY(128L, "Ruby", "Ruby"),
        LUA(256L, "LUA", "LUA"),
        GOLANG(512L, "Golang", "Golang"),
        SWIFT(1024L, "SWIFT", "Swift"),
        TYPESCRIPT(2048L, "TypeScript", "TS"),
        KOTLIN(4096L, "Kotlin", "Kotlin"),
        CSS(65536L, "CSS", "CSS"),
        OTHERS(1073741824L, "OTHERS", "其他");

        private Long langValue;

        private String langName;

        private String displayName;

        CodeLang(Long langValue, String langName, String displayName) {
            this.langValue = langValue;
            this.langName = langName;
            this.displayName = displayName;
        }

        public static String getCodeLang(Long value) {
            if (values() != null) {
                for (CodeLang lang : values()) {
                    if (lang.langValue.equals(value)) {
                        return lang.displayName();
                    }
                }
            }
            return null;
        }

        public static Set<String> getCodeLangAll(Long langValue) {
            Set<String> set = new HashSet<>();
            if (values() != null) {
                for (CodeLang value : values()) {
                    if (value.langValue > langValue) {
                        break;
                    }
                    if ((value.langValue & langValue) > 0) {
                        set.add(value.langName);
                    }
                }
            }
            return set;
        }

        public static List<Long> getCodeLangValueList(Long langValue) {
            List<Long> list = new ArrayList<>();
            for (CodeLang value : values()) {
                if (value.langValue > langValue) {
                    break;
                }
                if ((value.langValue & langValue) > 0) {
                    list.add(value.langValue);
                }
            }
            return list;
        }

        public Long langValue() {
            return langValue;
        }

        public String langName() {
            return langName;
        }

        public String displayName() {
            return displayName;
        }
    }

    /**
     * 风险系数：极高-SH, 高-H，中-M，低-L
     */
    enum RiskFactor {
        SH(1, "极高"),
        H(2, "高"),
        M(4, "中"),
        L(8, "低");

        private int value;

        private String desc;

        RiskFactor(int value, String desc) {
            this.value = value;
            this.desc = desc;
        }

        public static RiskFactor get(int value) {
            for (RiskFactor riskFactor : RiskFactor.values()) {
                if (riskFactor.value() == value) {
                    return riskFactor;
                }
            }

            throw new IllegalArgumentException("invalid ccn severity: " + value);
        }

        public int value() {
            return value;
        }

        public String desc() {
            return desc;
        }
    }

    /**
     * 区分蓝盾codecc任务创建来源
     */
    enum BsTaskCreateFrom {
        /**
         * codecc服务创建的codecc任务
         */
        BS_CODECC("bs_codecc"),

        /**
         * 蓝盾流水线创建的codecc任务
         */
        BS_PIPELINE("bs_pipeline"),

        /**
         * 工蜂代码扫描任务
         */
        GONGFENG_SCAN("gongfeng_scan"),

        /**
         * API 触发创建任务
         */
        API_TRIGGER("api_trigger"),

        /**
         * 定时扫描任务
         */
        TIMING_SCAN("timing_scan");

        private String value;

        BsTaskCreateFrom(String value) {
            this.value = value;
        }

        @NotNull
        public static Set<String> getByStatType(Set<String> type) {
            Set<String> createFrom;
            if (type != null) {
                createFrom = Sets.newHashSet();
                if (type.contains(DefectStatType.GONGFENG_SCAN.value)) {
                    // 开源
                    createFrom.add(GONGFENG_SCAN.value());
                }
                if (type.contains(DefectStatType.USER.value)) {
                    // 非开源
                    createFrom.add(BS_CODECC.value());
                    createFrom.add(BS_PIPELINE.value());
                }
            } else {
                createFrom = Sets.newHashSet(BS_CODECC.value(), BS_PIPELINE.value(), GONGFENG_SCAN.value());
            }
            return createFrom;
        }

        public String value() {
            return this.value;
        }
    }

    enum EslintFrameworkType {
        standard, vue, react
    }

    /**
     * 工具跟进状态
     */
    // NOCC:TypeName(工具误报:)
    enum FOLLOW_STATUS {
        NOT_FOLLOW_UP_0(0), //未跟进
        NOT_FOLLOW_UP_1(1), //未跟进
        EXPERIENCE(2),        //体验
        ACCESSING(3),        //接入中
        ACCESSED(4),        //已接入
        HANG_UP(5),            //挂起
        WITHDRAW(6);        //下架

        private int value;

        FOLLOW_STATUS(int value) {
            this.value = value;
        }

        @NotNull
        public static List<Integer> getEffectiveStatus() {
            return Lists.newArrayList(NOT_FOLLOW_UP_0.value, NOT_FOLLOW_UP_1.value, ACCESSED.value, EXPERIENCE.value);
        }

        @NotNull
        public static List<Integer> getNeWithDrawList() {
            List<Integer> statuses = new LinkedList<>();
            for (FOLLOW_STATUS status : FOLLOW_STATUS.values()) {
                if (status.value != WITHDRAW.value) {
                    statuses.add(status.value);
                }
            }
            return statuses;
        }

        public int value() {
            return value;
        }
    }

    /**
     * PHPCS规范编码
     */
    enum PHPCSStandardCode {
        PEAR(1),
        Generic(2),
        MySource(4),
        PSR2(8),
        PSR1(16),
        Zend(32),
        PSR12(64),
        Squiz(128);

        private int code;

        PHPCSStandardCode(int code) {
            this.code = code;
        }

        public int code() {
            return this.code;
        }
    }

    /**
     * 工具处理模式
     */
    enum ToolPattern {
        LINT,
        COVERITY,
        KLOCWORK,
        PINPOINT,
        CCN,
        DUPC,
        CLOC,
        SCC,
        STAT,
        TSCLUA;

        @NotNull
        public static List<String> getCommonPatternList() {
            return Lists.newArrayList(COVERITY.name(), KLOCWORK.name(), PINPOINT.name());
        }
    }

    /**
     * 流水线工具配置操作类型
     */
    enum PipelineToolUpdateType {
        ADD,
        REPLACE,
        REMOVE,
        GET
    }

    /*------------------------------- 工具参数提示国际化 -----------------------*/
    enum CommonJudge {
        COMMON_Y("Y"),
        COMMON_N("N");
        String value;

        CommonJudge(String value) {
            this.value = value;
        }

        public String value() {
            return this.value;
        }

    }

    /**
     * 任务文件状态
     */
    enum TaskFileStatus {
        NEW(1),
        PATH_MASK(8);

        private int value;

        TaskFileStatus(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    /**
     * 缺陷类型
     */
    enum DefectType {
        NEW(1),
        HISTORY(2);

        private int value;

        DefectType(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public String stringValue() {
            return String.valueOf(value);
        }
    }

    /**
     * 缺陷状态
     */
    enum DefectStatus {
        NEW(1),
        FIXED(2),
        IGNORE(4),
        PATH_MASK(8),
        CHECKER_MASK(16);

        private int value;

        DefectStatus(int value) {
            this.value = value;
        }

        public static DefectStatus valueOf(int value) {
            for (DefectStatus defectStatus : DefectStatus.values()) {
                if (defectStatus.value() == value) {
                    return defectStatus;
                }
            }
            return null;
        }

        public int value() {
            return value;
        }
    }

    /**
     * 聚类类型
     */
    enum ClusterType {
        file,
        defect
    }

    /**
     * rdm项目coverity分析状态
     */
    enum RDMCoverityStatus {
        success, failed
    }

    /**
     * 代码托管类型，包括SVN、GIT等
     */
    enum CodeHostingType {
        PERFORCE,
        SVN,
        GIT,
        HTTP_DOWNLOAD,
        GITHUB;
    }

    /**
     * 通用状态，0-启用，1-停用
     */
    enum Status {
        ENABLE(0),
        DISABLE(1);

        private int value;

        Status(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    /**
     * REPAIR: 待修复告警趋势，NEW: 每日新增告警，CLOSE: 每日关闭/修复告警
     */
    enum ChartType {
        REPAIR,
        NEW,
        CLOSE,
    }

    /**
     * 扫描方式
     */
    enum ScanType {
        /**
         * 全量
         */
        FULL(0),

        /**
         * 增量
         */
        INCREMENTAL(1),

        /**
         * diff模式
         */
        DIFF_MODE(2),

        /**
         * 快速增量，以下配置没有变更时，不需要执行扫描，直接生成结果
         * 1.规则
         * 2.规则集
         * 3.路径黑名单
         * 4.路径白名单
         * 5.代码(包括代码库)
         * 6.工具不是重新启用
         * 7.工具镜像
         */
        FAST_INCREMENTAL(3),

        /**
         * 局部增量（其他配置不变，只有规则变化时，coverity/klocwork等编译工具不需要重新执行构建，只需要执行analyze和commit）
         */
        PARTIAL_INCREMENTAL(4),

        /**
         * diff模式
         */
        FILE_DIFF_MODE(5),
        /**
         * diff模式 ： 分支差异扫描 - 与2类似，但非MR触发
         */
        BRANCH_DIFF_MODE(6);

        public int code;

        ScanType(int code) {
            this.code = code;
        }
    }

    /**
     * 忽略告警原因类型
     */
    enum IgnoreReasonType {
        // 默认为0不是已忽略告警
        DEFAULT(0),
        // 工具误报
        ERROR_DETECT(1),
        // 设计如此
        SPECIAL_PURPOSE(2),
        // 其它
        OTHER(4);

        private int ignoreReasonType;

        private IgnoreReasonType(int ignoreReasonType) {
            this.ignoreReasonType = ignoreReasonType;
        }

        public int value() {
            return this.ignoreReasonType;
        }
    }

    /**
     * 机器人通知范围
     */
    enum BotNotifyRange {
        /**
         * 新增告警
         */
        NEW(1),

        /**
         * 遗留告警(新+旧)
         */
        EXIST(2);

        public int code;

        BotNotifyRange(int code) {
            this.code = code;
        }
    }

    /**
     * 统计项
     */
    enum StaticticItem {
        NEW,
        EXIST,
        CLOSE,
        FIXED,
        EXCLUDE,
        NEW_PROMPT,
        NEW_NORMAL,
        NEW_SERIOUS,
        EXIST_PROMPT,
        EXIST_NORMAL,
        EXIST_SERIOUS;
    }

    /**
     * 报告类型：定时报告T，即时报告I, 开源检查报告O
     */
    enum ReportType {
        T,
        I,
        O,
        A;
    }

    enum InstantReportStatus {
        ENABLED("1"),
        DISABLED("2");

        private String code;

        InstantReportStatus(String code) {
            this.code = code;
        }

        public String code() {
            return this.code;
        }
    }

    enum EmailReceiverType {
        /**
         * 所有人(最新文案：所有权限角色（不含组织）)
         */
        TASK_MEMBER("0"),

        /**
         * 仅管理员(最新文案：仅拥有者（不含组织）)
         */
        TASK_OWNER("1"),

        /**
         * 自定义
         */
        CUSTOMIZED("2"),

        /**
         * 不发送
         */
        NOT_SEND("3"),

        /**
         * 遗留处理人
         */
        ONLY_AUTHOR("4");

        private String code;

        EmailReceiverType(String code) {
            this.code = code;
        }

        public String code() {
            return this.code;
        }
    }

    /**
     * 告警上报状态
     */
    enum DefectReportStatus {
        PROCESSING,
        SUCCESS,
        FAIL
    }

    enum MarkStatus {
        NOT_MARKED(0),
        MARKED(1),
        NOT_FIXED(2);

        private int value;

        MarkStatus(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }
    }

    /**
     * codecc分发路由规则（用于配置在codeccDispatchType中）
     */
    enum CodeCCDispatchRoute {
        //独立构建机集群
        INDEPENDENT(-1L),
        //开源扫描集群
        OPENSOURCE(-2L),

        //闭源扫描集群
        CLOSEDSOURCE(-3L),

        //devcloud集群
        DEVCLOUD(-101L),
        //EPC 独立的DockerHost集群
        EPC_INDEPENDENT(-102L);

        private Long flag;

        CodeCCDispatchRoute(Long flag) {
            this.flag = flag;
        }

        public static CodeCCDispatchRoute valueOf(Long flag) {
            for (CodeCCDispatchRoute value : CodeCCDispatchRoute.values()) {
                if (value.flag().equals(flag)) {
                    return value;
                }
            }
            return null;
        }

        public Long flag() {
            return this.flag;
        }
    }

    /**
     * 开源扫描规则集类型
     */
    enum OpenSourceCheckerSetType {
        //全量规则集
        FULL("FULL"),
        //简化规则集
        SIMPLIFIED("SIMPLIFIED"),
        //两者规则集都配置
        BOTH("BOTH"),
        //oteam专有规则集
        OTEAM("OTEAM"),
        //oteam且配置了ci的yml文件专有规则集
        OTEAM_CI("OTEAM_CI");

        private String type;

        OpenSourceCheckerSetType(String type) {
            this.type = type;
        }

        public String getType() {
            return this.type;
        }
    }

    /**
     * CLOC 告警查询类型
     */
    enum CLOCOrder {
        // 根据文件查询
        FILE,
        // 根据语言查询
        LANGUAGE
    }

    /**
     * 告警统计类型
     */
    enum StatisticType {
        // 按状态统计
        STATUS,

        // 按严重程度统计
        SEVERITY,

        // 按新旧告警统计
        DEFECT_TYPE
    }

    /**
     * 工具类型
     */
    enum AtomCode {
        CODECC_V2("CodeccCheckAtom"),
        CODECC_V3("CodeccCheckAtomDebug");

        private String code;

        AtomCode(String code) {
            this.code = code;
        }

        public String code() {
            return this.code;
        }
    }

    enum EmailNotifyTemplate {
        BK_PLUGIN_FAILED_TEMPLATE("BK_PLUGIN_FAILED_TEMPLATE");

        private String templateCode;

        EmailNotifyTemplate(String templateCode) {
            this.templateCode = templateCode;
        }

        public String value() {
            return this.templateCode;
        }
    }

    enum WeChatNotifyTemplate {
        BK_PLUGIN_FAILED_TEMPLATE("BK_PLUGIN_FAILED_TEMPLATE");

        private String templateCode;

        WeChatNotifyTemplate(String templateCode) {
            this.templateCode = templateCode;
        }

        public String value() {
            return this.templateCode;
        }
    }

    enum ScanStatus {
        //正在扫描中
        PROCESSING(3),
        //成功
        SUCCESS(0),
        //失败
        FAIL(1);
        private Integer code;

        ScanStatus(Integer code) {
            this.code = code;
        }

        public static String convertScanStatus(Integer code) {
            String status;
            if (PROCESSING.code.equals(code)) {
                status = "分析中";
            } else if (FAIL.code.equals(code)) {
                status = "分析失败";
            } else if (SUCCESS.code.equals(code)) {
                status = "分析成功";
            } else {
                status = "未知状态" + code;
            }
            return status;
        }

        public Integer getCode() {
            return this.code;
        }
    }

    enum ToolType {
        STANDARD,
        SECURITY,
        DUPC,
        CCN,
        DEFECT,
        CLOC,
        STAT;

        // LINT问题管理的全维度
        public static final Set<String> DIMENSION_FOR_LINT_PATTERN_SET = ImmutableSet.of(
                ToolType.DEFECT.name(),
                ToolType.SECURITY.name(),
                ToolType.STANDARD.name()
        );

        // LINT问题管理的全维度
        public static final List<String> DIMENSION_FOR_LINT_PATTERN_LIST = ImmutableList.copyOf(
                DIMENSION_FOR_LINT_PATTERN_SET
        );

        public static final Set<String> DUPC_CCN_SCC_CLOC_SET = ImmutableSet.of(
                Tool.DUPC.name(),
                Tool.CCN.name(),
                Tool.SCC.name(),
                Tool.CLOC.name()
        );
    }

    enum BaseConfig {
        // 不支持增量的工具列表
        INCREMENTAL_EXCEPT_TOOLS,

        // 支持coverity增量的灰度任务白名单列表
        INCREMENTAL_TASK_WHITE_LIST,

        // 支持快速增量的灰度任务白名单列表
        FAST_INCREMENTAL_TASK_WHITE_LIST,

        // 支持快速增量的开源灰度任务白名单列表
        FAST_INCREMENTAL_OPENSOURCE_TASK_WHITE_LIST,

        //安全工具
        SECURITY_TOOLS,

        // 规范工具
        STANDARD_TOOLS
    }

    enum DefectStatType {
        /**
         * 所有任务范围
         */
        ALL("all"),
        /**
         * 非开源扫描（服务、流水线）
         */
        USER("user"),
        /**
         * 开源扫描
         */
        GONGFENG_SCAN("gongfeng_scan");

        private String value;

        DefectStatType(String value) {
            this.value = value;
        }

        public String value() {
            return this.value;
        }
    }

    enum ScanStatType {
        /**
         * 超快增量
         */
        IS_FAST_INCREMENT("IS_FAST_INCRE"),
        /**
         * 非超快增量
         */
        NOT_FAST_INCREMENT("NOT_FAST_INCRE");

        private String value;

        ScanStatType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    enum CheckerSetType {
        /**
         * 自主配置
         */
        NORMAL("normal"),

        /**
         * 内网开源治理
         */
        OPEN_SCAN("openScan"),

        /**
         * 外网开源
         */
        COMMUNITY_OPEN_SCAN("communityOpenScan"),

        /**
         * PCG EPC
         */
        EPC_SCAN("epcScan");

        private String value;

        CheckerSetType(String value) {
            this.value = value;
        }

        @JsonCreator
        public static CheckerSetType forValue(String value) {
            CheckerSetType[] checkerSetTypes = CheckerSetType.values();
            for (CheckerSetType checkerSetType : checkerSetTypes) {
                if (checkerSetType.value.equalsIgnoreCase(value)) {
                    return checkerSetType;
                }
            }
            return NORMAL;
        }

        @JsonValue
        public String value() {
            return value;
        }
    }

    /**
     * 工具集成进展状态：T-测试，G-灰度，P-发布, O-开源扫描(Opensource), D-下架
     */
    enum ToolIntegratedStatus {
        T(-1),
        G(-2),
        PRE_PROD(-3),
        P(0),
        O(1),
        D(-99);

        private int value;

        ToolIntegratedStatus(int value) {
            this.value = value;
        }

        public static ToolIntegratedStatus getInstance(int value) {
            ToolIntegratedStatus[] values = ToolIntegratedStatus.values();
            for (ToolIntegratedStatus toolIntegratedStatus : values) {
                if (toolIntegratedStatus.value == value) {
                    return toolIntegratedStatus;
                }
            }
            return P;
        }

        public int value() {
            return this.value;
        }
    }

    /**
     * 文件生成过程状态标识
     */
    enum FileStatus {
        /**
         * 未开始
         */
        NOT_STARTED("-1"),

        /**
         * 已导出完成
         */
        FINISH("0"),

        /**
         * 正在生成中
         */
        DOING("1");

        private String code;

        FileStatus(String code) {
            this.code = code;
        }

        public String getCode() {
            return this.code;
        }
    }

    /**
     * 动态配置的Key
     */
    enum DynamicConfigKey {
        /**
         * 是否开启 CodeCC扫描集群切换, 目前仅对API接入的任务生效
         * DevCloud -> DockerHost
         * 1 开启  0 关闭(默认)
         */
        CLUSTER_SWITCH_ENABLE("cluster_switch_enable", "0"),

        /**
         * 控制切换的项目比例，当 CLUSTER_SWITCH_ENABLE=1 开启时起作用
         * 数值为 1 - 100，当数值为100时，所有EPC项目将完成切换
         */
        CLUSTER_SWITCH_SCALE("cluster_switch_scale", "5"),

        /**
         * 进行扫描集群替换的APPCODE，当 CLUSTER_SWITCH_ENABLE=1 开启时起作用
         * 默认EPC
         */
        CLUSTER_SWITCH_APP_CODE("cluster_switch_app_code", "show-code"),
        /**
         * 进行扫描集群替换的目标集群，当 CLUSTER_SWITCH_ENABLE=1 开启时起作用
         * 默认INDEPENDENT(-1L)
         */
        CLUSTER_SWITCH_DEST("cluster_switch_dest", "-1"),
        /**
         * 切换过程中的默认集群
         * 默认INDEPENDENT(-1L)
         */
        CLUSTER_SWITCH_DEFAULT_CLUSTER("cluster_switch_default_cluster", "-1"),

        /**
         * 有效注释过滤语言列表（使用逗号分隔）
         */
        EFFECTIVE_COMMENT_FILTER_LANGS("effective_comment_filter_langs", "");

        private String key;

        private String defaultValue;

        DynamicConfigKey(String key, String defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        public String getKey() {
            return this.key;
        }

        public String getDefaultValue() {
            return this.defaultValue;
        }
    }

    /**
     * 统计维度
     */
    enum StatDimension {
        /**
         * 每天
         */
        DAILY("daily"),
        /**
         * 每周
         */
        WEEKLY("weekly");

        private String type;

        StatDimension(String type) {
            this.type = type;
        }

        public String getType() {
            return this.type;
        }
    }

    /**
     * 腾讯内部开源规则类型
     */
    enum CheckerSetEnvType {
        PRE_PROD("preProd"),
        PROD("prod");

        private String key;

        CheckerSetEnvType(String key) {
            this.key = key;
        }

        public static CheckerSetEnvType getCheckerSetEnvType(String key) {
            if (StringUtils.isBlank(key)) {
                return null;
            }
            if (key.equals(PRE_PROD.getKey())) {
                return PRE_PROD;
            }
            if (key.equals(PROD.getKey())) {
                return PROD;
            }
            return null;
        }

        public String getKey() {
            return key;
        }
    }

    /**
     * 忽略缺陷定时通知人
     */
    enum IgnoreTypeNotifyReceiverType {
        /**
         * 忽略人
         */
        IGNORE_AUTHOR("ignore_author"),
        /**
         * 问题作者
         */
        DEFECT_AUTHOR("defect_author"),
        /**
         * 任务创建人
         */
        TASK_CREATOR("task_creator"),
        ;

        private String type;

        IgnoreTypeNotifyReceiverType(String type) {
            this.type = type;
        }

        public String getType() {
            return this.type;
        }
    }

    /**
     * 通知类型
     */
    enum NotifyType {
        /**
         * 邮件
         */
        EMAIL("email"),
        /**
         * 企业微信
         */
        RTX("rtx");

        private String type;

        NotifyType(String type) {
            this.type = type;
        }

        public String getType() {
            return this.type;
        }
    }

    /**
     * 通知类型
     */
    enum NotifyTypeCreateFrom {
        /**
         * 邮件
         */
        PROJECT("project"),
        /**
         * 企业微信
         */
        SYS("sys");

        private String type;

        NotifyTypeCreateFrom(String type) {
            this.type = type;
        }

        public String getType() {
            return this.type;
        }
    }


    /**
     * 数据迁移状态
     */
    enum DataMigrationStatus {
        PROCESSING(-1),
        FAIL(0),
        SUCCESS(1);


        private int status;

        DataMigrationStatus(int status) {
            this.status = status;
        }

        public int value() {
            return this.status;
        }
    }


    /**
     * 缺陷消费类型
     */
    enum DefectConsumerType {
        FAST_INCREMENT,
        DEFECT_COMMIT;

    }


    enum PreCiInstallType {

        NO_LIMIT(-1), INSTALL(0), NO_INSATLL(1);

        private Integer status;

        PreCiInstallType(Integer status) {
            this.status = status;
        }

        public Integer getStatus() {
            return status;
        }
    }

    enum ScoreRedLineEnum {
        CODE_STANDARD_SCORE,
        CODE_SECURITY_SCORE,
        CODE_CCN_SCORE
    }

    /**
     * 缺陷消费类型
     */
    enum BizServiceFlag {
        CORE(""),
        EXT("Tencent");

        String flag;

        BizServiceFlag(String flag) {
            this.flag = flag;
        }

        public String getFlag() {
            return flag;
        }
    }


    enum ColdDataArchivingType {
        LINT,
        DUPC,
        CCN,
        CLOC,
        STAT
    }

    enum ColdDataPurgingType {
        DEFECT,
        SNAPSHOT,
        STATISTIC,
        FILE_CACHE,
        SCM,
        OTHERS
    }

    /**
     * mock: TaskConstants.java
     */
    enum TaskStatus {
        ENABLE(0),
        DISABLE(1),
        COLD(2);

        private Integer value;

        TaskStatus(Integer value) {
            this.value = value;
        }

        public Integer value() {
            return value;
        }
    }

    /**
     * 用户状态
     */
    enum UserStatusType {
        /**
         * 未识别（p_）
         */
        UNIDENTIFIED(-1),
        /**
         * 在职
         */
        EMPLOYED(1),
        /**
         * 离职
         */
        RESIGNED(2),
        /**
         * 试用
         */
        ON_PROBATION(3);

        Integer id;
        UserStatusType(Integer id) {
            this.id = id;
        }

        public Integer getId() {
            return id;
        }
    }
}
