package com.tencent.bk.codecc.task.vo;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel("任务的基本信息")
public class BatchTestResultVO extends CommonVO {

    @ApiModelProperty(value = "工具名")
    private String toolName;
    @ApiModelProperty(value = "测试版本号")
    private String version;
    @ApiModelProperty(value = "测试阶段")
    private Integer stage;
    @ApiModelProperty(value = "本测试阶段的状态")
    private Integer status;
    @ApiModelProperty(value = "用户设置的测试任务总数")
    private Integer taskCount;

    @ApiModelProperty(value = "被找到的满足条件的任务数量")
    private Integer eligibleCount;
    @ApiModelProperty(value = "成功任务数")
    private Integer successCount;
    @ApiModelProperty(value = "失败任务数")
    private Integer failCount;
    @ApiModelProperty(value = "正在执行中的任务数")
    private Integer runningCount;
    @ApiModelProperty(value = "各个任务执行耗时的总和")
    private Long costTime;
    @ApiModelProperty(value = "扫出问题总数")
    private Long defectCount;
    @ApiModelProperty(value = "扫描代码总量 (总行数, 包括空行)")
    private Long codeCount;

    /******************************************************************************************************************
     * 下面是一些前端要求的回显变量
     *****************************************************************************************************************/
    // =============== for 指定测试 ===============
    @ApiModelProperty(value = "测试项目 id")
    private String projectId;
    @ApiModelProperty(value = "测试项目名")
    private String projectName;

    // =============== for 随机测试 ===============
    @ApiModelProperty("代码库数量")
    private Integer repoCount;
    @ApiModelProperty("代码库体量属性的id")
    private String repoScaleId;

}
