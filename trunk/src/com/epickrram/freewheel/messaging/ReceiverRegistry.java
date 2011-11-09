package com.epickrram.freewheel.messaging;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ReceiverRegistry
{
    private final Map<Integer, Receiver> receiverByTopicIdMap = new ConcurrentHashMap<Integer, Receiver>();

    public void registerReceiver(final int topicId, final Receiver receiver)
    {
        receiverByTopicIdMap.put(topicId, receiver);
    }

    public Receiver getReceiver(final int topicId)
    {
        return receiverByTopicIdMap.get(topicId);
    }
}