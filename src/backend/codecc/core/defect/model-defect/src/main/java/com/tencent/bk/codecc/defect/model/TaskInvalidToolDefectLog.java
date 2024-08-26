package com.tencent.bk.codecc.defect.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 任务失效的工具的告警屏蔽记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@CompoundIndexes({
        @CompoundIndex(name = "task_id_1_build_id_1_tool_name_1",
                def = "{'task_id': 1, 'build_id': 1, 'tool_name': 1}"),
        @CompoundIndex(name = "task_id_1_tool_name_1_create_date_1",
                def = "{'task_id': 1, 'tool_name': 1, 'create_date': 1}")
})
@Document(collection = "t_task_invalid_tool_defect_log")
public class TaskInvalidToolDefectLog extends CommonEntity {

    @Field("task_id")
    private Long taskId;

    @Field("build_id")
    private String buildId;

    @Field("tool_name")
    private String toolName;

    /**
     * 屏蔽的告警数量
     */
    @Field("defect_count")
    private Long defectCount;

}
