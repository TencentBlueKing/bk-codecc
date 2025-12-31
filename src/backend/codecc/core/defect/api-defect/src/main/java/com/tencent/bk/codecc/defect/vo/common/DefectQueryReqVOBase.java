package com.tencent.bk.codecc.defect.vo.common;

import com.tencent.devops.common.constant.ComConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Set;
import lombok.Data;

@Data
public class DefectQueryReqVOBase {

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "规则名")
    private String checker;

    @Schema(description = "作者")
    private String author;

    @Schema(description = "严重程度：严重（1），一般（2），提示（4）", allowableValues = "{1,2,4}")
    private Set<String> severity;

    @Schema(description = "告警状态：待修复（1），已修复（2），忽略（4），路径屏蔽（8），规则屏蔽（16）", allowableValues = "{1,2,4,8,16}")
    private Set<String> status;

    /**
     * 前端已确认：列表页没有使用到该参数
     */
    @Deprecated
    private Set<String> pkgChecker;

    @Schema(description = "文件或路径列表")
    private Set<String> fileList;

    /**
     * 前端已确认：列表页没有使用到该参数
     */
    @Schema(description = "规则包名")
    @Deprecated
    private String pkgId;

    @Schema(description = "起始创建时间")
    private String startCreateTime;

    @Schema(description = "截止创建时间")
    private String endCreateTime;

    @Schema(description = "起始修复时间")
    private String startFixTime;

    @Schema(description = "截止修复时间")
    private String endFixTime;

    @Schema(description = "告警类型:新增(1),历史(2)", allowableValues = "{1,2}")
    private Set<String> defectType;

    @Schema(description = "聚类类型:文件(file),问题(defect)", allowableValues = "{file,defect}")
    private String clusterType;

    @Schema(description = "构建ID")
    private String buildId;

    @Schema(description = "构建ID")
    private String lastId;

    @Schema(description = "统计类型: 状态(STATUS), 严重程度(SEVERITY), 新旧告警(DEFECT_TYPE)")
    private String statisticType;

    @Schema(description = "CLOC聚类类型：文件（FILE），语言（LANGUAGE）")
    private ComConstants.CLOCOrder order;

    @Schema(description = "规则集列表")
    private List<CheckerSet> checkerSets;

    @Schema(description = "忽略类型")
    private Set<Integer> ignoreReasonTypes;

    // controller层包装赋值，来源Header
    private String projectId;
    private String userId;

    @Schema(description = "是否带出任务名")
    private Boolean showTaskNameCn;

    @Schema(description = "是否跨任务查询")
    private Boolean multiTaskQuery;

    @Schema(description = "忽略审核状态")
    private List<Integer> ignoreApprovalStatus;

    @Schema(description = "忽略审核ID")
    private String ignoreApprovalId;

    @Data
    public static class CheckerSet {

        private String checkerSetId;

        private int version;

        private long codeLang;
    }
}
