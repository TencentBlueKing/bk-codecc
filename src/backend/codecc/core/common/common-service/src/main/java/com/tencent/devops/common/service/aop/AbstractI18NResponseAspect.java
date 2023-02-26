package com.tencent.devops.common.service.aop;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.devops.common.api.annotation.I18NFieldMarker;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.service.aop.I18NReflection.FieldMetaData;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.CollectionUtils;

@Slf4j
public abstract class AbstractI18NResponseAspect {

    private final static Cache<Class<?>, I18NReflection> CACHE_CONTAINER =
            Caffeine.newBuilder().maximumSize(100).build();
    private final static String DEFAULT_LOCALE = "zh_CN";

    @Pointcut("@annotation(com.tencent.devops.common.api.annotation.I18NResponse)")
    public void i18nResponse() {
    }

    @Around("i18nResponse()")
    public Object translate(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        long beginTime = System.currentTimeMillis();
        Object originReturning = proceedingJoinPoint.proceed();
        log.info("AbstractI18NResponseAspect proceedingJoinPoint.proceed() {}", System.currentTimeMillis() - beginTime);

        beginTime = System.currentTimeMillis();
        // http protocol:accept-language:en-US => java:en_US
        String localeString = LocaleContextHolder.getLocale().toString();
        if (originReturning == null || DEFAULT_LOCALE.equals(localeString)) {
            return originReturning;
        }

        Object unwrapReturning = originReturning;
        boolean isResultGeneric = false;
        boolean isListGeneric = false;

        if (unwrapReturning instanceof Result<?>) {
            unwrapReturning = ((Result<?>) unwrapReturning).getData();
            if (unwrapReturning == null) {
                return originReturning;
            }

            isResultGeneric = true;
        }

        if (unwrapReturning instanceof List<?>) {
            List<?> listObj = (List<?>) unwrapReturning;
            if (listObj.size() == 0) {
                return originReturning;
            }

            unwrapReturning = listObj.get(0);
            isListGeneric = true;
        }

        log.info("AbstractI18NResponseAspect unwrap returning {}", System.currentTimeMillis() - beginTime);

        beginTime = System.currentTimeMillis();
        Class<?> unwrapReturningClazz = unwrapReturning.getClass();
        I18NReflection i18NReflection = generateI18NReflection(unwrapReturningClazz);
        log.info("AbstractI18NResponseAspect generateI18NReflection {}", System.currentTimeMillis() - beginTime);

        if (CollectionUtils.isEmpty(i18NReflection.getFieldMetaDataList())
                || i18NReflection.getMethodAccess() == null) {
            return originReturning;
        }

        beginTime = System.currentTimeMillis();
        List<?> returningList = repackReturning(isResultGeneric, isListGeneric, originReturning);
        log.info("AbstractI18NResponseAspect repackReturning {}", System.currentTimeMillis() - beginTime);

        beginTime = System.currentTimeMillis();
        extractResourceCode(i18NReflection, returningList);
        log.info("AbstractI18NResponseAspect extractResourceCode {}", System.currentTimeMillis() - beginTime);

        beginTime = System.currentTimeMillis();
        addInternationalization(i18NReflection, localeString);
        log.info("AbstractI18NResponseAspect addInternationalization {}", System.currentTimeMillis() - beginTime);

        beginTime = System.currentTimeMillis();
        renderReturning(i18NReflection, returningList);
        log.info("AbstractI18NResponseAspect renderReturning {}", System.currentTimeMillis() - beginTime);

        return originReturning;
    }

    /**
     * 添加国际化信息
     *
     * @param i18NReflection
     * @param localeString
     */
    public abstract void addInternationalization(I18NReflection i18NReflection, String localeString);

