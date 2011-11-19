/*
Copyright 2011 Mark Price

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.epickrram.freewheel.util;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;

public final class Logger
{
    public static final LogFormatter LOG_FORMATTER = new LogFormatter();
    private final java.util.logging.Logger delegate;

    public Logger(final java.util.logging.Logger delegate)
    {
        this.delegate = delegate;
    }

    public void debug(final String message)
    {
        delegate.log(Level.FINE, message);
    }

    public void info(final String message)
    {
        delegate.log(Level.INFO, message);
    }

    public void warn(final String message)
    {
        delegate.warning(message);
    }

    public void error(final String message, final Throwable throwable)
    {
        delegate.log(Level.SEVERE, message, throwable);
    }

    public static Logger getLogger(final Class cls)
    {
        final java.util.logging.Logger delegate = java.util.logging.Logger.getLogger(cls.getSimpleName());
        return new Logger(delegate);
    }
}