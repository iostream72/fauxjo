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
    private ThreadLocal<HashMap<String, PreparedStatement>> threadPreparedStatements;

    // ============================================================
    // Constructors
    // ============================================================

    public PooledConnectionSupplier()
    {
        threadConnection = new ThreadLocal<Connection>();
        threadPreparedStatements = new ThreadLocal<HashMap<String, PreparedStatement>>();
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
        HashMap<String, PreparedStatement> map = threadPreparedStatements.get();
        if ( map != null )
        {
            for ( PreparedStatement statement : map.values() )
            {
                if ( statement == null )
                {
                    continue;
                }

                if ( !statement.isClosed() )
                {
                    statement.close();
                }
            }

            threadPreparedStatements.remove();
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

    @Override
    public PreparedStatement prepareStatement( String sql )
        throws SQLException
    {
        HashMap<String, PreparedStatement> map = threadPreparedStatements.get();
        if ( map == null )
        {
            map = new HashMap<String, PreparedStatement>();
            threadPreparedStatements.set( map );
        }

        PreparedStatement statement = map.get( sql );

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

            map.put( sql, statement );
        }

        return statement;
    }
}
