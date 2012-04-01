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
import com.epickrram.freewheel.messaging.OutgoingMessageEvent;
import com.epickrram.freewheel.messaging.config.Remote;
import com.epickrram.freewheel.protocol.CodeBook;
import com.epickrram.freewheel.util.DaemonThreadFactory;
import com.epickrram.freewheel.util.RingBufferWrapper;
import com.lmax.disruptor.EventProcessor;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.epickrram.freewheel.remoting.GeneratedClassRegistry.CONSTRUCTOR_MAP;
import static java.util.Collections.singletonList;

public final class BufferedPublisherFactory implements PublisherFactory
{
    private final RingBufferFactory ringBufferFactory;
    private final TopicIdGenerator topicIdGenerator;
    private final CodeBook codeBook;
    private final Collection<LifecycleAware> eventProcessorLifecycleHandler;
    // TODO replace with Memoizer
    private static final Map<Class<?>, RingBufferWrapper<OutgoingMessageEvent>> RING_BUFFER_MAP =
            new ConcurrentHashMap<Class<?>, RingBufferWrapper<OutgoingMessageEvent>>();


    public BufferedPublisherFactory(final RingBufferFactory ringBufferFactory,
                                    final TopicIdGenerator topicIdGenerator,
                                    final CodeBook codeBook)
    {
        this.ringBufferFactory = ringBufferFactory;
        this.topicIdGenerator = topicIdGenerator;
        this.codeBook = codeBook;
        final LifecycleAware lifecycleHandler = new EventProcessorLifecycleAware(ringBufferFactory.getEventProcessors());
        eventProcessorLifecycleHandler = singletonList(lifecycleHandler);
    }

    @Override
    public Collection<LifecycleAware> getLifecycleAwareCollection()
    {
        return eventProcessorLifecycleHandler;
    }

    @SuppressWarnings({"unchecked"})
    public <T> T createPublisher(final Class<T> descriptor) throws RemotingException
    {
        final Remote definition = validateRemoteInterface(descriptor);
        try
        {
            if(CONSTRUCTOR_MAP.containsKey(descriptor))
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
            final CtClass superClass = classPool.get("com.epickrram.freewheel.remoting.AbstractReliablePublisher");
            final CtClass ctClass = classPool.makeClass(generatedClassname, superClass);
            final ClassFile classFile = ctClass.getClassFile();

            ctClass.setModifiers(Modifier.PUBLIC | Modifier.FINAL);
            classFile.addInterface(descriptor.getName());

            final Method[] methods = descriptor.getDeclaredMethods();
            Arrays.sort(methods, new MethodNameComparator());
            for (int methodIndex = 0; methodIndex < methods.length; methodIndex++)
            {
                final Method method = methods[methodIndex];
                classFile.addMethod(createMethod(method, methodIndex, ctClass));
            }

            final Constructor jdkConstructor = ctClass.toClass().getConstructor(new Class[]{RingBufferWrapper.class, int.class, CodeBook.class});
            CONSTRUCTOR_MAP.put(descriptor, jdkConstructor);
            RING_BUFFER_MAP.put(descriptor, ringBufferFactory.createRingBuffer(definition.messageStoreSize()));

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

    private <T> Remote validateRemoteInterface(final Class<T> descriptor)
    {
        final Remote annotation = descriptor.getAnnotation(Remote.class);
        if(annotation == null)
        {
            throw new IllegalArgumentException("Publisher interfaces must be marked with the @Remote annotation");
        }

        return annotation;
    }

    @SuppressWarnings({"unchecked"})
    private <T> T createPublisher(final Class<T> descriptor, final Constructor jdkConstructor) throws InstantiationException, IllegalAccessException, InvocationTargetException
    {
        return (T) jdkConstructor.newInstance(RING_BUFFER_MAP.get(descriptor), topicIdGenerator.getTopicId(descriptor), codeBook);
    }

    private MethodInfo createMethod(final Method method, final int methodIndex, final CtClass ctClass) throws CannotCompileException
    {
        final StringBuilder methodSource = new StringBuilder();
        methodSource.append("public void ").append(method.getName()).append("(");

        final Class<?>[] parameterTypes = method.getParameterTypes();
        appendParameterTypes(methodSource, parameterTypes);

        methodSource.append(") {\n").

                append("final RingBufferWrapper ringBuffer = getRingBuffer();\n").
                append("final long sequence = ringBuffer.next();\n").
                append("\ntry {\n").
                append("final OutgoingMessageEvent messageEvent = (OutgoingMessageEvent) ringBuffer.get(sequence);\n").
                append("messageEvent.reset();\n").
                append("messageEvent.setTopicId(getTopicId());\n").
                append("final EncoderStream encoderStream = messageEvent.getEncoderStream();\n").
                append("encoderStream.writeInt(getTopicId());\n").
                append("encoderStream.writeByte((byte) ").
                append(methodIndex).
                append(");\n");

        appendBufferCalls(methodSource, parameterTypes);

        methodSource.append("} catch(IOException e) {\n").
                append("throw new RuntimeException(\"Failed to write \", e);\n").
                append("}\n").
                append("finally {\nringBuffer.publish(sequence);}\n").
                append("}\n");

        return CtNewMethod.make(methodSource.toString(), ctClass).getMethodInfo();
    }

    private void appendBufferCalls(final StringBuilder methodSource, final Class<?>[] parameterTypes)
    {
        char id = 'a';
        for(int i = 0, n = parameterTypes.length; i < n; i++)
        {
            methodSource.append("encoderStream.");
            appendBufferMethodCall(parameterTypes[i], methodSource, id++);
        }
    }

    private void appendParameterTypes(final StringBuilder methodSource, final Class<?>[] parameterTypes)
    {
        char id = 'a';
        for(int i = 0, n = parameterTypes.length; i < n; i++)
        {
            if(i != 0)
            {
                methodSource.append(", ");
            }
            methodSource.append(parameterTypes[i].getName()).append(' ').append(id++);
        }
    }

    private static void appendBufferMethodCall(final Class<?> parameterType, final StringBuilder methodSource, final char parameterName)
    {
        if(int.class == parameterType)
        {
            methodSource.append("writeInt(").append(parameterName).append(");");
        }
        else if(long.class == parameterType)
        {
            methodSource.append("writeLong(").append(parameterName).append(");");
        }
        else if(byte.class == parameterType)
        {
            methodSource.append("writeByte(").append(parameterName).append(");");
        }
        else if(String.class == parameterType)
        {
            methodSource.append("writeString(").append(parameterName).append(");");
        }
        else
        {
            methodSource.append("writeObject(").append(parameterName).append(");");
        }
    }

    private String getGeneratedClassname(final Class<?> descriptor)
    {
        return descriptor.getName() + "Publisher";
    }

    private static final class EventProcessorLifecycleAware implements LifecycleAware
    {
        private final List<EventProcessor> eventProcessors;
        private volatile ExecutorService executorService;

        public EventProcessorLifecycleAware(final List<EventProcessor> eventProcessors)
        {
            this.eventProcessors = eventProcessors;
        }

        @Override
        public void systemStarting()
        {
            // TODO central control for Thread lifecycle
            final DaemonThreadFactory threadFactory = new DaemonThreadFactory("publisher");
            this.executorService = Executors.newFixedThreadPool(eventProcessors.size(), threadFactory);
            for (EventProcessor eventProcessor : eventProcessors)
            {
                executorService.submit(eventProcessor);
            }
        }

        @Override
        public void systemStopping()
        {
            // TODO should halt() ringbuffers
            executorService.shutdown();
        }
    }
}