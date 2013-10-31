package net.jextra.fauxjo.coercer;

import net.jextra.fauxjo.FauxjoException;
import org.junit.Test;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class StringCoercerTest
{

    public enum AvatarName
    {
        Aang,
        Katara,
        Sokka,
        Zuko,
        Toph,
        Iroh
    }

    @Test
    public void testCoerceEnum()
        throws FauxjoException
    {
        StringCoercer coercer = new StringCoercer();
        Object retVal = coercer.coerce( "Aang", AvatarName.class );
        assertNotNull( retVal );
        assertTrue( retVal instanceof AvatarName );
        AvatarName name = (AvatarName) retVal;
        assertEquals( AvatarName.Aang, name );
    }

    @Test
    public void testCoerceBoolean()
        throws FauxjoException
    {
        StringCoercer coercer = new StringCoercer();
        final Object val = coercer.coerce( "true", Boolean.class );
        assertEquals( val, Boolean.TRUE );
    }

    @Test
    public void testCoerceInt()
        throws FauxjoException
    {
        StringCoercer coercer = new StringCoercer();
        assertEquals( 123, coercer.coerce( "123", Integer.class ) );
    }

}
