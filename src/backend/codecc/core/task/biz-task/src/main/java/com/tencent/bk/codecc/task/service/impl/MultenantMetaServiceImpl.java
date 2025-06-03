package com.tencent.bk.codecc.task.service.impl;

import com.tencent.bk.codecc.task.service.MetaService;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.constant.MultenantConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MetaServiceImpl 多租户版
 *
 * @date 2025/05/27
 */
@ConditionalOnProperty(name = "codecc.enableMultiTenant", havingValue = "true")
@Service
@Slf4j
public class MultenantMetaServiceImpl extends MetaServiceImpl implements MetaService {
    @Override
    public List<ToolMetaBaseVO> toolList(String tenantId, String projectId, Boolean isDetail) {
        List<ToolMetaBaseVO> beforeFilter = toolList(projectId, isDetail);

        if (beforeFilter == null || StringUtils.isBlank(tenantId)) {
            return new ArrayList<>();
        }

        return beforeFilter.stream()
                .filter(it -> (it.getTenantId() == null
                        || tenantId.equals(it.getTenantId())
                        || MultenantConstants.SYSTEM_TENANT.equals(it.getTenantId())))
                .collect(Collectors.toList());
    }

}
