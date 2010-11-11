package com.epickrram.remoting;

import com.epickrram.messaging.MessagingService;
import com.epickrram.stream.ByteArrayOutputBufferImpl;
import com.epickrram.stream.ByteOutputBuffer;

public abstract class AbstractPublisher
{
    private static final ThreadLocal<ByteOutputBuffer> OUTPUT_BUFFER = new ThreadLocal<ByteOutputBuffer>()
    {
        @Override
        protected ByteOutputBuffer initialValue()
        {
            return new ByteArrayOutputBufferImpl(2048);
        }
    };

    private final MessagingService messagingService;
    private final int topicId;

    public AbstractPublisher(final MessagingService messagingService, final int topicId)
    {
        this.messagingService = messagingService;
        this.topicId = topicId;
    }

    protected MessagingService getMessagingService()
    {
        return messagingService;
    }

    protected int getTopicId()
    {
        return topicId;
    }

    protected ByteOutputBuffer getBuffer()
    {
        return OUTPUT_BUFFER.get();
    }
}
