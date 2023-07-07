package com.tencent.bk.codecc.defect.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_defect_migration")
@CompoundIndexes({
        @CompoundIndex(
                name = "task_id_1_tool_name_1_status_1",
                def = "{'task_id': 1, 'tool_name': 1, 'status': 1}",
                background = true
        )
})
public class CommonDefectMigrationEntity extends CommonEntity {

    @Field("task_id")
    private Long taskId;

    @Field("tool_name")
    private String toolName;

    /**
     * 迁移结果
     *
     * @see com.tencent.devops.common.constant.ComConstants.DataMigrationStatus
     */
    @Indexed(background = true)
    private Integer status;

    /**
     * 总告警数
     */
    @Field("total_count")
    private Integer totalCount;

    /**
     * 成功迁移的告警数
     */
    @Field("success_count")
    private Integer successCount;

    /**
     * 总耗时，毫秒
     */
    @Field("cost_time_ms")
    private Long costTimeMS;

    /**
     * 异常堆栈
     */
    @Field("error_stack_trace")
    private String errorStackTrace;
}
