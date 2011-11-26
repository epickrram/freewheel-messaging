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
import org.msgpack.unpacker.Unpacker;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public final class UnpackerDecoderStream implements DecoderStream
{
    private final CodeBook codeBook;
    private final Unpacker unpacker;

    public UnpackerDecoderStream(final CodeBook codeBook, final Unpacker unpacker)
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
        final boolean isNull = unpacker.readBoolean();
        return isNull ? null : unpacker.readString();
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
            final int codeBookId = unpacker.readInt();
            final Translator<T> decoder = codeBook.getTranslator(codeBookId);
            if(decoder == null)
            {
                throw new IllegalStateException("Cannot decode class with id: " + codeBookId);
            }
            return decoder.decode(this);
        }
    }

    @Override
    public <T> void readCollection(final Collection<T> collection) throws IOException
    {
        final boolean isNull = unpacker.readBoolean();
        if(!isNull)
        {
            final int collectionSize = unpacker.readInt();
            for(int i = collectionSize; i != 0; i--)
            {
                collection.add(this.<T>readObject());
            }
        }
    }

    @Override
    public <K, V> void readMap(final Map<K, V> map) throws IOException
    {
        final boolean isNull = unpacker.readBoolean();
        if(!isNull)
        {
            final int mapSize = unpacker.readInt();
            for(int i = mapSize; i != 0; i--)
            {
                map.put(this.<K>readObject(), this.<V>readObject());
            }
        }
    }
}