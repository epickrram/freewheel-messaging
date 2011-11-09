package com.epickrram.freewheel.messaging;

public interface MessagingServiceRegistry
{
    MessagingService getMessagingService(final Class descriptor);
}
