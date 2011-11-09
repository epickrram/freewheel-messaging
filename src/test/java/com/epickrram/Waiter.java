package com.epickrram;

import static org.junit.Assert.fail;

public final class Waiter
{
    private static final long WAIT_TIMEOUT_MS = 5000L;

    private final Condition condition;
    private final long waitTimeout;

    public Waiter(final Condition condition)
    {
        this.condition = condition;
        waitTimeout = WAIT_TIMEOUT_MS;
    }

    public Waiter(final Condition condition, final long waitTimeout)
    {
        this.condition = condition;
        this.waitTimeout = waitTimeout;
    }

    public void waitForCondition()
    {
        final long timeoutPeriodEnd = System.currentTimeMillis() + waitTimeout;
        boolean conditionMet = false;
        while(timeoutPeriodEnd > System.currentTimeMillis())
        {
            if(condition.isMet())
            {
                conditionMet = true;
                break;
            }
            try
            {
                Thread.sleep(500L);
            }
            catch (InterruptedException e)
            {
                //ignore
            }
        }
        if(!conditionMet)
        {
            fail("Condition was not met before timeout. " + condition.getDescription());
        }
    }

    public interface Condition
    {
        boolean isMet();
        String getDescription();
    }
}