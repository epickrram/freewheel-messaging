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

package com.epickrram.freewheel.messaging.ptp;

import com.epickrram.freewheel.io.UnpackerDecoderStream;
import com.epickrram.freewheel.messaging.Receiver;
import com.epickrram.freewheel.messaging.ReceiverRegistry;
import com.epickrram.freewheel.protocol.CodeBook;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.msgpack.unpacker.MessagePackUnpacker;

import java.io.ByteArrayInputStream;
import java.util.Collection;

final class SubscriberChannelHandler extends SimpleChannelHandler
{
    private final CodeBook codeBook;
    private final ReceiverRegistry receiverRegistry;

    SubscriberChannelHandler(final CodeBook codeBook, final ReceiverRegistry receiverRegistry)
    {
        this.codeBook = codeBook;
        this.receiverRegistry = receiverRegistry;
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception
    {
        final ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
        // TODO handle partial message delivery
        while(buffer.readableBytes() > 4)
        {
            final int messageSize = buffer.readInt();
            final byte[] messageBuffer = new byte[messageSize];
            buffer.readBytes(messageBuffer, 0, messageSize);

            final MessagePackUnpacker messagePackUnpacker = new MessagePackUnpacker(new ByteArrayInputStream(messageBuffer));
            final UnpackerDecoderStream decoderStream = new UnpackerDecoderStream(codeBook, messagePackUnpacker);
            final int topicId = decoderStream.readInt();

            final Collection<Receiver> receiverList = receiverRegistry.getReceiverList(topicId);
            for (Receiver receiver : receiverList)
            {
                final MessagePackUnpacker innerMessagePackUnpacker = new MessagePackUnpacker(new ByteArrayInputStream(messageBuffer));
                final UnpackerDecoderStream innerDecoderStream = new UnpackerDecoderStream(codeBook, innerMessagePackUnpacker);
                innerDecoderStream.readInt();
                receiver.onMessage(topicId, innerDecoderStream);
            }
        }
    }
}
