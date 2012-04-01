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

public final class MethodHelper
{
    private MethodHelper() {}

    static void appendEncodeParameterCalls(final StringBuilder methodSource, final Class<?>[] parameterTypes)
    {
        char id = 'a';
        for (int i = 0, n = parameterTypes.length; i < n; i++)
        {
            methodSource.append("encoderStream.");
            appendBufferMethodCall(parameterTypes[i], methodSource, id++);
            methodSource.append("\n");
        }
    }

    static void appendParameterTypes(final StringBuilder methodSource, final Class<?>[] parameterTypes)
    {
        char id = 'a';
        for (int i = 0, n = parameterTypes.length; i < n; i++)
        {
            if (i != 0)
            {
                methodSource.append(", ");
            }
            methodSource.append(parameterTypes[i].getName()).append(' ').append(id++);
        }
    }

    static void appendBufferMethodCall(final Class<?> parameterType, final StringBuilder methodSource, final char parameterName)
    {
        methodSource.append("write");
        appendMethodNameSuffix(parameterType, methodSource);
        methodSource.append("(").append(parameterName).append(");");
    }

    static void appendMethodNameSuffix(final Class<?> type, final StringBuilder source)
    {
        if (boolean.class == type)
        {
            source.append("Boolean");
        }
        else if (byte.class == type)
        {
            source.append("Byte");
        }
        else if (int.class == type)
        {
            source.append("Int");
        }
        else if (long.class == type)
        {
            source.append("Long");
        }
        else if(float.class == type)
        {
            source.append("Float");
        }
        else if(double.class == type)
        {
            source.append("Double");
        }
        else if (String.class == type)
        {
            source.append("String");
        }
        else
        {
            source.append("Object");
        }
    }
}
