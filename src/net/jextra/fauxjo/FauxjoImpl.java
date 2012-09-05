//
// FauxjoImpl
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

import java.beans.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * <p>
 * Concrete implementation of {@link Fauxjo} interface that is {@link FauxjoPrimaryKey} annotation aware.
 * </p>
 * <p>
 * This implementation assumes for that to test if the object is already in the database or not that
 * {@link #isInDatabase()} just returns the inverse value calculated via {@link #hasEmptyPrimaryKey()}.
 * </p>
 * <p>
 * Note: This implementation overrides the {@code hashCode} and {@code equals} methods in order to compare rows in the
 * database properly (e.g. same primary key).
 * </p>
 */
public abstract class FauxjoImpl implements Fauxjo
{
    // ============================================================
    // Fields
    // ============================================================

    private static HashMap<Class<?>, FauxjoBeanDef> caches;

    // ============================================================
    // Constructors
    // ============================================================

    static
    {
        caches = new HashMap<Class<?>, FauxjoBeanDef>();
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    @Override
    public Map<String, FieldDef> extractFieldDefs()
        throws FauxjoException
    {
        try
        {
            FauxjoBeanDef beanDef = getFauxjoBeanDef();

            return beanDef.getFieldDefs();
        }
        catch ( Exception ex )
        {
            if ( ex instanceof FauxjoException )
            {
                throw (FauxjoException) ex;
            }

            throw new FauxjoException( ex );
        }
    }

    @Override
    public Object readValue( String key )
        throws FauxjoException
    {
        try
        {
            FauxjoBeanDef beanDef = getFauxjoBeanDef();

            Field field = beanDef.getField( key );
            if ( field != null )
            {
                field.setAccessible( true );
                return field.get( this );
            }

            Method readMethod = beanDef.getReadMethod( key );
            if ( readMethod != null )
            {
                return readMethod.invoke( this, new Object[0] );
            }
        }
        catch ( Exception ex )
        {
            if ( ex instanceof FauxjoException )
            {
                throw (FauxjoException) ex;
            }

            throw new FauxjoException( ex );
        }

        return null;
    }

    @Override
    public void writeValue( String key, Object value )
        throws FauxjoException
    {
        try
        {
            FauxjoBeanDef beanDef = getFauxjoBeanDef();

            Field field = beanDef.getField( key );
            if ( field != null )
            {
                try
                {
                    field.setAccessible( true );
                    field.set( this, value );
                    return;
                }
                catch ( Exception ex )
                {
                    throw new FauxjoException( "Unable to write to field [" + field.getName() + "]", ex );
                }
            }

            Method writeMethod = beanDef.getWriteMethod( key );
            if ( writeMethod != null )
            {
                try
                {
                    writeMethod.invoke( this, value );
                }
                catch ( Exception ex )
                {
                    throw new FauxjoException( "Unable to invoke write method [" + writeMethod.getName() + "]", ex );
                }
            }
        }
        catch ( Exception ex )
        {
            if ( ex instanceof FauxjoException )
            {
                throw (FauxjoException) ex;
            }

            throw new FauxjoException( ex );
        }
    }

    /**
     * @return true if object is already in the database. The default behavior is to return the inverse value of
     *         {@link #hasEmptyPrimaryKey(Fauxjo)}. This should be overridden if that is not accurate for this Fauxjo
     *         bean.
     */
    @Override
    public boolean isInDatabase()
        throws FauxjoException
    {
        return !hasEmptyPrimaryKey();
    }

    @Override
    public int hashCode()
    {
        try
        {
            List<Object> keys = getPrimaryKeyValues();

            // Any null keys equate to default of zero.
            if ( keys == null )
            {
                return 0;
            }

            // Just sum up the key item hashCodes for a final hashCode
            int hashCode = 0;
            for ( Object item : keys )
            {
                hashCode += item == null ? 0 : item.hashCode();
            }

            return hashCode;
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( ex );
        }
    }

    @Override
    public boolean equals( Object otherObj )
    {
        try
        {
            // If same object, just quickly return true.
            if ( this == otherObj )
            {
                return true;
            }

            // If other object is not same class as this object, quickly return false.
            if ( otherObj == null || !otherObj.getClass().equals( getClass() ) )
            {
                return false;
            }

            FauxjoImpl other = (FauxjoImpl) otherObj;

            List<Object> keys1 = getPrimaryKeyValues();
            List<Object> keys2 = other.getPrimaryKeyValues();

            // If the primary keys somehow are different lengths, they must not be the same.
            if ( keys1.size() != keys2.size() )
            {
                return false;
            }

            // Check each key item, if ever different, return false;
            for ( int i = 0; i < keys1.size(); i++ )
            {
                Object item1 = keys1.get( i );
                Object item2 = keys2.get( i );

                if ( item1 == null && item2 != null )
                {
                    return false;
                }
                else if ( !item1.equals( item2 ) )
                {
                    return false;
                }
            }
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( ex );
        }

        return true;
    }

    // ----------
    // protected
    // ----------

    /**
     * @return Values of primary keys in a consistent order so that it can be compared to another Fauxjo beans.
     */
    protected List<Object> getPrimaryKeyValues()
        throws FauxjoException
    {
        try
        {
            // Arbitrarily ordered by keys.
            TreeMap<String, Object> keys = new TreeMap<String, Object>();

            Map<String, FieldDef> map = extractFieldDefs();
            for ( String key : map.keySet() )
            {
                FieldDef def = map.get( key );
                if ( def.isPrimaryKey() )
                {
                    if ( def.getField() != null )
                    {
                        keys.put( key, def.getField().get( this ) );
                    }
                    else
                    {
                        keys.put( key, def.getReadMethod().invoke( this, new Object[0] ) );
                    }
                }
            }

            if ( keys.size() == 0 )
            {
                return null;
            }

            return new ArrayList<Object>( keys.values() );
        }
        catch ( Exception ex )
        {
            if ( ex instanceof FauxjoException )
            {
                throw (FauxjoException) ex;
            }

            throw new FauxjoException( ex );
        }
    }

    /**
     * @return false if any of the primary key values are null. This is implied that it is not actually in the database.
     */
    protected boolean hasEmptyPrimaryKey()
        throws FauxjoException
    {
        List<Object> keys = getPrimaryKeyValues();

        // If no values, assume empty.
        if ( keys == null || keys.isEmpty() )
        {
            return true;
        }

        // If any value is null, empty.
        for ( Object key : keys )
        {
            if ( key == null )
            {
                return true;
            }
        }

        return false;
    }

    protected FauxjoBeanDef getFauxjoBeanDef()
        throws FauxjoException, IntrospectionException
    {
        FauxjoBeanDef beanDef = caches.get( getClass() );
        if ( beanDef != null )
        {
            return beanDef;
        }

        //
        // Was not cached, collect information.
        //
        beanDef = new FauxjoBeanDef();

        for ( Field field : getFauxjoFields( getClass() ) )
        {
            FauxjoField ann = field.getAnnotation( FauxjoField.class );
            String key = ann.value();

            beanDef.addField( key, field );

            //
            // Check if FauxjoPrimaryKey.
            //
            if ( field.isAnnotationPresent( FauxjoPrimaryKey.class ) )
            {
                beanDef.getFieldDef( key ).setPrimaryKey( true );

                FauxjoPrimaryKey pkAnn = (FauxjoPrimaryKey) field.getAnnotation( FauxjoPrimaryKey.class );
                if ( pkAnn.value() != null && !pkAnn.value().trim().isEmpty() )
                {
                    beanDef.getFieldDef( key ).setPrimaryKeySequenceName( pkAnn.value().trim() );
                }
            }
        }

        BeanInfo info = Introspector.getBeanInfo( getClass() );
        for ( PropertyDescriptor prop : info.getPropertyDescriptors() )
        {
            if ( prop.getWriteMethod() != null )
            {
                FauxjoSetter ann = prop.getWriteMethod().getAnnotation( FauxjoSetter.class );
                if ( ann != null )
                {
                    String key = ann.value();

                    Field field = beanDef.getField( key );
                    if ( field != null )
                    {
                        throw new FauxjoException( "FauxjoSetter defined on method where a FauxjoField " +
                            "already defines the link to the column [" + key + "]" );
                    }

                    beanDef.addWriteMethod( key, prop.getWriteMethod() );
                }
            }

            if ( prop.getReadMethod() != null )
            {
                FauxjoGetter ann = prop.getReadMethod().getAnnotation( FauxjoGetter.class );
                if ( ann != null )
                {
                    String key = ann.value();

                    Field field = beanDef.getField( key );
                    if ( field != null )
                    {
                        throw new FauxjoException( "FauxjoGetter defined on method where a FauxjoField " +
                            "already defines the link to the column [" + key + "]" );
                    }

                    beanDef.addReadMethod( key, prop.getReadMethod() );

                    //
                    // Check if FauxjoPrimaryKey.
                    //
                    if ( prop.getReadMethod().isAnnotationPresent( FauxjoPrimaryKey.class ) )
                    {
                        beanDef.getFieldDef( key ).setPrimaryKey( true );

                        FauxjoPrimaryKey pkAnn = (FauxjoPrimaryKey) prop.getReadMethod().getAnnotation(
                            FauxjoPrimaryKey.class );
                        if ( pkAnn.value() != null && !pkAnn.value().trim().isEmpty() )
                        {
                            beanDef.getFieldDef( key ).setPrimaryKeySequenceName( pkAnn.value().trim() );
                        }
                    }
                }
            }
        }

        // Put in cacche 
        caches.put( getClass(), beanDef );

        return beanDef;
    }

    // ----------
    // private
    // ----------

    private Collection<Field> getFauxjoFields( Class<?> cls )
    {
        ArrayList<Field> list = new ArrayList<Field>();

        if ( cls == null )
        {
            return list;
        }

        // Add super-classes fields first.
        list.addAll( getFauxjoFields( cls.getSuperclass() ) );

        for ( Field field : cls.getDeclaredFields() )
        {
            if ( field.isAnnotationPresent( FauxjoField.class ) )
            {
                list.add( field );
            }
        }

        return list;
    }

    // ============================================================
    // Inner Classes
    // ============================================================

    private static class FauxjoBeanDef
    {
        private Map<String, FieldDef> cache;

        public FauxjoBeanDef()
        {
            cache = new TreeMap<String, FieldDef>();
        }

        public void addField( String key, Field field )
            throws FauxjoException
        {
            getFieldDef( key ).setField( field );
        }

        public Field getField( String key )
        {
            return getFieldDef( key ).getField();
        }

        public void addReadMethod( String key, Method method )
            throws FauxjoException
        {
            getFieldDef( key ).setReadMethod( method );
        }

        public Method getReadMethod( String key )
        {
            return getFieldDef( key ).getReadMethod();
        }

        public void addWriteMethod( String key, Method method )
            throws FauxjoException
        {
            getFieldDef( key ).setWriteMethod( method );
        }

        public Method getWriteMethod( String key )
        {
            return getFieldDef( key ).getWriteMethod();
        }

        private Map<String, FieldDef> getFieldDefs()
        {
            TreeMap<String, FieldDef> map = new TreeMap<String, FieldDef>();

            for ( String key : cache.keySet() )
            {
                map.put( key, getFieldDef( key ) );
            }

            return map;
        }

        private FieldDef getFieldDef( String key )
        {
            FieldDef def = cache.get( key.toLowerCase() );
            if ( def == null )
            {
                def = new FieldDef();
                cache.put( key.toLowerCase(), def );
            }

            return def;
        }
    }
}
