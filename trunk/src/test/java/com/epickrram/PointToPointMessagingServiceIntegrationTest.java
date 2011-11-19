package com.epickrram;

import com.epickrram.freewheel.messaging.ptp.PointToPointMessagingHelper;
import com.epickrram.freewheel.messaging.ptp.PropertiesFileEndPointProvider;
import com.epickrram.freewheel.protocol.CodeBookImpl;
import com.epickrram.freewheel.remoting.ClassNameTopicIdGenerator;
import com.epickrram.freewheel.remoting.TopicIdGenerator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public final class PointToPointMessagingServiceIntegrationTest
{
    private PointToPointMessagingHelper messagingHelper;

    @Test
    public void shouldSendMessages() throws Exception
    {
        final TestInterfaceImpl testInterface = new TestInterfaceImpl();
        messagingHelper.createSubscriber(TestInterface.class, testInterface);
        final TestInterface proxy = messagingHelper.createPublisher(TestInterface.class);

        final int expectedCallsOnMethodOne = 500;
        final int expectedCallsOnMethodTwo = 700;

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

    @Test
    public void shouldSuccessfullyCreatePublisherIfSubscriberIsNotYetListening() throws Exception
    {
        final TestInterfaceImpl testInterface = new TestInterfaceImpl();
        Executors.newSingleThreadExecutor().submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Thread.sleep(1500L);
                }
                catch (InterruptedException e)
                {
                    // ignore
                }
                messagingHelper.createSubscriber(TestInterface.class, testInterface);
            }
        });

        final TestInterface proxy = messagingHelper.createPublisher(TestInterface.class);

        final int expectedCallsOnMethodOne = 500;
        final int expectedCallsOnMethodTwo = 700;

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
    }

    @Before
    public void setUp() throws Exception
    {
        final CodeBookImpl codeBook = new CodeBookImpl();
        final TopicIdGenerator topicIdGenerator = new ClassNameTopicIdGenerator();
        final PropertiesFileEndPointProvider endPointProvider =
                new PropertiesFileEndPointProvider(getClass().getSimpleName() + "/end-point.properties");
        messagingHelper = new PointToPointMessagingHelper(endPointProvider, codeBook, topicIdGenerator);
    }

    @After
    public void tearDown()
    {
    }

    private void waitForExpectedMethodCalls(final TestInterfaceImpl testInterface, final int expectedCallsOnMethodTwo) throws InterruptedException
    {
        final long timeout = System.currentTimeMillis() + 10000L;
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