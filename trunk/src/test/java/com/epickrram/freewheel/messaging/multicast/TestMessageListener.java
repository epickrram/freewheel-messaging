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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class TestMessageListener
{
    private static final int TIMEOUT_MILLIS = 5000;

    private final List<byte[]> messageList = new CopyOnWriteArrayList<byte[]>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final String ipAddress;
    private final int port;

    private volatile boolean running;

    public TestMessageListener(final String ipAddress, final int port)
    {

        this.ipAddress = ipAddress;
        this.port = port;
    }

    void startListening()
    {
        running = true;
        final CountDownLatch latch = new CountDownLatch(1);
        try
        {
            executorService.submit(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        final DatagramPacket datagramPacket = new DatagramPacket(new byte[8192], 0, 8192);
                        final MulticastSocket socket = new MulticastSocket(port);
                        final InetAddress groupAddress = InetAddress.getByName(ipAddress);
                        socket.joinGroup(groupAddress);
                        while(running && !Thread.currentThread().isInterrupted())
                        {
                            latch.countDown();
                            socket.receive(datagramPacket);
                            byte[] copy = new byte[datagramPacket.getLength()];
                            System.arraycopy(datagramPacket.getData(), datagramPacket.getOffset(), copy, 0, datagramPacket.getLength());
                            
                            messageList.add(copy);
                        }
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                        throw new RuntimeException("Failed to receive packets", e);
                    }
                }
            });
            latch.await();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

    }

    void stopListening()
    {
        running = false;
        executorService.shutdownNow();
    }

    void clearMessages()
    {
        messageList.clear();
    }

    void waitForMessageReceived(final byte[] expectedMessage)
    {
        long endMs = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while((System.currentTimeMillis() < endMs))
        {
            for (byte[] actualMessage : messageList)
            {
                if(Arrays.equals(actualMessage, expectedMessage))
                {
                    return;
                }
            }
            try
            {
                Thread.sleep(50L);
            }
            catch (InterruptedException e)
            {
                // ignore
            }
        }

        org.junit.Assert.fail("Did not find message " + Arrays.toString(expectedMessage) + " in " + getMessagesAsString());
    }

    private String getMessagesAsString()
    {
        final StringBuilder buffer = new StringBuilder();
        for (byte[] message : messageList)
        {
            buffer.append(Arrays.toString(message)).append(", ");
        }
        return buffer.toString();
    }
}