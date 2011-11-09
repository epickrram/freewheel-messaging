package com.epickrram.freewheel.remoting;

import java.lang.reflect.Method;
import java.util.Comparator;

public final class MethodNameComparator implements Comparator<Method>
{
    public int compare(final Method o1, final Method o2)
    {
        return o1.toGenericString().compareTo(o2.toGenericString());
    }
}
