package com.tencent.bk.codecc.task.vo;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class ListTaskNameCnResponse extends HashMap<Long, String> {

    public ListTaskNameCnResponse() {
    }

    public ListTaskNameCnResponse(Map<Long, String> data) {
        putAll(data);
    }
}
