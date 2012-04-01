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

import com.epickrram.freewheel.messaging.LifecycleAware;
import com.epickrram.freewheel.messaging.config.Remote;
import com.epickrram.freewheel.protocol.CodeBook;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

import static com.epickrram.freewheel.remoting.GeneratedClassRegistry.CONSTRUCTOR_MAP;
import static com.epickrram.freewheel.remoting.MethodNameComparator.METHOD_COMPARATOR;
import static java.util.Arrays.sort;

public abstract class AbstractPublisherFactory implements PublisherFactory
{
    private final String abstractPublisherClassname;
    protected final TopicIdGenerator topicIdGenerator;
    protected final CodeBook codeBook;

    protected AbstractPublisherFactory(final String abstractPublisherClassname,
                                       final TopicIdGenerator topicIdGenerator,
                                       final CodeBook codeBook)
    {
        this.abstractPublisherClassname = abstractPublisherClassname;
        this.topicIdGenerator = topicIdGenerator;
        this.codeBook = codeBook;
    }

    @Override
    public Collection<LifecycleAware> getLifecycleAwareCollection()
    {
        return Collections.emptyList();
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <T> T createPublisher(final Class<T> descriptor) throws RemotingException
    {
        final Remote definition = validateRemoteInterface(descriptor);
        validatePublisher(descriptor);
        try
        {
            if (CONSTRUCTOR_MAP.containsKey(descriptor))
            {
                return createPublisher(descriptor, CONSTRUCTOR_MAP.get(descriptor));
            }
            final String generatedClassname = getGeneratedClassname(descriptor);
            final ClassPool classPool = new ClassPool(ClassPool.getDefault());
            classPool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
            classPool.appendClassPath(new LoaderClassPath(ClassLoader.getSystemClassLoader()));

            classPool.importPackage("com.epickrram.freewheel.messaging");
            classPool.importPackage("com.epickrram.freewheel.stream");
            classPool.importPackage("com.epickrram.freewheel.io");
            classPool.importPackage("com.epickrram.freewheel.remoting");
            classPool.importPackage("com.epickrram.freewheel.util");
            classPool.importPackage("org.msgpack.packer");
            classPool.importPackage("java.io");
            final CtClass superClass = classPool.get(abstractPublisherClassname);
            final CtClass ctClass = classPool.makeClass(generatedClassname, superClass);
            final ClassFile classFile = ctClass.getClassFile();

            ctClass.setModifiers(Modifier.PUBLIC | Modifier.FINAL);
            classFile.addInterface(descriptor.getName());

            final Method[] methods = descriptor.getDeclaredMethods();
            sort(methods, METHOD_COMPARATOR);
            for (int methodIndex = 0; methodIndex < methods.length; methodIndex++)
            {
                final Method method = methods[methodIndex];
                classFile.addMethod(generateMethod(method, methodIndex, ctClass));
            }

            final Constructor jdkConstructor = createConstructor(ctClass.toClass(), definition, descriptor);
            CONSTRUCTOR_MAP.put(descriptor, jdkConstructor);

            return createPublisher(descriptor, jdkConstructor);
        }
        catch (CannotCompileException e)
        {
            throw new RemotingException("Unable to generate publisher", e);
        }
        catch (NoSuchMethodException e)
        {
            throw new RemotingException("Unable to generate publisher", e);
        }
        catch (InvocationTargetException e)
        {
            throw new RemotingException("Unable to generate publisher", e);
        }
        catch (InstantiationException e)
        {
            throw new RemotingException("Unable to generate publisher", e);
        }
        catch (IllegalAccessException e)
        {
            throw new RemotingException("Unable to generate publisher", e);
        }
        catch (NotFoundException e)
        {
            throw new RemotingException("Unable to generate publisher", e);
        }
    }

    protected abstract <T> void validatePublisher(final Class<T> descriptor);

    protected abstract MethodInfo generateMethod(final Method method, final int methodIndex, final CtClass ctClass) throws CannotCompileException;

    protected abstract Constructor createConstructor(final Class<?> generatedPublisherClass, final Remote definition, final Class<?> descriptor) throws NoSuchMethodException;

    protected abstract <T> T createPublisher(final Class<T> descriptor, final Constructor jdkConstructor) throws InstantiationException, IllegalAccessException, InvocationTargetException;

    private String getGeneratedClassname(final Class<?> descriptor)
    {
        return descriptor.getName() + "Publisher";
    }

    private <T> Remote validateRemoteInterface(final Class<T> descriptor)
    {
        final Remote annotation = descriptor.getAnnotation(Remote.class);
        if (annotation == null)
        {
            throw new IllegalArgumentException("Publisher interfaces must be marked with the @Remote annotation");
        }

        return annotation;
    }
}
