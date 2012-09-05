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

package net.jextra.fauxjo;

import java.sql.*;
import java.util.concurrent.*;

public abstract class Schema
{
    // ============================================================
    // Fields
    // ============================================================

    private ConcurrentMap<Class<?>, Home<?>> homes;
    private String schemaName;

    // ============================================================
    // Constructors
    // ============================================================

    public Schema()
    {
        homes = new ConcurrentHashMap<Class<?>, Home<?>>();
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public abstract Connection getConnection()
        throws SQLException;

    /**
     * It is encouraged that Home objects use this method to prepareStatements and that the
     * implementation of the Schema knows how to cache the PreparedStatements (probably via
     * a {@link PreparedStatementCache}.
     */
    public abstract PreparedStatement prepareStatement( String sql )
        throws SQLException;

    public String getSchemaName()
    {
        return schemaName;
    }

    public void setSchemaName( String schemaName )
    {
        this.schemaName = schemaName;
    }

    /**
     * This method attaches the schema name to the front of the name passed in.
     *
     * @return String that represents the given short name.
     */
    public String getQualifiedName( String name )
    {
        if ( schemaName == null || schemaName.equals( "" ) )
        {
            return name;
        }
        else
        {
            return schemaName + "." + name;
        }
    }

    public void addHome( Class<?> homeClass, Home<?> home )
    {
        homes.put( homeClass, home );
    }

    public <T> T getHomeByClass( Class<T> homeClass )
    {
        return homeClass.cast( homes.get( homeClass ) );
    }
}
