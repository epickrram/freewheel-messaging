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
package com.epickrram.freewheel.protocol;

public final class TranslatorHelper
{
    public <T> Translator<T> createTranslator(final Class<T> cls)
    {
        final Class<?>[] declaredClasses = cls.getDeclaredClasses();
        for (Class<?> declaredClass : declaredClasses)
        {
            if(AbstractTranslator.class.equals(declaredClass.getSuperclass()))
            {
                return instantiateTranslator(declaredClass);
            }
        }
        throw new TranslatorException("No public static final inner class extending AbstractTranslator for class: " + cls);
    }

    public <T> int getCodeBookId(final Class<T> cls)
    {
        final Translatable translatable = cls.getAnnotation(Translatable.class);
        if(translatable == null)
        {
            throw new TranslatorException("Class " + cls + " is not annotated with @Translatable");
        }
        return translatable.codeBookId();
    }

    @SuppressWarnings({"unchecked"})
    private <T> Translator<T> instantiateTranslator(final Class<?> declaredClass)
    {
        try
        {
            return (Translator<T>) declaredClass.newInstance();
        }
        catch (InstantiationException e)
        {
            throw new TranslatorException("Unable to instantiate " + declaredClass + " does it have a no-arg constructor?");
        }
        catch (IllegalAccessException e)
        {
            throw new TranslatorException("Unable to instantiate " + declaredClass + " does it have a no-arg constructor?");
        }
    }
}