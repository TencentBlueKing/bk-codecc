package com.tencent.bk.codecc.defect.vo.ignore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ISTM 回调返回对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItsmCallbackRespVO {

    private String message;

    private Integer code;

    private String data;

    private Boolean result;

    public static ItsmCallbackRespVO success() {
        return new ItsmCallbackRespVO("success", 0, null, true);
    }

    public static ItsmCallbackRespVO fail() {
        return new ItsmCallbackRespVO("fail", 0, null, false);
    }
}
