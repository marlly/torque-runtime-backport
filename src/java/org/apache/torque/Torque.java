package org.apache.torque;

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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.torque.adapter.DB;
import org.apache.torque.adapter.DBFactory;
import org.apache.torque.map.DatabaseMap;
import org.apache.torque.map.TableMap;
import org.apache.torque.oid.AutoIncrementIdGenerator;
import org.apache.torque.oid.SequenceIdGenerator;
import org.apache.torque.oid.IDBroker;
import org.apache.torque.pool.ConnectionPool;
import org.apache.torque.pool.DBConnection;
import org.apache.torque.util.BasePeer;

import org.apache.log4j.Category;
import org.apache.velocity.runtime.configuration.Configuration;

/**
 * Turbine's default implementation of {@link DatabaseService}.
 *
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @author <a href="mailto:magnus@handtolvur.is">Magnús Þór Torfason</a>
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:Rafal.Krzewski@e-point.pl">Rafal Krzewski</a>
 * @version $Id$
 */
public class Torque
{
    /**
     * Name of property that specifies the default
     * map builder and map.
     */
    public static final String DATABASE_DEFAULT = "database.default";

    /** The global cache of database maps */
    private static Map dbMaps;

    /**
     * The various connection pools this broker contains.  Keyed by
     * database URL.
     */
    private static Map pools;

    /**
     * The property tag which specifies which
     * log4j category to use for logging in BasePeer.
     */
    private static final String CATEGORY = "log4j.category";

    /**
     * The default log4j category to use if the
     * the log4j.category property isn't set.
     */
    private static final String DEFAULT_CATEGORY = "sql";

    private static Category category;
    private static Configuration configuration;

    private static Monitor monitor;

    /**
     * Initializes the service.
     */
    public static void init()
        throws Exception
    {
        dbMaps = (Map)new HashMap();

        pools = (Map) new HashMap();
        // Create monitor thread
        monitor = new Monitor();
        // Indicate that this is a system thread. JVM will quit only when there
        // are no more active user threads. Settings threads spawned internally
        // by Turbine as daemons allows commandline applications using Turbine
        // to terminate in an orderly manner.
        monitor.setDaemon(true);
        monitor.start();

        DBFactory.setConfiguration(configuration);
        DBFactory.setCategory(category);
        DBFactory.init();
        BasePeer.setCategory(category);
    }

    public static void setCategory(Category c)
    {
        category = c;
    }

    public static Category getCategory()
    {
        return category;
    }

    public static void setConfiguration(Configuration c)
    {
        configuration = c;
    }

    /**
     * Shuts down the service.
     *
     * This method halts the IDBroker's daemon thread in all of
     * the DatabaseMap's.
     */
    public static void shutdown()
    {
        Iterator maps = dbMaps.values().iterator();
        while ( maps.hasNext() )
        {
            DatabaseMap map = (DatabaseMap) maps.next();
            IDBroker idBroker = map.getIDBroker();
            if (idBroker != null)
            {
                idBroker.stop();
            }
        }

        if ( pools != null )
        {
            // Release connections for each pool.
            Iterator pool = pools.values().iterator();
            while ( pool.hasNext() )
            {
                try
                {
                    ((ConnectionPool)pool.next()).shutdown();
                }
                catch (Exception ignored)
                {
                    // Unlikely.
                }
            }
        }
    }

    /**
     * Returns the default database map information.
     *
     * @return A DatabaseMap.
     * @throws TorqueException Any exceptions caught during procssing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static DatabaseMap getDatabaseMap()
        throws TorqueException
    {
        return getDatabaseMap(
            configuration.getString(DATABASE_DEFAULT,"default"));
    }

    /**
     * Returns the database map information. Name relates to the name
     * of the connection pool to associate with the map.
     *
     * @param name The name of the <code>DatabaseMap</code> to
     * retrieve.
     * @return The named <code>DatabaseMap</code>.
     * @throws TorqueException Any exceptions caught during procssing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static DatabaseMap getDatabaseMap(String name)
        throws TorqueException
    {
        if ( name == null )
        {
            throw new TorqueException ("DatabaseMap name was null!");
        }

        // Quick (non-sync) check for the map we want.
        DatabaseMap map = (DatabaseMap)dbMaps.get(name);
        if ( map == null )
        {
            // Map not there...
            synchronized( dbMaps )
            {
                // ... sync and look again to avoid race condition.
                map = (DatabaseMap)dbMaps.get(name);
                if ( map == null )
                {
                    // Still not there.  Create and add.
                    map = new DatabaseMap(name);

                    // Add info about IDBroker's table.
                    setupIdTable(map);
                    // setup other id generators
                    try
                    {
                        DB db = DBFactory.create(
                            getDatabaseProperty(name, "driver") );
                        map.addIdGenerator(TableMap.AUTOINCREMENT,
                                       new AutoIncrementIdGenerator(db) );
                        map.addIdGenerator(TableMap.SEQUENCE,
                                       new SequenceIdGenerator(db) );
                    }
                    catch (java.lang.InstantiationException e)
                    {
                        throw new TorqueException(e);
                    }

                    dbMaps.put(name, map);
                }
            }
        }
        return map;
    }

    /**
     * Returns the specified property of the given database, or the empty
     * string if no value is set for the property.
     *
     * @param db   The name of the database whose property to get.
     * @param prop The name of the property to get.
     * @return     The property's value.
     */
    private static String getDatabaseProperty(String db, String prop)
    {
        return configuration.getString( new StringBuffer("database.")
                .append(db)
                .append('.')
                .append(prop)
                .toString(), "" );
    }

