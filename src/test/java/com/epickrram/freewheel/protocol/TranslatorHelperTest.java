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
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public final class TranslatorHelperTest
{
    private static final int CODE_BOOK_ID = 1234;
    private TranslatorHelper translatorHelper;

    @Before
    public void setUp() throws Exception
    {
        translatorHelper = new TranslatorHelper();
    }

    @Test
    public void shouldRetrieveCodeBookId() throws Exception
    {
        assertThat(translatorHelper.getCodeBookId(TranscodableTest.class), is(CODE_BOOK_ID));
    }

    @Test
    public void shouldCreateTranslatorByClassname() throws Exception
    {
        final Class<? extends Translator> cls = translatorHelper.createTranslator(TranscodableTest.class).getClass();
        assertThat(cls.getName(), is(TranscodableTest.Translator.class.getName()));
    }

    @Translatable(codeBookId = CODE_BOOK_ID)
    private static final class TranscodableTest
    {
        public static final class Translator extends AbstractTranslator<TranscodableTest>
        {
            @Override
            protected void doEncode(final TranscodableTest encodable, final EncoderStream encoderStream) throws IOException
            {
            }

            @Override
            protected TranscodableTest doDecode(final DecoderStream decoderStream) throws IOException
            {
                return null;
            }
        }
    }
}