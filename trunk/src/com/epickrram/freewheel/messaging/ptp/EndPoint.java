package com.epickrram.freewheel.messaging.ptp;

import java.net.InetAddress;

final class EndPoint
{
    private final InetAddress address;
    private final int port;

    EndPoint(final InetAddress address, final int port)
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
