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

package com.epickrram.freewheel.remoting;

import com.epickrram.freewheel.messaging.OutgoingMessageEvent;
import com.epickrram.freewheel.util.DisruptorRingBufferWrapper;
import com.epickrram.freewheel.util.RingBufferWrapper;
import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventProcessor;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class RingBufferFactoryImpl implements RingBufferFactory
{
    private final List<EventProcessor> eventProcessors = new CopyOnWriteArrayList<EventProcessor>();

    private final EventFactory<OutgoingMessageEvent> eventFactory;
    private final EventHandler<OutgoingMessageEvent> eventHandler;

    public RingBufferFactoryImpl(final EventFactory<OutgoingMessageEvent> eventFactory,
                                 final EventHandler<OutgoingMessageEvent> eventHandler)
    {
        this.eventFactory = eventFactory;
        this.eventHandler = eventHandler;
    }

    @Override
    public RingBufferWrapper<OutgoingMessageEvent> createRingBuffer(final int size)
    {
        final RingBuffer<OutgoingMessageEvent> ringBuffer = new RingBuffer<OutgoingMessageEvent>(eventFactory, size);
        final SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();
        final EventProcessor eventProcessor =
                new BatchEventProcessor<OutgoingMessageEvent>(ringBuffer, sequenceBarrier, eventHandler);

        ringBuffer.setGatingSequences(eventProcessor.getSequence());
        eventProcessors.add(eventProcessor);

        return new DisruptorRingBufferWrapper<OutgoingMessageEvent>(ringBuffer);
    }

    @Override
    public List<EventProcessor> getEventProcessors()
    {
        return eventProcessors;
    }
}
