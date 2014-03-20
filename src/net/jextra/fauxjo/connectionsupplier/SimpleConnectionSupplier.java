//
// SimpleConnectionSupplier
//
// Copyright (C) jextra.net.
//
//  This file is part of the Fauxjo Library.
//
//  The Fauxjo Library is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation; either
//  version 2.1 of the License, or (at your option) any later version.
//
//  The Fauxjo Library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//  Lesser General Public License for more details.
//
//  You should have received a copy of the GNU Lesser General Public
//  License along with the Fauxjo Library; if not, write to the Free
//  Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
//  02111-1307 USA.
//

package net.jextra.fauxjo.connectionsupplier;

import java.sql.*;
import java.util.*;
import javax.sql.*;

/**
 * <p>
 * Essentially a pass-through {@link ConnectionSupplier} for a single {@link Connection} object.
 * </p>
 * <p>
 * This is useful for thick Swing applications where Swing threads should get same connection as the main thread.
 * </p>
 */
public class SimpleConnectionSupplier implements ConnectionSupplier
{
    // ============================================================
    // Fields
    // ============================================================

    private Connection connection;
    private DataSource dataSource;
    private HashMap<String, PreparedStatement> preparedStatements;

    // ============================================================
    // Constructors
    // ============================================================

    public SimpleConnectionSupplier()
    {
        preparedStatements = new HashMap<String, PreparedStatement>();
    }

    public SimpleConnectionSupplier( Connection conn )
    {
        this();

        setConnection( conn );
    }

    public SimpleConnectionSupplier( DataSource ds )
        throws SQLException
    {
        this();

        setDataSource( ds );
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public void setDataSource( DataSource ds )
        throws SQLException
    {
        dataSource = ds;
        connection = dataSource.getConnection();
    }

    public void setConnection( Connection conn )
    {
        connection = conn;
        dataSource = null;
    }

    @Override
    public Connection getConnection()
        throws SQLException
    {
        validateConnection();

        return connection;
    }

    @Override
    public boolean closeConnection()
        throws SQLException
    {
        for ( PreparedStatement statement : preparedStatements.values() )
        {
            if ( !statement.isClosed() )
            {
                statement.close();
            }
        }
        preparedStatements.clear();

        if ( connection == null )
        {
            return false;
        }

        if ( connection != null )
        {
            connection.close();
        }

        connection = null;

        return true;
    }

    @Override
    public PreparedStatement prepareStatement( String sql )
        throws SQLException
    {
        validateConnection();

        PreparedStatement statement = preparedStatements.get( sql );
        if ( statement == null || statement.isClosed() )
        {
            if ( SQLInspector.isInsertStatement( sql ) )
            {
                statement = getConnection().prepareStatement( sql, Statement.RETURN_GENERATED_KEYS );
            }
            else
            {
                statement = getConnection().prepareStatement( sql );
            }
            preparedStatements.put( sql, statement );
        }

        return statement;
    }

    public void validateConnection()
        throws SQLException
    {
        if ( connection != null && !connection.isValid( 1000 ) )
        {
            closeConnection();

            // If there is a dataSource, go get a new connection.
            if ( dataSource != null )
            {
                connection = dataSource.getConnection();
            }
        }
    }
}
