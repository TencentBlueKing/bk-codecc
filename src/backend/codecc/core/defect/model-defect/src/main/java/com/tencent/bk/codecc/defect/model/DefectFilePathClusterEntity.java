package com.tencent.bk.codecc.defect.model;

import com.tencent.codecc.common.db.CommonEntity;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.Sharded;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_defect_file_path_cluster")
@CompoundIndexes({
        @CompoundIndex(name = "task_id_1_tool_name_1_build_id_1_status_1",
                def = "{'task_id': 1, 'tool_name': 1, 'build_id': 1, 'status': 1}", background = true)
})
@Sharded(shardKey = "task_id")
public class DefectFilePathClusterEntity extends CommonEntity {

    /**
     * 任务ID
     */
    @Field("task_id")
    private Long taskId;

    /**
     * 工具
     */
    @Field("tool_name")
    private String toolName;

    /**
     * 构建ID
     */
    @Field("build_id")
    private String buildId;

    /**
     * 绝对路径
     */
    @Field("file_path")
    private String filePath;

    /**
     * 相对路径
     */
    @Field("rel_path")
    private String relPath;

    /**
     * 状态
     */
    @Field("status")
    private Integer status;

    /**
     * 记录创建时间 （方便删除）
     */
    @Field("create_at")
    private Date createAt;
}
