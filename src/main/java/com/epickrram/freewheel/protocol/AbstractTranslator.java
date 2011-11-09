package com.epickrram.freewheel.protocol;

import com.epickrram.freewheel.io.DecoderStream;
import com.epickrram.freewheel.io.EncoderStream;

import java.io.IOException;

public abstract class AbstractTranslator<T> implements Translator<T>
{
    @Override
    public void encode(final T encodable, final EncoderStream encoderStream)
    {
        try
        {
            doEncode(encodable, encoderStream);
        }
        catch (IOException e)
        {
            throw new TranslatorException("Exception occurred while encoding " + encodable, e);
        }
    }

    @Override
    public T decode(final DecoderStream decoderStream)
    {
        try
        {
            return doDecode(decoderStream);
        }
        catch (IOException e)
        {
            throw new TranslatorException("Exception occurred while decoding", e);
        }
    }
    
    protected abstract void doEncode(final T encodable, final EncoderStream encoderStream) throws IOException;
    protected abstract T doDecode(final DecoderStream decoderStream) throws IOException;
}