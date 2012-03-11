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

package com.epickrram.freewheel.util;

import com.lmax.disruptor.RingBuffer;

public final class DisruptorRingBufferWrapper<T> implements RingBufferWrapper<T>
{
    private final RingBuffer<T> ringBuffer;

    public DisruptorRingBufferWrapper(final RingBuffer<T> ringBuffer)
    {
        this.ringBuffer = ringBuffer;
    }

    @Override
    public long next()
    {
        return ringBuffer.next();
    }

    @Override
    public T get(final long sequence)
    {
        return ringBuffer.get(sequence);
    }

    @Override
    public void publish(final long sequence)
    {
        ringBuffer.publish(sequence);
    }
}
