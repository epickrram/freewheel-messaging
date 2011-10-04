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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClassnameCodeBook implements CodeBook<String>
{
    private final Map<String, Transcoder> transcoderMap = new ConcurrentHashMap<String, Transcoder>();

    public ClassnameCodeBook()
    {
        registerStandardHandlers();
    }

    @SuppressWarnings({"unchecked"})
    public <T> Transcoder<T> getTranscoder(final String code)
    {
        return transcoderMap.get(code);
    }

    public <T> void registerTranscoder(final String code, final Transcoder<T> transcoder)
    {
        transcoderMap.put(code, transcoder);
    }

    private void registerStandardHandlers()
    {
        final IntegerTranscoder integerTranscoder = new IntegerTranscoder();
        registerTranscoder(Integer.class.getName(), integerTranscoder);
        final StringTranscoder stringTranscoder = new StringTranscoder();
        registerTranscoder(String.class.getName(), stringTranscoder);
        final LongTranscoder longTranscoder = new LongTranscoder();
        registerTranscoder(Long.class.getName(), longTranscoder);
    }

    private static final class StringTranscoder implements Transcoder<String>
    {
        @Override
        public String decode(final DecoderStream decoderStream) throws IOException
        {
            return decoderStream.readString();
        }

        @Override
        public void encode(final String encodable, final EncoderStream encoderStream) throws IOException
        {
            encoderStream.writeString(encodable);
        }
    }

    private static final class IntegerTranscoder implements Transcoder<Integer>
    {
        @Override
        public void encode(final Integer encodable, final EncoderStream encoderStream) throws IOException
        {
            encoderStream.writeInt(encodable);
        }

        @Override
        public Integer decode(final DecoderStream decoderStream) throws IOException
        {
            return decoderStream.readInt();
        }
    }

    private static final class LongTranscoder implements Transcoder<Long>
    {
        @Override
        public void encode(final Long encodable, final EncoderStream encoderStream) throws IOException
        {
            encoderStream.writeLong(encodable);
        }

        @Override
        public Long decode(final DecoderStream decoderStream) throws IOException
        {
            return decoderStream.readLong();
        }
    }
}