package com.tencent.devops.common.storage.constant;

public enum StorageType {
    BKREPO("bkrepo"),

    BKREPO_CSI("bkrepo-csi"),

    NFS("nfs"),

    LOCAL("local");
    private String code;

    StorageType(String code) {
        this.code = code;
    }

    /**
     * 是否为挂载类型
     *
     * @return
     */
    public static boolean isMountType(String code) {
        return code == null || BKREPO_CSI.code.equals(code) || NFS.code.equals(code) || LOCAL.code.equals(code);
    }

    public String code() {
        return code;
    }
}
