package com.epickrram.freewheel.messaging.ptp;

import com.epickrram.freewheel.messaging.MessagingService;
import com.epickrram.freewheel.messaging.MessagingServiceRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PointToPointMessagingServiceRegistry implements MessagingServiceRegistry
{
    private final Map<EndPoint, PointToPointMessagingService> messagingServiceMap =
            new ConcurrentHashMap<EndPoint, PointToPointMessagingService>();

    @Override
    public MessagingService getMessagingService(final Class descriptor)
    {
        return getMessagingService(getEndPointProvider().resolveEndPoint(descriptor));
    }

    private MessagingService getMessagingService(final EndPoint endPoint)
    {
        PointToPointMessagingService messagingService = messagingServiceMap.get(endPoint);
        if(messagingService == null)
        {
            messagingService = new PointToPointMessagingService();
            final PointToPointMessagingService existing = messagingServiceMap.put(endPoint, messagingService);
            if(existing != null)
            {
                messagingService = existing;
            }
            else
            {
                messagingService.start();
            }
        }
        return messagingService;
    }

    private EndPointProvider getEndPointProvider()
    {
        throw new UnsupportedOperationException();
    }
}