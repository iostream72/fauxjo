//
// Person
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

import net.fauxjo.*;

public class Person extends FauxjoImpl
{
    // ============================================================
    // Fields
    // ============================================================

    private String _personId;
    private String _firstName;
    private String _lastName;
    private String _address;
    private java.util.Date _entryDate;
    private java.util.Date _entryTimestamp;
    private Long _departmentId;

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    @FauxjoPrimaryKey
    public String getPersonId()
    {
        return _personId;
    }

    public void setPersonId( String personId )
    {
        _personId = personId;
    }

    public String getFirstName()
    {
        return _firstName;
    }

    public void setFirstName( String firstName )
    {
        _firstName = firstName;
    }

    public String getLastName()
    {
        return _lastName;
    }

    public void setLastName( String lastName )
    {
        _lastName = lastName;
    }

    public String getAddress()
    {
        return _address;
    }

    public void setAddress( String address )
    {
        _address = address;
    }

    public java.util.Date getEntryDate()
    {
        return _entryDate;
    }

    public void setEntryDate( java.util.Date entryDate )
    {
        _entryDate = entryDate;
    }

    public java.util.Date getEntryTimestamp()
    {
        return _entryTimestamp;
    }

    public void setEntryTimestamp( java.util.Date entryTimestamp )
    {
        _entryTimestamp = entryTimestamp;
    }

    public Long getDepartmentId()
    {
        return _departmentId;
    }

    public void setDepartmentId( Long departmentId )
    {
        _departmentId = departmentId;
    }

    public Department getDepartment()
    {
        return getSchema().getForeignBean( Department.class, _departmentId );
    }

    public void setDepartment( Department department )
    {
        _departmentId = department == null ? null : department.getDepartmentId();
    }

    public int calculateLength()
    {
        return _firstName.length();
    }

    public String getFullName()
    {
        return _firstName + " " + _lastName;
    }
}

