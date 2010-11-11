package com.epickrram.remoting;

import com.epickrram.junit.PerfTest;
import com.epickrram.junit.PerfTestRunner;
import com.epickrram.messaging.Receiver;
import com.epickrram.stream.ByteArrayInputBufferImpl;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(PerfTestRunner.class)
public final class SubscriberFactoryPerfTest
{
    private static final int ITERATIONS = 20000000;
    private static SubscriberFactory subscriberFactory;
    private static Impl instance;
    private static Receiver nonCastingSubscriber;

    @PerfTest(name = "subscriber", iterations = ITERATIONS, warmUpRuns = 10)
    @Test
    public void shouldNotCast() throws Exception
    {
        final ByteArrayInputBufferImpl inputBuffer = new ByteArrayInputBufferImpl(new byte[]{0}, 0, 1);
        for(int i = 0; i < ITERATIONS; i++)
        {
            nonCastingSubscriber.onMessage(-1, inputBuffer);
            inputBuffer.reset();
        }

        if(instance.count > System.currentTimeMillis())
        {
            System.err.println("?");
        }
    }

    @BeforeClass
    public static void startUp()
    {
        subscriberFactory = new SubscriberFactory();

        instance = new Impl();
        nonCastingSubscriber = subscriberFactory.createReceiver(SingleNoArgsMethodInterface.class, instance);
    }

    private static final class Impl implements SingleNoArgsMethodInterface
    {
        private int count;

        public void invoke()
        {
            count++;
        }
    }

    private interface SingleNoArgsMethodInterface
    {
        void invoke();
    }
}