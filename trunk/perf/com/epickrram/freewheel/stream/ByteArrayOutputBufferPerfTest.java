package com.epickrram.freewheel.stream;

import com.epickrram.junit.PerfTest;
import com.epickrram.junit.PerfTestRunner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;

@Ignore
@RunWith(PerfTestRunner.class)
public final class ByteArrayOutputBufferPerfTest
{
    private static final int WRITE_OP_COUNT = (2048 / 4) - 10;
    private static final int TEST_ITERATIONS = 1000000;
    private static final int PERF_ITERATIONS = TEST_ITERATIONS * WRITE_OP_COUNT;
    private static final byte[] TEST_BYTES = new byte[] {7, 13, 117, 127, 45, 67, 35, 77};

    @Test
    @PerfTest(name = "write int ByteOutputBuffer", warmUpRuns = 5, iterations = PERF_ITERATIONS)
    public void perfTestWriteInt() throws Exception
    {
        final ByteArrayOutputBufferImpl byteArrayOutputBuffer;
        byteArrayOutputBuffer = new ByteArrayOutputBufferImpl(2048);
        for(int i = 0; i < TEST_ITERATIONS; i++)
        {
            for(int j = 0; j < WRITE_OP_COUNT; j++)
            {
                byteArrayOutputBuffer.writeInt(j);
            }
            byteArrayOutputBuffer.reset();
        }
    }

    @Test
    @PerfTest(name = "write int nio.ByteBuffer", warmUpRuns = 5, iterations = PERF_ITERATIONS)
    public void perfNioWriteInt() throws Exception
    {
        final ByteBuffer buffer = ByteBuffer.allocate(2048);
        buffer.mark();
        for(int i = 0; i < TEST_ITERATIONS; i++)
        {
            for(int j = 0; j < WRITE_OP_COUNT; j++)
            {
                buffer.putInt(j);
            }
            buffer.reset();
        }
    }

    @Test
    @PerfTest(name = "write long ByteOutputBuffer", warmUpRuns = 5, iterations = PERF_ITERATIONS)
    public void perfTestWriteLong() throws Exception
    {
        final ByteArrayOutputBufferImpl byteArrayOutputBuffer;
        byteArrayOutputBuffer = new ByteArrayOutputBufferImpl(4096);
        for(int i = 0; i < TEST_ITERATIONS; i++)
        {
            for(int j = 0; j < WRITE_OP_COUNT; j++)
            {
                byteArrayOutputBuffer.writeLong(j);
            }
            byteArrayOutputBuffer.reset();
        }
    }

    @Test
    @PerfTest(name = "write long nio.ByteBuffer", warmUpRuns = 5, iterations = PERF_ITERATIONS)
    public void perfNioWriteLong() throws Exception
    {
        final ByteBuffer buffer = ByteBuffer.allocate(4096);
        buffer.mark();
        for(int i = 0; i < TEST_ITERATIONS; i++)
        {
            for(int j = 0; j < WRITE_OP_COUNT; j++)
            {
                buffer.putLong(j);
            }
            buffer.reset();
        }
    }

    @Test
    @PerfTest(name = "write byte[] ByteOutputBuffer", warmUpRuns = 5, iterations = PERF_ITERATIONS)
    public void perfTestWriteBytes() throws Exception
    {
        final ByteArrayOutputBufferImpl byteArrayOutputBuffer;
        byteArrayOutputBuffer = new ByteArrayOutputBufferImpl(4096);
        for(int i = 0; i < TEST_ITERATIONS; i++)
        {
            for(int j = 0; j < WRITE_OP_COUNT; j++)
            {
                byteArrayOutputBuffer.writeBytes(TEST_BYTES, 0, 8);
            }
            byteArrayOutputBuffer.reset();
        }
    }

    @Test
    @PerfTest(name = "write byte[] nio.ByteBuffer", warmUpRuns = 5, iterations = PERF_ITERATIONS)
    public void perfNioWriteBytes() throws Exception
    {
        final ByteBuffer buffer = ByteBuffer.allocate(4096);
        buffer.mark();
        for(int i = 0; i < TEST_ITERATIONS; i++)
        {
            for(int j = 0; j < WRITE_OP_COUNT; j++)
            {
                buffer.put(TEST_BYTES, 0, 8);
            }
            buffer.reset();
        }
    }
}
