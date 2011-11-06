package com.epickrram.freewheel.messaging.ptp;

import com.epickrram.freewheel.messaging.MessagingException;
import com.epickrram.freewheel.messaging.MessagingService;
import com.epickrram.freewheel.messaging.Receiver;

import java.io.ByteArrayOutputStream;

// TODO - split into publishing service, subscribing service?
public final class PointToPointMessagingService implements MessagingService
{
    @Override
    public void send(final int topicId, final ByteArrayOutputStream byteArrayOutputStream) throws MessagingException
    {
    }

    @Override
    public void registerReceiver(final int topicId, final Receiver receiver)
    {
    }

    @Override
    public void start() throws MessagingException
    {
    }

    @Override
    public void shutdown() throws MessagingException
    {
    }
}
