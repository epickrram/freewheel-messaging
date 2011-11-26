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
package com.epickrram.freewheel;

import com.epickrram.freewheel.io.DecoderStream;
import com.epickrram.freewheel.io.EncoderStream;
import com.epickrram.freewheel.protocol.AbstractTranslator;
import com.epickrram.freewheel.protocol.Translatable;

import java.io.IOException;

@Translatable(codeBookId = 9999)
public final class TestObject
{
    private final int foo;
    private final String bar;

    TestObject(final int foo, final String bar)
    {
        this.foo = foo;
        this.bar = bar;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final TestObject that = (TestObject) o;

        if (foo != that.foo) return false;
        if (bar != null ? !bar.equals(that.bar) : that.bar != null) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = foo;
        result = 31 * result + (bar != null ? bar.hashCode() : 0);
        return result;
    }

    @Translatable(codeBookId = 5555)
    public static final class Translator extends AbstractTranslator<TestObject>
    {
        @Override
        public void doEncode(final TestObject encodable, final EncoderStream encoderStream) throws IOException
        {
            encoderStream.writeInt(encodable.foo);
            encoderStream.writeString(encodable.bar);
        }

        @Override
        public TestObject doDecode(final DecoderStream decoderStream) throws IOException
        {
            final int foo = decoderStream.readInt();
            final String bar = decoderStream.readString();

            return new TestObject(foo, bar);
        }
    }
}
