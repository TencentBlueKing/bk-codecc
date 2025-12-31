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
@Document(collection = "t_language_check")
@CompoundIndexes({
        @CompoundIndex(
                name = "start_type_1_date_1_build_id_1",
                def = "{'start_type': 1, 'date': 1, 'build_id': 1}",
                background = true
        )
})
public class LanguageCheckEntity extends CommonEntity {

    /**
     * 创建时间戳
     */
    @Field("create_time")
    private Long createTime;

    /**
     * 构建编号
     */
    @Field("build_id")
    @Indexed(background = true)
    private String buildId;

    /**
     * 启动类型: TIME_TRIGGER 或 MANUAL
     */
    @Field("start_type")
    private String startType;

    /**
     * 语言名称
     */
    @Field("language")
    private String language;

    /**
     * 执行任务总数
     */
    @Field("task_num")
    private int taskNum;

    /**
     * 任务数相比昨日变化比率
     */
    @Field("task_trend")
    private double taskTrend;

    /**
     * 代码行数
     */
    @Field("code_lines")
    private Long codeLines;

    /**
     * 空白行数
     */
    @Field("blank_lines")
    private Long blankLines;

    /**
     * 注释行数
     */
    @Field("comment_lines")
    private Long commentLines;

    /**
     * 代码行数相对昨日变化比率
     */
    @Field("code_trend")
    private double codeTrend;

    /**
     * 空白行数相比昨日变化比率
     */
    @Field("blank_trend")
    private double blankTrend;

    /**
     * 注释行数相比昨日变化比率
     */
    @Field("comment_trend")
    private double commentTrend;

    /**
     * 日期，格式为 YYYY-MM-DD
     */
    @Field("date")
    private String date;
}
