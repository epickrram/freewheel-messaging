package com.epickrram.freewheel.util;

import org.jboss.netty.channel.Channel;

import java.io.Closeable;
import java.io.IOException;

public final class IoUtil
{
    private IoUtil() {}

    public static void close(final Closeable closeable)
    {
        if(closeable != null)
        {
            try
            {
                closeable.close();
            }
            catch(IOException e)
            {
                // ignore
            }
        }
    }

    public static void close(final Channel channel)
    {
        if(channel != null)
        {
            channel.close();
        }
    }
}
