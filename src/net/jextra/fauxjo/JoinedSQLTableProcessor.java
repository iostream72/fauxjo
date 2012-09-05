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
 * will be used when you have a parent table which represents a base class with multiple child tables representing the
 * subclasses. Note that this implementation uses the SQLTableProcessor underneath to do single table operations.
 * 
 * @param <T>
 *            The {@link Fauxjo} bean class represented by the underlying tables.
 */
public class JoinedSQLTableProcessor<T extends Fauxjo> extends AbstractSQLProcessor<T>
{
    // ============================================================
    // Fields
    // ============================================================

    private Schema schema;
    private Table rootTable;
    private List<Join> joinedTables = new ArrayList<Join>();
    private Map<Class<? extends Fauxjo>, SQLTableProcessor<? extends Fauxjo>> sqlTableProcessors = new HashMap<Class<? extends Fauxjo>, SQLTableProcessor<? extends Fauxjo>>();

    // ============================================================
    // Constructors
    // ============================================================

    public JoinedSQLTableProcessor( Class<T> beanClass, Schema schema )
    {
        super( new ResultSetRecordProcessor<T>( beanClass ) );
        this.schema = schema;
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
        rootTable = new Table( beanClass, schema.getQualifiedName( tableName ), tableAlias );
        sqlTableProcessors.put( beanClass, new SQLTableProcessor( schema, tableName, beanClass ) );
        return this;
    }

    @SuppressWarnings(
    { "unchecked", "rawtypes" } )
    public JoinedSQLTableProcessor<T> addChildTable( Class<? extends Fauxjo> beanClass, Schema schema,
        String tableName, String tableAlias, String joinCriteria )
        throws SQLException
    {
        joinedTables.add( new Join( new Table( beanClass, schema.getQualifiedName( tableName ), tableAlias ),
            joinCriteria ) );
        sqlTableProcessors.put( beanClass, new SQLTableProcessor( schema, tableName, beanClass ) );
        return this;
    }

    @Override
    public boolean insert( T bean )
        throws SQLException
    {
        boolean success = true;
        success &= sqlTableProcessors.get( rootTable.getBeanClass() ).insert( bean );
        for ( Join join : joinedTables )
        {
            success &= sqlTableProcessors.get( join.getTable().getBeanClass() ).insert( bean );
        }

        return success;
    };

    @Override
    public boolean delete( T bean )
        throws SQLException
    {
        boolean success = true;
        for ( int i = joinedTables.size() - 1; i >= 0; --i )
        {
            Join join = joinedTables.get( i );
            success &= sqlTableProcessors.get( join.getTable().getBeanClass() ).delete( bean );
        }

        success &= sqlTableProcessors.get( rootTable.getBeanClass() ).delete( bean );

        return success;
    }

    @Override
    public int update( T bean )
        throws SQLException
    {
        int n = 0;
        n += sqlTableProcessors.get( rootTable.getBeanClass() ).update( bean );
        for ( Join join : joinedTables )
        {
            n += sqlTableProcessors.get( join.getTable().getBeanClass() ).update( bean );
        }

        return n;
    }

    @Override
    public String buildBasicSelect( String clause )
    {
        String c = "";
        if ( clause != null && !clause.trim().isEmpty() )
        {
            c = clause;
        }

        StringBuilder builder = new StringBuilder( "select " );
        builder.append( rootTable.getSelectClause() );
        for ( Join join : joinedTables )
        {
            builder.append( ", " );
            builder.append( join.getTable().getSelectClause() );
        }

        builder.append( "\nfrom\n" );
        builder.append( rootTable.getQualifiedName() );
        builder.append( " as " );
        builder.append( rootTable.getAlias() );
        for ( Join join : joinedTables )
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
        return schema;
    }

    @Override
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
        private Table table;
        private String joinCriteria;

        public Join( Table table, String joinCriteria )
        {
            this.table = table;
            this.joinCriteria = joinCriteria;
        }

        public Table getTable()
        {
            return table;
        }

        public String getJoinCriteria()
        {
            return joinCriteria;
        }
    }

    private static final class Table
    {
        private Class<? extends Fauxjo> beanClass;
        private String qualifiedName;
        private String alias;

        public Table( Class<? extends Fauxjo> beanClass, String qualifiedName, String alias )
        {
            this.beanClass = beanClass;
            this.qualifiedName = qualifiedName;
            this.alias = alias;
        }

        public Class<? extends Fauxjo> getBeanClass()
        {
            return beanClass;
        }

        public String getAlias()
        {
            return alias;
        }

        public String getSelectClause()
        {
            return alias + ".*";
        }

        public String getQualifiedName()
        {
            return qualifiedName;
        }
    }
}
