//
// Schema
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

    public abstract Connection getConnection()
        throws SQLException;

    public abstract PreparedStatement prepareStatement( String sql )
        throws SQLException;


    /**
     * @deprecated
     */
    public void addHome( Class<?> beanType, Home<?> home )
    {
        _homes.put( beanType, home );
    }

    /**
     * @deprecated
     */
    public Home<?> getHome( Class<?> beanClass )
    {
        return _homes.get( beanClass );
    }
    
    public String getSchemaName()
    {
        return _schemaName;
    }

    public void setSchemaName( String schemaName )
    {
        _schemaName = schemaName;
    }

    public String getQualifiedName( String name )
    {
        if ( _schemaName == null || _schemaName.equals( "" ) )
        {
            return name;
        }
        else
        {
            return _schemaName + "." + name;
        }
    }
}

