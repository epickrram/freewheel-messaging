/*
Copyright 2011 Mark Price

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.epickrram.freewheel.io;

import org.msgpack.unpacker.Unpacker;

import java.io.IOException;

public final class UnpackerDecoderStream implements DecoderStream
{
    private final CodeBook<String> codeBook;
    private final Unpacker unpacker;

    public UnpackerDecoderStream(final CodeBook<String> codeBook, final Unpacker unpacker)
    {
        this.codeBook = codeBook;
        this.unpacker = unpacker;
    }

    @Override
    public boolean readBoolean() throws IOException
    {
        return unpacker.readBoolean();
    }

    @Override
    public byte readByte() throws IOException
    {
        return unpacker.readByte();
    }

    @Override
    public int readInt() throws IOException
    {
        return unpacker.readInt();
    }

    @Override
    public long readLong() throws IOException
    {
        return unpacker.readLong();
    }

    @Override
    public float readFloat() throws IOException
    {
        return unpacker.readFloat();
    }

    @Override
    public double readDouble() throws IOException
    {
        return unpacker.readDouble();
    }

    @Override
    public byte[] readByteArray() throws IOException
    {
        return unpacker.readByteArray();
    }

    @Override
    public String readString() throws IOException
    {
        return unpacker.readString();
    }

    @Override
    public <T> T readObject() throws IOException
    {
        final boolean isNull = unpacker.readBoolean();
        if(isNull)
        {
            return null;
        }
        else
        {
            final String className = unpacker.readString();
            final Decoder<Object> decoder = codeBook.getDecoder(className);
            if(decoder == null)
            {
                throw new IllegalStateException("Cannot decode class: " + className);
            }
            return (T) decoder.decode(this);
        }
    }
}
