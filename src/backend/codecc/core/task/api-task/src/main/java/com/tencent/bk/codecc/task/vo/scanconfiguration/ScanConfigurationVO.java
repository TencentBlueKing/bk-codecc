package com.tencent.bk.codecc.task.vo.scanconfiguration;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 扫描触发配置视图
 *
 * @version V4.0
 * @date 2019/11/8
 */
@Data
@Schema(description = "扫描触发配置视图")
public class ScanConfigurationVO extends CommonVO
{
    /**
     * 定时扫描配置
     */
    private TimeAnalysisConfigVO timeAnalysisConfig;

    /**
     * 1：增量；0：全量; 2: diff模式
     */
    private Integer scanType;

    /**
     * 告警作者转换配置
     */
    private List<TransferAuthorPair> transferAuthorList;

    /*
     * 是否回写工蜂
     */
    private Boolean mrCommentEnable;

    /**
     * 是否允许页面忽略告警
     */
    private Boolean prohibitIgnore;

    /**
     * 项目编译脚本
     */
    private String compileCommand;

    @Data
    public static class TransferAuthorPair
    {
        private String sourceAuthor;
        private String targetAuthor;
    }
}
