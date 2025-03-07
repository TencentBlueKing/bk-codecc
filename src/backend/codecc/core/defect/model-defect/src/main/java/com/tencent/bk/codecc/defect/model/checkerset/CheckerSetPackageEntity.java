package com.tencent.bk.codecc.defect.model.checkerset;


import com.tencent.codecc.common.db.OrgInfoEntity;
import com.tencent.codecc.common.db.CommonEntity;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 规则集包的详情
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
        @CompoundIndex(name = "idx_lang_value_1_type_1_checker_set_id_1",
                def = "{'lang_value': 1, 'type': 1, 'checker_set_id': 1}"),
        @CompoundIndex(name = "idx_env_type_1_type_1_lang_value_1",
                def = "{'env_type': 1, 'type': 1, 'lang_value': 1}"),
        @CompoundIndex(name = "idx_type_1_lang_value_1",
                def = "{'type': 1, 'lang_value': 1}")
})
@Document(collection = "t_checker_set_package")
public class CheckerSetPackageEntity extends CommonEntity {

    /**
     * 语言
     */
    @Field("lang")
    private String lang;

    /**
     * 语言
     */
    @Field("lang_value")
    private Long langValue;

    /**
     * 规则集包类型
     */
    @Field("type")
    private String type;

    /**
     * 环境类型
     * preProd
     * Prod
     */
    @Field("env_type")
    private String envType;

    /**
     * 规则集ID
     */
    @Field("checker_set_id")
    private String checkerSetId;

    /**
     * 规则集类型
     */
    @Field("checker_set_type")
    private String checkerSetType;

    /**
     * 可见范围
     */
    @Field("scopes")
    private List<OrgInfoEntity> scopes;

    /**
     * 可见范围-任务创建来源
     */
    @Field("task_create_from_scopes")
    private List<String> taskCreateFromScopes;

    /**
     * 版本
     */
    @Field("version")
    private Integer version;

    /**
     * 最新版本
     */
    @Field("last_version")
    private Integer lastVersion;

    /**
     * 工具列表
     */
    @Field("tool_list")
    private Set<String> toolList;
}
