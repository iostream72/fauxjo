//
// Home
//
// Copyright (C) 2007 Brian Stevens.
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

package net.fauxjo;

import java.sql.*;
import java.util.*;
import net.fauxjo.coercer.*;

public class Home < T extends Fauxjo > 
{
    // ============================================================
    // Fields
    // ============================================================

    private Schema _schema;
    private boolean _overrideSchemaName;
    private String _schemaName;
    private Class<T> _beanClass;
    private String _tableName;
    private SQLProcessor<T> _sqlProcessor;
    private HashMap<String,String> _sqls;
    private WeakHashMap<Thread,HashMap<String,PreparedStatement>> _preparedStatements;

    // ============================================================
    // Constructors
    // ============================================================

    public Home( Schema schema, Class<T> beanClass, String tableName )
        throws SQLException
    {
        _schema = schema;
        _beanClass = beanClass;
        _tableName = tableName;
        _sqlProcessor = new SQLProcessor<T>( this, _beanClass );
        _sqls = new HashMap<String,String>();
        _preparedStatements = new WeakHashMap<Thread,HashMap<String,PreparedStatement>>();
        _overrideSchemaName = false;
        _schemaName = null;
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // protected
    // ----------

    public void registerPreparedStatement( String statementId, String sql )
    {
        _sqls.put( statementId, sql );
    }

    public PreparedStatement getPreparedStatement( String statementId )
        throws SQLException
    {
        HashMap<String,PreparedStatement> map = _preparedStatements.get( Thread.currentThread() );
        if ( map == null )
        {
            map = new HashMap<String,PreparedStatement>();
            _preparedStatements.put( Thread.currentThread(), map );
        }

        PreparedStatement statement = map.get( statementId );
        if ( statement == null || statement.isClosed() || statement.getConnection().isClosed() )
        {
            statement = getConnection().prepareStatement( _sqls.get( statementId ) );
            map.put( statementId, statement );
        }

        return statement;
    }

    public String getTableName()
    {
        return _tableName;
    }

    public void setTableName( String tableName )
    {
        _tableName = tableName;
    }

    public String getQualifiedTableName()
    {
        return getQualifiedName( getTableName() );
    }

    public String getQualifiedName( String name )
    {
        if ( _overrideSchemaName )
        {
            return _schemaName == null ? name : _schemaName + "." + name;
        }

        return _schema.getQualifiedName( name );
    }

    public String getSchemaName()
    {
        return _overrideSchemaName ? _schemaName : _schema.getSchemaName();
    }

    public void setSchemaName( String schemaName ) throws SQLException
    {
        _overrideSchemaName = true;
        _schemaName = schemaName;
        
        // Have to recreate processor because schemaName change.
        _sqlProcessor = new SQLProcessor<T>( this, _beanClass );
    }
    
    public void clearSchemaNameOverride() throws SQLException
    {
        _overrideSchemaName = false;
        
        // Have to recreate processor because schemaName change.
        _sqlProcessor = new SQLProcessor<T>( this, _beanClass );
    }

    public long getNextKey( String sequenceName )
        throws SQLException
    {
        return _sqlProcessor.getNextKey( sequenceName );
    }

    public boolean insert( T bean )
        throws SQLException
    {
        return _sqlProcessor.insert( bean );
    }

    public int update( T bean )
        throws SQLException
    {
        return _sqlProcessor.update( bean );
    }

    public boolean delete( T bean )
        throws SQLException
    {
        return _sqlProcessor.delete( bean );
    }

    public boolean save( T bean )
        throws SQLException
    {
        // If has empty PK, assumed to be new.
        if ( bean.hasEmptyPrimaryKey( _schema ) )
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

    // ----------
    // protected
    // ----------

    protected Schema getSchema()
    {
        return _schema;
    }

    protected Connection getConnection()
        throws SQLException
    {
        return _schema.getConnection();
    }

    protected T getOne( ResultSet rs )
        throws SQLException
    {
        return _sqlProcessor.getOne( rs );
    }

    protected T getOnlyOne( ResultSet rs )
        throws SQLException
    {
        return _sqlProcessor.getOnlyOne( rs );
    }

    protected T getFirst( ResultSet rs )
        throws SQLException
    {
        return _sqlProcessor.getFirst( rs );
    }

    protected T getOnlyFirst( ResultSet rs )
        throws SQLException
    {
        return _sqlProcessor.getOnlyFirst( rs );
    }

    protected List<T> getList( ResultSet rs )
        throws SQLException
    {
        return _sqlProcessor.getList( rs );
    }

    protected List<T> getList( ResultSet rs, int maxNumRows )
        throws SQLException
    {
        return _sqlProcessor.getList( rs, maxNumRows );
    }

    protected ResultSetIterator<T> getIterator( ResultSet rs )
        throws SQLException
    {
        return _sqlProcessor.getIterator( rs );
    }

    protected String buildBasicSelect( String clause )
    {
        String c = "";
        if ( clause != null && !clause.trim().isEmpty() )
        {
            c = clause;
        }
        return "select * from " + getQualifiedTableName() + " " + c;
    }

    protected < S > void mapCoercer( Class<S> coercerClass, TypeCoercer<S> coercer )
    {
        _sqlProcessor.getCoercer().putCoercer( coercerClass, coercer );
    }

    protected < S > TypeCoercer<S> getCoercer( Class<S> coercerClass )
    {
        return _sqlProcessor.getCoercer().getCoercer( coercerClass );
    }
}

