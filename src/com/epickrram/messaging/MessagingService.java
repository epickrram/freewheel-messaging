package com.epickrram.messaging;

import com.epickrram.stream.ByteOutputBuffer;

public interface MessagingService
{
    void send(int topicId, ByteOutputBuffer message) throws MessagingException;
    void registerReceiver(int topicId, Receiver receiver);

    void start() throws MessagingException;
    void shutdown() throws MessagingException;
}