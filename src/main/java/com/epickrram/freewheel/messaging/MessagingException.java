package com.epickrram.freewheel.messaging;

public final class MessagingException extends RuntimeException
{
    public MessagingException(final String message)
    {
        super(message);
    }

    public MessagingException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}