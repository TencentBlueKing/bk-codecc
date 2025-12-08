package com.tencent.bk.codecc.task.vo.pipeline;

import com.tencent.devops.common.api.CodeRepoVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 流水线构建信息视图
 *
 * @version V1.0
 * @date 2019/11/18
 */
@Data
@Schema(description = "流水线CodeCC原子视图")
public class PipelineBuildInfoVO
{
    @Schema(description = "代码仓库repoId列表，V2插件使用")
    private List<String> repoIds;

    @Schema(description = "本次扫描的代码仓库列表，V3插件使用")
    private List<CodeRepoVO> codeRepos;

    @Schema(description = "扫描白名单列表")
    private List<String> repoWhiteList;

    @Schema(description = "拉取代码库相对子路径")
    private List<String> repoRelativePathList;
}
