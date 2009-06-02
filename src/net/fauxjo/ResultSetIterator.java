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

package net.fauxjo;

import java.sql.*;
import java.util.*;

public class ResultSetIterator < T extends Fauxjo > implements Iterator<T>, Iterable<T>
{
    // ============================================================
    // Fields
    // ============================================================

    private SQLProcessor<T> _sqlProcessor;
    private ResultSet _resultSet;
    private boolean _hasNext;

    // ============================================================
    // Constructors
    // ============================================================

    public ResultSetIterator( SQLProcessor<T> sqlProcessor, ResultSet resultSet )
        throws SQLException
    {
        _sqlProcessor = sqlProcessor;
        _resultSet = resultSet;
        _hasNext = _resultSet.next();
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public boolean hasNext()
    {
        return _hasNext;
    }

    public T next()
    {
        if ( !_hasNext )
        {
            return null;
        }

        try
        {
            T object = _sqlProcessor.convertResultSetRow( _resultSet );
            _hasNext = _resultSet.next();
            if ( !_hasNext )
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

    public void remove()
    {
        throw new UnsupportedOperationException( "Remove is not supported for " +
            "ResultSetIterators." );
    }

    public void close()
        throws SQLException
    {
        if ( _resultSet != null )
        {
            _resultSet.close();
            _resultSet = null;
        }
    }

    public void finalize()
        throws Throwable
    {
        close();
        super.finalize();
    }

    public Iterator<T> iterator()
    {
        return this;
    }
}

