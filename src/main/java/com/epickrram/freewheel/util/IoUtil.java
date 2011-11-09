package com.epickrram.freewheel.util;

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
}
