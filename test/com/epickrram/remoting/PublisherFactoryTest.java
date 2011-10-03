package com.epickrram.remoting;

import com.epickrram.messaging.RecordingMessagingService;
import com.epickrram.stream.ByteArrayOutputBufferImpl;
import com.epickrram.stream.ByteOutputBuffer;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.epickrram.MatcherFactory.aByteOutputBufferMatching;

@RunWith(JMock.class)
public final class PublisherFactoryTest
{
    private static final int TOPIC_IC = 7;
    private static final byte FIRST_METHOD_INDEX = 1;
    private static final int INT_VALUE_1 = 289374234;
    private static final int INT_VALUE_2 = 389475234;
    private static final byte BYTE_VALUE = (byte)126;
    private static final long LONG_VALUE = 3928473424L;

    private Mockery mockery = new Mockery();
    private RecordingMessagingService messagingService;
    private TopicIdGenerator topicIdGenerator;
    private PublisherFactory publisherFactory;

    @Test
    public void shouldGeneratePublisherForSingleNoArgsMethodInterface() throws Exception
    {
        final SingleNoArgsMethodInterface publisher = publisherFactory.createPublisher(SingleNoArgsMethodInterface.class);
        final ByteOutputBuffer expectedMessageOne = new ByteArrayOutputBufferImpl(8);
        expectedMessageOne.writeInt(TOPIC_IC);
        expectedMessageOne.writeLong(0L);
        expectedMessageOne.writeByte(FIRST_METHOD_INDEX);
        expectedMessageOne.setPosition(0);

        final ByteOutputBuffer expectedMessageTwo = new ByteArrayOutputBufferImpl(8);
        expectedMessageTwo.writeInt(TOPIC_IC);
        expectedMessageTwo.writeLong(1L);
        expectedMessageTwo.writeByte(FIRST_METHOD_INDEX);
        expectedMessageTwo.setPosition(0);

//        publisher.invoke();
        publisher.invoke();

        messagingService.waitForMessage(TOPIC_IC, aByteOutputBufferMatching(expectedMessageOne));
        messagingService.waitForMessage(TOPIC_IC, aByteOutputBufferMatching(expectedMessageTwo));
    }

    @Test
    public void shouldGeneratePublisherForSingleArgMethodInterface() throws Exception
    {
        final SingleArgMethodInterface publisher = publisherFactory.createPublisher(SingleArgMethodInterface.class);
        final ByteOutputBuffer expectedMessageOne = new ByteArrayOutputBufferImpl(16);
        expectedMessageOne.writeInt(TOPIC_IC);
        expectedMessageOne.writeLong(0L);
        expectedMessageOne.writeByte(FIRST_METHOD_INDEX);
        expectedMessageOne.writeInt(INT_VALUE_1);

        final ByteOutputBuffer expectedMessageTwo = new ByteArrayOutputBufferImpl(16);
        expectedMessageTwo.writeInt(TOPIC_IC);
        expectedMessageTwo.writeLong(1L);
        expectedMessageTwo.writeByte(FIRST_METHOD_INDEX);
        expectedMessageTwo.writeInt(INT_VALUE_2);

        publisher.invoke(INT_VALUE_1);
        publisher.invoke(INT_VALUE_2);

        messagingService.waitForMessage(TOPIC_IC, aByteOutputBufferMatching(expectedMessageOne));
        messagingService.waitForMessage(TOPIC_IC, aByteOutputBufferMatching(expectedMessageTwo));

    }

    @Test
    public void shouldGeneratePublisherForMultipleArgMultipleMethodInterface() throws Exception
    {
        final MultipleArgMultipleMethodInterface publisher = publisherFactory.createPublisher(MultipleArgMultipleMethodInterface.class);
        final ByteOutputBuffer expectedMessageOne = new ByteArrayOutputBufferImpl(16);
        expectedMessageOne.writeInt(TOPIC_IC);
        expectedMessageOne.writeLong(0L);
        expectedMessageOne.writeByte(FIRST_METHOD_INDEX);
        expectedMessageOne.writeInt(INT_VALUE_1);
        expectedMessageOne.writeByte(BYTE_VALUE);

        final ByteOutputBuffer expectedMessageTwo = new ByteArrayOutputBufferImpl(24);
        expectedMessageTwo.writeInt(TOPIC_IC);
        expectedMessageTwo.writeLong(1L);
        expectedMessageTwo.writeByte(FIRST_METHOD_INDEX);
        expectedMessageTwo.writeLong(LONG_VALUE);
        expectedMessageTwo.writeInt(INT_VALUE_2);
        expectedMessageTwo.writeByte(BYTE_VALUE);

        publisher.invoke(INT_VALUE_1, BYTE_VALUE);
        publisher.invoke(LONG_VALUE, INT_VALUE_2, BYTE_VALUE);

        messagingService.waitForMessage(TOPIC_IC, aByteOutputBufferMatching(expectedMessageOne));
        messagingService.waitForMessage(TOPIC_IC, aByteOutputBufferMatching(expectedMessageTwo));
    }

    @Before
    public void setUp() throws Exception
    {
        messagingService = new RecordingMessagingService();
        topicIdGenerator = mockery.mock(TopicIdGenerator.class);

        publisherFactory = new PublisherFactory(messagingService, topicIdGenerator);

        mockery.checking(new Expectations()
        {
            {
                allowing(topicIdGenerator).getTopicId(with(any(Class.class)));
                will(returnValue(TOPIC_IC));
            }
        });
    }

    private interface MultipleArgMultipleMethodInterface
    {
        void invoke(int value, byte b);
        void invoke(long value, int i, byte b);
    }

    private interface SingleArgMethodInterface
    {
        void invoke(int value);
    }

    private interface SingleNoArgsMethodInterface
    {
        void invoke();
    }
}