    /**
     * 生成反射元数据
     *
     * @param clazz
     * @return
     */
    private I18NReflection generateI18NReflection(Class<?> clazz) {
        I18NReflection i18NReflection = CACHE_CONTAINER.getIfPresent(clazz);
        if (i18NReflection != null) {
            log.info("AbstractI18NResponseAspect generateI18NReflection on cached");
            return i18NReflection.getClone();
        }

        List<Field> allFields = getAllFields(new ArrayList<>(), clazz);
        List<FieldMetaData> fieldMetaDataList = Lists.newArrayList();
        Map<String, List<FieldMetaData>> map = Maps.newHashMap();
        i18NReflection = new I18NReflection(fieldMetaDataList, MethodAccess.get(clazz));

        for (Field field : allFields) {
            I18NFieldMarker annotation = field.getAnnotation(I18NFieldMarker.class);
            if (annotation != null) {
                fieldMetaDataList.add(
                        new I18NReflection.FieldMetaData(
                                field.getName(),
                                annotation.keyFieldHolder(),
                                annotation.moduleCode(),
                                null,
                                null
                        )
                );
            }
        }

        CACHE_CONTAINER.put(clazz, i18NReflection);
        log.info("AbstractI18NResponseAspect generateI18NReflection generated");

        return i18NReflection.getClone();
    }

    /**
     * 根据泛型信息，将原始返回对象拆包，再重新打包为List<TargetObject>
     *
     * @param isResultGeneric
     * @param isListGeneric
     * @param originReturning
     * @return
     */
    private List<?> repackReturning(boolean isResultGeneric, boolean isListGeneric, Object originReturning) {
        if (isListGeneric && isResultGeneric) {
            return (List<?>) (((Result<?>) originReturning).getData());
        } else if (isListGeneric) {
            return (List<?>) originReturning;
        } else if (isResultGeneric) {
            Object data = ((Result<?>) originReturning).getData();
            return Lists.newArrayList(data);
        } else {
            return Lists.newArrayList(originReturning);
        }
    }

    /**
     * 提取资源编码
     *
     * @param i18NReflection
     * @param objList
     */
    private void extractResourceCode(I18NReflection i18NReflection, List<?> objList) {
        for (FieldMetaData fieldMetaData : i18NReflection.getFieldMetaDataList()) {
            if (fieldMetaData.getKeySet() == null) {
                fieldMetaData.setKeySet(Sets.newHashSetWithExpectedSize(objList.size()));
            }

            String getterName = getGetterMethodName(fieldMetaData.getResourceCodeField());
            MethodAccess methodAccess = i18NReflection.getMethodAccess();
            int getterCallIndex = methodAccess.getIndex(getterName);

            for (Object obj : objList) {
                Object resourceCode = methodAccess.invoke(obj, getterCallIndex);
                if (resourceCode != null) {
                    fieldMetaData.getKeySet().add(String.valueOf(resourceCode));
                }
            }
        }
    }

    /**
     * 渲染结果
     *
     * @param i18NReflection
     * @param objList
     */
    private void renderReturning(I18NReflection i18NReflection, List<?> objList) {
        for (FieldMetaData fieldMetaData : i18NReflection.getFieldMetaDataList()) {
            if (CollectionUtils.isEmpty(fieldMetaData.getKeyAndValueMap())) {
                continue;
            }

            String getterName = getGetterMethodName(fieldMetaData.getResourceCodeField());
            MethodAccess methodAccess = i18NReflection.getMethodAccess();
            int getterCallIndex = methodAccess.getIndex(getterName);
            String setterName = getSetterMethodName(fieldMetaData.getWillTranslateField());
            int setterCallIndex = methodAccess.getIndex(setterName);

            for (Object obj : objList) {
                Object resourceCode = methodAccess.invoke(obj, getterCallIndex);
                if (resourceCode != null) {
                    String value = fieldMetaData.getKeyAndValueMap().get(String.valueOf(resourceCode));
                    methodAccess.invoke(obj, setterCallIndex, value);
                }
            }
        }
    }

    private String getSetterMethodName(String fieldName) {
        return String.format("set%s", toUpperCaseFirstLetter(fieldName));
    }

    private String getGetterMethodName(String fieldName) {
        return String.format("get%s", toUpperCaseFirstLetter(fieldName));
    }

    private String toUpperCaseFirstLetter(String word) {
        if (word == null || word.length() < 1) {
            return word;
        }

        return word.substring(0, 1).toUpperCase() + word.substring(1);
    }

    private List<Field> getAllFields(List<Field> fieldList, Class<?> clazz) {
        fieldList.addAll(Arrays.asList(clazz.getDeclaredFields()));

        if (clazz.getSuperclass() != null) {
            getAllFields(fieldList, clazz.getSuperclass());
        }

        return fieldList;
    }
}
