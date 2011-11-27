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
import com.epickrram.freewheel.messaging.ptp.EndPoint;
import com.epickrram.freewheel.sync.Service;
import com.epickrram.freewheel.sync.ServiceClient;
import com.epickrram.freewheel.sync.ServiceResponse;
import com.epickrram.freewheel.sync.SynchronousProxyHelper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.net.InetAddress;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public final class SynchronousProxyHelperIntegrationTest
{
    public static final BigDecimal BALANCE = new BigDecimal("12.345");
    public static final long ACCOUNT_ID = 998877L;
    
    private MessagingContext messagingContext;

    @Test
    public void shouldMakeSyncMethodInvocations() throws Exception
    {
        final ServiceResponse serviceResponse = messagingContext.createPublisher(ServiceResponse.class);
        messagingContext.createSubscriber(Service.class, new ServiceImpl(serviceResponse));
        final SynchronousProxyHelper proxyHelper = new SynchronousProxyHelper();
        final ServiceClient serviceClient =
                proxyHelper.createSynchronousProxy(Service.class, ServiceResponse.class,
                                                   ServiceClient.class, messagingContext);

        messagingContext.start();

        Assert.assertThat(serviceClient.getBalance(ACCOUNT_ID), is(equalTo(BALANCE)));
    }

    @Before
    public void setUp() throws Exception
    {
        final EndPoint multicastAddress = new EndPoint(InetAddress.getByName("239.0.0.1"), 23400);
        messagingContext = new MessagingContextFactory().createMulticastMessagingContext(multicastAddress);
    }

    @After
    public void tearDown()
    {
        messagingContext.stop();
    }

    private static final class ServiceImpl implements Service
    {
        private final ServiceResponse serviceResponse;

        public ServiceImpl(final ServiceResponse serviceResponse)
        {
            this.serviceResponse = serviceResponse;
        }

        @Override
        public void requestAccountState(final long accountId)
        {
            serviceResponse.onAccountState(accountId, BALANCE);
        }

        @Override
        public void requestMethodTwo(final String identifier)
        {

        }
    }
}