package com.tencent.bk.codecc.defect.model.ignore;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_ignore_type_project_config")
@CompoundIndexes({
        @CompoundIndex(name = "idx_ignore_type_id_1", def = "{'ignore_type_id': 1}", background = true),
        @CompoundIndex(name = "idx_ignore_type_id_1_name_1",
                def = "{'ignore_type_id': 1, 'name': 1}", background = true),
        @CompoundIndex(name = "idx_project_id_1_ignore_type_id_1",
                def = "{'project_id': 1, 'ignore_type_id': 1}", background = true),
        @CompoundIndex(name = "idx_project_id_1_status_1", def = "{'project_id': 1, 'status': 1}", background = true),
        @CompoundIndex(name = "idx_project_id_1_name_1", def = "{'project_id': 1, 'name': 1}", background = true)
})
public class IgnoreTypeProjectConfig  extends CommonEntity {

    /**
     * 名字
     */
    @Field("name")
    private String name;
    /**
     * 忽略类型的ID
     */
    @Field("ignore_type_id")
    private Integer ignoreTypeId;
    /**
     * 创建来源 codecc ， project
     */
    @Field("create_from")
    private String createFrom;
    /**
     * 创建来源为 project 时,有值
     */
    @Field("project_id")
    private String projectId;
    /**
     * 状态： 0启用，1不启用
     */
    @Field("status")
    private Integer status;
    /**
     * 通知启用状态： 0启用，1不启用
     */
    @Field("notify_status")
    private Integer notifyStatus;
    /**
     * 通知相关配置
     */
    @Field("notify")
    private IgnoreTypeNotifyEntity notify;

    public Boolean hasNotifyConfig() {
        if (notify == null) {
            return false;
        }
        return notify.hasNotifyConfig();
    }

}
