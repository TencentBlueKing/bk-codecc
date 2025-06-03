package com.tencent.bk.codecc.task.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * VarOption 的数据库视图
 *
 * @date 2025/02/20
 */
@Data
public class VarOptionEntity {
    // 选项 id
    @Field("option_id")
    private String id;
    // 选项展示名
    @Field("option_name")
    private String name;
}
