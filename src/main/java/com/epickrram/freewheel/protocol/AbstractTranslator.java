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

import com.epickrram.freewheel.io.DecoderStream;
import com.epickrram.freewheel.io.EncoderStream;

import java.io.IOException;

public abstract class AbstractTranslator<T> implements Translator<T>
{
    @Override
    public void encode(final T encodable, final EncoderStream encoderStream)
    {
        try
        {
            doEncode(encodable, encoderStream);
        }
        catch (IOException e)
        {
            throw new TranslatorException("Exception occurred while encoding " + encodable, e);
        }
    }

    @Override
    public T decode(final DecoderStream decoderStream)
    {
        try
        {
            return doDecode(decoderStream);
        }
        catch (IOException e)
        {
            throw new TranslatorException("Exception occurred while decoding", e);
        }
    }
    
    protected abstract void doEncode(final T encodable, final EncoderStream encoderStream) throws IOException;
    protected abstract T doDecode(final DecoderStream decoderStream) throws IOException;
}