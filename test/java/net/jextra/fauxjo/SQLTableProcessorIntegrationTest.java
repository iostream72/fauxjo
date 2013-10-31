package net.jextra.fauxjo;

import net.jextra.fauxjo.mock.TestCharacter;
import net.jextra.fauxjo.mock.TestCharacterHome;
import net.jextra.fauxjo.mock.TestSchema;
import org.junit.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SQLTableProcessorIntegrationTest
{

    private TestSchema testSchema;
    private static Connection conn;

    @BeforeClass
    public static void init()
        throws Exception
    {
        conn = SQLTestHelper.createConnection();
        SQLTestHelper.executeSQL( conn, "Create schema %s", TestSchema.SCHEMA_NAME );
        SQLTestHelper.executeSQL( conn, "Create table %s.%s (" + "   characterId bigserial primary key," + "   name varchar not null,"
            + "   email varchar," + "   dateCreated timestamptz not null default now()" + ")", TestSchema.SCHEMA_NAME, TestCharacterHome.TABLE_NAME );
    }

    @Before
    public void setUp()
        throws Exception
    {
        testSchema = new TestSchema();
        SQLTestHelper.initSchema( testSchema, conn );
    }

    @After
    public void tearDown()
        throws Exception
    {

    }

    @AfterClass
    public static void destroy()
        throws SQLException
    {
        if ( conn != null )
        {
            try
            {
                SQLTestHelper.executeSQL( conn, "drop schema %s cascade", TestSchema.SCHEMA_NAME );
                conn.close();
            }
            catch ( SQLException e )
            {
                // don't care
            }
        }
    }

    @Test
    public void testInsertWithDefaults()
        throws SQLException
    {
        SQLTableProcessor<TestCharacter> sqlTableProcessor = new SQLTableProcessor<TestCharacter>( testSchema, TestCharacterHome.TABLE_NAME,
            TestCharacter.class );

        TestCharacter char1 = new TestCharacter( "Aang", "aang@airnomads.org" );
        sqlTableProcessor.insert( char1 );

        assertNotNull( "id was null", char1.getId() );
        assertNotNull( "dateCreated was null", char1.getDateCreated() );

        TestCharacter actualChar = testSchema.getHomeByClass( TestCharacterHome.class ).findById( char1.getId() );
        assertNotNull( actualChar );
        assertEquals( char1.getName(), actualChar.getName() );
        assertEquals( char1.getDateCreated(), actualChar.getDateCreated() );
    }

    @Test
    public void testOverridingDefault()
        throws SQLException
    {
        SQLTableProcessor<TestCharacter> sqlTableProcessor = new SQLTableProcessor<TestCharacter>( testSchema, TestCharacterHome.TABLE_NAME,
            TestCharacter.class );
        TestCharacter char1 = new TestCharacter( "Aang", "aang@airnomads.org" );
        sqlTableProcessor.insert( char1 );

        assertNotNull( "id was null", char1.getId() );
        assertNotNull( "dateCreated was null", char1.getDateCreated() );

        TestCharacter char2 = new TestCharacter( "Zuko", "zuko@firenation.com" );
        char2.setDateCreated( new Date() );
        sqlTableProcessor.insert( char2 );

        TestCharacter actualChar = testSchema.getHomeByClass( TestCharacterHome.class ).findById( char2.getId() );
        assertNotNull( actualChar );
        assertEquals( char2.getName(), actualChar.getName() );
        assertEquals( char2.getDateCreated(), actualChar.getDateCreated() );
    }
}
