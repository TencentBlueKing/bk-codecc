package com.tencent.bk.codecc.defect.model.ignore;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_ignore_approval_config")
@CompoundIndexes({
        @CompoundIndex(name = "idx_project_scope_type_1_status_1_project_id_1",
                def = "{'project_scope_type': 1,'status': 1,'project_id': 1}", background = true),
})
public class IgnoreApprovalConfigEntity extends CommonEntity {

    /**
     * 名字
     */
    @Field("name")
    private String name;

    /**
     * 问题维度
     */
    @Field("dimensions")
    private List<String> dimensions;

    /**
     * 问题级别
     */
    @Field("severities")
    private List<Integer> severities;

    /**
     * 忽略类型
     */
    @Field("ignore_type_ids")
    private List<Integer> ignoreTypeIds;

    /**
     * 问题创建时间
     */
    @Field("defect_create_time")
    private Long defectCreateTime;

    /**
     * 项目范围类型, SINGLE, 多项目范围类型
     */
    @Field("project_scope_type")
    private String projectScopeType;

    /**
     * 项目ID，当project = SINGLE是生效
     */
    @Field("project_id")
    private String projectId;

    /**
     * 限制项目ID，当project != SINGLE是生效, 不配置表示生效所有
     */
    @Field("limited_project_id")
    private List<String> limitedProjectIds;

    /**
     * 任务范围类型, ALL,INCLUDE,EXCLUDE，当project = SINGLE是生效
     */
    @Field("task_scope_type")
    private String taskScopeType;

    /**
     * 任务范围INCLUDE,EXCLUDE下，选择的任务列表，当project = SINGLE是生效
     */
    @Field("task_scope_list")
    private List<Long> taskScopeList;

    /**
     * 审批人,
     * 项目管理员
     * 任务管理员
     * 规则发布者
     * 忽略人LEADER
     * 自定义
     */
    @Field("approver_types")
    private List<String> approverTypes;

    /**
     * 自定义审批人列表
     */
    @Field("custom_approver")
    private List<String> customApprovers;

    /**
     * 状态
     */
    @Field("status")
    private Integer status;
}
