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

import com.epickrram.freewheel.messaging.MessagingException;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

import java.net.SocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

final class PublisherChannelCallable implements Callable<Channel>
{
    private static final Logger LOGGER = Logger.getLogger(PublisherChannelCallable.class.getName());
    private final ClientBootstrap bootstrap;
    private final SocketAddress remoteAddress;

    public PublisherChannelCallable(final ClientBootstrap bootstrap, final SocketAddress remoteAddress)
    {
        this.bootstrap = bootstrap;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public Channel call() throws Exception
    {
        final AtomicBoolean socketConnectedFlag = new AtomicBoolean(false);

        ChannelFuture publisherChannelFuture = null;
        while (!socketConnectedFlag.get())
        {
            publisherChannelFuture = bootstrap.connect(remoteAddress);

            publisherChannelFuture.addListener(new ChannelFutureListener()
            {
                @Override
                public void operationComplete(final ChannelFuture future) throws Exception
                {
                    LOGGER.fine("Publisher connection to remote host complete. Success: " + future.isSuccess() +
                            ", cancelled: " + future.isCancelled() + ", done: " + future.isDone());
                    if (future.isSuccess())
                    {
                        socketConnectedFlag.set(true);
                    }
                }
            });

            try
            {
                LOGGER.fine("Waiting for publisher channel");
                publisherChannelFuture.awaitUninterruptibly();
                Thread.sleep(1000L);
            }
            catch (InterruptedException e)
            {
                throw new MessagingException("Failed to wait for publisher channel", e);
            }

        }
        if (publisherChannelFuture == null)
        {
            throw new RuntimeException("Unable to connect to remote port");
        }

        return publisherChannelFuture.getChannel();
    }
}
