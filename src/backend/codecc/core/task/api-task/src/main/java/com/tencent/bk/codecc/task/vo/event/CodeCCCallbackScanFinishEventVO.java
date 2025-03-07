package com.tencent.bk.codecc.task.vo.event;

import com.tencent.devops.common.constant.ComConstants.CodeCCCallbackEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CodeCCCallbackScanFinishEventVO extends CodeCCCallbackEventVO<CodeCCCallbackScanFinishEventDataVO> {
    public CodeCCCallbackScanFinishEventVO() {
        this(null);
    }

    public CodeCCCallbackScanFinishEventVO(CodeCCCallbackScanFinishEventDataVO dataVO) {
        super.setEvent(CodeCCCallbackEvent.SCAN_FINISH);
        super.setData(dataVO);
    }
}
