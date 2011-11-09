package com.epickrram.freewheel.messaging;

public interface MessagingHelper
{
    <T> T createPublisher(Class<T> descriptor) throws MessagingException;

    <T> void createSubscriber(Class<T> descriptor, T implementation) throws MessagingException;

    void stop();
}
