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
package com.epickrram.freewheel.protocol;

import com.epickrram.freewheel.io.DecoderStream;
import com.epickrram.freewheel.io.EncoderStream;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CodeBookImpl implements CodeBook
{
    private final Map<Integer, Translator> translatorsByCodeBookIdMap = new ConcurrentHashMap<Integer, Translator>();
    private final Map<Class, Translator> translatorsByClassMap = new ConcurrentHashMap<Class, Translator>();
    private final Map<Class, Integer> codeBookIdByClassMap = new ConcurrentHashMap<Class, Integer>();

    public CodeBookImpl()
    {
        registerStandardTranslators();
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public <T> Translator<T> getTranslator(final int code)
    {
        return (Translator<T>) translatorsByCodeBookIdMap.get(code);
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public <T> Translator<T> getTranslator(final Class<T> cls)
    {
        return (Translator<T>) translatorsByClassMap.get(cls);
    }

    @Override
    public <T> int getTranslatorCode(final Class<T> cls)
    {
        return codeBookIdByClassMap.get(cls);
    }

    private <T> void registerTranslator(final int codeBookId, final Class<T> cls, final Translator<T> translator)
    {
        codeBookIdByClassMap.put(cls, codeBookId);
        translatorsByClassMap.put(cls, translator);
        translatorsByCodeBookIdMap.put(codeBookId, translator);
    }

    public static final class CodeBookRegistryImpl implements CodeBookRegistry
    {
        private static final int MINIMUM_CODE_BOOK_ID = 1025;
        private final CodeBookImpl codeBook;

        public CodeBookRegistryImpl(final CodeBookImpl codeBook)
        {
            this.codeBook = codeBook;
        }

        @SuppressWarnings({"unchecked"})
        @Override
        public <T> void registerTranslatable(final Class<T> cls)
        {
            final TranslatorHelper translatorHelper = new TranslatorHelper();
            registerTranslator(translatorHelper.getCodeBookId(cls),
                    translatorHelper.createTranslator(cls), cls);
        }

        @Override
        public <T> void registerTranslator(final int codeBookId, final Translator<T> translator, final Class<T> cls)
        {
            if(codeBookId < MINIMUM_CODE_BOOK_ID)
            {
                throw new TranslatorException("Illegal codeBookId: " + codeBookId + ", 0-1024 are reserved");
            }
            codeBook.registerTranslator(codeBookId, cls, translator);
        }
    }

    private void registerStandardTranslators()
    {
        registerTranslator(1, Integer.class, new IntegerTranslator());
        registerTranslator(1, int.class, new IntegerTranslator());
        registerTranslator(2, Long.class, new LongTranslator());
        registerTranslator(2, long.class, new LongTranslator());
        registerTranslator(3, Float.class, new FloatTranslator());
        registerTranslator(3, float.class, new FloatTranslator());
        registerTranslator(4, Double.class, new DoubleTranslator());
        registerTranslator(4, double.class, new DoubleTranslator());
        registerTranslator(5, Byte.class, new ByteTranslator());
        registerTranslator(5, byte.class, new ByteTranslator());
        registerTranslator(6, Boolean.class, new BooleanTranslator());
        registerTranslator(6, boolean.class, new BooleanTranslator());
        registerTranslator(7, String.class, new StringTranslator());
    }

    private static final class StringTranslator extends AbstractTranslator<String>
    {
        @Override
        public String doDecode(final DecoderStream decoderStream) throws IOException
        {
            return decoderStream.readString();
        }

        @Override
        public void doEncode(final String encodable, final EncoderStream encoderStream) throws IOException
        {
            encoderStream.writeString(encodable);
        }
    }

    private static final class IntegerTranslator extends AbstractTranslator<Integer>
    {
        @Override
        public void doEncode(final Integer encodable, final EncoderStream encoderStream) throws IOException
        {
            encoderStream.writeInt(encodable);
        }

        @Override
        public Integer doDecode(final DecoderStream decoderStream) throws IOException
        {
            return decoderStream.readInt();
        }
    }

    private static final class LongTranslator extends AbstractTranslator<Long>
    {
        @Override
        public void doEncode(final Long encodable, final EncoderStream encoderStream) throws IOException
        {
            encoderStream.writeLong(encodable);
        }

        @Override
        public Long doDecode(final DecoderStream decoderStream) throws IOException
        {
            return decoderStream.readLong();
        }
    }

    private static final class ByteTranslator extends AbstractTranslator<Byte>
    {
        @Override
        public void doEncode(final Byte encodable, final EncoderStream encoderStream) throws IOException
        {
            encoderStream.writeByte(encodable);
        }

        @Override
        public Byte doDecode(final DecoderStream decoderStream) throws IOException
        {
            return decoderStream.readByte();
        }
    }

    private static final class BooleanTranslator extends AbstractTranslator<Boolean>
    {
        @Override
        public void doEncode(final Boolean encodable, final EncoderStream encoderStream) throws IOException
        {
            encoderStream.writeBoolean(encodable);
        }

        @Override
        public Boolean doDecode(final DecoderStream decoderStream) throws IOException
        {
            return decoderStream.readBoolean();
        }
    }

    private static final class FloatTranslator extends AbstractTranslator<Float>
    {
        @Override
        public void doEncode(final Float encodable, final EncoderStream encoderStream) throws IOException
        {
            encoderStream.writeFloat(encodable);
        }

        @Override
        public Float doDecode(final DecoderStream decoderStream) throws IOException
        {
            return decoderStream.readFloat();
        }
    }

    private static final class DoubleTranslator extends AbstractTranslator<Double>
    {
        @Override
        public void doEncode(final Double encodable, final EncoderStream encoderStream) throws IOException
        {
            encoderStream.writeDouble(encodable);
        }

        @Override
        public Double doDecode(final DecoderStream decoderStream) throws IOException
        {
            return decoderStream.readDouble();
        }
    }
}