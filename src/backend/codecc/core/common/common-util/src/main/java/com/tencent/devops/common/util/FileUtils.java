package com.tencent.devops.common.util;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;

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

    /**
     * 保存内容到文件中
     *
     * @param filePath
     * @param content
     * @return
     */
    public static boolean saveContentToFile(String filePath, String content) {
        File file = new File(filePath);
        if (file.getParent() != null) {
            File parentPath = new File(file.getParent());
            if (!parentPath.exists()) {
                parentPath.mkdirs();
            }
        }

        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            bw.write(new String(content.getBytes(), StandardCharsets.UTF_8));
            file.setExecutable(true, false);
        } catch (IOException e) {
            logger.error("保存内容到文件中失败: ", e);
            return false;
        }

        return true;
    }
}
