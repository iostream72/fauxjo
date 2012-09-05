//
// UtilDateCoercer
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

package net.jextra.fauxjo.coercer;

import java.sql.*;
import net.jextra.fauxjo.*;

public class UtilDateCoercer implements TypeCoercer<java.util.Date>
{
    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    @Override
    public Object coerce( java.util.Date value, Class<?> destClass )
        throws FauxjoException
    {
        if ( destClass.equals( java.sql.Date.class ) )
        {
            return new java.sql.Date( value.getTime() );
        }
        else if ( destClass.equals( Timestamp.class ) )
        {
            return new Timestamp( value.getTime() );
        }

        throw new FauxjoException( "The UtilDateCoercer does not know how to convert to type " +
            destClass.getCanonicalName() );
    }

}
