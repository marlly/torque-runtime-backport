package org.apache.torque.jdbc2pool.adapter;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and 
 *    "Apache Turbine" must not be used to endorse or promote products 
 *    derived from this software without prior written permission. For 
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Turbine", nor may "Apache" appear in their name, without 
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

import java.util.Map;
import java.util.Iterator;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.CallableStatement;
import java.sql.Statement;
import java.sql.SQLWarning;
import java.sql.SQLException;
import java.util.EventObject;
import java.util.Vector;
import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;
import org.apache.torque.Torque;

/**
 * Implementation of PooledConnection that is returned by
 * PooledConnectionDataSource.
 *
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @version $Id$
 */
public class PooledConnectionImpl 
    implements PooledConnection
{
    /**
     * The JDBC database connection that represents the physical db connection.
     */
    private Connection connection = null;

    /**
     * The JDBC database logical connection.
     */
    private Connection torqueConnection = null;

    /**
     * ConnectionEventListeners
     */
    private Vector eventListeners;

    /**
     * Wrap the real connection.
     */
    PooledConnectionImpl(Connection connection)
    {
        this.connection = connection;
        eventListeners = new Vector();
    }

    /**
     * Add an event listener.
     */
    public void addConnectionEventListener(ConnectionEventListener listener)
    {
        if ( !eventListeners.contains(listener) )
        {
            eventListeners.add(listener);
        }
    }

    public void close()
        throws SQLException
    {        
        connection.close();
    }

    /**
     * Returns a JDBC connection.
     *
     * @return The database connection.
     */
    public Connection getConnection()
        throws SQLException
    {
        // make sure the last connection is marked as closed
        if ( !torqueConnection.isClosed() ) 
        {
            // should notify pool of error so the pooled connection can
            // be removed !FIXME!
            throw new SQLException("PooledConnection was reused, without" +
                                   "its previous Connection being closed.");
        }

        // the spec requires that this return a new Connection instance.
        torqueConnection = new ConnectionImpl(this, connection);
        return torqueConnection;
    }

    /**
     * Remove an event listener.
     */
    public void removeConnectionEventListener(ConnectionEventListener listener)
    {
        eventListeners.remove(listener);
    }

    /**
     * The finalizer helps prevent <code>ConnectionPool</code> leakage.
     */
    protected void finalize()
        throws Throwable
    {
        // If this DBConnection object is finalized while linked
        // to a ConnectionPool, it means that it was taken from a pool
        // and not returned.  We log this fact, close the underlying
        // Connection, and return it to the ConnectionPool.
        Torque.getCategory().warn( "A PooledConnection was " 
                                   + "finalized, without being returned "
                                   + "to the ConnectionPool it belonged to" );
        
        // Closing the Connection ensures that if anyone tries to use it,
        // an error will occur.
        connection.close();
    }

    void notifyListeners()
    {
        ConnectionEvent event = new ConnectionEvent(this);
        Iterator i = eventListeners.iterator();
        while ( i.hasNext() ) 
        {
            ((ConnectionEventListener)i.next()).connectionClosed(event);
        }
    }
}
