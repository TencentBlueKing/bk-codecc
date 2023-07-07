package com.tencent.devops.common.storage.constant;

public enum StorageType {
    BKREPO("bkrepo"),
    NFS("nfs")
    ;
    private String code;

    StorageType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
