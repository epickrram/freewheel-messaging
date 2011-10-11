package com.epickrram.freewheel.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public final class LogFormatter extends Formatter
{
    @Override
    public String format(final LogRecord record)
    {
        final StringBuilder builder = new StringBuilder();
        builder.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(record.getMillis())));
        builder.append(" [").append(record.getThreadID()).append("] ").append(record.getLevel().getName());
        builder.append(" [").append(record.getLoggerName()).append("] ").append(record.getMessage());
        if(record.getThrown() != null)
        {
            builder.append('\n');
            record.getThrown().printStackTrace(new PrintWriter(new StringBuilderWriter(builder)));
        }
        return builder.append('\n').toString();
    }

    private class StringBuilderWriter extends Writer
    {
        private final StringBuilder builder;

        public StringBuilderWriter(final StringBuilder builder)
        {
            this.builder = builder;
        }

        @Override
        public void write(final char[] cbuf, final int off, final int len) throws IOException
        {
            builder.append(cbuf, off, len);
        }

        @Override
        public void flush() throws IOException
        {
            // no-op
        }

        @Override
        public void close() throws IOException
        {
            // no-op
        }
    }
}
