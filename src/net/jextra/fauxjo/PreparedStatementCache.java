//
// PreparedStatementCache
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

import java.lang.ref.*;
import java.sql.*;
import java.util.*;

/**
 * <p>
 * {@link PreparedStatement} cache where the {@link PreparedStatement}s are stored per
 * {@link Thread}:{@link Connection} combo.
 * </p><p>
 * {@link PreparedStatement}'s are not thread-safe and certainly can not be shared between
 * {@link Connection}s.
 * {@link Connection}s on-the-other-hand may be thread-safe (depends on implementation).
 * Therefore, in order to guarentee thread and connection boundary safety, both are used as keys
 * in the cache.
 * </p>
 */
public class PreparedStatementCache
{
    // ============================================================
    // Fields
    // ============================================================

    private WeakHashMap<Thread, PreparedStatementCacheThreadData> cache;

    // A listener thread that waits for Thread to finish then throws away the
    // PreparedStatements for that Thread.
    private HashMap<Thread, Thread> listenerMap;

    // ============================================================
    // Constructors
    // ============================================================

    public PreparedStatementCache()
    {
        cache = new WeakHashMap<Thread, PreparedStatementCacheThreadData>();
        listenerMap = new HashMap<Thread, Thread>();
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public PreparedStatement prepareStatement( Connection connection, String sql )
        throws SQLException
    {
        PreparedStatementCacheThreadData threadData = getOrCreateThreadData( Thread.currentThread() );

        PreparedStatementGroup group = threadData.get( connection );
        if ( group == null )
        {
            group = new PreparedStatementGroup();
            threadData.put( connection, group );
        }

        return group.getPreparedStatement( connection, sql );
    }

    public CallableStatement prepareCall( Connection connection, String sql )
        throws SQLException
    {
        PreparedStatementCacheThreadData threadData = getOrCreateThreadData( Thread.currentThread() );

        PreparedStatementGroup group = threadData.get( connection );
        if ( group == null )
        {
            group = new PreparedStatementGroup();
            threadData.put( connection, group );
        }

        return group.getCallableStatement( connection, sql );
    }

    /**
     * Close all known PreparedStatements.
     * @throws SQLException
     */
    public void clearAll()
        throws SQLException
    {
        for ( Thread thread : cache.keySet() )
        {
            clearThread( thread );
        }
    }

    /**
     * Used to close any connections for a particular thread.
     * @throws SQLException
     */
    @SuppressWarnings( "deprecation" )
    public void clearThread( Thread thread )
        throws SQLException
    {
        //
        // Kill any ThreadListeners associated with this thread.
        //
        Thread listenerThread = listenerMap.get( thread );
        listenerMap.remove( thread );
        if ( listenerThread != null )
        {
            // Force a stop on the listening thread.
            listenerThread.stop();
        }

        //
        // Remove any PreparedStatements associated with this Thread
        //
        PreparedStatementCacheThreadData threadData = cache.get( thread );
        if ( threadData != null )
        {
            for ( PreparedStatementGroup group : threadData.values() )
            {
                group.close();
            }
        }
        cache.remove( thread );
    }

    @Override
    public void finalize()
    {
        try
        {
            clearAll();
        }
        catch ( SQLException ex )
        {
            // ignore
        }
    }

    // ----------
    // protected
    // ----------

    /**
     * Get the thread data for the given thread. If none is found yet, create it and add a listener
     * to clear the cached statements out when the thread dies.
     */
    protected PreparedStatementCacheThreadData getOrCreateThreadData( Thread thread )
    {
        PreparedStatementCacheThreadData threadData = cache.get( thread );
        if ( threadData == null )
        {
            threadData = new PreparedStatementCacheThreadData();
            cache.put( thread, threadData );

            // Listen to the Thread and close PreparedStatement when it is gone.
            Thread listenerThread = listenerMap.get( Thread.currentThread() );
            if ( listenerThread == null )
            {
                PreparedStatementCacheThreadListener threadListener = new PreparedStatementCacheThreadListener(
                    Thread.currentThread() );
                listenerThread = new Thread( threadListener );
                listenerThread.setDaemon( true );
                listenerMap.put( Thread.currentThread(), listenerThread );
                listenerThread.start();
            }
        }

        return threadData;
    }

    // ============================================================
    // Inner Classes
    // ============================================================

    private class PreparedStatementCacheThreadListener implements Runnable
    {
        Thread thread;

        public PreparedStatementCacheThreadListener( Thread thread )
        {
            this.thread = thread;
        }

        @Override
        public void run()
        {
            try
            {
                // Wait for the parent thread to finish.
                thread.join();

                listenerMap.remove( thread );
                clearThread( thread );
            }
            catch ( Exception ex )
            {
                throw new RuntimeException( ex );
            }
        }
    }

    private class PreparedStatementCacheThreadData extends WeakHashMap<Connection, PreparedStatementGroup>
    {
    }

    private class PreparedStatementGroup
    {
        // Cached prepared statements for connections.
        // key = sql String
        private Map<String, SoftReference<PreparedStatement>> map;

        public PreparedStatementGroup()
        {
            map = new HashMap<String, SoftReference<PreparedStatement>>();
        }

        public PreparedStatement getPreparedStatement( Connection conn, String sql )
            throws SQLException
        {
            SoftReference<PreparedStatement> ref = map.get( sql );
            if ( ref == null || ref.get() == null )
            {
                PreparedStatement statement = conn.prepareStatement( sql );
                map.put( sql, new SoftReference<PreparedStatement>( statement ) );
                return statement;
            }
            else
            {
                return ref.get();
            }
        }

        public CallableStatement getCallableStatement( Connection conn, String sql )
            throws SQLException
        {
            SoftReference<PreparedStatement> ref = map.get( sql );
            if ( ref == null || ref.get() == null )
            {
                CallableStatement statement = conn.prepareCall( sql );
                map.put( sql, new SoftReference<PreparedStatement>( statement ) );
                return statement;
            }
            else
            {
                return (CallableStatement) ref.get();
            }
        }

        public void close()
            throws SQLException
        {
            for ( SoftReference<PreparedStatement> ref : map.values() )
            {
                PreparedStatement statement = ref.get();
                if ( statement != null )
                {
                    statement.close();
                }
            }
        }
    }
}
