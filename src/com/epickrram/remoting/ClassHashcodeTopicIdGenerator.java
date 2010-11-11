package com.epickrram.remoting;

public final class ClassHashcodeTopicIdGenerator implements TopicIdGenerator
{
    public int getTopicId(final Class<?> descriptor)
    {
        return descriptor.hashCode();
    }
}
