package com.tencent.bk.codecc.defect.model.ignore;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_ignore_type_sys")
public class IgnoreTypeSysEntity extends CommonEntity {
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
     * 状态： 0启用，1不启用
     */
    @Field("status")
    private Integer status;
}
