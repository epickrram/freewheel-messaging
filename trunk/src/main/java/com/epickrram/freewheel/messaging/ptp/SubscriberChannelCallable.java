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

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;

import java.net.SocketAddress;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

final class SubscriberChannelCallable implements Callable<Channel>
{
    private static final Logger LOGGER = Logger.getLogger(SubscriberChannelCallable.class.getName());
    private final SocketAddress subscriberAddress;
    private final ServerBootstrap bootstrap;

    public SubscriberChannelCallable(final ServerBootstrap bootstrap, final SocketAddress subscriberAddress)
    {
        this.subscriberAddress = subscriberAddress;
        this.bootstrap = bootstrap;
    }

    @Override
    public Channel call() throws Exception
    {
        LOGGER.fine("Creating Subscriber for address: " + subscriberAddress);
        return bootstrap.bind(subscriberAddress);
    }
}
