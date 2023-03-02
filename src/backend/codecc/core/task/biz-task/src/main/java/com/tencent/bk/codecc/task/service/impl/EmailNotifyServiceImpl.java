package com.tencent.bk.codecc.task.service.impl;

import com.tencent.bk.codecc.defect.dto.IgnoreTypeEmailDTO;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeReportDetailVO;
import com.tencent.bk.codecc.quartz.pojo.OperationType;
import com.tencent.bk.codecc.task.pojo.EmailMessageModel;
import com.tencent.bk.codecc.task.pojo.EmailNotifyModel;
import com.tencent.bk.codecc.task.pojo.RtxNotifyModel;
import com.tencent.bk.codecc.task.pojo.WeChatMessageModel;
import com.tencent.bk.codecc.task.service.EmailNotifyService;
import com.tencent.devops.common.api.RtxNotifyVO;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class EmailNotifyServiceImpl implements EmailNotifyService {
    @Override
    public void sendReport(EmailNotifyModel emailNotifyModel) {

    }

    @Override
    public void sendRtx(RtxNotifyModel rtxNotifyModel) {

    }

    @Override
    public String addEmailScheduleTask(Long taskId, Set<Integer> week, Integer hour, OperationType operationType,
                                       String jobName) {
        return null;
    }

    @Override
    public void sendEmail(EmailMessageModel emailMessageModel) {

    }

    @Override
    public void sendWeChat(WeChatMessageModel weChatMessageModel) {

    }

    @Override
    public List<Long> turnOnWechatNotifyForGongFeng(Integer bgId, Integer deptId, Integer centerId) {
        return null;
    }

    @Override
    public Boolean codeCommentSendRtx(RtxNotifyVO rtxNotifyVO) {
        return true;
    }

    @Override
    public String calcDailyEmailCronExpression(Collection<Integer> dates, Integer hour) {
        return null;
    }

    @Override
    public void fixDailyEmailQuartz() {

    }

    @Override
    public void commonSendRtx(RtxNotifyVO rtxNotifyVO) {
    }

    @Override
    public void sendIgnoreTypeReportEmail(IgnoreTypeEmailDTO ignoreTypeEmailDTO) {
    }
}
