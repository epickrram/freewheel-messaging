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

package com.epickrram.freewheel.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class DaemonThreadFactory implements ThreadFactory
{
    private final AtomicInteger counter = new AtomicInteger();
    private final String namePrefix;

    public DaemonThreadFactory(final String namePrefix)
    {
        this.namePrefix = namePrefix;
    }

    @Override
    public Thread newThread(final Runnable r)
    {
        final Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.setName(namePrefix + "-" + counter.incrementAndGet());
        return thread;
    }
}
