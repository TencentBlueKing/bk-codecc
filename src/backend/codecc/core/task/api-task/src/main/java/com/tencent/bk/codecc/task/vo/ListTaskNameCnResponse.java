package com.tencent.bk.codecc.task.vo;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ListTaskNameCnResponse extends HashMap<Long, String> {

    private static final long serialVersionUID = 1L;

    public ListTaskNameCnResponse() {
    }

    public ListTaskNameCnResponse(Map<Long, String> data) {
        putAll(data);
    }
}
