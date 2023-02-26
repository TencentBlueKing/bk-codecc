package com.tencent.devops.common.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * 国际化信息模块，格式一般为：{表名}_{字段名}
 */
public class I18NModuleCode {
    private final static Map<String,String> TEST =new HashMap<>();

    public final static String TOOL_DISPLAY_NAME = "TOOL_DISPLAY_NAME";
    public final static String TOOL_DESCRIPTION = "TOOL_DESCRIPTION";

    public final static String CHECKER_SET_CHECKER_SET_NAME = "CHECKER_SET_CHECKER_SET_NAME";
    public final static String CHECKER_SET_DESCRIPTION = "CHECKER_SET_DESCRIPTION";

    public final static String CHECKER_DETAIL_CHECKER_DESC = "CHECKER_DETAIL_CHECKER_DESC";
    public final static String CHECKER_DETAIL_CHECKER_DESC_MODEL = "CHECKER_DETAIL_CHECKER_DESC_MODEL";
    public final static String CHECKER_DETAIL_CHECKER_TYPE = "CHECKER_DETAIL_CHECKER_TYPE";
}
