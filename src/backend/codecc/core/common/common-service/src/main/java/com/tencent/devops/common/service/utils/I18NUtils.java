package com.tencent.devops.common.service.utils;

import com.tencent.devops.common.service.aop.AbstractI18NResponseAspect;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

@Slf4j
public class I18NUtils {

    private static final Locale ZH_CN = new Locale("zh", "CN");
    private static final Locale EN_US = new Locale("en", "US");
    private static final String I18N_ERROR_MESSAGE = "[I18N_ERR]";

    /**
     * 获取国际化信息
     *
     * @param resourceCode
     * @return
     */
    public static String getMessage(String resourceCode) {
        return getMessageCore(resourceCode, null, null);
    }

    /**
     * 获取国际化信息
     *
     * @param resourceCode
     * @param locale
     * @return
     */
    public static String getMessage(String resourceCode, Locale locale) {
        return getMessageCore(resourceCode, null, locale);
    }

    /**
     * 获取国际化信息
     *
     * @param resourceCode
     * @param params
     * @return
     */
    public static String getMessageWithParams(String resourceCode, String[] params) {
        return getMessageCore(resourceCode, params, null);
    }

    /**
     * 获取国际化信息
     *
     * @param resourceCode
     * @param params
     * @param defaultMessage
     * @return
     */
    public static String getMessageWithParams(String resourceCode, String[] params, String defaultMessage) {
        String message = getMessageWithParams(resourceCode, params);

        if (I18N_ERROR_MESSAGE.equals(message) || ObjectUtils.isEmpty(message)) {
            return defaultMessage;
        }

        return message;
    }

    public static ArrayList<String> getAllLocaleMessage(String resourceCode) {
        return new ArrayList<String>() {{
            add(getMessage(resourceCode, ZH_CN));
            add(getMessage(resourceCode, EN_US));
        }};
    }

    private static String getMessageCore(String resourceCode, String[] params, Locale locale) {
        if (ObjectUtils.isEmpty(resourceCode)) {
            return "";
        }

        try {
            if (locale == null) {
                locale = AbstractI18NResponseAspect.getLocale();
            }

            ResourceBundle resourceBundle = ResourceBundle.getBundle("i18n/message", locale);

            if (!ObjectUtils.isEmpty(params)) {
                return MessageFormat.format(resourceBundle.getString(resourceCode), params);
            }

            return resourceBundle.getString(resourceCode);
        } catch (Throwable t) {
            log.error("i18n get message error, resource code: {}, params: {}, locale: {}",
                    resourceCode, params, locale, t);

            return I18N_ERROR_MESSAGE;
        }
    }
}
