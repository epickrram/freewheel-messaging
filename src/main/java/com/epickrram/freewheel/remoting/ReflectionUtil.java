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

package com.epickrram.freewheel.remoting;

import java.lang.reflect.Method;

public final class ReflectionUtil
{
    public static boolean isSyncMethod(final Method method)
    {
        return (method.getReturnType() != Void.class && method.getReturnType() != void.class) ||
                method.getExceptionTypes().length != 0;
    }

    public static <T> void ensureNoPrimitiveReturnTypes(final Class<T> descriptor)
    {
        final Method[] methods = descriptor.getMethods();
        for (Method method : methods)
        {
            if(method.getReturnType().isPrimitive() && method.getReturnType() != void.class && method.getReturnType() != Void.class)
            {
                throw new IllegalArgumentException(String.format("Primitive return types are not currently supported. Use a wrapper type for method %s, returning %s.",
                        method.getName(), method.getReturnType().getName()));
            }
        }
    }

    public static <T> boolean hasSyncMethods(final Class<T> descriptor)
    {
        final Method[] methods = descriptor.getMethods();
        for (Method method : methods)
        {
            if (isSyncMethod(method))
            {
                return true;
            }
        }
        return false;
    }
}
