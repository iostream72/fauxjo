//
// Home
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

package net.jextra.fauxjo;

import java.sql.*;
import java.util.*;

/**
 * <p>
 * Default implementation of a Home object that represents a single table in the database.
 * </p>
 */
public class Home<T extends Fauxjo>
{
    // ============================================================
    // Fields
    // ============================================================

    private SQLProcessor<T> sqlProcessor;

    // ============================================================
    // Constructors
    // ============================================================

    public Home( Schema schema, Class<T> beanClass, String tableName )
    {
        sqlProcessor = new SQLTableProcessor<T>( schema, tableName, beanClass );
    }

    public Home( Schema schema, Class<T> beanClass, SQLProcessor<T> sqlProcessor )
    {
        this.sqlProcessor = sqlProcessor;
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // protected
    // ----------

    public PreparedStatement prepareStatement( String sql )
        throws SQLException
    {
        return getSchema().prepareStatement( sql );
    }

    @Deprecated
    public String getTableName()
    {
        if ( sqlProcessor instanceof SQLTableProcessor<?> )
        {
            return ( (SQLTableProcessor<?>) sqlProcessor ).getTableName();
        }

        return null;
    }

    @Deprecated
    public String getQualifiedTableName()
    {
        return getQualifiedName( getTableName() );
    }

    public String getQualifiedName( String name )
    {
        return getSchema().getQualifiedName( name );
    }

    public String getSchemaName()
    {
        return getSchema().getSchemaName();
    }

    public boolean insert( T bean )
        throws SQLException
    {
        return sqlProcessor.insert( bean );
    }

    public int update( T bean )
        throws SQLException
    {
        return sqlProcessor.update( bean );
    }

    public boolean delete( T bean )
        throws SQLException
    {
        return sqlProcessor.delete( bean );
    }

    public boolean save( T bean )
        throws SQLException
    {
        // If has empty PK, assumed to be new.
        if ( !bean.isInDatabase() )
        {
            return insert( bean );
        }
        else
        {
            // Attempt to do an update
            int numRowsUpdated = update( bean );

            // If no rows were actually updated, assume must actually be new.
            if ( numRowsUpdated == 0 )
            {
                return insert( bean );
            }
        }

        return true;
    }

    public T getFirst( ResultSet rs )
        throws SQLException
    {
        return sqlProcessor.getFirst( rs );
    }

    public T getFirst( ResultSet rs, boolean errorIfEmpty )
        throws SQLException
    {
        return sqlProcessor.getFirst( rs, errorIfEmpty );
    }

    public T getUnique( ResultSet rs )
        throws SQLException
    {
        return sqlProcessor.getUnique( rs );
    }

    public T getUnique( ResultSet rs, boolean errorIfEmpty )
        throws SQLException
    {
        return sqlProcessor.getUnique( rs, errorIfEmpty );
    }

    public List<T> getList( ResultSet rs )
        throws SQLException
    {
        return sqlProcessor.getList( rs );
    }

    public List<T> getList( ResultSet rs, int maxNumRows )
        throws SQLException
    {
        return sqlProcessor.getList( rs, maxNumRows );
    }

    public Set<T> getSet( ResultSet rs )
        throws SQLException
    {
        return sqlProcessor.getSet( rs );
    }

    public Set<T> getSet( ResultSet rs, int maxNumRows )
        throws SQLException
    {
        return sqlProcessor.getSet( rs, maxNumRows );
    }

    public ResultSetIterator<T> getIterator( ResultSet rs )
        throws SQLException
    {
        return sqlProcessor.getIterator( rs );
    }

    public String buildBasicSelect( String clause )
    {
        return sqlProcessor.buildBasicSelect( clause );
    }

    // ----------
    // protected
    // ----------

    protected Schema getSchema()
    {
        return sqlProcessor.getSchema();
    }

    protected Connection getConnection()
        throws SQLException
    {
        return getSchema().getConnection();
    }

    protected SQLProcessor<T> getSQLProcessor()
    {
        return sqlProcessor;
    }

    @Deprecated
    protected SQLProcessor<T> getSQLTableProcessor()
    {
        return sqlProcessor;
    }
}
