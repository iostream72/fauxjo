//
// ResultSetRecordProcessor
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
 * Class that's responsible for converting a result set into a {@link FauxjoInterface} bean.
 * 
 * @param <T>
 *            The {@link FauxjoInterface} bean this instance handles conversions for.
 */
public class ResultSetRecordProcessor<T extends FauxjoInterface>
{
    // ============================================================
    // Fields
    // ============================================================

    private Class<T> beanClass;
    private Coercer coercer;

    // Key = Lowercase column name (in code known as the "key").
    // Value = Information about the bean property.
    private Map<String, FieldDef> fieldDefs;

    // ============================================================
    // Constructors
    // ============================================================

    public ResultSetRecordProcessor( Class<T> beanClass )
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

    public T convertResultSetRow( ResultSet rs )
        throws SQLException
    {
        try
        {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            Map<String, Object> record = new HashMap<String, Object>();
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
                    record.put( meta.getColumnName( i ).toLowerCase(), actualArray );
                }
                else
                {
                    record.put( meta.getColumnName( i ).toLowerCase(), rs.getObject( i ) );
                }
            }

            return processRecord( record );
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

    public Map<String, FieldDef> getBeanFieldDefs( FauxjoInterface bean )
        throws FauxjoException
    {
        if ( fieldDefs == null )
        {
            fieldDefs = bean.extractFieldDefs();
        }

        return fieldDefs;
    }

    // ----------
    // protected
    // ----------

    protected T processRecord( Map<String, Object> record )
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

        Map<String, FieldDef> fieldDefs = new HashMap<String, FieldDef>( getBeanFieldDefs( bean ) );
        for ( String key : record.keySet() )
        {
            FieldDef fieldDef = fieldDefs.get( key );

            // Remove key from fieldDefs in order to take inventory to check later that all were used.
            fieldDefs.remove( key );

            // If column in database but not in bean, assumed OK, ignore.
            if ( fieldDef != null )
            {
                Object value = record.get( key );

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

                bean.writeValue( key, value );
            }
        }

        // If any of the columns was not accounted for, throw an Exception
        if ( !fieldDefs.isEmpty() )
        {
            throw new FauxjoException( "Missing column [" + fieldDefs.keySet().iterator().next() + "] in ResultSet for Fauxjo [" +
                beanClass.getCanonicalName() + "]" );
        }

        return bean;
    }
}
