package com.epickrram.freewheel;

import com.epickrram.freewheel.io.EncoderStream;
import com.epickrram.freewheel.io.PackerEncoderStream;
import com.epickrram.freewheel.io.UnpackerDecoderStream;
import com.epickrram.freewheel.protocol.CodeBookImpl;
import org.junit.Before;
import org.junit.Test;
import org.msgpack.packer.MessagePackPacker;
import org.msgpack.unpacker.MessagePackUnpacker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public final class ProtocolTest
{
    private CodeBookImpl codeBook;
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
        assertTranslation(Byte.valueOf((byte) 127));
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
        assertTranslation(Boolean.valueOf(true));
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
        assertTranslation(Integer.MAX_VALUE);
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
        assertTranslation(Long.MIN_VALUE);
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
        assertTranslation(Float.MAX_VALUE);
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
        assertTranslation(Double.MIN_VALUE);
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
    public void shouldHandleListOfObjects() throws Exception
    {
        final List<TestObject> input = new ArrayList<TestObject>(3);
        final List<TestObject> output = new ArrayList<TestObject>(3);

        input.add(new TestObject(1, "foo"));
        input.add(new TestObject(2, "bar"));
        input.add(new TestObject(5, "boo"));

        assertCollectionTranslation(input, output);
    }

    @Test
    public void shouldHandleSetOfObjects() throws Exception
    {
        final Set<TestObject> input = new HashSet<TestObject>();
        final Set<TestObject> output = new HashSet<TestObject>();
        input.add(new TestObject(1, "foo"));
        input.add(new TestObject(2, "bar"));
        input.add(new TestObject(5, "boo"));

        assertCollectionTranslation(input, output);
    }

    @Test
    public void shouldHandleMapOfObjects() throws Exception
    {
        final Map<Long, String> input = new HashMap<Long, String>();
        final Map<Long, String> output = new HashMap<Long, String>();
        input.put(7L, "foo");
        input.put(11L, "bar");
        input.put(13L, "foobar");

        assertMapTranslation(input, output);
    }

    @Test
    public void shouldHandleNullObject() throws Exception
    {
        assertTranslation(null);
    }

    @Test
    public void shouldHandleNullCollection() throws Exception
    {
        final List<String> output = new ArrayList<String>();

        encoderStream.writeCollection(null);
        getDecoderStream().readCollection(output);
        assertThat(output, is(equalTo(Collections.<String>emptyList())));
    }

    @Test
    public void shouldHandleNullMap() throws Exception
    {
        final Map<Long, String> output = new HashMap<Long, String>();

        encoderStream.writeMap(null);
        getDecoderStream().readMap(output);
        assertThat(output, is(equalTo(Collections.<Long, String>emptyMap())));
    }

    @Before
    public void setUp() throws Exception
    {
        codeBook = new CodeBookImpl();
        outputStream = new ByteArrayOutputStream(2048);
        encoderStream = new PackerEncoderStream(codeBook, new MessagePackPacker(outputStream));
        new CodeBookImpl.CodeBookRegistryImpl(codeBook).registerTranslatable(TestObject.class);
    }

    private void assertCollectionTranslation(final Collection<TestObject> input,
                                             final Collection<TestObject> output) throws IOException
    {
        encoderStream.writeCollection(input);
        getDecoderStream().readCollection(output);
        assertThat(output, is(equalTo(input)));
    }

    private <K, V> void assertMapTranslation(final Map<K, V> input,
                                             final Map<K, V> output) throws IOException
    {
        encoderStream.writeMap(input);
        getDecoderStream().readMap(output);
        assertThat(output, is(equalTo(input)));
    }

    private void assertTranslation(final Object value) throws IOException
    {
        encoderStream.writeObject(value);

        assertThat(value, is(getDecoderStream().readObject()));
    }

    private UnpackerDecoderStream getDecoderStream()
    {
        return new UnpackerDecoderStream(codeBook,
                new MessagePackUnpacker(new ByteArrayInputStream(outputStream.toByteArray())));
    }

}