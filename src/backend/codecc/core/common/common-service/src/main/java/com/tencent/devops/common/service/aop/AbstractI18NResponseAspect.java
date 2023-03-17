package com.tencent.devops.common.service.aop;

import static com.tencent.devops.common.constant.ComConstants.BLUEKING_LANGUAGE;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.devops.common.api.annotation.I18NFieldMarker;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.service.aop.I18NReflection.FieldMetaData;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
public abstract class AbstractI18NResponseAspect {

    /**
     * 反射缓存
     *
     * @link https://github.com/ben-manes/caffeine/wiki/Eviction-zh-CN
     */
    private static final Cache<Class<?>, I18NReflection> CACHE_CONTAINER =
            Caffeine.newBuilder().maximumSize(100).build();
    private static final Locale DEFAULT_LOCALE = new Locale("zh", "CN");

    // NOCC:MissingJavadocMethod(设计如此:)
    @Pointcut("@annotation(com.tencent.devops.common.api.annotation.I18NResponse)")
    public void i18nResponse() {
    }

    // NOCC:MissingJavadocMethod(设计如此:)
    @Around("i18nResponse()")
    public Object translate(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        Object originReturning = proceedingJoinPoint.proceed();
        String localeString = getLocale().toString();
        log.info("AbstractI18NResponseAspect localeString: {}", localeString);
        if (originReturning == null
                || ObjectUtils.isEmpty(localeString)
                || DEFAULT_LOCALE.toString().equals(localeString)) {
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

        Class<?> unwrapReturningClazz = unwrapReturning.getClass();
        I18NReflection i18NReflection = generateI18NReflection(unwrapReturningClazz);
        if (CollectionUtils.isEmpty(i18NReflection.getFieldMetaDataList())
                || i18NReflection.getMethodAccess() == null) {
            return originReturning;
        }

        List<?> returningList = repackReturning(isResultGeneric, isListGeneric, originReturning);
        extractResourceCode(i18NReflection, returningList);
        addInternationalization(i18NReflection, localeString);
        renderReturning(i18NReflection, returningList);

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
     * 获取语言信息
     *
     * @return
     */
    public static Locale getLocale() {
        // 优先级从高到低: cookie -> header:accept-language
        ServletRequestAttributes requestAttributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        String cookieVal = "";
        if (requestAttributes != null) {
            Cookie[] cookies = requestAttributes.getRequest().getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (BLUEKING_LANGUAGE.equals(cookie.getName())) {
                        cookieVal = cookie.getValue();
                        break;
                    }
                }
            }
        }

        if (!ObjectUtils.isEmpty(cookieVal)) {
            return Locale.forLanguageTag(cookieVal);
        }

        // header:accept-language:zh-cn => java:zh_CN
        String acceptLanguageHeader = requestAttributes.getRequest().getHeader("accept-language");
        if (ObjectUtils.isEmpty(acceptLanguageHeader)) {
            return DEFAULT_LOCALE;
        } else {
            return LocaleContextHolder.getLocale();
        }
    }

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
        for (Field field : allFields) {
            I18NFieldMarker annotation = field.getAnnotation(I18NFieldMarker.class);
            if (annotation != null) {
                boolean isListType = field.getType() == List.class;
                fieldMetaDataList.add(
                        new I18NReflection.FieldMetaData(
                                field.getName(),
                                annotation.keyFieldHolder(),
                                annotation.moduleCode(),
                                isListType,
                                null,
                                null
                        )
                );
            }
        }

        MethodAccess methodAccess = MethodAccess.get(clazz);
        i18NReflection = new I18NReflection(fieldMetaDataList, methodAccess);
        CACHE_CONTAINER.put(clazz, i18NReflection);

        return i18NReflection.getClone();
    }

    /**
     * 根据泛型信息，将原始返回对象拆包，再重新打包为{@codec List<T>}
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
            String getterName = getGetterMethodName(fieldMetaData.getResourceCodeField());
            MethodAccess methodAccess = i18NReflection.getMethodAccess();
            int getterCallIndex = methodAccess.getIndex(getterName);
            String setterName = getSetterMethodName(fieldMetaData.getWillTranslateField());
            int setterCallIndex = methodAccess.getIndex(setterName);

            for (Object obj : objList) {
                Object resourceCode = methodAccess.invoke(obj, getterCallIndex);
                if (resourceCode == null) {
                    continue;
                }

                String key = String.valueOf(resourceCode);
                Map<String, String> keyAndValueMap = fieldMetaData.getKeyAndValueMap();
                String value = CollectionUtils.isEmpty(keyAndValueMap) ? null : keyAndValueMap.get(key);

                if (fieldMetaData.isListType()) {
                    List<String> stringSet = ObjectUtils.isEmpty(value)
                            ? Lists.newArrayList()
                            : Lists.newArrayList(value.split(","));
                    methodAccess.invoke(obj, setterCallIndex, stringSet);
                } else {
                    if (value == null) {
                        value = "";
                    }
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
