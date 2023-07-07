package com.tencent.bk.codecc.task.model;


import com.tencent.codecc.common.db.CommonEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "t_i18n_message")
@CompoundIndexes({
        @CompoundIndex(
                name = "module_code_1_key_1_locale_1",
                def = "{'module_code': 1, 'key': 1, 'locale':1}",
                background = true
        )
})
public class I18NMessageEntity extends CommonEntity {

    /**
     * 资源模块
     */
    @Field("module_code")
    private String moduleCode;

    /**
     * 语言信息
     */
    private String locale;

    /**
     * 资源编码
     */
    private String key;

    /**
     * 资源值
     */
    private String value;
}
