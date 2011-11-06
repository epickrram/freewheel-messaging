package com.epickrram.freewheel.messaging.ptp;

public interface EndPointProvider
{
    EndPoint resolveEndPoint(final Class descriptor);
}
