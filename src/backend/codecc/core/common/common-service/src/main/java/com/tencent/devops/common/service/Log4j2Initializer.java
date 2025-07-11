/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.service;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.net.URI;

/**
 * Created by Aaron Sheng on 2017/12/20.
 */
public class Log4j2Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            ConfigurableEnvironment environment = applicationContext.getEnvironment();

            ClassPathResource configFileClasspathRes = new ClassPathResource("log4j2.xml");
            if (configFileClasspathRes.exists()) {
                setConfigLocation(configFileClasspathRes.getURI());
                return;
            }

            ClassPathResource configSpringFileClasspathRes = new ClassPathResource("log4j2-spring.xml");
            if (configSpringFileClasspathRes.exists()) {
                setConfigLocation(configSpringFileClasspathRes.getURI());
                return;
            }

            configLog4j2(environment);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setConfigLocation(URI uri) {
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        loggerContext.setConfigLocation(uri);
    }

    /**
     * 为 BK-Audit 的日志配置
     */
    private void bkAuditConfig(ConfigurationBuilder<BuiltConfiguration> builder, String logPath, String appName) {
        LayoutComponentBuilder layoutBuilder = builder.newLayout("PatternLayout")
                .addAttribute("pattern", "%m%n")
                .addAttribute("charset", "UTF-8");

        AppenderComponentBuilder appender = builder.newAppender("audit-event-appender", "RollingFile")
                .add(layoutBuilder)
                .addComponent(
                        builder.newComponent("Policies")
                                .addComponent(
                                        builder.newComponent("TimeBasedTriggeringPolicy")
                                                .addAttribute("interval", "1")
                                                .addAttribute("modulate", "true")
                                )
                                .addComponent(
                                        builder.newComponent("SizeBasedTriggeringPolicy")
                                                .addAttribute("size", "250 MB")
                                )
                )
                .addComponent(builder.newComponent("DefaultRolloverStrategy").addAttribute("max", "10"))
                .addAttribute("fileName", logPath + appName + "-" + "audit.log")
                .addAttribute("filePattern", logPath + appName + "-" + "audit-%d{yyyy-MM-dd}-%i.log.gz");

        builder.add(appender)
                .add(builder.newLogger("bk_audit", Level.INFO)
                .add(builder.newAppenderRef("audit-event-appender"))
                .addAttribute("additivity", false));
    }

    private void configLog4j2(ConfigurableEnvironment environment) {
        String pattern =
                "%d{yyyy-MM-dd HH:mm:ss.SSS}|%X{ip:--}|%F|%L|%level|%X{err_code:-0}|[%TID]|[%SID]|||[%t] %m%ex%n";
        String appName = environment.getProperty("spring.application.name");

        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        builder.setStatusLevel(Level.ERROR);
        builder.setConfigurationName("Config");
        builder.setPackages(ExceptionAppender.class.getPackage().getName());

        LayoutComponentBuilder consoleLayoutBuilder = builder.newLayout("PatternLayout")
                .addAttribute("pattern", pattern)
                .addAttribute("charset", "UTF-8");

        // console日志输出
        AppenderComponentBuilder appenderBuilder =
                builder.newAppender("Stdout", "CONSOLE").addAttribute("target",
                ConsoleAppender.Target.SYSTEM_OUT)
                .add(consoleLayoutBuilder);
        // 异常日志输出
        AppenderComponentBuilder exceptionAppender = builder.newAppender("Exception", "Exception")
                .add(consoleLayoutBuilder);

        builder.add(appenderBuilder);
        builder.add(exceptionAppender);

        // 过滤hibernate输出
        builder.add(builder.newLogger("org.hibernate", Level.ERROR)
                .add(builder.newAppenderRef("Stdout"))
                .addAttribute("additivity", false));

        // 有指定日志目录才输出日志文件
        String logPath = System.getProperty("service.log.dir");
        if (StringUtils.isEmpty(logPath)) {
            builder.add(builder.newRootLogger(Level.INFO)
                    .add(builder.newAppenderRef("Exception"))
                    .add(builder.newAppenderRef("Stdout")));
        } else {
            LayoutComponentBuilder rollingLayoutBuilder = builder.newLayout("PatternLayout")
                    .addAttribute("pattern", pattern)
                    .addAttribute("charset", "UTF-8");

            ComponentBuilder triggeringPolicy = builder.newComponent("Policies")
                    .addComponent(builder.newComponent("TimeBasedTriggeringPolicy")
                            .addAttribute("interval", "1").addAttribute("modulate", "true"))
                    .addComponent(builder.newComponent("SizeBasedTriggeringPolicy")
                            .addAttribute("size", "250 MB"));

            appenderBuilder = builder.newAppender("Rolling", "RollingFile")
                    .addAttribute("fileName", logPath + appName + ".log")
                    .addAttribute("filePattern", logPath + appName + "-%d{yyyy-MM-dd}-%i.log.gz")
                    .add(rollingLayoutBuilder)
                    .addComponent(builder.newComponent("DefaultRolloverStrategy").addAttribute("max", "10"))
                    .addComponent(triggeringPolicy);
            builder.add(appenderBuilder);

            String levelConfig = System.getProperty("service.log.level");
            Level level = Level.toLevel(levelConfig, Level.INFO);
            String console = System.getProperty("console.log.enabled");

            if (!StringUtils.isEmpty(console) && console.equals("true")) {
                builder.add(builder.newRootLogger(level)
                        .add(builder.newAppenderRef("Exception"))
                        .add(builder.newAppenderRef("Rolling"))
                        .add(builder.newAppenderRef("Stdout")));
            } else {
                builder.add(builder.newRootLogger(level)
                        .add(builder.newAppenderRef("Exception"))
                        .add(builder.newAppenderRef("Rolling")));
            }

            bkAuditConfig(builder, logPath, appName);
        }

        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        loggerContext.start(builder.build());
    }
}
