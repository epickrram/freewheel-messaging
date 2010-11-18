package com.epickrram.util;

public final class ContiguousSequenceImpl implements ContiguousSequence
{
    private final int bufferSize;
    private final boolean[] set;
    private long highestSequenceSeen = -1;
    private long highestContiguousSequence = -1;
    private long highwaterMark = -1;

    public ContiguousSequenceImpl(final int bufferSize)
    {
        this.bufferSize = bufferSize;
        set = new boolean[this.bufferSize];
    }

    public void set(final long sequence)
    {
        if (sequence > highestContiguousSequence + bufferSize)
        {
            throw new IllegalStateException("Buffer would wrap");
        }
        highestSequenceSeen = Math.max(highestSequenceSeen, sequence);
        final int index = getIndex(sequence);
        set[index] = true;
        if (index == bufferSize - 1)
        {
            for (int i = 0; i < getIndex(highwaterMark); i++)
            {
                set[i] = false;
            }
        }
        if (sequence - 1 == highestContiguousSequence)
        {
            highestContiguousSequence++;
            int nextTest = (int) (sequence + 1);
            while (nextTest <= highestSequenceSeen && set[getIndex(nextTest++)])
            {
                highestContiguousSequence++;
            }
        }
    }

    public long getHighestContiguousSequence()
    {
        highwaterMark = highestContiguousSequence;
        return highestContiguousSequence;
    }

    private int getIndex(final long sequence)
    {
        return (int) (sequence % bufferSize);
    }
}
