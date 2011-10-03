package com.epickrram.messaging;

import com.epickrram.Waiter;
import com.epickrram.stream.ByteOutputBuffer;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class RecordingMessagingService implements MessagingService
{
    private final ConcurrentMap<Integer, List<ByteOutputBuffer>> recordedMessages =
            new ConcurrentHashMap<Integer, List<ByteOutputBuffer>>();

    public void send(final int topicId, final ByteOutputBuffer message) throws MessagingException
    {
        getTopicMessageList(topicId).add(message);
    }

    public void registerReceiver(final int topicId, final Receiver receiver)
    {
        throw new UnsupportedOperationException();
    }

    public void start() throws MessagingException
    {
        // no-op
    }

    public void shutdown() throws MessagingException
    {
        // no-op
    }

    public void waitForMessage(final int topicId, final Matcher<ByteOutputBuffer> expectedMessage)
    {
        new Waiter(new Waiter.Condition()
        {
            public boolean isMet()
            {
                boolean matchFound = false;
                final List<ByteOutputBuffer> messageList = getTopicMessageList(topicId);
                for (ByteOutputBuffer receivedMessage : messageList)
                {
                    if(expectedMessage.matches(receivedMessage))
                    {
                        matchFound = true;
                    }
                }
                return matchFound;
            }

            public String getDescription()
            {
                final StringDescription description = new StringDescription();
                expectedMessage.describeTo(description);
                return description.toString();
            }
        }).waitForCondition();
    }

    private List<ByteOutputBuffer> getTopicMessageList(final int topicId)
    {
        List<ByteOutputBuffer> messageList = recordedMessages.get(topicId);
        if(messageList == null)
        {
            messageList = new CopyOnWriteArrayList<ByteOutputBuffer>();
            final List<ByteOutputBuffer> existing = recordedMessages.putIfAbsent(topicId, messageList);
            if(existing != null)
            {
                messageList = existing;
            }
        }

        return messageList;
    }
}
