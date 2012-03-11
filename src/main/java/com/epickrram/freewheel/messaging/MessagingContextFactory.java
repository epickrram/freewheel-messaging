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
import com.epickrram.freewheel.remoting.BufferedPublisherFactory;
import com.epickrram.freewheel.remoting.ClassNameTopicIdGenerator;
import com.epickrram.freewheel.remoting.RingBufferFactoryImpl;
import com.epickrram.freewheel.remoting.SubscriberFactory;
import com.epickrram.freewheel.util.DaemonThreadFactory;
import com.lmax.disruptor.EventProcessor;

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
        return createMessagingContext(messagingService);
    }

    public MessagingContext createMulticastMessagingContext(final EndPoint endPoint)
    {
        final MulticastMessagingService messagingService = new MulticastMessagingService(endPoint, codeBook);
        return createMessagingContext(messagingService);
    }

    private MessagingContext createMessagingContext(final MessagingService messagingService)
    {
        final OutgoingMessageEventFactory eventFactory = new OutgoingMessageEventFactory(codeBook);
        final MessagingServiceEventHandler eventHandler = new MessagingServiceEventHandler(messagingService);
        final RingBufferFactoryImpl ringBufferFactory = new RingBufferFactoryImpl(eventFactory, eventHandler);
        final BufferedPublisherFactory publisherFactory = new BufferedPublisherFactory(ringBufferFactory, topicIdGenerator, codeBook);
        final SubscriberFactory subscriberFactory = new SubscriberFactory();
        final MessagingContextImpl messagingContext = new MessagingContextImpl(publisherFactory, subscriberFactory, messagingService, topicIdGenerator);
        final List<EventProcessor> eventProcessors = ringBufferFactory.getEventProcessors();
        messagingContext.registerLifecyleAware(new EventProcessorLifecycleAware(eventProcessors));
        return messagingContext;
    }

    private static final class EventProcessorLifecycleAware implements LifecycleAware
    {
        private final List<EventProcessor> eventProcessors;
        private volatile ExecutorService executorService;

        public EventProcessorLifecycleAware(final List<EventProcessor> eventProcessors)
        {
            this.eventProcessors = eventProcessors;
        }

        @Override
        public void systemStarting()
        {
            // TODO central control for Thread lifecycle
            final DaemonThreadFactory threadFactory = new DaemonThreadFactory("publisher");
            this.executorService = Executors.newFixedThreadPool(eventProcessors.size(), threadFactory);
            for (EventProcessor eventProcessor : eventProcessors)
            {
                executorService.submit(eventProcessor);
            }
        }

        @Override
        public void systemStopping()
        {
            // TODO should halt() ringbuffers
            executorService.shutdown();
        }
    }
}