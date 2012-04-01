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

package com.epickrram.freewheel;

import com.epickrram.freewheel.messaging.MessagingContext;
import com.epickrram.freewheel.messaging.MessagingContextFactory;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JMock;
import org.jmock.lib.action.CustomAction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static com.epickrram.freewheel.messaging.ptp.StaticEndPointProvider.localPort;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JMock.class)
public final class BlockingPointToPointMessagingServiceIntegrationTest
{
    private static final int PORT = 17658;
    private static final Integer INT_VALUE = Integer.valueOf(42);

    private Mockery mockery = new Mockery();
    private TestSyncInterface syncInterface;
    private TestSyncInterface syncPublisher;
    private MessagingContext messagingContext;

    @Before
    public void setUp() throws Exception
    {
        messagingContext = new MessagingContextFactory().
                createDirectBlockingPointToPointMessagingContext(localPort(PORT));

        syncInterface = mockery.mock(TestSyncInterface.class);

        messagingContext.createSubscriber(TestSyncInterface.class, syncInterface);
        syncPublisher = messagingContext.createPublisher(TestSyncInterface.class);

        messagingContext.start();
    }

    @After
    public void teardown()
    {
        messagingContext.stop();
    }

    @Test
    public void shouldInvokeAsyncMethod() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);
        mockery.checking(new Expectations()
        {
            {
                oneOf(syncInterface).ayncMethod(INT_VALUE);
                will(new CustomAction("count down")
                {
                    @Override
                    public Object invoke(final Invocation invocation) throws Throwable
                    {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        syncPublisher.ayncMethod(INT_VALUE);
        latch.await();
    }

    @Test
    public void shouldPublishMessageAndReturnResponse() throws Exception
    {
        mockery.checking(new Expectations()
        {
            {
                oneOf(syncInterface).methodOne(INT_VALUE);
                will(returnValue(Integer.valueOf(INT_VALUE)));
            }
        });

        assertThat(syncPublisher.methodOne(INT_VALUE), is(INT_VALUE));
    }
}