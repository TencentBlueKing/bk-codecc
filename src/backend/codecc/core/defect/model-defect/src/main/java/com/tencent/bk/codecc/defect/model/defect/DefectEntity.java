package com.tencent.bk.codecc.defect.model.defect;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@EqualsAndHashCode(callSuper = true)
public class DefectEntity extends CommonEntity {
    /**
     * 任务ID
     */
    @Field("task_id")
    private long taskId;

    /**
     * 告警状态：NEW(1), FIXED(2), IGNORE(4), PATH_MASK(8), CHECKER_MASK(16);
     */
    @Field("status")
    private Integer status;
}
