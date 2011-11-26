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
package com.epickrram.freewheel.example;

import java.net.UnknownHostException;

public final class LocalRunner
{
    public static void main(String[] args) throws UnknownHostException, InterruptedException
    {
        final Thread pongThread = startThread("PONG", "127.0.0.1");
        final Thread pingThread = startThread("PING", "127.0.0.1");

        pongThread.join();
        pingThread.join();
    }

    private static Thread startThread(final String mode, final String address)
    {
        final Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    PingPong.main(new String[]{mode, address});
                }
                catch (UnknownHostException e)
                {
                    // ignore
                }
            }
        });
        thread.setName(mode);
        thread.start();
        return thread;
    }
}
