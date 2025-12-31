package com.tencent.devops.common.constant.audit;

import static com.tencent.devops.common.constant.ComConstants.CREATE_ACTION;
import static com.tencent.devops.common.constant.ComConstants.DELETE_ACTION;
import static com.tencent.devops.common.constant.ComConstants.DISABLE_ACTION;
import static com.tencent.devops.common.constant.ComConstants.FUNC_BATCH_DEFECT;
import static com.tencent.devops.common.constant.ComConstants.FUNC_CHECKER_CONFIG;
import static com.tencent.devops.common.constant.ComConstants.FUNC_CHECKER_SET_CONFIG;
import static com.tencent.devops.common.constant.ComConstants.FUNC_REGISTER_TOOL;
import static com.tencent.devops.common.constant.ComConstants.FUNC_TASK_MANAGE;
import static com.tencent.devops.common.constant.ComConstants.FUNC_TASK_SWITCH;
import static com.tencent.devops.common.constant.ComConstants.UPDATE_ACTION;

/**
 * 兼容已有的 OperationHistory 体系, 所有的 ActionIds 均以 (funcId)[_(operType)] 的形式拼接而成.
 * 创建任务和创建规则集这 2 个 action 由于蓝盾已在权限中心注册, 直接复用蓝盾注册的 action id.
 */
public interface ActionIds {
    // 任务
    String CREATE_TASK = "codecc_task_create";
    String STOP_TASK = FUNC_TASK_SWITCH + "_" + DISABLE_ACTION;     // 停用任务
    String DELETE_TASK = FUNC_TASK_MANAGE + "_" + DELETE_ACTION;

    // 规则集
    String CREATE_CHECKER_SET = "codecc_rule_set_create";
    String UPDATE_CHECKER_SET = FUNC_CHECKER_SET_CONFIG + "_" + UPDATE_ACTION;

    // 规则
    String CREATE_REGEX_RULE = FUNC_CHECKER_CONFIG + "_" + CREATE_ACTION;
    String UPDATE_REGEX_RULE = FUNC_CHECKER_CONFIG + "_" + UPDATE_ACTION;

    // 工具
    String REGISTER_TOOL = FUNC_REGISTER_TOOL;    // 工具集成

    // 告警处理
    String PROCESS_DEFECT = FUNC_BATCH_DEFECT;
}
