package com.tencent.bk.codecc.task.vo;

import com.tencent.bk.codecc.task.vo.I18NMessageRequest.BaseVO;
import java.util.ArrayList;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class I18NMessageRequest extends ArrayList<BaseVO> {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BaseVO {

        private String moduleCode;

        private Set<String> keySet;

        private String localeString;
    }
}
