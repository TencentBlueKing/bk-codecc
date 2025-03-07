package com.tencent.bk.codecc.defect.model.ignore;

import com.tencent.codecc.common.db.CommonEntity;
import com.tencent.codecc.common.db.OrgInfoEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_bg_security_approver")
public class BgSecurityApproverEntity extends CommonEntity {


    /**
     * 项目范围类型, SINGLE, 多项目范围类型
     */
    @Indexed(background = true)
    @Field("project_scope_type")
    private String projectScopeType;


    /**
     * 组织信息
     */
    @Field("org")
    private OrgInfoEntity orgInfoEntity;

    /**
     * 审批人列表
     */
    @Field("approvers")
    private List<String> approvers;
}
