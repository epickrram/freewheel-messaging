package com.epickrram.messaging;

public interface Sender<T>
{
    void send(T item, long sequence);

    void start();
    void stop();
}