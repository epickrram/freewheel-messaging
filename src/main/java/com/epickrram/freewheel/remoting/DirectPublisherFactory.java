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
import com.epickrram.freewheel.messaging.MessagingService;
import com.epickrram.freewheel.protocol.CodeBook;
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

import static com.epickrram.freewheel.remoting.GeneratedClassRegistry.CONSTRUCTOR_MAP;

public final class DirectPublisherFactory implements PublisherFactory
{
    private final MessagingService messagingService;
    private final TopicIdGenerator topicIdGenerator;
    private final CodeBook codeBook;

    public DirectPublisherFactory(final MessagingService messagingService, final TopicIdGenerator topicIdGenerator, final CodeBook codeBook)
    {
        this.messagingService = messagingService;
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
        validatePublisherInterface(descriptor);
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
            classPool.importPackage("org.msgpack.packer");
            classPool.importPackage("java.io");
            final CtClass superClass = classPool.get("com.epickrram.freewheel.remoting.AbstractPublisher");
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

            final Constructor jdkConstructor = ctClass.toClass().getConstructor(new Class[]{MessagingService.class, int.class, CodeBook.class});
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

    private <T> void validatePublisherInterface(final Class<T> descriptor)
    {
        final boolean hasSyncMethods = hasSyncMethods(descriptor);
        if (hasSyncMethods && !messagingService.supportsSendAndWait())
        {
            throw new IllegalArgumentException("Publisher interface requires a MessagingService that supports sendAndWait");
        }
        if(hasSyncMethods)
        {
            ensureNoPrimitiveReturnTypes(descriptor);
        }
    }

    private <T> void ensureNoPrimitiveReturnTypes(final Class<T> descriptor)
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

    private <T> boolean hasSyncMethods(final Class<T> descriptor)
    {
        final Method[] methods = descriptor.getMethods();
        for (Method method : methods)
        {
            if (ReflectionUtil.isSyncMethod(method))
            {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings({"unchecked"})
    private <T> T createPublisher(final Class<T> descriptor, final Constructor jdkConstructor) throws InstantiationException, IllegalAccessException, InvocationTargetException
    {
        return (T) jdkConstructor.newInstance(messagingService, topicIdGenerator.getTopicId(descriptor), codeBook);
    }

    private MethodInfo createMethod(final Method method, final int methodIndex, final CtClass ctClass) throws CannotCompileException
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

        appendBufferCalls(methodSource, parameterTypes);

        if (isSyncMethod)
        {
            methodSource.append("final DecoderStream decoderStream = getMessagingService().sendAndWait(getTopicId(), buffer);\n");
            methodSource.append("return ");

            if (int.class == returnType)
            {
                methodSource.append("decoderStream.readInt();");
            }
            else if (long.class == returnType)
            {
                methodSource.append("decoderStream.readLong();");
            }
            else if (byte.class == returnType)
            {
                methodSource.append("decoderStream.readByte();");
            }
            else if (String.class == returnType)
            {
                methodSource.append("decoderStream.readString();");
            }
            else
            {
                methodSource.append(" (").append(returnType.getName()).append(") ");
                methodSource.append("decoderStream.readObject();");
            }
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

    private void appendBufferCalls(final StringBuilder methodSource, final Class<?>[] parameterTypes)
    {
        char id = 'a';
        for (int i = 0, n = parameterTypes.length; i < n; i++)
        {
            methodSource.append("encoderStream.");
            appendBufferMethodCall(parameterTypes[i], methodSource, id++);
            methodSource.append("\n");
        }
    }

    private void appendParameterTypes(final StringBuilder methodSource, final Class<?>[] parameterTypes)
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

    private static void appendBufferMethodCall(final Class<?> parameterType, final StringBuilder methodSource, final char parameterName)
    {
        if (int.class == parameterType)
        {
            methodSource.append("writeInt(").append(parameterName).append(");");
        }
        else if (long.class == parameterType)
        {
            methodSource.append("writeLong(").append(parameterName).append(");");
        }
        else if (byte.class == parameterType)
        {
            methodSource.append("writeByte(").append(parameterName).append(");");
        }
        else if (String.class == parameterType)
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
}