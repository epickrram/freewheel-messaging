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
package com.epickrram.freewheel.messaging;

import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ReceiverRegistry
{
    private final Map<Integer, Queue<Receiver>> receiverByTopicIdMap = new ConcurrentHashMap<Integer, Queue<Receiver>>();

    public void registerReceiver(final int topicId, final Receiver receiver)
    {
        Queue<Receiver> receiverQueue = receiverByTopicIdMap.get(topicId);
        if(receiverQueue == null)
        {
            receiverQueue = new ConcurrentLinkedQueue<Receiver>();
            final Queue<Receiver> existing = receiverByTopicIdMap.put(topicId, receiverQueue);
            if(existing != null)
            {
                receiverQueue = existing;
            }
        }
        receiverQueue.add(receiver);
    }

    public Collection<Receiver> getReceiverList(final int topicId)
    {
        return receiverByTopicIdMap.get(topicId);
    }
}