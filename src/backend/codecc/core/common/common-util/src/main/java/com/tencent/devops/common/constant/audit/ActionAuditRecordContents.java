package com.tencent.devops.common.constant.audit;

import static com.tencent.bk.audit.constants.AuditAttributeNames.INSTANCE_ID;
import static com.tencent.bk.audit.constants.AuditAttributeNames.INSTANCE_NAME;
import static com.tencent.devops.common.constant.audit.CodeccAuditAttributeNames.TASK_ID;
import static com.tencent.devops.common.constant.audit.CodeccAuditAttributeNames.TOOL_NAME;

public interface ActionAuditRecordContents {
    String CREATE_TASK = "create task [{{" + INSTANCE_NAME + "}}]({{" + INSTANCE_ID + "}})";
    String STOP_TASK = "stop task ({{" + INSTANCE_ID + "}})";
    String DELETE_TASK = "delete task ({{" + INSTANCE_ID + "}})";

    String CREATE_CHECKER_SET = "create checker set [{{" + INSTANCE_NAME + "}}]({{" + INSTANCE_ID + "}})";
    String UPDATE_CHECKER_SET = "update checker set ({{" + INSTANCE_ID + "}}) by ({{" + INSTANCE_NAME + "}})";

    String CREATE_REGEX_RULE = "create regex rule [{{" + INSTANCE_NAME + "}}]({{" + TOOL_NAME + "}}) at project({{"
            + INSTANCE_ID + "}})";
    String UPDATE_REGEX_RULE = "update regex rule [{{" + INSTANCE_NAME + "}}] at project({{" + INSTANCE_ID + "}})";

    String REGISTER_TOOL = "register tool [{{" + INSTANCE_NAME + "}}]";

    String PROCESS_DEFECT = "process biz({{" + INSTANCE_NAME + "}}) of task({{" + TASK_ID + "}}), defect({{"
            + INSTANCE_ID + "}})";
}
