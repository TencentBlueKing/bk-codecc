package com.tencent.bk.codecc.scanschedule.pojo.input;

import com.google.common.collect.Lists;
import lombok.Data;

import java.util.List;

@Data
public class OpenCheckers {

    private String checkerName; //规则名称
    private boolean nativeChecker = true; //是否为工具本身规则
    private int severity = 1; //规则严重等级 1=>严重，2=>一般，3=>提示
    private List<CheckerOptions> checkerOptions = Lists.newArrayList(); //规则子选项
}
