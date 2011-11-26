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
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.epickrram.freewheel.stream.ByteComparisonUtil.assertByteArraysEqual;

@Ignore
public final class ByteArrayOutputBufferImplTest
{
    private static final int INT_VALUE = 17;
    private static final long LONG_VALUE = 23472394872342L;
    private static final int ORIGINAL_BUFFER_SIZE = 8;
    private ByteArrayOutputBufferImpl byteArrayOutputBufferImpl;

    @Test
    public void shouldWriteIntAndUpdatePointers() throws Exception
    {
        Assert.assertEquals(0, byteArrayOutputBufferImpl.count());
        Assert.assertEquals(0, byteArrayOutputBufferImpl.position());

        byteArrayOutputBufferImpl.writeInt(INT_VALUE);

        Assert.assertEquals(4, byteArrayOutputBufferImpl.count());
        Assert.assertEquals(4, byteArrayOutputBufferImpl.position());

        byteArrayOutputBufferImpl.writeInt(INT_VALUE);

        Assert.assertEquals(8, byteArrayOutputBufferImpl.count());
        Assert.assertEquals(8, byteArrayOutputBufferImpl.position());

        byte[] expected = new byte[8];
        Bits.writeInt(INT_VALUE, expected, 0);
        Bits.writeInt(INT_VALUE, expected, 4);
        byte[] actual = new byte[8];
        byteArrayOutputBufferImpl.flip();
        byteArrayOutputBufferImpl.copyInto(actual, 0, 8);

        assertByteArraysEqual(expected, actual);
    }

    @Test
    public void shouldWriteLongAndUpdatePointers() throws Exception
    {
        byteArrayOutputBufferImpl.writeLong(LONG_VALUE);

        Assert.assertEquals(8, byteArrayOutputBufferImpl.count());
        Assert.assertEquals(8, byteArrayOutputBufferImpl.position());

        byte[] expected = new byte[8];
        Bits.writeLong(LONG_VALUE, expected, 0);

        assertByteArraysEqual(expected, byteArrayOutputBufferImpl.getBackingArray());
    }

    @Test
    public void shouldWriteBooleanAndUpdatePointers() throws Exception
    {
        byteArrayOutputBufferImpl.writeBoolean(true);

        Assert.assertEquals(1, byteArrayOutputBufferImpl.count());
        Assert.assertEquals(1, byteArrayOutputBufferImpl.position());

        byte[] expected = new byte[] {1};
        byte[] actual = new byte[1];
        byteArrayOutputBufferImpl.flip();
        byteArrayOutputBufferImpl.copyInto(actual, 0, 1);

        assertByteArraysEqual(expected, actual);
    }

    @Test
    public void shouldWriteByteArrayAndUpdatePointers() throws Exception
    {
        byte[] first = new byte[] {56, 67, 2, 11, 89};
        byte[] second = new byte[] {4, 123, 99, 13};

        byteArrayOutputBufferImpl.writeBytes(first, 1, 3);
        Assert.assertEquals(3, byteArrayOutputBufferImpl.count());
        Assert.assertEquals(3, byteArrayOutputBufferImpl.position());

        byteArrayOutputBufferImpl.writeBytes(second, 0, second.length);
        Assert.assertEquals(7, byteArrayOutputBufferImpl.count());
        Assert.assertEquals(7, byteArrayOutputBufferImpl.position());

        byte[] expected = new byte[] {67, 2, 11, 4, 123, 99, 13};
        byte[] actual = new byte[7];
        byteArrayOutputBufferImpl.flip();
        byteArrayOutputBufferImpl.copyInto(actual, 0, 7);

        assertByteArraysEqual(expected, actual);
    }

    @Test
    public void shouldReset() throws Exception
    {
        byteArrayOutputBufferImpl.writeInt(7);
        byteArrayOutputBufferImpl.writeInt(11);

        byteArrayOutputBufferImpl.reset();

        Assert.assertEquals(0, byteArrayOutputBufferImpl.count());
        Assert.assertEquals(0, byteArrayOutputBufferImpl.position());
    }

    @Test
    public void shouldGrowBufferForWriteInt() throws Exception
    {
        byteArrayOutputBufferImpl.writeInt(1);
        byteArrayOutputBufferImpl.writeInt(2);
        byteArrayOutputBufferImpl.writeInt(5);
        byteArrayOutputBufferImpl.writeInt(7);

        Assert.assertEquals(16, byteArrayOutputBufferImpl.count());
        Assert.assertEquals(16, byteArrayOutputBufferImpl.position());
        byte[] expected = new byte[16];
        Bits.writeInt(1, expected, 0);
        Bits.writeInt(2, expected, 4);
        Bits.writeInt(5, expected, 8);
        Bits.writeInt(7, expected, 12);

        assertByteArraysEqual(expected, byteArrayOutputBufferImpl.getBackingArray());
    }

    @Test
    public void shouldGrowBufferForWriteBytes() throws Exception
    {
        byteArrayOutputBufferImpl.writeBytes(new byte[6], 0, 6);
        byteArrayOutputBufferImpl.writeBytes(new byte[3], 0, 3);

        Assert.assertEquals(9, byteArrayOutputBufferImpl.count());
        Assert.assertEquals(9, byteArrayOutputBufferImpl.position());
    }

    @Test
    public void shouldSetPosition() throws Exception
    {
        byteArrayOutputBufferImpl.setPosition(4);
        byteArrayOutputBufferImpl.writeInt(17);

        byte[] expected = new byte[8];
        Bits.writeInt(17, expected, 4);

        assertByteArraysEqual(expected, byteArrayOutputBufferImpl.getBackingArray());
    }

    @Before
    public void setUp() throws Exception
    {
        byteArrayOutputBufferImpl = new ByteArrayOutputBufferImpl(ORIGINAL_BUFFER_SIZE);
    }

}