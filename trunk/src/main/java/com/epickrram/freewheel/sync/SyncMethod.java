package com.epickrram.freewheel.sync;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface SyncMethod
{
    String requestMethod();
    String responseMethod();
    long timeoutMilliseconds() default 10000L;
    int requestParameterIdentifierIndex() default 0;
    int responseParameterIdentifierIndex() default 0;
    int responseParameterValueIndex() default 1;
}