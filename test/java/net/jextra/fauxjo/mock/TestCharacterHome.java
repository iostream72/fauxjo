package net.jextra.fauxjo.mock;

import net.jextra.fauxjo.Home;
import net.jextra.fauxjo.Schema;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TestCharacterHome extends Home<TestCharacter>
{

    public static final String TABLE_NAME = "character";

    public TestCharacterHome( Schema schema )
    {
        super( schema, TestCharacter.class, TABLE_NAME );
    }

    public TestCharacter findById( Long id )
        throws SQLException
    {
        PreparedStatement pstmt = prepareStatement( buildBasicSelect( "where characterId = ?" ) );

        pstmt.setLong( 1, id );
        return getUnique( pstmt.executeQuery() );
    }

}
