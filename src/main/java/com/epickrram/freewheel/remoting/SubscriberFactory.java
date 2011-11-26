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

import com.epickrram.freewheel.messaging.Receiver;
import com.epickrram.freewheel.util.Memoizer;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public final class SubscriberFactory
{
    private static final Memoizer<String, Class> GENERATED_SUBSCRIBER_CLASS_MAP = new Memoizer<String, Class>();

    public <T> Receiver createReceiver(final Class<T> descriptor, final T instance) throws RemotingException
    {
        final String subscriberClassname = getGeneratedClassname(descriptor);
        final ClassPool classPool = new ClassPool(ClassPool.getDefault());

        classPool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
        classPool.appendClassPath(new LoaderClassPath(ClassLoader.getSystemClassLoader()));

        classPool.importPackage("com.epickrram.freewheel.messaging");
        classPool.importPackage("com.epickrram.freewheel.io");
        classPool.importPackage("com.epickrram.freewheel.stream");
        classPool.importPackage("com.epickrram.freewheel.remoting");

        try
        {
            final String descriptorClassname = classDefinitionToClassname(descriptor);
            final String invokerInterfaceName = descriptorClassname + "Invoker";
            final CtClass invokerInterfaceClass = classPool.makeInterface(invokerInterfaceName);

            final Class generatedClass = GENERATED_SUBSCRIBER_CLASS_MAP.getValue(subscriberClassname,
                    new GeneratedSubscriberClassProvider<T>(subscriberClassname, classPool, descriptorClassname,
                            invokerInterfaceName, invokerInterfaceClass, descriptor));

            final Constructor jdkConstructor = generatedClass.getConstructor(new Class[]{descriptor});
            return (Receiver) jdkConstructor.newInstance(instance);
        }
        catch (NoSuchMethodException e)
        {
            throw new RemotingException("Failed to create Subscriber", e);
        }
        catch (InvocationTargetException e)
        {
            throw new RemotingException("Failed to create Subscriber", e);
        }
        catch (InstantiationException e)
        {
            throw new RemotingException("Failed to create Subscriber", e);
        }
        catch (IllegalAccessException e)
        {
            throw new RemotingException("Failed to create Subscriber", e);
        }
    }

    private String getGeneratedClassname(final Class<?> descriptor)
    {
        return descriptor.getName() + "Subscriber";
    }

    static <T> String classDefinitionToClassname(final Class<T> descriptor)
    {
        return descriptor.getName().replace('$', '.');
    }
}