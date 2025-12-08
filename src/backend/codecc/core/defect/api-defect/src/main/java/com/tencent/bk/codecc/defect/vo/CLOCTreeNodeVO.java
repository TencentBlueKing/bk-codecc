package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "CLOC工具文件树")
public class CLOCTreeNodeVO extends TreeNodeVO {
    @Schema(description = "文件数")
    private long fileNum;

    @Schema(description = "代码行数")
    private long codeLines;

    @Schema(description = "空白行数")
    private long blankLines;

    @Schema(description = "注释行数")
    private long commentLines;

    @Schema(description = "注释率")
    private Double commentRate;

    @Schema(description = "有效注释行数")
    private Long efficientCommentLines;

    @Schema(description = "有效注释率")
    private Double efficientCommentRate;

    @Schema(description = "总行数")
    private long totalLines;

    @Schema(description = "用于计算有效注释率的总行数-排除了部分语言")
    private long totalLinesForEfficient;

    @Schema(description = "树ID")
    private String treeId;

    @Schema(description = "树节点")
    private String name;

    @Schema(description = "子树集合")
    private List<CLOCTreeNodeVO> clocChildren;

    @Schema(description = "是否开放")
    private Boolean expanded;

    public CLOCTreeNodeVO() {
    }

    public CLOCTreeNodeVO(String treeId, String name, boolean open) {
        if (name == null) {
            this.name = "";
        } else {
            this.name = name;
        }
        this.treeId = treeId;
        this.expanded = open;
    }

    public void addBlank(long blankLines) {
        this.blankLines += blankLines;
    }

    public void addCode(long codeLines) {
        this.codeLines += codeLines;
    }

    public void addComment(long commentLines) {
        this.commentLines += commentLines;
    }

    public void addEfficientComment(Long efficientCommentLines) {
        if (efficientCommentLines != null && this.efficientCommentLines != null) {
            this.efficientCommentLines += efficientCommentLines;
        } else if (efficientCommentLines != null) {
            this.efficientCommentLines = efficientCommentLines;
        }
    }

    public void addTotal(long totalLines) {
        this.totalLines += totalLines;
    }

    public void addFileNum(long fileNum) {
        this.fileNum += fileNum;
    }

    public void addTotalForEfficient(long totalLines) {
        this.totalLinesForEfficient += totalLines;
    }

}
