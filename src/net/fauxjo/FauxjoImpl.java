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

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

/**
 * {@link FauxjoPrimaryKey} annotation aware implementation of Fauxjo bean.
 */
public abstract class FauxjoImpl implements Fauxjo
{
    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    /**
     * @return false if any of the primary key values are null. This is implied that it is
     * not actually in the database.
     */
    public boolean isInDatabase()
        throws SQLException
    {
        List<Object> keys = getPrimaryKeys();

        // If no values, assume empty.
        if ( keys == null || keys.isEmpty() )
        {
            return true;
        }

        // If any values null, empty.
        for ( Object key : keys )
        {
            if ( key == null )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        try
        {
            // Any nulled hash keys equate to default of zero.
            return getPrimaryKeys() == null ? 0 : getPrimaryKeys().hashCode();
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( ex );
        }
    }

    @Override
    public boolean equals( Object other )
    {
        try
        {
            if ( getPrimaryKeys() != null && other != null && other.getClass().equals(
                getClass() ) )
            {
                return getPrimaryKeys().equals( ( (FauxjoImpl)other ).getPrimaryKeys() );
            }
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( ex );
        }

        return super.equals( other );
    }

    // ----------
    // protected
    // ----------

    /**
     * Returns value of primary keys in a standard order.
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

