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

import com.epickrram.freewheel.messaging.OutgoingMessageEvent;
import com.epickrram.freewheel.protocol.CodeBook;
import com.epickrram.freewheel.util.RingBufferWrapper;

public abstract class AbstractReliablePublisher
{
    private final RingBufferWrapper<OutgoingMessageEvent> ringBuffer;
    private final int topicId;
    private final CodeBook codeBook;

    public AbstractReliablePublisher(final RingBufferWrapper<OutgoingMessageEvent> ringBuffer,
                                     final int topicId, final CodeBook codeBook)
    {
        this.ringBuffer = ringBuffer;
        this.topicId = topicId;
        this.codeBook = codeBook;
    }

    protected RingBufferWrapper<OutgoingMessageEvent> getRingBuffer()
    {
        return ringBuffer;
    }

    protected int getTopicId()
    {
        return topicId;
    }

    protected CodeBook getCodeBook()
    {
        return codeBook;
    }
}