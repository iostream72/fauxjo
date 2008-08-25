//
// DepartmentHome
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

public class DepartmentHome extends Home<Department>
{
    // ============================================================
    // Constructors
    // ============================================================

    public DepartmentHome( Schema schema )
        throws SQLException
    {
        super( schema, Department.class, "Department" );
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    @FauxjoPrimaryFinder
    public Department findByDepartmentId( long departmentId )
        throws SQLException
    {
        PreparedStatement statement = getConnection().prepareStatement( buildBasicSelect(
            "where departmentId=?" ) );
        statement.setLong( 1, departmentId );

        return getOne( statement.executeQuery() );
    }

    public Department findByDepartmentIdNotPrepared( long departmentId )
        throws SQLException
    {
        Statement statement = getConnection().createStatement();

        return getOne( statement.executeQuery( "select * from Department where departmentId=" +
            departmentId ) );
    }
}

