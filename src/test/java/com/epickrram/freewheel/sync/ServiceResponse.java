package com.epickrram.freewheel.sync;

import java.math.BigDecimal;

public interface ServiceResponse
{
    void onAccountState(final long accountId, final BigDecimal balance);
    void onMethodTwoResponse(final String identifier, final String value);
}
