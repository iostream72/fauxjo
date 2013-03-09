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

    private Field field;
    private Method writeMethod;
    private Method readMethod;
    private Class<?> valueClass;
    private boolean primaryKey;
    private String primaryKeySequenceName;

    // ============================================================
    // Constructors
    // ============================================================

    public Field getField()
    {
        return field;
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
        this.field = field;

        if ( valueClass == null )
        {
            valueClass = field.getType();
        }
        else
        {
            if ( !valueClass.equals( field.getType() ) )
            {
                throw new FauxjoException( "Field [" + field.getName() + "] must have type of [" + valueClass.getCanonicalName() + "]" );
            }
        }
    }

    public Method getWriteMethod()
    {
        return writeMethod;
    }

    public void setWriteMethod( Method writeMethod )
        throws FauxjoException
    {
        this.writeMethod = writeMethod;

        if ( valueClass == null )
        {
            valueClass = writeMethod.getParameterTypes()[0];
        }
        else
        {
            if ( !valueClass.equals( writeMethod.getParameterTypes()[0] ) )
            {
                throw new FauxjoException( "Write method [" + writeMethod.getName() + "] must have first argument of type [" +
                    valueClass.getCanonicalName() + "]" );
            }
        }
    }

    public Method getReadMethod()
    {
        return readMethod;
    }

    public void setReadMethod( Method readMethod )
        throws FauxjoException
    {
        this.readMethod = readMethod;

        if ( valueClass == null )
        {
            valueClass = readMethod.getReturnType();
        }
        else
        {
            if ( !valueClass.equals( readMethod.getReturnType() ) )
            {
                throw new FauxjoException( "Read method [" + readMethod.getName() + "] must have return type of [" + valueClass.getCanonicalName() +
                    "]" );
            }
        }
    }

    public boolean isPrimaryKey()
    {
        return primaryKey;
    }

    public void setPrimaryKey( boolean primaryKey )
    {
        this.primaryKey = primaryKey;
    }

    @Deprecated
    public String getPrimaryKeySequenceName()
    {
        return primaryKeySequenceName;
    }

    @Deprecated
    public void setPrimaryKeySequenceName( String primaryKeySequenceName )
    {
        this.primaryKeySequenceName = primaryKeySequenceName;
    }

    public Class<?> getValueClass()
    {
        return valueClass;
    }

    public void setValueClass( Class<?> valueClass )
    {
        this.valueClass = valueClass;
    }
}
