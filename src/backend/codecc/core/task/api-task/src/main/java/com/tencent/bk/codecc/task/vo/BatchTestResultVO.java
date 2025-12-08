package com.tencent.bk.codecc.task.vo;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 批量指定测试结果视图
 *
 * @date 2024/03/29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "任务的基本信息")
public class BatchTestResultVO extends CommonVO {

    @Schema(description = "工具名")
    private String toolName;
    @Schema(description = "测试版本号")
    private String version;
    @Schema(description = "测试阶段")
    private Integer stage;
    @Schema(description = "本测试阶段的状态")
    private Integer status;
    @Schema(description = "用户设置的测试任务总数")
    private Integer taskCount;

    @Schema(description = "被找到的满足条件的任务数量")
    private Integer eligibleCount;
    @Schema(description = "成功任务数")
    private Integer successCount;
    @Schema(description = "失败任务数")
    private Integer failCount;
    @Schema(description = "正在执行中的任务数")
    private Integer runningCount;
    @Schema(description = "各个任务执行耗时的总和")
    private Long costTime;
    @Schema(description = "扫出问题总数")
    private Long defectCount;
    @Schema(description = "扫描代码总量 (总行数, 包括空行)")
    private Long codeCount;

    /******************************************************************************************************************
     * 下面是一些前端要求的回显变量
     *****************************************************************************************************************/
    // =============== for 指定测试 ===============
    @Schema(description = "测试项目 id")
    private String projectId;
    @Schema(description = "测试项目名")
    private String projectName;

    // =============== for 随机测试 ===============
    @Schema(description = "代码库数量")
    private Integer repoCount;
    @Schema(description = "代码库体量属性的id")
    private String repoScaleId;

}
