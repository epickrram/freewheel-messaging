package com.epickrram;

import com.epickrram.freewheel.io.ClassnameCodeBook;
import com.epickrram.freewheel.messaging.MessagingService;
import com.epickrram.freewheel.messaging.MessagingServiceImpl;
import com.epickrram.freewheel.messaging.Receiver;
import com.epickrram.freewheel.remoting.ClassNameTopicIdGenerator;
import com.epickrram.freewheel.remoting.PublisherFactory;
import com.epickrram.freewheel.remoting.SubscriberFactory;
import com.epickrram.freewheel.remoting.TopicIdGenerator;
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

//    @Ignore("currently failing - requires reliability features")
    @Test
    public void shouldSendMessages() throws Exception
    {
        final TestInterface proxy = publisherFactory.createPublisher(TestInterface.class);
        final TestInterfaceImpl testInterface = new TestInterfaceImpl();
        final Receiver receiver = subscriberFactory.createReceiver(TestInterface.class, testInterface);

        messagingService.registerReceiver(topicIdGenerator.getTopicId(TestInterface.class), receiver);
        messagingService.start();

        final int expectedCallsOnMethodOne = 7117;
        final int expectedCallsOnMethodTwo = 7123;

        for(int i = 0; i < expectedCallsOnMethodOne; i++)
        {
            proxy.methodOne(i);
        }

        for(int i = 0; i < expectedCallsOnMethodTwo; i++)
        {
            proxy.methodTwo(17L, i, (byte) 127);
        }

        waitForExpectedMethodCalls(testInterface, expectedCallsOnMethodTwo);

        Assert.assertEquals(printMissing(testInterface.methodOneInvocationArguments), expectedCallsOnMethodOne, testInterface.methodOneInvocationCount);
        Assert.assertEquals(printMissing(testInterface.methodTwoInvocationArguments), expectedCallsOnMethodTwo, testInterface.methodTwoInvocationCount);

        Assert.assertTrue(isInAscendingOrder(testInterface.methodOneInvocationArguments));
        Assert.assertTrue(isInAscendingOrder(testInterface.methodTwoInvocationArguments));
    }

    @Before
    public void setUp() throws Exception
    {
        final ClassnameCodeBook codeBook = new ClassnameCodeBook();
        messagingService = new MessagingServiceImpl(MULTICAST_ADDR, PORT_ID, codeBook);
        topicIdGenerator = new ClassNameTopicIdGenerator();
        publisherFactory = new PublisherFactory(messagingService, topicIdGenerator, codeBook);
        subscriberFactory = new SubscriberFactory();
    }

    @After
    public void tearDown()
    {
        messagingService.shutdown();
    }

    private void waitForExpectedMethodCalls(final TestInterfaceImpl testInterface, final int expectedCallsOnMethodTwo) throws InterruptedException
    {
        final long timeout = System.currentTimeMillis() + 40000L;
        while((System.currentTimeMillis() < timeout) && (testInterface.methodTwoInvocationCount < expectedCallsOnMethodTwo))
        {
            Thread.sleep(500L);
        }
    }

    private boolean isInAscendingOrder(final List<Integer> integerList)
    {
        boolean inOrder = true;
        int last = -1;
        for(int i = 0, n = integerList.size(); i < n; i++)
        {
            final Integer value = integerList.get(i);
            if(value < last)
            {
                System.err.println("Message out of order: " + value + " (last: " + last + ")");
                inOrder = false;
            }
        }
        return inOrder;
    }

    private String printMissing(final List<Integer> sequence)
    {
        final StringBuilder msg = new StringBuilder();
        int last = -1;
        for (Integer current : sequence)
        {
            if(current != last + 1)
            {
                msg.append("\nMissing messages between ").append(last).append(" and ").append(current);
            }
            last = current;
        }
        return msg.toString();
    }

    private static final class TestInterfaceImpl implements TestInterface
    {
        private final List<Integer> methodOneInvocationArguments = new ArrayList<Integer>();
        private final List<Integer> methodTwoInvocationArguments = new ArrayList<Integer>();
        private int methodOneInvocationCount = 0;
        private int methodTwoInvocationCount = 0;

        public void methodOne(final int value)
        {
            methodOneInvocationCount++;
            methodOneInvocationArguments.add(value);
        }

        public void methodTwo(final long first, final int second, final byte third)
        {
            methodTwoInvocationCount++;
            methodTwoInvocationArguments.add(second);
        }
    }

    public interface TestInterface
    {
        void methodOne(int value);
        void methodTwo(long first, int second, byte third);
    }
}