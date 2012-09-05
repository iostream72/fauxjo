//
// SQLTypeMapper
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
 * Mapping between SQL type and Java type. It is intended to go from SQL type -> Java type.
 */
public class SQLTypeMapper extends HashMap<Integer, Class<?>>
{
    // ============================================================
    // Fields
    // ============================================================

    private static SQLTypeMapper instance;

    // ============================================================
    // Constructors
    // ============================================================

    private SQLTypeMapper()
    {
        put( java.sql.Types.ARRAY, Object[].class );
        put( java.sql.Types.BIGINT, Long.class );
        put( java.sql.Types.BINARY, String.class );
        put( java.sql.Types.BIT, Boolean.class );
        put( java.sql.Types.BLOB, Object.class );
        put( java.sql.Types.BOOLEAN, Boolean.class );
        put( java.sql.Types.CHAR, String.class );
        put( java.sql.Types.CLOB, String.class );
        put( java.sql.Types.DATALINK, Object.class );
        put( java.sql.Types.DATE, java.sql.Date.class );
        put( java.sql.Types.DECIMAL, Double.class );
        put( java.sql.Types.DISTINCT, Object.class );
        put( java.sql.Types.DOUBLE, Double.class );
        put( java.sql.Types.FLOAT, Float.class );
        put( java.sql.Types.INTEGER, Integer.class );
        put( java.sql.Types.JAVA_OBJECT, Object.class );
        put( java.sql.Types.LONGNVARCHAR, String.class );
        put( java.sql.Types.LONGVARBINARY, String.class );
        put( java.sql.Types.LONGVARCHAR, String.class );
        put( java.sql.Types.NCHAR, String.class );
        put( java.sql.Types.NCLOB, String.class );
        put( java.sql.Types.NULL, Object.class );
        put( java.sql.Types.NUMERIC, Double.class );
        put( java.sql.Types.NVARCHAR, String.class );
        put( java.sql.Types.OTHER, Object.class );
        put( java.sql.Types.REAL, Double.class );
        put( java.sql.Types.REF, Object.class );
        put( java.sql.Types.ROWID, Object.class );
        put( java.sql.Types.SMALLINT, Short.class );
        put( java.sql.Types.SQLXML, String.class );
        put( java.sql.Types.STRUCT, Object.class );
        put( java.sql.Types.TIME, Time.class );
        put( java.sql.Types.TIMESTAMP, Timestamp.class );
        put( java.sql.Types.TINYINT, Short.class );
        put( java.sql.Types.VARBINARY, String.class );
        put( java.sql.Types.VARCHAR, String.class );
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public static SQLTypeMapper getInstance()
    {
        if ( instance == null )
        {
            instance = new SQLTypeMapper();
        }

        return instance;
    }

    public Class<?> putType( int sqlType, Class<?> javaClass )
    {
        return put( sqlType, javaClass );
    }

    public Class<?> getJavaClass( int sqlType )
    {
        return get( sqlType );
    }
}
