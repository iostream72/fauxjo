package net.jextra.fauxjo;

import net.jextra.fauxjo.connectionsupplier.SimpleConnectionSupplier;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

public final class SQLTestHelper
{
    public static Connection createConnection()
        throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException, SQLException
    {
        Properties props = new Properties();
        InputStream in = SQLTestHelper.class.getResourceAsStream( "/integration.properties" );
        props.load( in );
        in.close();

        Driver driver = (Driver) Class.forName( props.getProperty( "integration.db.driver" ) ).newInstance();
        Properties dbProps = new Properties();
        dbProps.setProperty( "user", props.getProperty( "integration.db.user" ) );
        dbProps.setProperty( "password", props.getProperty( "integration.db.password" ) );
        Connection conn = driver.connect( props.getProperty( "integration.db.url" ), dbProps );
        return conn;
    }

    public static void initSchema( ConnectionSupplierSchema schema, Connection conn )
        throws SQLException
    {
        SimpleConnectionSupplier connSupplier = new SimpleConnectionSupplier( conn );
        schema.setSchemaConnectionSupplier( connSupplier );
    }

    public static Connection initSchema( ConnectionSupplierSchema schema )
        throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
    {
        Connection conn = createConnection();
        initSchema( schema, conn );
        return conn;
    }

    public static void executeSQL( Connection conn, String sql, Object ... args )
        throws SQLException
    {
        if ( args.length > 0 )
        {
            sql = String.format( sql, args );
        }
        PreparedStatement pstmt = conn.prepareStatement( sql );
        pstmt.execute();
        pstmt.close();
    }
}
