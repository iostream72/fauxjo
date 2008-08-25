//
// Coercer
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

package net.fauxjo;

import java.util.*;
import net.fauxjo.coercer.*;
import net.jextra.log.*;

/**
 * Coerces one value type to another value type.
 */
public class Coercer
{
    // ============================================================
    // Fields
    // ============================================================

    private Map<Class<?>,TypeCoercer<?>> _coercerMap = new HashMap<Class<?>,TypeCoercer<?>>();

    // ============================================================
    // Constructors
    // ============================================================

    public Coercer()
    {
        _coercerMap.put( Object.class, new ObjectCoercer() );
        _coercerMap.put( String.class, new StringCoercer() );
        _coercerMap.put( Byte.class, new ByteCoercer() );
        _coercerMap.put( Short.class, new ShortCoercer() );
        _coercerMap.put( Integer.class, new IntegerCoercer() );
        _coercerMap.put( Long.class, new LongCoercer() );
        _coercerMap.put( Float.class, new FloatCoercer() );
        _coercerMap.put( Double.class, new DoubleCoercer() );
        _coercerMap.put( java.util.Date.class, new UtilDateCoercer() );
        _coercerMap.put( java.sql.Date.class, new SQLDateCoercer() );
        _coercerMap.put( java.sql.Timestamp.class, new SQLTimestampCoercer() );
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------


    public < T > void putCoercer( Class<T> coercerClass, TypeCoercer<T> coercer )
    {
        _coercerMap.put( ( Class<?> ) coercerClass, ( TypeCoercer<?> ) coercer );
    }

    @SuppressWarnings( "unchecked" )
    public < T > TypeCoercer<T> getCoercer( Class<T> coercerClass )
    {
        return( TypeCoercer<T> ) _coercerMap.get( coercerClass );
    }

    @SuppressWarnings( "unchecked" )
    public < T > Object coerce( T value, Class<?> destClass )
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

        TypeCoercer<T> coercer = ( TypeCoercer<T> ) _coercerMap.get( value.getClass() );

        // use default coercer if none is found
        if ( coercer == null )
        {
            Log.info( "No coercer for class " + value.getClass() + "." );
            coercer = ( TypeCoercer<T> ) _coercerMap.get( Object.class );
        }

        return coercer.coerce( value, destClass );
    }
}

