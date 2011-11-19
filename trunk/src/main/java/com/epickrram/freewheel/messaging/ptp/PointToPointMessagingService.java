package com.epickrram.freewheel.messaging.ptp;

import com.epickrram.freewheel.io.UnpackerDecoderStream;
import com.epickrram.freewheel.messaging.MessagingException;
import com.epickrram.freewheel.messaging.MessagingService;
import com.epickrram.freewheel.messaging.Receiver;
import com.epickrram.freewheel.messaging.ReceiverRegistry;
import com.epickrram.freewheel.protocol.CodeBook;
import com.epickrram.freewheel.util.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictor;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.ReceiveBufferSizePredictor;
import org.jboss.netty.channel.ReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.msgpack.unpacker.MessagePackUnpacker;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;

public final class PointToPointMessagingService implements MessagingService
{
    private static final Logger LOGGER = Logger.getLogger(PointToPointMessagingService.class);
    private static final int MAX_MESSAGE_SIZE = 64 * 1024;

    private final ReceiverRegistry receiverRegistry = new ReceiverRegistry();
    private final EndPoint endPoint;
    private final ServiceType serviceType;
    private final CodeBook codeBook;
    private Channel subscriberChannel;
    private ChannelFuture publisherChannelFuture;

    public PointToPointMessagingService(final EndPoint endPoint, final ServiceType serviceType, final CodeBook codeBook)
    {
        this.endPoint = endPoint;
        this.serviceType = serviceType;
        this.codeBook = codeBook;
    }

    @Override
    public void send(final int topicId, final ByteArrayOutputStream byteArrayOutputStream) throws MessagingException
    {
        if(serviceType == ServiceType.SUBSCRIBE)
        {
            throw new IllegalStateException("SUBSCRIBE services cannot send messages");
        }
        final ChannelBuffer buffer = ChannelBuffers.buffer(4 + byteArrayOutputStream.size());
        buffer.writeInt(byteArrayOutputStream.size());
        buffer.writeBytes(byteArrayOutputStream.toByteArray(), 0, byteArrayOutputStream.size());

        final ChannelFuture writeFuture = publisherChannelFuture.getChannel().write(buffer);
        // TODO should block until sent?
    }

    @Override
    public void registerReceiver(final int topicId, final Receiver receiver)
    {
        if(serviceType == ServiceType.PUBLISH)
        {
            throw new IllegalStateException("PUBLISH services cannot receive messages");
        }
        receiverRegistry.registerReceiver(topicId, receiver);
    }

    @Override
    public void start() throws MessagingException
    {
        try
        {
            switch (serviceType)
            {
                case PUBLISH:
                    createPublishMessagingService();
                    break;
                case SUBSCRIBE:
                    createSubscribeMessagingService();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown ServiceType: " + serviceType);
            }
        } catch (UnknownHostException e)
        {
            throw new MessagingException("Failed to start messaging service", e);
        }
    }

    private void createSubscribeMessagingService() throws UnknownHostException
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
                            final UnpackerDecoderStream decoderStream = new UnpackerDecoderStream(codeBook,
                                    new MessagePackUnpacker(new ChannelBufferInputStream(buffer, messageSize)));
                            final int topicId = decoderStream.readInt();

                            final Receiver receiver = receiverRegistry.getReceiver(topicId);
                            receiver.onMessage(topicId, decoderStream);
                        }
                    }
                });
            }
        });
        final InetSocketAddress subscriberAddress = new InetSocketAddress(endPoint.getPort());
        subscriberChannel = bootstrap.bind(subscriberAddress);
        LOGGER.info("Subscriber is bound to address: " + subscriberAddress);
    }

    private void createPublishMessagingService()
    {
        final ChannelFactory channelFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool());
        final ClientBootstrap bootstrap = new ClientBootstrap(channelFactory);
        bootstrap.setOption("sendBufferSize", MAX_MESSAGE_SIZE);
        bootstrap.setPipelineFactory(new ChannelPipelineFactory()
        {
            public ChannelPipeline getPipeline()
            {
                return Channels.pipeline(new SimpleChannelHandler()
                {
                    @Override
                    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception
                    {
                        LOGGER.error("Received message on publisher service channel", null);
                    }
                });
            }
        });
        final InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", endPoint.getPort());

        LOGGER.info("Publisher connecting to remote address: " + remoteAddress);

        publisherChannelFuture = bootstrap.connect(remoteAddress);
        publisherChannelFuture.addListener(new ChannelFutureListener()
        {
            @Override
            public void operationComplete(final ChannelFuture future) throws Exception
            {
                LOGGER.info("Channel op complete. Success: " + future.isSuccess() +
                        ", cancelled: " + future.isCancelled() + ", done: " + future.isDone());
            }
        });

        try
        {
            LOGGER.info("Waiting for publisher channel");
            if(publisherChannelFuture.await(5000L))
            {
                LOGGER.info("Publisher channel complete");
            }
            else
            {
                throw new MessagingException("Could not connect client channel");
            }
        }
        catch (InterruptedException e)
        {
            throw new MessagingException("Failed to wait for publisher channel", e);
        }
    }

    @Override
    public void shutdown() throws MessagingException
    {
        switch(serviceType)
        {
            case SUBSCRIBE:
                subscriberChannel.unbind();
                break;
            case PUBLISH:
                publisherChannelFuture.getChannel().unbind();
                break;
        }
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
