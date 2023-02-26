package com.tencent.bk.codecc.defect.vo.ignore;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Data
public class IgnoreTypeNotifyVO {

    /**
     * 通知 - 月份配置
     * 是否为每月
     */
    @ApiModelProperty("every_month")
    private Boolean everyMonth;

    /**
     * 通知 - 月份配置
     * 1～12
     */
    @ApiModelProperty("notify_months")
    private Set<Integer> notifyMonths;

    /**
     * 通知 - 每周配置
     * 是否为每周
     */
    @ApiModelProperty("every_week")
    private Boolean everyWeek;

    /**
     * 通知 - 每月第几周配置
     * 1～5
     */
    @ApiModelProperty("notify_week_of_months")
    private Set<Integer> notifyWeekOfMonths;


    /**
     * 通知 - 每周第几天配置
     * 1～7
     */
    @ApiModelProperty("notify_day_of_weeks")
    private Set<Integer> notifyDayOfWeeks;

    /**
     * 通知接接收者类型
     * {@link com.tencent.devops.common.constant.ComConstants.IgnoreTypeNotifyReceiverType}
     */
    @ApiModelProperty("notify_receiver_types")
    private List<String> notifyReceiverTypes;

    /**
     * 附加通知人
     */
    @ApiModelProperty("ext_receiver")
    private Set<String> extReceiver;

    /**
     * 通知类型
     * {@link com.tencent.devops.common.constant.ComConstants.NotifyType}
     */
    @ApiModelProperty("notify_type")
    private List<String> notifyTypes;

    public static IgnoreTypeNotifyVO newIgnoreTypeNotifyVO() {
        return newIgnoreTypeNotifyVO(false);
    }

    public static IgnoreTypeNotifyVO newIgnoreTypeNotifyVO(boolean init) {
        IgnoreTypeNotifyVO vo = new IgnoreTypeNotifyVO();
        if (init) {
            vo.setEveryMonth(false);
            vo.setNotifyMonths(new HashSet<>());
            vo.setEveryWeek(false);
            vo.setNotifyWeekOfMonths(new HashSet<>());
            vo.setNotifyDayOfWeeks(new HashSet<>());
            vo.setNotifyTypes(new LinkedList<>());
            vo.setNotifyReceiverTypes(new LinkedList<>());
            vo.setExtReceiver(new HashSet<>());
        }
        return vo;
    }

}
