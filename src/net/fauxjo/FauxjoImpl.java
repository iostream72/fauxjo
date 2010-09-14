//
// FauxjoImpl
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

package net.fauxjo;

import java.beans.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * <p>
 * Concrete implementation of {@link Fauxjo} interface that is {@link FauxjoPrimaryKey} annotation 
 * aware.
 * </p><p>
 * This implementation assumes for that to test if the object
 * is already in the database or not that {@link #isInDatabase()} just returns the
 * inverse value calculated via {@link #hasEmptyPrimaryKey()}.
 * </p><p>
 * Note: This implementation overrides the {@code hashCode} and {@code equals} methods
 * in order to compare rows in the database properly (e.g. same primary key).
 * </p>
 */
public abstract class FauxjoImpl implements Fauxjo
{
    // ============================================================
    // Fields
    // ============================================================

    private static HashMap<String,ColumnDefsCache> _caches;

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public Map<String,ValueDef> getValueDefs()
        throws FauxjoException
    {
        try
        {
            ColumnDefsCache cache = getColumnDefsCache();

            return cache._columnDefs;
        }
        catch ( Exception ex )
        {
            throw new FauxjoException( ex );
        }
    }

    public Object readValue( String key )
        throws FauxjoException
    {
        try
        {
            ColumnDefsCache cache = getColumnDefsCache();

            Method readMethod = cache._readMethods.get( key );
            if ( readMethod != null )
            {
                return readMethod.invoke( this, new Object[0] );
            }
        }
        catch ( Exception ex )
        {
            throw new FauxjoException( ex );
        }

        return null;
    }

    public void writeValue( String key, Object value )
        throws FauxjoException
    {
        try
        {
            ColumnDefsCache cache = getColumnDefsCache();

            Method writeMethod = cache._writeMethods.get( key );
            if ( writeMethod != null )
            {
                writeMethod.invoke( this, value );
            }
        }
        catch ( Exception ex )
        {
            throw new FauxjoException( ex );
        }
    }

    /**
     * @return true if object is already in the database. The default behavior is to return the
     * inverse value of {@link #hasEmptyPrimaryKey(Fauxjo)}. This should be overridden if that
     * is not accurate for this Fauxjo bean.
     */
    public boolean isInDatabase()
        throws FauxjoException
    {
        return !hasEmptyPrimaryKey();
    }

    @Override
    public int hashCode()
    {
        try
        {
            List<Object> keys = getPrimaryKeyValues();

            // Any null keys equate to default of zero.
            if ( keys == null )
            {
                return 0;
            }

            // Just sum up the key item hashCodes for a final hashCode
            int hashCode = 0;
            for ( Object item : keys )
            {
                hashCode += item == null ? 0 : item.hashCode();
            }

            return hashCode;
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( ex );
        }
    }

    @Override
    public boolean equals( Object otherObj )
    {
        try
        {
            // If same object, just quickly return true.
            if ( this == otherObj )
            {
                return true;
            }

            // If other object is not same class as this object, quickly return false.
            if ( otherObj == null || otherObj.getClass().equals( getClass() ) )
            {
                return false;
            }

            FauxjoImpl other = (FauxjoImpl)otherObj;

            List<Object> keys1 = getPrimaryKeyValues();
            List<Object> keys2 = other.getPrimaryKeyValues();

            // If the primary keys somehow are different lengths, they must not be the same.
            if ( keys1.size() != keys2.size() )
            {
                return false;
            }

            // Check each key item, if ever different, return false;
            for ( int i = 0; i < keys1.size(); i++ )
            {
                Object item1 = keys1.get( i );
                Object item2 = keys2.get( i );

                if ( item1 == null && item2 != null )
                {
                    return false;
                }
                else if ( !item1.equals( item2 ) )
                {
                    return false;
                }
            }
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( ex );
        }

        return true;
    }

    // ----------
    // protected
    // ----------

    /**
     * @return Values of primary keys in a consistent order so that it can be compared to another 
     * Fauxjo beans.
     */
    protected List<Object> getPrimaryKeyValues()
        throws FauxjoException
    {
        try
        {
            ColumnDefsCache cache = getColumnDefsCache();

            // Arbitrarily ordered by keys.
            TreeMap<String,Object> keys = new TreeMap<String,Object>();

            for ( String key : getValueDefs().keySet() )
            {
                if ( getValueDefs().get( key ).isPrimaryKey() )
                {
                    keys.put( key, cache._readMethods.get( key ).invoke( this, new Object[0] ) );
                }
            }

            if ( keys.size() == 0 )
            {
                return null;
            }

            return new ArrayList<Object>( keys.values() );
        }
        catch ( Exception ex )
        {
            throw new FauxjoException( ex );
        }
    }

    /**
     * @return false if any of the primary key values are null. This is implied that it is
     * not actually in the database.
     */
    protected boolean hasEmptyPrimaryKey()
        throws FauxjoException
    {
        List<Object> keys = getPrimaryKeyValues();

        // If no values, assume empty.
        if ( keys == null || keys.isEmpty() )
        {
            return true;
        }

        // If any value is null, empty.
        for ( Object key : keys )
        {
            if ( key == null )
            {
                return true;
            }
        }

        return false;
    }

    protected ColumnDefsCache getColumnDefsCache()
        throws FauxjoException, IntrospectionException
    {
        if ( _caches == null )
        {
            _caches = new HashMap<String,ColumnDefsCache>();
        }

        ColumnDefsCache cache = _caches.get( getClass().getCanonicalName() );
        if ( cache != null )
        {
            return cache;
        }

        //
        // Was not cached, collect information.
        //
        cache = new ColumnDefsCache();
        _caches.put( getClass().getCanonicalName(), cache );
        cache._columnDefs = new TreeMap<String,ValueDef>();
        cache._writeMethods = new TreeMap<String,Method>();
        cache._readMethods = new TreeMap<String,Method>();
        BeanInfo info = Introspector.getBeanInfo( getClass() );

        for ( PropertyDescriptor prop : info.getPropertyDescriptors() )
        {
            if ( prop.getWriteMethod() != null )
            {
                String name = prop.getName();

                    //
                    // Check for override of column name in database for this setter method.
                    //
                FauxjoSetter ann = prop.getWriteMethod().getAnnotation( FauxjoSetter.class );
                if ( ann != null )
                {
                    name = ann.column();
                }
                String key = name.toLowerCase();

                ValueDef def = cache._columnDefs.get( key );
                if ( def == null )
                {
                    def = new ValueDef();
                    def.setValueClass( prop.getWriteMethod().getParameterTypes()[0] );
                    cache._columnDefs.put( key, def );
                }
                cache._writeMethods.put( key, prop.getWriteMethod() );
            }

            if ( prop.getReadMethod() != null )
            {
                String name = prop.getName();

                    //
                    // Check for override of column name in database for this getter method.
                    //
                FauxjoGetter ann = prop.getReadMethod().getAnnotation( FauxjoGetter.class );
                if ( ann != null )
                {
                    name = ann.column();
                }
                String key = name.toLowerCase();

                ValueDef def = cache._columnDefs.get( key );
                if ( def == null )
                {
                    def = new ValueDef();
                    def.setValueClass( prop.getReadMethod().getReturnType() );
                    cache._columnDefs.put( key, def );
                }
                cache._readMethods.put( key, prop.getReadMethod() );

                //
                // Check if FauxjoPrimaryKey.
                //
                if ( prop.getReadMethod().isAnnotationPresent( FauxjoPrimaryKey.class ) )
                {
                    def.setPrimaryKey( true );

                    FauxjoPrimaryKey pkAnn = (FauxjoPrimaryKey)prop.getReadMethod().getAnnotation(
                        FauxjoPrimaryKey.class );
                    if ( pkAnn.value() != null && !pkAnn.value().trim().isEmpty() )
                    {
                        def.setPrimaryKeySequenceName( pkAnn.value().trim() );
                    }
                }
            }
        }

        return cache;
    }

    // ============================================================
    // Inner Classes
    // ============================================================

    private static class ColumnDefsCache
    {
        public Map<String,ValueDef> _columnDefs;
        public Map<String,Method> _writeMethods;
        public Map<String,Method> _readMethods;
    }
}

