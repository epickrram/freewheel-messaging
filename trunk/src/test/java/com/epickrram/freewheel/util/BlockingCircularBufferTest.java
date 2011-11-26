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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
@SuppressWarnings({"UnnecessaryBoxing"})
public final class BlockingCircularBufferTest
{
    private static final int DEFAULT_BUFFER_SIZE = 8;

    private CircularBuffer<Integer> circularBuffer;

    @Test
    public void shouldStartAtNegativeOne() throws Exception
    {
        Assert.assertEquals(-1, circularBuffer.getSequence());
    }

    @Test
    public void shouldStoreValuesInSequence() throws Exception
    {
        final Integer valueOne = Integer.valueOf(17);
        final Integer valueTwo = Integer.valueOf(19);
        final Integer valueThree = Integer.valueOf(23);
        circularBuffer.add(valueOne);
        circularBuffer.add(valueTwo);
        circularBuffer.add(valueThree);

        Assert.assertEquals(valueOne, circularBuffer.get(0));
        Assert.assertEquals(valueTwo, circularBuffer.get(1));
        Assert.assertEquals(valueThree, circularBuffer.get(2));
    }

    @Test
    public void shouldReportCurrentSequence() throws Exception
    {
        circularBuffer.add(Integer.valueOf(17));
        circularBuffer.add(Integer.valueOf(19));
        circularBuffer.add(Integer.valueOf(23));

        Assert.assertEquals(2, circularBuffer.getSequence());
    }

    @Test
    public void shouldWrapValues() throws Exception
    {
        for(int i = 0; i < DEFAULT_BUFFER_SIZE + 2; i++)
        {
            circularBuffer.add(Integer.valueOf(i));
        }

        Assert.assertEquals(DEFAULT_BUFFER_SIZE + 1, circularBuffer.getSequence());
        Assert.assertEquals((Integer) DEFAULT_BUFFER_SIZE, circularBuffer.get(0L));
    }

    @Before
    public void setUp() throws Exception
    {
        circularBuffer = new BlockingCircularBuffer<Integer>(DEFAULT_BUFFER_SIZE);
    }
}