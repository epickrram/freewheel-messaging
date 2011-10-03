package com.epickrram.freewheel.remoting;

public interface TopicIdGenerator
{
    int getTopicId(Class<?> descriptor);
}