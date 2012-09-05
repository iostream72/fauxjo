//
// ResultSetIterator
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

import java.sql.*;
import java.util.*;

/**
 * Iterator of a {@link ResultSet} that knows how to convert each row in the ResultSet to a
 * Fauxjo bean. This is used primarily to iterate over a large number records without having to
 * load them all into memory.
 */
public class ResultSetIterator<T extends Fauxjo> implements Iterator<T>, Iterable<T>
{
    // ============================================================
    // Fields
    // ============================================================

    private ResultSetRecordProcessor<T> sqlProcessor;
    private ResultSet resultSet;
    private boolean hasNext;

    // ============================================================
    // Constructors
    // ============================================================

    public ResultSetIterator( ResultSetRecordProcessor<T> sqlProcessor, ResultSet resultSet )
        throws SQLException
    {
        this.sqlProcessor = sqlProcessor;
        this.resultSet = resultSet;
        hasNext = resultSet.next();
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    @Override
    public boolean hasNext()
    {
        return hasNext;
    }

    @Override
    public T next()
    {
        if ( !hasNext )
        {
            return null;
        }

        try
        {
            T object = sqlProcessor.convertResultSetRow( resultSet );
            hasNext = resultSet.next();
            if ( !hasNext )
            {
                close();
            }

            return object;
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( ex );
        }
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException( "Remove is not supported for " + "ResultSetIterators." );
    }

    public void close()
        throws SQLException
    {
        if ( resultSet != null )
        {
            resultSet.close();
            resultSet = null;
        }
    }

    @Override
    public void finalize()
        throws Throwable
    {
        close();
        super.finalize();
    }

    @Override
    public Iterator<T> iterator()
    {
        return this;
    }
}
