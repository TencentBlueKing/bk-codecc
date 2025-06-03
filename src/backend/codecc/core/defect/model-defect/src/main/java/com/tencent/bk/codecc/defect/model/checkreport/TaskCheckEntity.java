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
@Document(collection = "t_task_check")
@CompoundIndexes({
        @CompoundIndex(
                name = "start_type_1_date_1_build_id_1",
                def = "{'start_type': 1, 'date': 1, 'build_id': 1}",
                background = true
        )
})
public class TaskCheckEntity extends CommonEntity {

  /**
   * 启动类型: TIME_TRIGGER 或 MANUAL
   */
  @Field("start_type")
  private String startType;

  /**
   * 构建编号
   */
  @Field("build_id")
  @Indexed(background = true)
  private String buildId;

  /**
   * 日期，格式为 YYYY-MM-DD
   */
  @Field("date")
  private String date;

  /**
   * 开始时间，通常以时间戳表示
   */
  @Field("start_time")
  private long startTime;

  /**
   * 工蜂同步任务数
   */
  @Field("task_num")
  private long taskNum;

  /**
   * 停用任务数 -- 删除或变为私有
   */
  @Field("delete_private_num")
  private long deletePrivateNum;

  /**
   * 归档任务数
   */
  @Field("archive_num")
  private long archiveNum;

  /**
   * 不可克隆任务数
   */
  @Field("no_clone_num")
  private long noCloneNum;

  /**
   * 无提交记录任务数
   */
  @Field("no_commit_num")
  private long noCommitNum;

  /**
   * Owner 不规范（无 Owner 或 Owner 为机器人）任务数
   */
  @Field("owner_problem_num")
  private long ownerProblemNum;

  /**
   * 没有工蜂统计信息（Owner 或创建人已离职）任务数
   */
  @Field("no_gongfeng_stat_num")
  private long noGongfengStatNum;

  /**
   * 可扫描任务数
   */
  @Field("scan_able_num")
  private long scanAbleNum;

  /**
   * 已执行任务数
   */
  @Field("scaned_num")
  private int scanedNum;

  /**
   * 工蜂同步任务数相比昨日的趋势
   */
  @Field("task_num_trend")
  private double taskNumTrend;

  /**
   * 停用任务数 -- 删除或变为私有 相比昨日的趋势
   */
  @Field("delete_private_num_trend")
  private double deletePrivateNumTrend;

  /**
   * 归档任务数相比昨日的趋势
   */
  @Field("archive_num_trend")
  private double archiveNumTrend;

  /**
   * 不可克隆任务数相比昨日的趋势
   */
  @Field("no_clone_num_trend")
  private double noCloneNumTrend;

  /**
   * 无提交记录任务数相比昨日的趋势
   */
  @Field("no_commit_num_trend")
  private double noCommitNumTrend;

  /**
   * Owner 不规范（无 Owner 或 Owner 为机器人）任务数相比昨日的趋势
   */
  @Field("owner_problem_num_trend")
  private double ownerProblemNumTrend;

  /**
   * 没有工蜂统计信息（Owner 或创建人已离职）任务数相比昨日的趋势
   */
  @Field("no_gongfeng_stat_num_trend")
  private double noGongfengStatNumTrend;

  /**
   * 可扫描任务数相比昨日的趋势
   */
  @Field("scan_able_num_trend")
  private double scanAbleNumTrend;

  /**
   * 已执行任务数相比昨日的趋势
   */
  @Field("scaned_num_trend")
  private double scanedNumTrend;
}
