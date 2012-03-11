//////////////////////////////////////////////////////////////////////////////////
//   Copyright 2011   Mark Price     mark at epickrram.com                      //
//                                                                              //
//   Licensed under the Apache License, Version 2.0 (the "License");            //
//   you may not use this file except in compliance with the License.           //
//   You may obtain a copy of the License at                                    //
//                                                                              //
//       http://www.apache.org/licenses/LICENSE-2.0                             //
//                                                                              //
//   Unless required by applicable law or agreed to in writing, software        //
//   distributed under the License is distributed on an "AS IS" BASIS,          //
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   //
//   See the License for the specific language governing permissions and        //
//   limitations under the License.                                             //
//////////////////////////////////////////////////////////////////////////////////

package com.epickrram;

import com.epickrram.freewheel.messaging.MessagingContext;
import com.epickrram.freewheel.messaging.MessagingContextFactory;
import com.epickrram.freewheel.messaging.config.Remote;
import com.epickrram.freewheel.messaging.ptp.PropertiesFileEndPointProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public final class PointToPointMessagingServiceIntegrationTest
{
    private MessagingContext messagingContext;

    @Test
    public void shouldAllowMultipleSubscribers() throws Exception
    {
        final TestInterfaceImpl testInterfaceOne = new TestInterfaceImpl();
        final TestInterfaceImpl testInterfaceTwo = new TestInterfaceImpl();
        messagingContext.createSubscriber(TestInterface.class, testInterfaceOne);
        messagingContext.createSubscriber(TestInterface.class, testInterfaceTwo);
        final TestInterface proxy = messagingContext.createPublisher(TestInterface.class);

        messagingContext.start();

        final int expectedCallsOnMethodTwo = 50;

        for(int i = 0; i < expectedCallsOnMethodTwo; i++)
        {
            proxy.methodTwo(0L, i, (byte) 0);
        }

        waitForExpectedMethodCalls(testInterfaceOne, expectedCallsOnMethodTwo);
        waitForExpectedMethodCalls(testInterfaceTwo, expectedCallsOnMethodTwo);
    }

    @Test
    public void shouldSendMessages() throws Exception
    {
        final TestInterfaceImpl testInterface = new TestInterfaceImpl();
        messagingContext.createSubscriber(TestInterface2.class, testInterface);
        final TestInterface2 proxy = messagingContext.createPublisher(TestInterface2.class);

        messagingContext.start();

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

    @Ignore("publishers now created with deferred connection")
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
                messagingContext.createSubscriber(TestInterface.class, testInterface);
            }
        });

        final TestInterface proxy = messagingContext.createPublisher(TestInterface.class);

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
        final PropertiesFileEndPointProvider endPointProvider =
                new PropertiesFileEndPointProvider(getClass().getSimpleName() + "/end-point.properties");

        messagingContext = new MessagingContextFactory().createPointToPointMessagingContext(endPointProvider);
    }

    @After
    public void tearDown()
    {
        messagingContext.stop();
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

    private static final class TestInterfaceImpl implements TestInterface, TestInterface2
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

    @Remote
    public interface TestInterface
    {
        void methodOne(int value);
        void methodTwo(long first, int second, byte third);
    }

    @Remote
    public interface TestInterface2
    {
        void methodOne(int value);
        void methodTwo(long first, int second, byte third);
    }
}