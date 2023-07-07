package com.tencent.bk.codecc.task.service;

import com.tencent.bk.codecc.task.vo.I18NMessageRequest;
import com.tencent.bk.codecc.task.vo.I18NMessageResponse;

public interface I18NService {
    I18NMessageResponse queryByCondition(I18NMessageRequest request);
}
