package com.epickrram.freewheel.sync;

import java.math.BigDecimal;

public interface ServiceClient
{
    @SyncMethod(requestMethod = "requestAccountState", responseMethod = "onAccountState")
    BigDecimal getBalance(final long accountId);

    @SyncMethod(requestMethod = "requestMethodTwo", responseMethod = "onMethodTwoResponse")
    String getMethodTwo(final String identifier);

    @SyncMethod(requestMethod = "requestAccountState", responseMethod = "onAccountState", timeoutMilliseconds = 1)
    BigDecimal timeoutAfterOneMillisecond(final long accountId);
}