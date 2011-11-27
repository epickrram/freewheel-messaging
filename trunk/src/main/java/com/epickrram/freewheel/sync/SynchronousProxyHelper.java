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

package com.epickrram.freewheel.sync;

import com.epickrram.freewheel.messaging.MessagingContext;

public final class SynchronousProxyHelper
{
    private final SynchronousProxyGenerator proxyGenerator = new SynchronousProxyGenerator();

    @SuppressWarnings({"unchecked"})
    public <P, S, C> C createSynchronousProxy(final Class<P> publisherClass,
                                              final Class<S> subscriberClass,
                                              final Class<C> clientClass,
                                              final MessagingContext messagingContext)
    {
        final P publisher = messagingContext.createPublisher(publisherClass);
        final C client = proxyGenerator.generateProxy(publisher, subscriberClass, clientClass);
        messagingContext.createSubscriber(subscriberClass, (S) client);
        return client;
    }
}