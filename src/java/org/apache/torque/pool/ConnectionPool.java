package org.apache.torque.pool;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class implements a simple connection pooling scheme.
 *
 * @author <a href="mailto:csterg@aias.gr">Costas Stergiou</a>
 * @author <a href="mailto:frank.kim@clearink.com">Frank Y. Kim</a>
 * @author <a href="mailto:bmclaugh@algx.net">Brett McLaughlin</a>
 * @author <a href="mailto:greg@shwoop.com">Greg Ritter</a>
 * @author <a href="mailto:dlr@collab.net">Daniel L. Rall</a>
 * @author <a href="mailto:paul@evolventtech.com">Paul O'Leary</a>
 * @author <a href="mailto:magnus@handtolvur.is">Magnús Þór Torfason</a>
 * @author <a href="mailto:jon@latchkey.com">Jon S. Stevens</a>
 * @author <a href="mailto:jmcnally@collab.net">John McNally</a>
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @version $Id$
 */
class ConnectionPool implements ConnectionEventListener
{

    /** Default maximum Number of connections from this pool: One */
    public static final int DEFAULT_MAX_CONNECTIONS = 1;

    /** Default Expiry Time for a pool: 1 hour */
    public static final int DEFAULT_EXPIRY_TIME = 60 * 60 * 1000;

    /** Default Connect Wait Timeout: 10 Seconds */
    public static final int DEFAULT_CONNECTION_WAIT_TIMEOUT = 10 * 1000;

    /** Pool containing database connections. */
    private Stack pool;

    /** The url for this pool. */
    private String url;

    /** The user name for this pool. */
    private String username;

    /** The password for this pool. */
    private String password;

    /** The current number of database connections that have been created. */
    private int totalConnections;

    /** The maximum number of database connections that can be created. */
    private int maxConnections = DEFAULT_MAX_CONNECTIONS;

    /** The amount of time in milliseconds that a connection will be pooled. */
    private long expiryTime = DEFAULT_EXPIRY_TIME;

    /**
     * Counter that keeps track of the number of threads that are in
     * the wait state, waiting to aquire a connection.
     */
    private int waitCount = 0;

    /** The logging logger. */
    private static Log log = LogFactory.getLog(ConnectionPool.class);

    /** Interval (in seconds) that the monitor thread reports the pool state */
    private int logInterval = 0;

    /** Monitor thread reporting the pool state */
    private Monitor monitor;

    /**
     * Amount of time a thread asking the pool for a cached connection will
     * wait before timing out and throwing an error.
     */
    private long connectionWaitTimeout = DEFAULT_CONNECTION_WAIT_TIMEOUT;

    /** The ConnectionPoolDataSource  */
    private ConnectionPoolDataSource cpds;

    /**
     * Keep track of when connections were created.  Keyed by a
     * PooledConnection and value is a java.util.Date
     */
    private Map timeStamps;

    /**
     * Creates a <code>ConnectionPool</code> with the default
     * attributes.
     *
     * @param cpds The datasource
     * @param username The user name for this pool.
     * @param password The password for this pool.
     * @param maxConnections max number of connections
     * @param expiryTime connection expiry time
     * @param connectionWaitTimeout timeout
     * @param logInterval log interval
     */
    ConnectionPool(ConnectionPoolDataSource cpds, String username,
                   String password, int maxConnections, int expiryTime,
                   int connectionWaitTimeout, int logInterval)
    {
        totalConnections = 0;
        pool = new Stack();
        timeStamps = new HashMap();

        this.cpds = cpds;
        this.username = username;
        this.password = password;

        this.maxConnections =
            (maxConnections > 0) ? maxConnections : DEFAULT_MAX_CONNECTIONS;

        this.expiryTime =
            ((expiryTime > 0) ? expiryTime * 1000 : DEFAULT_EXPIRY_TIME);

        this.connectionWaitTimeout =
            ((connectionWaitTimeout > 0)
             ? connectionWaitTimeout * 1000
             : DEFAULT_CONNECTION_WAIT_TIMEOUT);

        this.logInterval = 1000 * logInterval;

        if (logInterval > 0)
        {
            log.debug("Starting Pool Monitor Thread with Log Interval "
                           + logInterval + " Milliseconds");

            // Create monitor thread
            monitor = new Monitor();

            // Indicate that this is a system thread. JVM will quit only
            // when there are no more active user threads. Settings threads
            // spawned internally by Torque as daemons allows commandline
            // applications using Torque to terminate in an orderly manner.
            monitor.setDaemon(true);
            monitor.start();
        }
    }

