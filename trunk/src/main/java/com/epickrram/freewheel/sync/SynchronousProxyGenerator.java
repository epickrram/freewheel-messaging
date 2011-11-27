package com.epickrram.freewheel.sync;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.currentThread;
import static java.lang.reflect.Proxy.newProxyInstance;

public final class SynchronousProxyGenerator
{
    @SuppressWarnings({"unchecked"})
    public <Service, Response, Client> Client generateProxy(final Service service, final Class<Response> response, final Class<Client> clientClass)
    {
        return (Client) newProxyInstance(currentThread().getContextClassLoader(),
                new Class[]{clientClass, response},
                new SynchronousProxyInvocationHandler(clientClass, response, service));
    }

    private static final class ResponseListener
    {
        private final Object responseIdentifier;
        private final int responseValueIndex;
        private final int responseIdentifierArgsIndex;
        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile Object responseValue;

        private ResponseListener(final Object responseIdentifier, final SyncMethod descriptor)
        {
            this.responseIdentifier = responseIdentifier;
            this.responseValueIndex = descriptor.responseParameterValueIndex();
            this.responseIdentifierArgsIndex = descriptor.responseParameterIdentifierIndex();
        }

        private boolean handleResponse(final Object[] args)
        {
            if(args[responseIdentifierArgsIndex].equals(responseIdentifier))
            {
                responseValue = args[responseValueIndex];
                return true;
            }

            return false;
        }

        private CountDownLatch latch()
        {
            return latch;
        }

        private Object getResponseValue()
        {
            return responseValue;
        }

        private void complete()
        {
            latch.countDown();
        }
    }

    private class SynchronousProxyInvocationHandler<Client, Response, Service> implements InvocationHandler
    {
        private final Map<String, List<ResponseListener>> responseListenersByMethodNameMap = new HashMap<String, List<ResponseListener>>();
        private final Class<Client> clientClass;
        private final Class<Response> responseClass;
        private final Service service;

        public SynchronousProxyInvocationHandler(final Class<Client> clientClass, final Class<Response> responseClass, final Service service)
        {
            this.clientClass = clientClass;
            this.responseClass = responseClass;
            this.service = service;
        }

        private void validateMethodNames(final String asyncRequestMethodName, final String asyncResponseMethodName)
        {
            getMethodByName(asyncRequestMethodName, service.getClass());
            getMethodByName(asyncResponseMethodName, responseClass);
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
        {
            if(method.getDeclaringClass() == clientClass)
            {
                final SyncMethod syncMethod = method.getAnnotation(SyncMethod.class);
                validateMethodNames(syncMethod.requestMethod(), syncMethod.responseMethod());

                final Method requestMethod = getMethodByName(syncMethod.requestMethod(), service.getClass());
                final ResponseListener responseListener = new ResponseListener(args[syncMethod.requestParameterIdentifierIndex()], syncMethod);

                addListener(syncMethod.requestMethod(), syncMethod.responseMethod(), responseListener);

                requestMethod.invoke(service, args);

                waitForResponse(syncMethod, syncMethod.responseMethod(), responseListener);

                return responseListener.getResponseValue();
            }
            else if(method.getDeclaringClass() == responseClass)
            {
                notifyInterestedListeners(method, args);
            }

            return null;
        }

        private void waitForResponse(final SyncMethod syncMethod, final String asyncResponseMethodName, final ResponseListener responseListener) throws InterruptedException
        {
            // TODO wait interruptibly
            responseListener.latch().await(syncMethod.timeoutMilliseconds(), TimeUnit.MILLISECONDS);
            if(responseListener.latch().getCount() != 0)
            {
                throw new IllegalStateException("Did not receive response for method " + responseClass.getSimpleName() + "." +
                        asyncResponseMethodName + " within " + syncMethod.timeoutMilliseconds() + "ms");
            }
        }

        private void addListener(final String asyncRequestMethodName, final String asyncResponseMethodName, final ResponseListener responseListener)
        {
            synchronized (responseListenersByMethodNameMap)
            {
                List<ResponseListener> responseListeners = responseListenersByMethodNameMap.get(asyncRequestMethodName);
                if(responseListeners == null)
                {
                    responseListeners = new ArrayList<ResponseListener>();
                    responseListenersByMethodNameMap.put(asyncResponseMethodName, responseListeners);
                }
                responseListeners.add(responseListener);
            }
        }

        private void notifyInterestedListeners(final Method method, final Object[] args)
        {
            final String responseMethodName = method.getName();
            synchronized (responseListenersByMethodNameMap)
            {
                if(responseListenersByMethodNameMap.containsKey(responseMethodName))
                {
                    final List<ResponseListener> responseListeners = responseListenersByMethodNameMap.get(responseMethodName);
                    for (Iterator<ResponseListener> iterator = responseListeners.iterator(); iterator.hasNext(); )
                    {
                        final ResponseListener responseListener = iterator.next();
                        if (responseListener.handleResponse(args))
                        {
                            responseListener.complete();
                            iterator.remove();
                        }
                    }
                }
            }
        }

        @SuppressWarnings({"TypeParameterExplicitlyExtendsObject"})
        private Method getMethodByName(final String methodName, final Class<? extends Object> declaringClass)
        {
            for (Method method : declaringClass.getDeclaredMethods())
            {
                if(method.getName().equals(methodName))
                {
                    return method;
                }
            }
            throw new IllegalArgumentException("Cannot find method named: " + methodName);
        }
    }
}