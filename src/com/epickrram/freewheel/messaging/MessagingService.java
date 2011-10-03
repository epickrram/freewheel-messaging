package com.epickrram.freewheel.messaging;

import java.io.ByteArrayOutputStream;

public interface MessagingService
{
    void send(int topicId, ByteArrayOutputStream byteArrayOutputStream) throws MessagingException;
    void registerReceiver(int topicId, Receiver receiver);

    void start() throws MessagingException;
    void shutdown() throws MessagingException;
}