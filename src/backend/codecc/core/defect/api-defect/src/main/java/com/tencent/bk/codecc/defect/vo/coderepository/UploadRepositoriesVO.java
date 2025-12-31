package com.tencent.bk.codecc.defect.vo.coderepository;

import com.tencent.devops.common.api.CodeRepoVO;
import com.tencent.devops.common.api.CommonVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 代码仓库信息视图
 *
 * @version V1.0
 * @date 2019/11/15
 */
@Data
@Schema(description = "代码仓库信息视图")
public class UploadRepositoriesVO extends CommonVO
{
    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 本次启动的工具名称列表
     */
    private String toolName;

    /**
     * 项目ID
     */
    private String projectId;

    /**
     * 构建ID
     */
    private String buildId;

    /**
     * 仓库列表
     */
    private List<CodeRepoVO> repoList;

    /**
     * 删除的文件列表
     */
    private List<String> deleteFiles;

    /**
     * 根路径列表
     */
    private Set<String> rootPaths;

    /**
     * 本次启动的工具列表
     */
    private List<String> triggerToolNames;

    /**
     * 扫描目录白名单
     */
    private List<String> repoWhiteList;

    /**
     * 拉取仓库子路径
     */
    private List<String> repoRelativePathList;
}
