package com.tencent.devops.common.util;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {

    private static Logger logger = LoggerFactory.getLogger(FileUtils.class);

    public static String getFileNameByPath(String filePath) {
        if (StringUtils.isBlank(filePath)) {
            return null;
        }
        int fileNameIndex = filePath.lastIndexOf("/");
        if (fileNameIndex == -1) {
            fileNameIndex = filePath.lastIndexOf("\\");
        }
        return filePath.substring(fileNameIndex + 1);
    }
}
