package com.tencent.bk.codecc.defect.model.checkerset;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 描述
 *
 * @version V1.0
 * @date 2020/1/7
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckerSetCatagoryEntity
{
    /**
     * 英文名称
     */
    @Field("en_name")
    private String enName;

    /**
     * 中文名称
     */
    @Field("cn_name")
    private String cnName;
}
