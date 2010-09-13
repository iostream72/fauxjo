//
// Fauxjo
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


/**
 * <p>
 * Absolute minimum required representation of a Fauxjo bean. A fauxjo bean is a class that
 * represents a row within a specific database table.
 * </p><p>
 * The {@link Schema} is set on the Fauxjo object via the {@link Home} object. The {@link Schema}
 * is used for any foreign key lookups.
 * </p><p>
 * Note: A Fauxjo implementation should also have a special hashCode and equals method that take
 * into account two objects that represent the same record in the database (e.g. same primary key).
 * </p>
 */
public interface Fauxjo
{
    /**
     * <p>
     * Return true if this fauxjo does not represent an actual row in the database. This can usually
     * be tested by checking the primary key values which should most likely be null if it is not
     * in the database (auto-generated).
     * </p>
     */
    public boolean isInDatabase()
        throws SQLException;
}

