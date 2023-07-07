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
import com.tencent.devops.common.service.utils.I18NUtils;
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
import org.springframework.data.domain.Page;
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

    // NOCC:MissingJavadocMethod(设计如此:)
    @Pointcut("@annotation(com.tencent.devops.common.api.annotation.I18NResponse)")
    public void i18nResponse() {
    }

    // NOCC:MissingJavadocMethod(设计如此:)
    @Around("i18nResponse()")
    public Object translate(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        Object originReturning = proceedingJoinPoint.proceed();
        Locale locale = getLocale();
        log.info("AbstractI18NResponseAspect localeString: {}", locale);
        if (originReturning == null || locale.getLanguage().equals("zh")) {
            return originReturning;
        }

        Object unwrapReturning = originReturning;
        boolean isResultGeneric = false;
        boolean isListGeneric = false;
        boolean isPageGeneric = false;

        if (unwrapReturning instanceof Result<?>) {
            unwrapReturning = ((Result<?>) unwrapReturning).getData();
            if (unwrapReturning == null) {
                return originReturning;
            }

            isResultGeneric = true;
        }

        if (unwrapReturning instanceof Page<?>) {
            List<?> pageObj = ((Page<?>) unwrapReturning).getContent();
            if (pageObj == null || pageObj.size() == 0) {
                return originReturning;
            }

            unwrapReturning = pageObj.get(0);
            isPageGeneric = true;
        } else if (unwrapReturning instanceof List<?>) {
            List<?> listObj = (List<?>) unwrapReturning;
            if (listObj == null || listObj.size() == 0) {
                return originReturning;
            }

            unwrapReturning = listObj.get(0);
            isListGeneric = true;
        }

        Class<?> unwrapReturningClazz = unwrapReturning.getClass();
        I18NReflection i18nReflection = generateI18NReflection(unwrapReturningClazz);
        if (CollectionUtils.isEmpty(i18nReflection.getFieldMetaDataList())
                || i18nReflection.getMethodAccess() == null) {
            return originReturning;
        }

        List<?> returningList = repackReturning(isResultGeneric, isListGeneric, isPageGeneric, originReturning);
        extractResourceCode(i18nReflection, returningList);

        // 校验提取的资源编码
        if (i18nReflection.getFieldMetaDataList() == null
                || i18nReflection.getFieldMetaDataList().stream()
                .allMatch(x -> CollectionUtils.isEmpty(x.getKeySet()))) {
            return originReturning;
        }

        addInternationalization(i18nReflection, locale);
        renderReturning(i18nReflection, returningList);

        return originReturning;
    }

    /**
     * 添加国际化信息
     *
     * @param i18nReflection
     * @param locale
     */
    public abstract void addInternationalization(I18NReflection i18nReflection, Locale locale);

    /**
     * 获取语言信息
     *
     * @return
     */
    public static Locale getLocale() {
        // 优先级从高到低: cookie -> header:accept-language
        ServletRequestAttributes requestAttributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (requestAttributes == null) {
            log.info("servlet request is null, return default locale");
            return I18NUtils.EN;
        }

        String cookieVal = "";
        Cookie[] cookies = requestAttributes.getRequest().getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (BLUEKING_LANGUAGE.equals(cookie.getName())) {
                    cookieVal = cookie.getValue();
                    break;
                }
            }
        }

        if (!ObjectUtils.isEmpty(cookieVal)) {
            return Locale.forLanguageTag(cookieVal);
        }

        // (http:accept-language:zh-cn) => (java:zh_CN)
        String acceptLanguageHeader = requestAttributes.getRequest().getHeader("accept-language");
        if (ObjectUtils.isEmpty(acceptLanguageHeader)) {
            log.info("accept language header is null, return default locale");
            return I18NUtils.EN;
        } else {
            Locale locale = LocaleContextHolder.getLocale();
            log.info("get locale from accept language: {}", locale);

            // 中英均不是，则返回默认
            if (!"en".equalsIgnoreCase(locale.getLanguage()) && !"zh".equalsIgnoreCase(locale.getLanguage())) {
                return I18NUtils.EN;
            }

            return locale;
        }
    }

    /**
     * 生成反射元数据
     *
     * @param clazz
     * @return
     */
    private I18NReflection generateI18NReflection(Class<?> clazz) {
        I18NReflection i18nReflection = CACHE_CONTAINER.getIfPresent(clazz);
        if (i18nReflection != null) {
            log.info("AbstractI18NResponseAspect generateI18NReflection on cached");
            return i18nReflection.getClone();
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
        i18nReflection = new I18NReflection(fieldMetaDataList, methodAccess);
        CACHE_CONTAINER.put(clazz, i18nReflection);

        return i18nReflection.getClone();
    }

    /**
     * 根据泛型信息，将原始返回对象拆包，再重新打包为{@codec List<T>}
     *
     * @param isResultGeneric
     * @param isListGeneric
     * @param originReturning
     * @return
     */
    private List<?> repackReturning(
            boolean isResultGeneric,
            boolean isListGeneric,
            boolean isPageGeneric,
            Object originReturning
    ) {
        if (isResultGeneric) {
            originReturning = ((Result<?>) originReturning).getData();
        }

        if (isPageGeneric) {
            return ((Page<?>) originReturning).getContent();
        } else if (isListGeneric) {
            return (List<?>) originReturning;
        } else {
            return Lists.newArrayList(originReturning);
        }
    }

    /**
     * 提取资源编码
     *
     * @param i18nReflection
     * @param objList
     */
    private void extractResourceCode(I18NReflection i18nReflection, List<?> objList) {
        for (FieldMetaData fieldMetaData : i18nReflection.getFieldMetaDataList()) {
            if (fieldMetaData.getKeySet() == null) {
                fieldMetaData.setKeySet(Sets.newHashSetWithExpectedSize(objList.size()));
            }

            String getterName = getGetterMethodName(fieldMetaData.getResourceCodeField());
            MethodAccess methodAccess = i18nReflection.getMethodAccess();
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
     * @param i18nReflection
     * @param objList
     */
    private void renderReturning(I18NReflection i18nReflection, List<?> objList) {
        for (FieldMetaData fieldMetaData : i18nReflection.getFieldMetaDataList()) {
            MethodAccess methodAccess = i18nReflection.getMethodAccess();
            int resourceCodeGetterIndex =
                    methodAccess.getIndex(getGetterMethodName(fieldMetaData.getResourceCodeField()));
            int translateFieldSetterIndex =
                    methodAccess.getIndex(getSetterMethodName(fieldMetaData.getWillTranslateField()));

            for (Object obj : objList) {
                Object resourceCode = methodAccess.invoke(obj, resourceCodeGetterIndex);
                if (resourceCode == null) {
                    continue;
                }

                String key = String.valueOf(resourceCode);
                Map<String, String> keyAndValueMap = fieldMetaData.getKeyAndValueMap();
                String value = CollectionUtils.isEmpty(keyAndValueMap) ? null : keyAndValueMap.get(key);

                // 若翻译不为空，才进行替换；否则保持原值
                if (!ObjectUtils.isEmpty(value)) {
                    if (fieldMetaData.isListType()) {
                        List<String> strList = Lists.newArrayList(value.split(","));
                        methodAccess.invoke(obj, translateFieldSetterIndex, strList);
                    } else {
                        methodAccess.invoke(obj, translateFieldSetterIndex, value);
                    }
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
