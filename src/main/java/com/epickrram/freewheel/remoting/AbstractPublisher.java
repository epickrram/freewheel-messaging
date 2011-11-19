package com.epickrram.freewheel.remoting;

import com.epickrram.freewheel.protocol.CodeBook;
import com.epickrram.freewheel.messaging.MessagingService;

import java.io.ByteArrayOutputStream;

public abstract class AbstractPublisher
{
    private static final int DEFAULT_BUFFER_SIZE = 2048;
    private final MessagingService messagingService;
    private final int topicId;
    private final CodeBook codeBook;

    public AbstractPublisher(final MessagingService messagingService, final int topicId, final CodeBook codeBook)
    {
        this.messagingService = messagingService;
        this.topicId = topicId;
        this.codeBook = codeBook;
    }

    protected MessagingService getMessagingService()
    {
        return messagingService;
    }

    protected int getTopicId()
    {
        return topicId;
    }

    protected ByteArrayOutputStream getOutputStream()
    {
        return new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);
    }

    protected CodeBook getCodeBook()
    {
        return codeBook;
    }
}