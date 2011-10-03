/*
Copyright 2011 Mark Price

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.epickrram.freewheel.io;

import com.epickrram.freewheel.util.Logger;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Enumeration;

public final class NetUtil
{
    private static final Logger LOGGER = Logger.getLogger(NetUtil.class);

    private NetUtil() {}

    public static NetworkInterface getMulticastCapableAddress() throws IOException
    {
        final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while(networkInterfaces.hasMoreElements())
        {
            final NetworkInterface networkInterface = networkInterfaces.nextElement();
            if(networkInterface.supportsMulticast())
            {
                LOGGER.info("Using multicast interface: " + networkInterface.getDisplayName());
                return networkInterface;
            }
        }
        throw new IOException("Could not find multicast-capable interface");
    }
}
