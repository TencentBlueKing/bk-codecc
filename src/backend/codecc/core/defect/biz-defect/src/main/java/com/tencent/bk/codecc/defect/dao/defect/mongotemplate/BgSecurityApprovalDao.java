package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.tencent.bk.codecc.defect.model.ignore.BgSecurityApproverEntity;
import com.tencent.devops.common.api.OrgInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.tencent.devops.common.constant.ComConstants.DEFAULT_BG_ID;

@Slf4j
@Repository
public class BgSecurityApprovalDao {
    @Autowired
    private MongoTemplate defectMongoTemplate;

    /**
     * 根据项目类型和组织架构查询BG安全管理员
     * @param projectScopeType 项目类型
     * @param orgInfo 组织架构
     * @return list
     */
    public List<BgSecurityApproverEntity> findByProjectScopeTypeAndOrgInfo(String projectScopeType, OrgInfoVO orgInfo) {
        Criteria criteria = new Criteria();
        if (StringUtils.isNotBlank(projectScopeType)) {
            criteria.and("projectScopeType").is(projectScopeType);
        }
        // 添加组织信息的筛选条件
        if (orgInfo != null) {
            if (orgInfo.getBgId() != null && orgInfo.getBgId() > 0) {
                criteria.and("org.bg_id").is(orgInfo.getBgId());
            }
            if (orgInfo.getBusinessLineId() != null && orgInfo.getBusinessLineId() > 0) {
                criteria.and("org.business_line_id").is(orgInfo.getBusinessLineId());
            }
            if (orgInfo.getDeptId() != null && orgInfo.getDeptId() > 0) {
                criteria.and("org.dept_id").is(orgInfo.getDeptId());
            }
            if (orgInfo.getCenterId() != null && orgInfo.getCenterId() > 0) {
                criteria.and("org.center_id").is(orgInfo.getCenterId());
            }
            if (orgInfo.getGroupId() != null && orgInfo.getGroupId() > 0) {
                criteria.and("org.group_id").is(orgInfo.getGroupId());
            }
        }

        return defectMongoTemplate.find(Query.query(criteria), BgSecurityApproverEntity.class);
    }
}
