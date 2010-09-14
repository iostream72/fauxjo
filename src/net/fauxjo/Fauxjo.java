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

package net.fauxjo;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

/**
 * <p>
 * Absolute minimum required representation of a Fauxjo bean. A fauxjo bean is a class that
 * represents a row within a specific database table.
 * </p><p>
 * Note: This implementation overrides the {@code hashCode} and {@code equals} methods
 * in order to compare rows in the database properly (e.g. same primary key).
 * </p><p>
 * Fauxjo beans are {@link FauxjoPrimaryKey} annotation aware.
 * </p>
 */
public abstract class Fauxjo
{
    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    @Override
    public int hashCode()
    {
        try
        {
            List<Object> keys = getPrimaryKeys();

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

            Fauxjo other = (Fauxjo)otherObj;

            List<Object> keys1 = getPrimaryKeys();
            List<Object> keys2 = other.getPrimaryKeys();

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
     * Returns value of primary keys in a consistent order so that it can be compared to another 
     * Fauxjo beans.
     */
    protected List<Object> getPrimaryKeys()
        throws SQLException
    {
        try
        {
            // Arbitrarily ordered by method names.
            TreeMap<String,Object> keys = new TreeMap<String,Object>();
            for ( Class<?> cls = getClass(); cls != null; cls = cls.getSuperclass() )
            {
                for ( Method method : cls.getMethods() )
                {
                    if ( method.isAnnotationPresent( FauxjoPrimaryKey.class ) )
                    {
                        keys.put( method.getName(), method.invoke( this, new Object[0] ) );
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
            throw new FauxjoException( ex );
        }
    }
}

