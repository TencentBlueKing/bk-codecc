package com.tencent.bk.codecc.defect.vo;

import lombok.Data;

import java.util.List;

/**
 * bk-user 的 batch_lookup_virtual_user 响应体
 *
 * @date 2025/07/28
 */
@Data
public class BkUserBatchGetUserRespVO {
    private List<User> data;

    @Data
    public static class User {
        private String bk_username;
        private String display_name;
    }
}
