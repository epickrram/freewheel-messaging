package com.epickrram.freewheel.protocol;

public final class TranslatorException extends RuntimeException
{
    public TranslatorException(final String message)
    {
        super(message);
    }

    public TranslatorException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}