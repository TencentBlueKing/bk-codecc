/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 Tencent. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.vo;

import com.tencent.devops.common.util.BeanUtils;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * 批量告警处理的请求对象
 *
 * @version V1.0
 * @date 2019/10/31
 */
@Data
@ApiModel("批量告警处理的请求对象")
public class BatchDefectProcessReqOldVO {

    @ApiModelProperty("任务ID")
    private long taskId;

    private List<Long> taskIdList;

    @ApiModelProperty("工具名称，数据迁移后支持多选，逗号分割多个")
    private String toolName;

    @ApiModelProperty("工具名称，数据迁移后支持多选，逗号分割多个")
    private String dimension;

    @ApiModelProperty("业务类型：忽略IgnoreDefect、分配AssignDefect、标志修改MarkDefect")
    private String bizType;

    @ApiModelProperty("是否全选的标志，Y表示全选，N或者空表示非全选")
    private String isSelectAll;

    @ApiModelProperty("告警缺陷列表")
    private Set<String> defectKeySet;

    @ApiModelProperty("文件告警列表")
    private List<QueryFileDefectVO> fileDefects;

    @ApiModelProperty("告警查询条件json")
    private String queryDefectCondition;

    @ApiModelProperty("源告警处理人")
    private Set<String> sourceAuthor;

    @ApiModelProperty("分配给新的处理人")
    private LinkedHashSet<String> newAuthor;

    @ApiModelProperty("忽略告警原因类型")
    private int ignoreReasonType;

    @ApiModelProperty("忽略告警具体原因")
    private String ignoreReason;

    @ApiModelProperty("忽略告警的作者")
    private String ignoreAuthor;

    @ApiModelProperty("标志修改，0表示取消标志，1表示标志修改")
    private Integer markFlag;

    @ApiModelProperty(value = "数据迁移是否成功", required = false)
    private Boolean dataMigrationSuccessful;

    /**
     * 将老的BatchDefectProcessReqOldVO转为新的BatchDefectProcessReqVO
     * @return
     */
    public BatchDefectProcessReqVO toBatchDefectProcessReqVO() {
        BatchDefectProcessReqVO reqVO = new BatchDefectProcessReqVO();
        BeanUtils.copyProperties(this, reqVO);
        if (StringUtils.isNotBlank(this.getToolName())) {
            reqVO.setToolNameList(Collections.singletonList(this.getToolName()));
        }
        if (StringUtils.isNotBlank(this.getDimension())) {
            reqVO.setDimensionList(Collections.singletonList(this.getDimension()));
        }
        return reqVO;
    }
}
