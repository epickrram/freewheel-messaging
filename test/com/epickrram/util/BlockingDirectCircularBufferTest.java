package com.epickrram.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings({"UnnecessaryBoxing"})
public final class BlockingDirectCircularBufferTest
{
    private static final int DEFAULT_BUFFER_SIZE = 8;

    private DirectCircularBuffer<Integer> circularBuffer;

    @Test
    public void shouldStartAtNegativeOne() throws Exception
    {
        Assert.assertEquals(-1, circularBuffer.getSequence());
    }

    @Test
    public void shouldReportCurrentSequenceBasedOnContiguousEntries() throws Exception
    {
        circularBuffer.set(0, Integer.valueOf(17));
        circularBuffer.set(1, Integer.valueOf(19));
        circularBuffer.set(3, Integer.valueOf(23));

        Assert.assertEquals(1, circularBuffer.getSequence());

        circularBuffer.set(2, Integer.valueOf(31));

        Assert.assertEquals(3, circularBuffer.getSequence());
    }

    @Test
    public void shouldStoreEntriesInCorrectSlots() throws Exception
    {
        final Integer valueOne = Integer.valueOf(17);
        final Integer valueTwo = Integer.valueOf(19);
        final Integer valueThree = Integer.valueOf(23);
        circularBuffer.set(0, valueOne);
        circularBuffer.set(1, valueTwo);
        circularBuffer.set(3, valueThree);

        Assert.assertEquals(valueOne, circularBuffer.get(0));
        Assert.assertEquals(valueTwo, circularBuffer.get(1));
        Assert.assertEquals(valueThree, circularBuffer.get(3));
    }

    @Test
    public void shouldWrapValues() throws Exception
    {
        for(int i = 0; i < DEFAULT_BUFFER_SIZE + 2; i++)
        {
            circularBuffer.set(i, Integer.valueOf(i));
        }

        Assert.assertEquals(DEFAULT_BUFFER_SIZE + 1, circularBuffer.getSequence());
        Assert.assertEquals((Integer) DEFAULT_BUFFER_SIZE, circularBuffer.get(0L));
    }

    @Ignore
    @Test
    public void shouldRecordContiguousSequenceWhileWrapping() throws Exception
    {
        Assert.fail("Not yet implemented");
    }

    @Before
    public void setUp() throws Exception
    {
        circularBuffer = new BlockingDirectCircularBuffer<Integer>(DEFAULT_BUFFER_SIZE);
    }
}