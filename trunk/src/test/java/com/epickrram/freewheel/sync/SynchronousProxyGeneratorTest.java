package com.epickrram.freewheel.sync;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.concurrent.*;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public final class SynchronousProxyGeneratorTest
{
    private static final long ACCOUNT_ID = 1234L;
    private static final BigDecimal BALANCE = new BigDecimal("888.90");
    private static final String RESPONSE_VALUE = "RESPONSE_VALUE";

    private SynchronousProxyGenerator generator;
    private Service service;
    private ServiceResponse serviceResponse;

    @Before
    public void before() throws Exception
    {
        generator = new SynchronousProxyGenerator();
        service = mock(Service.class);
    }

    @Test
    public void shouldGenerateClientThatImplementsResponseInterface() throws Exception
    {
        assertTrue(generator.generateProxy(service, ServiceResponse.class, ServiceClient.class) instanceof ServiceResponse);
    }

    @Test
    public void shouldGenerateClientThatListensToServiceResponseForSingleLongIdAndResponseValue() throws Exception
    {
        final ServiceClient serviceClient = generator.generateProxy(new ServiceImplOne(), ServiceResponse.class, ServiceClient.class);
        serviceResponse = (ServiceResponse) serviceClient;

        final BigDecimal balance = serviceClient.getBalance(ACCOUNT_ID);
        assertThat(balance, is(BALANCE));
    }

    @Test
    public void shouldGenerateClientThatListensToServiceResponseForSingleStringIdAndResponseValue() throws Exception
    {
        final ServiceImplTwo serviceImplTwo = new ServiceImplTwo(RESPONSE_VALUE);
        final ServiceClient serviceClient = generator.generateProxy(serviceImplTwo, ServiceResponse.class, ServiceClient.class);
        serviceResponse = (ServiceResponse) serviceClient;

        serviceImplTwo.setResponse((ServiceResponse) serviceClient);

        final String response = serviceClient.getMethodTwo("request-id");
        assertThat(response, is(RESPONSE_VALUE));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldTimeoutAfterSpecifiedInterval() throws Exception
    {
        final ServiceClient serviceClient = generator.generateProxy(new ServiceImplOne(), ServiceResponse.class, ServiceClient.class);
        serviceResponse = (ServiceResponse) serviceClient;

        serviceClient.timeoutAfterOneMillisecond(ACCOUNT_ID);
    }

    @Test
    public void shouldServeMultipleThreadsSimultaneously() throws Exception
    {
        final ServiceImplTwo implOne = new ServiceImplTwo("one");
        final ServiceImplTwo implTwo = new ServiceImplTwo("two");
        final ServiceClient clientOne = generator.generateProxy(implOne, ServiceResponse.class, ServiceClient.class);
        final ServiceClient clientTwo = generator.generateProxy(implTwo, ServiceResponse.class, ServiceClient.class);
        implOne.setResponse((ServiceResponse) clientOne);
        implTwo.setResponse((ServiceResponse) clientTwo);

        final ExecutorService executorService = Executors.newFixedThreadPool(2);
        final Future<String> clientOneInvocation = executorService.submit(new Callable<String>()
        {
            @Override
            public String call() throws Exception
            {
                return clientOne.getMethodTwo("id-1");
            }
        });
        final Future<String> clientTwoInvocation = executorService.submit(new Callable<String>()
        {
            @Override
            public String call() throws Exception
            {
                return clientTwo.getMethodTwo("id-2");
            }
        });

        assertThat(clientOneInvocation.get(), is("one"));
        assertThat(clientTwoInvocation.get(), is("two"));
    }

    private final class ServiceImplOne extends ServiceStub
    {
        @Override
        public void requestAccountState(final long accountId)
        {
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    serviceResponse.onAccountState(23874L, BigDecimal.TEN);
                    try
                    {
                        // time for calling thread to wake up
                        Thread.sleep(10L);
                    }
                    catch (InterruptedException e)
                    {
                        // ignore
                    }
                    serviceResponse.onAccountState(ACCOUNT_ID, BALANCE);
                }
            }).start();
        }
    }

    private final class ServiceImplTwo extends ServiceStub
    {
        private final String responseValue;
        private volatile ServiceResponse response;

        public ServiceImplTwo(final String responseValue)
        {
            this.responseValue = responseValue;
        }

        @Override
        public void requestMethodTwo(final String id)
        {
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        Thread.sleep(10L);
                    }
                    catch (InterruptedException e)
                    {
                        // ignore
                    }
                    response.onMethodTwoResponse(id, responseValue);
                }
            }).start();
        }

        void setResponse(final ServiceResponse response)
        {
            this.response = response;
        }
    }

    private abstract class ServiceStub implements Service
    {
        @Override
        public void requestAccountState(final long accountId)
        {
        }

        @Override
        public void requestMethodTwo(final String identifier)
        {
        }
    }
}