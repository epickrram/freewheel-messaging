package com.epickrram.stream;

public interface ByteOutputBuffer
{
    void writeBoolean(boolean value);

    void writeLong(long value);

    void writeInt(int value);

    void writeByte(byte value);

    void writeBytes(byte[] value, int srcOffset, int srcLength);

    void copyInto(byte[] dest, int destOffset, int length);

    void flip();

    int position();

    int count();

    void setPosition(int position);

    void reset();

    byte[] getBackingArray();
}
