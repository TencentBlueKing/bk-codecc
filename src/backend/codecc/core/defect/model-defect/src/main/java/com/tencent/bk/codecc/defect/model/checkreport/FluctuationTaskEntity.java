package com.tencent.bk.codecc.defect.model.checkreport;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;


@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Document("t_fluctuation_task")
public class FluctuationTaskEntity extends CommonEntity {

    /**
     * language_id
     */
    @Field("fk")
    private String fk;

    /**
     * 任务编号
     */
    @Field("task_id")
    @Indexed(background = true)
    private long taskId;
}
