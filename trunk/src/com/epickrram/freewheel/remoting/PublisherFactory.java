package com.epickrram.freewheel.remoting;

import com.epickrram.freewheel.io.CodeBook;
import com.epickrram.freewheel.messaging.MessagingService;
import com.epickrram.freewheel.util.Logger;
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

public final class PublisherFactory
{
    private static final Logger LOGGER = Logger.getLogger(PublisherFactory.class);
    private final MessagingService messagingService;
    private final TopicIdGenerator topicIdGenerator;
    private final CodeBook<String> codeBook;

    public PublisherFactory(final MessagingService messagingService, final TopicIdGenerator topicIdGenerator, final CodeBook<String> codeBook)
    {
        this.messagingService = messagingService;
        this.topicIdGenerator = topicIdGenerator;
        this.codeBook = codeBook;
    }

    @SuppressWarnings({"unchecked"})
    public <T> T createPublisher(final Class<T> descriptor) throws RemotingException
    {
        try
        {
            final String generatedClassname = getGeneratedClassname(descriptor);
            final ClassPool classPool = ClassPool.getDefault();
            classPool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
            classPool.appendClassPath(new LoaderClassPath(ClassLoader.getSystemClassLoader()));

            classPool.importPackage("com.epickrram.freewheel.messaging");
            classPool.importPackage("com.epickrram.freewheel.stream");
            classPool.importPackage("com.epickrram.freewheel.io");
            classPool.importPackage("com.epickrram.freewheel.remoting");
            classPool.importPackage("org.msgpack.packer");
            classPool.importPackage("java.io");
            LOGGER.info("AbstractPublisher: " + classPool.find("com.epickrram.freewheel.remoting.AbstractPublisher"));
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
            return (T) jdkConstructor.newInstance(messagingService, topicIdGenerator.getTopicId(descriptor), codeBook);
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

    private MethodInfo createMethod(final Method method, final int methodIndex, final CtClass ctClass) throws CannotCompileException
    {
        final StringBuilder methodSource = new StringBuilder();
        methodSource.append("public void ").append(method.getName()).append("(");

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

        methodSource.append("getMessagingService().send(getTopicId(), buffer);");

        methodSource.append("} catch(IOException e) {\n").
                append("throw new RuntimeException(\"Failed to write \", e);\n").
                append("}\n}\n");

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
}