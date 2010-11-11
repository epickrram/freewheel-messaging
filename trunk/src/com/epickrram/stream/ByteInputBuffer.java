package com.epickrram.stream;

public interface ByteInputBuffer
{
    int readInt();

    long readLong();

    boolean readBoolean();

    byte readByte();

    void readBytes(byte[] dest, int offset, int length);

    int position();

    int remaining();
}
