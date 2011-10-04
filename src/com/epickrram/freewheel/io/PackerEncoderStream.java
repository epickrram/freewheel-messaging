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

import org.msgpack.packer.Packer;

import java.io.IOException;

public final class PackerEncoderStream implements EncoderStream
{
    private final CodeBook<String> codeBook;
    private final Packer packer;

    public PackerEncoderStream(final CodeBook<String> codeBook, final Packer packer)
    {
        this.codeBook = codeBook;
        this.packer = packer;
    }

    @Override
    public void writeBoolean(final boolean v) throws IOException
    {
        packer.writeBoolean(v);
    }

    @Override
    public void writeByte(final byte v) throws IOException
    {
        packer.writeByte(v);
    }

    @Override
    public void writeInt(final int v) throws IOException
    {
        packer.writeInt(v);
    }

    @Override
    public void writeLong(final long v) throws IOException
    {
        packer.writeLong(v);
    }

    @Override
    public void writeFloat(final float v) throws IOException
    {
        packer.writeFloat(v);
    }

    @Override
    public void writeDouble(final double v) throws IOException
    {
        packer.writeDouble(v);
    }

    @Override
    public void writeByteArray(final byte[] b) throws IOException
    {
        packer.writeByteArray(b);
    }

    @Override
    public void writeByteArray(final byte[] b, final int off, final int len) throws IOException
    {
        packer.writeByteArray(b, off, len);
    }

    @Override
    public void writeString(final String s) throws IOException
    {
        packer.writeString(s);
    }

    public <T> void writeObject(final T o) throws IOException
    {
        packer.writeBoolean(o == null);
        if(o != null)
        {
            packer.writeString(o.getClass().getName());
            final Encoder<Object> encoder = codeBook.getTranscoder(o.getClass().getName());
            if(encoder == null)
            {
                throw new IllegalStateException("Cannot encode object of type: " + o.getClass().getName());
            }
            encoder.encode(o, this);
        }
    }
}