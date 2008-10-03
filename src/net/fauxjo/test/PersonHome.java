//
// PersonHome
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

public class PersonHome extends Home<Person>
{
    // ============================================================
    // Enums
    // ============================================================

    private enum StatementId
    {
        findByPersonId}

    // ============================================================
    // Constructors
    // ============================================================

    public PersonHome( Schema schema )
        throws SQLException
    {
        super( schema, Person.class, "Person" );

        registerPreparedStatement( StatementId.findByPersonId.name(),
            buildBasicSelect( "where personId=?" ) );
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    @FauxjoPrimaryFinder
    public Person findByPersonId( String personId )
        throws SQLException
    {
        PreparedStatement statement = getPreparedStatement( StatementId.findByPersonId.name() );
        statement.setString( 1, personId );

        return getOne( statement.executeQuery() );
    }

    public List<Person> findAll()
        throws SQLException
    {
        PreparedStatement statement = getConnection().prepareStatement( buildBasicSelect( "" ) );

        return getList( statement.executeQuery() );
    }

    public Person findByFirstName( String firstName )
        throws SQLException
    {
        PreparedStatement statement = getConnection().prepareStatement( buildBasicSelect(
            "where firstName=?" ) );
        statement.setString( 1, firstName );

        return getOne( statement.executeQuery() );
    }

    public List<Person> findByDepartment( Department department )
        throws SQLException
    {
        PreparedStatement statement = getConnection().prepareStatement( buildBasicSelect(
            "where departmentId=?" ) );
        statement.setLong( 1, department.getDepartmentId() );

        return getList( statement.executeQuery() );
    }
}

