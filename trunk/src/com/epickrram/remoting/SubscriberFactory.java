package com.epickrram.remoting;

import com.epickrram.messaging.Receiver;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.Modifier;
import javassist.NotFoundException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public final class SubscriberFactory
{
    public <T> Receiver createReceiver(final Class<T> descriptor, final T instance) throws RemotingException
    {
        final String subscriberClassname = getGeneratedClassname(descriptor);
        final ClassPool classPool = ClassPool.getDefault();
        classPool.importPackage("com.epickrram.messaging");
        classPool.importPackage("com.epickrram.stream");
        classPool.importPackage("com.epickrram.remoting");

        try
        {
            final String descriptorClassname = classDefinitionToClassname(descriptor);
            final String invokerInterfaceName = descriptorClassname + "Invoker";
            final CtClass invokerInterfaceClass = classPool.makeInterface(invokerInterfaceName);

            final CtClass ctClass = createSubscriberClass(subscriberClassname, classPool, descriptorClassname, invokerInterfaceName, invokerInterfaceClass);
            createConstructor(descriptor, subscriberClassname, classPool, ctClass, invokerInterfaceName);
            createReceiveMethod(ctClass);

            final Class generatedClass = ctClass.toClass();
            final Constructor jdkConstructor = generatedClass.getConstructor(new Class[]{descriptor});
            return (Receiver) jdkConstructor.newInstance(instance);
        }
        catch (NotFoundException e)
        {
            throw new RemotingException("Failed to create Subscriber", e);
        }
        catch (NoSuchMethodException e)
        {
            throw new RemotingException("Failed to create Subscriber", e);
        }
        catch (CannotCompileException e)
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

    private CtClass createSubscriberClass(final String subscriberClassname, final ClassPool classPool, final String descriptorClassname,
                                          final String invokerInterfaceName, final CtClass invokerInterfaceClass)
            throws NotFoundException, CannotCompileException
    {
        final CtClass ctClass = classPool.makeClass(subscriberClassname);
        ctClass.addInterface(classPool.get("com.epickrram.messaging.Receiver"));
        final CtMethod invocationMethod = CtMethod.make("public void invoke(" + descriptorClassname + " implementation, " +
                                                        "ByteInputBuffer byteInputBuffer);", invokerInterfaceClass);
        invokerInterfaceClass.addMethod(invocationMethod);
        invokerInterfaceClass.toClass();

        ctClass.setModifiers(Modifier.PUBLIC | Modifier.FINAL);
        ctClass.addField(CtField.make("private final " + invokerInterfaceName + "[] invokers;", ctClass));
        ctClass.addField(CtField.make("private final " + descriptorClassname + " implementation;", ctClass));
        return ctClass;
    }

    private <T> void createConstructor(final Class<T> descriptor, final String subscriberClassname, final ClassPool classPool,
                                       final CtClass ctClass, final String invokerInterfaceName)
            throws NotFoundException, CannotCompileException
    {
        final Method[] methods = descriptor.getDeclaredMethods();
        Arrays.sort(methods, new MethodNameComparator());
        final CtClass[] methodInvokers = new CtClass[methods.length];
        final StringBuilder constructorBody = new StringBuilder("{ implementation = $1; invokers = new ").
                append(invokerInterfaceName).append("[").append(methods.length).append("];");
        for (int methodIndex = 0; methodIndex < methods.length; methodIndex++)
        {
            final Method method = methods[methodIndex];
            methodInvokers[methodIndex] = generateMethodInvoker(methodIndex, method, classPool, subscriberClassname, descriptor, invokerInterfaceName);
            constructorBody.append("invokers[").append(methodIndex).append("] = new ").
                    append(methodInvokers[methodIndex].getName().replace('$', '.')).append("();\n");
        }
        constructorBody.append("}");

        final CtConstructor ctConstructor = new CtConstructor(new CtClass[]{classPool.getCtClass(descriptor.getName())}, ctClass);
        ctConstructor.setBody(constructorBody.toString());
        ctClass.addConstructor(ctConstructor);
    }

    private <T> CtClass generateMethodInvoker(final int methodIndex, final Method method, final ClassPool classPool,
                                          final String generatedClassname, final Class<T> descriptor, final String invokerInterfaceName)
            throws NotFoundException, CannotCompileException
    {
        final CtClass ctClass = classPool.makeClass(generatedClassname + "Invoker" + methodIndex);
        ctClass.addInterface(classPool.getCtClass(invokerInterfaceName));
        final StringBuilder methodSource = new StringBuilder().
                append("public void invoke(").append(classDefinitionToClassname(descriptor)).
                append(" implementation, ByteInputBuffer inputBuffer) {");
        final Class<?>[] parameterTypes = method.getParameterTypes();
        char parameterId = 'a';
        for (final Class<?> parameterType : parameterTypes)
        {
            if (parameterType.equals(int.class))
            {
                methodSource.append("final int ").append((parameterId++)).append(" = inputBuffer.readInt();\n");
            }
            else if (parameterType.equals(long.class))
            {
                methodSource.append("final long ").append((parameterId++)).append(" = inputBuffer.readLong();\n");
            }
            else if (parameterType.equals(byte.class))
            {
                methodSource.append("final byte ").append((parameterId++)).append(" = inputBuffer.readByte();\n");
            }
            else
            {
                throw new RemotingException("Don't know how to read type " + parameterType + " from stream!");
            }
        }
        methodSource.append("implementation.").append(method.getName()).append("(");
        parameterId = 'a';
        for (int i = 0; i < parameterTypes.length; i++)
        {
            if(i != 0)
            {
                methodSource.append(", ");
            }
            methodSource.append((parameterId++));
        }
        methodSource.append(");}");

        ctClass.addMethod(CtMethod.make(methodSource.toString(), ctClass));
        ctClass.addConstructor(CtNewConstructor.defaultConstructor(ctClass));
        ctClass.toClass();
        return ctClass;
    }

    private void createReceiveMethod(final CtClass ctClass) throws CannotCompileException
    {
        final String invocation = "public void onMessage(int topicId, ByteInputBuffer byteInputBuffer) {" +
                                  " int methodIndex = byteInputBuffer.readByte();" +
                                  " invokers[methodIndex].invoke(implementation, byteInputBuffer);" +
                                  "}";
        ctClass.addMethod(CtMethod.make(invocation, ctClass));
    }

    private String getGeneratedClassname(final Class<?> descriptor)
    {
        return descriptor.getName() + "Subscriber";
    }

    private <T> String classDefinitionToClassname(final Class<T> descriptor)
    {
        return descriptor.getName().replace('$', '.');
    }
}