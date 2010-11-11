package com.epickrram;

import com.epickrram.messaging.MessagingService;
import com.epickrram.messaging.MessagingServiceImpl;
import com.epickrram.messaging.Receiver;
import com.epickrram.remoting.ClassHashcodeTopicIdGenerator;
import com.epickrram.remoting.PublisherFactory;
import com.epickrram.remoting.SubscriberFactory;
import com.epickrram.remoting.TopicIdGenerator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public final class IntegrationTest
{
    private static final int PORT_ID = 8765;
    private static final String MULTICAST_ADDR = "239.0.0.1";
    private MessagingService messagingService;
    private PublisherFactory publisherFactory;
    private SubscriberFactory subscriberFactory;
    private TopicIdGenerator topicIdGenerator;

    @Test
    public void shouldSendMessages() throws Exception
    {
        final TestInterface proxy = publisherFactory.createPublisher(TestInterface.class);
        final TestInterfaceImpl testInterface = new TestInterfaceImpl();
        final Receiver receiver = subscriberFactory.createReceiver(TestInterface.class, testInterface);

        messagingService.registerReceiver(topicIdGenerator.getTopicId(TestInterface.class), receiver);
        messagingService.start();

        final int expectedOneCount = 7117;
        final int expectedTwoCount = 6123;

        for(int i = 0; i < expectedOneCount; i++)
        {
            proxy.methodOne(i);
        }

        for(int i = 0; i < expectedTwoCount; i++)
        {
            proxy.methodTwo(17L, i, (byte) 127);
        }

        Thread.sleep(5000L);

        Assert.assertEquals(printMissing(testInterface.oneArgs), expectedOneCount, testInterface.oneCount);
        Assert.assertEquals(printMissing(testInterface.twoArgs), expectedTwoCount, testInterface.twoCount);
    }

    @Before
    public void setUp() throws Exception
    {
        messagingService = new MessagingServiceImpl(MULTICAST_ADDR, PORT_ID);
        topicIdGenerator = new ClassHashcodeTopicIdGenerator();
        publisherFactory = new PublisherFactory(messagingService, topicIdGenerator);
        subscriberFactory = new SubscriberFactory();
    }

    @After
    public void tearDown()
    {
        messagingService.shutdown();
    }

    private String printMissing(final List<Integer> sequence)
    {
        final StringBuilder msg = new StringBuilder();
        int last = -1;
        for (Integer current : sequence)
        {
            if(current != last + 1)
            {
                msg.append("Missing messages between ").append(last).append(" and ").append(current).append("\n");
            }
            last = current;
        }
        return msg.toString();
    }

    private static final class TestInterfaceImpl implements TestInterface
    {
        private final List<Integer> oneArgs = new ArrayList<Integer>();
        private final List<Integer> twoArgs = new ArrayList<Integer>();
        private int oneCount = 0;
        private int twoCount = 0;

        public void methodOne(final int value)
        {
            oneCount++;
            oneArgs.add(value);
        }

        public void methodTwo(final long first, final int second, final byte third)
        {
            twoCount++;
            twoArgs.add(second);
        }
    }

    public interface TestInterface
    {
        void methodOne(int value);
        void methodTwo(long first, int second, byte third);
    }
}