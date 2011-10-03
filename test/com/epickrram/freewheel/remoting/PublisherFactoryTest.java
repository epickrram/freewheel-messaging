package com.epickrram.freewheel.remoting;

import com.epickrram.freewheel.io.ClassnameCodeBook;
import com.epickrram.freewheel.io.PackerEncoderStream;
import com.epickrram.freewheel.messaging.MessagingService;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.msgpack.packer.MessagePackPacker;

import java.io.ByteArrayOutputStream;

import static com.epickrram.MatcherFactory.aByteOutputBufferMatching;

@RunWith(JMock.class)
public final class PublisherFactoryTest
{
    private static final int TOPIC_IC = 7;
    private static final byte FIRST_METHOD_INDEX = 0;
    private static final int INT_VALUE_1 = 289374234;
    private static final int INT_VALUE_2 = 389475234;
    private static final byte SECOND_METHOD_INDEX = (byte) 1;
    private static final byte BYTE_VALUE = (byte)126;
    private static final long LONG_VALUE = 3928473424L;

    private Mockery mockery = new Mockery();
    private MessagingService messagingService;
    private TopicIdGenerator topicIdGenerator;
    private PublisherFactory publisherFactory;
    private ClassnameCodeBook codeBook;

    @Test
    public void shouldGeneratePublisherForSingleNoArgsMethodInterface() throws Exception
    {
        final SingleNoArgsMethodInterface publisher = publisherFactory.createPublisher(SingleNoArgsMethodInterface.class);
        final ByteArrayOutputStream expectedMessage = new ByteArrayOutputStream(10);
        final PackerEncoderStream encoderStream = encoderFor(expectedMessage);
        encoderStream.writeInt(TOPIC_IC);
        encoderStream.writeInt(FIRST_METHOD_INDEX);

        mockery.checking(new Expectations()
        {
            {
                exactly(2).of(messagingService).send(with(TOPIC_IC), with(aByteOutputBufferMatching(expectedMessage)));
            }
        });

        publisher.invoke();
        publisher.invoke();
    }

    @Test
    public void shouldGeneratePublisherForSingleArgMethodInterface() throws Exception
    {
        final SingleArgMethodInterface publisher = publisherFactory.createPublisher(SingleArgMethodInterface.class);
        final ByteArrayOutputStream expectedMessageOne = new ByteArrayOutputStream(16);
        final PackerEncoderStream encoderOne = encoderFor(expectedMessageOne);
        encoderOne.writeInt(TOPIC_IC);
        encoderOne.writeByte(FIRST_METHOD_INDEX);
        encoderOne.writeInt(INT_VALUE_1);

        final ByteArrayOutputStream expectedMessageTwo = new ByteArrayOutputStream(16);
        final PackerEncoderStream encoderTwo = encoderFor(expectedMessageTwo);
        encoderTwo.writeInt(TOPIC_IC);
        encoderTwo.writeByte(FIRST_METHOD_INDEX);
        encoderTwo.writeInt(INT_VALUE_2);

        mockery.checking(new Expectations()
        {
            {
                one(messagingService).send(with(TOPIC_IC), with(aByteOutputBufferMatching(expectedMessageOne)));
                one(messagingService).send(with(TOPIC_IC), with(aByteOutputBufferMatching(expectedMessageTwo)));
            }
        });

        publisher.invoke(INT_VALUE_1);
        publisher.invoke(INT_VALUE_2);
    }

    @Test
    public void shouldGeneratePublisherForMultipleArgMultipleMethodInterface() throws Exception
    {
        final MultipleArgMultipleMethodInterface publisher = publisherFactory.createPublisher(MultipleArgMultipleMethodInterface.class);
        final ByteArrayOutputStream expectedMessageOne = new ByteArrayOutputStream(24);
        final PackerEncoderStream encoderOne = encoderFor(expectedMessageOne);
        encoderOne.writeInt(TOPIC_IC);
        encoderOne.writeByte(FIRST_METHOD_INDEX);
        encoderOne.writeInt(INT_VALUE_1);
        encoderOne.writeByte(BYTE_VALUE);

        final ByteArrayOutputStream expectedMessageTwo = new ByteArrayOutputStream(36);
        final PackerEncoderStream encoderTwo = encoderFor(expectedMessageTwo);
        encoderTwo.writeInt(TOPIC_IC);
        encoderTwo.writeByte(SECOND_METHOD_INDEX);
        encoderTwo.writeLong(LONG_VALUE);
        encoderTwo.writeInt(INT_VALUE_2);
        encoderTwo.writeByte(BYTE_VALUE);

        mockery.checking(new Expectations()
        {
            {
                one(messagingService).send(with(TOPIC_IC), with(aByteOutputBufferMatching(expectedMessageOne)));
                one(messagingService).send(with(TOPIC_IC), with(aByteOutputBufferMatching(expectedMessageTwo)));
            }
        });

        publisher.invoke(INT_VALUE_1, BYTE_VALUE);
        publisher.invoke(LONG_VALUE, INT_VALUE_2, BYTE_VALUE);
    }

    @Before
    public void setUp() throws Exception
    {
        messagingService = mockery.mock(MessagingService.class);
        topicIdGenerator = mockery.mock(TopicIdGenerator.class);

        publisherFactory = new PublisherFactory(messagingService, topicIdGenerator, codeBook);

        mockery.checking(new Expectations()
        {
            {
                allowing(topicIdGenerator).getTopicId(with(any(Class.class)));
                will(returnValue(TOPIC_IC));
            }
        });
        codeBook = new ClassnameCodeBook();
    }

    private PackerEncoderStream encoderFor(final ByteArrayOutputStream expectedMessage)
    {
        return new PackerEncoderStream(codeBook, new MessagePackPacker(expectedMessage));
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
