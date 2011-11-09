package com.epickrram.freewheel.stream;

import org.junit.Assert;

import java.util.Arrays;

public final class ByteComparisonUtil
{
    private ByteComparisonUtil() {}

    static void assertByteArraysEqual(final byte[] expected, final byte[] actual)
    {
        Assert.assertTrue(printArrays(expected, actual), Arrays.equals(expected, actual));
    }

    private static String printArrays(final byte[] expected, final byte[] actual)
    {
        return "Arrays did not match. Expected " + Arrays.toString(expected) + ", actual " + Arrays.toString(actual);
    }
}
