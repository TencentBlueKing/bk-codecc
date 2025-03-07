package com.tencent.bk.codecc.defect.vo;

import lombok.Data;

import java.util.List;

/**
 * /query/clocInfo 的请求 VO
 *
 * @date 2024/04/22
 */
@Data
public class ClocStatisticReqVO {

    private List<Long> taskIds;
    private List<String> languages;

}
