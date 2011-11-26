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

public final class EndPoint
{
    private final InetAddress address;
    private final int port;

    public EndPoint(final InetAddress address, final int port)
    {
        this.address = address;
        this.port = port;
    }

    public InetAddress getAddress()
    {
        return address;
    }

    public int getPort()
    {
        return port;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final EndPoint endPoint = (EndPoint) o;

        if (port != endPoint.port) return false;
        if (address != null ? !address.equals(endPoint.address) : endPoint.address != null) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = address != null ? address.hashCode() : 0;
        result = 31 * result + port;
        return result;
    }
}
