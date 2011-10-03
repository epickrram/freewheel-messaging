package com.epickrram.remoting;

import com.epickrram.messaging.MessagingService;
import com.epickrram.messaging.Sender;
import com.epickrram.messaging.SenderImpl;
import com.epickrram.stream.ByteArrayOutputBufferImpl;
import com.epickrram.util.BlockingDirectCircularBuffer;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public abstract class AbstractPublisher
{
    private static final ThreadLocal<ByteArrayOutputBufferImpl> OUTPUT_BUFFER = new ThreadLocal<ByteArrayOutputBufferImpl>()
    {
        @Override
        protected ByteArrayOutputBufferImpl initialValue()
        {
            return new ByteArrayOutputBufferImpl(2048);
        }
    };

    private static final Logger LOGGER = Logger.getLogger(AbstractPublisher.class.getName());

    private final AtomicLong sequence = new AtomicLong(-1);
    private final MessagingService messagingService;
    private final int topicId;
    private final Sender<ByteArrayOutputBufferImpl> sender;

    public AbstractPublisher(final MessagingService messagingService, final int topicId)
    {
        this.messagingService = messagingService;
        this.topicId = topicId;
        this.sender = new SenderImpl(new BlockingDirectCircularBuffer<ByteArrayOutputBufferImpl>(2048), messagingService, topicId);
        sender.start();
    }

    protected long getSequence()
    {
        return sequence.incrementAndGet();
    }

    protected MessagingService getMessagingService()
    {
        return messagingService;
    }

    protected int getTopicId()
    {
        return topicId;
    }

    protected void send()
    {
        LOGGER.info("sending message to sender");
        sender.send(OUTPUT_BUFFER.get(), sequence.get());
    }

    protected ByteArrayOutputBufferImpl getBuffer()
    {
        final ByteArrayOutputBufferImpl buffer = OUTPUT_BUFFER.get();
        buffer.reset();
        buffer.writeInt(topicId);
        buffer.writeLong(getSequence());
        return buffer;
    }
}
