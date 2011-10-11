package com.epickrram.freewheel;

import com.epickrram.freewheel.io.DecoderStream;
import com.epickrram.freewheel.io.EncoderStream;
import com.epickrram.freewheel.io.PackerEncoderStream;
import com.epickrram.freewheel.io.UnpackerDecoderStream;
import com.epickrram.freewheel.protocol.ClassnameCodeBook;
import org.junit.Before;
import org.junit.Test;
import org.msgpack.packer.MessagePackPacker;
import org.msgpack.unpacker.MessagePackUnpacker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public final class ProtocolTest
{
    private ClassnameCodeBook codeBook;
    private EncoderStream encoderStream;
    private ByteArrayOutputStream outputStream;

    @Test
    public void shouldHandlePrimitiveByte() throws Exception
    {
        final byte value = (byte) 127;
        encoderStream.writeByte(value);

        assertThat(getDecoderStream().readByte(), is(value));
    }

    @Test
    public void shouldHandleWrapperByte() throws Exception
    {
        assertTranscoding(Byte.valueOf((byte) 127));
    }

    @Test
    public void shouldHandlePrimitiveBoolean() throws Exception
    {
        final boolean value = true;
        encoderStream.writeBoolean(value);

        assertThat(getDecoderStream().readBoolean(), is(value));
    }

    @Test
    public void shouldHandleWrapperBoolean() throws Exception
    {
        assertTranscoding(Boolean.valueOf(true));
    }

    @Test
    public void shouldHandlePrimitiveInt() throws Exception
    {
        final int value = 17;
        encoderStream.writeInt(value);

        assertThat(getDecoderStream().readInt(), is(value));
    }

    @Test
    public void shouldHandleInteger() throws Exception
    {
        assertTranscoding(Integer.MAX_VALUE);
    }

    @Test
    public void shouldHandlePrimitiveLong() throws Exception
    {
        final long value = Long.MAX_VALUE;
        encoderStream.writeLong(value);

        assertThat(getDecoderStream().readLong(), is(value));
    }

    @Test
    public void shouldHandleLong() throws Exception
    {
        assertTranscoding(Long.MIN_VALUE);
    }

    @Test
    public void shouldHandlePrimitiveFloat() throws Exception
    {
        final float value = 17.77f;
        encoderStream.writeFloat(value);

        assertThat(getDecoderStream().readFloat(), is(value));
    }

    @Test
    public void shouldHandleFloat() throws Exception
    {
        assertTranscoding(Float.MAX_VALUE);
    }

    @Test
    public void shouldHandlePrimitiveDouble() throws Exception
    {
        final double value = 0.23847348374d;
        encoderStream.writeDouble(value);

        assertThat(getDecoderStream().readDouble(), is(value));
    }

    @Test
    public void shouldHandleDouble() throws Exception
    {
        assertTranscoding(Double.MIN_VALUE);
    }

    @Test
    public void shouldHandleByteArray() throws Exception
    {
        final byte[] value = {14, 15, 16, 16, 99, 127};
        encoderStream.writeByteArray(value);

        assertThat(getDecoderStream().readByteArray(), is(value));
    }

    @Test
    public void shouldHandlePartialByteArray() throws Exception
    {
        final byte[] value = {14, 15, 16, 16, 99, 127};
        final byte[] expected = {15, 16, 16, 99};
        encoderStream.writeByteArray(value, 1, 4);

        assertThat(getDecoderStream().readByteArray(), is(expected));
    }

    @Test
    public void shouldHandleString() throws Exception
    {
        final String value = "foobar";
        encoderStream.writeString(value);

        assertThat(value, is(getDecoderStream().readString()));
    }

    @Test
    public void shouldHandleObject() throws Exception
    {
        final TestObject value = new TestObject(17, "foobar");
        encoderStream.writeObject(value);

        assertThat(value, is(getDecoderStream().<TestObject>readObject()));
    }

    @Test
    public void shouldHandleArrayListOfObjects() throws Exception
    {
        final List<TestObject> value = new ArrayList<TestObject>(3);
        value.add(new TestObject(1, "foo"));
        value.add(new TestObject(2, "bar"));
        value.add(new TestObject(5, "boo"));

        assertTranscoding(value);
    }

    @Test
    public void shouldHandleLinkedListOfObjects() throws Exception
    {
        final List<TestObject> value = new LinkedList<TestObject>();
        value.add(new TestObject(1, "foo"));
        value.add(new TestObject(2, "bar"));
        value.add(new TestObject(5, "boo"));

        assertTranscoding(value);
    }

    // TODO null tests for Wrapper classes + String

    @Before
    public void setUp() throws Exception
    {
        codeBook = new ClassnameCodeBook();
        outputStream = new ByteArrayOutputStream(2048);
        encoderStream = new PackerEncoderStream(codeBook, new MessagePackPacker(outputStream));
        codeBook.registerTranscoder(TestObject.class.getName(), new TestObject.Transcoder());
    }

    private void assertTranscoding(final Object value) throws IOException
    {
        encoderStream.writeObject(value);

        assertThat(value, is(getDecoderStream().readObject()));
    }

    private UnpackerDecoderStream getDecoderStream()
    {
        return new UnpackerDecoderStream(codeBook,
                new MessagePackUnpacker(new ByteArrayInputStream(outputStream.toByteArray())));
    }

    private static final class TestObject
    {
        private final int foo;
        private final String bar;

        private TestObject(final int foo, final String bar)
        {
            this.foo = foo;
            this.bar = bar;
        }

        @Override
        public boolean equals(final Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final TestObject that = (TestObject) o;

            if (foo != that.foo) return false;
            if (bar != null ? !bar.equals(that.bar) : that.bar != null) return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = foo;
            result = 31 * result + (bar != null ? bar.hashCode() : 0);
            return result;
        }

        public static final class Transcoder implements com.epickrram.freewheel.protocol.Transcoder<TestObject>
        {
            @Override
            public void encode(final TestObject encodable, final EncoderStream encoderStream) throws IOException
            {
                encoderStream.writeInt(encodable.foo);
                encoderStream.writeString(encodable.bar);
            }

            @Override
            public TestObject decode(final DecoderStream decoderStream) throws IOException
            {
                final int foo = decoderStream.readInt();
                final String bar = decoderStream.readString();

                return new TestObject(foo, bar);
            }
        }
    }
}