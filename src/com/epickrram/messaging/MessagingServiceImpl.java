package com.epickrram.messaging;

import com.epickrram.stream.ByteArrayInputBufferImpl;
import com.epickrram.stream.ByteOutputBuffer;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MessagingServiceImpl implements MessagingService
{
    private static final Logger LOGGER = Logger.getLogger(MessagingServiceImpl.class.getSimpleName());

    private final MulticastSocket multicastSocket;
    private final String ipAddress;
    private final int port;
    // TODO not thread-safe!!
    private final Int2ObjectOpenHashMap<Receiver> topicIdToReceiverMap = new Int2ObjectOpenHashMap<Receiver>();
    private final Thread listenerThread;
    private final CountDownLatch listenerThreadStartedLatch = new CountDownLatch(1);

    private volatile boolean isShuttingDown = false;

    public MessagingServiceImpl(final String ipAddress, final int port)
    {
        this.ipAddress = ipAddress;
        this.port = port;
        try
        {
            multicastSocket = new MulticastSocket(port);
            listenerThread = new Thread(new MessageHandler(multicastSocket), "MessageHandler");
        }
        catch (SocketException e)
        {
            throw new MessagingException("Could not bind socket: ", e);
        }
        catch (IOException e)
        {
            throw new MessagingException("Could not bind socket: ", e);
        }
    }

    public void send(final int topicId, final ByteOutputBuffer message) throws MessagingException
    {
        message.setPosition(0);
        message.writeInt(topicId);
        try
        {
            final DatagramPacket sendPacket = new DatagramPacket(message.getBackingArray(), 0, message.count());
            sendPacket.setPort(port);
            sendPacket.setAddress(InetAddress.getByName(ipAddress));
            multicastSocket.send(sendPacket);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw new MessagingException("Failed to send message", e);
        }
    }

    public void registerReceiver(final int topicId, final Receiver receiver)
    {
        // TODO not thread-safe!!!
        topicIdToReceiverMap.put(topicId, receiver);
    }

    public void start() throws MessagingException
    {
        try
        {
            multicastSocket.joinGroup(InetAddress.getByName(ipAddress));
        }
        catch (IOException e)
        {
            throw new MessagingException("Could not bind to multicast group", e);
        }
        listenerThread.start();
        try
        {
            listenerThreadStartedLatch.await();
        }
        catch (InterruptedException e)
        {
            throw new MessagingException("Listener thread did not start.");
        }
    }

    public void shutdown() throws MessagingException
    {
        try
        {
            isShuttingDown = true;
            listenerThread.interrupt();
            multicastSocket.close();
            listenerThread.join();
        }
        catch (InterruptedException e)
        {
            throw new MessagingException("Failed to shutdown listener", e);
        }
    }

    private final class MessageHandler implements Runnable
    {
        private final MulticastSocket socket;

        public MessageHandler(final MulticastSocket socket)
        {
            this.socket = socket;
        }

        public void run()
        {
            LOGGER.info("MessageHandler Thread listening.");
            listenerThreadStartedLatch.countDown();
            while(!Thread.currentThread().isInterrupted())
            {
                final byte[] receiveBuffer = new byte[4096];
                final DatagramPacket recvPacket = new DatagramPacket(receiveBuffer, 0, 4096);
                try
                {
                    socket.receive(recvPacket);
                    final ByteArrayInputBufferImpl inputBuffer = new ByteArrayInputBufferImpl(recvPacket.getData(),
                                                                                              recvPacket.getOffset(),
                                                                                              recvPacket.getLength());
                    final int topicId = inputBuffer.readInt();
                    final Receiver receiver = topicIdToReceiverMap.get(topicId);
                    if(receiver != null)
                    {
                        receiver.onMessage(topicId, inputBuffer);
                    }
                }
                catch (IOException e)
                {
                    if(!isShuttingDown)
                    {
                        LOGGER.log(Level.WARNING, "Failed to receive datagram packet", e);
                    }
                }
            }
            LOGGER.info("MessageHandler thread interrupted. Shutting down.");
        }
    }
}