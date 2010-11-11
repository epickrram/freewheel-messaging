package com.epickrram.remoting;

public final class RemotingException extends RuntimeException
{
    public RemotingException(final String message)
    {
        super(message);
    }

    public RemotingException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
