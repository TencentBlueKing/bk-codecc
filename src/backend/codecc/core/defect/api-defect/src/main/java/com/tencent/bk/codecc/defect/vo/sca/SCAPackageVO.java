package com.tencent.bk.codecc.defect.vo.sca;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("SCA组件类视图")
public class SCAPackageVO {
    @ApiModelProperty("告警数据库id")
    private String entityId;

    @ApiModelProperty("任务Id")
    private long taskId;

    @ApiModelProperty("工具名")
    private String toolName;

    @ApiModelProperty("组件名称")
    private String name;

    @ApiModelProperty("包名称")
    private String packageFileName;

    @ApiModelProperty("版本")
    private String version;

    @ApiModelProperty("语言")
    private String language;

    @ApiModelProperty("告警作者")
    private List<String> author;

    @ApiModelProperty("依赖方式：是否直接依赖")
    private boolean direct;

    @ApiModelProperty("告警行的变更时间")
    private long lastUpdateTime;

    @ApiModelProperty("首次发现构建号")
    private String createBuildNumber;

    @ApiModelProperty("风险等级")
    private int severity;

    @ApiModelProperty("严重漏洞数")
    private int seriousCount;

    @ApiModelProperty("高危漏洞数")
    private int highCount;

    // 目前仅定义3中严重等级
//    @ApiModelProperty("中危漏洞数")
//    private int middleCount;

    @ApiModelProperty("低危漏洞数")
    private int lowCount;

    /**
     * 告警状态：NEW(1), FIXED(2), IGNORE(4), PATH_MASK(8), CHECKER_MASK(16);
     */
    @ApiModelProperty("告警状态")
    private int status;

    @ApiModelProperty("许可证列表")
    private List<SCALicenseVO> licenseList;

    @ApiModelProperty("告警忽略时间")
    private Long ignoreTime;

    @ApiModelProperty("告警忽略原因类型")
    private Integer ignoreReasonType;

    @ApiModelProperty("告警忽略原因")
    private String ignoreReason;

    @ApiModelProperty("告警忽略操作人")
    private String ignoreAuthor;

    @ApiModelProperty(value = "标记了，但是再次扫描没有修复")
    private Boolean markButNoFixed;

    /**
     * 告警是否被标记为已修改的标志，checkbox for developer, 0 is normal, 1 is tag, 2 is prompt
     */
    @ApiModelProperty(value = "告警是否被标记为已修改的标志")
    private Integer mark;

    @ApiModelProperty(value = "告警被标记为已修改的时间")
    private Long markTime;


//    @ApiModelProperty(value = "作者清单/处理人")
//    private Set<List<String>> authorList;
//
//    /**
//     * 告警行的变更时间，用于跟新旧告警的判断时间做对比
//     */
//    @ApiModelProperty("告警行的代码提交时间")
//    private long lineUpdateTime;
//

//
//    /**
//     * 发现该告警的最近分析版本号，项目工具每次分析都有一个版本，用于区分一个方法是哪个版本扫描出来的，根据版本号来判断是否修复
//     */
//    @ApiModelProperty("发现该告警的最近分析版本号")
//    private String analysisVersion;
//

}
