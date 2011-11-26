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

import org.junit.Assert;
import org.junit.Test;

public final class BitsTest
{
    private static final int INT_VALUE = Integer.MAX_VALUE - 1;
    private static final long LONG_VALUE = Long.MAX_VALUE - 1;

    @Test
    public void shouldTranscodeIntToBytes() throws Exception
    {
        final byte[] buffer = new byte[4];
        Bits.writeInt(INT_VALUE, buffer, 0);

        Assert.assertEquals(INT_VALUE, Bits.readInt(buffer, 0));
    }

    @Test
    public void shouldTranscodeIntToBytesAtOffset() throws Exception
    {
        final byte[] buffer = new byte[6];
        Bits.writeInt(INT_VALUE, buffer, 2);

        Assert.assertEquals(INT_VALUE, Bits.readInt(buffer, 2));
    }

    @Test
    public void shouldTranscodeLongToBytes() throws Exception
    {
        final byte[] buffer = new byte[8];
        Bits.writeLong(LONG_VALUE, buffer, 0);

        Assert.assertEquals(LONG_VALUE, Bits.readLong(buffer, 0));
    }

    @Test
    public void shouldTranscodeLongToBytesAtOffset() throws Exception
    {
        final byte[] buffer = new byte[10];
        Bits.writeLong(LONG_VALUE, buffer, 2);

        Assert.assertEquals(LONG_VALUE, Bits.readLong(buffer, 2));
    }

    @Test
    public void shouldTranscodeBooleanToBytes() throws Exception
    {
        final byte[] buffer = new byte[1];
        Bits.writeBoolean(true, buffer, 0);

        Assert.assertTrue(Bits.readBoolean(buffer, 0));

        Bits.writeBoolean(false, buffer, 0);

        Assert.assertFalse(Bits.readBoolean(buffer, 0));
    }
    
    @Test
    public void shouldTranscodeBooleanToBytesAtOffset() throws Exception
    {
        final byte[] buffer = new byte[2];
        Bits.writeBoolean(true, buffer, 1);

        Assert.assertTrue(Bits.readBoolean(buffer, 1));

        Bits.writeBoolean(false, buffer, 1);

        Assert.assertFalse(Bits.readBoolean(buffer, 1));
    }
}