//
// SimpleConnectionSupplier
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

package net.jextra.fauxjo.connectionsupplier;

import java.sql.*;
import javax.sql.*;

/**
 * <p>
 * Essentially a pass-through {@link ConnectionSupplier} for a single {@link Connection} object.
 * </p><p>
 * This is useful
 * for thick Swing applications where Swing threads should get same connection as the main thread.
 * </p>
 */
public class SimpleConnectionSupplier implements ConnectionSupplier
{
    // ============================================================
    // Fields
    // ============================================================

    private Connection connection;

    // ============================================================
    // Constructors
    // ============================================================

    public SimpleConnectionSupplier()
    {
    }

    public SimpleConnectionSupplier( Connection conn )
    {
        connection = conn;
    }

    public SimpleConnectionSupplier( DataSource ds )
        throws SQLException
    {
        connection = ds.getConnection();
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public void setDataSource( DataSource ds )
        throws SQLException
    {
        connection = ds.getConnection();
    }

    public void setConnection( Connection conn )
    {
        connection = conn;
    }

    @Override
    public Connection getConnection()
    {
        return connection;
    }

    @Override
    public boolean closeConnection()
        throws SQLException
    {
        if ( connection == null )
        {
            return false;
        }

        connection.close();
        connection = null;

        return true;
    }
}
