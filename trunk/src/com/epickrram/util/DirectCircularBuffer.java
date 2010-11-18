package com.epickrram.util;

public interface DirectCircularBuffer<T>
{
    void set(long sequence, T item);
    T get(long sequence);
    long getSequence();
}