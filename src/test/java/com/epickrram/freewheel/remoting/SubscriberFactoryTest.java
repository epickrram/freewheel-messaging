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
import com.epickrram.freewheel.messaging.Receiver;
import com.epickrram.freewheel.protocol.CodeBookImpl;
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

@RunWith(JMock.class)
public final class SubscriberFactoryTest
{
    private static final int INT_VALUE = 89237423;
    private static final byte BYTE_VALUE = (byte) 126;
    private static final long LONG_VALUE = 238472394L;
    private Mockery mockery = new Mockery();
    private SubscriberFactory subscriberFactory;

    @Test
    public void shouldCreateReceiverForSingleNoArgsMethodInterface() throws Exception
    {
        final SingleNoArgsMethodInterface implementation = mockery.mock(SingleNoArgsMethodInterface.class);
        final Receiver receiver = subscriberFactory.createReceiver(SingleNoArgsMethodInterface.class, implementation);

        final UnpackerDecoderStream decoderStream = decoderStreamFor(new byte[]{0});

        mockery.checking(new Expectations()
        {
            {
                one(implementation).invoke();
            }
        });

        receiver.onMessage(-1, decoderStream);
    }



    @Test
    public void shouldCreateReceiverForMultipleMethods() throws Exception
    {
        final MultipleMethodNoArgsInterface implementation = mockery.mock(MultipleMethodNoArgsInterface.class);
        final Receiver receiver = subscriberFactory.createReceiver(MultipleMethodNoArgsInterface.class, implementation);

        mockery.checking(new Expectations()
        {
            {
                one(implementation).one();
                one(implementation).two();
                one(implementation).three();
            }
        });

        receiver.onMessage(-1, decoderStreamFor(new byte[] {0}));
        receiver.onMessage(-1, decoderStreamFor(new byte[] {1}));
        receiver.onMessage(-1, decoderStreamFor(new byte[] {2}));
    }

    @Test
    public void shouldCreateReceiverForMethodWithArguments() throws Exception
    {
        final MethodWithArgsInterface implementation = mockery.mock(MethodWithArgsInterface.class);
        final Receiver receiver = subscriberFactory.createReceiver(MethodWithArgsInterface.class, implementation);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final PackerEncoderStream encoderStream = new PackerEncoderStream(new CodeBookImpl(), new MessagePackPacker(outputStream));
        encoderStream.writeByte((byte) 0);
        encoderStream.writeInt(INT_VALUE);
        encoderStream.writeByte(BYTE_VALUE);
        encoderStream.writeLong(LONG_VALUE);

        final UnpackerDecoderStream decoderStream = decoderStreamFor(outputStream.toByteArray());

        mockery.checking(new Expectations()
        {
            {
                one(implementation).invoke(INT_VALUE, BYTE_VALUE, LONG_VALUE);
            }
        });

        receiver.onMessage(-1, decoderStream);
    }

    @Before
    public void setUp() throws Exception
    {
        subscriberFactory = new SubscriberFactory();
    }

    private UnpackerDecoderStream decoderStreamFor(final byte[] payload)
    {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(payload, 0, payload.length);
        return new UnpackerDecoderStream(new CodeBookImpl(), new MessagePackUnpacker(inputStream));
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