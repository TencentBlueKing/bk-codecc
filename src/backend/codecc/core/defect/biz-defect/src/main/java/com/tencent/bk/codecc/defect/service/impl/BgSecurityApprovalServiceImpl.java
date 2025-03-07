package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.core.mongorepository.BgApprovalConfigRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.BgSecurityApprovalDao;
import com.tencent.bk.codecc.defect.model.ignore.BgSecurityApproverEntity;
import com.tencent.bk.codecc.defect.service.BgSecurityApprovalService;
import com.tencent.bk.codecc.defect.vo.ignore.BgSecurityApprovalVO;
import com.tencent.codecc.common.db.OrgInfoEntity;
import com.tencent.devops.common.api.OrgInfoVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.util.BeanUtils;
import io.micrometer.core.instrument.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BgSecurityApprovalServiceImpl implements BgSecurityApprovalService {
    @Autowired
    private BgSecurityApprovalDao bgSecurityApprovalDao;
    @Autowired
    private BgApprovalConfigRepository bgApprovalConfigRepository;

    private static final Logger logger = LoggerFactory.getLogger(BgSecurityApprovalServiceImpl.class);

    @Override
    public List<BgSecurityApprovalVO> bgSecurityApprovalList(String projectScopeType, OrgInfoVO orgInfo) {

        List<BgSecurityApproverEntity> approverList =
                bgSecurityApprovalDao.findByProjectScopeTypeAndOrgInfo(projectScopeType, orgInfo);
        if (approverList == null || approverList.isEmpty()) {
            logger.info("approverList find is null or empty");
            return Collections.emptyList();
        }
        return approverList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public Boolean upsertBgApprovalConfig(BgSecurityApprovalVO reqVO) {
        logger.info("upsertBgApprovalConfig reqVO: {}", JsonUtil.INSTANCE.toJson(reqVO));
        if (reqVO == null || reqVO.getOrgInfo() == null || reqVO.getOrgInfo().getBgId() == null
                || StringUtils.isBlank(reqVO.getProjectScopeType()) || CollectionUtils.isEmpty(reqVO.getApprovers())) {
            logger.error("reqVO param valid fail!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL);
        }
        BgSecurityApproverEntity bgSecurityApproverEntity =
                bgApprovalConfigRepository.findByEntityId(reqVO.getEntityId());
        if (bgSecurityApproverEntity == null) {
            bgSecurityApproverEntity = new BgSecurityApproverEntity();
            bgSecurityApproverEntity.applyAuditInfoOnCreate(reqVO.getUpdatedBy());
        } else {
            bgSecurityApproverEntity.applyAuditInfoOnUpdate(reqVO.getUpdatedBy());
        }
        OrgInfoEntity orgInfoEntity = new OrgInfoEntity();
        BeanUtils.copyProperties(reqVO.getOrgInfo(), orgInfoEntity);
        bgSecurityApproverEntity.setOrgInfoEntity(orgInfoEntity);
        bgSecurityApproverEntity.setProjectScopeType(reqVO.getProjectScopeType());
        bgSecurityApproverEntity.setApprovers(reqVO.getApprovers());
        bgApprovalConfigRepository.save(bgSecurityApproverEntity);
        logger.info("upsertBgApprovalConfig finished!");
        return true;
    }

    @Override
    public Boolean deleteBgApprovalConfig(String entityId, String userId) {
        logger.info("deleteIgnoreApprovalConfig entityId: {}, userId: {}", entityId, userId);

        if (StringUtils.isBlank(entityId)) {
            logger.warn("entityId is blank! delete failed!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL);
        }
        BgSecurityApproverEntity bgSecurityApproverEntity = bgApprovalConfigRepository.findByEntityId(entityId);
        if (bgSecurityApproverEntity == null) {
            logger.warn("bgSecurityApproverEntity not find! delete failed!");
            String errorMsg = "审批人配置未找到";
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, errorMsg);
        }
        logger.info("deleteBgApprovalConfig entityId: {}, userId: {}", entityId, userId);
        bgApprovalConfigRepository.delete(bgSecurityApproverEntity);
        return true;
    }

    private BgSecurityApprovalVO convertToVO(BgSecurityApproverEntity entity) {
        BgSecurityApprovalVO vo = new BgSecurityApprovalVO();
        OrgInfoVO orgInfo = new OrgInfoVO();
        BeanUtils.copyProperties(entity.getOrgInfoEntity(), orgInfo);
        vo.setOrgInfo(orgInfo);
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
