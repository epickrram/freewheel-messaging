//////////////////////////////////////////////////////////////////////////////////
//   Copyright 2011   Mark Price     mark at epickrram.com                      //
//                                                                              //
//   Licensed under the Apache License, Version 2.0 (the "License");            //
//   you may not use this file except in compliance with the License.           //
//   You may obtain a copy of the License at                                    //
//                                                                              //
//       http://www.apache.org/licenses/LICENSE-2.0                             //
//                                                                              //
//   Unless required by applicable law or agreed to in writing, software        //
//   distributed under the License is distributed on an "AS IS" BASIS,          //
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   //
//   See the License for the specific language governing permissions and        //
//   limitations under the License.                                             //
//////////////////////////////////////////////////////////////////////////////////
package com.epickrram.freewheel.messaging.multicast;

import com.epickrram.freewheel.io.DecoderStream;
import com.epickrram.freewheel.io.UnpackerDecoderStream;
import com.epickrram.freewheel.messaging.MessagingException;
import com.epickrram.freewheel.messaging.MessagingService;
import com.epickrram.freewheel.messaging.Receiver;
import com.epickrram.freewheel.messaging.ReceiverRegistry;
import com.epickrram.freewheel.messaging.ptp.EndPoint;
import com.epickrram.freewheel.protocol.CodeBook;
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
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MulticastMessagingService implements MessagingService
{
    private static final Logger LOGGER = Logger.getLogger(MulticastMessagingService.class.getSimpleName());
    private static final int BUFFER_SIZE = 1024 * 32;

    private final MulticastSocket multicastSocket;
    private final SocketAddress multicastAddress;
    private final String ipAddress;
    private final ReceiverRegistry receiverRegistry = new ReceiverRegistry();
    private final Thread listenerThread;
    private final CountDownLatch listenerThreadStartedLatch = new CountDownLatch(1);
    private final CodeBook codeBook;

    private volatile boolean isShuttingDown = false;

    public MulticastMessagingService(final EndPoint endPoint, final CodeBook codeBook)
    {
        this.ipAddress = endPoint.getAddress().getHostAddress();
        this.codeBook = codeBook;
        try
        {
            multicastAddress = new InetSocketAddress(InetAddress.getByName(ipAddress), endPoint.getPort());
            multicastSocket = createMulticastSocket(endPoint.getPort());
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

    @Override
    public <T> void registerPublisher(final Class<T> descriptor)
    {
    }

    @Override
    public <T> void registerSubscriber(final Class<T> descriptor)
    {
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

            LOGGER.info("Sending message of size " + dataLength + " to address " + multicastAddress);

            multicastSocket.send(sendPacket);

            LOGGER.info("Sent message");
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw new MessagingException("Failed to send message", e);
        }
    }

    @Override
    public DecoderStream sendAndWait(final int topicId, final ByteArrayOutputStream byteArrayOutputStream) throws MessagingException
    {
        throw new IllegalStateException(getClass().getSimpleName() + " does not support sendAndWait");
    }

    @Override
    public boolean supportsSendAndWait()
    {
        return false;
    }

    public void registerReceiver(final int topicId, final Receiver receiver)
    {
        receiverRegistry.registerReceiver(topicId, receiver);
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
            listenerThreadStartedLatch.countDown();
            while (!Thread.currentThread().isInterrupted())
            {
                final byte[] receiveBuffer = new byte[BUFFER_SIZE];
                final DatagramPacket recvPacket = new DatagramPacket(receiveBuffer, 0, BUFFER_SIZE);
                try
                {
                    LOGGER.info("Waiting for packet...");
                    socket.receive(recvPacket);
                    LOGGER.info("Received a packet of length " + recvPacket.getLength());
                    final ByteArrayInputStream inputBuffer = new ByteArrayInputStream(recvPacket.getData(),
                            recvPacket.getOffset(),
                            recvPacket.getLength());


                    final UnpackerDecoderStream decoderStream = new UnpackerDecoderStream(getCodeBook(), new MessagePackUnpacker(inputBuffer));
                    final int topicId = decoderStream.readInt();

                    final Collection<Receiver> receiverList = receiverRegistry.getReceiverList(topicId);
                    for (Receiver receiver : receiverList)
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