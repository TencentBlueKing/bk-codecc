package com.tencent.bk.codecc.scanschedule.pojo.input;

import com.google.common.collect.Lists;
import lombok.Data;

import java.util.List;

/**
 * 规则对象类
 * @author jimxzcai
 */
@Data
public class OpenCheckers {

    /**
     * 规则名称
     */
    public String checkerName;

    /**
     * 是否为工具本身规则
     */
    private boolean nativeChecker = true;

    /**
     * 规则严重等级 1=>严重，2=>一般，3=>提示
     */
    private int severity = 1;

    /**
     * 规则子选项
     */
    public List<CheckerOptions> checkerOptions = Lists.newArrayList();
}
