package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分析记录分组查询视图类
 *
 * @version V1.0
 * @date 2019/5/17
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "分析记录代码库信息查询视图类")
public class TaskLogRepoInfoVO {
    @Schema(description = "代码库路径")
    private String repoUrl;

    @Schema(description = "代码库版本号")
    private String revision;

    @Schema(description = "提交时间")
    private String commitTime;

    @Schema(description = "提交用户")
    private String commitUser;

    @Schema(description = "分支名")
    private String branch;
}
