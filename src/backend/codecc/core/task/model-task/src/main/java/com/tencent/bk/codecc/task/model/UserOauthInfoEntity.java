package com.tencent.bk.codecc.task.model;

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
@Document(collection = "t_user_oauth")
@CompoundIndexes({
        @CompoundIndex(name = "user_id_system_extra_idx", def = "{'user_id': 1, 'system': 1, 'extra': 1}",
                unique = true, background = true)
})
public class UserOauthInfoEntity extends CommonEntity {
    /**
     * 项目主键id
     */
    @Field("user_id")
    @Indexed
    private String userId;

    /**
     * 提单系统
     */
    @Field("system")
    @Indexed
    private String system;

    /**
     * api调用token
     */
    @Field("access_token")
    private String accessToken;

    /**
     * 过期时间，毫秒
     */
    @Field("expires")
    private Long expires;

    @Field("token_type")
    private String tokenType;

    /***
     * 提单预留字段，一般是系统的项目id
     */
    @Field("extra")
    private String extra;

    @Field("refresh_token")
    private String refreshToken;
}
