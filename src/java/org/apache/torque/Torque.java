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

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.log4j.Category;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.NullEnumeration;
import org.apache.torque.adapter.DB;
import org.apache.torque.adapter.DBFactory;
import org.apache.torque.map.DatabaseMap;
import org.apache.torque.map.TableMap;
import org.apache.torque.oid.IDGeneratorFactory;
import org.apache.torque.oid.IDBroker;
import org.apache.torque.pool.ConnectionPool;
import org.apache.torque.pool.DBConnection;

/**
 * The implementation of Torque.
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

    /**
     * A constant for <code>default</code>.
     */
    private static final String DEFAULT_NAME = "default";

    /**
     * The db name that is specified as the default in the property file
     */
    private static String defaultDBName;

    /** 
     * The global cache of database maps 
     */
    private static Map dbMaps;

    /**
     * The various connection pools this broker contains.  Keyed by
     * database URL.
     */
    private static Map pools;

    /**
     * The logging category.
     */
    private static Category category;

    /**
     * Torque-specific configuration.
     */
    private static ExtendedProperties configuration;

    /**
     * The connection pool monitor.
     */
    private static Monitor monitor;

    /**
     * Initializes Torque.
     */
    public static void init()
        throws Exception
    {
        dbMaps = new HashMap();
        pools = new HashMap();
        DBFactory.init(configuration);

        // Create monitor thread
        monitor = new Monitor();
        // Indicate that this is a system thread. JVM will quit only when there
        // are no more active user threads. Settings threads spawned internally
        // by Turbine as daemons allows commandline applications using Turbine
        // to terminate in an orderly manner.
        monitor.setDaemon(true);
        monitor.start();
    }

    /**
     * Initialization of Torque with a properties file.
     *
     * @param configFile The path to the configuration file.
     */
    public static void init(String configFile)
        throws Exception
    {
        ExtendedProperties c = new ExtendedProperties(configFile);
        
        // First look for properties that are in the "torque"
        //  namespace.
        c = c.subset("torque");
        
        if (c.isEmpty())
        {
            // If there are no properties in the "torque" namespace
            // than try the "services.DatabaseService" namespace. This
            // will soon be deprecated.
            c = c.subset("services.DatabaseService");
        }

        if (isLoggingConfigured() == false)
        {
            // Get the applicationRoot for use in the log4j
            // properties.
            String applicationRoot = c.getString("applicationRoot", ".");

            File logsDir = new File(applicationRoot, "logs");

            if (logsDir.exists() == false)
            {
                if (logsDir.mkdirs() == false)
                {
                    System.err.println("Cannot create logs directory!");
                }
            }

            Properties p = new Properties();
            p.load(new FileInputStream(configFile));
            // Set the applicationRoot in the log4j properties so that
            // ${applicationRoot} can be used in the properties file.
            p.setProperty("applicationRoot", applicationRoot);
            PropertyConfigurator.configure(p);
        }

        Torque.setConfiguration(c);
        Torque.init();
    }

    /**
     * Sets the configuration for Torque and all dependencies.
     */
    public static void setConfiguration(ExtendedProperties c)
    {
        configuration = c;
    }

    /**
     * Determine whether log4j has already been configured.
     *
     * @return boolean Whether log4j is configured.
     */
    protected static boolean isLoggingConfigured()
    {
        // This is a note from Ceki, taken from a message on the log4j
        // user list:
        //
        // Having defined categories does not necessarily mean
        // configuration. Remember that most categories are created
        // outside the configuration file. What you want to check for
        // is the existence of appenders. The correct procedure is to
        // first check for appenders in the root category and if that
        // returns no appenders to check in other categories.

        Enumeration enum = Category.getRoot().getAllAppenders();

        if (!(enum instanceof NullEnumeration))
        {
            return true;
        }
        else
        {
            Enumeration cats =  Category.getCurrentCategories();
            while(cats.hasMoreElements())
            {
                Category c = (Category) cats.nextElement();
                if (!(c.getAllAppenders() instanceof NullEnumeration))
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Shuts down the service.
     *
     * This method halts the IDBroker's daemon thread in all of
     * the DatabaseMap's.
     */
    public static void shutdown()
    {
        if ( dbMaps != null )
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

        // shutdown the thread
        monitor = null;
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
        return getDatabaseMap(getDefaultDB());
    }

    /**
     * Returns the database map information. Name relates to the name
     * of the connection pool to associate with the map.
     *
     * @param name The name of the database corresponding to the
     * <code>DatabaseMap</code> to retrieve.
     * @return The named <code>DatabaseMap</code>.
     * @throws TorqueException Any exceptions caught during procssing
     * will be rethrown wrapped into a <code>TorqueException</code>.
     */
    public static DatabaseMap getDatabaseMap(String name)
        throws TorqueException
    {
        if (name == null)
        {
            throw new TorqueException ("DatabaseMap name was null!");
        }

        if (dbMaps == null)
        {
            try
            {
                Torque.init();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        // Quick (non-sync) check for the map we want.
        DatabaseMap map = (DatabaseMap) dbMaps.get(name);
        if (map == null)
        {
            // Map not there...
            synchronized (dbMaps)
            {
                // ... sync and look again to avoid race condition.
                map = (DatabaseMap) dbMaps.get(name);
                if (map == null)
                {
                    // Still not there.  Create and add.
                    map = initDatabaseMap(name);
                }
            }
        }
        return map;
    }

    /**
     * Creates and initializes the mape for the named database.
     * Assumes that <code>dbMaps</code> member is sync'd.
     *
     * @param name The name of the database to map.
     * @return The desired map.
     */
    private static final DatabaseMap initDatabaseMap(String name)
        throws TorqueException
    {
        DatabaseMap map = new DatabaseMap(name);

        // Add info about IDBroker's table.
        setupIdTable(map);

        // Setup other ID generators for this map.
        try
        {
            DB db = DBFactory.create(getDatabaseProperty(name, "driver"));
            for (int i = 0; i < IDGeneratorFactory.ID_GENERATOR_METHODS.length;
                 i++)
            {
                map.addIdGenerator(IDGeneratorFactory.ID_GENERATOR_METHODS[i],
                                   IDGeneratorFactory.create(db));
            }
        }
        catch (java.lang.InstantiationException e)
        {
            throw new TorqueException(e);
        }
        dbMaps.put(name, map);

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
     * IDBroker functionality is available in all databases used by the
     * application.
     *
     * @param map the DataBaseMap to setup.
     */
    private static final void setupIdTable(DatabaseMap map)
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
    public static DBConnection getConnection()
        throws Exception
    {
        return getConnection(getDefaultDB());
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
    public static DBConnection getConnection(String name)
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
    public static DBConnection getConnection(String driver,
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
    public static void releaseConnection(DBConnection dbconn)
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
    public static void registerPool( String name,
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
    public static void registerPool( String name,
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
            synchronized (pools)
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
    public static DB getDB()
        throws Exception
    {
        return getDB(getDefaultDB());
    }

    /**
     * Returns database adapter for a specific connection pool.
     *
     * @param name A pool name.
     * @return     The corresponding database adapter.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static DB getDB(String name)
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
     * @see #getPool(String name)
     */
    private static ConnectionPool getPool()
        throws Exception
    {
        return getPool(getDefaultDB());
    }

    /**
     * This method returns a pool with the specified name.  The pool must
     * either have been registered with the
     * {@link #registerPool(String,String,String,String,String)} method, or be
     * specified in the TurbineResources properties. This method is used
     * internally by the service.
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
     * @param dbName The name of the database whose property to get.
     * @param prop The name of the property to get.
     * @return The string of the property.
     */
    private static final String getProperty(String dbName, String prop)
    {
        return ("database." + dbName + '.' + prop);
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
                    
                    category.info(buf.toString());
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

    /**
     * Returns the name of the default database.
     */
    public static String getDefaultDB()
    {
        if (configuration == null)
        {
            return DEFAULT_NAME;
        }
        // save the property lookup, so that we can be sure it is always the
        // same object.
        else if (defaultDBName == null)
        {
            defaultDBName = 
                configuration.getString(DATABASE_DEFAULT, DEFAULT_NAME);
        }
        
        return defaultDBName;
    }

    /**
     * @deprecated Use getDefaultDB() instead.
     */
    public static String getDefaultMap()
    {
        return getDefaultDB();
    }
}
