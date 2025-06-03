package com.tencent.bk.codecc.defect.model.checkreport;

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
@Document(collection = "t_project_check")
@CompoundIndexes({
        @CompoundIndex(
                name = "start_type_1_date_1_build_id_1",
                def = "{'start_type': 1, 'date': 1, 'build_id': 1}",
                background = true
        )
})
public class ProjectCheckEntity extends CommonEntity {

    /**
     * 日期，格式为 YYYY-MM-DD
     */
    @Field("date")
    private String date;

    /**
     * 构建编号
     */
    @Field("build_id")
    @Indexed(background = true)
    private String buildId;

    /**
     * 创建时间，通常以时间戳表示
     */
    @Field("create_time")
    private long createTime;

    /**
     * 启动类型: TIME_TRIGGER 或 MANUAL
     */
    @Field("start_type")
    private String startType;

    /**
     * 代码行数范围
     * 0: 0~10万行,
     * 1: 10~50万行,
     * ...
     */
    private int grade;

    /**
     * 任务数
     */
    @Field("task_num")
    private long taskNum;

    /**
     * 任务数相比昨日的变化率
     */
    @Field("task_trend")
    private double taskTrend;
}
