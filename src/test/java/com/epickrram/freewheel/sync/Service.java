package com.epickrram.freewheel.sync;

import com.epickrram.freewheel.messaging.config.Remote;

@Remote
public interface Service
{
    void requestAccountState(final long accountId);
    void requestMethodTwo(final String identifier);
}
