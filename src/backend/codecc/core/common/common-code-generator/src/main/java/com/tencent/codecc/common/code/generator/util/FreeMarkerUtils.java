package com.tencent.codecc.common.code.generator.util;

import freemarker.template.Configuration;
import freemarker.template.Template;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FreeMarkerUtils {

    public static Configuration initConfig(File templateDir) throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setDirectoryForTemplateLoading(templateDir);
        cfg.setDefaultEncoding("UTF-8");
        return cfg;
    }

    /**
     * 生成代码主逻辑入口
     *
     * @param dataModel 数据模型
     * @param templateName 模板名称
     * @param targetFilePath 生成目标文件完整路径，比如：/xx/yy/zz.java
     * @param cfg 配置
     */
    public static void generate(
            Object dataModel,
            String templateName,
            String targetFilePath,
            Configuration cfg
    ) {
        try {
            Path dir = Paths.get(targetFilePath).getParent();
            if (dir != null && !dir.toFile().exists()) {
                Files.createDirectories(dir);
            }
        } catch (IOException e) {
            System.out.println("文件目录创建异常");
            e.printStackTrace();
        }

        try (FileOutputStream fileOutputStream = new FileOutputStream(targetFilePath);
                OutputStreamWriter outputSW = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
                BufferedWriter bufferedWriter = new BufferedWriter(outputSW)) {
            Template template = cfg.getTemplate(templateName);
            template.process(dataModel, bufferedWriter);
        } catch (Throwable e) {
            System.out.println("视图引擎渲染异常");
            e.printStackTrace();
        }
    }
}
