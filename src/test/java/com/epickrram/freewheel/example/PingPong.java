package com.epickrram.freewheel.example;

import com.epickrram.freewheel.messaging.MessagingContext;
import com.epickrram.freewheel.messaging.MessagingContextImpl;
import com.epickrram.freewheel.messaging.ptp.EndPoint;
import com.epickrram.freewheel.messaging.ptp.EndPointProvider;
import com.epickrram.freewheel.messaging.ptp.PointToPointMessagingService;
import com.epickrram.freewheel.protocol.CodeBookImpl;
import com.epickrram.freewheel.remoting.ClassNameTopicIdGenerator;
import com.epickrram.freewheel.remoting.PublisherFactory;
import com.epickrram.freewheel.remoting.SubscriberFactory;
import com.epickrram.freewheel.util.Logger;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;

public final class PingPong
{
    private enum Mode
    {
        PING,
        PONG
    }

    private static final Logger LOGGER = Logger.getLogger(PingPong.class);
    private static final int PING_PORT = 16789;
    private static final int PONG_PORT = 16799;

    private final Mode mode;
    private final InetAddress remoteHost;
    private final MessagingContext messagingContext;

    public static void main(String[] args) throws UnknownHostException
    {
        if(args.length != 2)
        {
            System.out.println("Usage: java PingPong [PING|PONG] <remote-host>");
            System.exit(1);
        }

        final Mode mode = Mode.valueOf(args[0].toUpperCase());
        final InetAddress remoteSocket = InetAddress.getByName(args[1]);

        new PingPong(mode, remoteSocket).start();
    }

    private PingPong(final Mode mode, final InetAddress remoteHost)
    {
        this.remoteHost = remoteHost;
        this.mode = mode;
        final CodeBookImpl codeBook = new CodeBookImpl();
        final ClassNameTopicIdGenerator topicIdGenerator = new ClassNameTopicIdGenerator();
        final PointToPointMessagingService messagingService =
                new PointToPointMessagingService(new FixedEndPointProvider(), codeBook, topicIdGenerator);

        messagingContext = new MessagingContextImpl(new PublisherFactory(messagingService, topicIdGenerator, codeBook),
                new SubscriberFactory(), messagingService, topicIdGenerator);
    }

    private void start()
    {
        switch(mode)
        {
            case PING:
                createPing();
                break;
            case PONG:
                createPong();
                break;
        }
        messagingContext.start();
    }

    private void createPong()
    {
        final RespondingPingReceiver respondingPingReceiver = new RespondingPingReceiver(messagingContext);
        messagingContext.createSubscriber(Ping.class, respondingPingReceiver);
        respondingPingReceiver.init();
    }

    private void createPing()
    {
        messagingContext.createSubscriber(Pong.class, new LoggingPongReceiver());
        final Ping ping = messagingContext.createPublisher(Ping.class);
        Executors.newSingleThreadExecutor().submit(new Runnable()
        {
            int count = 0;

            @Override
            public void run()
            {
                pause();
                while(true)
                {
                    try
                    {
                        final String message = "Hello from " + getLocalHostname() + " [" + (++count) + "]";
                        LOGGER.info("Sending " + message);
                        ping.onPing(message);
                    }
                    catch(RuntimeException e)
                    {
                        System.err.println("Caught exception: " + e.getMessage());
                    }
                    pause();
                }
            }
        });
    }

    private void pause()
    {
        try
        {
            Thread.sleep(2500L);
        }
        catch (InterruptedException e)
        {
            // ignore
        }
    }

    private String getLocalHostname()
    {
        try
        {
            return Inet4Address.getLocalHost().getHostName();
        }
        catch (UnknownHostException e)
        {
            throw new RuntimeException("Cannot determine hostname", e);
        }
    }

    interface Ping
    {
        void onPing(final String message);
    }

    interface Pong
    {
        void onPong(final String message);
    }

    private class FixedEndPointProvider implements EndPointProvider
    {
        @Override
        public EndPoint resolveEndPoint(final Class descriptor)
        {
            return descriptor == Ping.class ? new EndPoint(remoteHost, PING_PORT) : new EndPoint(remoteHost, PONG_PORT);
        }
    }

    private class LoggingPongReceiver implements Pong
    {
        @Override
        public void onPong(final String message)
        {
            LOGGER.info("Received pong message: " + message);
        }
    }

    private class RespondingPingReceiver implements Ping
    {
        private Pong pong;
        private final MessagingContext messagingContext;

        public RespondingPingReceiver(final MessagingContext messagingContext)
        {
            this.messagingContext = messagingContext;
        }

        @Override
        public void onPing(final String message)
        {
            LOGGER.info("Received ping message: " + message + ", responding");
            pong.onPong("Hello back, I received: " + message);
        }

        public void init()
        {
            pong = messagingContext.createPublisher(Pong.class);
        }
    }
}