package com.tencent.devops.common.api.annotation;

import java.util.HashMap;
import java.util.Map;

/**
 * 国际化信息模块，格式一般为：{表名}_{字段名}
 */
public class I18NModuleCode {

    // 工具
    public static final String TOOL_DISPLAY_NAME = "TOOL_DISPLAY_NAME";
    public static final String TOOL_DESCRIPTION = "TOOL_DESCRIPTION";

    // 规则集
    public static final String CHECKER_SET_CHECKER_SET_NAME = "CHECKER_SET_CHECKER_SET_NAME";
    public static final String CHECKER_SET_DESCRIPTION = "CHECKER_SET_DESCRIPTION";


    // 规则详情
    public static final String CHECKER_DETAIL_CHECKER_DESC = "CHECKER_DETAIL_CHECKER_DESC";
    public static final String CHECKER_DETAIL_CHECKER_DESC_MODEL = "CHECKER_DETAIL_CHECKER_DESC_MODEL";
    public static final String CHECKER_DETAIL_CHECKER_TYPE = "CHECKER_DETAIL_CHECKER_TYPE";
    public static final String CHECKER_DETAIL_CHECKER_CATEGORY_NAME = "CHECKER_DETAIL_CHECKER_CATEGORY_NAME";
    public static final String CHECKER_DETAIL_CHECKER_TAG = "CHECKER_DETAIL_CHECKER_TAG";


    // t_base_data: { param_type: 'TOOL_TYPE' }
    public static final String BASE_DATA_PARAM_CODE = "BASE_DATA_PARAM_CODE";
    public static final String BASE_DATA_PARAM_NAME = "BASE_DATA_PARAM_NAME";
    public static final String BASE_DATA_PARAM_EXTEND1 = "BASE_DATA_PARAM_EXTEND1";


    // 忽略类型(系统内置)
    public static final String IGNORE_TYPE_SYS_NAME = "IGNORE_TYPE_SYS_NAME";
}
