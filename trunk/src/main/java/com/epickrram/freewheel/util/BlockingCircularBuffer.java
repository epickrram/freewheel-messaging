package com.epickrram.freewheel.util;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class BlockingCircularBuffer<T> implements CircularBuffer<T>
{
    private final T[] data;
    private final int size;
    private final ReentrantReadWriteLock.WriteLock writeLock;
    private final ReentrantReadWriteLock.ReadLock readLock;

    private long currentSequence = -1;

    @SuppressWarnings({"unchecked"})
    public BlockingCircularBuffer(final int bufferSize)
    {
        data = (T[]) new Object[bufferSize];
        size = bufferSize;
        final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        writeLock = readWriteLock.writeLock();
        readLock = readWriteLock.readLock();
    }

    public void add(final T item)
    {
        writeLock.lock();
        try
        {
            data[(int) (++currentSequence % size)] = item;
        }
        finally
        {
            writeLock.unlock();
        }
    }

    public T get(final long sequence)
    {
        readLock.lock();
        try
        {
            return data[(int) (sequence % size)];
        }
        finally
        {
            readLock.lock();
        }
    }

    public long getSequence()
    {
        readLock.lock();
        try
        {
            return currentSequence;
        }
        finally
        {
            readLock.unlock();
        }
    }
}