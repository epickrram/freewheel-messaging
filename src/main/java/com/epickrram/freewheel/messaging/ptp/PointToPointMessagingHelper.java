package com.epickrram.freewheel.messaging.ptp;

import com.epickrram.freewheel.messaging.MessagingException;
import com.epickrram.freewheel.messaging.MessagingHelper;
import com.epickrram.freewheel.messaging.Receiver;
import com.epickrram.freewheel.protocol.CodeBook;
import com.epickrram.freewheel.remoting.PublisherFactory;
import com.epickrram.freewheel.remoting.SubscriberFactory;
import com.epickrram.freewheel.remoting.TopicIdGenerator;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PointToPointMessagingHelper implements MessagingHelper
{
    private final EndPointProvider endPointProvider;
    private final CodeBook codeBook;
    private final TopicIdGenerator topicIdGenerator;
    private final ConcurrentMap<Class<?>, Object> publisherMap = new ConcurrentHashMap<Class<?>, Object>();

    public PointToPointMessagingHelper(final EndPointProvider endPointProvider, final CodeBook codeBook, final TopicIdGenerator topicIdGenerator)
    {
        this.endPointProvider = endPointProvider;
        this.codeBook = codeBook;
        this.topicIdGenerator = topicIdGenerator;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public <T> T createPublisher(final Class<T> descriptor) throws MessagingException
    {
        // TODO should not allow multiple publishers - use a memoizer keyed by class
        if(!publisherMap.containsKey(descriptor))
        {
            final PointToPointMessagingService messagingService =
                    new PointToPointMessagingService(getEndPoint(descriptor), ServiceType.PUBLISH, codeBook);
            messagingService.start();
            final T publisher = new PublisherFactory(messagingService, topicIdGenerator, codeBook).createPublisher(descriptor);
            publisherMap.put(descriptor, publisher);
        }
        return (T) publisherMap.get(descriptor);
    }

    @Override
    public <T> void createSubscriber(final Class<T> descriptor, final T implementation) throws MessagingException
    {
        // TODO should add implementation to composite subscriber
        final PointToPointMessagingService messagingService = new PointToPointMessagingService(getEndPoint(descriptor), ServiceType.SUBSCRIBE, codeBook);
        final Receiver receiver = new SubscriberFactory().createReceiver(descriptor, implementation);
        messagingService.registerReceiver(topicIdGenerator.getTopicId(descriptor), receiver);
        messagingService.start();
    }

    @Override
    public void stop()
    {
        // TODO shutdown generated MessagingServices
    }

    private <T> EndPoint getEndPoint(final Class<T> descriptor)
    {
        return endPointProvider.resolveEndPoint(descriptor);
    }
}