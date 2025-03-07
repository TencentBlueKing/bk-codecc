package com.tencent.bk.codecc.defect.dao;

import com.tencent.bk.codecc.defect.vo.enums.CheckerCategory;
import com.tencent.bk.codecc.defect.vo.enums.CheckerListSortType;
import com.tencent.bk.codecc.defect.vo.enums.CheckerRecommendType;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSource;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.Set;

@Data
@ApiModel("规则清单数据查询参数类")
public class CheckerListQueryParams {
    @ApiModelProperty("关键字")
    private String keyWord;

    @ApiModelProperty("语言")
    private Set<String> checkerLanguage;

    @ApiModelProperty("规则类型")
    private Set<CheckerCategory> checkerCategory;

    @ApiModelProperty("工具")
    private Set<String> toolName;

    @ApiModelProperty("标签")
    private Set<String> tag;

    @ApiModelProperty("严重等级")
    private Set<String> severity;

    @ApiModelProperty("可修改参数")
    private Set<Boolean> editable;

    @ApiModelProperty("推荐")
    private Set<CheckerRecommendType> checkerRecommend;

    @ApiModelProperty("规则集id")
    private String checkerSetId;

    @ApiModelProperty("版本号")
    private Integer version;

    @ApiModelProperty("是否规则集选中")
    private Set<Boolean> checkerSetSelected;

    @ApiModelProperty("选中的规则集")
    private Set<String> selectedCheckerKey;

    @ApiModelProperty("页数")
    private Integer pageNum;

    @ApiModelProperty("页数")
    private Integer pageSize;

    @ApiModelProperty("升序或降序")
    private Sort.Direction sortType;

    @ApiModelProperty("排序字段")
    private CheckerListSortType sortField;

    @ApiModelProperty("工具集成状态")
    private Map<String, Integer> toolIntegratedStatusMap;

    @ApiModelProperty("项目ID，用于过滤项目不可见的自定义规则")
    private String projectId;

    @ApiModelProperty("规则创建来源")
    private Set<CheckerSource> checkerSource;

    @ApiModelProperty("查询请求是否来源于op")
    private Boolean isOp = false;

}
