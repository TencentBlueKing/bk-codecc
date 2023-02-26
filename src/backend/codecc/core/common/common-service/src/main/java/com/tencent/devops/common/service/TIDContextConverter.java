package com.tencent.devops.common.service;

import io.opentelemetry.api.trace.Span;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;

@Plugin(name = "TIDPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({ "TID" })
public class TIDContextConverter extends LogEventPatternConverter {

    private static final TIDContextConverter INSTANCE = new TIDContextConverter();

    public static TIDContextConverter newInstance(final String[] options) {
        return INSTANCE;
    }

    protected TIDContextConverter() {
        super("TID", "TID");
    }

    @Override
    public void format(LogEvent event, StringBuilder toAppendTo) {
        toAppendTo.append(Span.current().getSpanContext().getTraceId());
    }
}
