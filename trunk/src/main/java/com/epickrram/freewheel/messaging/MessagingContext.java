package com.epickrram.freewheel.messaging;

public interface MessagingContext
{
    public <T> T createPublisher(final Class<T> descriptor) throws MessagingException;
    public <T> void createSubscriber(final Class<T> descriptor, final T implementation) throws MessagingException;
    public void start();
    public void stop();
}