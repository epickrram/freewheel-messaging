/*
Lifted from Java Concurrency In Practice, Goetz et al.
 */
package com.epickrram.freewheel.util;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public final class Memoizer<K, V>
{
    private final ConcurrentMap<K, Future<V>> valueCache = new ConcurrentHashMap<K, Future<V>>();

    public V getValue(final K key, final Provider<K, V> valueProvider)
    {
        while(true)
        {
            Future<V> future = valueCache.get(key);
            if(future == null)
            {
                final Callable<V> providerMethod = new Callable<V>()
                {
                    @Override
                    public V call() throws Exception
                    {
                        return valueProvider.provide(key);
                    }
                };
                final FutureTask<V> task = new FutureTask<V>(providerMethod);
                future = valueCache.putIfAbsent(key, task);
                if(future == null)
                {
                    future = task;
                    task.run();
                }
            }
            try
            {
                return future.get();
            }
            catch (CancellationException e)
            {
                valueCache.remove(key);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException("Could not provide value for " + key);
            }
            catch (ExecutionException e)
            {
                throw new RuntimeException(e.getCause());
            }
        }
    }
}