    /**
     * Returns a connection that maintains a link to the pool it came from.
     *
     * @param username The name of the database user.
     * @param password The password of the database user.
     * @return         A database connection.
     * @exception SQLException if there is aproblem with the db connection
     */
    final synchronized PooledConnection getConnection(String username,
            String password)
            throws SQLException
    {
        if (username != this.username || password != this.password)
        {
            throw new SQLException("Username and password are invalid.");
        }

        PooledConnection pcon = null;
        if (pool.empty() && totalConnections < maxConnections)
        {
            pcon = getNewConnection();
        }
        else
        {
            try
            {
                pcon = getInternalPooledConnection();
            }
            catch (Exception e)
            {
                throw new SQLException(e.getMessage());
            }
        }
        return pcon;
    }

    /**
     * Returns a fresh connection to the database.  The database type
     * is specified by <code>driver</code>, and its connection
     * information by <code>url</code>, <code>username</code>, and
     * <code>password</code>.
     *
     * @return A database connection.
     * @exception SQLException if there is aproblem with the db connection
     */
    private PooledConnection getNewConnection()
        throws SQLException
    {
        PooledConnection pc = null;
        if (username == null)
        {
            pc = cpds.getPooledConnection();
        }
        else
        {
            pc = cpds.getPooledConnection(username, password);
        }
        pc.addConnectionEventListener(this);

        // Age some connections so that there will not be a run on the db,
        // when connections start expiring
        //
        // I did some experimentation here with integers but as this
        // is not a really time critical path, we keep the floating
        // point calculation.
        long currentTime = System.currentTimeMillis();

        double ratio = new Long(maxConnections - totalConnections).doubleValue()
            / maxConnections;

        long ratioTime = new Double(currentTime - (expiryTime * ratio) / 4)
            .longValue();

        ratioTime = (expiryTime < 0) ? currentTime : ratioTime;

        timeStamps.put(pc, new Long(ratioTime));
        totalConnections++;
        return pc;
    }

    /**
     * Gets a pooled database connection.
     *
     * @return A database connection.
     * @exception ConnectionWaitTimeoutException Wait time exceeded.
     * @exception Exception No pooled connections.
     */
    private synchronized PooledConnection getInternalPooledConnection()
        throws ConnectionWaitTimeoutException, Exception
    {
        // We test waitCount > 0 to make sure no other threads are
        // waiting for a connection.
        if (waitCount > 0 || pool.empty())
        {
            // The connection pool is empty and we cannot allocate any new
            // connections.  Wait the prescribed amount of time and see if
            // a connection is returned.
            try
            {
                waitCount++;
                wait(connectionWaitTimeout);
            }
            catch (InterruptedException ignored)
            {
                // Don't care how we come out of the wait state.
            }
            finally
            {
                waitCount--;
            }

            // Check for a returned connection.
            if (pool.empty())
            {
                // If the pool is still empty here, we were not awoken by
                // someone returning a connection.
                throw new ConnectionWaitTimeoutException(url);
            }
        }
        return popConnection();
    }
    /**
     * Helper function that attempts to pop a connection off the pool's stack,
     * handling the case where the popped connection has become invalid by
     * creating a new connection.
     *
     * @return An existing or new database connection.
     * @throws Exception if the pool is empty
     */
    private PooledConnection popConnection()
        throws Exception
    {
        while (!pool.empty())
        {
            PooledConnection con = (PooledConnection) pool.pop();

            // It's really not safe to assume this connection is
            // valid even though it's checked before being pooled.
            if (isValid(con))
            {
                return con;
            }
            else
            {
                // Close invalid connection.
                con.close();
                totalConnections--;

                // If the pool is now empty, create a new connection.  We're
                // guaranteed not to exceed the connection limit since we
                // just killed off one or more invalid connections, and no
                // one else can be accessing this cache right now.
                if (pool.empty())
                {
                    return getNewConnection();
                }
            }
        }

        // The connection pool was empty to start with--don't call this
        // routine if there's no connection to pop!
        // TODO: Propose general Turbine assertion failure exception? -PGO
        throw new Exception("Assertion failure: Attempted to pop "
                + "connection from empty pool!");
    }

    /**
     * Helper method which determines whether a connection has expired.
     *
     * @param pc The connection to test.
     * @return True if the connection is expired, false otherwise.
     */
    private boolean isExpired(PooledConnection pc)
    {
        // Test the age of the connection (defined as current time
        // minus connection birthday) against the connection pool
        // expiration time.
        long birth = ((Long) timeStamps.get(pc)).longValue();
        long age   = System.currentTimeMillis() - birth;

        boolean dead = (expiryTime > 0)
            ? age > expiryTime
            : age > DEFAULT_EXPIRY_TIME;

        return dead; // He is dead, Jim.
    }

    /**
     * Determines if a connection is still valid.
     *
     * @param connection The connection to test.
     * @return True if the connection is valid, false otherwise.
     */
    private boolean isValid(PooledConnection connection)
    {
        // all this code is commented out because
        // connection.getConnection() is called when the connection
        // is returned to the pool and it will open a new logical Connection
        // which does not get closed, then when it is called again
        // when a connection is requested it likely fails because a
        // new Connection has been requested and the old one is still
        // open.  need to either do it right or skip it.  null check
        // was not working either.

        //try
        //{
            // This will throw an exception if:
            //     The connection is null
            //     The connection is closed
            // Therefore, it would be false.
        //connection.getConnection();
            // Check for expiration
            return !isExpired(connection);
            /*
        }
        catch (SQLException e)
        {
            return false;
        }
            */
    }


