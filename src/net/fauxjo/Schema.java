//
// Schema
//
// Copyright (C) Brian Stevens.
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

public abstract class Schema
{
    // ============================================================
    // Fields
    // ============================================================

    private HashMap<Class<?>,Home<?>> _homes;
    private String _schemaName;

    // ============================================================
    // Constructors
    // ============================================================

    public Schema()
    {
        _homes = new HashMap<Class<?>,Home<?>>();
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public abstract DBSource getDBSource()
        throws SQLException;
    

    public void addHome( Class<?> beanType, Home<?> home )
    {
        _homes.put( beanType, home );
    }

    public Home<?> getHome( Class<?> beanType )
    {
        return _homes.get( beanType );
    }

    public String getSchemaName()
    {
        return _schemaName;
    }

    public void setSchemaName( String schemaName )
    {
        _schemaName = schemaName;
    }

    public String getQualifiedName( String relationName )
    {
        if ( _schemaName == null || _schemaName.equals( "" ) )
        {
            return relationName;
        }
        else
        {
            return _schemaName + "." + relationName;
        }
    }

    /**
     * Given a bean class, a potentially null bean, and an array of foreign keys find the
     * bean, set it and return it.
     * @throws FauxjoException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     */
    @SuppressWarnings( "unchecked" )
    public < T > T getForeignBean( Class<T> foreignBeanClass, Object ...foreignKeys )
        throws SQLException
    {
        Home<?> home = _homes.get( foreignBeanClass );
        Method finderMethod = findPrimaryFinder( home );
        if ( foreignKeys == null )
        {
            return null;
        }

        try
        {
            return (T)finderMethod.invoke( home, foreignKeys );
        }
        catch ( Exception ex )
        {
            throw new FauxjoException( ex );
        }
    }

    // ----------
    // protected
    // ----------

    protected Method findPrimaryFinder( Home<?> home )
        throws FauxjoException
    {
        for ( Method method : home.getClass().getMethods() )
        {
            if ( method.isAnnotationPresent( FauxjoPrimaryFinder.class ) )
            {
                return method;
            }
        }

        throw new FauxjoException( "The home object " + home.getClass().getCanonicalName() +
            " does not have an annotated FJOPrimaryFinder method" );
    }

    protected Object[] findPrimaryKey( Object bean )
        throws SQLException
    {
        try
        {
            List<Object> returnval = new ArrayList<Object>( 5 );
            for ( Class<?> cls = bean.getClass(); cls != null; cls = cls.getSuperclass() )
            {
                for ( Method method : cls.getMethods() )
                {
                    if ( method.isAnnotationPresent( FauxjoPrimaryKey.class ) )
                    {
                        FauxjoPrimaryKey fpk = method.getAnnotation( FauxjoPrimaryKey.class );
                        returnval.add( fpk.sequence(), method.invoke( bean, new Object[0] ) );
                    }
                }
            }
            if ( returnval.size() == 0 )
            {
                return null;
            }
            return returnval.toArray();
        }
        catch ( Exception ex )
        {
            throw new FauxjoException( ex );
        }
    }
}

