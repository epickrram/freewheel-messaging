package com.epickrram.freewheel.example;

import java.net.UnknownHostException;

public final class LocalRunner
{
    public static void main(String[] args) throws UnknownHostException, InterruptedException
    {
        final Thread pongThread = startThread("PONG", "127.0.0.1");
        final Thread pingThread = startThread("PING", "127.0.0.1");

        pongThread.join();
        pingThread.join();
    }

    private static Thread startThread(final String mode, final String address)
    {
        final Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    PingPong.main(new String[]{mode, address});
                }
                catch (UnknownHostException e)
                {
                    // ignore
                }
            }
        });
        thread.setName(mode);
        thread.start();
        return thread;
    }
}
