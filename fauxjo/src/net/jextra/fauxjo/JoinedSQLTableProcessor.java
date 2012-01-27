//
// JoinedSQLTableProcessor
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
 * SQLProcessor implementation that handles multiple tables representing a single {@link Fauxjo} class. This typically
 * will be used when you have a parent table which represents a base class with multiple child tables representing
 * the subclasses. Note that this implementation uses the SQLTableProcessor underneath to do single table operations.
 *
 * @param <T> The {@link Fauxjo} bean class represented by the underlying tables.
 */
public class JoinedSQLTableProcessor<T extends Fauxjo> extends AbstractSQLProcessor<T>
{
    // ============================================================
    // Fields
    // ============================================================

    private Schema _schema;
    private Table _rootTable;
    private List<Join> _joinedTables = new ArrayList<Join>();
    private Map<Class<? extends Fauxjo>, SQLTableProcessor<? extends Fauxjo>> _sqlTableProcessors = new HashMap<Class<? extends Fauxjo>, SQLTableProcessor<? extends Fauxjo>>();

    // ============================================================
    // Constructors
    // ============================================================

    public JoinedSQLTableProcessor( Class<T> beanClass, Schema schema )
    {
        super( new ResultSetRecordProcessor<T>( beanClass ) );
        _schema = schema;
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    @SuppressWarnings(
    { "unchecked", "rawtypes" } )
    public JoinedSQLTableProcessor<T> setRootTable( Class<? extends Fauxjo> beanClass, String tableName,
        String tableAlias )
        throws SQLException
    {
        _rootTable = new Table( beanClass, _schema.getQualifiedName( tableName ), tableAlias );
        _sqlTableProcessors.put( beanClass, new SQLTableProcessor( _schema, tableName, beanClass ) );
        return this;
    }

    @SuppressWarnings(
    { "unchecked", "rawtypes" } )
    public JoinedSQLTableProcessor<T> addChildTable( Class<? extends Fauxjo> beanClass, Schema schema,
        String tableName, String tableAlias, String joinCriteria )
        throws SQLException
    {
        _joinedTables.add( new Join( new Table( beanClass, _schema.getQualifiedName( tableName ), tableAlias ),
            joinCriteria ) );
        _sqlTableProcessors.put( beanClass, new SQLTableProcessor( schema, tableName, beanClass ) );
        return this;
    }

    public boolean insert( T bean )
        throws SQLException
    {
        boolean success = true;
        success &= _sqlTableProcessors.get( _rootTable.getBeanClass() ).insert( bean );
        for ( Join join : _joinedTables )
        {
            success &= _sqlTableProcessors.get( join.getTable().getBeanClass() ).insert( bean );
        }

        return success;
    };

    public boolean delete( T bean )
        throws SQLException
    {
        boolean success = true;
        for ( int i = _joinedTables.size() - 1; i >= 0; --i )
        {
            Join join = _joinedTables.get( i );
            success &= _sqlTableProcessors.get( join.getTable().getBeanClass() ).delete( bean );
        }

        success &= _sqlTableProcessors.get( _rootTable.getBeanClass() ).delete( bean );

        return success;
    }

    public int update( T bean )
        throws SQLException
    {
        int n = 0;
        n += _sqlTableProcessors.get( _rootTable.getBeanClass() ).update( bean );
        for ( Join join : _joinedTables )
        {
            n += _sqlTableProcessors.get( join.getTable().getBeanClass() ).update( bean );
        }

        return n;
    }

    public String buildBasicSelect( String clause )
    {
        String c = "";
        if ( clause != null && !clause.trim().isEmpty() )
        {
            c = clause;
        }

        StringBuilder builder = new StringBuilder( "select " );
        builder.append( _rootTable.getSelectClause() );
        for ( Join join : _joinedTables )
        {
            builder.append( ", " );
            builder.append( join.getTable().getSelectClause() );
        }

        builder.append( "\nfrom\n" );
        builder.append( _rootTable.getQualifiedName() );
        builder.append( " as " );
        builder.append( _rootTable.getAlias() );
        for ( Join join : _joinedTables )
        {
            builder.append( "\njoin " );
            builder.append( join.getTable().getQualifiedName() );
            builder.append( " as " );
            builder.append( join.getTable().getAlias() );
            builder.append( " on (" );
            builder.append( join.getJoinCriteria() );
            builder.append( ")" );
        }

        builder.append( "\n" );
        builder.append( c );

        return builder.toString();
    }

    @Override
    public Schema getSchema()
    {
        return _schema;
    }

    public T convertResultSetRow( ResultSet rs )
        throws SQLException
    {
        return getResultSetRecordProcessor().convertResultSetRow( rs );
    }

    // ============================================================
    // Inner Classes
    // ============================================================

    private static final class Join
    {
        private Table _table;
        private String _joinCriteria;

        public Join( Table table, String joinCriteria )
        {
            _table = table;
            _joinCriteria = joinCriteria;
        }

        public Table getTable()
        {
            return _table;
        }

        public String getJoinCriteria()
        {
            return _joinCriteria;
        }
    }

    private static final class Table
    {
        private Class<? extends Fauxjo> _beanClass;
        private String _qualifiedName;
        private String _alias;

        public Table( Class<? extends Fauxjo> beanClass, String qualifiedName, String alias )
        {
            _beanClass = beanClass;
            _qualifiedName = qualifiedName;
            _alias = alias;
        }

        public Class<? extends Fauxjo> getBeanClass()
        {
            return _beanClass;
        }

        public String getAlias()
        {
            return _alias;
        }

        public String getSelectClause()
        {
            return _alias + ".*";
        }

        public String getQualifiedName()
        {
            return _qualifiedName;
        }
    }
}
