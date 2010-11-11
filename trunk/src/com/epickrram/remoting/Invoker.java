package com.epickrram.remoting;

import com.epickrram.stream.ByteInputBuffer;

public interface Invoker<T>
{
    void invoke(T implementation, ByteInputBuffer inputBuffer);
}
