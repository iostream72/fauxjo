//
// AbstractSQLProcessor
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
 * AbstractSQLProcessor is a base class of the various SQLProcessor classes and does all the shared stuff that doesn't
 * depend on the underlying table structure.
 *
 * @param <T> The {@link Fauxjo} bean class this processor works with.
 */
public abstract class AbstractSQLProcessor<T extends Fauxjo> implements SQLProcessor<T>
{
    // ============================================================
    // Fields
    // ============================================================

	private ResultSetRecordProcessor<T> _recordProcessor;

    // ============================================================
    // Constructors
    // ============================================================

	public AbstractSQLProcessor( ResultSetRecordProcessor<T> recordProcessor )
	{
		_recordProcessor = recordProcessor;
	}

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

	@Override
	public T getFirst( ResultSet rs )
		throws SQLException
	{
		return getFirst( rs, false, false );
	}

	@Override
	public T getFirst( ResultSet rs, boolean errorIfEmpty )
		throws SQLException
	{
		return getFirst( rs, errorIfEmpty, false );
	}

	@Override
	public T getUnique( ResultSet rs )
		throws SQLException
	{
		return getFirst( rs, false, true );
	}

	@Override
	public T getUnique( ResultSet rs, boolean errorIfEmpty )
		throws SQLException
	{
		return getFirst( rs, errorIfEmpty, true );
	}

	@Override
	public T getFirst( ResultSet rs, boolean errorIfEmpty, boolean errorIfNotUnique )
		throws SQLException
	{
		List<T> beans = processResultSet( rs, 2 );
        if ( beans == null || beans.isEmpty() )
        {
            if ( errorIfEmpty )
            {
                throw new FauxjoException( "ResultSet is improperly empty." );
            }

            return null;
        }

        if ( errorIfNotUnique && beans.size() != 1 )
        {
            throw new FauxjoException( "ResultSet improperly contained more than one item." );
        }

        return beans.get( 0 );
	}

	@Override
	public List<T> getList( ResultSet rs )
		throws SQLException
	{
		return getList( rs, Integer.MAX_VALUE );
	}

	@Override
	public List<T> getList( ResultSet rs, int maxNumItems )
		throws SQLException
	{
		return processResultSet( rs, maxNumItems );
	}

	@Override
    public ResultSetIterator<T> getIterator( ResultSet rs )
        throws SQLException
    {
        ResultSetIterator<T> iterator = new ResultSetIterator<T>( _recordProcessor, rs );

        return iterator;
    }

    // ----------
    // protected
    // ----------

	protected List<T> processResultSet( ResultSet rs, int numRows )
		throws SQLException
    {
        List<T> list = new ArrayList<T>();

        int counter = 0;
        while ( rs.next() && ( counter < numRows ) )
        {
            list.add( _recordProcessor.convertResultSetRow( rs ) );
            counter++;
        }
        rs.close();

        return list;
    }

	protected ResultSetRecordProcessor<T> getResultSetRecordProcessor()
	{
		return _recordProcessor;
	}
}
