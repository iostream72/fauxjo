//
// Fauxjo
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
import java.util.*;
import net.jextra.fauxjo.beandef.*;

/**
 * <p>
 * Concrete implementation of {@link FauxjoInterface} interface that is {@link FauxjoField} annotation aware.
 * </p>
 * <p>
 * Note: This implementation overrides the {@code hashCode} and {@code equals} methods in order to properly compare Fauxjo's properly (e.g. same
 * primary key) when placed in Collections, etc.
 * </p>
 */
public abstract class Fauxjo implements FauxjoInterface
{
    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    @Override
    public Object readValue( String key )
        throws FauxjoException
    {
        try
        {
            BeanDef beanDef = BeanDefCache.getBeanDef( getClass() );

            Field field = beanDef.getField( key );
            if ( field != null )
            {
                field.setAccessible( true );
                return field.get( this );
            }

            Method readMethod = beanDef.getReadMethod( key );
            if ( readMethod != null )
            {
                return readMethod.invoke( this, new Object[0] );
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

        return null;
    }

    @Override
    public void writeValue( String key, Object value )
        throws FauxjoException
    {
        try
        {
            BeanDef beanDef = BeanDefCache.getBeanDef( getClass() );

            Field field = beanDef.getField( key );
            if ( field != null )
            {
                try
                {
                    field.setAccessible( true );
                    field.set( this, value );
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
                    writeMethod.invoke( this, value );
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
            if ( otherObj == null || !otherObj.getClass().equals( getClass() ) )
            {
                return false;
            }

            Fauxjo other = (Fauxjo) otherObj;

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
     * @return Values of primary keys in a consistent order so that it can be compared to another Fauxjo beans.
     */
    protected List<Object> getPrimaryKeyValues()
        throws FauxjoException
    {
        try
        {
            // Arbitrarily ordered by keys.
            TreeMap<String, Object> keys = new TreeMap<String, Object>();

            Map<String, FieldDef> map = BeanDefCache.getBeanDef( getClass() ).getFieldDefs();
            for ( String key : map.keySet() )
            {
                FieldDef def = map.get( key );
                if ( def.isPrimaryKey() )
                {
                    if ( def.getField() != null )
                    {
                        def.getField().setAccessible( true );
                        keys.put( key, def.getField().get( this ) );
                    }
                    else
                    {
                        keys.put( key, def.getReadMethod().invoke( this, new Object[0] ) );
                    }
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
            if ( ex instanceof FauxjoException )
            {
                throw (FauxjoException) ex;
            }

            throw new FauxjoException( ex );
        }
    }
}
