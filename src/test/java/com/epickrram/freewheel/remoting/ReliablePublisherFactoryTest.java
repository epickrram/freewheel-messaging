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

import com.epickrram.freewheel.io.EncoderStream;
import com.epickrram.freewheel.io.PackerEncoderStream;
import com.epickrram.freewheel.messaging.OutgoingMessageEvent;
import com.epickrram.freewheel.protocol.CodeBook;
import com.epickrram.freewheel.protocol.CodeBookImpl;
import com.epickrram.freewheel.util.RingBufferWrapper;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.msgpack.packer.MessagePackPacker;

import java.io.ByteArrayOutputStream;

import static com.epickrram.MatcherFactory.aByteOutputBufferMatching;

@RunWith(JMock.class)
public final class ReliablePublisherFactoryTest
{
    private static final int TOPIC_ID = 987324234;

    private Mockery mockery = new Mockery();
    private RingBufferFactory ringBufferFactory;
    private TopicIdGenerator topicIdGenerator;
    private ReliablePublisherFactory reliablePublisherFactory;
    private RingBufferWrapper<OutgoingMessageEvent> ringBufferWrapper;
    private CodeBook codeBook;

    @SuppressWarnings({"unchecked"})
    @Before
    public void setUp() throws Exception
    {
        ringBufferFactory = mockery.mock(RingBufferFactory.class);
        topicIdGenerator = mockery.mock(TopicIdGenerator.class);
        ringBufferWrapper = mockery.mock(RingBufferWrapper.class);

        mockery.checking(new Expectations()
        {
            {
                allowing(topicIdGenerator).getTopicId(with(any(Class.class)));
                will(returnValue(TOPIC_ID));
            }
        });
        codeBook = new CodeBookImpl();

        reliablePublisherFactory = new ReliablePublisherFactory(ringBufferFactory, topicIdGenerator, codeBook);
    }

    @Test
    public void shouldCreateSingleRingBufferForPublisher() throws Exception
    {
        mockery.checking(new Expectations()
        {
            {
                oneOf(ringBufferFactory).createRingBuffer(RingBufferFactory.DEFAULT_RING_BUFFER_SIZE);
                will(returnValue(ringBufferWrapper));
            }
        });

        reliablePublisherFactory.createPublisher(SingleArgMethodInterface.class);
    }

    @Test
    public void shouldPublishMessagesToRingBuffer() throws Exception
    {
        final Sequence seq = mockery.sequence("seq");
        final ByteArrayOutputStream actualMessageOne = new ByteArrayOutputStream();
        final ByteArrayOutputStream actualMessageTwo = new ByteArrayOutputStream();
        final OutgoingMessageEvent firstEvent = new OutgoingMessageEvent(createEmptyEncoderStream(actualMessageOne));
        final OutgoingMessageEvent secondEvent = new OutgoingMessageEvent(createEmptyEncoderStream(actualMessageTwo));

        final ByteArrayOutputStream expectedMessageOne = new ByteArrayOutputStream();
        final EncoderStream encoderOne = encoderFor(expectedMessageOne);
        encoderOne.writeInt(TOPIC_ID);
        encoderOne.writeByte((byte) 0);
        encoderOne.writeInt(42);
        encoderOne.writeByte((byte) 11);

        final ByteArrayOutputStream expectedMessageTwo = new ByteArrayOutputStream();
        final EncoderStream encoderTwo = encoderFor(expectedMessageTwo);
        encoderTwo.writeInt(TOPIC_ID);
        encoderTwo.writeByte((byte) 1);
        encoderTwo.writeLong(Long.MAX_VALUE);
        encoderTwo.writeInt(11);
        encoderTwo.writeByte((byte) 3);

        mockery.checking(new Expectations()
        {
            {
                oneOf(ringBufferWrapper).next();
                will(returnValue(0L));
                inSequence(seq);
                oneOf(ringBufferWrapper).get(0L);
                will(returnValue(firstEvent));
                inSequence(seq);
                oneOf(ringBufferWrapper).publish(0L);
                inSequence(seq);

                oneOf(ringBufferWrapper).next();
                will(returnValue(1L));
                inSequence(seq);
                oneOf(ringBufferWrapper).get(1L);
                will(returnValue(secondEvent));
                inSequence(seq);
                oneOf(ringBufferWrapper).publish(1L);
                inSequence(seq);

                allowing(ringBufferFactory).createRingBuffer(RingBufferFactory.DEFAULT_RING_BUFFER_SIZE);
                will(returnValue(ringBufferWrapper));
            }
        });

        final MultipleArgMultipleMethodInterface publisher =
                reliablePublisherFactory.createPublisher(MultipleArgMultipleMethodInterface.class);

        publisher.invoke(42, (byte) 11);
        publisher.invoke(Long.MAX_VALUE, 11, (byte) 3);

        assertMessageContents(expectedMessageOne, actualMessageOne);
        assertMessageContents(expectedMessageTwo, actualMessageTwo);
    }

    private void assertMessageContents(final ByteArrayOutputStream expected,
                                       final ByteArrayOutputStream actual)
    {
        Assert.assertThat(actual, aByteOutputBufferMatching(expected));
    }


    private EncoderStream createEmptyEncoderStream(final ByteArrayOutputStream output)
    {
        return encoderFor(output);
    }

    private EncoderStream encoderFor(final ByteArrayOutputStream expectedMessage)
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
}