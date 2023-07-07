package com.tencent.codecc.common.code.generator.component;

import com.tencent.codecc.common.code.generator.common.Constants;
import com.tencent.codecc.common.code.generator.pojo.FieldInfo;
import com.tencent.codecc.common.code.generator.pojo.TrackingEntityDataModel;
import com.tencent.codecc.common.code.generator.util.StringUtils;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.DBRef;

public class TrackingEntityComponent {

    public static final TrackingEntityComponent INSTANCE = new TrackingEntityComponent();

    private TrackingEntityComponent() {
    }

    /**
     * 获取渲染模板的数据
     *
     * @param clazz
     * @return
     */
    public TrackingEntityDataModel getRenderDataModel(Class<?> clazz) {
        List<Field> allFields = getAllFields(new ArrayList<>(), clazz);
        // 黑名单模式，排除@Transient、@DBRef、@Id
        Predicate<Field> matchBlackList = x -> Stream.of(x.getAnnotations()).anyMatch(y ->
                y.annotationType().equals(DBRef.class)
                        || y.annotationType().equals(Transient.class)
                        || y.annotationType().equals(Id.class)
        );
        Predicate<Field> annotationFilter = x -> x.getAnnotations().length == 0 || !matchBlackList.test(x);
        List<FieldInfo> fieldInfoList = allFields.stream()
                .filter(annotationFilter)
                .map(this::getFieldInfo)
                .sorted(Comparator.comparing(FieldInfo::getJavaFieldName))
                .collect(Collectors.toList());

        TrackingEntityDataModel dataModel = new TrackingEntityDataModel();
        dataModel.setBaseEntityName(clazz.getSimpleName());
        dataModel.setBaseEntityImport(String.format("%s.%s", clazz.getPackage().getName(), clazz.getSimpleName()));
        dataModel.setFullPackagePath(String.format("%s.%s", clazz.getPackage().getName(), Constants.PACKAGE_REL));
        dataModel.setFieldInfoList(fieldInfoList);
        dataModel.setNumberOfFields(fieldInfoList.size());

        return dataModel;
    }

    private List<Field> getAllFields(List<Field> fieldList, Class<?> type) {
        fieldList.addAll(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            getAllFields(fieldList, type.getSuperclass());
        }

        return fieldList;
    }

    private FieldInfo getFieldInfo(Field field) {
        org.springframework.data.mongodb.core.mapping.Field mongoFieldAnnotation = field.getAnnotation(
                org.springframework.data.mongodb.core.mapping.Field.class);

        String databaseFiledName = mongoFieldAnnotation == null ? field.getName() : mongoFieldAnnotation.value();
        String javaFiledName = field.getName();
        String javaSetterName = StringUtils.toUpperCaseFirstLetter(field.getName());
        String filedType = field.getType().getTypeName();
        boolean isPrimitive = field.getType().isPrimitive();

        return new FieldInfo(databaseFiledName, javaFiledName, javaSetterName, filedType, isPrimitive);
    }
}

