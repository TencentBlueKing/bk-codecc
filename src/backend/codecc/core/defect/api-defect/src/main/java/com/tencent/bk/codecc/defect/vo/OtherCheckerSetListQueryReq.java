package com.tencent.bk.codecc.defect.vo;

import com.tencent.bk.codecc.defect.vo.enums.CheckerSetCategory;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.Data;
import org.springframework.data.domain.Sort;

/**
 * 描述
 *
 * @version V1.0
 * @date 2020/2/25
 */
@Data
public class OtherCheckerSetListQueryReq {

    @Schema(description = "关键字")
    private String keyWord;

    @Schema(description = "语言")
    private Set<String> checkerSetLanguage;

    @Schema(description = "规则集类别")
    private Set<String> checkerSetCategory;

    @Schema(description = "工具名")
    private Set<String> toolName;

    @Schema(description = "规则集来源")
    private Set<CheckerSetSource> checkerSetSource;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "快速搜索框")
    private String quickSearch;

    @Schema(description = "排序字段")
    private String sortField;

    @Schema(description = "排序方向")
    private Sort.Direction sortType;

    int pageNum;

    int pageSize;

    @Schema(description = "项目是否已安装")
    private Boolean projectInstalled;
}
