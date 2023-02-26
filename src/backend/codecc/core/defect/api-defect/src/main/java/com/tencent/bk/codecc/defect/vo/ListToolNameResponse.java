package com.tencent.bk.codecc.defect.vo;

import com.tencent.bk.codecc.defect.vo.ListToolNameResponse.ToolBase;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
public class ListToolNameResponse extends ArrayList<ToolBase> {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ToolBase {

        private String toolName;
        private String toolDisplayName;
    }
}
