//
// SQLProcessor
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

import java.beans.*;
import java.lang.reflect.*;
import java.sql.*;
import java.sql.Statement;
import java.util.*;

/**
 * Business logic for interacting with an SQL database.
 */
public class SQLProcessor < T extends Fauxjo > 
{
    // ============================================================
    // Fields
    // ============================================================

    private static final String TABLE = "TABLE";
    private static final String TABLE_NAME = "TABLE_NAME";
    private static final String COLUMN_NAME = "COLUMN_NAME";
    private static final String REMARKS = "REMARKS";
    private static final String DATA_TYPE = "DATA_TYPE";

    private Home<T> _home;
    private Class<T> _beanClass;
    // Lower property name, real column name
    private HashMap<String,String> _propToColumnMap;
    // Lower property name, real column name
    private HashMap<String,Integer> _propToDataTypeMap;
    // Lower real column name, property name
    private HashMap<String,String> _columnToPropMap;
    private Coercer _coercer;
    
    // Key=lowercase column name.
    private HashMap<String,Method> _writeMethods;
    private HashMap<String,Method> _readMethods;

    // ============================================================
    // Constructors
    // ============================================================

    public SQLProcessor( Home<T> home, Class<T> beanClass )
        throws SQLException
    {
        _home = home;
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
     * @throws IntrospectionException 
     * @throws SQLException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     */
    public boolean insert( Fauxjo bean )
        throws SQLException
    {
        try
        {
            BeanInfo info = Introspector.getBeanInfo( bean.getClass() );
            StringBuilder columns = new StringBuilder();
            StringBuilder questionMarks = new StringBuilder();
            List<DataValue> values = new ArrayList<DataValue>();
            HashMap<PropertyDescriptor,String> generatedKeys =
                new HashMap<PropertyDescriptor,String>();
            for ( PropertyDescriptor prop : info.getPropertyDescriptors() )
            {
                // If not really a column, forget it.
                if ( !_propToColumnMap.keySet().contains( prop.getName().toLowerCase() ) )
                {
                    continue;
                }

                String realColumnName = _propToColumnMap.get( prop.getName().toLowerCase() );
                int type = _propToDataTypeMap.get( prop.getName().toLowerCase() );
                Class<?> destClass = SQLTypeMapper.getInstance().getJavaClass( type );

                Method readMethod = prop.getReadMethod();
                Object val = readMethod.invoke( bean, new Object[0] );
                val = _coercer.coerce( val, destClass );

                // If this is a primary key and it is null, try to get sequence name from
                // annotation and not include this column in insert statement.
                if ( readMethod.isAnnotationPresent( FauxjoPrimaryKey.class ) && val == null )
                {
                    FauxjoPrimaryKey ann = (FauxjoPrimaryKey)readMethod.getAnnotation(
                        FauxjoPrimaryKey.class );
                    if ( ann.value() != null && !ann.value().trim().isEmpty() )
                    {
                        generatedKeys.put( prop, ann.value().trim() );
                        continue;
                    }
                }

                if ( columns.length() > 0 )
                {
                    columns.append( "," );
                    questionMarks.append( "," );
                }
                columns.append( realColumnName );
                questionMarks.append( "?" );
                values.add( new DataValue( val, type ) );

                // If this is a TS Vector text search column, add the TS Vector column.
                if ( readMethod.isAnnotationPresent( TextIndexedColumn.class ) )
                {
                    TextIndexedColumn ann = readMethod.getAnnotation( TextIndexedColumn.class );
                    String tsVectorColumn = null;
                    if ( ann != null )
                    {
                        tsVectorColumn = ann.textIndexMetaColumn().trim();
                    }

                    // If the tsVectorColumn is not specified, we use the original column name +
                    // TsVector. So if you have a column named 'noteText', then it would use
                    // noteTextTsVector unless specified in the annotation.
                    if ( tsVectorColumn == null || tsVectorColumn.equals( "" ) )
                    {
                        tsVectorColumn = realColumnName + "TsVector";
                    }

                    columns.append( "," + tsVectorColumn );
                    questionMarks.append( ",to_tsvector(?)" );
                    values.add( new DataValue( val, type ) );
                }
            }
            String sql = "insert into " + _home.getQualifiedTableName() + " (" + columns +
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
            String prefix = _home.getSchemaName() == null ? "" : _home.getSchemaName() + ".";
            for ( PropertyDescriptor prop : generatedKeys.keySet() )
            {
                Statement gkStatement = getConnection().createStatement();
                ResultSet rs = gkStatement.executeQuery( "select currval('" + prefix +
                    generatedKeys.get( prop ) + "')" );
                rs.next();
                Object value = rs.getObject( 1 );
                rs.close();
                gkStatement.close();

                Method writeMethod = prop.getWriteMethod();
                writeMethod.invoke( bean, new Object[]
                {
                    value
                } );
            }
            //
            // Fill in the schema value since it is a new object.
            //
            bean.setSchema( _home.getSchema() );
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
    public int update( Fauxjo bean )
        throws SQLException
    {
        try
        {
            BeanInfo info = Introspector.getBeanInfo( bean.getClass() );
            StringBuilder setterClause = new StringBuilder();
            StringBuilder whereClause = new StringBuilder();
            List<DataValue> values = new ArrayList<DataValue>();
            List<DataValue> keyValues = new ArrayList<DataValue>();
            for ( PropertyDescriptor prop : info.getPropertyDescriptors() )
            {
                // If not really a column, forget it.
                if ( !_propToColumnMap.keySet().contains( prop.getName().toLowerCase() ) )
                {
                    continue;
                }

                String realColumnName = _propToColumnMap.get( prop.getName().toLowerCase() );
                int type = _propToDataTypeMap.get( prop.getName().toLowerCase() );
                Class<?> destClass = SQLTypeMapper.getInstance().getJavaClass( type );

                Method readMethod = prop.getReadMethod();
                Object val = readMethod.invoke( bean, new Object[]
                {
                } );
                val = _coercer.coerce( val, destClass );

                if ( readMethod.isAnnotationPresent( FauxjoPrimaryKey.class ) )
                {
                    if ( whereClause.length() > 0 )
                    {
                        whereClause.append( " and " );
                    }
                    whereClause.append( realColumnName + "=?" );
                    keyValues.add( new DataValue( val, type ) );
                }
                else
                {
                    if ( setterClause.length() > 0 )
                    {
                        setterClause.append( "," );
                    }
                    setterClause.append( realColumnName + "=?" );
                    values.add( new DataValue( val, type ) );

                    if ( readMethod.isAnnotationPresent( TextIndexedColumn.class ) )
                    {
                        TextIndexedColumn ann = readMethod.getAnnotation( TextIndexedColumn.class );
                        String tsVectorColumn = null;
                        if ( ann != null )
                        {
                            tsVectorColumn = ann.textIndexMetaColumn().trim();
                        }

                        // If the tsVectorColumn is not specified, we use the original column name +
                        // TsVector. So if you have a column named 'noteText', then it would use
                        // noteTextTsVector unless specified in the annotation.
                        if ( tsVectorColumn == null || tsVectorColumn.equals( "" ) )
                        {
                            tsVectorColumn = realColumnName + "TsVector";
                        }

                        setterClause.append( "," + tsVectorColumn + "=to_tsvector(?)" );
                        values.add( new DataValue( val, type ) );
                    }
                }
            }
            String sql = "update " + _home.getQualifiedTableName() + " set " + setterClause +
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
    public boolean delete( Fauxjo bean )
        throws SQLException
    {
        try
        {
            BeanInfo info = Introspector.getBeanInfo( bean.getClass() );
            StringBuilder whereClause = new StringBuilder();
            List<DataValue> keyValues = new ArrayList<DataValue>();
            for ( PropertyDescriptor prop : info.getPropertyDescriptors() )
            {
                // If not really a column, forget it.
                if ( !_propToColumnMap.keySet().contains( prop.getName().toLowerCase() ) )
                {
                    continue;
                }

                String realColumnName = _propToColumnMap.get( prop.getName().toLowerCase() );
                int type = _propToDataTypeMap.get( prop.getName().toLowerCase() );
                Class<?> destClass = SQLTypeMapper.getInstance().getJavaClass( type );

                Method readMethod = prop.getReadMethod();
                Object val = readMethod.invoke( bean, new Object[]
                {
                } );
                val = _coercer.coerce( val, destClass );

                if ( readMethod.isAnnotationPresent( FauxjoPrimaryKey.class ) )
                {
                    if ( whereClause.length() > 0 )
                    {
                        whereClause.append( " and " );
                    }
                    whereClause.append( realColumnName + "=?" );
                    keyValues.add( new DataValue( val, type ) );
                }
            }
            String sql = "delete from " + _home.getQualifiedTableName() + " where " + whereClause;
            PreparedStatement statement = getConnection().prepareStatement( sql );
            int propIndex = 1;
            for ( DataValue value : keyValues )
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
     * @throws SQLException 
     */
    public Long getNextKey( String sequenceName )
        throws SQLException
    {
        assert sequenceName != null;

        String name = sequenceName;
        if ( _home.getSchemaName() != null )
        {
            name = _home.getSchemaName() + "." + sequenceName;
        }

        PreparedStatement getKey = getConnection().prepareStatement( "select nextval('" + name +
            "')" );

        ResultSet rs = getKey.executeQuery();
        rs.next();
        return rs.getLong( 1 );
    }

    public void setLong( PreparedStatement statement, int index, Long value )
        throws SQLException
    {
        if ( value == null )
        {
            statement.setNull( index, Types.NUMERIC );
        }
        else
        {
            statement.setLong( index, value );
        }
    }

    public void setString( PreparedStatement statement, int index, String value )
        throws SQLException
    {
        if ( value == null )
        {
            statement.setNull( index, Types.VARCHAR );
        }
        else
        {
            statement.setString( index, value );
        }
    }

    public void setDouble( PreparedStatement statement, int index, Double value )
        throws SQLException
    {
        if ( value == null )
        {
            statement.setNull( index, Types.DOUBLE );
        }
        else
        {
            statement.setDouble( index, value );
        }
    }

    public void setInt( PreparedStatement statement, int index, Integer value )
        throws SQLException
    {
        if ( value == null )
        {
            statement.setNull( index, Types.INTEGER );
        }
        else
        {
            statement.setInt( index, value );
        }
    }

    public void setShort( PreparedStatement statement, int index, Short value )
        throws SQLException
    {
        if ( value == null )
        {
            statement.setNull( index, Types.SMALLINT );
        }
        else
        {
            statement.setInt( index, value );
        }
    }

    // ----------
    // protected
    // ----------

    protected Coercer getCoercer()
    {
        return _coercer;
    }

    protected Connection getConnection()
        throws SQLException
    {
        return _home.getSchema().getConnection();
    }

    protected Map<String,String> getColumnToPropMap()
    {
        return _columnToPropMap;
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
        try
        {
            T bean = (T)_beanClass.newInstance();
            bean.setSchema( _home.getSchema() );

            for ( String key : record.keySet() )
            {
                Method method = _writeMethods.get( key );
                if ( method != null )
                {
                    Object value = record.get( key );
                    if ( value != null )
                    {
                        // Assume one argument to the write method.
                        Class<?> destClass = method.getParameterTypes()[0];
                        value = _coercer.coerce( value, destClass );
                    }

                    try
                    {
                        method.invoke( bean, value );
                    }
                    catch ( IllegalAccessException ex )
                    {
                        throw new FauxjoException( "Unable to get value: " + key, ex );
                    }
                    catch ( IllegalArgumentException ex )
                    {
                        throw new FauxjoException( "Unable to get value: " + key, ex );
                    }
                    catch ( InvocationTargetException ex )
                    {
                        throw new FauxjoException( "Unable to get value: " + key, ex );
                    }
                }
            }
            return bean;
        }
        catch ( Exception ex )
        {
            throw new FauxjoException( ex );
        }
    }

    // ----------
    // private
    // ----------

    private void init()
        throws SQLException
    {
        _propToColumnMap = new HashMap<String,String>();
        _propToDataTypeMap = new HashMap<String,Integer>();
        _columnToPropMap = new HashMap<String,String>();

        getReadAndWriteMethods();

        ResultSet rs = getConnection().getMetaData().getColumns( null, _home.getSchemaName(),
            getRealTableName( _home.getTableName() ), null );
        while ( rs.next() )
        {
            String rawName = rs.getString( COLUMN_NAME );
            Integer type = rs.getInt( DATA_TYPE );
            String name = rs.getString( REMARKS );
            if ( name == null )
            {
                name = rawName.toLowerCase();
            }

            _propToColumnMap.put( name.toLowerCase(), rawName );
            _propToDataTypeMap.put( name.toLowerCase(), type );
            _columnToPropMap.put( rawName.toLowerCase(), name );
        }
        rs.close();
    }

    private String getRealTableName( String tableName )
        throws SQLException
    {
        ResultSet rs = getConnection().getMetaData().getTables( null, _home.getSchemaName(), null,
            new String[]
        {
            TABLE
        } );
        while ( rs.next() )
        {
            if ( rs.getString( TABLE_NAME ).equalsIgnoreCase( tableName ) )
            {
                return rs.getString( TABLE_NAME );
            }
        }

        return null;
    }

    private void getReadAndWriteMethods()
        throws SQLException
    {
        try
        {
            BeanInfo info = Introspector.getBeanInfo( _beanClass );
            _writeMethods = new HashMap<String,Method>();
            _readMethods = new HashMap<String,Method>();

            for ( PropertyDescriptor prop : info.getPropertyDescriptors() )
            {
                if ( prop.getWriteMethod() != null )
                {
                    String name = prop.getName();
                    
                    //
                    // Check for override of column name in database for this write method.
                    //
                    FauxjoSetter ann = prop.getWriteMethod().getAnnotation( FauxjoSetter.class );
                    if ( ann != null )
                    {
                        if ( !ann.column().isEmpty() )
                        {
                            name = ann.column();
                        }
                    }
                    
                    _writeMethods.put( name.toLowerCase(), prop.getWriteMethod() );
                }

                if ( prop.getReadMethod() != null )
                {
                    _readMethods.put( prop.getName().toLowerCase(), prop.getReadMethod() );
                }
            }
        }
        catch ( Exception ex )
        {
            throw new FauxjoException( ex );
        }
    }

    // ============================================================
    // Inner Classes
    // ============================================================

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

