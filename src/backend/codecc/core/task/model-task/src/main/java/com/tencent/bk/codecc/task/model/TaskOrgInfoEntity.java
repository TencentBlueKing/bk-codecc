package com.tencent.bk.codecc.task.model;

import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * 任务详情的用户组织架构信息
 */
@Data
public class TaskOrgInfoEntity {

    @Field("task_id")
    private Long taskId;

    /**
     * 项目拥有者
     */
    @Field("task_owner")
    private List<String> taskOwner;

    /**
     * 创建人
     */
    @Field("created_by")
    private String createdBy;

    /**
     * 事业群id
     */
    @Field("bg_id")
    private Integer bgId;

    /**
     * 部门id
     */
    @Field("dept_id")
    private Integer deptId;

    /**
     * 中心id
     */
    @Field("center_id")
    private Integer centerId;

    /**
     * 组id
     */
    @Field("group_id")
    private Integer groupId;
}
