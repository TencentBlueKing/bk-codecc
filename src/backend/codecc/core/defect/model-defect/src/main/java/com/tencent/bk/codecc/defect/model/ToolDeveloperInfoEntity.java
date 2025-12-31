package com.tencent.bk.codecc.defect.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Set;

/**
 * 记录特定工具的权限开发者名单
 *
 * @date 2024/01/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_tool_developer_info")
public class ToolDeveloperInfoEntity extends CommonEntity {

    @Field("tool_name")
    @Indexed(unique = true)
    private String toolName;

    @Field("developers")
    private Set<String> developers;

    @Field("owners")
    private Set<String> owners;

    @Field("masters")
    private Set<String> masters;

}
