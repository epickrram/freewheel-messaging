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

import com.epickrram.freewheel.io.DecoderStream;
import com.epickrram.freewheel.messaging.MessagingException;
import com.epickrram.freewheel.messaging.MessagingService;
import com.epickrram.freewheel.messaging.Receiver;
import com.epickrram.freewheel.messaging.ReceiverRegistry;
import com.epickrram.freewheel.protocol.CodeBook;
import com.epickrram.freewheel.remoting.TopicIdGenerator;
import com.epickrram.freewheel.util.Creator;
import com.epickrram.freewheel.util.IoUtil;
import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictor;
import org.jboss.netty.channel.ReceiveBufferSizePredictor;
import org.jboss.netty.channel.ReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import static com.epickrram.freewheel.util.ConcurrentMapIdiom.getOrCreateFromConcurrentMap;

public final class PointToPointMessagingService implements MessagingService
{
    private static final int MAX_MESSAGE_SIZE = 64 * 1024;
    private static final long CONNECTION_TIMEOUT_MILLIS = 10000L;

    private final ReceiverRegistry receiverRegistry = new ReceiverRegistry();
    private final EndPointProvider endPointProvider;
    private final CodeBook codeBook;
    private final TopicIdGenerator topicIdGenerator;
    private final Map<Integer, Channel> publisherChannelByTopicIdMap = new ConcurrentHashMap<Integer, Channel>();
    private final List<Channel> subscriberChannels = new CopyOnWriteArrayList<Channel>();
    private final ConcurrentMap<Integer, RunnableFuture<Channel>> publisherChannelFutures = new ConcurrentHashMap<Integer, RunnableFuture<Channel>>();
    private final ConcurrentMap<Integer, RunnableFuture<Channel>> subscriberChannelFutures = new ConcurrentHashMap<Integer, RunnableFuture<Channel>>();
    private final ExecutorService messagingThreadPool;

    private volatile boolean started;

    public PointToPointMessagingService(final EndPointProvider endPointProvider,
                                        final CodeBook codeBook,
                                        final TopicIdGenerator topicIdGenerator)
    {
        this.endPointProvider = endPointProvider;
        this.codeBook = codeBook;
        this.topicIdGenerator = topicIdGenerator;
        messagingThreadPool = Executors.newCachedThreadPool();
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
        createSubscriberChannel(endPointProvider.resolveEndPoint(descriptor), topicIdGenerator.getTopicId(descriptor));
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
    public DecoderStream sendAndWait(final int topicId, final ByteArrayOutputStream byteArrayOutputStream) throws MessagingException
    {
        throw new IllegalStateException(getClass().getSimpleName() + " does not support sendAndWait");
    }

    @Override
    public boolean supportsSendAndWait()
    {
        return false;
    }

    @Override
    public void registerReceiver(final int topicId, final Receiver receiver)
    {
        receiverRegistry.registerReceiver(topicId, receiver);
    }

    @Override
    public void start() throws MessagingException
    {
        startSubscriberChannels();
        startPublisherChannels();
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
            messagingThreadPool.shutdownNow();
        }
    }

    private void startPublisherChannels()
    {
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
    }

    private void startSubscriberChannels()
    {
        for (RunnableFuture<Channel> subscriberBootstrapFuture : subscriberChannelFutures.values())
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
    }

    private void createPublisherChannel(final EndPoint endPoint, final int topicId)
    {
        final ChannelFactory channelFactory = new NioClientSocketChannelFactory(messagingThreadPool, messagingThreadPool);
        final ClientBootstrap bootstrap = new ClientBootstrap(channelFactory);
        setPublisherOptions(bootstrap);
        setHandler(bootstrap, new PublisherChannelHandler());
        getOrCreateFromConcurrentMap(publisherChannelFutures, new Creator<RunnableFuture<Channel>>()
        {
            @Override
            public RunnableFuture<Channel> create()
            {
                return new FutureTask<Channel>(new PublisherChannelCallable(bootstrap, endPoint.toSocketAddress()));
            }
        }, topicId);
    }

    private void createSubscriberChannel(final EndPoint endPoint, final int topicId)
    {
        final ChannelFactory channelFactory = new NioServerSocketChannelFactory(messagingThreadPool, messagingThreadPool);
        final ServerBootstrap bootstrap = new ServerBootstrap(channelFactory);
        setSubscriberOptions(bootstrap);
        setHandler(bootstrap, new SubscriberChannelHandler(codeBook, receiverRegistry));
        getOrCreateFromConcurrentMap(subscriberChannelFutures, new Creator<RunnableFuture<Channel>>()
        {
            @Override
            public RunnableFuture<Channel> create()
            {
                return new FutureTask<Channel>(new SubscriberChannelCallable(bootstrap, endPoint.toLocalSocketAddress()));
            }
        }, topicId);
    }

    private void setHandler(final Bootstrap bootstrap, final ChannelHandler handler)
    {
        bootstrap.setPipelineFactory(new ChannelPipelineFactory()
        {
            public ChannelPipeline getPipeline()
            {
                return Channels.pipeline(handler);
            }
        });
    }

    private void setPublisherOptions(final ClientBootstrap bootstrap)
    {
        bootstrap.setOption("sendBufferSize", MAX_MESSAGE_SIZE);
        bootstrap.setOption("connectTimeoutMillis", CONNECTION_TIMEOUT_MILLIS);
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