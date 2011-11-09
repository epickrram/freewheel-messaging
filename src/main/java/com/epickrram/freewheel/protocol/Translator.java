package com.epickrram.freewheel.protocol;

import com.epickrram.freewheel.io.DecoderStream;
import com.epickrram.freewheel.io.EncoderStream;

import java.io.IOException;

public interface Translator<T>
{
    void encode(final T encodable, final EncoderStream encoderStream);
    T decode(final DecoderStream decoderStream);
}
