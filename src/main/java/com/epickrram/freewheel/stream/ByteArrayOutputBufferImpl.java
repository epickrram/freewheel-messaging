package com.epickrram.freewheel.stream;

import com.epickrram.freewheel.messaging.Bits;

public final class ByteArrayOutputBufferImpl implements ByteOutputBuffer
{
    private static final int BUFFER_SIZE = 2048;

    private final byte[] originalBuffer;

    private byte[] currentBuffer;
    private int position;
    private int count;

    public ByteArrayOutputBufferImpl()
    {
        this(BUFFER_SIZE);
    }

    public ByteArrayOutputBufferImpl(final int bufferSize)
    {
        originalBuffer = new byte[bufferSize];
        currentBuffer = originalBuffer;
        position = 0;
        count = 0;
    }

    public void writeBoolean(final boolean value)
    {
        ensureCapacity(1);
        Bits.writeBoolean(value, currentBuffer, position);
        position += 1;
        count += 1;
    }

    public void writeLong(final long value)
    {
        ensureCapacity(8);
        Bits.writeLong(value, currentBuffer, position);
        position += 8;
        count += 8;
    }

    public void writeInt(final int value)
    {
        ensureCapacity(4);
        Bits.writeInt(value, originalBuffer, position);
        position += 4;
        count += 4;
    }

    public void writeByte(final byte value)
    {
        ensureCapacity(1);
        currentBuffer[position] = value;
        position += 1;
        count += 1;
    }

    public void writeBytes(final byte[] value, final int srcOffset, final int srcLength)
    {
        ensureCapacity(srcLength);
        System.arraycopy(value, srcOffset, currentBuffer, position, srcLength);
        position += srcLength;
        count += srcLength;
    }

    public void copyInto(final byte[] dest, final int destOffset, final int length)
    {
        System.arraycopy(currentBuffer, position, dest, destOffset, length);
    }

    public void flip()
    {
        position = 0;
    }

    public int position()
    {
        return position;
    }

    public void setPosition(final int position)
    {
        this.position = position;
    }

    public int count()
    {
        return count;
    }

    public void reset()
    {
        position = 0;
        count = 0;
        currentBuffer = originalBuffer;
    }

    public byte[] getBackingArray()
    {
        return currentBuffer;
    }

    private void ensureCapacity(final int length)
    {
        if(currentBuffer.length - position < length)
        {
            System.err.println("growing");
            byte[] old = currentBuffer;
            currentBuffer = new byte[currentBuffer.length * 2];
            System.arraycopy(old, 0, currentBuffer, 0, count);
        }
    }
}