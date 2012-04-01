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

import com.epickrram.freewheel.messaging.MessagingService;
import com.epickrram.freewheel.messaging.config.Remote;
import com.epickrram.freewheel.protocol.CodeBook;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtNewMethod;
import javassist.bytecode.MethodInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.epickrram.freewheel.remoting.MethodHelper.appendEncodeParameterCalls;
import static com.epickrram.freewheel.remoting.MethodHelper.appendMethodNameSuffix;
import static com.epickrram.freewheel.remoting.MethodHelper.appendParameterTypes;
import static com.epickrram.freewheel.remoting.ReflectionUtil.ensureNoPrimitiveReturnTypes;
import static com.epickrram.freewheel.remoting.ReflectionUtil.hasSyncMethods;

public final class DirectPublisherFactory extends AbstractPublisherFactory
{
    private final MessagingService messagingService;

    public DirectPublisherFactory(final MessagingService messagingService, final TopicIdGenerator topicIdGenerator, final CodeBook codeBook)
    {
        super(AbstractPublisher.class.getName(), topicIdGenerator, codeBook);
        this.messagingService = messagingService;
    }

    @Override
    protected <T> void validatePublisher(final Class<T> descriptor)
    {
        final boolean hasSyncMethods = hasSyncMethods(descriptor);
        if (hasSyncMethods && !messagingService.supportsSendAndWait())
        {
            throw new IllegalArgumentException(String.format("Publisher interface %s requires a MessagingService that supports sendAndWait", descriptor.getName()));
        }
        if(hasSyncMethods)
        {
            ensureNoPrimitiveReturnTypes(descriptor);
        }
    }

    @SuppressWarnings({"unchecked"})
    @Override
    protected <T> T createPublisher(final Class<T> descriptor, final Constructor jdkConstructor) throws InstantiationException, IllegalAccessException, InvocationTargetException
    {
        return (T) jdkConstructor.newInstance(messagingService, topicIdGenerator.getTopicId(descriptor), codeBook);
    }

    @Override
    protected Constructor createConstructor(final Class<?> generatedPublisherClass, final Remote definition, final Class<?> descriptor) throws NoSuchMethodException
    {
        return generatedPublisherClass.getConstructor(new Class[]{MessagingService.class, int.class, CodeBook.class});
    }

    @Override
    protected MethodInfo generateMethod(final Method method, final int methodIndex, final CtClass ctClass) throws CannotCompileException
    {
        final StringBuilder methodSource = new StringBuilder();
        final boolean isSyncMethod = ReflectionUtil.isSyncMethod(method);

        final Class<?> returnType = method.getReturnType();
        methodSource.append("public ").append(isSyncMethod ? returnType.getName() : "void").append(" ").append(method.getName()).append("(");

        final Class<?>[] parameterTypes = method.getParameterTypes();
        appendParameterTypes(methodSource, parameterTypes);

        methodSource.append(") {").
                append("\ntry {\n").
                append("final ByteArrayOutputStream buffer = getOutputStream();\n").
                append("final EncoderStream encoderStream = new PackerEncoderStream(getCodeBook(), new MessagePackPacker(buffer));\n").
                append("encoderStream.writeInt(getTopicId());\n").
                append("encoderStream.writeByte((byte) ").
                append(methodIndex).
                append(");\n");

        appendEncodeParameterCalls(methodSource, parameterTypes);

        if (isSyncMethod)
        {
            methodSource.append("final DecoderStream decoderStream = getMessagingService().sendAndWait(getTopicId(), buffer);\n");
            methodSource.append("return (").append(returnType.getName()).append(") ");
            methodSource.append("decoderStream.read");
            appendMethodNameSuffix(returnType, methodSource);
            methodSource.append("();");
        }
        else
        {
            methodSource.append("getMessagingService().send(getTopicId(), buffer);\n");
        }

        methodSource.append("\n} catch(Throwable e) {\n").
                append("throw new RuntimeException(\"Failed to write \", e);\n").
                append("}\n}\n");

        return CtNewMethod.make(methodSource.toString(), ctClass).getMethodInfo();
    }
}