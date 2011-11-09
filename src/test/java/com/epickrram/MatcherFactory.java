package com.epickrram;

import com.epickrram.freewheel.stream.ByteOutputBuffer;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public final class MatcherFactory
{
    private MatcherFactory() {}

    public static Matcher<ByteArrayOutputStream> aByteOutputBufferMatching(final ByteArrayOutputStream expected)
    {
        return new TypeSafeMatcher<ByteArrayOutputStream>()
        {
            private ByteArrayOutputStream actual;

            @Override
            public boolean matchesSafely(final ByteArrayOutputStream actual)
            {
                this.actual = actual;
                return Arrays.equals(expected.toByteArray(), actual.toByteArray());
            }

            public void describeTo(final Description description)
            {
                description.appendText("Expected buffer " + Arrays.toString(expected.toByteArray()));
                description.appendText(", got buffer " + Arrays.toString(actual.toByteArray()));
            }
        };
    }

    public static Matcher<ByteOutputBuffer> aByteOutputBufferMatching(final ByteOutputBuffer expected)
    {
        return new TypeSafeMatcher<ByteOutputBuffer>()
        {
            private ByteOutputBuffer actual;

            @Override
            public boolean matchesSafely(final ByteOutputBuffer actual)
            {
                this.actual = actual;
                if(actual.position() == expected.position() && actual.count() == expected.count())
                {
                    byte[] actualBytes = new byte[actual.count() - actual.position()];
                    actual.copyInto(actualBytes, 0, actualBytes.length);

                    byte[] expectedBytes = new byte[expected.count() - expected.position()];
                    expected.copyInto(expectedBytes, 0, expectedBytes.length);

                    return Arrays.equals(expectedBytes, actualBytes);
                }
                return false;
            }

            public void describeTo(final Description description)
            {
                description.appendText("Expected buffer [" + expected.position() + ", " + expected.count() + "]:" + Arrays.toString(expected.getBackingArray()));
                description.appendText(", got buffer [" + actual.position() + ", " + actual.count() + "]:" + Arrays.toString(actual.getBackingArray()));
            }
        };
    }
}
