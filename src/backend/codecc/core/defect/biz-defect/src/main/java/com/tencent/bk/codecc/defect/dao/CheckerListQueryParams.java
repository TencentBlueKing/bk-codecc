package com.tencent.bk.codecc.defect.dao;

import com.tencent.bk.codecc.defect.vo.enums.CheckerCategory;
import com.tencent.bk.codecc.defect.vo.enums.CheckerListSortType;
import com.tencent.bk.codecc.defect.vo.enums.CheckerRecommendType;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.Set;

@Data
@Schema(description = "规则清单数据查询参数类")
public class CheckerListQueryParams {
    @Schema(description = "关键字")
    private String keyWord;

    @Schema(description = "语言")
    private Set<String> checkerLanguage;

    @Schema(description = "规则类型")
    private Set<CheckerCategory> checkerCategory;

    @Schema(description = "工具")
    private Set<String> toolName;

    @Schema(description = "标签")
    private Set<String> tag;

    @Schema(description = "严重等级")
    private Set<String> severity;

    @Schema(description = "可修改参数")
    private Set<Boolean> editable;

    @Schema(description = "推荐")
    private Set<CheckerRecommendType> checkerRecommend;

    @Schema(description = "规则集id")
    private String checkerSetId;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "是否规则集选中")
    private Set<Boolean> checkerSetSelected;

    @Schema(description = "选中的规则集")
    private Set<String> selectedCheckerKey;

    @Schema(description = "页数")
    private Integer pageNum;

    @Schema(description = "页数")
    private Integer pageSize;

    @Schema(description = "升序或降序")
    private Sort.Direction sortType;

    @Schema(description = "排序字段")
    private CheckerListSortType sortField;

    @Schema(description = "工具集成状态")
    private Map<String, Integer> toolIntegratedStatusMap;

    @Schema(description = "项目ID，用于过滤项目不可见的自定义规则")
    private String projectId;

    @Schema(description = "规则创建来源")
    private Set<CheckerSource> checkerSource;

    @Schema(description = "查询请求是否来源于op")
    private Boolean isOp = false;

}
