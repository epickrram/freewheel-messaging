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

import com.epickrram.freewheel.protocol.CodeBook;
import com.epickrram.freewheel.messaging.MessagingService;

import java.io.ByteArrayOutputStream;

public abstract class AbstractPublisher
{
    private static final int DEFAULT_BUFFER_SIZE = 2048;
    private final MessagingService messagingService;
    private final int topicId;
    private final CodeBook codeBook;

    public AbstractPublisher(final MessagingService messagingService, final int topicId, final CodeBook codeBook)
    {
        this.messagingService = messagingService;
        this.topicId = topicId;
        this.codeBook = codeBook;
    }

    protected MessagingService getMessagingService()
    {
        return messagingService;
    }

    protected int getTopicId()
    {
        return topicId;
    }

    protected ByteArrayOutputStream getOutputStream()
    {
        return new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);
    }

    protected CodeBook getCodeBook()
    {
        return codeBook;
    }
}
