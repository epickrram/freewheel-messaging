package com.epickrram.util;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class BlockingDirectCircularBuffer<T> implements DirectCircularBuffer<T>
{
    private final T[] data;
    private final int size;
    private final ReentrantReadWriteLock.WriteLock writeLock;
    private final ReentrantReadWriteLock.ReadLock readLock;
    private final ContiguousSequence contiguousSequence;

    @SuppressWarnings({"unchecked"})
    public BlockingDirectCircularBuffer(final int bufferSize)
    {
        data = (T[]) new Object[bufferSize];
        size = bufferSize;
        contiguousSequence = new ContiguousSequenceImpl(size);
        final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        writeLock = readWriteLock.writeLock();
        readLock = readWriteLock.readLock();
    }

    public void set(final long sequence, final T item)
    {
        System.err.println("writeLock.lock");
        writeLock.lock();
        try
        {
            System.err.println("locked");
            data[getIndex(sequence)] = item;
            contiguousSequence.set(sequence);
        }
        finally
        {
            writeLock.unlock();
            System.err.println("unlocked");
        }
    }

    public T get(final long sequence)
    {
        readLock.lock();
        try
        {
            return data[getIndex(sequence)];
        }
        finally
        {
            readLock.unlock();
        }
    }

    public long getSequence()
    {
        readLock.lock();
        try
        {
            return contiguousSequence.getHighestContiguousSequence();
        }
        finally
        {
            readLock.unlock();
        }
    }

    private int getIndex(final long sequence)
    {
        return (int) (sequence % size);
    }
}
