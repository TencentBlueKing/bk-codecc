package com.tencent.bk.codecc.task.model;


import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 动态配置表，这个表会定时将数据库的数据更新到内存
 * 接入配置中心以后，使用配置中心替换
 *
 * @version V1.0
 * @date 2021/05/08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_dynamic_config")
public class DynamicConfigEntity extends CommonEntity {

    /**
     * 键值（唯一），标识这个配置
     */
    @Field("key")
    @Indexed(background = true,unique = true)
    private String key;
    /**
     * 描述，方便理解变量含义
     */
    @Field("decs")
    private String decs;

    /**
     * 对应的值，字符串，复杂类型可使用JSON字符串，再解析使用
     */
    @Field("value")
    private String value;


}
