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

import java.sql.*;
import java.util.*;

/**
 * Business logic for interacting with an SQL database table.
 */
public class SQLTableProcessor < T extends Fauxjo >
{
    // ============================================================
    // Fields
    // ============================================================

    private static final String TABLE_NAME = "TABLE_NAME";
    private static final String COLUMN_NAME = "COLUMN_NAME";
    private static final String DATA_TYPE = "DATA_TYPE";

    private Schema _schema;
    private String _tableName;
    private Class<T> _beanClass;

    private Coercer _coercer;

    // Key = Lowercase column name (in code known as the "key").
    // Value = Name of column used by the database and SQL type.
    private Map<String,ColumnInfo> _dbColumnInfos;

    // Key = Lowercase column name (in code known as the "key").
    // Value = Information about the bean property.
    private Map<String,ValueDef> _valueDefs;

    // ============================================================
    // Constructors
    // ============================================================

    public SQLTableProcessor( Schema schema, String tableName, Class<T> beanClass )
        throws SQLException
    {
        _schema = schema;
        _tableName = tableName;
        _beanClass = beanClass;
        _coercer = new Coercer();
        init();
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public Coercer getCoercer()
    {
        return _coercer;
    }

    /**
     * Get first item from result set and not zero. It will never return null.
     */
    public T getOne( ResultSet rs )
        throws SQLException
    {
        T bean = getFirst( rs );
        assert bean != null : "Resultset is improperly empty.";

        return bean;
    }

    /**
     * Get only item from result set (resultset must contain 1 item).
     */
    public T getOnlyOne( ResultSet rs )
        throws SQLException
    {
        T bean = getOnlyFirst( rs );
        assert bean != null : "Resultset is improperly empty.";

        return bean;
    }

    /**
     * Create an item from the first row in the result set.
     */
    public T getFirst( ResultSet rs )
        throws SQLException
    {
        List<T> beans = processResultSet( rs, 1 );
        if ( beans == null || beans.isEmpty() )
        {
            return null;
        }
        else
        {
            return beans.get( 0 );
        }
    }

    /**
     * Get only item from result or null (resultset must contain 0 or 1 items).
     */
    public T getOnlyFirst( ResultSet rs )
        throws SQLException
    {
        List<T> beans = processResultSet( rs, 2 );
        if ( beans == null || beans.isEmpty() )
        {
            return null;
        }

        assert beans.size() == 1 : "More than one item was in resultset.";

        return beans.get( 0 );
    }

    /**
     * Convert each row in the result set to an item.
     */
    public List<T> getList( ResultSet rs )
        throws SQLException
    {
        return getList( rs, Integer.MAX_VALUE );
    }

    public List<T> getList( ResultSet rs, int maxNumItems )
        throws SQLException
    {
        return processResultSet( rs, maxNumItems );
    }

    public ResultSetIterator<T> getIterator( ResultSet rs )
        throws SQLException
    {
        ResultSetIterator<T> iterator = new ResultSetIterator<T>( this, rs );

        return iterator;
    }

    /**
     * Convert the bean into an insert statement and execute it.
     */
    public boolean insert( T bean )
        throws SQLException
    {
        try
        {
            StringBuilder columns = new StringBuilder();
            StringBuilder questionMarks = new StringBuilder();
            List<DataValue> values = new ArrayList<DataValue>();
            HashMap<String,String> generatedKeys = new HashMap<String,String>();

            for ( String key : getDBColumnInfos().keySet() )
            {
                ColumnInfo columnInfo = getDBColumnInfos().get( key );
                Class<?> destClass = SQLTypeMapper.getInstance().getJavaClass(
                    columnInfo.getSQLType() );

                Object val = bean.readValue( key );
                try
                {
                    val = _coercer.coerce( val, destClass );
                }
                catch ( FauxjoException ex )
                {
                    throw new FauxjoException( "Failed to coerce " +
                        getQualifiedName( _tableName ) + "." + columnInfo.getRealName() +
                        " for insert: " + key + ":" + columnInfo.getRealName(), ex );
                }

                // If this is a primary key and it is null, try to get sequence name from
                // annotation and not include this column in insert statement.
                ValueDef valueDef = getValueDefs( bean ).get( key );
                if ( valueDef == null )
                {
                    continue;
                }
                else if ( valueDef.isPrimaryKey() && val == null &&
                    valueDef.getPrimaryKeySequenceName() != null )
                {
                    generatedKeys.put( key, valueDef.getPrimaryKeySequenceName() );
                    continue;
                }

                if ( columns.length() > 0 )
                {
                    columns.append( "," );
                    questionMarks.append( "," );
                }
                columns.append( columnInfo.getRealName() );
                questionMarks.append( "?" );
                values.add( new DataValue( val, columnInfo.getSQLType() ) );

                // If this is a TS Vector text search column, add the TS Vector column.
//                if ( readMethod.isAnnotationPresent( TextIndexedColumn.class ) )
//                {
//                    TextIndexedColumn ann = readMethod.getAnnotation( TextIndexedColumn.class );
//                    String tsVectorColumn = null;
//                    if ( ann != null )
//                    {
//                        tsVectorColumn = ann.textIndexMetaColumn().trim();
//                    }
//
//                    // If the tsVectorColumn is not specified, we use the original column name +
//                    // TsVector. So if you have a column named 'noteText', then it would use
//                    // noteTextTsVector unless specified in the annotation.
//                    if ( tsVectorColumn == null || tsVectorColumn.equals( "" ) )
//                    {
//                        tsVectorColumn = columnInfo.getRealName() + "TsVector";
//                    }
//
//                    columns.append( "," + tsVectorColumn );
//                    questionMarks.append( ",to_tsvector(?)" );
//                    values.add( new DataValue( val, columnInfo.getSQLType() ) );
//                }
            }
            String sql = "insert into " + getQualifiedName( _tableName ) + " (" + columns +
                ") values (" + questionMarks + ")";
            PreparedStatement statement = getConnection().prepareStatement( sql );
            int propIndex = 1;
            for ( DataValue value : values )
            {
                statement.setObject( propIndex, value.getValue(), value.getSqlType() );
                propIndex++;
            }
            boolean retVal = statement.execute();

            //
            // Now get generated keys
            //
            for ( String key : generatedKeys.keySet() )
            {
                Statement gkStatement = getConnection().createStatement();
                ResultSet rs = gkStatement.executeQuery( "select currval('" +
                    getQualifiedName( generatedKeys.get( key ) ) + "')" );
                rs.next();
                Object value = rs.getObject( 1 );
                rs.close();
                gkStatement.close();

                bean.writeValue( key, value );
            }

            return retVal;
        }
        catch ( Exception ex )
        {
            throw new FauxjoException( ex );
        }
    }

    /**
     * Convert the bean into an update statement and execute it.
     */
    public int update( T bean )
        throws SQLException
    {
        try
        {
            StringBuilder setterClause = new StringBuilder();
            StringBuilder whereClause = new StringBuilder();
            List<DataValue> values = new ArrayList<DataValue>();
            List<DataValue> keyValues = new ArrayList<DataValue>();
            for ( String key : getDBColumnInfos().keySet() )
            {
                ColumnInfo columnInfo = getDBColumnInfos().get( key );
                Class<?> destClass = SQLTypeMapper.getInstance().getJavaClass(
                    columnInfo.getSQLType() );

                Object val = bean.readValue( key );
                try
                {
                    val = _coercer.coerce( val, destClass );
                }
                catch ( FauxjoException ex )
                {
                    throw new FauxjoException( "Failed to coerce " +
                        getQualifiedName( _tableName ) + "." + columnInfo.getRealName() +
                        " for insert: " + key + ":" + columnInfo.getRealName(), ex );
                }

                ValueDef valueDef = getValueDefs( bean ).get( key );
                if ( valueDef != null )
                {
                    if ( valueDef.isPrimaryKey() )
                    {
                        if ( whereClause.length() > 0 )
                        {
                            whereClause.append( " and " );
                        }
                        whereClause.append( columnInfo.getRealName() + "=?" );
                        keyValues.add( new DataValue( val, columnInfo.getSQLType() ) );
                    }
                    else
                    {
                        if ( setterClause.length() > 0 )
                        {
                            setterClause.append( "," );
                        }
                        setterClause.append( columnInfo.getRealName() + "=?" );
                        values.add( new DataValue( val, columnInfo.getSQLType() ) );

//                    if ( readMethod.isAnnotationPresent( TextIndexedColumn.class ) )
//                    {
//                        TextIndexedColumn ann = readMethod.getAnnotation( TextIndexedColumn.class );
//                        String tsVectorColumn = null;
//                        if ( ann != null )
//                        {
//                            tsVectorColumn = ann.textIndexMetaColumn().trim();
//                        }
//
//                        // If the tsVectorColumn is not specified, we use the original column name +
//                        // TsVector. So if you have a column named 'noteText', then it would use
//                        // noteTextTsVector unless specified in the annotation.
//                        if ( tsVectorColumn == null || tsVectorColumn.equals( "" ) )
//                        {
//                            tsVectorColumn = columnInfo.getRealName() + "TsVector";
//                        }
//
//                        setterClause.append( "," + tsVectorColumn + "=to_tsvector(?)" );
//                        values.add( new DataValue( val, columnInfo.getSQLType() ) );
//                    }
                    }
                }
            }

            String sql = "update " + getQualifiedName( _tableName ) + " set " + setterClause +
                " where " + whereClause;
            PreparedStatement statement = getConnection().prepareStatement( sql );
            int propIndex = 1;
            for ( DataValue value : values )
            {
                statement.setObject( propIndex, value.getValue(), value.getSqlType() );
                propIndex++;
            }
            for ( DataValue value : keyValues )
            {
                statement.setObject( propIndex, value.getValue(), value.getSqlType() );
                propIndex++;
            }

            return statement.executeUpdate();
        }
        catch ( Exception ex )
        {
            throw new FauxjoException( ex );
        }
    }

    /**
     * Convert the bean into an delete statement and execute it.
     */
    public boolean delete( T bean )
        throws SQLException
    {
        try
        {
            StringBuilder whereClause = new StringBuilder();
            List<DataValue> primaryKeyValues = new ArrayList<DataValue>();
            for ( String key : getValueDefs( bean ).keySet() )
            {
                ValueDef valueDef = getValueDefs( bean ).get( key );
                if ( valueDef == null || !valueDef.isPrimaryKey() )
                {
                    continue;
                }

                ColumnInfo columnInfo = getDBColumnInfos().get( key );
                Class<?> destClass = SQLTypeMapper.getInstance().getJavaClass(
                    columnInfo.getSQLType() );

                Object val = bean.readValue( key );
                val = _coercer.coerce( val, destClass );

                if ( whereClause.length() > 0 )
                {
                    whereClause.append( " and " );
                }
                whereClause.append( columnInfo.getRealName() + "=?" );
                primaryKeyValues.add( new DataValue( val, columnInfo.getSQLType() ) );
            }

            String sql = "delete from " + getQualifiedName( _tableName ) + " where " + whereClause;
            PreparedStatement statement = getConnection().prepareStatement( sql );
            int propIndex = 1;
            for ( DataValue value : primaryKeyValues )
            {
                statement.setObject( propIndex, value.getValue(), value.getSqlType() );
                propIndex++;
            }
            return statement.execute();
        }
        catch ( Exception ex )
        {
            throw new FauxjoException( ex );
        }
    }

    /**
     * Get the next value from a sequence.
     */
    public Long getNextKey( String sequenceName )
        throws SQLException
    {
        assert sequenceName != null;

        PreparedStatement getKey = getConnection().prepareStatement( "select nextval('" +
            getQualifiedName( sequenceName ) + "')" );

        ResultSet rs = getKey.executeQuery();
        rs.next();
        return rs.getLong( 1 );
    }

    // ----------
    // protected
    // ----------

    protected Connection getConnection()
        throws SQLException
    {
        return _schema.getConnection();
    }

    private String getQualifiedName( String name )
    {
        return _schema.getQualifiedName( name );
    }

    protected List<T> processResultSet( ResultSet rs, int numRows )
        throws SQLException
    {
        List<T> list = new ArrayList<T>();

        int counter = 0;
        while ( rs.next() && ( counter < numRows ) )
        {
            list.add( convertResultSetRow( rs ) );
            counter++;
        }
        rs.close();

        return list;
    }

    protected T convertResultSetRow( ResultSet rs )
        throws SQLException
    {
        try
        {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            Map<String,Object> record = new HashMap<String,Object>();
            for ( int i = 1; i <= columnCount; i++ )
            {
                record.put( meta.getColumnName( i ).toLowerCase(), rs.getObject( i ) );
            }

            return processRecord( record );
        }
        catch ( Exception ex )
        {
            throw new FauxjoException( ex );
        }
    }

    protected T processRecord( Map<String,Object> record )
        throws SQLException
    {
        T bean = null;

        try
        {
            bean = (T)_beanClass.newInstance();
        }
        catch ( Exception ex )
        {
            throw new FauxjoException( ex );
        }

        for ( String key : record.keySet() )
        {
            ValueDef valueDef = getValueDefs( bean ).get( key );
            if ( valueDef != null )
            {
                Object value = record.get( key );

                try
                {
                    if ( value != null )
                    {
                        Class<?> destClass = valueDef.getValueClass();
                        value = _coercer.coerce( value, destClass );
                    }
                }
                catch ( FauxjoException ex )
                {
                    throw new FauxjoException( "Failed to coerce " + key, ex );
                }

                bean.writeValue( key, value );
            }
        }

        return bean;
    }

    // ----------
    // private
    // ----------

    private void init()
        throws SQLException
    {
        cacheColumnInfos( false );
    }

    private Map<String,ColumnInfo> getDBColumnInfos()
        throws SQLException
    {
        if ( _dbColumnInfos == null )
        {
            cacheColumnInfos( true );
        }

        return _dbColumnInfos;
    }

    /**
     * This is a really slow method to call when it actually gets the meta data.
     */
    private void cacheColumnInfos( boolean throwException )
        throws SQLException
    {
        String realTableName = getRealTableName( _tableName );

        //
        // If the table does not actually exist optionally throw exception.
        //
        if ( realTableName == null )
        {
            if ( throwException )
            {
                throw new FauxjoException( String.format( "Table %s does not exist.",
                    getQualifiedName( _tableName ) ) );
            }
            else
            {
                return;
            }
        }

        _dbColumnInfos = new HashMap<String,ColumnInfo>();

        ResultSet rs = getConnection().getMetaData().getColumns( null, _schema.getSchemaName(),
            realTableName, null );
        while ( rs.next() )
        {
            String realName = rs.getString( COLUMN_NAME );
            Integer type = rs.getInt( DATA_TYPE );

            _dbColumnInfos.put( realName.toLowerCase(), new ColumnInfo( realName, type ) );
        }
        rs.close();
    }

    /**
     * This takes a case insensitive tableName and searches for it in the connection's meta data
     * to find the connections case sensitive tableName.
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

        rs = getConnection().getMetaData().getTables( null, _schema.getSchemaName(), null,
            tableTypes.toArray( new String[0] ) );

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

    /**
     * See if the ValueDefs have already been cached, if not call this bean to get it's
     * ValueDefs.
     */
    private Map<String,ValueDef> getValueDefs( Fauxjo bean )
        throws FauxjoException
    {
        if ( _valueDefs == null )
        {
            _valueDefs = bean.getValueDefs();
        }

        return _valueDefs;
    }

    // ============================================================
    // Inner Classes
    // ============================================================

    public static class ColumnInfo
    {
        private String _realName;
        private int _sqlType;

        public ColumnInfo( String realName, int sqlType )
        {
            _realName = realName;
            _sqlType = sqlType;
        }

        public String getRealName()
        {
            return _realName;
        }

        public void setRealName( String realName )
        {
            _realName = realName;
        }

        public int getSQLType()
        {
            return _sqlType;
        }

        public void setSQLType( int sqlType )
        {
            _sqlType = sqlType;
        }
    }

    private class DataValue
    {
        private Object _value;
        private int _sqlType;

        public DataValue( Object value, int sqlType )
        {
            _value = value;
            _sqlType = sqlType;
        }

        public Object getValue()
        {
            return _value;
        }

        public int getSqlType()
        {
            return _sqlType;
        }
    }
}

