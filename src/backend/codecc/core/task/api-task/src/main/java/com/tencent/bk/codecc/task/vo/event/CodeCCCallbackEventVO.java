package com.tencent.bk.codecc.task.vo.event;

import com.tencent.devops.common.constant.ComConstants;
import lombok.Data;

@Data
public class CodeCCCallbackEventVO<T extends CodeCCCallbackEventDataVO> {
    private ComConstants.CodeCCCallbackEvent event;
    private T data;
}
