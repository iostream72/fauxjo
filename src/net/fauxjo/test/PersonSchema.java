//
// PersonSchema
//

package net.fauxjo.test;

import java.sql.*;
import java.util.*;
import net.fauxjo.*;

public class PersonSchema extends Schema
{
    // ============================================================
    // Fields
    // ============================================================

    private static WeakHashMap<Connection,PersonSchema> _instances;

    private Connection _connection;

    // ============================================================
    // Constructors
    // ============================================================

    public PersonSchema( Connection connection )
        throws SQLException
    {
        _connection = connection;
        addHome( Person.class, new PersonHome( this ) );
        addHome( Department.class, new DepartmentHome( this ) );
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public static PersonSchema getInstance( Connection connection )
        throws SQLException
    {
        if ( _instances == null )
        {
            _instances = new WeakHashMap<Connection,PersonSchema>();
        }

        PersonSchema instance = _instances.get( connection );
        if ( instance == null )
        {
            instance = new PersonSchema( connection );
            _instances.put( connection, instance );
        }

        return instance;
    }


    public Connection getConnection()
    {
        return _connection;
    }

    public PersonHome getPersonHome()
    {
        return (PersonHome)getHome( Person.class );
    }

    public DepartmentHome getDepartmentHome()
    {
        return (DepartmentHome)getHome( Department.class );
    }
}

