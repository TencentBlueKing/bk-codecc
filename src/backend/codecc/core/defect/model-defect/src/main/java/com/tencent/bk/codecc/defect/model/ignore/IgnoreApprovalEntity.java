package com.tencent.bk.codecc.defect.model.ignore;

import com.tencent.codecc.common.db.CommonEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_ignore_approval")
@CompoundIndexes({
        @CompoundIndex(name = "idx_status_1_next_check_status_time_1",
                def = "{'status': 1,'next_check_status_time': 1}", background = true),
})
public class IgnoreApprovalEntity extends CommonEntity {


    /**
     * 审核配置ID
     */
    @Field("approval_config_id")
    private String approvalConfigId;

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
    @Field("ignore_type_id")
    private Integer ignoreTypeId;

    /**
     * 忽略类型
     */
    @Field("ignore_reason")
    private String ignoreReason;

    /**
     * 项目ID
     */
    @Field("project_id")
    private String projectId;


    /**
     * 任务ID列表
     */
    @Field("task_id_list")
    private List<Long> taskIds;


    /**
     * 忽略类型
     */
    @Field("ignore_author")
    private String ignoreAuthor;

    /**
     * 审批人,
     * 项目管理员
     * 任务管理员
     * 规则发布者
     * 忽略人LEADER
     * 自定义
     */
    @Field("approver_type")
    private String approverType;

    /**
     * 自定义审批人列表
     */
    @Field("approvers")
    private List<String> approvers;

    /**
     * 审批人 - 真实审批人
     */
    @Field("approver")
    private String approver;

    /**
     * 审批单号
     */
    @Field("itsm_sn")
    private String itsmSn;

    /**
     * 审批单号
     */
    @Field("itsm_id")
    private Long itsmId;

    /**
     * 审批详情URL
     */
    @Field("itsm_url")
    private String itsmUrl;

    /**
     * 审批状态
     */
    @Field("status")
    private Integer status;

    /**
     * 创建单据状态
     */
    @Field("create_ticket_status")
    private Integer createTicketStatus;


    /**
     * 创建单据错误信息
     */
    @Field("create_ticket_msg")
    private String createTicketMsg;

    /**
     * 告警数量
     */
    @Field("defect_count")
    private Long defectCount;

    /**
     * 下次检查状态的时间
     */
    @Field("next_check_status_time")
    private Long nextCheckStatusTime;
}
