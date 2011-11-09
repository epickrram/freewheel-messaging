package com.epickrram.freewheel.remoting;

import com.epickrram.freewheel.io.DecoderStream;

public interface Invoker<T>
{
    void invoke(T implementation, DecoderStream decoderStream);
}
