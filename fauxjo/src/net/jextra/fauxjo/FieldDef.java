//
// FieldDef
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

import java.lang.reflect.*;

public class FieldDef
{
    // ============================================================
    // Fields
    // ============================================================

    private Field _field;
    private Method _writeMethod;
    private Method _readMethod;
    private Class<?> _valueClass;
    private boolean _primaryKey;
    private String _primaryKeySequenceName;

    // ============================================================
    // Constructors
    // ============================================================

    public Field getField()
    {
        return _field;
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public void setField( Field field )
        throws FauxjoException
    {
        _field = field;

        if ( _valueClass == null )
        {
            _valueClass = field.getType();
        }
        else
        {
            if ( !_valueClass.equals( field.getType() ) )
            {
                throw new FauxjoException( "Field [" + field.getName() + "] must have type of ["
                    + _valueClass.getCanonicalName() + "]" );
            }
        }
    }

    public Method getWriteMethod()
    {
        return _writeMethod;
    }

    public void setWriteMethod( Method writeMethod )
        throws FauxjoException
    {
        _writeMethod = writeMethod;

        if ( _valueClass == null )
        {
            _valueClass = writeMethod.getParameterTypes()[0];
        }
        else
        {
            if ( !_valueClass.equals( writeMethod.getParameterTypes()[0] ) )
            {
                throw new FauxjoException( "Write method [" + writeMethod.getName()
                    + "] must have first argument of type [" + _valueClass.getCanonicalName() + "]" );
            }
        }
    }

    public Method getReadMethod()
    {
        return _readMethod;
    }

    public void setReadMethod( Method readMethod )
        throws FauxjoException
    {
        _readMethod = readMethod;

        if ( _valueClass == null )
        {
            _valueClass = readMethod.getReturnType();
        }
        else
        {
            if ( !_valueClass.equals( readMethod.getReturnType() ) )
            {
                throw new FauxjoException( "Read method [" + readMethod.getName() + "] must have return type of ["
                    + _valueClass.getCanonicalName() + "]" );
            }
        }
    }

    public boolean isPrimaryKey()
    {
        return _primaryKey;
    }

    public void setPrimaryKey( boolean primaryKey )
    {
        _primaryKey = primaryKey;
    }

    public String getPrimaryKeySequenceName()
    {
        return _primaryKeySequenceName;
    }

    public void setPrimaryKeySequenceName( String primaryKeySequenceName )
    {
        _primaryKeySequenceName = primaryKeySequenceName;
    }

    public Class<?> getValueClass()
    {
        return _valueClass;
    }

    public void setValueClass( Class<?> valueClass )
    {
        _valueClass = valueClass;
    }
}
