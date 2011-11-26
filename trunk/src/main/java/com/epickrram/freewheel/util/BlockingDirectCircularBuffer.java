//////////////////////////////////////////////////////////////////////////////////
//   Copyright 2011   Mark Price     mark at epickrram.com                      //
//                                                                              //
//   Licensed under the Apache License, Version 2.0 (the "License");            //
//   you may not use this file except in compliance with the License.           //
//   You may obtain a copy of the License at                                    //
//                                                                              //
//       http://www.apache.org/licenses/LICENSE-2.0                             //
//                                                                              //
//   Unless required by applicable law or agreed to in writing, software        //
//   distributed under the License is distributed on an "AS IS" BASIS,          //
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   //
//   See the License for the specific language governing permissions and        //
//   limitations under the License.                                             //
//////////////////////////////////////////////////////////////////////////////////
package com.epickrram.freewheel.util;

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
        writeLock.lock();
        try
        {
            data[getIndex(sequence)] = item;
            contiguousSequence.set(sequence);
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
            return data[getIndex(sequence)];
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
