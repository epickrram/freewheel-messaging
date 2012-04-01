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

package com.epickrram.freewheel.messaging.ptp;

import com.epickrram.freewheel.io.DecoderStream;
import com.epickrram.freewheel.io.PackerEncoderStream;
import com.epickrram.freewheel.io.UnpackerDecoderStream;
import com.epickrram.freewheel.messaging.Bits;
import com.epickrram.freewheel.messaging.MessagingException;
import com.epickrram.freewheel.messaging.MessagingService;
import com.epickrram.freewheel.messaging.Receiver;
import com.epickrram.freewheel.protocol.CodeBook;
import com.epickrram.freewheel.remoting.TopicIdGenerator;
import com.epickrram.freewheel.util.IoUtil;
import org.msgpack.packer.MessagePackPacker;
import org.msgpack.unpacker.MessagePackUnpacker;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BlockingPointToPointMessagingService implements MessagingService
{
    private static final Logger LOGGER = Logger.getLogger(BlockingPointToPointMessagingService.class.getSimpleName());

    private final EndPointProvider endPointProvider;
    private final CodeBook codeBook;
    private final TopicIdGenerator topicIdGenerator;
    private final SocketFactory socketFactory = SocketFactory.getDefault();
    private final ServerSocketFactory serverSocketFactory = ServerSocketFactory.getDefault();
    private final Map<Integer, Class<?>> topicIdToInterfaceMap = new ConcurrentHashMap<Integer, Class<?>>();
    private final Collection<BlockingConnectionReceiverRunnable> receiverCollection = new CopyOnWriteArrayList<BlockingConnectionReceiverRunnable>();
    // TODO configuration
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicInteger startedReceiverThreadCounter = new AtomicInteger(0);

    public BlockingPointToPointMessagingService(final EndPointProvider endPointProvider,
                                                final CodeBook codeBook,
                                                final TopicIdGenerator topicIdGenerator)
    {
        this.endPointProvider = endPointProvider;
        this.codeBook = codeBook;
        this.topicIdGenerator = topicIdGenerator;
    }

    @Override
    public void send(final int topicId, final ByteArrayOutputStream byteArrayOutputStream) throws MessagingException
    {
        final Class<?> interfaceClass = topicIdToInterfaceMap.get(topicId);
        final EndPoint endPoint = endPointProvider.resolveEndPoint(interfaceClass);
        try
        {
            final Socket socket = socketFactory.createSocket(endPoint.getAddress(), endPoint.getPort());
            final OutputStream outputStream = socket.getOutputStream();
            final byte[] messageSize = new byte[4];
            Bits.writeInt(byteArrayOutputStream.size(), messageSize, 0);
            outputStream.write(messageSize, 0, 4);
            outputStream.write((byte) 0);
            outputStream.write(byteArrayOutputStream.toByteArray());
            outputStream.flush();
            IoUtil.close(socket);
        }
        catch (IOException e)
        {
            throw new MessagingException("Unable to write to remote socket", e);
        }
    }

    @Override
    public DecoderStream sendAndWait(final int topicId, final ByteArrayOutputStream byteArrayOutputStream) throws MessagingException
    {
        final Class<?> interfaceClass = topicIdToInterfaceMap.get(topicId);
        final EndPoint endPoint = endPointProvider.resolveEndPoint(interfaceClass);
        try
        {
            final Socket socket = socketFactory.createSocket(endPoint.getAddress(), endPoint.getPort());
            final OutputStream outputStream = socket.getOutputStream();
            final byte[] messageSize = new byte[4];
            Bits.writeInt(byteArrayOutputStream.size(), messageSize, 0);
            outputStream.write(messageSize, 0, 4);
            outputStream.write((byte) 1);
            outputStream.write(byteArrayOutputStream.toByteArray());
            outputStream.flush();
            final InputStream inputStream = socket.getInputStream();
            inputStream.read(messageSize, 0, 4);
            final int responseSize = Bits.readInt(messageSize, 0);
            final byte[] buffer = new byte[responseSize];
            inputStream.read(buffer, 0, responseSize);
            IoUtil.close(socket);
            return new UnpackerDecoderStream(codeBook, new MessagePackUnpacker(new ByteArrayInputStream(buffer)));
        }
        catch (IOException e)
        {
            throw new MessagingException("Unable to write to remote socket", e);
        }
    }

    @Override
    public void registerReceiver(final int topicId, final Receiver receiver)
    {
        receiverCollection.add(new BlockingConnectionReceiverRunnable(running, serverSocketFactory,
                endPointProvider.resolveEndPoint(topicIdToInterfaceMap.get(topicId)), receiver, codeBook, startedReceiverThreadCounter));
    }

    @Override
    public <T> void registerPublisher(final Class<T> descriptor)
    {
        topicIdToInterfaceMap.put(topicIdGenerator.getTopicId(descriptor), descriptor);
    }

    @Override
    public <T> void registerSubscriber(final Class<T> descriptor)
    {
        topicIdToInterfaceMap.put(topicIdGenerator.getTopicId(descriptor), descriptor);
    }

    @Override
    public void start() throws MessagingException
    {
        running.set(true);

        final int targetStartThreadCount = receiverCollection.size();

        for (BlockingConnectionReceiverRunnable receiverRunnable : receiverCollection)
        {
            executor.submit(receiverRunnable);
        }

        while(targetStartThreadCount != startedReceiverThreadCounter.get())
        {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1L));
        }
    }

    @Override
    public void shutdown() throws MessagingException
    {
        running.set(false);
        for (BlockingConnectionReceiverRunnable receiverRunnable : receiverCollection)
        {
            receiverRunnable.stop();
        }
        executor.shutdown();
        final long timeoutSeconds = 5L;
        try
        {
            executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            LOGGER.warning(String.format("Receiver threads did not shutdown within %d seconds", timeoutSeconds));
        }
    }

    @Override
    public boolean supportsSendAndWait()
    {
        return true;
    }

    private static final class BlockingConnectionReceiverRunnable implements Runnable
    {
        private static final Logger LOGGER = Logger.getLogger(BlockingConnectionReceiverRunnable.class.getSimpleName());

        private final AtomicBoolean runningFlag;
        private final ServerSocketFactory serverSocketFactory;
        private final EndPoint endPoint;
        private final Receiver receiver;
        private final CodeBook codeBook;
        private final AtomicInteger startedReceiverThreadCounter;
        private volatile ServerSocket serverSocket;

        public BlockingConnectionReceiverRunnable(final AtomicBoolean runningFlag, final ServerSocketFactory serverSocketFactory,
                                                  final EndPoint endPoint, final Receiver receiver, final CodeBook codeBook,
                                                  final AtomicInteger startedReceiverThreadCounter)
        {
            this.runningFlag = runningFlag;
            this.serverSocketFactory = serverSocketFactory;
            this.endPoint = endPoint;
            this.receiver = receiver;
            this.codeBook = codeBook;
            this.startedReceiverThreadCounter = startedReceiverThreadCounter;
        }

        @Override
        public void run()
        {
            startedReceiverThreadCounter.incrementAndGet();
            while(isRunning())
            {
                try
                {
                    serverSocket = serverSocketFactory.createServerSocket(endPoint.getPort());
                    while(isRunning())
                    {
                        final Socket socket = serverSocket.accept();
                        final byte[] size = new byte[4];
                        final InputStream inputStream = socket.getInputStream();
                        inputStream.read(size, 0, 4);
                        final int messageSize = Bits.readInt(size, 0);
                        final boolean isSyncMethod = ((byte)inputStream.read()) != 0;

                        final byte[] message = new byte[messageSize];
                        inputStream.read(message, 0, messageSize);

                        final UnpackerDecoderStream decoderStream = new UnpackerDecoderStream(codeBook, new MessagePackUnpacker(new ByteArrayInputStream(message)));
                        final int topicId = decoderStream.readInt();
                        if(isSyncMethod)
                        {
                            final Object result = receiver.onSyncMessage(topicId, decoderStream);
                            final ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
                            final PackerEncoderStream encoderStream = new PackerEncoderStream(codeBook, new MessagePackPacker(outBuffer));

                            if(result == null)
                            {
                                encoderStream.writeObject(result);
                            }
                            else
                            {
                                final Class<?> returnType = result.getClass();
                                if (int.class == returnType)
                                {
                                    encoderStream.writeInt((Integer) result);
                                }
                                else if (long.class == returnType)
                                {
                                    encoderStream.writeLong((Long) result);
                                }
                                else if (byte.class == returnType)
                                {
                                    encoderStream.writeByte((Byte) result);
                                }
                                else if (String.class == returnType)
                                {
                                    encoderStream.writeString((String) result);
                                }
                                else
                                {
                                    encoderStream.writeObject(result);
                                }
                            }

                            final OutputStream outputStream = socket.getOutputStream();
                            Bits.writeInt(outBuffer.size(), size, 0);
                            outputStream.write(size, 0, 4);
                            final byte[] responseBytes = outBuffer.toByteArray();
                            outputStream.write(responseBytes);
                            outputStream.flush();
                        }
                        else
                        {
                            receiver.onMessage(topicId, decoderStream);
                        }
                    }

                }
                catch(IOException e)
                {
                    if(runningFlag.get())
                    {
                        LOGGER.log(Level.WARNING, "Unable to create server socket, pausing..", e);
                    }
                    IoUtil.close(serverSocket);
                    LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1L));
                }
                catch(Throwable e)
                {
                    LOGGER.log(Level.SEVERE, "Failed to invoke Receiver", e);
                    IoUtil.close(serverSocket);
                    LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1L));
                }
            }
        }

        private boolean isRunning()
        {
            return runningFlag.get() && !Thread.currentThread().isInterrupted();
        }

        void stop()
        {
            IoUtil.close(serverSocket);
        }
    }
}
