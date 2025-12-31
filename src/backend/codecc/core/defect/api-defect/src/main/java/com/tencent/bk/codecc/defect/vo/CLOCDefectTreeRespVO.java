package com.tencent.bk.codecc.defect.vo;

import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 代码统计查询返回视图
 *
 * @version V1.0
 * @date 2019/5/27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "代码统计文件树返回视图")
public class CLOCDefectTreeRespVO extends CommonDefectQueryRspVO {
    @Schema(description = "本次扫描代码库信息")
    private List<CodeRepo> codeRepo;

    @Schema(description = "CLOC文件树根结点")
    private CLOCTreeNodeVO clocTreeNodeVO;

    @Data
    public static class CodeRepo {

        public CodeRepo(String url, String branch) {
            this.url = url;
            this.branch = branch;

            // 获取代码库名称
            if (StringUtils.isNotBlank(url)) {
                String[] names = url.split("/");
                if (names.length > 1) {
                    this.name = names[names.length - 1].replace(".git", "");
                }
            }
        }

        @Schema(description = "代码库名称")
        private String name;

        @Schema(description = "代码库url")
        private String url;

        @Schema(description = "代码库分支")
        private String branch;
    }
}
