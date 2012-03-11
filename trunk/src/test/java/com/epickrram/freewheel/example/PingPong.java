//////////////////////////////////////////////////////////////////////////////////
//   Copyright 2011   Mark Price     mark at epickrram.com                      //
//                                                                              //
//   Licensed under the Apache License, Version 2.0 (the "License");            //
//   you may not use this file except in compliance with the License.           //
//   You may obtain a copy of the License at                                    //
//                                                                              //
//       http://www.apache.org/licenses/LICENSE-2.0                             //
//                                                                              //
//   Unless required by applicable law or agreed to in writing, software        //
//   distributed under the License is distributed on an "AS IS" BASIS,          //
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   //
//   See the License for the specific language governing permissions and        //
//   limitations under the License.                                             //
//////////////////////////////////////////////////////////////////////////////////
package com.epickrram.freewheel.example;

import com.epickrram.freewheel.messaging.MessagingContext;
import com.epickrram.freewheel.messaging.MessagingContextImpl;
import com.epickrram.freewheel.messaging.ptp.EndPoint;
import com.epickrram.freewheel.messaging.ptp.EndPointProvider;
import com.epickrram.freewheel.messaging.ptp.PointToPointMessagingService;
import com.epickrram.freewheel.protocol.CodeBookImpl;
import com.epickrram.freewheel.remoting.ClassNameTopicIdGenerator;
import com.epickrram.freewheel.remoting.DirectPublisherFactory;
import com.epickrram.freewheel.remoting.SubscriberFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public final class PingPong
{
    private enum Mode
    {
        PING,
        PONG
    }

    private static final Logger LOGGER = Logger.getLogger(PingPong.class.getName());
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

        messagingContext = new MessagingContextImpl(new DirectPublisherFactory(messagingService, topicIdGenerator, codeBook),
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
        final LoggingPongReceiver loggingPongReceiver = new LoggingPongReceiver();
        messagingContext.createSubscriber(Pong.class, loggingPongReceiver);
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
                        final int messageNumber = ++count;
                        loggingPongReceiver.sending(messageNumber);
                        ping.onPing(messageNumber);
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

    interface Ping
    {
        void onPing(final int messageNumber);
    }

    interface Pong
    {
        void onPong(final int messageNumber);
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
        private final Map<Integer, Long> messageSendTimestampMap = new ConcurrentHashMap<Integer, Long>();

        @Override
        public void onPong(final int messageNumber)
        {
            final Long startTimestamp = messageSendTimestampMap.get(messageNumber);
            LOGGER.info("Received message " + messageNumber + ", RTT " + (System.currentTimeMillis() - startTimestamp) + "ms");
        }

        public void sending(final int messageNumber)
        {
            messageSendTimestampMap.put(messageNumber, System.currentTimeMillis());
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
        public void onPing(final int messageNumber)
        {
            pong.onPong(messageNumber);
        }

        public void init()
        {
            pong = messagingContext.createPublisher(Pong.class);
        }
    }
}