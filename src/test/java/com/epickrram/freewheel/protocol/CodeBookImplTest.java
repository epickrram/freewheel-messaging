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