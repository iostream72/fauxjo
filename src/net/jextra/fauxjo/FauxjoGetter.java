//
// FauxjoGetter
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

import java.lang.annotation.*;

/**
 * <p>
 * Optional annotation placed on a method of a Fauxjo beans that represents the getter for a
 * column in the database when creating an INSERT, UPDATE, and DELETE statement.
 * </p><p>
 * By default, the {@link Home} object locates the 'get' method by searching for a method with the
 * standard name getXYZ (or isXYZ) where XYZ is the name of the column in the
 * database (case insensitive).
 * </p>
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.METHOD )
public @interface FauxjoGetter
{
    // Column in the database for insert,update statements.
    String value();
}
