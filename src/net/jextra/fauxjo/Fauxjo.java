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

package net.jextra.fauxjo;

import java.util.*;

/**
 * Absolute minimum required representation of a Fauxjo bean. A fauxjo bean is a class that
 * represents a row within a specific database table.
 */
public interface Fauxjo
{
    /**
     * Return a Map of keys,ValueDef. The keys are the lowercase database column names. The ValueDef
     * tells if the column is a primary key or not and what Class the Fauxjo bean expects the
     * value to be.
     */
    Map<String, FieldDef> extractFieldDefs()
        throws FauxjoException;

    /**
     * @return Value from the bean for the given "key" = lowercase database column name. The Object
     * Class should be same as returned from {@link getValueDefs} for the given key.
     */
    Object readValue( String key )
        throws FauxjoException;

    /**
     * Set a value in the Fauxjo bean with the given "key" = lowercase database column name. The
     * Object Class will be the equal to whatever was returned from {@link getValueDefs} for the
     * given key.
     */
    void writeValue( String key, Object value )
        throws FauxjoException;

    /**
     * Tells whether or not this Fauxjo bean is already in the database or not. This is usually
     * done by checking the primary keys for null values.
     */
    boolean isInDatabase()
        throws FauxjoException;
}
