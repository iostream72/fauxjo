//
// ThreadSafeConnectionSupplier
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

public class ThreadSafeConnectionSupplier implements ConnectionSupplier
{
    // ============================================================
    // Fields
    // ============================================================

    private ConnectionBuilder connectionBuilder;
    private ThreadLocal<Connection> threadConnection;
    private DataSource dataSource;
    private HashMap<String, ThreadLocal<PreparedStatement>> preparedStatements;

    // ============================================================
    // Constructors
    // ============================================================

    public ThreadSafeConnectionSupplier()
    {
        threadConnection = new ThreadLocal<Connection>();
        preparedStatements = new HashMap<String, ThreadLocal<PreparedStatement>>();
    }

    public ThreadSafeConnectionSupplier( ConnectionBuilder builder )
    {
        this();

        connectionBuilder = builder;
    }

    public ThreadSafeConnectionSupplier( DataSource dataSource )
    {
        this();

        this.dataSource = dataSource;
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
            if ( connectionBuilder != null )
            {
                threadConnection.set( connectionBuilder.getConnection() );
            }
            else
            {
                threadConnection.set( dataSource.getConnection() );
            }
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

    public void setConnectionBuilder( ConnectionBuilder builder )
    {
        connectionBuilder = builder;
    }

    @Override
    public PreparedStatement prepareStatement( String sql )
        throws SQLException
    {
        ThreadLocal<PreparedStatement> statement = preparedStatements.get( sql );

        if ( statement == null || statement.get() == null || statement.get().isClosed() )
        {
            PreparedStatement s;
            if ( SQLInspector.isInsertStatement( sql ) )
            {
                s = getConnection().prepareStatement( sql, Statement.RETURN_GENERATED_KEYS );
            }
            else
            {
                s = getConnection().prepareStatement( sql );
            }
            if ( statement == null )
            {
                statement = new ThreadLocal<PreparedStatement>();
                preparedStatements.put( sql, statement );
            }
            statement.set( s );
        }

        return statement.get();
    }

    // ============================================================
    // Inner Classes
    // ============================================================

    public interface ConnectionBuilder
    {
        Connection getConnection()
            throws SQLException;
    }
}
