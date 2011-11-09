package com.epickrram.freewheel.remoting;

public final class ClassNameTopicIdGenerator implements TopicIdGenerator
{
    public int getTopicId(final Class<?> descriptor)
    {
        return descriptor.getName().hashCode();
    }
}
