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
package com.epickrram.freewheel.messaging;

import com.epickrram.freewheel.io.DecoderStream;
import com.epickrram.freewheel.io.PackerEncoderStream;
import com.epickrram.freewheel.protocol.CodeBookImpl;
import junit.framework.Assert;
import org.msgpack.packer.MessagePackPacker;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TestMessageReceiver implements Receiver
{
    private static final long TIMEOUT_MILLIS = 5000L;

    private final ConcurrentMap<Integer, List<byte[]>> topicMessages = new ConcurrentHashMap<Integer, List<byte[]>>();

    @Override
    public void onMessage(final int topicId, final DecoderStream decoderStream)
    {
        List<byte[]> topicMessageList = topicMessages.get(topicId);
        if(topicMessageList == null)
        {
            topicMessageList = new CopyOnWriteArrayList<byte[]>();
            final List<byte[]> existing = topicMessages.putIfAbsent(topicId, topicMessageList);
            if(existing != null)
            {
                topicMessageList = existing;
            }
        }
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final PackerEncoderStream encoderStream = new PackerEncoderStream(new CodeBookImpl(), new MessagePackPacker(buffer));

        try
        {
            encoderStream.writeInt(topicId);
            final byte[] bytes = decoderStream.readByteArray();
            encoderStream.writeByteArray(bytes, 0, bytes.length);
        }
        catch (Exception e)
        {
            Assert.fail("Did not get expected contents in message");
        }
        topicMessageList.add(buffer.toByteArray());
    }

    public void waitForMessageReceived(final int topicId, final byte[] expectedMessage)
    {
        final long timeout = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while(System.currentTimeMillis() < timeout)
        {
            final List<byte[]> topicMessageList = topicMessages.get(topicId);
            if(topicMessageList != null)
            {
                for (byte[] message : topicMessageList)
                {
                    if(Arrays.equals(expectedMessage, message))
                    {
                        return;
                    }
                }
            }
            try
            {
                Thread.sleep(250L);
            }
            catch (InterruptedException e)
            {
                // ignore
            }
        }
        org.junit.Assert.fail("Did not find expected message " + Arrays.toString(expectedMessage) + " for topic " + topicId);
    }

}
