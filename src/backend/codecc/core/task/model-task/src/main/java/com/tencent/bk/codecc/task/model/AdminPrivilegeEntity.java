package com.tencent.bk.codecc.task.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * 管理员授权信息实体类
 *
 * @version V1.0
 * @date 2025/4/18
 */

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "t_admin_privilege_info")
@CompoundIndexes({
        @CompoundIndex(name = "user_id_1_privilege_type_1_status_1",
                def = "{'user_id': 1, 'privilege_type': 1, 'status': 1}"
        ),
        @CompoundIndex(name = "privilege_type_1_status_1", def = "{'privilege_type': 1, 'status': 1}"),
        @CompoundIndex(name = "status_1_end_time_1", def = "{'status': 1, 'end_time': 1}")
})
public class AdminPrivilegeEntity extends CommonEntity {

    /**
     * 用户名ID
     */
    @Field("user_id")
    @Indexed(unique = true)
    private String userId;

    /**
     * 枚举：GLOBAL_ADMIN(原平台管理员)/BG_ADMIN(原BG管理员)/TOOL_ADMIN(工具管理员)
     */
    @Field("privilege_type")
    private String privilegeType;

    /**
     * 已授权的BG id
     */
    @Field("bg_id_list")
    private List<Integer> bgIdList;

    /**
     * 已授权的来源平台
     */
    @Field("create_froms")
    private List<String> createFroms;

    /**
     * 工具列表
     */
    @Field("tool_list")
    private List<String> toolList;

    /**
     * 时效控制
     */
    @Field("start_time")
    private Long startTime;

    /**
     * 时效控制
     */
    @Field("end_time")
    private Long endTime;

    /**
     * 逻辑删除、禁用状态，true为有效中
     */
    @Field("status")
    @Indexed
    private Boolean status = Boolean.FALSE;

    /**
     * 权限授予原因
     */
    @Field("reason")
    private String reason;

}
