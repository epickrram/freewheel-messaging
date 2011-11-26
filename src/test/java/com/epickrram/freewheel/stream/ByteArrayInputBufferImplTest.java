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
import org.junit.Assert;
import org.junit.Test;

public final class ByteArrayInputBufferImplTest
{
    private static final int INT_VALUE = 17;
    private static final long LONG_VALUE = 37L;

    @Test
    public void shouldReadInt() throws Exception
    {
        final byte[] input = new byte[4];
        Bits.writeInt(INT_VALUE, input, 0);
        Assert.assertEquals(INT_VALUE, new ByteArrayInputBufferImpl(input, 0, 4).readInt());
    }

    @Test
    public void shouldReadBoolean() throws Exception
    {
        final byte[] input = new byte[1];
        Bits.writeBoolean(true, input, 0);
        Assert.assertTrue(new ByteArrayInputBufferImpl(input, 0, 1).readBoolean());
    }

    @Test
    public void shouldReadLong() throws Exception
    {
        final byte[] input = new byte[8];
        Bits.writeLong(LONG_VALUE, input, 0);
        Assert.assertEquals(LONG_VALUE, new ByteArrayInputBufferImpl(input, 0, 8).readLong());
    }

    @Test
    public void shouldReadBytes() throws Exception
    {
        final byte[] input = new byte[] {7, 9, 13, 56, 77};
        final byte[] expected = new byte[] {0, 7, 9, 13};
        final byte[] actual = new byte[4];
        new ByteArrayInputBufferImpl(input, 0, 5).readBytes(actual, 1, 3);

        ByteComparisonUtil.assertByteArraysEqual(expected, actual);
    }

    @Test
    public void shouldReadSequence() throws Exception
    {
        final ByteArrayOutputBufferImpl outputBuffer = new ByteArrayOutputBufferImpl();
        outputBuffer.writeBoolean(false);
        outputBuffer.writeInt(INT_VALUE);
        outputBuffer.writeLong(LONG_VALUE);
        outputBuffer.writeBoolean(true);

        final byte[] input = new byte[outputBuffer.count()];
        outputBuffer.flip();
        outputBuffer.copyInto(input, 0, outputBuffer.count());

        final ByteArrayInputBufferImpl inputBuffer = new ByteArrayInputBufferImpl(input, 0, input.length);
        Assert.assertFalse(inputBuffer.readBoolean());
        Assert.assertEquals(INT_VALUE, inputBuffer.readInt());
        Assert.assertEquals(LONG_VALUE, inputBuffer.readLong());
        Assert.assertTrue(inputBuffer.readBoolean());
    }
}