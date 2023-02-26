package com.tencent.bk.codecc.task.vo;

import lombok.Data;

/**
 * 流水线回调实体
 */
@Data
public class PipelineCallbackVo {

    /**
     * 回调事件
     */
    private String event;

    /**
     * 回调内容
     */
    private PipelineCallbackData data;


    @Data
    public static class PipelineCallbackData {

        /*-- ----   公共参数 ------- */
        /**
         * 流水线ID
         */
        private String pipelineId;
        /**
         * 流水线名称
         */
        private String pipelineName;
        /**
         * 用户ID
         */
        private String userId;
        /**
         * 更新时间
         */
        private Long updateTime;

        /*-- ----   公共参数 ------- */
        /**
         * 构建ID
         */
        private String buildId;
        /**
         * 项目ID
         */
        private String projectId;
        /**
         * 开始时间
         */
        private Long startTime;
        /**
         * 结束时间
         */
        private Long endTime;
        /**
         * 状态
         */
        private String status;


    }

}


