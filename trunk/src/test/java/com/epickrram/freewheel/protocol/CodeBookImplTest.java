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

import com.epickrram.freewheel.TestObject;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;

public final class CodeBookImplTest
{
    private CodeBookImpl codeBook;

    @Test
    public void shouldBlowUpIfRegisteredObjectHasCodeBookIdOfLessThan1025() throws Exception
    {
        final CodeBookImpl.CodeBookRegistryImpl codeBookRegistry = new CodeBookImpl.CodeBookRegistryImpl(codeBook);
        for(int i = 0; i < 1025; i++)
        {
            try
            {
                codeBookRegistry.registerTranslator(i, null, null);
                fail("Should have thrown an exception for codeBookId " + i);
            }
            catch(RuntimeException e)
            {
                // ignore
            }
        }
        codeBookRegistry.registerTranslator(1025, new TestObject.Translator(), TestObject.class);
    }

    @Before
    public void setUp() throws Exception
    {
        codeBook = new CodeBookImpl();
    }
}