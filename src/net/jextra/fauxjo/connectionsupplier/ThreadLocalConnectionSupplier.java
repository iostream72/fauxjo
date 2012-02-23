//
// ThreadLocalConnectionSupplier
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

/**
 * <p>
 * Stored an independent {@link Connection} for each {@link Thread}. Useful for web-development which uses caches.
 * </p><p>
 * Example setup:
 * <pre>
 *    InitialContext cxt = new InitialContext();
 *    DataSource ds = (DataSource) cxt.lookup( "java:/comp/env/jdbc/xyz/abc" );
 *    Connection conn = ds.getConnection();
 *    connectionProvider.setConnection( conn );
 * </pre>
 * Example cleanup:
 * <pre>
 *    connectionProvider.getConnection().close();
 *    connectionProvider.removeConnection();
 * </pre>
 * </p>
 */
public class ThreadLocalConnectionSupplier implements ConnectionSupplier
{
    // ============================================================
    // Fields
    // ============================================================

    private ThreadLocal<Connection> _connections;

    // ============================================================
    // Constructors
    // ============================================================

    public ThreadLocalConnectionSupplier()
    {
        _connections = new ThreadLocal<Connection>();
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public void setConnection( Connection conn )
    {
        if ( conn == null )
        {
            removeConnection();
        }
        else
        {
            _connections.set( conn );
        }
    }

    public void removeConnection()
    {
        _connections.remove();
    }

    @Override
    public Connection getConnection()
    {
        return _connections.get();
    }
}