    /**
     * Setup IDBroker's table information within given database map.
     *
     * This method should be called on all new database map to ensure that
     * IDBroker functionality is available in all databases userd by the
     * application.
     *
     * @param map the DataBaseMap to setup.
     */
    private static void setupIdTable(DatabaseMap map)
    {
        map.setIdTable("ID_TABLE");
        TableMap tMap = map.getIdTable();
        tMap.addPrimaryKey("ID_TABLE_ID", new Integer(0));
        tMap.addColumn("TABLE_NAME", new String(""));
        tMap.addColumn("NEXT_ID", new Integer(0));
        tMap.addColumn("QUANTITY", new Integer(0));
    }

    /**
     * This method returns a DBConnection from the default pool.
     *
     * @return The requested connection.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static  DBConnection getConnection()
        throws Exception
    {
        return getConnection(
            configuration.getString(DATABASE_DEFAULT,"default"));
    }

    /**
     * This method returns a DBConnection from the pool with the
     * specified name.  The pool must either have been registered
     * with the {@link #registerPool(String,String,String,String,String)}
     * method, or be specified in the property file using the
     * following syntax:
     *
     * <pre>
     * database.[name].driver
     * database.[name].url
     * database.[name].username
     * database.[name].password
     * </pre>
     *
     * @param name The name of the pool to get a connection from.
     * @return     The requested connection.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static  DBConnection getConnection(String name)
        throws Exception
    {
        // The getPool method ensures the validity of the returned pool.
        return getPool(name).getConnection();
    }

    /**
     * This method returns a DBConnecton using the given parameters.
     *
     * @param driver The fully-qualified name of the JDBC driver to use.
     * @param url The URL of the database from which the connection is
     * desired.
     * @param username The name of the database user.
     * @param password The password of the database user.
     * @return A DBConnection.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     *
     * @deprecated Database parameters should not be specified each
     * time a DBConnection is fetched from the service.
     */
    public static  DBConnection getConnection(String driver,
                                      String url,
                                      String username,
                                      String password)
        throws Exception
    {
        ConnectionPool pool = null;
        url = url.trim();

        // Quick (non-sync) check for the pool we want.
        // NOTE: Here we must not call getPool(), since the pool
        // is almost certainly not defined in the properties file
        pool = (ConnectionPool) pools.get(url + username);
        if ( pool == null )
        {
            registerPool(url + username, driver,  url, username, password);
            pool = (ConnectionPool) pools.get(url + username);
        }

        return pool.getConnection();
    }

    /**
     * Release a connection back to the database pool.  <code>null</code>
     * references are ignored.
     *
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     * @exception Exception A generic exception.
     */
    public static  void releaseConnection(DBConnection dbconn)
        throws Exception
    {
        if ( dbconn != null )
        {
            ConnectionPool pool = dbconn.getPool();
            if ( pools.containsValue( pool ) )
            {
                pool.releaseConnection( dbconn );
            }
        }
    }

