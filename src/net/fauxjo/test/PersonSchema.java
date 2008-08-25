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
import java.util.*;
import net.fauxjo.*;
import net.jextra.dbconnection.*;

public class PersonSchema extends Schema
{
    // ============================================================
    // Fields
    // ============================================================

    private static WeakHashMap<Connection,PersonSchema> _instances;

    private Connection _connection;

    // ============================================================
    // Constructors
    // ============================================================

    public PersonSchema( Connection connection )
        throws SQLException
    {
        _connection = connection;
        addHome( Person.class, new PersonHome( this ) );
        addHome( Department.class, new DepartmentHome( this ) );
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public static PersonSchema getInstance( Connection connection )
        throws SQLException
    {
        if ( _instances == null )
        {
            _instances = new WeakHashMap<Connection,PersonSchema>();
        }

        PersonSchema instance = _instances.get( connection );
        if ( instance == null )
        {
            instance = new PersonSchema( connection );
            _instances.put( connection, instance );
        }

        return instance;
    }


    public Connection getConnection()
    {
        return _connection;
    }

    public void commit()
        throws SQLException
    {
        getConnection().commit();
    }

    public void rollback()
        throws SQLException
    {
        getConnection().rollback();
    }

    public PersonHome getPersonHome()
        throws DBException
    {
        return (PersonHome)getHome( Person.class );
    }

    public DepartmentHome getDepartmentHome()
        throws DBException
    {
        return (DepartmentHome)getHome( Department.class );
    }
}

