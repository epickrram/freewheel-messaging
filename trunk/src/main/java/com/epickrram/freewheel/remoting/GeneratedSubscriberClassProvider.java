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

import com.epickrram.freewheel.messaging.MessagingException;
import com.epickrram.freewheel.util.Provider;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.Modifier;
import javassist.NotFoundException;

import java.lang.reflect.Method;
import java.util.Arrays;

final class GeneratedSubscriberClassProvider<T> implements Provider<String, Class>
{
    private final String subscriberClassname;
    private final ClassPool classPool;
    private final String descriptorClassname;
    private final String invokerInterfaceName;
    private final CtClass invokerInterfaceClass;
    private final Class<T> descriptor;

    public GeneratedSubscriberClassProvider(final String subscriberClassname, final ClassPool classPool,
                                            final String descriptorClassname, final String invokerInterfaceName,
                                            final CtClass invokerInterfaceClass, final Class<T> descriptor)
    {
        this.subscriberClassname = subscriberClassname;
        this.classPool = classPool;
        this.descriptorClassname = descriptorClassname;
        this.invokerInterfaceName = invokerInterfaceName;
        this.invokerInterfaceClass = invokerInterfaceClass;
        this.descriptor = descriptor;
    }

    @Override
    public Class provide(final String key)
    {
        try
        {
            final CtClass ctClass = createSubscriberClass(subscriberClassname, classPool, descriptorClassname, invokerInterfaceName, invokerInterfaceClass);
            createConstructor(descriptor, subscriberClassname, classPool, ctClass, invokerInterfaceName);
            createReceiveMethod(ctClass);
            return ctClass.toClass();
        }
        catch (NotFoundException e)
        {
            throw new MessagingException("Could not create Subscriber", e);
        }
        catch (CannotCompileException e)
        {
            throw new MessagingException("Could not create Subscriber", e);
        }
    }

    private CtClass createSubscriberClass(final String subscriberClassname, final ClassPool classPool, final String descriptorClassname,
                                          final String invokerInterfaceName, final CtClass invokerInterfaceClass)
            throws NotFoundException, CannotCompileException
    {
        final CtClass ctClass = classPool.makeClass(subscriberClassname);
        ctClass.addInterface(classPool.get("com.epickrram.freewheel.messaging.Receiver"));
        final CtMethod invocationMethod = CtMethod.make("public void invoke(" + descriptorClassname + " implementation, " +
                "DecoderStream decoderStream);", invokerInterfaceClass);
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
                append("public void invoke(").append(SubscriberFactory.classDefinitionToClassname(descriptor)).
                append(" implementation, DecoderStream decoderStream) {");
        final Class<?>[] parameterTypes = method.getParameterTypes();
        char parameterId = 'a';
        for (final Class<?> parameterType : parameterTypes)
        {
            if (parameterType.equals(int.class))
            {
                methodSource.append("final int ").append((parameterId++)).append(" = decoderStream.readInt();\n");
            }
            else if (parameterType.equals(long.class))
            {
                methodSource.append("final long ").append((parameterId++)).append(" = decoderStream.readLong();\n");
            }
            else if (parameterType.equals(byte.class))
            {
                methodSource.append("final byte ").append((parameterId++)).append(" = decoderStream.readByte();\n");
            }
            else if (parameterType.equals(String.class))
            {
                methodSource.append("final String ").append((parameterId++)).append(" = decoderStream.readString();\n");
            }
            else
            {
                methodSource.append("final ").append(parameterType.getName()).append(" ").
                        append((parameterId++)).append(" = (").append(parameterType.getName()).
                        append(") decoderStream.readObject();\n");
            }
        }
        methodSource.append("implementation.").append(method.getName()).append("(");
        parameterId = 'a';
        for (int i = 0; i < parameterTypes.length; i++)
        {
            if (i != 0)
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
        final String invocation = "public void onMessage(int topicId, DecoderStream decoderStream) {" +
                " int methodIndex = decoderStream.readByte();" +
                " invokers[methodIndex].invoke(implementation, decoderStream);" +
                "}";
        ctClass.addMethod(CtMethod.make(invocation, ctClass));
    }


}
