package com.epickrram.freewheel.messaging;

import com.epickrram.freewheel.protocol.CodeBook;
import com.epickrram.freewheel.io.UnpackerDecoderStream;
import org.msgpack.unpacker.MessagePackUnpacker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MessagingServiceImpl implements MessagingService
{
    private static final Logger LOGGER = Logger.getLogger(MessagingServiceImpl.class.getSimpleName());
    private static final int BUFFER_SIZE = 1024 * 32;

    private final MulticastSocket multicastSocket;
    private final SocketAddress multicastAddress;
    private final String ipAddress;
    private final Map<Integer, Receiver> topicIdToReceiverMap = new ConcurrentHashMap<Integer, Receiver>();
    private final Thread listenerThread;
    private final CountDownLatch listenerThreadStartedLatch = new CountDownLatch(1);
    private final CodeBook codeBook;

    private volatile boolean isShuttingDown = false;

    public MessagingServiceImpl(final String ipAddress, final int port, final CodeBook codeBook)
    {
        this.ipAddress = ipAddress;
        this.codeBook = codeBook;
        try
        {
            multicastAddress = new InetSocketAddress(InetAddress.getByName(ipAddress), port);
            multicastSocket = createMulticastSocket(port);
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

    public void send(final int topicId, final ByteArrayOutputStream byteArrayOutputStream) throws MessagingException
    {
        try
        {
            final int dataLength = byteArrayOutputStream.size();
            final DatagramPacket sendPacket = new DatagramPacket(byteArrayOutputStream.toByteArray(), 0, dataLength);

            if (dataLength > BUFFER_SIZE)
            {
                LOGGER.warning("Attempting to send message of " + dataLength + " bytes");
            }
            sendPacket.setSocketAddress(multicastAddress);
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

    private MulticastSocket createMulticastSocket(final int port) throws IOException
    {
        final MulticastSocket socket = new MulticastSocket(port);
        int suppliedBufferSize = 1024;
        do
        {
            suppliedBufferSize += 1024;
            socket.setSendBufferSize(suppliedBufferSize);

        } while (socket.getSendBufferSize() == suppliedBufferSize);

        suppliedBufferSize = 1024;
        do
        {
            suppliedBufferSize += 1024;
            socket.setReceiveBufferSize(suppliedBufferSize);

        } while (socket.getReceiveBufferSize() == suppliedBufferSize);


        LOGGER.info(String.format("Allocated socket buffer sizes [send = %d, receive = %d]",
                socket.getSendBufferSize(), socket.getReceiveBufferSize()));

        return socket;
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
            while (!Thread.currentThread().isInterrupted())
            {
                final byte[] receiveBuffer = new byte[BUFFER_SIZE];
                final DatagramPacket recvPacket = new DatagramPacket(receiveBuffer, 0, BUFFER_SIZE);
                try
                {
                    socket.receive(recvPacket);
                    final ByteArrayInputStream inputBuffer = new ByteArrayInputStream(recvPacket.getData(),
                            recvPacket.getOffset(),
                            recvPacket.getLength());


                    final UnpackerDecoderStream decoderStream = new UnpackerDecoderStream(getCodeBook(), new MessagePackUnpacker(inputBuffer));
                    final int topicId = decoderStream.readInt();

                    final Receiver receiver = topicIdToReceiverMap.get(topicId);
                    if (receiver != null)
                    {
                        receiver.onMessage(topicId, decoderStream);
                    }
                }
                catch (IOException e)
                {
                    if (!isShuttingDown)
                    {
                        LOGGER.log(Level.WARNING, "Failed to receive datagram packet", e);
                    }
                }
            }
            LOGGER.info("MessageHandler thread interrupted. Shutting down.");
        }
    }

    private CodeBook getCodeBook()
    {
        return codeBook;
    }
}