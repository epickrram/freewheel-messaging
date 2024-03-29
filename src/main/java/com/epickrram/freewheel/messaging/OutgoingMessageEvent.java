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

import com.epickrram.freewheel.io.EncoderStream;
import com.epickrram.freewheel.io.EncoderStreamFactory;
import com.epickrram.freewheel.io.PackerEncoderStreamFactory;
import com.lmax.disruptor.EventFactory;

import java.io.ByteArrayOutputStream;

public final class OutgoingMessageEvent
{
    private final EncoderStream encoderStream;
    private final ByteArrayOutputStream output;

    private int topicId;

    public OutgoingMessageEvent(final EncoderStreamFactory encoderStreamFactory)
    {
        this.output = new ByteArrayOutputStream(256);
        this.encoderStream = encoderStreamFactory.create(output);
    }

    public int getTopicId()
    {
        return topicId;
    }

    public void setTopicId(final int topicId)
    {
        this.topicId = topicId;
    }

    public EncoderStream getEncoderStream()
    {
        return encoderStream;
    }

    public ByteArrayOutputStream getOutput()
    {
        return output;
    }

    public void reset()
    {
        output.reset();
    }

}