package com.epickrram.freewheel.protocol.transcoder;

import com.epickrram.freewheel.io.DecoderStream;
import com.epickrram.freewheel.io.EncoderStream;
import com.epickrram.freewheel.protocol.Transcoder;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public final class ListTranscoder<ListType extends List> implements Transcoder<ListType>
{
    private final Class<? extends List> concreteListType;
    private final Constructor<? extends List> constructorWithSize;
    private final Constructor<? extends List> constructor;

    public ListTranscoder(final Class<? extends List> concreteListType)
    {
        this.concreteListType = concreteListType;
        constructorWithSize = getConstructorWithSize(concreteListType);
        constructor = getDefaultConstructor(concreteListType);
        validateConstructors(concreteListType);
    }

    @Override
    public void encode(final ListType encodable, final EncoderStream encoderStream) throws IOException
    {
        encoderStream.writeBoolean(encodable == null);
        if(encodable != null)
        {
            encoderStream.writeInt(encodable.size());
            for(int i = 0, n = encodable.size(); i < n; i++)
            {
                encoderStream.writeObject(encodable.get(i));
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public ListType decode(final DecoderStream decoderStream) throws IOException
    {
        final boolean isNull = decoderStream.readBoolean();
        if(isNull)
        {
            return null;
        }
        final int listSize = decoderStream.readInt();
        final List list = createList(listSize);

        for(int i = 0; i < listSize; i++)
        {
            list.add(decoderStream.readObject());
        }

        return (ListType) list;
    }

    @SuppressWarnings({"unchecked"})
    private List createList(final int listSize)
    {
        try
        {
            return (constructorWithSize != null) ?
                    constructorWithSize.newInstance(listSize) :
                    constructor.newInstance();
        }
        catch (InstantiationException e)
        {
            throw new IllegalArgumentException("Cannot construct new List type: " + concreteListType, e);
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalArgumentException("Cannot construct new List type: " + concreteListType, e);
        }
        catch (InvocationTargetException e)
        {
            throw new IllegalArgumentException("Cannot construct new List type: " + concreteListType, e);
        }
    }

    private void validateConstructors(final Class<? extends List> concreteListType)
    {
        if(constructor == null && constructorWithSize == null)
        {
            throw new IllegalArgumentException("Could not find suitable constructor for class: " + concreteListType);
        }
    }

    private Constructor<? extends List> getConstructorWithSize(final Class<? extends List> concreteListType)
    {
        try
        {
            return concreteListType.getConstructor(int.class);
        }
        catch (NoSuchMethodException e)
        {
            return null;
        }
    }

    private Constructor<? extends List> getDefaultConstructor(final Class<? extends List> concreteListType)
    {
        try
        {
            return concreteListType.getConstructor();
        }
        catch (NoSuchMethodException e)
        {
            return null;
        }
    }
}