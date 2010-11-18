package com.epickrram.util;

import org.junit.Assert;
import org.junit.Test;

public final class ContiguousSequenceImplTest
{
    @Test(expected = IllegalStateException.class)
    public void shouldBlowUpIfOverflowOccurs() throws Exception
    {
        final ContiguousSequenceImpl contiguousBuffer = new ContiguousSequenceImpl(4);

        contiguousBuffer.set(0);
        contiguousBuffer.set(1);
        contiguousBuffer.set(2);
        contiguousBuffer.set(3);
        contiguousBuffer.set(4);

        Assert.assertEquals(4, contiguousBuffer.getHighestContiguousSequence());

        contiguousBuffer.set(6);
        contiguousBuffer.set(7);
        contiguousBuffer.set(8);
        contiguousBuffer.set(9);
    }

    @Test
    public void shouldReportHighestContiguousSequence() throws Exception
    {
        final ContiguousSequenceImpl contiguousBuffer = new ContiguousSequenceImpl(8);
        Assert.assertEquals(-1, contiguousBuffer.getHighestContiguousSequence());

        contiguousBuffer.set(0);
        Assert.assertEquals(0, contiguousBuffer.getHighestContiguousSequence());

        contiguousBuffer.set(1);
        Assert.assertEquals(1, contiguousBuffer.getHighestContiguousSequence());

        contiguousBuffer.set(6);
        Assert.assertEquals(1, contiguousBuffer.getHighestContiguousSequence());

        contiguousBuffer.set(5);
        Assert.assertEquals(1, contiguousBuffer.getHighestContiguousSequence());

        contiguousBuffer.set(2);
        Assert.assertEquals(2, contiguousBuffer.getHighestContiguousSequence());

        contiguousBuffer.set(4);
        Assert.assertEquals(2, contiguousBuffer.getHighestContiguousSequence());

        contiguousBuffer.set(3);
        Assert.assertEquals(6, contiguousBuffer.getHighestContiguousSequence());
    }

    @Test
    public void shouldHandleWrapping() throws Exception
    {
        final ContiguousSequenceImpl contiguousBuffer = new ContiguousSequenceImpl(8);
        Assert.assertEquals(-1, contiguousBuffer.getHighestContiguousSequence());

        contiguousBuffer.set(0);
        Assert.assertEquals(0, contiguousBuffer.getHighestContiguousSequence());

        contiguousBuffer.set(1);
        Assert.assertEquals(1, contiguousBuffer.getHighestContiguousSequence());

        contiguousBuffer.set(4);
        Assert.assertEquals(1, contiguousBuffer.getHighestContiguousSequence());

        contiguousBuffer.set(3);
        Assert.assertEquals(1, contiguousBuffer.getHighestContiguousSequence());

        contiguousBuffer.set(2);
        Assert.assertEquals(4, contiguousBuffer.getHighestContiguousSequence());

        contiguousBuffer.set(5);
        contiguousBuffer.set(6);
        contiguousBuffer.set(7);
        contiguousBuffer.set(8);

        Assert.assertEquals(8, contiguousBuffer.getHighestContiguousSequence());

        contiguousBuffer.set(11);
        Assert.assertEquals(8, contiguousBuffer.getHighestContiguousSequence());

        contiguousBuffer.set(9);
        Assert.assertEquals(9, contiguousBuffer.getHighestContiguousSequence());

        contiguousBuffer.set(10);
        Assert.assertEquals(11, contiguousBuffer.getHighestContiguousSequence());
    }
}
