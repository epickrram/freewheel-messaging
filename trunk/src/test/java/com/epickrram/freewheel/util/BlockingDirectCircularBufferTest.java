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
public final class BlockingDirectCircularBufferTest
{
    private static final int DEFAULT_BUFFER_SIZE = 8;

    private DirectCircularBuffer<Integer> circularBuffer;

    @Test
    public void shouldStartAtNegativeOne() throws Exception
    {
        Assert.assertEquals(-1, circularBuffer.getSequence());
    }

    @Test
    public void shouldReportCurrentSequenceBasedOnContiguousEntries() throws Exception
    {
        circularBuffer.set(0, Integer.valueOf(17));
        circularBuffer.set(1, Integer.valueOf(19));
        circularBuffer.set(3, Integer.valueOf(23));

        Assert.assertEquals(1, circularBuffer.getSequence());

        circularBuffer.set(2, Integer.valueOf(31));

        Assert.assertEquals(3, circularBuffer.getSequence());
    }

    @Test
    public void shouldStoreEntriesInCorrectSlots() throws Exception
    {
        final Integer valueOne = Integer.valueOf(17);
        final Integer valueTwo = Integer.valueOf(19);
        final Integer valueThree = Integer.valueOf(23);
        circularBuffer.set(0, valueOne);
        circularBuffer.set(1, valueTwo);
        circularBuffer.set(3, valueThree);

        Assert.assertEquals(valueOne, circularBuffer.get(0));
        Assert.assertEquals(valueTwo, circularBuffer.get(1));
        Assert.assertEquals(valueThree, circularBuffer.get(3));
    }

    @Test
    public void shouldWrapValues() throws Exception
    {
        for(int i = 0; i < DEFAULT_BUFFER_SIZE + 2; i++)
        {
            circularBuffer.set(i, Integer.valueOf(i));
        }

        Assert.assertEquals(DEFAULT_BUFFER_SIZE + 1, circularBuffer.getSequence());
        Assert.assertEquals((Integer) DEFAULT_BUFFER_SIZE, circularBuffer.get(0L));
    }

    @Ignore
    @Test
    public void shouldRecordContiguousSequenceWhileWrapping() throws Exception
    {
        Assert.fail("Not yet implemented");
    }

    @Before
    public void setUp() throws Exception
    {
        circularBuffer = new BlockingDirectCircularBuffer<Integer>(DEFAULT_BUFFER_SIZE);
    }
}