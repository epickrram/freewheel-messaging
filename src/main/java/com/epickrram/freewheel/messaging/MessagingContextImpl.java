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

import com.epickrram.freewheel.remoting.PublisherFactory;
import com.epickrram.freewheel.remoting.SubscriberFactory;
import com.epickrram.freewheel.remoting.TopicIdGenerator;

public final class MessagingContextImpl implements MessagingContext
{
    private final PublisherFactory publisherFactory;
    private final SubscriberFactory subscriberFactory;
    private final MessagingService messagingService;
    private final TopicIdGenerator topicIdGenerator;

    public MessagingContextImpl(final PublisherFactory publisherFactory,
                                final SubscriberFactory subscriberFactory,
                                final MessagingService messagingService,
                                final TopicIdGenerator topicIdGenerator)
    {
        this.publisherFactory = publisherFactory;
        this.subscriberFactory = subscriberFactory;
        this.messagingService = messagingService;
        this.topicIdGenerator = topicIdGenerator;
    }

    @Override
    public <T> T createPublisher(final Class<T> descriptor) throws MessagingException
    {
        final T publisher = publisherFactory.createPublisher(descriptor);
        messagingService.registerPublisher(descriptor);
        return publisher;
    }

    @Override
    public <T> void createSubscriber(final Class<T> descriptor, final T implementation) throws MessagingException
    {
        final Receiver receiver = subscriberFactory.createReceiver(descriptor, implementation);
        final int topicId = topicIdGenerator.getTopicId(descriptor);
        messagingService.registerReceiver(topicId, receiver);
        messagingService.registerSubscriber(descriptor);
    }

    @Override
    public void start()
    {
        messagingService.start();
    }

    @Override
    public void stop()
    {
        messagingService.shutdown();
    }
}