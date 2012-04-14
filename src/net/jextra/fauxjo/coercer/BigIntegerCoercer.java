//
// BigIntegerCoercer
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

import java.math.*;
import net.jextra.fauxjo.*;

public class BigIntegerCoercer implements TypeCoercer<BigInteger>
{
    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    @Override
    public Object coerce( BigInteger value, Class<?> destClass )
        throws FauxjoException
    {
        if ( destClass.equals( Byte.class ) )
        {
            return value.byteValue();
        }
        else if ( destClass.equals( Short.class ) )
        {
            return value.shortValue();
        }
        else if ( destClass.equals( Integer.class ) )
        {
            return value.intValue();
        }
        else if ( destClass.equals( Long.class ) )
        {
            return value.longValue();
        }

        throw new FauxjoException( "The BigInteger does not know how to convert to type " + destClass );
    }

}
