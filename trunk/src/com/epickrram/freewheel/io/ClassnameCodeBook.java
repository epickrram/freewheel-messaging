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
    private final Map<String, Encoder> encoderMap = new ConcurrentHashMap<String, Encoder>();
    private final Map<String, Decoder> decoderMap = new ConcurrentHashMap<String, Decoder>();

    public ClassnameCodeBook()
    {
        registerStandardHandlers();
    }

    @SuppressWarnings({"unchecked"})
    public Decoder getDecoder(final String code)
    {
        return decoderMap.get(code);
    }

    @SuppressWarnings({"unchecked"})
    public Encoder getEncoder(final String code)
    {
        return encoderMap.get(code);
    }

    public void registerHandlers(final String code, final Encoder encoder, final Decoder decoder)
    {
        encoderMap.put(code, encoder);
        decoderMap.put(code, decoder);
    }

    private void registerStandardHandlers()
    {
        final IntegerTranslator integerTranslator = new IntegerTranslator();
        registerHandlers(Integer.class.getName(), integerTranslator, integerTranslator);
        registerHandlers(int.class.getName(), integerTranslator, integerTranslator);
        final StringTranslator stringTranslator = new StringTranslator();
        registerHandlers(String.class.getName(), stringTranslator, stringTranslator);
        final LongTranslator longTranslator = new LongTranslator();
        registerHandlers(Long.class.getName(), longTranslator, longTranslator);
        registerHandlers(long.class.getName(), longTranslator, longTranslator);
    }

    private static final class StringTranslator implements Encoder<String>, Decoder<String>
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

    private static final class IntegerTranslator implements Encoder<Integer>, Decoder<Integer>
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

    private static final class LongTranslator implements Encoder<Long>, Decoder<Long>
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