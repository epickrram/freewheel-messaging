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
package com.epickrram.freewheel.messaging.ptp;

import com.epickrram.freewheel.io.UnpackerDecoderStream;
import com.epickrram.freewheel.messaging.MessagingException;
import com.epickrram.freewheel.messaging.MessagingService;
import com.epickrram.freewheel.messaging.Receiver;
import com.epickrram.freewheel.messaging.ReceiverRegistry;
import com.epickrram.freewheel.protocol.CodeBook;
import com.epickrram.freewheel.remoting.TopicIdGenerator;
import com.epickrram.freewheel.util.IoUtil;
import com.epickrram.freewheel.util.Memoizer;
import com.epickrram.freewheel.util.Provider;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictor;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.ReceiveBufferSizePredictor;
import org.jboss.netty.channel.ReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.msgpack.unpacker.MessagePackUnpacker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public final class PointToPointMessagingService implements MessagingService
{
    private static final Logger LOGGER = Logger.getLogger(PointToPointMessagingService.class.getName());
    private static final int MAX_MESSAGE_SIZE = 64 * 1024;
    private static final long CONNECTION_TIMEOUT_MILLIS = 10000L;

    private final ReceiverRegistry receiverRegistry = new ReceiverRegistry();
    private final EndPointProvider endPointProvider;
    private final CodeBook codeBook;
    private final TopicIdGenerator topicIdGenerator;
    private final Map<Integer, Channel> publisherChannelByTopicIdMap = new ConcurrentHashMap<Integer, Channel>();
    private final List<Channel> subscriberChannels = new CopyOnWriteArrayList<Channel>();
    private final Memoizer<EndPoint, RunnableFuture<Channel>> subscriberChannelMemoizer =
            new Memoizer<EndPoint, RunnableFuture<Channel>>();

    private final List<RunnableFuture<Channel>> subscriberChannelFutures = new CopyOnWriteArrayList<RunnableFuture<Channel>>();
    private final Map<Integer, RunnableFuture<Channel>> publisherChannelFutures = new ConcurrentHashMap<Integer, RunnableFuture<Channel>>();

    private volatile boolean started;

    public PointToPointMessagingService(final EndPointProvider endPointProvider,
                                        final CodeBook codeBook,
                                        final TopicIdGenerator topicIdGenerator)
    {
        this.endPointProvider = endPointProvider;
        this.codeBook = codeBook;
        this.topicIdGenerator = topicIdGenerator;
    }

    @Override
    public <T> void registerPublisher(final Class<T> descriptor)
    {
        if(started)
        {
            throw new MessagingException("Cannot register a publisher after MessagingService has been started");
        }
        createPublisherChannel(endPointProvider.resolveEndPoint(descriptor), topicIdGenerator.getTopicId(descriptor));
    }

    @Override
    public <T> void registerSubscriber(final Class<T> descriptor)
    {
        if(started)
        {
            throw new MessagingException("Cannot register a publisher after MessagingService has been started");
        }
        final EndPoint endPoint = endPointProvider.resolveEndPoint(descriptor);
        createSubscriberChannel(endPoint);
    }

    @Override
    public void send(final int topicId, final ByteArrayOutputStream byteArrayOutputStream) throws MessagingException
    {
        if(!started)
        {
            throw new MessagingException("MessagingService is not yet started");
        }
        final ChannelBuffer buffer = ChannelBuffers.buffer(4 + byteArrayOutputStream.size());
        buffer.writeInt(byteArrayOutputStream.size());
        buffer.writeBytes(byteArrayOutputStream.toByteArray(), 0, byteArrayOutputStream.size());

        final ChannelFuture writeFuture = publisherChannelByTopicIdMap.get(topicId).write(buffer);
        // TODO monitor messaging success in different queue, or implement reliable messaging
    }

    @Override
    public void registerReceiver(final int topicId, final Receiver receiver)
    {
        receiverRegistry.registerReceiver(topicId, receiver);
    }

    @Override
    public void start() throws MessagingException
    {
        for (RunnableFuture<Channel> subscriberBootstrapFuture : subscriberChannelFutures)
        {
            subscriberBootstrapFuture.run();
            try
            {
                subscriberChannels.add(subscriberBootstrapFuture.get());
            }
            catch (InterruptedException e)
            {
                throw new MessagingException("Unable to start subscriber", e);
            }
            catch (ExecutionException e)
            {
                throw new MessagingException("Unable to start subscriber", e);
            }
        }
        for (Map.Entry<Integer, RunnableFuture<Channel>> entry : publisherChannelFutures.entrySet())
        {
            entry.getValue().run();
            try
            {
                final Channel publisherChannel = entry.getValue().get();
                publisherChannelByTopicIdMap.put(entry.getKey(), publisherChannel);
            }
            catch (InterruptedException e)
            {
                throw new MessagingException("Unable to start publisher", e);
            }
            catch (ExecutionException e)
            {
                throw new MessagingException("Unable to start publisher", e);
            }
        }
        started = true;
    }

    @Override
    public void shutdown() throws MessagingException
    {
        if(started)
        {
            for (Channel channel : publisherChannelByTopicIdMap.values())
            {
                IoUtil.close(channel);
            }
            for (Channel channel : subscriberChannels)
            {
                IoUtil.close(channel);
            }
        }
    }

    private void createPublisherChannel(final EndPoint endPoint, final int topicId)
    {
        final ChannelFactory channelFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool());
        final ClientBootstrap bootstrap = new ClientBootstrap(channelFactory);
        bootstrap.setOption("sendBufferSize", MAX_MESSAGE_SIZE);
        bootstrap.setOption("connectTimeoutMillis", CONNECTION_TIMEOUT_MILLIS);
        bootstrap.setPipelineFactory(new ChannelPipelineFactory()
        {
            public ChannelPipeline getPipeline()
            {
                return Channels.pipeline(new SimpleChannelHandler()
                {
                    @Override
                    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception
                    {

                    }

                    @Override
                    public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent e) throws Exception
                    {
                        LOGGER.info("Error connecting to EndPoint: " + e.getCause().getMessage());
                        e.getCause().printStackTrace();
                    }
                });
            }
        });
        final InetSocketAddress remoteAddress = new InetSocketAddress(endPoint.getAddress(), endPoint.getPort());

        LOGGER.info("Publisher connecting to remote address: " + remoteAddress);

        publisherChannelFutures.put(topicId, new FutureTask<Channel>(new Callable<Channel>()
        {
            @Override
            public Channel call() throws Exception
            {
                final AtomicBoolean socketConnectedFlag = new AtomicBoolean(false);

                ChannelFuture publisherChannelFuture = null;
                while (!socketConnectedFlag.get())
                {
                    publisherChannelFuture = bootstrap.connect(remoteAddress);

                    publisherChannelFuture.addListener(new ChannelFutureListener()
                    {
                        @Override
                        public void operationComplete(final ChannelFuture future) throws Exception
                        {
                            LOGGER.info("Channel op complete. Success: " + future.isSuccess() +
                                    ", cancelled: " + future.isCancelled() + ", done: " + future.isDone());
                            if (future.isSuccess())
                            {
                                socketConnectedFlag.set(true);
                            }
                        }
                    });

                    try
                    {
                        LOGGER.info("Waiting for publisher channel");
                        publisherChannelFuture.awaitUninterruptibly();
                        Thread.sleep(1000L);
                    }
                    catch (InterruptedException e)
                    {
                        throw new MessagingException("Failed to wait for publisher channel", e);
                    }

                }
                if (publisherChannelFuture == null)
                {
                    throw new RuntimeException("Unable to connect to remote port");
                }

                return publisherChannelFuture.getChannel();
            }
        }));
    }

    private void createSubscriberChannel(final EndPoint endPoint)
    {
        final ChannelFactory channelFactory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool());
        final ServerBootstrap bootstrap = new ServerBootstrap(channelFactory);
        setSubscriberOptions(bootstrap);

        bootstrap.setPipelineFactory(new ChannelPipelineFactory()
        {
            public ChannelPipeline getPipeline()
            {
                return Channels.pipeline(new SimpleChannelHandler()
                {
                    @Override
                    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception
                    {
                        final ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
                        // TODO handle partial message delivery
                        while(buffer.readableBytes() > 4)
                        {
                            final int messageSize = buffer.readInt();
                            final byte[] messageBuffer = new byte[messageSize];
                            buffer.readBytes(messageBuffer, 0, messageSize);

                            final MessagePackUnpacker messagePackUnpacker = new MessagePackUnpacker(new ByteArrayInputStream(messageBuffer));
                            final UnpackerDecoderStream decoderStream = new UnpackerDecoderStream(codeBook, messagePackUnpacker);
                            final int topicId = decoderStream.readInt();

                            final Collection<Receiver> receiverList = receiverRegistry.getReceiverList(topicId);
                            for (Receiver receiver : receiverList)
                            {
                                final MessagePackUnpacker innerMessagePackUnpacker = new MessagePackUnpacker(new ByteArrayInputStream(messageBuffer));
                                final UnpackerDecoderStream innerDecoderStream = new UnpackerDecoderStream(codeBook, innerMessagePackUnpacker);
                                innerDecoderStream.readInt();
                                receiver.onMessage(topicId, innerDecoderStream);
                            }
                        }
                    }
                });
            }
        });
        final InetSocketAddress subscriberAddress = new InetSocketAddress(endPoint.getPort());

        final RunnableFuture<Channel> subscriberChannelFuture =
                subscriberChannelMemoizer.getValue(endPoint, new Provider<EndPoint, RunnableFuture<Channel>>()
        {
            @Override
            public RunnableFuture<Channel> provide(final EndPoint key)
            {
                return new FutureTask<Channel>(new Callable<Channel>()
                {
                    @Override
                    public Channel call() throws Exception
                    {
                        LOGGER.info("Creating Subscriber for address: " + subscriberAddress);
                        return bootstrap.bind(subscriberAddress);
                    }
                });
            }
        });

        subscriberChannelFutures.add(subscriberChannelFuture);
    }

    private void setSubscriberOptions(final ServerBootstrap bootstrap)
    {
        bootstrap.setOption("receiveBufferSize", MAX_MESSAGE_SIZE);
        bootstrap.setOption("receiveBufferSizePredictor", new FixedReceiveBufferSizePredictor(MAX_MESSAGE_SIZE));
        bootstrap.setOption("child.receiveBufferSizePredictor", new FixedReceiveBufferSizePredictor(MAX_MESSAGE_SIZE));
        bootstrap.setOption("receiveBufferSizePredictor", new FixedReceiveBufferSizePredictor(MAX_MESSAGE_SIZE));
        bootstrap.setOption("child.receiveBufferSizePredictor", new FixedReceiveBufferSizePredictor(MAX_MESSAGE_SIZE));
        bootstrap.setOption("receiveBufferSizePredictorFactory", new ReceiveBufferSizePredictorFactory()
        {
            @Override
            public ReceiveBufferSizePredictor getPredictor() throws Exception
            {
                return new FixedReceiveBufferSizePredictor(MAX_MESSAGE_SIZE);
            }
        });

        bootstrap.setOption("child.receiveBufferSizePredictorFactory", new ReceiveBufferSizePredictorFactory()
        {
            @Override
            public ReceiveBufferSizePredictor getPredictor() throws Exception
            {
                return new FixedReceiveBufferSizePredictor(MAX_MESSAGE_SIZE);
            }
        });
    }
}