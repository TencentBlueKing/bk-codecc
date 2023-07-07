package com.tencent.bk.codecc.defect.model.file;

import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.Sharded;

@Data
@Document(collection = "t_scm_file_info_snapshot")
@CompoundIndexes({
        @CompoundIndex(name = "task_build_file_indx", def = "{'task_id': 1, 'build_id': 1, 'file_path': 1}",
                background = true)
})
@Sharded(shardKey = "task_id")
public class ScmFileInfoSnapshotEntity {
    @Field("task_id")
    private Long taskId;

    @Field("build_id")
    private String buildId;

    @Field("file_path")
    private String filePath;

    @Field("rel_path")
    private String relPath;

    /**
     * 文件md5
     */
    @Field("md5")
    private String md5;

    @Field("update_time")
    private long updateTime;

    /**
     * 代码库id
     */
    @Field("repo_id")
    private String repoId;

    @Field("scm_type")
    private String scmType;

    private String url;

    private String branch;

    private String revision;

    /**
     * SVN才有，是svn的根路径，而url是代码库的完整路径
     */
    @Field("root_url")
    private String rootUrl;

    /**
     * 代码库子模块
     */
    @Field("sub_module")
    private String subModule;
}
