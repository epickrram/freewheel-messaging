package com.epickrram.freewheel.sync;

import com.epickrram.freewheel.messaging.config.Remote;

import java.math.BigDecimal;

@Remote
public interface ServiceResponse
{
    void onAccountState(final long accountId, final BigDecimal balance);
    void onMethodTwoResponse(final String identifier, final String value);
}
