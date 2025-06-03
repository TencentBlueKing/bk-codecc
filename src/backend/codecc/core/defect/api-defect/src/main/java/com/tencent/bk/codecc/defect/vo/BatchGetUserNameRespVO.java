package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * ServiceMultiTenantRestResource#batchGetUserName 请求体
 *
 * @date 2025/07/28
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("batchGetUserName 响应体")
public class BatchGetUserNameRespVO {
    @ApiModelProperty("多租户用户 id -> 用户名")
    private Map<String, String> userId2userName;
}
