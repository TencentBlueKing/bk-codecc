package com.tencent.devops.common.util;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
        File parentPath = file.getParentFile();
        if (parentPath != null && !parentPath.exists()) {
            if (!parentPath.mkdirs()) {
                logger.error("when save content to file, create directory failed {}", parentPath.getAbsolutePath());
                return false;
            }
        }

        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(file.getCanonicalPath()), StandardCharsets.UTF_8))) {
            bw.write(new String(content.getBytes(), StandardCharsets.UTF_8));
            file.setExecutable(true, false);
        } catch (IOException e) {
            logger.error("file {} save content failed: ", filePath, e);
            return false;
        }

        return true;
    }

    /**
     * 解压zip
     *
     * @param file
     * @param destDir
     * @return
     */
    public static boolean unzipFile(String file, String destDir) {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(file);
        } catch (IOException e) {
            logger.error("ZipFile {} ready failed: ", file, e);
            return false;
        }
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            File entryDestination = new File(destDir, entry.getName());
            if (entry.isDirectory()) {
                if (!entryDestination.exists()) {
                    entryDestination.mkdirs();
                }
            } else {
                File parentDir = entryDestination.getParentFile();
                if (!parentDir.exists()) {
                    parentDir.mkdirs();
                }
                if (!unzip(zipFile, entry, entryDestination)) {
                    return false;
                }
            }
        }
        return true;
    }

    // 解压单个文件
    private static boolean unzip(ZipFile zipFile, ZipEntry entry, File entryDestination) {
        try (InputStream in = zipFile.getInputStream(entry);
             OutputStream out = Files.newOutputStream(entryDestination.toPath())) {
            IOUtils.copy(in, out);
            return true;
        } catch (IOException e) {
            logger.error("file {} unzip failed, it maybe is broken", zipFile, e);
            return false;
        }
    }

    public static boolean chmodPath(String path, boolean readable, boolean writable, boolean executable) {
        try {
            for (String filePath : walkPath(path)) {
                if (Paths.get(filePath).toFile().isFile()) {
                    Paths.get(filePath).toFile().setReadable(readable);
                    Paths.get(filePath).toFile().setWritable(writable);
                    Paths.get(filePath).toFile().setExecutable(executable);
                }
            }
        } catch (Exception e) {
            logger.error("file {} right command failed!", path, e);
            return false;
        }
        return true;
    }

    /**
     * 遍历路径
     * @param scanPath
     * @return
     */
    public static List<String> walkPath(String scanPath)  {
        List<String> scanFiles = Lists.newArrayList();
        List<Path> resultList;
        Stream<Path> walk = null;
        try {
            walk = Files.walk(Paths.get(scanPath));
        } catch (IOException e) {
            logger.error("path {} walk failed!", scanPath, e);
        }
        if (walk != null) {
            resultList = walk.filter(Files::isRegularFile)
                    .collect(Collectors.toList());
            for (Path path : resultList) {
                scanFiles.add(path.toFile().getAbsolutePath().replace("\\", "/"));
            }
        }
        return scanFiles;
    }
}
