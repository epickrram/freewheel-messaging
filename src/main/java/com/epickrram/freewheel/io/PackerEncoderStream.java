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
package com.epickrram.freewheel.io;

import com.epickrram.freewheel.protocol.CodeBook;
import com.epickrram.freewheel.protocol.Translator;
import org.msgpack.packer.Packer;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public final class PackerEncoderStream implements EncoderStream
{
    private final CodeBook codeBook;
    private final Packer packer;

    public PackerEncoderStream(final CodeBook codeBook, final Packer packer)
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
        final boolean isNull = s == null;
        packer.writeBoolean(isNull);
        if(!isNull)
        {
            packer.writeString(s);
        }
    }

    @SuppressWarnings({"unchecked"})
    public <T> void writeObject(final T o) throws IOException
    {
        packer.writeBoolean(o == null);
        if(o != null)
        {
            packer.writeInt(codeBook.getTranslatorCode(o.getClass()));
            final Translator<T> translator = (Translator<T>) codeBook.getTranslator(o.getClass());
            if(translator == null)
            {
                throw new IllegalStateException("Cannot encode object of type: " + o.getClass().getName());
            }
            translator.encode(o, this);
        }
    }

    @Override
    public <T> void writeCollection(final Collection<T> collection) throws IOException
    {
        packer.writeBoolean(collection == null);
        if(collection != null)
        {
            packer.writeInt(collection.size());
            for (T t : collection)
            {
                writeObject(t);
            }
        }
    }

    @Override
    public <K, V> void writeMap(final Map<K, V> collection) throws IOException
    {
        packer.writeBoolean(collection == null);
        if(collection != null)
        {
            packer.writeInt(collection.size());
            for (Map.Entry<K, V> entry : collection.entrySet())
            {
                writeObject(entry.getKey());
                writeObject(entry.getValue());
            }
        }
    }

}