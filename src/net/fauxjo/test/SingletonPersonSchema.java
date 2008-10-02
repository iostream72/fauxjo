//
// PersonSchema
//
// Copyright (C) 2007 Brian Stevens.
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

package net.fauxjo.test;

import java.sql.*;
import net.fauxjo.*;
import net.jextra.dbconnection.*;

public class SingletonPersonSchema extends Schema
{
    // ============================================================
    // Fields
    // ============================================================

    private static SingletonPersonSchema _instance;

    // ============================================================
    // Constructors
    // ============================================================

    public SingletonPersonSchema()
        throws SQLException
    {
        addHome( Person.class, new PersonHome( this ) );
        addHome( Department.class, new DepartmentHome( this ) );
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public static SingletonPersonSchema getInstance()
        throws SQLException
    {
        if ( _instance == null )
        {
            _instance = new SingletonPersonSchema();
        }

        return _instance;
    }

    public static void commit()
        throws SQLException
    {
        getInstance().getConnection().commit();
    }

    public static void rollback()
        throws SQLException
    {
        getInstance().getConnection().rollback();
    }

    public static PersonHome getPersonHome()
        throws SQLException
    {
        return (PersonHome)getInstance().getHome( Person.class );
    }

    public static DepartmentHome getDepartmentHome()
        throws SQLException
    {
        return (DepartmentHome)getInstance().getHome( Department.class );
    }

    public Connection getConnection()
    {
        return DB.getConnection( "person" );
    }
}

