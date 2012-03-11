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

import com.epickrram.freewheel.io.PackerEncoderStreamFactory;
import com.epickrram.freewheel.protocol.CodeBookImpl;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public final class MessagingServiceEventHandlerTest
{
    private static final int TOPIC_ID = 928374293;
    
    private Mockery mockery = new Mockery();
    private MessagingService messagingService;
    private MessagingServiceEventHandler eventHandler;
    private OutgoingMessageEvent event;

    @Test
    public void shouldPublishOutgoingEventToMessagingService() throws Exception
    {
        event.setTopicId(TOPIC_ID);

        mockery.checking(new Expectations()
        {
            {
                one(messagingService).send(TOPIC_ID, event.getOutput());
            }
        });

        eventHandler.onEvent(event, 0L, true);
    }

    @Before
    public void setUp() throws Exception
    {
        messagingService = mockery.mock(MessagingService.class);
        eventHandler = new MessagingServiceEventHandler(messagingService);
        event = new OutgoingMessageEvent(new PackerEncoderStreamFactory(new CodeBookImpl()));
    }
}