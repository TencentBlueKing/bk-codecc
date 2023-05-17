/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.scanschedule.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * 工具扫描记录的实体类
 *
 * @version V1.0
 * @date 2019/11/4
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_scan_record")
public class ScanRecordEntity extends CommonEntity {

    /**
     * scanId
     */
    @Field("scan_id")
    @Indexed
    private String scanId;

    /**
     * 应用code
     */
    @Field("app_code")
    @Indexed
    private String appCode;

    /**
     * 用户名
     */
    @Field("user_name")
    @Indexed
    private String userName;

    /**
     * 扫描内容
     */
    @Field("content")
    private String content;

    /**
     * 扫描规则集
     */
    @Field("checker_sets")
    private List<SimpleCheckerSet> checkerSets;

    /**
     * 扫描开始时间
     */
    @Field("start_time")
    private Long startTime;

    /**
     * 扫描结束时间
     */
    @Field("end_time")
    private Long endTime;

    /**
     * 扫描耗时
     */
    @Field("elapse_time")
    private Long elapseTime;

    /**
     * 扫描状态: 0：成功，1：失败
     */
    @Field("status")
    private int status;

    /**
     * 扫描失败信息
     */
    @Field("fail_msg")
    private String failMsg;

    /**
     * 扫描总告警数
     */
    @Field("defect_count")
    private int defectCount;

    @Data
    public static class SimpleCheckerSet {

        /**
         * 规则集名称
         */
        @Field("checker_set")
        private String checkerSet;

        /**
         * 规则集版本号
         */
        @Field("checker_set_version")
        private int checkerSetVersion;
    }
}