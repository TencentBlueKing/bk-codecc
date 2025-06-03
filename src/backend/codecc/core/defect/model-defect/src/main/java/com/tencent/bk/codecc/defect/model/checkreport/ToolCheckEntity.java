package com.tencent.bk.codecc.defect.model.checkreport;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_tool_check")
@CompoundIndexes({
        @CompoundIndex(
                name = "start_type_1_date_1_build_id_1",
                def = "{'start_type': 1, 'date': 1, 'build_id': 1}",
                background = true
        )
})
public class ToolCheckEntity extends CommonEntity {

  /**
   * 构建编号
   */
  @Field("build_id")
  @Indexed(background = true)
  private String buildId;

  /**
   * 工具名称
   */
  @Field("tool_name")
  private String toolName;

  /**
   * 启动类型: TIME_TRIGGER 或 MANUAL
   */
  @Field("start_type")
  private String startType;

  /**
   * 任务数
   */
  @Field("task_num")
  private int taskNum;

  /**
   * 任务数相比昨日变化率
   */
  @Field("task_trend")
  private double taskTrend;

  /**
   * 任务相比昨日变化值
   */
  @Field("task_change_num")
  private int taskChangeNum;

  /**
   * 告警数
   */
  @Field("defect_num")
  private long defectNum;

  /**
   * 告警数相比昨日变化值
   */
  @Field("defect_change_num")
  private long defectChangeNum;

  /**
   * 告警数变化
   */
  @Field("defect_num_change")
  private long defectNumChange;

  /**
   * 总告警数相对昨日变化率
   */
  @Field("defect_trend")
  private double defectTrend;

  /**
   * 严重缺陷数
   */
  @Field("serious_defect")
  private long seriousDefect;

  /**
   * 普通缺陷数
   */
  @Field("normal_defect")
  private long normalDefect;

  /**
   * 提示缺陷数
   */
  @Field("prompt_defect")
  private long promptDefect;

  /**
   * 创建时间，通常以时间戳表示
   */
  @Field("create_time")
  private long createTime;

  /**
   * 日期，格式为 YYYY-MM-DD
   */
  @Field("date")
  private String date;
}
