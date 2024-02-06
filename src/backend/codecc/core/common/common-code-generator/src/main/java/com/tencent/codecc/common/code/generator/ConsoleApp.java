package com.tencent.codecc.common.code.generator;

import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.codecc.common.code.generator.common.Constants;
import com.tencent.codecc.common.code.generator.component.TrackingEntityComponent;
import com.tencent.codecc.common.code.generator.pojo.TrackingEntityDataModel;
import com.tencent.codecc.common.code.generator.util.FreeMarkerUtils;
import freemarker.template.Configuration;
import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 代码生成器入口类，直接执行main方法则生成对应代码
 * 该module不参与运行时，在开发期间生成代码
 * 已接入的entity，请查看see的列举：
 *
 * @see com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity
 * @see com.tencent.bk.codecc.defect.model.defect.code.gen.LintDefectV2EntityTracking
 */
public class ConsoleApp {


    /**
     * 入口
     *
     * @param args
     * @throws Throwable
     */
    public static void main(String[] args) throws Throwable {
        URI resourceUri = Objects.requireNonNull(
                ConsoleApp.class.getClassLoader().getResource(Constants.TEMPLATE_TRACKING_ENTITY)
        ).toURI();
        // NOCC:NP-NULL-ON-SOME-PATH-FROM-RETURN-VALUE(设计如此:)
        File resourceDir = Paths.get(resourceUri).getParent().toFile();

        List<Class<?>> clazzList = Stream.of(LintDefectV2Entity.class, CommonDefectEntity.class)
                .collect(Collectors.toList());

        for (Class<?> clazz : clazzList) {
            // 目标路径
            String targetFilePath = getFullTargetFilePathOnDefectModule(clazz);
            System.out.println("To: " + targetFilePath);

            // 数据模型
            TrackingEntityDataModel data = TrackingEntityComponent.INSTANCE.getRenderDataModel(clazz);

            // 生成代码
            Configuration cfg = FreeMarkerUtils.initConfig(resourceDir);
            FreeMarkerUtils.generate(data, Constants.TEMPLATE_TRACKING_ENTITY, targetFilePath, cfg);
        }
    }

    /**
     * 写入的目标文件路径
     * defect-model模块，生成的代码会在clazz同级的code.gen文件夹下
     *
     * @param clazz
     * @return
     */
    private static String getFullTargetFilePathOnDefectModule(Class<?> clazz) {
        String projectRoot = System.getProperty("user.dir");
        String fileName = String.format("%sTracking.java", clazz.getSimpleName());
        String[] paths = Stream.of(
                new String[]{"core", "defect", "model-defect", "src", "main", "java"},
                clazz.getPackage().getName().split("\\."),
                Constants.PACKAGE_REL.split("\\."),
                new String[]{fileName}
        ).flatMap(Stream::of).toArray(String[]::new);

        return Paths.get(projectRoot, paths).toString();
    }
}
