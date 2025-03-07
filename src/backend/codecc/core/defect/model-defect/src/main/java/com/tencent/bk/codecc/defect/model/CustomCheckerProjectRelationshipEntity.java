package com.tencent.bk.codecc.defect.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_custom_checker_project_relationship")
@CompoundIndexes({
    @CompoundIndex(name = "checker_name_1_tool_name_1", def = "{'checker_name': 1, 'tool_name': 1}")
})
public class CustomCheckerProjectRelationshipEntity extends CommonEntity {
    /**
     * 自定义规则名
     */
    @Field("checker_name")
    @Indexed
    private String checkerName;

    /**
     * 关联的项目ID
     */
    @Field("project_id")
    @Indexed
    private String projectId;

    /**
     * 工具名
     */
    @Field("tool_name")
    @Indexed
    private String toolName;
}
