/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.vo;

import com.tencent.bk.codecc.task.vo.CovSubcategoryVO;
import com.tencent.devops.common.api.CommonVO;
import com.tencent.devops.common.api.annotation.I18NFieldMarker;
import com.tencent.devops.common.api.annotation.I18NModuleCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

/**
 * 规则详情视图实体类
 *
 * @version V1.0
 * @date 2019/5/5
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "规则详情视图实体类")
public class CheckerDetailVO extends CommonVO {

    @Schema(description = "工具名", required = true)
    private String toolName;

    @Schema(description = "告警类型key", required = true)
    private String checkerKey;

    @Schema(description = "规则名称", required = true)
    @Pattern(regexp = "^[a-zA-Z_]{1,20}$", message = "输入的规则名不符合命名规则")
    private String checkerName;

    @Schema(description = "规则详细描述", required = true)
    @Size(min = 0, max = 150)
    @I18NFieldMarker(keyFieldHolder = "entityId", moduleCode = I18NModuleCode.CHECKER_DETAIL_CHECKER_DESC)
    private String checkerDesc;

    @Schema(description = "规则详细描述-带占位符")
    @I18NFieldMarker(keyFieldHolder = "entityId", moduleCode = I18NModuleCode.CHECKER_DETAIL_CHECKER_DESC_MODEL)
    private String checkerDescModel;

    @Schema(description = "规则严重程度，1=>严重，2=>一般，3=>提示", required = true, allowableValues = "{1,2,3}")
    @Min(1)
    @Max(3)
    private Integer severity;

    @Schema(description = "规则所属语言（针对KLOCKWORK）", required = true)
    private Integer language;

    @Schema(description = "规则状态 2=>打开 1=>关闭;", required = true, allowableValues = "{1,2}")
    private Integer status;

    @Schema(description = "规则状态是否打开")
    private Boolean checkerStatus;

    @Schema(description = "规则类型", required = true)
    @I18NFieldMarker(keyFieldHolder = "entityId", moduleCode = I18NModuleCode.CHECKER_DETAIL_CHECKER_TYPE)
    private String checkerType;

    @Schema(description = "规则类型说明", required = true)
    private String checkerTypeDesc;

    @Schema(description = "规则类型排序序列号", required = true)
    private String checkerTypeSort;

    @Schema(description = "所属规则包", required = true)
    private String pkgKind;

    @Schema(description = "项目框架（针对Eslint工具,目前有vue,react,standard）", required = true)
    private String frameworkType;

    @Schema(description = "规则配置", required = true)
    private String props;

    @Schema(description = "规则参数值", required = true)
    private String paramValue;

    @Schema(description = "规则所属标准", required = true)
    private Integer standard;

    @Schema(description = "规则是否支持配置true：支持;空或false：不支持", required = true)
    private Boolean editable;

    @Schema(description = "示例代码", required = true)
    private String codeExample;

    @Schema(description = "是否原生规则true:原生;false:自定义")
    private Boolean nativeChecker;

    @Schema(description = "是否进阶规则1:是;0:否")
    private int covProperty;

    @Schema(description = "Coverity规则子选项")
    private List<CovSubcategoryVO> covSubcategory;

    @Schema(description = "规则集是否选中")
    private Boolean checkerSetSelected;

    /*-------------------根据改动新增规则字段---------------------*/
    /**
     * 规则对应语言，都存文字，mongodb对按位与不支持
     */
    @Schema(description = "规则对应语言，都存文字，mongodb对按位与不支持")
    private Set<String> checkerLanguage;

    /**
     * 规则类型
     */
    @Schema(description = "规则类型")
    @Pattern(regexp = "CODE_DEFECT|CODE_FORMAT|SECURITY_RISK|COMPLEXITY|DUPLICATE|SOFTWARE_COMPOSITION")
    private String checkerCategory;

    /**
     * 规则类型中文名
     */
    @Schema(description = "规则类型中文名")
    @I18NFieldMarker(keyFieldHolder = "entityId", moduleCode = I18NModuleCode.CHECKER_DETAIL_CHECKER_CATEGORY_NAME)
    private String checkerCategoryName;

    /**
     * 规则标签
     */
    @Schema(description = "规则标签")
    @I18NFieldMarker(keyFieldHolder = "entityId", moduleCode = I18NModuleCode.CHECKER_DETAIL_CHECKER_TAG)
    private List<String> checkerTag;

    /**
     * 规则推荐类型
     */
    @Schema(description = "规则推荐类型")
    private String checkerRecommend;

    @Schema(description = "错误代码示例")
    private String errExample;

    @Schema(description = "正确代码示例")
    private String rightExample;

    @Schema(description = "规则值和工具名集合")
    private String checkerKeyAndToolName;

    @Schema(description = "规则参数列表，规则导入时接口传入")
    private List<CheckerProps> checkerProps;

    @Schema(description = "规则的扫描粒度")
    private String checkGranularity;

    @Schema(description = "规则版本")
    private Integer checkerVersion;

    @Schema(description = "规则发布者")
    private String publisher;

    @Schema(description = "规则来源：用户自定义/工具集成")
    private String checkerSource;
}
