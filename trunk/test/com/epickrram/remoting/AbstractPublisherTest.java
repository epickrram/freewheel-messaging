package com.epickrram.remoting;

import org.junit.Assert;
import org.junit.Test;

public final class AbstractPublisherTest
{
    @Test
    public void shouldIncrementSequenceNumber() throws Exception
    {
        final StubPublisher publisher = new StubPublisher();
        Assert.assertEquals(0, publisher.getSequence());
        Assert.assertEquals(1, publisher.getSequence());
        Assert.assertEquals(2, publisher.getSequence());
        Assert.assertEquals(3, publisher.getSequence());
        Assert.assertEquals(4, publisher.getSequence());
    }

    private static final class StubPublisher extends AbstractPublisher
    {
        public StubPublisher()
        {
            super(null, 0);
        }
    }
}