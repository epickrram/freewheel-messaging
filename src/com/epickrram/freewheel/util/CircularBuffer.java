package com.epickrram.freewheel.util;

public interface CircularBuffer<T>
{
    void add(T item);
    T get(long sequence);
    long getSequence();
}