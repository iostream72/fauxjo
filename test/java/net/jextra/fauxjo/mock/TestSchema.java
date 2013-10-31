package net.jextra.fauxjo.mock;

import net.jextra.fauxjo.ConnectionSupplierSchema;

/**
 * @author Craig Moon
 *         Date: 5/23/13
 */
public class TestSchema extends ConnectionSupplierSchema
{

    public static final String SCHEMA_NAME = "test";

    public TestSchema()
    {
        super( SCHEMA_NAME );
    }

    @Override
    protected void initHomeObjects()
    {
        addHome( TestCharacterHome.class, new TestCharacterHome( this ) );
    }
}
