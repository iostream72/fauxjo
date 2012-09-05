//
// Coercer
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

import java.math.*;
import java.util.*;
import net.jextra.fauxjo.coercer.*;

/**
 * General use tool that coerces one value type to another value type. For example from a
 * String to an Integer.
 */
public class Coercer
{
    // ============================================================
    // Fields
    // ============================================================

    private Map<Class<?>, TypeCoercer<?>> coercerMap = new HashMap<Class<?>, TypeCoercer<?>>();

    // ============================================================
    // Constructors
    // ============================================================

    public Coercer()
    {
        coercerMap.put( Object.class, new ObjectCoercer() );
        coercerMap.put( String.class, new StringCoercer() );
        coercerMap.put( Byte.class, new ByteCoercer() );
        coercerMap.put( Short.class, new ShortCoercer() );
        coercerMap.put( Integer.class, new IntegerCoercer() );
        coercerMap.put( Long.class, new LongCoercer() );
        coercerMap.put( BigInteger.class, new BigIntegerCoercer() );
        coercerMap.put( Float.class, new FloatCoercer() );
        coercerMap.put( Double.class, new DoubleCoercer() );
        coercerMap.put( java.util.Date.class, new UtilDateCoercer() );
        coercerMap.put( java.sql.Date.class, new SQLDateCoercer() );
        coercerMap.put( java.sql.Timestamp.class, new SQLTimestampCoercer() );
        coercerMap.put( UUID.class, new UUIDCoercer() );
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public <T> void addTypeCoercer( Class<T> coercerClass, TypeCoercer<T> coercer )
    {
        coercerMap.put( (Class<?>) coercerClass, (TypeCoercer<?>) coercer );
    }

    @SuppressWarnings( "unchecked" )
    public <T> TypeCoercer<T> getTypeCoercer( Class<T> coercerClass )
    {
        return (TypeCoercer<T>) coercerMap.get( coercerClass );
    }

    @SuppressWarnings( "unchecked" )
    public <T> Object coerce( T value, Class<?> destClass )
        throws FauxjoException
    {
        // Null values are just null values.
        if ( value == null )
        {
            return null;
        }

        if ( destClass.isPrimitive() )
        {
            if ( destClass == Boolean.TYPE )
            {
                destClass = Boolean.class;
            }
            else if ( destClass == Byte.TYPE )
            {
                destClass = Byte.class;
            }
            else if ( destClass == Character.TYPE )
            {
                destClass = Character.class;
            }
            else if ( destClass == Double.TYPE )
            {
                destClass = Double.class;
            }
            else if ( destClass == Float.TYPE )
            {
                destClass = Float.class;
            }
            else if ( destClass == Integer.TYPE )
            {
                destClass = Integer.class;
            }
            else if ( destClass == Long.TYPE )
            {
                destClass = Long.class;
            }
            else if ( destClass == Short.TYPE )
            {
                destClass = Short.class;
            }
        }

        // Short-circuit if destClass same as value class
        if ( value.getClass().equals( destClass ) )
        {
            return value;
        }

        TypeCoercer<T> coercer = (TypeCoercer<T>) coercerMap.get( value.getClass() );

        // use default coercer if none is found
        if ( coercer == null )
        {
            coercer = (TypeCoercer<T>) coercerMap.get( Object.class );
        }

        return coercer.coerce( value, destClass );
    }
}
