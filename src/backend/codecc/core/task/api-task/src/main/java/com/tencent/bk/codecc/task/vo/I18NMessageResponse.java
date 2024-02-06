package com.tencent.bk.codecc.task.vo;


import com.tencent.bk.codecc.task.vo.I18NMessageResponse.BaseVO;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class I18NMessageResponse extends ArrayList<BaseVO> {

    private static final long serialVersionUID = 1L;

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class BaseVO {

        private String moduleCode;
        private String locale;
        private String key;
        private String value;
    }
}
