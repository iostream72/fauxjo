//
// PooledConnectionSupplier
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
 * Stored an independent {@link Connection} for each {@link Thread}. Useful for web-development which uses caches.
 * </p>
 * <p>
 * Example setup:
 * 
 * <pre>
 * InitialContext cxt = new InitialContext();
 * DataSource ds = (DataSource) cxt.lookup( &quot;java:/comp/env/jdbc/xyz/abc&quot; );
 * connectionSupplier = new PooledConnectionSupplier( ds );
 * </pre>
 * 
 * Example cleanup:
 * 
 * <pre>
 * connectionProvider.clear();
 * </pre>
 * 
 * </p>
 */
public class PooledConnectionSupplier implements ConnectionSupplier
{
    // ============================================================
    // Fields
    // ============================================================

    private DataSource dataSource;
    private ThreadLocal<Connection> threadConnection;
    private HashMap<String, ThreadLocal<PreparedStatement>> preparedStatements;

    // ============================================================
    // Constructors
    // ============================================================

    public PooledConnectionSupplier()
    {
        threadConnection = new ThreadLocal<Connection>();
        preparedStatements = new HashMap<String, ThreadLocal<PreparedStatement>>();
    }

    public PooledConnectionSupplier( DataSource ds )
    {
        this();
        dataSource = ds;
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    @Override
    public Connection getConnection()
        throws SQLException
    {
        Connection cnx = null;
        if ( threadConnection.get() == null )
        {
            threadConnection.set( dataSource.getConnection() );
        }

        cnx = threadConnection.get();

        return cnx;
    }

    @Override
    public boolean closeConnection()
        throws SQLException
    {
        for ( ThreadLocal<PreparedStatement> statement : preparedStatements.values() )
        {
            if ( statement.get() == null )
            {
                continue;
            }

            if ( !statement.get().isClosed() )
            {
                statement.get().close();
            }

            statement.remove();
        }

        Connection cnx = null;
        if ( threadConnection.get() == null )
        {
            return false;
        }

        cnx = threadConnection.get();
        cnx.close();
        threadConnection.remove();

        return true;
    }

    public void setDataSource( DataSource dataSource )
    {
        this.dataSource = dataSource;
    }

    // public void clear()
    // {
    // try
    // {
    // if ( closeConnectionOnClear && threadConnection.get() != null )
    // {
    // threadConnection.get().close();
    // }
    // }
    // catch ( SQLException ignore )
    // {
    // }
    // finally
    // {
    // threadConnection.set( null );
    // }
    // }

    @Override
    public PreparedStatement prepareStatement( String sql )
        throws SQLException
    {
        ThreadLocal<PreparedStatement> statement = preparedStatements.get( sql );

        if ( statement == null || statement.get() == null || statement.get().isClosed() )
        {
            PreparedStatement s = getConnection().prepareStatement( sql );
            if ( statement == null )
            {
                statement = new ThreadLocal<PreparedStatement>();
                preparedStatements.put( sql, statement );
            }
            statement.set( s );
        }

        return statement.get();
    }
}
