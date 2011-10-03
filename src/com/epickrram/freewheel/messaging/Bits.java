package com.epickrram.freewheel.messaging;

public final class Bits
{
    private Bits() {}

    public static void writeInt(final int value, final byte[] dest, int offset)
    {
        dest[offset] = (byte) (value >>> 24);
        dest[offset + 1] = (byte) (value >>> 16);
        dest[offset + 2] = (byte) (value >>> 8);
        dest[offset + 3] = (byte) (value);
    }

    public static int readInt(final byte[] src, int offset)
    {
        return ((((src[offset] & 0xff) << 24) |
                  ((src[offset + 1] & 0xff) << 16) |
                  ((src[offset + 2] & 0xff) <<  8) |
                  ((src[offset + 3] & 0xff) <<  0)));
    }

    public static void writeLong(final long value, final byte[] dest, final int offset)
    {
        dest[offset] = (byte) (value >> 56);
        dest[offset + 1] = (byte) (value >> 48);
        dest[offset + 2] = (byte) (value >> 40);
        dest[offset + 3] = (byte) (value >> 32);
        dest[offset + 4] = (byte) (value >> 24);
        dest[offset + 5] = (byte) (value >> 16);
        dest[offset + 6] = (byte) (value >> 8);
        dest[offset + 7] = (byte) (value >> 0);
    }

    public static long readLong(final byte[] src, int offset)
    {
        return ((((long)src[offset] & 0xff) << 56) |
                (((long)src[offset + 1] & 0xff) << 48) |
                (((long)src[offset + 2] & 0xff) << 40) |
                (((long)src[offset + 3] & 0xff) << 32) |
                (((long)src[offset + 4] & 0xff) << 24) |
                (((long)src[offset + 5] & 0xff) << 16) |
                (((long)src[offset + 6] & 0xff) <<  8) |
                (((long)src[offset + 7] & 0xff) <<  0));
    }

    public static void writeBoolean(final boolean value, final byte[] dest, final int offset)
    {
        dest[offset] = value ? (byte) 1 : 0;
    }

    public static boolean readBoolean(final byte[] src, int offset)
    {
        return src[offset] == 1;
    }
}