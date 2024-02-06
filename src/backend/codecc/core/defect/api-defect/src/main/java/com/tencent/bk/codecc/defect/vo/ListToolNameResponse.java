package com.tencent.bk.codecc.defect.vo;

import com.tencent.bk.codecc.defect.vo.ListToolNameResponse.ToolBase;
import com.tencent.devops.common.api.annotation.I18NFieldMarker;
import com.tencent.devops.common.api.annotation.I18NModuleCode;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
public class ListToolNameResponse extends ArrayList<ToolBase> {

    private static final long serialVersionUID = 1L;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ToolBase {

        private String entityId;
        private String toolName;

        @I18NFieldMarker(keyFieldHolder = "entityId", moduleCode = I18NModuleCode.TOOL_DISPLAY_NAME)
        private String toolDisplayName;
    }
}