    /**
     * Close any open connections when this object is garbage collected.
     *
     * @exception Throwable Anything might happen...
     */
    protected void finalize()
        throws Throwable
    {
        shutdown();
    }

    /**
     * Close all connections to the database,
     */
    void shutdown()
    {
        if (pool != null)
        {
            while (!pool.isEmpty())
            {
                try
                {
                    ((PooledConnection) pool.pop()).close();
                }
                catch (SQLException ignore)
                {
                }
                finally
                {
                    totalConnections--;
                }
            }
        }
        monitor.shutdown();
    }

    /**
     * Returns the Total connections in the pool
     *
     * @return total connections in the pool
     */
    int getTotalCount()
    {
        return totalConnections;
    }

    /**
     * Returns the available connections in the pool
     *
     * @return number of available connections in the pool
     */
    int getNbrAvailable()
    {
        return pool.size();
    }

    /**
     * Returns the checked out connections in the pool
     *
     * @return number of checked out connections in the pool
     */
    int getNbrCheckedOut()
    {
        return (totalConnections - pool.size());
    }

    /**
     * Decreases the count of connections in the pool
     * and also calls <code>notify()</code>.
     */
    void decrementConnections()
    {
        totalConnections--;
        notify();
    }

    /**
     * Get the name of the pool
     *
     * @return the name of the pool
     */
    String getPoolName()
    {
        return toString();
    }

    // ***********************************************************************
    // java.sql.ConnectionEventListener implementation
    // ***********************************************************************

    /**
     * This will be called if the Connection returned by the getConnection
     * method came from a PooledConnection, and the user calls the close()
     * method of this connection object. What we need to do here is to
     * release this PooledConnection from our pool...
     *
     * @param event the connection event
     */
    public void connectionClosed(ConnectionEvent event)
    {
        releaseConnection((PooledConnection) event.getSource());
    }

    /**
     * If a fatal error occurs, close the underlying physical connection so as
     * not to be returned in the future
     *
     * @param event the connection event
     */
    public void connectionErrorOccurred(ConnectionEvent event)
    {
        try
        {
            System.err.println("CLOSING DOWN CONNECTION DUE TO INTERNAL ERROR");
            //remove this from the listener list because we are no more
            //interested in errors since we are about to close this connection
            ((PooledConnection) event.getSource())
                    .removeConnectionEventListener(this);
        }
        catch (Exception ignore)
        {
            //just ignore
        }

        try
        {
            closePooledConnection((PooledConnection) event.getSource());
        }
        catch (Exception ignore)
        {
            //just ignore
        }
    }

    /**
     * This method returns a connection to the pool, and <b>must</b>
     * be called by the requestor when finished with the connection.
     *
     * @param pcon The database connection to release.
     */
    private synchronized void releaseConnection(PooledConnection pcon)
    {
        if (isValid(pcon))
        {
            pool.push(pcon);
            notify();
        }
        else
        {
            closePooledConnection(pcon);
        }
    }

    /**
     *
     * @param pcon The database connection to close.
     */
    private void closePooledConnection(PooledConnection pcon)
    {
        try
        {
            pcon.close();
            timeStamps.remove(pcon);
        }
        catch (Exception e)
        {
            log.error("Error occurred trying to close a PooledConnection.", e);
        }
        finally
        {
            decrementConnections();
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * This inner class monitors the <code>PoolBrokerService</code>.
     *
     * This class is capable of logging the number of connections available in
     * the pool periodically. This can prove useful if you application
     * frozes after certain amount of time/requests and you suspect
     * that you have connection leakage problem.
     *
     * Set the <code>logInterval</code> property of your pool definition
     * to the number of seconds you want to elapse between loging the number of
     * connections.
     */
    protected class Monitor extends Thread
    {
        /** true if the monot is running */
        private boolean isRun = true;

        /**
         * run method for the monitor thread
         */
        public void run()
        {
            StringBuffer buf = new StringBuffer();
            while (logInterval > 0 && isRun)
            {
                buf.setLength(0);

                buf.append(getPoolName());
                buf.append(" avail: ").append(getNbrAvailable());
                buf.append(" in use: ").append(getNbrCheckedOut());
                buf.append(" total: ").append(getTotalCount());
                log.info(buf.toString());

                // Wait for a bit.
                try
                {
                    Thread.sleep(logInterval);
                }
                catch (InterruptedException ignored)
                {
                }
            }
        }

        /**
         * Shut down the monitor
         */
        public void shutdown()
        {
            isRun = false;
        }
    }
}
