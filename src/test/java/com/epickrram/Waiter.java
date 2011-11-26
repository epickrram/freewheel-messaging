//////////////////////////////////////////////////////////////////////////////////
//   Copyright 2011   Mark Price     mark at epickrram.com                      //
//                                                                              //
//   Licensed under the Apache License, Version 2.0 (the "License");            //
//   you may not use this file except in compliance with the License.           //
//   You may obtain a copy of the License at                                    //
//                                                                              //
//       http://www.apache.org/licenses/LICENSE-2.0                             //
//                                                                              //
//   Unless required by applicable law or agreed to in writing, software        //
//   distributed under the License is distributed on an "AS IS" BASIS,          //
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   //
//   See the License for the specific language governing permissions and        //
//   limitations under the License.                                             //
//////////////////////////////////////////////////////////////////////////////////
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