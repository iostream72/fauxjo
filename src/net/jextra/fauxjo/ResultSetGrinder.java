//
// ResultSetGrinder
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

import java.lang.reflect.*;
import java.sql.*;
import java.sql.Array;
import java.util.*;
import net.jextra.fauxjo.beandef.*;

/**
 * Converts a {@link ResultSet} into a colletion of Java Bean objects.
 */
public class ResultSetGrinder<T>
{
    // ============================================================
    // Fields
    // ============================================================

    private Class<T> beanClass;
    private Coercer coercer;

    // Key = Lowercase column name (in code known as the "key").
    // Value = Information about the bean property.
    private Map<String, FieldDef> fieldDefs;

    private ResultSet resultSet;
    private Long maxRowCount;

    // ============================================================
    // Constructors
    // ============================================================

    public ResultSetGrinder( Class<T> beanClass )
    {
        this.beanClass = beanClass;
        coercer = new Coercer();
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public void setStatement( PreparedStatement statement )
        throws SQLException
    {
        resultSet = statement.executeQuery();
    }

    public void setResultSet( ResultSet rs )
    {
        resultSet = rs;
    }

    public void setMaxRowCount( long maxRowCount )
    {
        this.maxRowCount = maxRowCount;
    }

    public void clearMaxRowCount()
    {
        maxRowCount = null;
    }

    public List<T> getList()
        throws SQLException
    {
        ArrayList<T> list = new ArrayList<T>();

        int counter = 0;
        while ( resultSet.next() && ( maxRowCount == null || counter < maxRowCount ) )
        {
            list.add( processRow( resultSet ) );
            counter++;
        }
        resultSet.close();
        resultSet = null;

        return list;
    }

    public Set<T> getSet()
        throws SQLException
    {
        HashSet<T> set = new HashSet<T>();

        int counter = 0;
        while ( resultSet.next() && ( maxRowCount == null || counter < maxRowCount ) )
        {
            set.add( processRow( resultSet ) );
            counter++;
        }
        resultSet.close();
        resultSet = null;

        return set;
    }

    //    public ResultSetIterator<T> getIterator()
    //        throws SQLException
    //    {
    //        ResultSetIterator<T> iterator = new ResultSetIterator<T>( this, resultSet );
    //
    //        return iterator;
    //    }

    public T processRow( ResultSet rs )
        throws SQLException
    {
        try
        {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            Map<String, Object> values = new HashMap<String, Object>();
            for ( int i = 1; i <= columnCount; i++ )
            {
                if ( meta.getColumnType( i ) == java.sql.Types.ARRAY )
                {
                    Array array = rs.getArray( i );
                    Object actualArray = null;
                    if ( array != null )
                    {
                        actualArray = array.getArray();
                    }
                    values.put( meta.getColumnName( i ).toLowerCase(), actualArray );
                }
                else
                {
                    values.put( meta.getColumnName( i ).toLowerCase(), rs.getObject( i ) );
                }
            }

            return createBean( values );
        }
        catch ( Exception ex )
        {
            if ( ex instanceof FauxjoException )
            {
                throw (FauxjoException) ex;
            }

            throw new FauxjoException( ex );
        }
    }

    public Map<String, FieldDef> getBeanFieldDefs( Class<?> beanClass )
        throws FauxjoException
    {
        if ( fieldDefs == null )
        {
            fieldDefs = BeanDefCache.getFieldDefs( beanClass );
        }

        return fieldDefs;
    }

    // ----------
    // protected
    // ----------

    protected T createBean( Map<String, Object> values )
        throws SQLException
    {
        T bean = null;

        try
        {
            bean = (T) beanClass.newInstance();
        }
        catch ( Exception ex )
        {
            throw new FauxjoException( ex );
        }

        Map<String, FieldDef> fieldDefs = new HashMap<String, FieldDef>( getBeanFieldDefs( beanClass ) );
        for ( String key : values.keySet() )
        {
            FieldDef fieldDef = fieldDefs.get( key );

            // Remove key from fieldDefs in order to take inventory to check later that all were used.
            fieldDefs.remove( key );

            // If column in database but not in bean, assumed OK, ignore.
            if ( fieldDef != null )
            {
                Object value = values.get( key );

                try
                {
                    if ( value != null )
                    {
                        Class<?> destClass = fieldDef.getValueClass();
                        value = coercer.coerce( value, destClass );
                    }
                }
                catch ( FauxjoException ex )
                {
                    throw new FauxjoException( "Failed to coerce " + key, ex );
                }

                writeValue( bean, key, value );
            }
        }

        // If any of the columns was not accounted for, throw an Exception
        if ( !fieldDefs.isEmpty() )
        {
            throw new FauxjoException( "Missing column [" + fieldDefs.keySet().iterator().next() + "] in ResultSet for Object [" +
                beanClass.getCanonicalName() + "]" );
        }

        return bean;
    }

    protected void writeValue( Object bean, String key, Object value )
        throws FauxjoException
    {
        try
        {
            BeanDef beanDef = BeanDefCache.getBeanDef( beanClass );

            Field field = beanDef.getField( key );
            if ( field != null )
            {
                try
                {
                    field.setAccessible( true );
                    field.set( bean, value );
                    return;
                }
                catch ( Exception ex )
                {
                    throw new FauxjoException( "Unable to write to field [" + field.getName() + "]", ex );
                }
            }

            Method writeMethod = beanDef.getWriteMethod( key );
            if ( writeMethod != null )
            {
                try
                {
                    writeMethod.invoke( bean, value );
                }
                catch ( Exception ex )
                {
                    throw new FauxjoException( "Unable to invoke write method [" + writeMethod.getName() + "]", ex );
                }
            }
        }
        catch ( Exception ex )
        {
            if ( ex instanceof FauxjoException )
            {
                throw (FauxjoException) ex;
            }

            throw new FauxjoException( ex );
        }
    }
}
