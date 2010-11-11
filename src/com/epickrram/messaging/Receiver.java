package com.epickrram.messaging;

import com.epickrram.stream.ByteInputBuffer;

public interface Receiver
{
    void onMessage(int topicId, ByteInputBuffer byteInputBuffer);
}