package com.tencent.devops.common.service;

import io.opentelemetry.api.trace.Span;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;

/**
 * SpanID 服务内的链路ID，也是局部链路ID
 */
@Plugin(name = "SIDPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"SID"})
public class SIDContextConverter extends LogEventPatternConverter {

    private static final SIDContextConverter INSTANCE = new SIDContextConverter();

    protected SIDContextConverter() {
        super("SID", "SID");
    }

    public static SIDContextConverter newInstance(final String[] options) {
        return INSTANCE;
    }

    @Override
    public void format(LogEvent event, StringBuilder toAppendTo) {
        toAppendTo.append(Span.current().getSpanContext().getSpanId());
    }
}
