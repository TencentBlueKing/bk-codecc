package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.core.mongorepository.CustomCheckerProjectRelationshipRepository;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.CustomCheckerProjectRelationshipEntity;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSource;
import com.tencent.bk.codecc.defect.service.CommonFilterCheckerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.devops.common.constant.ComConstants.DATA_MIGRATION_SWITCH_BATCH_MODE;
import static com.tencent.devops.common.constant.ComConstants.DATA_MIGRATION_SWITCH_SINGLE_MODE;

/**
 * common规则数据处理服务
 */
@Slf4j
@Service
public class CommonFilterCheckerServiceImpl implements CommonFilterCheckerService {

    @Autowired
    private CustomCheckerProjectRelationshipRepository customCheckerProjectRelationshipRepository;

    @Override
    public List<CheckerDetailEntity>  filterInvisibleChecker(
        List<CheckerDetailEntity> checkerDetailEntityList,
        String projectId
    ) {
        if (StringUtils.isNotBlank(projectId)) {
            // 查询项目可见的所有用户自定义规则列表
            List<CustomCheckerProjectRelationshipEntity> customCheckerProjectRelationshipEntityList =
                customCheckerProjectRelationshipRepository.findByProjectId(projectId);

            // 滤除该项目不可见的用户自定义规则
            Set<String> projectCustomCheckerNames = customCheckerProjectRelationshipEntityList.stream()
                .map(CustomCheckerProjectRelationshipEntity::getCheckerName)
                .collect(Collectors.toSet());

            return checkerDetailEntityList.stream()
                .filter(checkerDetailEntity ->
                    !CheckerSource.CUSTOM.name().equals(checkerDetailEntity.getCheckerSource())
                    || projectCustomCheckerNames.contains(checkerDetailEntity.getCheckerName()))
                .collect(Collectors.toList());
        }

        return checkerDetailEntityList;
    }


}
