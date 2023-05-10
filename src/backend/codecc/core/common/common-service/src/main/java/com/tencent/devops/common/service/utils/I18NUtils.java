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

    private static Locale ZH_CN = new Locale("zh", "CN");
    private static Locale EN_US = new Locale("en", "US");


    /**
     * 获取国际化信息
     *
     * @param resourceCode
     * @return
     */
    public static String getMessage(String resourceCode) {
        return getMessage(resourceCode, null);
    }

    /**
     * 获取国际化信息
     *
     * @param resourceCode
     * @param locale
     * @return
     */
    public static String getMessage(String resourceCode, Locale locale) {
        try {
            if (locale == null) {
                locale = AbstractI18NResponseAspect.getLocale();
            }

            ResourceBundle resourceBundle = ResourceBundle.getBundle("i18n/message", locale);

            return resourceBundle.getString(resourceCode);
        } catch (Throwable t) {
            log.error("i18n util get message error, resource code: {}, locale: {}", resourceCode, locale, t);

            return "I18N_ERR";
        }
    }

    /**
     * 获取国际化信息
     *
     * @param resourceCode
     * @param params
     * @return
     */
    public static String getMessageWithParams(String resourceCode, String[] params) {
        if (params == null) {
            params = new String[0];
        }

        String message = getMessage(resourceCode);
        if (ObjectUtils.isEmpty(message)) {
            return "";
        }

        try {
            return MessageFormat.format(message, params);
        } catch (Throwable t) {
            log.error("i18n format message error, code: {}, params: {}", resourceCode, params, t);

            return "I18N_ERR";
        }
    }

    public static ArrayList<String> getAllLocaleMessage(String resourceCode) {
        return new ArrayList<String>() {{
            add(getMessage(resourceCode, ZH_CN));
            add(getMessage(resourceCode, EN_US));
        }};
    }
}
