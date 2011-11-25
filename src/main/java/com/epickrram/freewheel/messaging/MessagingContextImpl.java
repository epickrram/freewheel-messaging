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