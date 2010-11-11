package com.epickrram.remoting;

import com.epickrram.messaging.Receiver;
import com.epickrram.stream.ByteArrayInputBufferImpl;
import com.epickrram.stream.ByteArrayOutputBufferImpl;
import com.epickrram.stream.ByteInputBuffer;
import com.epickrram.stream.ByteOutputBuffer;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public final class SubscriberFactoryTest
{
    private Mockery mockery = new Mockery();
    private SubscriberFactory subscriberFactory;
    private static final int INT_VALUE = 89237423;
    private static final byte BYTE_VALUE = (byte) 126;
    private static final long LONG_VALUE = 238472394L;

    @Test
    public void shouldCreateReceiverForSingleNoArgsMethodInterface() throws Exception
    {
        final SingleNoArgsMethodInterface implementation = mockery.mock(SingleNoArgsMethodInterface.class);
        final Receiver receiver = subscriberFactory.createReceiver(SingleNoArgsMethodInterface.class, implementation);
        final ByteArrayInputBufferImpl inputBuffer = new ByteArrayInputBufferImpl(new byte[] {0}, 0, 1);

        mockery.checking(new Expectations()
        {
            {
                one(implementation).invoke();
            }
        });

        receiver.onMessage(-1, inputBuffer);
    }

    @Test
    public void shouldCreateReceiverForMultipleMethods() throws Exception
    {
        final MultipleMethodNoArgsInterface implementation = mockery.mock(MultipleMethodNoArgsInterface.class);
        final Receiver receiver = subscriberFactory.createReceiver(MultipleMethodNoArgsInterface.class, implementation);
        final ByteArrayInputBufferImpl firstMethodBuffer = new ByteArrayInputBufferImpl(new byte[] {0}, 0, 1);
        final ByteArrayInputBufferImpl secondMethodBuffer = new ByteArrayInputBufferImpl(new byte[] {1}, 0, 1);
        final ByteArrayInputBufferImpl thirdMethodBuffer = new ByteArrayInputBufferImpl(new byte[] {2}, 0, 1);

        mockery.checking(new Expectations()
        {
            {
                one(implementation).one();
                one(implementation).two();
                one(implementation).three();
            }
        });

        receiver.onMessage(-1, firstMethodBuffer);
        receiver.onMessage(-1, secondMethodBuffer);
        receiver.onMessage(-1, thirdMethodBuffer);
    }

    @Test
    public void shouldCreateReceiverForMethodWithArguments() throws Exception
    {
        final MethodWithArgsInterface implementation = mockery.mock(MethodWithArgsInterface.class);
        final Receiver receiver = subscriberFactory.createReceiver(MethodWithArgsInterface.class, implementation);
        final ByteOutputBuffer buffer = new ByteArrayOutputBufferImpl(128);
        buffer.writeByte((byte) 0);
        buffer.writeInt(INT_VALUE);
        buffer.writeByte(BYTE_VALUE);
        buffer.writeLong(LONG_VALUE);

        final ByteInputBuffer inputBuffer = new ByteArrayInputBufferImpl(buffer.getBackingArray(), 0, buffer.count());

        mockery.checking(new Expectations()
        {
            {
                one(implementation).invoke(INT_VALUE, BYTE_VALUE, LONG_VALUE);
            }
        });

        receiver.onMessage(-1, inputBuffer);
    }

    @Before
    public void setUp() throws Exception
    {
        subscriberFactory = new SubscriberFactory();
    }

    private interface MethodWithArgsInterface
    {
        void invoke(int intValue, byte b, long longValue);
    }

    private interface SingleNoArgsMethodInterface
    {
        void invoke();
    }

    private interface MultipleMethodNoArgsInterface
    {
        void one();
        void two();
        void three();
    }
}