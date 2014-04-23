//
// SQLTableProcessor
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

import net.jextra.fauxjo.beandef.FieldDef;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Core Business logic for interacting with a single SQL database table.
 */
public class SQLTableProcessor<T extends FauxjoInterface> extends AbstractSQLProcessor<T>
{
    // ============================================================
    // Fields
    // ============================================================

    private static final String TABLE_NAME = "TABLE_NAME";
    private static final String COLUMN_NAME = "COLUMN_NAME";
    private static final String DATA_TYPE = "DATA_TYPE";

    private Schema schema;
    private String tableName;
    private Coercer coercer;
    private Class<T> beanClass;

    // Key = Lowercase column name (in code known as the "key").
    // Value = Name of column used by the database and SQL type.
    private Map<String, ColumnInfo> dbColumnInfos;

    private String updateSQL;
    private String deleteSQL;
    private String[] generatedColumns;

    // ============================================================
    // Constructors
    // ============================================================

    public SQLTableProcessor( Schema schema, String tableName, Class<T> beanClass )
    {
        super( new ResultSetRecordProcessor<T>( beanClass ) );
        this.schema = schema;
        this.tableName = tableName;
        this.coercer = new Coercer();
        this.beanClass = beanClass;
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public Coercer getCoercer()
    {
        return coercer;
    }

    /**
     * Convert the bean into an insert statement and execute it.
     * TODO Consider changing the signature to return the number of rows updated.
     */
    @Override
    public boolean insert( T bean )
        throws SQLException
    {
        PreparedStatement statement = getInsertStatement( bean );

        setInsertValues( statement, bean );

        int rows = statement.executeUpdate();

        retrieveGeneratedKeys( statement, bean );

        return rows > 0;
    }

    /**
     * Convert the bean into an update statement and execute it.
     */
    @Override
    public int update( T bean )
        throws SQLException
    {
        PreparedStatement statement = getUpdateStatement();
        setUpdateValues( statement, bean );

        return statement.executeUpdate();
    }

    /**
     * Convert the bean into an delete statement and execute it.
     */
    @Override
    public boolean delete( T bean )
        throws SQLException
    {
        PreparedStatement statement = getDeleteStatement();
        setDeleteValues( statement, bean );

        return statement.executeUpdate() > 0;
    }

    @Override
    public String buildBasicSelect( String clause )
    {
        String trimmedClause = "";
        if ( clause != null && !clause.trim().isEmpty() )
        {
            trimmedClause = clause;
        }

        return String.format( "select * from %s %s", schema.getQualifiedName( tableName ), trimmedClause );
    }

    @Override
    public Schema getSchema()
    {
        return schema;
    }

    public String getTableName()
    {
        return tableName;
    }

    @Override
    public T convertResultSetRow( ResultSet rs )
        throws SQLException
    {
        return getResultSetRecordProcessor().convertResultSetRow( rs );
    }

    @Override
    public PreparedStatement getInsertStatement( T bean )
        throws SQLException
    {
        StringBuilder columns = new StringBuilder();
        StringBuilder questionMarks = new StringBuilder();

        final Map<String, FieldDef> beanFieldDefs = getResultSetRecordProcessor().getBeanFieldDefs( beanClass );
        final Map<String, ColumnInfo> dbColumnInfos = getDBColumnInfos();
        List<String> generatedKeyColumns = new ArrayList<String>();
        for ( String key : dbColumnInfos.keySet() )
        {
            FieldDef fieldDef = beanFieldDefs.get( key );
            if ( fieldDef != null )
            {
                ColumnInfo columnInfo = dbColumnInfos.get( key );
                boolean addColumn = true;
                if ( fieldDef.isDefaultable() )
                {
                    Object value = getFieldValueFromBean( bean, key, columnInfo );
                    if ( value == null )
                    {
                        generatedKeyColumns.add( columnInfo.getRealName() );
                        addColumn = false;
                    }
                }

                if ( addColumn )
                {
                    if ( columns.length() > 0 )
                    {
                        columns.append( "," );
                        questionMarks.append( "," );
                    }

                    columns.append( columnInfo.getRealName() );
                    questionMarks.append( "?" );
                }
            }
        }

        String insertSQL = String.format( "insert into %s (%s) values (%s)", getQualifiedName( tableName ), columns, questionMarks );
        generatedColumns = generatedKeyColumns.toArray( new String[generatedKeyColumns.size()] );

        return prepareStatement( insertSQL );
    }

    @Override
    public void setInsertValues( PreparedStatement statement, T bean )
        throws SQLException
    {
        final Map<String, FieldDef> beanFieldDefs = getResultSetRecordProcessor().getBeanFieldDefs( beanClass );
        int propIndex = 1;
        for ( String key : getDBColumnInfos().keySet() )
        {
            ColumnInfo columnInfo = getDBColumnInfos().get( key );
            FieldDef fieldDef = beanFieldDefs.get( key );
            if ( fieldDef != null )
            {
                Object val = getFieldValueFromBean( bean, key, columnInfo );

                if ( !fieldDef.isDefaultable() || val != null )
                {
                    //
                    // Set in statement
                    //
                    int sqlType = columnInfo.getSQLType();

                    if ( sqlType == Types.ARRAY )
                    {
                        if ( val == null )
                        {
                            statement.setNull( propIndex, sqlType );
                        }
                        else
                        {
                            Array array = getConnection().createArrayOf( "varchar", (Object[]) val );
                            statement.setArray( propIndex, array );
                        }
                    }
                    else
                    {
                        statement.setObject( propIndex, val, sqlType );
                    }

                    propIndex++;
                }
            }
        }
    }

    @Override
    public PreparedStatement getUpdateStatement()
        throws SQLException
    {
        if ( updateSQL != null )
        {
            return prepareStatement( updateSQL );
        }

        StringBuilder setterClause = new StringBuilder();
        StringBuilder whereClause = new StringBuilder();

        for ( String key : getDBColumnInfos().keySet() )
        {
            ColumnInfo columnInfo = getDBColumnInfos().get( key );

            FieldDef fieldDef = getResultSetRecordProcessor().getBeanFieldDefs( beanClass ).get( key );
            if ( fieldDef != null )
            {
                if ( fieldDef.isPrimaryKey() )
                {
                    if ( whereClause.length() > 0 )
                    {
                        whereClause.append( " and " );
                    }
                    whereClause.append( columnInfo.getRealName() + "=?" );
                }
                else
                {
                    if ( setterClause.length() > 0 )
                    {
                        setterClause.append( "," );
                    }
                    setterClause.append( columnInfo.getRealName() + "=?" );
                }
            }
        }

        if ( whereClause.length() == 0 )
        {
            throw new FauxjoException( "At least one field must be identified as a primary key in order to update rows in the table [" +
                getQualifiedName( tableName ) + "]" );
        }

        updateSQL = String.format( "update %s set %s where %s", getQualifiedName( tableName ), setterClause, whereClause );
        PreparedStatement statement = prepareStatement( updateSQL );

        return statement;
    }

    @Override
    public void setUpdateValues( PreparedStatement statement, T bean )
        throws SQLException
    {
        List<DataValue> values = new ArrayList<DataValue>();
        List<DataValue> keyValues = new ArrayList<DataValue>();

        final Map<String, FieldDef> beanFieldDefs = getResultSetRecordProcessor().getBeanFieldDefs( bean.getClass() );
        for ( String key : getDBColumnInfos().keySet() )
        {
            ColumnInfo columnInfo = getDBColumnInfos().get( key );
            Object val = getFieldValueFromBean( bean, key, columnInfo );

            FieldDef fieldDef = beanFieldDefs.get( key );
            if ( fieldDef != null )
            {
                if ( fieldDef.isPrimaryKey() )
                {
                    keyValues.add( new DataValue( val, columnInfo.getSQLType() ) );
                }
                else
                {
                    values.add( new DataValue( val, columnInfo.getSQLType() ) );
                }
            }
        }

        int propIndex = 1;
        for ( DataValue value : values )
        {
            if ( value.getSqlType() == java.sql.Types.ARRAY )
            {
                if ( value.getValue() == null )
                {
                    statement.setNull( propIndex, value.getSqlType() );
                }
                else
                {
                    Array array = getConnection().createArrayOf( "varchar", (Object[]) value.getValue() );
                    statement.setArray( propIndex, array );
                }
            }
            else
            {
                statement.setObject( propIndex, value.getValue(), value.getSqlType() );
            }
            propIndex++;
        }
        for ( DataValue value : keyValues )
        {
            statement.setObject( propIndex, value.getValue(), value.getSqlType() );
            propIndex++;
        }
    }

    @Override
    public PreparedStatement getDeleteStatement()
        throws SQLException
    {
        if ( deleteSQL != null )
        {
            return prepareStatement( deleteSQL );
        }

        StringBuilder whereClause = new StringBuilder();

        Map<String, FieldDef> fieldDefs = getResultSetRecordProcessor().getBeanFieldDefs( beanClass );
        for ( String key : fieldDefs.keySet() )
        {
            FieldDef fieldDef = fieldDefs.get( key );
            if ( fieldDef == null || !fieldDef.isPrimaryKey() )
            {
                continue;
            }

            ColumnInfo columnInfo = getDBColumnInfos().get( key );

            if ( whereClause.length() > 0 )
            {
                whereClause.append( " and " );
            }
            whereClause.append( columnInfo.getRealName() + "=?" );
        }

        if ( whereClause.length() == 0 )
        {
            throw new FauxjoException( "At least one field must be identified as a primary key in order to delete from the table [" +
                getQualifiedName( tableName ) + "]" );
        }

        deleteSQL = String.format( "delete from %s where %s", getQualifiedName( tableName ), whereClause );

        return prepareStatement( deleteSQL );
    }

    @Override
    public void setDeleteValues( PreparedStatement statement, T bean )
        throws SQLException
    {
        List<DataValue> primaryKeyValues = new ArrayList<DataValue>();

        Map<String, FieldDef> fieldDefs = getResultSetRecordProcessor().getBeanFieldDefs( beanClass );
        for ( String key : fieldDefs.keySet() )
        {
            FieldDef fieldDef = fieldDefs.get( key );
            if ( fieldDef == null || !fieldDef.isPrimaryKey() )
            {
                continue;
            }

            ColumnInfo columnInfo = getDBColumnInfos().get( key );
            Class<?> destClass = SQLTypeMapper.getInstance().getJavaClass( columnInfo.getSQLType() );

            Object val = bean.readValue( key );
            val = coercer.coerce( val, destClass );

            primaryKeyValues.add( new DataValue( val, columnInfo.getSQLType() ) );
        }

        int propIndex = 1;
        for ( DataValue value : primaryKeyValues )
        {
            statement.setObject( propIndex, value.getValue(), value.getSqlType() );
            propIndex++;
        }
    }

    // ----------
    // protected
    // ----------

    protected Connection getConnection()
        throws SQLException
    {
        return schema.getConnection();
    }

    protected PreparedStatement prepareStatement( String sql )
        throws SQLException
    {
        return schema.prepareStatement( sql );
    }

    // ----------
    // private
    // ----------

    private String getQualifiedName( String name )
    {
        return schema.getQualifiedName( name );
    }

    private Map<String, ColumnInfo> getDBColumnInfos()
        throws SQLException
    {
        if ( dbColumnInfos == null )
        {
            cacheColumnInfos( true );
        }

        return dbColumnInfos;
    }

    /**
     * This is a really slow method to call when it actually gets the meta data.
     */
    private void cacheColumnInfos( boolean throwException )
        throws SQLException
    {
        String realTableName = getRealTableName( tableName );

        //
        // If the table does not actually exist optionally throw exception.
        //
        if ( realTableName == null )
        {
            if ( throwException )
            {
                throw new FauxjoException( String.format( "Table %s does not exist.", getQualifiedName( tableName ) ) );
            }
            else
            {
                return;
            }
        }

        HashMap<String, ColumnInfo> map = new HashMap<String, ColumnInfo>();

        ResultSet rs = getConnection().getMetaData().getColumns( null, schema.getSchemaName(), realTableName, null );
        while ( rs.next() )
        {
            String realName = rs.getString( COLUMN_NAME );
            Integer type = rs.getInt( DATA_TYPE );

            map.put( realName.toLowerCase(), new ColumnInfo( realName, type ) );
        }
        rs.close();

        // Only set field if all went well
        dbColumnInfos = map;
    }

    /**
     * This takes a case insensitive tableName and searches for it in the connection's meta data to find the connections case sensitive tableName.
     */
    private String getRealTableName( String tableName )
        throws SQLException
    {
        ArrayList<String> tableTypes = new ArrayList<String>();

        ResultSet rs = getConnection().getMetaData().getTableTypes();
        while ( rs.next() )
        {
            if ( rs.getString( 1 ).toLowerCase().contains( "table" ) )
            {
                tableTypes.add( rs.getString( 1 ) );
            }
        }
        rs.close();

        rs = getConnection().getMetaData().getTables( null, schema.getSchemaName(), null, tableTypes.toArray( new String[0] ) );

        while ( rs.next() )
        {
            if ( rs.getString( TABLE_NAME ).equalsIgnoreCase( tableName ) )
            {
                String name = rs.getString( TABLE_NAME );
                rs.close();
                return name;
            }
        }
        rs.close();

        return null;
    }

    private Object getFieldValueFromBean( T bean, String key, ColumnInfo columnInfo )
        throws FauxjoException
    {
        Class<?> destClass = SQLTypeMapper.getInstance().getJavaClass( columnInfo.getSQLType() );

        Object val = bean.readValue( key );
        try
        {
            val = coercer.coerce( val, destClass );
        }
        catch ( FauxjoException ex )
        {
            throw new FauxjoException( "Failed to coerce " + getQualifiedName( tableName ) + "." + columnInfo.getRealName() + " for insert: " + key +
                ":" + columnInfo.getRealName(), ex );
        }
        return val;
    }

    private void retrieveGeneratedKeys( PreparedStatement statement, T bean )
        throws SQLException
    {
        if ( generatedColumns.length > 0 )
        {
            ResultSet rsKeys = statement.getGeneratedKeys();
            if ( rsKeys.next() )
            {
                final Map<String, FieldDef> beanFieldDefs = getResultSetRecordProcessor().getBeanFieldDefs( beanClass );
                for ( String column : generatedColumns )
                {
                    try
                    {
                        Object value = rsKeys.getObject( column );
                        if ( value != null )
                        {
                            FieldDef fieldDef = beanFieldDefs.get( column );
                            value = coercer.coerce( value, fieldDef.getValueClass() );
                        }
                        bean.writeValue( column, value );
                    }
                    catch ( FauxjoException e )
                    {
                        throw new FauxjoException( "Failed to coerce " + column, e );
                    }
                }
            }
        }
    }

    // ============================================================
    // Inner Classes
    // ============================================================

    public static class ColumnInfo
    {
        private String realName;
        private int sqlType;

        public ColumnInfo( String realName, int sqlType )
        {
            this.realName = realName;
            this.sqlType = sqlType;
        }

        public String getRealName()
        {
            return realName;
        }

        public void setRealName( String realName )
        {
            this.realName = realName;
        }

        public int getSQLType()
        {
            return sqlType;
        }

        public void setSQLType( int sqlType )
        {
            this.sqlType = sqlType;
        }
    }

    private class DataValue
    {
        private Object value;
        private int sqlType;

        public DataValue( Object value, int sqlType )
        {
            this.value = value;
            this.sqlType = sqlType;
        }

        public Object getValue()
        {
            return value;
        }

        public int getSqlType()
        {
            return sqlType;
        }
    }
}
