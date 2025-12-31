package com.tencent.devops.common.api;

import com.tencent.devops.common.util.PathUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 代码仓库视图
 *
 * @version V1.0
 * @date 2019/11/17
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "代码仓库信息视图")
public class CodeRepoVO {
    /**
     * 仓库ID
     */
    private String repoId;

    /**
     * 仓库url
     */
    private String url;

    /**
     * 仓库版本号(短)
     */
    private String revision;
    /**
     * 仓库版本号(长)
     */
    private String commitID;

    /**
     * 仓库分支
     */
    private String branch;

    /**
     * 仓库别名
     */
    private String aliasName;

    /**
     * 子模块列表
     */
    private List<RepoSubModuleVO> subModules;
}
