package com.epickrram.freewheel.protocol;

public interface CodeBookRegistry
{
    <T> void registerTranslatable(final Class<T> cls);
    <T> void registerTranslator(final int codeBookId, final Translator<T> translator, final Class<T> cls);
}