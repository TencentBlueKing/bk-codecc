package com.tencent.bk.codecc.defect.vo.ignore;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 忽略统计详情
 *
 * @date 2022/7/15
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "忽略统计详情")
public class IgnoreStatDetail {

    public IgnoreStatDetail(Long taskId, String nameCn, String gitRepo) {
        this.taskId = taskId;
        this.nameCn = nameCn;
        this.gitRepo = gitRepo;
    }

    public IgnoreStatDetail(String name) {
        this.name = name;
    }

    @Schema(description = "任务id")
    private Long taskId;

    @Schema(description = "任务中文名")
    private String nameCn;

    @Schema(description = "代码库")
    private String gitRepo;

    @Schema(description = "跳转问题列表默认的工具维度")
    private String dimension;

    @Schema(description = "忽略问题数量")
    private Integer defectIgnoreCount;

    @Schema(description = "忽略风险函数数量")
    private Integer ccnIgnoreCount;

    @Schema(description = "告警处理人ID(忽略人)")
    private String name;

    public void addDefectIgnoreCount(int defectIgnoreCount) {
        if (null == this.defectIgnoreCount) {
            this.defectIgnoreCount = defectIgnoreCount;
        } else {
            this.defectIgnoreCount += defectIgnoreCount;
        }
    }

    public void addCcnIgnoreCount(int ccnIgnoreCount) {
        if (null == this.ccnIgnoreCount) {
            this.ccnIgnoreCount = ccnIgnoreCount;
        } else {
            this.ccnIgnoreCount += ccnIgnoreCount;
        }
    }
}
