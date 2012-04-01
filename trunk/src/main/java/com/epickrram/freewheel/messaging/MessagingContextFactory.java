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
import com.epickrram.freewheel.messaging.ptp.BlockingPointToPointMessagingService;
import com.epickrram.freewheel.messaging.ptp.EndPoint;
import com.epickrram.freewheel.messaging.ptp.EndPointProvider;
import com.epickrram.freewheel.messaging.ptp.PointToPointMessagingService;
import com.epickrram.freewheel.protocol.CodeBookImpl;
import com.epickrram.freewheel.protocol.CodeBookRegistry;
import com.epickrram.freewheel.remoting.BufferedPublisherFactory;
import com.epickrram.freewheel.remoting.ClassNameTopicIdGenerator;
import com.epickrram.freewheel.remoting.DirectPublisherFactory;
import com.epickrram.freewheel.remoting.PublisherFactory;
import com.epickrram.freewheel.remoting.PublisherType;
import com.epickrram.freewheel.remoting.RingBufferFactoryImpl;
import com.epickrram.freewheel.remoting.SubscriberFactory;
import com.epickrram.freewheel.util.DaemonThreadFactory;
import com.lmax.disruptor.EventProcessor;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        return createMessagingContext(messagingService, PublisherType.BUFFERED);
    }

    public MessagingContext createDirectPointToPointMessagingContext(final EndPointProvider endPointProvider)
    {
        final PointToPointMessagingService messagingService = new PointToPointMessagingService(endPointProvider, codeBook, topicIdGenerator);
        return createMessagingContext(messagingService, PublisherType.DIRECT);
    }

    public MessagingContext createDirectBlockingPointToPointMessagingContext(final EndPointProvider endPointProvider)
    {
        final BlockingPointToPointMessagingService messagingService = new BlockingPointToPointMessagingService(endPointProvider, codeBook, topicIdGenerator);
        return createMessagingContext(messagingService, PublisherType.DIRECT);
    }

    public MessagingContext createMulticastMessagingContext(final EndPoint endPoint)
    {
        final MulticastMessagingService messagingService = new MulticastMessagingService(endPoint, codeBook);
        return createMessagingContext(messagingService, PublisherType.BUFFERED);
    }

    private MessagingContext createMessagingContext(final MessagingService messagingService, final PublisherType publisherType)
    {
        final PublisherFactory publisherFactory = publisherType.isDirect() ?
                createDirectPublisherFactory(messagingService) : createBufferedPublisherFactory(messagingService);

        final SubscriberFactory subscriberFactory = new SubscriberFactory();
        final MessagingContextImpl messagingContext =
                new MessagingContextImpl(publisherFactory, subscriberFactory, messagingService, topicIdGenerator);
        final Collection<LifecycleAware> lifecycleAwareCollection = publisherFactory.getLifecycleAwareCollection();
        for (LifecycleAware lifecycleAware : lifecycleAwareCollection)
        {
            messagingContext.registerLifecyleAware(lifecycleAware);
        }
        return messagingContext;
    }

    private PublisherFactory createDirectPublisherFactory(final MessagingService messagingService)
    {
        return new DirectPublisherFactory(messagingService, topicIdGenerator, codeBook);
    }

    private PublisherFactory createBufferedPublisherFactory(final MessagingService messagingService)
    {
        final OutgoingMessageEventFactory eventFactory = new OutgoingMessageEventFactory(codeBook);
        final MessagingServiceEventHandler eventHandler = new MessagingServiceEventHandler(messagingService);
        final RingBufferFactoryImpl ringBufferFactory = new RingBufferFactoryImpl(eventFactory, eventHandler);
        return new BufferedPublisherFactory(ringBufferFactory, topicIdGenerator, codeBook);
    }


}