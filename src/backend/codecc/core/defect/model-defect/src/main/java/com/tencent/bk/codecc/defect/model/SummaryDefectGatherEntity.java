package com.tencent.bk.codecc.defect.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 告警总览的聚合实体
 * @version V2.0
 * @date 2020/5/25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_summary_defect_gather")
@CompoundIndexes({
        @CompoundIndex(name = "taskid_toolname_status_idx", def = "{'task_id': 1, 'tool_name': 1, 'status': 1}")
})
public class SummaryDefectGatherEntity extends CommonEntity {

    @Field("task_id")
    private long taskId;

    @Field("tool_name")
    private String toolName;

    @Field("defect_count")
    private Integer defectCount;

    @Field("file_count")
    private Integer fileCount;

    @Field("file_name")
    private String fileName;

    /**
     * 状态：NEW(1), FIXED(2)（修复是指低于收敛阈值）
     */
    private Integer status;


    @Field("fixed_time")
    private Long fixedTime;

}
