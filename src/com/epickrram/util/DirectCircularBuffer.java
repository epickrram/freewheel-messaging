package com.epickrram.util;

public interface DirectCircularBuffer<T> extends CircularBuffer<T>
{
    void set(T item, long sequence);
}