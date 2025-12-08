package com.tencent.bk.codecc.defect.vo.sca;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "SCA组件类视图")
public class SCAPackageVO {
    @Schema(description = "告警数据库id")
    private String entityId;

    @Schema(description = "任务Id")
    private long taskId;

    @Schema(description = "工具名")
    private String toolName;

    @Schema(description = "组件名称")
    private String name;

    @Schema(description = "包名称")
    private String packageFileName;

    @Schema(description = "版本")
    private String version;

    @Schema(description = "语言")
    private String language;

    @Schema(description = "告警作者")
    private List<String> author;

    @Schema(description = "依赖方式：是否直接依赖")
    private boolean direct;

    @Schema(description = "告警行的变更时间")
    private long lastUpdateTime;

    @Schema(description = "首次发现构建号")
    private String createBuildNumber;

    @Schema(description = "风险等级")
    private int severity;

    @Schema(description = "高危漏洞数")
    private int highCount;

    @Schema(description = "中危漏洞数")
    private int middleCount;

    @Schema(description = "低危漏洞数")
    private int lowCount;

    @Schema(description = "低危漏洞数")
    private int unknownCount;

    /**
     * 告警状态：NEW(1), FIXED(2), IGNORE(4), PATH_MASK(8), CHECKER_MASK(16);
     */
    @Schema(description = "告警状态")
    private int status;

    @Schema(description = "许可证列表")
    private List<SCALicenseVO> licenseList;

    @Schema(description = "告警忽略时间")
    private Long ignoreTime;

    @Schema(description = "告警忽略原因类型")
    private Integer ignoreReasonType;

    @Schema(description = "告警忽略原因")
    private String ignoreReason;

    @Schema(description = "告警忽略操作人")
    private String ignoreAuthor;

    @Schema(description = "标记了，但是再次扫描没有修复")
    private Boolean markButNoFixed;

    /**
     * 告警是否被标记为已修改的标志，checkbox for developer, 0 is normal, 1 is tag, 2 is prompt
     */
    @Schema(description = "告警是否被标记为已修改的标志")
    private Integer mark;

    @Schema(description = "告警被标记为已修改的时间")
    private Long markTime;


//    @Schema(description = "作者清单/处理人")
//    private Set<List<String>> authorList;
//
//    /**
//     * 告警行的变更时间，用于跟新旧告警的判断时间做对比
//     */
//    @Schema(description = "告警行的代码提交时间")
//    private long lineUpdateTime;
//

//
//    /**
//     * 发现该告警的最近分析版本号，项目工具每次分析都有一个版本，用于区分一个方法是哪个版本扫描出来的，根据版本号来判断是否修复
//     */
//    @Schema(description = "发现该告警的最近分析版本号")
//    private String analysisVersion;
//

}