    /**
     * This method registers a new pool using the given parameters.
     *
     * @param name The name of the pool to register.
     * @param driver The fully-qualified name of the JDBC driver to use.
     * @param url The URL of the database to use.
     * @param username The name of the database user.
     * @param password The password of the database user.
     *
     * @throws Exception Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static  void registerPool( String name,
                              String driver,
                              String url,
                              String username,
                              String password )
        throws Exception
    {
        /**
         * Added so that the configuration file can define maxConnections &
         * expiryTime for each database pool that is defined in the
         * TurbineResources.properties
         * Was defined as: database.expiryTime=3600000
         * If you need per database, it is
         * now database.helpdesk.expiryTime=3600000
         */
        registerPool(
            name,
            driver,
            url,
            username,
            password,
            configuration.getInt(getProperty(name, "maxConnections"), 10),
            configuration.getLong(getProperty(name, "expiryTime"), 3600000),
            configuration.getLong(getProperty(name, "maxConnectionAttempts"), 50),
            configuration.getLong(getProperty(name, "connectionWaitTimeout"), 10000));
    }

    /**
     * This thread-safe method registers a new pool using the given parameters.
     *
     * @param name The name of the pool to register.
     * @param driver The fully-qualified name of the JDBC driver to use.
     * @param url The URL of the database to use.
     * @param username The name of the database user.
     * @param password The password of the database user.
     * @exception Exception A generic exception.
     */
    public static  void registerPool( String name,
                              String driver,
                              String url,
                              String username,
                              String password,
                              int maxCons,
                              long expiryTime,
                              long maxConnectionAttempts,
                              long connectionWaitTimeout)
        throws Exception
    {

        // Quick (non-sync) check for the pool we want.
        if ( !pools.containsKey(name) )
        {
            // Pool not there...
            synchronized ( pools )
            {
                // ... sync and look again to avoid race collisions.
                if ( !pools.containsKey(name) )
                {
                    // Still not there.  Create and add.
                    ConnectionPool pool =
                        new ConnectionPool(
                            driver,
                            url,
                            username,
                            password,
                            maxCons,
                            expiryTime,
                            maxConnectionAttempts,
                            connectionWaitTimeout);

                    pools.put( name, pool );
                }
            }
        }
    }

    /**
     * Returns the database adapter for the default connection pool.
     *
     * @return The database adapter.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static  DB getDB()
        throws Exception
    {
        return getDB(configuration.getString(DATABASE_DEFAULT,"default"));
    }

    /**
     * Returns database adapter for a specific connection pool.
     *
     * @param name A pool name.
     * @return     The corresponding database adapter.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static  DB getDB(String name)
        throws Exception
    {
        return getPool(name).getDB();
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * This method returns the default pool.
     *
     * @return The default pool.
     * @exception Exception A generic exception.
     */
    private static ConnectionPool getPool()
        throws Exception
    {
        return getPool(configuration.getString(DATABASE_DEFAULT,"default"));
    }

    /**
     * This method returns a pool with the specified name.  The pool must
     * either have been registered with the
     * {@link #registerPool(String,String,String,String,String)} methd, or be
     * specified in the TurbineResources properties. This method is used
     * interanlly by the service.
     *
     * @param name The name of the pool to get.
     * @return     The requested pool.
     *
     * @exception Exception A generic exception.
     */
    private static ConnectionPool getPool(String name)
        throws Exception
    {
        if (name == null)
        {
            throw new TorqueException ("Torque.getPool(): name is null");
        }
        if (pools == null)
        {
            throw new TorqueException (
                "Torque.getPool(): pools is null, did you call Torque.init() first?");
        }

        ConnectionPool pool = (ConnectionPool) pools.get(name);

        // If the pool is not in the Hashtable, we must register it.
        if ( pool == null )
        {
            registerPool(
                name,
                getDatabaseProperty(name, "driver"),
                getDatabaseProperty(name, "url"),
                getDatabaseProperty(name, "username"),
                getDatabaseProperty(name, "password"));

            pool = (ConnectionPool) pools.get(name);
        }

        return pool;
    }

    /**
     * Returns the string for the specified property of the given database.
     *
     * @param db   The name of the database whose property to get.
     * @param prop The name of the property to get.
     * @return     The string of the property.
     */
    private static String getProperty(String db, String prop)
    {
        return
            new StringBuffer("database.")
            .append(db)
            .append('.')
            .append(prop)
            .toString();
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
     * Set the <code>database.logInterval</code> property to the number of
     * milliseconds you want to elapse between loging the number of
     * connections.
     */
    protected static class Monitor extends Thread
    {
        public void run()
        {
            int logInterval = configuration.getInt("database.logInterval",0);
            StringBuffer buf = new StringBuffer();
            while (logInterval > 0)
            {
                // Loop through all pools and log.
                Iterator poolIter = pools.keySet().iterator();
                while ( poolIter.hasNext() )
                {
                    String poolName = (String) poolIter.next();
                    ConnectionPool pool = (ConnectionPool) pools.get(poolName);
                    buf.setLength(0);
                    buf.append(poolName).append(" (in + out = total): ")
                        .append(pool.getNbrAvailable()).append(" + ")
                        .append(pool.getNbrCheckedOut()).append(" = ")
                        .append(pool.getTotalCount());
                    getCategory().info(buf.toString());
                }

                // Wait for a bit.
                try
                {
                    Thread.sleep(logInterval);
                }
                catch (InterruptedException ignored)
                {
                    // Don't care.
                }
            }
        }
    }

    public static  String getDefaultDB()
    {
        if (configuration == null)
        {
            return "default";
        }
        return configuration.getString(DATABASE_DEFAULT,"default");
    }

    public static  String getDefaultMap()
    {
        if (configuration == null)
        {
            return "default";
        }
        return configuration.getString(DATABASE_DEFAULT,"default");
    }
}
