package com.epickrram.freewheel.messaging;

import com.epickrram.freewheel.io.DecoderStream;

public interface Receiver
{
    void onMessage(int topicId, DecoderStream decoderStream);
}