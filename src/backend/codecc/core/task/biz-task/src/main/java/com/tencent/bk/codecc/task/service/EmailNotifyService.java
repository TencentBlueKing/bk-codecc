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

package com.tencent.bk.codecc.task.service;

import com.tencent.bk.codecc.defect.dto.IgnoreTypeEmailDTO;
import com.tencent.bk.codecc.quartz.pojo.OperationType;
import com.tencent.bk.codecc.task.pojo.EmailMessageModel;
import com.tencent.bk.codecc.task.pojo.EmailNotifyModel;
import com.tencent.bk.codecc.task.pojo.RtxNotifyModel;
import com.tencent.bk.codecc.task.pojo.WeChatMessageModel;
import com.tencent.devops.common.api.RtxNotifyVO;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 通知服务逻辑接口
 * 
 * @date 2019/11/19
 * @version V1.0
 */
public interface EmailNotifyService {

    /**
     * 发送即时通知报告
     * @param emailNotifyModel
     */
    void sendReport(EmailNotifyModel emailNotifyModel);

    /**
     * 发送企业微信报告
     * @param rtxNotifyModel
     */
    void sendRtx(RtxNotifyModel rtxNotifyModel);


    /**
     * 添加定时报告任务
     * @param taskId
     * @param week
     * @param hour
     */
    String addEmailScheduleTask(Long taskId, Set<Integer> week, Integer hour, OperationType operationType, String jobName);

    /**
     * 从指定邮件模版发送邮件
     * @param emailMessageModel 邮件参数
     */
    void sendEmail(EmailMessageModel emailMessageModel);

    /**
     * 从指定微信模版发送微信
     * @param weChatMessageModel 邮件参数
     */
    void sendWeChat(WeChatMessageModel weChatMessageModel);


    /**
     * 为开源扫描打开企业微信实时通知，接收人为"遗留问题处理人"
     *
     * @param bgId
     * @param deptId
     * @param centerId
     * @return 更新命中的taskId
     */
    List<Long> turnOnWechatNotifyForGongFeng(Integer bgId, Integer deptId, Integer centerId);

    /**
     * 评论问题企业微信通知@到的开发者
     *
     * @param rtxNotifyVO 企业微信通知视图
     * @return boolean
     */
    Boolean codeCommentSendRtx(RtxNotifyVO rtxNotifyVO);


    /**
     * 计算定时报告cron表达式
     *
     * @param dates 非quartz表达式的星期，单纯的周一~周日对应1~7
     * @param hour
     * @return
     */
    String calcDailyEmailCronExpression(Collection<Integer> dates, Integer hour);

    /**
     * 修复定时报告相关数据
     */
    void fixDailyEmailQuartz();

    void commonSendRtx(RtxNotifyVO rtxNotifyVO);

    void sendIgnoreTypeReportEmail(IgnoreTypeEmailDTO ignoreTypeEmailDTO);
}
