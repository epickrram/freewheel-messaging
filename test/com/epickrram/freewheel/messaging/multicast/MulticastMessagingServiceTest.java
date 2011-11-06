package com.epickrram.freewheel.messaging.multicast;

import com.epickrram.freewheel.io.PackerEncoderStream;
import com.epickrram.freewheel.messaging.TestMessageReceiver;
import com.epickrram.freewheel.protocol.CodeBookImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.msgpack.packer.MessagePackPacker;

import java.io.ByteArrayOutputStream;

public final class MulticastMessagingServiceTest
{
    private static final int TOPIC_ID = 2384734;
    private static final byte[] MESSAGE_PAYLOAD = new byte[] {9, 8, 7, 6, 5, 4};
    private static final int PORT_ID = 8765;
    private static final String MULTICAST_ADDR = "239.0.0.1";
    private MulticastMessagingService multicastMessagingService;
    private TestMessageListener messageListener;

    @Test
    public void shouldSendMulticastMessageToConfiguredAddress() throws Exception
    {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final PackerEncoderStream encoderStream = new PackerEncoderStream(new CodeBookImpl(), new MessagePackPacker(outputStream));
        encoderStream.writeInt(TOPIC_ID);
        encoderStream.writeByteArray(MESSAGE_PAYLOAD, 0, MESSAGE_PAYLOAD.length);
        messageListener.startListening();
        multicastMessagingService.send(TOPIC_ID, outputStream);

        messageListener.waitForMessageReceived(outputStream.toByteArray());
    }

    @Test
    public void shouldReceiveMulticastMessageFromConfiguredAddress() throws Exception
    {
        final TestMessageReceiver testMessageReceiver = new TestMessageReceiver();
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final PackerEncoderStream encoderStream = new PackerEncoderStream(new CodeBookImpl(), new MessagePackPacker(outputStream));
        encoderStream.writeInt(TOPIC_ID);
        encoderStream.writeByteArray(MESSAGE_PAYLOAD, 0, MESSAGE_PAYLOAD.length);

        multicastMessagingService.registerReceiver(TOPIC_ID, testMessageReceiver);
        multicastMessagingService.start();

        multicastMessagingService.send(TOPIC_ID, outputStream);

        testMessageReceiver.waitForMessageReceived(TOPIC_ID, outputStream.toByteArray());

        multicastMessagingService.shutdown();
    }

    @Before
    public void setUp() throws Exception
    {
        multicastMessagingService = new MulticastMessagingService(MULTICAST_ADDR, PORT_ID, new CodeBookImpl());
        messageListener = new TestMessageListener(MULTICAST_ADDR, PORT_ID);
    }

    @After
    public void tearDown() throws Exception
    {
        messageListener.stopListening();
    }

}
