package com.tencent.bk.codecc.defect.vo.common;

import com.tencent.devops.common.constant.ComConstants;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Set;
import lombok.Data;

@Data
public class DefectQueryReqVOBase {

    @ApiModelProperty("任务名称")
    private String taskName;

    @ApiModelProperty("规则名")
    private String checker;

    @ApiModelProperty("作者")
    private String author;

    @ApiModelProperty(value = "严重程度：严重（1），一般（2），提示（4）", allowableValues = "{1,2,4}")
    private Set<String> severity;

    @ApiModelProperty(value = "告警状态：待修复（1），已修复（2），忽略（4），路径屏蔽（8），规则屏蔽（16）", allowableValues = "{1,2,4,8,16}")
    private Set<String> status;

    /**
     * 前端已确认：列表页没有使用到该参数
     */
    @Deprecated
    private Set<String> pkgChecker;

    @ApiModelProperty(value = "文件或路径列表")
    private Set<String> fileList;

    /**
     * 前端已确认：列表页没有使用到该参数
     */
    @ApiModelProperty(value = "规则包名")
    @Deprecated
    private String pkgId;

    @ApiModelProperty(value = "起始创建时间")
    private String startCreateTime;

    @ApiModelProperty(value = "截止创建时间")
    private String endCreateTime;

    @ApiModelProperty(value = "起始修复时间")
    private String startFixTime;

    @ApiModelProperty(value = "截止修复时间")
    private String endFixTime;

    @ApiModelProperty(value = "告警类型:新增(1),历史(2)", allowableValues = "{1,2}")
    private Set<String> defectType;

    @ApiModelProperty(value = "聚类类型:文件(file),问题(defect)", allowableValues = "{file,defect}")
    private String clusterType;

    @ApiModelProperty(value = "构建ID")
    private String buildId;

    @ApiModelProperty(value = "构建ID")
    private String lastId;

    @ApiModelProperty(value = "统计类型: 状态(STATUS), 严重程度(SEVERITY), 新旧告警(DEFECT_TYPE)")
    private String statisticType;

    @ApiModelProperty(value = "CLOC聚类类型：文件（FILE），语言（LANGUAGE）")
    private ComConstants.CLOCOrder order;

    @ApiModelProperty(value = "规则集列表")
    private CheckerSet checkerSet;

    @ApiModelProperty("忽略类型")
    private Set<Integer> ignoreReasonTypes;

    // controller层包装赋值，来源Header
    private String projectId;
    private String userId;

    @ApiModelProperty("是否带出任务名")
    private Boolean showTaskNameCn;

    @ApiModelProperty("是否跨任务查询")
    private Boolean multiTaskQuery;

    @ApiModelProperty("忽略审核状态")
    private List<Integer> ignoreApprovalStatus;

    @ApiModelProperty("忽略审核ID")
    private String ignoreApprovalId;

    @Data
    public static class CheckerSet {

        private String checkerSetId;

        private int version;

        private long codeLang;
    }
}
