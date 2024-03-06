package com.tencent.bk.codecc.codeccjob.service.impl.hotcold.pojo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageFetchResult<T> implements AutoCloseable {

    private Boolean hasNext;
    private List<T> data;

    /**
     * 通用分页
     */
    private Pageable nextPageable;

    /**
     * LINT分页专用
     */
    private LintPageable nextPageableForLint;

    @Override
    public void close() {
        // jvm
        if (data != null) {
            data.clear();
            data = null;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LintPageable {

        private String marker;
        private int skip;
        private int limit;
        private List<String> toolNameList;
    }
}
