package com.tencent.devops.common.audit;

import com.tencent.bk.audit.filter.AuditPostFilter;
import com.tencent.bk.audit.model.AuditEvent;

/**
 * BK-Audit 的后置操作
 *
 * @date 2024/11/18
 */
public class CodeccAuditPostFilter implements AuditPostFilter {

    @Override
    public AuditEvent map(AuditEvent auditEvent) {
        auditEvent.setScopeType("project");
        return auditEvent;
    }
}
