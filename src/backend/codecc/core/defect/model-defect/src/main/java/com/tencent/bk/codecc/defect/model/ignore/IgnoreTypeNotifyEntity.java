package com.tencent.bk.codecc.defect.model.ignore;

import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.Set;

@Data
public class IgnoreTypeNotifyEntity {

    /**
     * 通知的job名称
     */
    @Field("job_name")
    private String jobName;

    /**
     * 通知 - 月份配置
     * 是否为每月
     */
    @Field("every_month")
    private Boolean everyMonth;

    /**
     * 通知 - 月份配置
     * 1～12
     */
    @Field("notify_months")
    private Set<Integer> notifyMonths;

    /**
     * 通知 - 每周配置
     * 是否为每周
     */
    @Field("every_week")
    private Boolean everyWeek;

    /**
     * 通知 - 每月第几周配置
     * 1～5
     */
    @Field("notify_week_of_months")
    private Set<Integer> notifyWeekOfMonths;


    /**
     * 通知 - 每周第几天配置
     * 1～7
     */
    @Field("notify_day_of_weeks")
    private Set<Integer> notifyDayOfWeeks;

    /**
     * 通知接接收者类型
     * {@link com.tencent.devops.common.constant.ComConstants.IgnoreTypeNotifyReceiverType}
     */
    @Field("notify_receiver_types")
    private List<String> notifyReceiverTypes;

    /**
     * 附加通知人
     */
    @Field("ext_receiver")
    private Set<String> extReceiver;

    /**
     * 通知类型
     * {@link com.tencent.devops.common.constant.ComConstants.NotifyType}
     */
    @Field("notify_type")
    private List<String> notifyTypes;


    public Boolean hasNotifyConfig() {
        if (CollectionUtils.isEmpty(notifyDayOfWeeks)) {
            return false;
        }
        if ((everyMonth != null && everyMonth) || CollectionUtils.isNotEmpty(notifyMonths)) {
            return true;
        }
        if ((everyWeek != null && everyWeek) || CollectionUtils.isNotEmpty(notifyWeekOfMonths)) {
            return true;
        }
        return false;
    }

}
