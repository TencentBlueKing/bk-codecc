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
@Document("t_bg_check")
@CompoundIndexes({
        @CompoundIndex(
                name = "start_type_1_date_1_build_id_1",
                def = "{'start_type': 1, 'date': 1, 'build_id': 1}",
                background = true
        )
})
public class BgCheckEntity extends CommonEntity {

  /**
   * 启动类型: TIME_TRIGGER 或 MANUAL
   */
  @Field("start_type")
  private String startType;

  /**
   * 事业群标识
   */
  @Field("bg")
  private int bg;

  /**
   * 日期，格式为 YYYY-MM-DD
   */
  @Field("date")
  private String date;

  /**
   * 构建号
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
   * 任务数
   */
  @Field("task_num")
  private long taskNum;

  /**
   * 任务数相比昨日的变化率
   */
  @Field("task_trend")
  private double taskTrend;

  /**
   * 代码缺陷问题数
   */
  @Field("defect_num")
  private long defectNum;

  /**
   * 代码缺陷相比昨日的变化率
   */
  @Field("defect_trend")
  private double defectTrend;

  /**
   * 安全漏洞问题数
   */
  @Field("safety_defect")
  private long safetyDefect;

  /**
   * 安全漏洞相比昨日的变化率
   */
  @Field("safety_defect_trend")
  private double safetyDefectTrend;

  /**
   * 代码规范问题数
   */
  @Field("standard_defect")
  private long standardDefect;

  /**
   * 代码规范问题数相比昨日的变化率
   */
  @Field("standard_defect_trend")
  private double standardDefectTrend;

  /**
   * 圈复杂度风险函数个数
   */
  @Field("ccn_defect")
  private long ccnDefect;

  /**
   * 圈复杂度风险函数变化率
   */
  @Field("ccn_defect_trend")
  private double ccnDefectTrend;

  /**
   * 重复率风险函数个数
   */
  @Field("dupc_defect")
  private long dupcDefect;

  /**
   * 重复率风险函数相比昨日的变化率
   */
  @Field("dupc_defect_trend")
  private double dupcDefectTrend;
}
