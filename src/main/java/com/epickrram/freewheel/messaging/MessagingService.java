package com.epickrram.freewheel.messaging;

import java.io.ByteArrayOutputStream;

public interface MessagingService
{
    void send(int topicId, ByteArrayOutputStream byteArrayOutputStream) throws MessagingException;
    void registerReceiver(int topicId, Receiver receiver);

    <T> void registerPublisher(final Class<T> descriptor);
    <T> void registerSubscriber(final Class<T> descriptor);

    void start() throws MessagingException;
    void shutdown() throws MessagingException;
}