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

package com.epickrram.freewheel.messaging.ptp;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class StaticEndPointProvider implements EndPointProvider
{
    private final String hostname;
    private final int port;

    public StaticEndPointProvider(final String hostname, final int port)
    {
        this.hostname = hostname;
        this.port = port;
    }

    public static EndPointProvider localPort(final int port)
    {
        return new StaticEndPointProvider("localhost", port);
    }

    @Override
    public EndPoint resolveEndPoint(final Class descriptor)
    {
        try
        {
            return new EndPoint(InetAddress.getByName(hostname), port);
        }
        catch (UnknownHostException e)
        {
            throw new IllegalArgumentException("Cannot resolve hostname: " + hostname);
        }
    }
}
