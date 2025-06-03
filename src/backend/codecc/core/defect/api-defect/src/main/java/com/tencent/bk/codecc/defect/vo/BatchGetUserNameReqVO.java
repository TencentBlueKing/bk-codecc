package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ServiceMultiTenantRestResource#batchGetUserName 请求体
 *
 * @date 2025/07/28
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("batchGetUserName 请求体")
public class BatchGetUserNameReqVO {
    private String tenantId;
    private List<String> userIds;
}
