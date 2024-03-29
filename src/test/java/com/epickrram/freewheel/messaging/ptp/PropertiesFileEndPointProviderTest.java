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
package com.epickrram.freewheel.messaging.ptp;

import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public final class PropertiesFileEndPointProviderTest
{
    private static final String VALID_RESOURCE_NAME = "endpoint/valid-end-point.properties";
    private static final String INVALID_RESOURCE_NAME = "endpoint/invalid-end-point.properties";
    private static final String INCOMPLETE_RESOURCE_NAME = "endpoint/incomplete-end-point.properties";
    private static final String SERVICE_A_HOST = "192.168.1.12";
    private static final int SERVICE_A_PORT = 1234;
    private static final String SERVICE_B_HOST = "192.168.1.24";
    private static final int SERVICE_B_PORT = 5678;

    @Test
    public void shouldProvideEndPointFromPropertiesResource() throws Exception
    {
        assertEndPoint(ServiceA.class, SERVICE_A_HOST, SERVICE_A_PORT);
        assertEndPoint(ServiceB.class, SERVICE_B_HOST, SERVICE_B_PORT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfResourceDoesNotExist() throws Exception
    {
        new PropertiesFileEndPointProvider("non-existent.properties").resolveEndPoint(ServiceA.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfPortIsMisconfigured() throws Exception
    {
        new PropertiesFileEndPointProvider(INVALID_RESOURCE_NAME).resolveEndPoint(ServiceA.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfHostIsUnknown() throws Exception
    {
        new PropertiesFileEndPointProvider(INVALID_RESOURCE_NAME).resolveEndPoint(ServiceB.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfInformationIsIncomplete() throws Exception
    {
        new PropertiesFileEndPointProvider(INCOMPLETE_RESOURCE_NAME).resolveEndPoint(ServiceB.class);
    }

    private void assertEndPoint(final Class<?> descriptor, final String expectedHost, final int expectedPort) throws UnknownHostException
    {
        final EndPoint endPoint = new PropertiesFileEndPointProvider(VALID_RESOURCE_NAME).resolveEndPoint(descriptor);

        assertThat(endPoint.getAddress(), is(equalTo(InetAddress.getByName(expectedHost))));
        assertThat(endPoint.getPort(), is(equalTo(expectedPort)));
    }

}