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

import com.epickrram.freewheel.messaging.multicast.MulticastMessagingService;
import com.epickrram.freewheel.messaging.ptp.EndPoint;
import com.epickrram.freewheel.messaging.ptp.EndPointProvider;
import com.epickrram.freewheel.messaging.ptp.PointToPointMessagingService;
import com.epickrram.freewheel.protocol.CodeBookImpl;
import com.epickrram.freewheel.protocol.CodeBookRegistry;
import com.epickrram.freewheel.remoting.ClassNameTopicIdGenerator;
import com.epickrram.freewheel.remoting.PublisherFactory;
import com.epickrram.freewheel.remoting.SubscriberFactory;

public final class MessagingContextFactory
{
    private final CodeBookImpl codeBook = new CodeBookImpl();
    private final ClassNameTopicIdGenerator topicIdGenerator = new ClassNameTopicIdGenerator();

    public CodeBookRegistry getCodeBookRegistry()
    {
        return new CodeBookImpl.CodeBookRegistryImpl(codeBook);
    }

    public MessagingContext createPointToPointMessagingContext(final EndPointProvider endPointProvider)
    {
        final PointToPointMessagingService messagingService = new PointToPointMessagingService(endPointProvider, codeBook, topicIdGenerator);
        return createMessagingContext(messagingService);
    }

    public MessagingContext createMulticastMessagingContext(final EndPoint endPoint)
    {
        final MulticastMessagingService messagingService = new MulticastMessagingService(endPoint, codeBook);
        return createMessagingContext(messagingService);
    }

    private MessagingContext createMessagingContext(final MessagingService messagingService)
    {
        final PublisherFactory publisherFactory = new PublisherFactory(messagingService, topicIdGenerator, codeBook);
        final SubscriberFactory subscriberFactory = new SubscriberFactory();
        return new MessagingContextImpl(publisherFactory, subscriberFactory, messagingService, topicIdGenerator);
    }
}