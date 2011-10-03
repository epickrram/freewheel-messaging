package com.epickrram.messaging;

import com.epickrram.stream.ByteArrayOutputBufferImpl;
import com.epickrram.util.DirectCircularBuffer;

import java.util.logging.Logger;

public final class SenderImpl implements Sender<ByteArrayOutputBufferImpl>
{
    private static final Logger LOGGER = Logger.getLogger(SenderImpl.class.getName());

    private final DirectCircularBuffer<ByteArrayOutputBufferImpl> buffer;
    private final MessagingService messagingService;
    private final Thread thread;
    private final int topicId;

    public SenderImpl(final DirectCircularBuffer<ByteArrayOutputBufferImpl> buffer,
                      final MessagingService messagingService, final int topicId)
    {
        this.buffer = buffer;
        this.messagingService = messagingService;
        this.topicId = topicId;
        this.thread = new Thread(new MessageSender(), "Sender");
    }

    public void send(final ByteArrayOutputBufferImpl item, final long sequence)
    {
        final ByteArrayOutputBufferImpl existingItem = buffer.get(sequence);
        if(existingItem == null)
        {
            final ByteArrayOutputBufferImpl newItem = new ByteArrayOutputBufferImpl(item.count());
            newItem.copy(item);
            buffer.set(sequence, newItem);
        }
        else
        {
            existingItem.copy(item);
        }
        LOGGER.info("inserted message at " + sequence);
    }

    public void start()
    {
        thread.start();
    }

    public void stop()
    {
        thread.interrupt();
        try
        {
            thread.join();
        }
        catch (InterruptedException e)
        {
            // log warning
        }
    }

    private final class MessageSender implements Runnable
    {
        public void run()
        {
            LOGGER.info("Waiting for messages in circular buffer");
            long sentSequence = -1L;
            while(!Thread.currentThread().isInterrupted())
            {
                long lastSequenceSent = -1L;
                while(lastSequenceSent != buffer.getSequence())
                {
                    messagingService.send(topicId, buffer.get(++sentSequence));
                    lastSequenceSent = sentSequence;
                    LOGGER.info("sent message at " + lastSequenceSent);
                }
                try
                {
                    Thread.sleep(500L);
                }
                catch (InterruptedException e)
                {
                    // ignore for now
                }
            }
        }
    }
}