//
// SQLProcessor
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

public interface SQLProcessor<T extends FauxjoInterface>
{
    /**
     * Create an item from the first row in the ResultSet or return null if empty ResultSet.
     */
    T getFirst( ResultSet rs )
        throws SQLException;

    /**
     * Create an item from the first row in the ResultSet or return null or throw exception if empty ResultSet.
     */
    T getFirst( ResultSet rs, boolean errorIfEmpty )
        throws SQLException;

    /**
     * Create ONLY item from the ResultSet or return null if empty ResultSet.
     */
    T getUnique( ResultSet rs )
        throws SQLException;

    /**
     * Create ONLY item from the ResultSet or return null or throw exception if empty ResultSet.
     */
    T getUnique( ResultSet rs, boolean errorIfEmpty )
        throws SQLException;

    /**
     * Get first item from result-set.
     */
    T getFirst( ResultSet rs, boolean errorIfEmpty, boolean errorIfNotUnique )
        throws SQLException;

    /**
     * Convert each row in the result set to an item.
     */
    List<T> getList( ResultSet rs )
        throws SQLException;

    /**
     * Convert each row in the result set to an item, limited to maxNumItems.
     */
    List<T> getList( ResultSet rs, int maxNumItems )
        throws SQLException;

    /**
     * Convert each row in the result to an item and return as a Set.
     */
    Set<T> getSet( ResultSet rs )
        throws SQLException;

    /**
     * Convert each row in the result set limited to maxNumItems and return as a Set.
     */
    Set<T> getSet( ResultSet rs, int maxNumItems )
        throws SQLException;

    ResultSetIterator<T> getIterator( ResultSet rs )
        throws SQLException;

    /**
     * Convert the bean into an insert statement and execute it.
     */
    boolean insert( T bean )
        throws SQLException;

    /**
     * Convert the bean into an update statement and execute it.
     */
    int update( T bean )
        throws SQLException;

    /**
     * Convert the bean into an delete statement and execute it.
     */
    boolean delete( T bean )
        throws SQLException;

    /**
     * Get the schema associated with this {@link SQLProcessor}.
     * 
     * @return
     */
    Schema getSchema();

    /**
     * Build a basic select statement for the underlying table or tables with the given clause.
     * 
     * @param clause
     *            The where clause to build the statement with.
     * @return
     * @throws SQLException
     */
    String buildBasicSelect( String clause );

    T convertResultSetRow( ResultSet rs )
        throws SQLException;

    PreparedStatement getInsertStatement( T bean )
        throws SQLException;

    void setInsertValues( PreparedStatement statement, T bean )
        throws SQLException;

    PreparedStatement getUpdateStatement()
        throws SQLException;

    void setUpdateValues( PreparedStatement statement, T bean )
        throws SQLException;

    PreparedStatement getDeleteStatement()
        throws SQLException;

    void setDeleteValues( PreparedStatement statement, T bean )
        throws SQLException;

}
