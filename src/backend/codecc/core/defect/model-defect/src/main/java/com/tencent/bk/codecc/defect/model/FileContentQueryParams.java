package com.tencent.bk.codecc.defect.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 文件内容查询条件
 */
@Data
@AllArgsConstructor
public class FileContentQueryParams {

    /**
     * 任务ID
     */
    private long taskId;

    /**
     * 项目ID
     */
    private String projectId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 仓库URL
     */
    private String url;

    /**
     * 仓库ID
     */
    private String repoId;

    /**
     * 文件相对路径
     */
    private String relPath;

    /**
     * 文件绝对路径
     */
    private String filePath;

    /**
     * 版本号
     */
    private String revision;

    /**
     * 分支
     */
    private String branch;

    /**
     * 子模块
     */
    private String subModule;

    /**
     * 构建ID
     */
    private String buildId;

    /**
     * 是否要 "尽最大努力" 查看私有库代码
     */
    private boolean tryBestForPrivate = true;

    public static FileContentQueryParams queryParams(long taskId, String projectId, String userId, String url,
            String repoId, String relPath, String filePath, String revision, String branch, String subModule,
            String buildId) {
        return new FileContentQueryParams(taskId, projectId, userId, url, repoId, relPath, filePath, revision, branch,
                subModule, buildId, true);
    }

}
