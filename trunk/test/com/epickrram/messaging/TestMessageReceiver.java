package com.epickrram.messaging;

import com.epickrram.stream.ByteInputBuffer;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TestMessageReceiver implements Receiver
{
    private static final long TIMEOUT_MILLIS = 5000L;

    private final ConcurrentMap<Integer, List<byte[]>> topicMessages = new ConcurrentHashMap<Integer, List<byte[]>>();

    public void onMessage(final int topicId, final ByteInputBuffer byteInputBuffer)
    {
        List<byte[]> topicMessageList = topicMessages.get(topicId);
        if(topicMessageList == null)
        {
            topicMessageList = new CopyOnWriteArrayList<byte[]>();
            final List<byte[]> existing = topicMessages.putIfAbsent(topicId, topicMessageList);
            if(existing != null)
            {
                topicMessageList = existing;
            }
        }
        final int len = byteInputBuffer.remaining();
        final byte[] payload = new byte[len];
        byteInputBuffer.readBytes(payload, 0, len);
        topicMessageList.add(payload);
    }

    public void waitForMessageReceived(final int topicId, final byte[] expectedMessage)
    {
        final long timeout = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while(System.currentTimeMillis() < timeout)
        {
            final List<byte[]> topicMessageList = topicMessages.get(topicId);
            if(topicMessageList != null)
            {
                for (byte[] message : topicMessageList)
                {
                    if(Arrays.equals(expectedMessage, message))
                    {
                        return;
                    }
                }
            }
            try
            {
                Thread.sleep(250L);
            }
            catch (InterruptedException e)
            {
                // ignore
            }
        }
        org.junit.Assert.fail("Did not find expected message " + Arrays.toString(expectedMessage) + " for topic " + topicId);
    }
}
