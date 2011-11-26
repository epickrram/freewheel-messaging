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
package com.epickrram.freewheel.stream;

import com.epickrram.freewheel.messaging.Bits;

public final class ByteArrayInputBufferImpl implements ByteInputBuffer
{
    private final byte[] buffer;
    private final int initialOffset;
    private int remaining;
    private int position;

    public ByteArrayInputBufferImpl(final byte[] buffer, int offset, int length)
    {
        this.buffer = buffer;
        this.initialOffset = offset;
        this.remaining = length;
        this.position = 0;
    }

    public int readInt()
    {
        final int value = Bits.readInt(buffer, position + initialOffset);
        position += 4;
        remaining -= 4;
        return value;
    }

    public long readLong()
    {
        final long value = Bits.readLong(buffer, position + initialOffset);
        position += 8;
        remaining -= 8;
        return value;
    }

    public boolean readBoolean()
    {
        final boolean value = Bits.readBoolean(buffer, position + initialOffset);
        position += 1;
        remaining -= 1;
        return value;
    }

    public byte readByte()
    {
        remaining -= 1;
        return buffer[position++];
    }

    public void readBytes(final byte[] dest, final int offset, final int length)
    {
        System.arraycopy(buffer, position + initialOffset, dest, offset, length);
        position += length;
        remaining -= length;
    }

    public int position()
    {
        return position;
    }

    public int remaining()
    {
        return remaining;
    }

    public void reset()
    {
        remaining += position;
        position = 0;
    }
}
