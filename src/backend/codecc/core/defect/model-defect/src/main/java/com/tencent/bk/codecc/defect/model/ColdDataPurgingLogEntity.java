package com.tencent.bk.codecc.defect.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@Document(collection = "t_cold_data_purging_log")
@CompoundIndexes({
        @CompoundIndex(
                name = "task_id_1_type_1",
                def = "{'task_id': 1, 'type': 1}",
                background = true
        )
})
public class ColdDataPurgingLogEntity extends CommonEntity {

    @Field("task_id")
    private long taskId;

    private String type;

    private boolean success;

    /**
     * 数据量
     */
    @Field("data_count")
    private long dataCount;

    private String remark;

    private long cost;
}
