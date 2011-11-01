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