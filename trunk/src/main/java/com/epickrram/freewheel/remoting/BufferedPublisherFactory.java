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
import javassist.CtClass;
import javassist.CtNewMethod;
import javassist.bytecode.MethodInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.epickrram.freewheel.remoting.ReflectionUtil.hasSyncMethods;
import static java.util.Collections.singletonList;

public final class BufferedPublisherFactory extends AbstractPublisherFactory
{
    private final RingBufferFactory ringBufferFactory;
    private final Collection<LifecycleAware> eventProcessorLifecycleHandler;
    // TODO replace with Memoizer
    private static final Map<Class<?>, RingBufferWrapper<OutgoingMessageEvent>> RING_BUFFER_MAP =
            new ConcurrentHashMap<Class<?>, RingBufferWrapper<OutgoingMessageEvent>>();


    public BufferedPublisherFactory(final RingBufferFactory ringBufferFactory,
                                    final TopicIdGenerator topicIdGenerator,
                                    final CodeBook codeBook)
    {
        super(AbstractReliablePublisher.class.getName(), topicIdGenerator, codeBook);
        this.ringBufferFactory = ringBufferFactory;
        final LifecycleAware lifecycleHandler = new EventProcessorLifecycleAware(ringBufferFactory.getEventProcessors());
        eventProcessorLifecycleHandler = singletonList(lifecycleHandler);
    }

    @Override
    public Collection<LifecycleAware> getLifecycleAwareCollection()
    {
        return eventProcessorLifecycleHandler;
    }

    @Override
    protected <T> void validatePublisher(final Class<T> descriptor)
    {
        if(hasSyncMethods(descriptor))
        {
            throw new IllegalArgumentException("Buffered Publisher methods cannot have return values");
        }
    }

    @Override
    protected MethodInfo generateMethod(final Method method, final int methodIndex, final CtClass ctClass) throws CannotCompileException
    {
        final StringBuilder methodSource = new StringBuilder();
        methodSource.append("public void ").append(method.getName()).append("(");

        final Class<?>[] parameterTypes = method.getParameterTypes();
        MethodHelper.appendParameterTypes(methodSource, parameterTypes);

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

        MethodHelper.appendEncodeParameterCalls(methodSource, parameterTypes);

        methodSource.append("} catch(IOException e) {\n").
                append("throw new RuntimeException(\"Failed to write \", e);\n").
                append("}\n").
                append("finally {\nringBuffer.publish(sequence);}\n").
                append("}\n");

        return CtNewMethod.make(methodSource.toString(), ctClass).getMethodInfo();
    }

    @Override
    protected Constructor createConstructor(final Class<?> generatedPublisherClass, final Remote definition, final Class<?> descriptor) throws NoSuchMethodException
    {
        final Constructor jdkConstructor = generatedPublisherClass.getConstructor(new Class[]{RingBufferWrapper.class, int.class, CodeBook.class});
        RING_BUFFER_MAP.put(descriptor, ringBufferFactory.createRingBuffer(definition.messageStoreSize()));
        return jdkConstructor;
    }

    @SuppressWarnings({"unchecked"})
    protected <T> T createPublisher(final Class<T> descriptor, final Constructor jdkConstructor) throws InstantiationException, IllegalAccessException, InvocationTargetException
    {
        return (T) jdkConstructor.newInstance(RING_BUFFER_MAP.get(descriptor), topicIdGenerator.getTopicId(descriptor), codeBook);
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