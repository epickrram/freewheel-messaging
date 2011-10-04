package com.epickrram.freewheel.io;

import java.io.IOException;

public interface Transcoder<T>
{
    void encode(final T encodable, final EncoderStream encoderStream) throws IOException;
    T decode(final DecoderStream decoderStream) throws IOException;
}
