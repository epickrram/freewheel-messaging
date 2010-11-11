package com.epickrram.messaging;

import com.epickrram.stream.ByteArrayOutputBufferImpl;
import com.epickrram.stream.ByteOutputBuffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class MessagingServiceImplTest
{
    private static final int TOPIC_ID = 2384734;
    private static final byte[] MESSAGE_PAYLOAD = new byte[] {9, 8, 7, 6, 5, 4};
    private static final int PORT_ID = 8765;
    private static final String MULTICAST_ADDR = "239.0.0.1";
    private MessagingServiceImpl messagingService;
    private TestMessageListener messageListener;

    @Test
    public void shouldSendMulticastMessageToConfiguredAddress() throws Exception
    {
        final byte[] expectedMessage = createMessage(TOPIC_ID, MESSAGE_PAYLOAD);
        final ByteOutputBuffer outputBuffer = new ByteArrayOutputBufferImpl();
        outputBuffer.setPosition(4);
        outputBuffer.writeBytes(MESSAGE_PAYLOAD, 0, MESSAGE_PAYLOAD.length);
        messageListener.startListening();
        messagingService.send(TOPIC_ID, outputBuffer);

        messageListener.waitForMessageReceived(expectedMessage);
    }

    @Test
    public void shouldReceiveMulticastMessageFromConfiguredAddress() throws Exception
    {
        final byte[] expectedMessage = MESSAGE_PAYLOAD;
        final TestMessageReceiver testMessageReceiver = new TestMessageReceiver();
        final ByteOutputBuffer outputBuffer = new ByteArrayOutputBufferImpl();
        outputBuffer.setPosition(4);
        outputBuffer.writeBytes(MESSAGE_PAYLOAD, 0, MESSAGE_PAYLOAD.length);

        messagingService.registerReceiver(TOPIC_ID, testMessageReceiver);
        messagingService.start();

        messagingService.send(TOPIC_ID, outputBuffer);

        testMessageReceiver.waitForMessageReceived(TOPIC_ID, expectedMessage);

        messagingService.shutdown();
    }

    @Before
    public void setUp() throws Exception
    {
        messagingService = new MessagingServiceImpl(MULTICAST_ADDR, PORT_ID);
        messageListener = new TestMessageListener(MULTICAST_ADDR, PORT_ID);
    }

    @After
    public void tearDown() throws Exception
    {
        messageListener.stopListening();
    }

    private byte[] createMessage(final int topicId, final byte[] messagePayload)
    {
        final byte[] data = new byte[4 + messagePayload.length];
        Bits.writeInt(topicId, data, 0);
        System.arraycopy(messagePayload, 0, data, 4, messagePayload.length);
        return data;
    }
}
