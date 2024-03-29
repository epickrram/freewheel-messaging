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
package com.epickrram.freewheel.remoting;

import com.epickrram.freewheel.io.PackerEncoderStream;
import com.epickrram.freewheel.io.UnpackerDecoderStream;
import com.epickrram.freewheel.messaging.Bits;
import com.epickrram.freewheel.messaging.MessagingService;
import com.epickrram.freewheel.messaging.config.Remote;
import com.epickrram.freewheel.protocol.CodeBook;
import com.epickrram.freewheel.protocol.CodeBookImpl;
import org.hamcrest.CoreMatchers;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.msgpack.packer.MessagePackPacker;
import org.msgpack.unpacker.MessagePackUnpacker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import static com.epickrram.MatcherFactory.aByteOutputBufferMatching;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JMock.class)
public final class DirectPublisherFactoryTest
{
    private static final int TOPIC_IC = 7;
    private static final byte FIRST_METHOD_INDEX = 0;
    private static final int INT_VALUE_1 = 289374234;
    private static final int INT_VALUE_2 = 389475234;
    private static final byte SECOND_METHOD_INDEX = (byte) 1;
    private static final byte BYTE_VALUE = (byte)126;
    private static final long LONG_VALUE = 3928473424L;
    private static final String STRING_VALUE = "STRING_VALUE";

    private Mockery mockery = new Mockery();
    private MessagingService messagingService;
    private TopicIdGenerator topicIdGenerator;
    private PublisherFactory publisherFactory;
    private CodeBook codeBook;

    @Test(expected = IllegalArgumentException.class)
    public void shouldBlowUpIfSyncMethodRequiredButMessagingServiceDoesNotSupportSendAndWait() throws Exception
    {
        mockery.checking(new Expectations()
        {
            {
                allowing(messagingService).supportsSendAndWait();
                will(returnValue(false));
            }
        });

        publisherFactory.createPublisher(SyncMethodInterface.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldBlowUpIfSyncMethodReturnsPrimitiveValue() throws Exception
    {
        mockery.checking(new Expectations()
        {
            {
                allowing(messagingService).supportsSendAndWait();
                will(returnValue(true));
            }
        });

        publisherFactory.createPublisher(SyncMethodPrimitiveReturnValueInterface.class);
    }

    @Test
    public void shouldGeneratePublisherForSyncMethodInterface() throws Exception
    {
        mockery.checking(new Expectations()
        {
            {
                allowing(messagingService).supportsSendAndWait();
                will(returnValue(true));
            }
        });

        final SyncMethodInterface publisher = publisherFactory.createPublisher(SyncMethodInterface.class);
        final ByteArrayOutputStream expectedMessage = new ByteArrayOutputStream(10);
        final PackerEncoderStream encoderStream = encoderFor(expectedMessage);

        final ByteArrayOutputStream encodedResponse = new ByteArrayOutputStream();
        final PackerEncoderStream responseEncoder = encoderFor(encodedResponse);
        responseEncoder.writeString(STRING_VALUE);
        final UnpackerDecoderStream decoderStream = new UnpackerDecoderStream(codeBook, new MessagePackUnpacker(new ByteArrayInputStream(encodedResponse.toByteArray())));
        encoderStream.writeInt(TOPIC_IC);
        encoderStream.writeByte(FIRST_METHOD_INDEX);
        encoderStream.writeInt(INT_VALUE_1);

        mockery.checking(new Expectations()
        {
            {
                exactly(1).of(messagingService).sendAndWait(with(TOPIC_IC), with(aByteOutputBufferMatching(expectedMessage)));
                will(returnValue(decoderStream));
            }
        });

        assertThat(publisher.invoke(INT_VALUE_1), is(STRING_VALUE));
    }

    @Test
    public void shouldGeneratePublisherForSingleNoArgsMethodInterface() throws Exception
    {
        final SingleNoArgsMethodInterface publisher = publisherFactory.createPublisher(SingleNoArgsMethodInterface.class);
        final ByteArrayOutputStream expectedMessage = new ByteArrayOutputStream(10);
        final PackerEncoderStream encoderStream = encoderFor(expectedMessage);
        encoderStream.writeInt(TOPIC_IC);
        encoderStream.writeByte(FIRST_METHOD_INDEX);

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

        publisherFactory = new DirectPublisherFactory(messagingService, topicIdGenerator, codeBook);

        mockery.checking(new Expectations()
        {
            {
                allowing(topicIdGenerator).getTopicId(with(any(Class.class)));
                will(returnValue(TOPIC_IC));
            }
        });
        codeBook = new CodeBookImpl();
    }

    private PackerEncoderStream encoderFor(final ByteArrayOutputStream expectedMessage)
    {
        return new PackerEncoderStream(codeBook, new MessagePackPacker(expectedMessage));
    }

    @Remote
    private interface MultipleArgMultipleMethodInterface
    {
        void invoke(int value, byte b);
        void invoke(long value, int i, byte b);
    }

    @Remote
    private interface SingleArgMethodInterface
    {
        void invoke(int value);
    }

    @Remote
    private interface SingleNoArgsMethodInterface
    {
        void invoke();
    }

    @Remote
    private interface SyncMethodInterface
    {
        String invoke(int value);
    }

    @Remote
    private interface SyncMethodPrimitiveReturnValueInterface
    {
        int invoke(long value);
    }
}
