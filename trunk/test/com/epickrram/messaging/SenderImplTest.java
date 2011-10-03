package com.epickrram.messaging;

import com.epickrram.stream.ByteArrayOutputBufferImpl;
import com.epickrram.util.BlockingDirectCircularBuffer;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public final class SenderImplTest
{
    private Mockery mockery = new Mockery();
    private MessagingService messagingService;
    private SenderImpl sender;

    @Test
    public void shouldStoreInBuffer() throws Exception
    {
        sender.send(new ByteArrayOutputBufferImpl(8), 0L);
    }

    @Before
    public void setUp() throws Exception
    {
        messagingService = mockery.mock(MessagingService.class);
        sender = new SenderImpl(new BlockingDirectCircularBuffer<ByteArrayOutputBufferImpl>(16), messagingService, 7);
    }
}
