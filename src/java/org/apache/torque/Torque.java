package org.apache.torque;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.sql.Connection;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.torque.adapter.DB;
import org.apache.torque.manager.AbstractBaseManager;
import org.apache.torque.map.DatabaseMap;

/**
 * A static facade wrapper around the Torque implementation (which is in
 * {@link org.apache.torque.TorqueInstance}).
 * <br/>
 *
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @author <a href="mailto:magnus@handtolvur.is">Magn�s ��r Torfason</a>
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:Rafal.Krzewski@e-point.pl">Rafal Krzewski</a>
 * @author <a href="mailto:mpoeschl@marmot.at">Martin Poeschl</a>
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @author <a href="mailto:kschrader@karmalab.org">Kurt Schrader</a>
 * @version $Id$
 */
public class Torque
{
    /**
     * The prefix for all configuration keys used by Torque.
     */
    public static final String TORQUE_KEY = "torque";

    /**
     * The prefix for configuring the database adapters
     * and the default database.
     */
    public static final String DATABASE_KEY = "database";

    /**
     * The key used to configure the name of the default database.
     */
    public static final String DEFAULT_KEY = "default";

    /**
     * Name of property that specifies the default map builder and map.
     * @deprecated is not used any more. Use DATABASE_KEY and
     *             DEFAULT_KEY instead
     */
    public static final String DATABASE_DEFAULT
            = DATABASE_KEY + "." + DEFAULT_KEY;

    /**
     * A prefix for <code>Manager</code> properties in the configuration.
     */
    public static final String MANAGER_PREFIX = "managed_class.";

    /**
     * A <code>Service</code> property determining its implementing
     * class name .
     */
    public static final String MANAGER_SUFFIX = ".manager";

    /**
     * property to determine whether caching is used.
     */
    public static final String CACHE_KEY = "manager.useCache";

    /**
     * The single instance of {@link TorqueInstance} used by the
     * static API presented by this class.
     */
    private static TorqueInstance torqueSingleton = null;

    /**
     * Private constructor to prevent instantiation.
     *
     * Class contains only static method ans should therefore not be
     * instantiated.
     */
    private Torque()
    {
    }

    /**
     * Retrieves the single {@link org.apache.torque.TorqueInstance}
     * used by this class.
     *
     * @return Our singleton.
     */
    public static TorqueInstance getInstance()
    {
        if (torqueSingleton == null)
        {
            torqueSingleton = new TorqueInstance();
        }
        return torqueSingleton;
    }

    /**
     * Sets the single {@link org.apache.torque.TorqueInstance}
     * used by this class. This is used by the Avalon component
     * to make sure that only one instance of Torque exists
     *
     * @param instance Our singleton.
     */
    public static synchronized void setInstance(TorqueInstance instance)
    {
        torqueSingleton = instance;
    }

    /**
     * Initialization of Torque with a properties file.
     *
     * @param configFile The absolute path to the configuration file.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static void init(String configFile)
        throws TorqueException
    {
        getInstance().init(configFile);
    }

    /**
     * Initialization of Torque with a properties file.
     *
     * @param conf The Torque configuration.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static void init(Configuration conf)
        throws TorqueException
    {
        getInstance().init(conf);
    }

    /**
     * Determine whether Torque has already been initialized.
     *
     * @return true if Torque is already initialized
     */
    public static boolean isInit()
    {
        return getInstance().isInit();
    }

    /**
     * Sets the configuration for Torque and all dependencies.
     *
     * @param conf the Configuration
     */
    public static void setConfiguration(Configuration conf)
    {
        getInstance().setConfiguration(conf);
    }

    /**
     * Get the configuration for this component.
     *
     * @return the Configuration
     */
    public static Configuration getConfiguration()
    {
        return getInstance().getConfiguration();
    }

    /**
     * This method returns a Manager for the given name.
     *
     * @param name name of the manager
     * @return a Manager
     */
    public static AbstractBaseManager getManager(String name)
    {
        return getInstance().getManager(name);
    }

    /**
     * This methods returns either the Manager from the configuration file,
     * or the default one provided by the generated code.
     *
     * @param name name of the manager
     * @param defaultClassName the class to use if name has not been configured
     * @return a Manager
     */
    public static AbstractBaseManager getManager(String name,
            String defaultClassName)
    {
        return getInstance().getManager(name, defaultClassName);
    }

    /**
     * Shuts down the service.
     *
     * This method halts the IDBroker's daemon thread in all of
     * the DatabaseMap's. It also closes all SharedPoolDataSourceFactories
     * and PerUserPoolDataSourceFactories initialized by Torque.
     * @exception TorqueException if a DataSourceFactory could not be closed
     *            cleanly. Only the first exception is rethrown, any following
     *            exceptions are logged but ignored.
     */
    public static void shutdown()
        throws TorqueException
    {
        getInstance().shutdown();
    }

    /**
     * Returns the default database map information.
     *
     * @return A DatabaseMap.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static DatabaseMap getDatabaseMap()
        throws TorqueException
    {
        return getInstance().getDatabaseMap();
    }

    /**
     * Returns the database map information. Name relates to the name
     * of the connection pool to associate with the map.
     *
     * @param name The name of the database corresponding to the
     *        <code>DatabaseMap</code> to retrieve.
     * @return The named <code>DatabaseMap</code>.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static DatabaseMap getDatabaseMap(String name)
        throws TorqueException
    {
        return getInstance().getDatabaseMap(name);
    }

    /**
     * Register a MapBuilder
     *
     * @param className the MapBuilder
     */
    public static void registerMapBuilder(String className)
    {
        getInstance().registerMapBuilder(className);
    }

    /**
     * This method returns a Connection from the default pool.
     *
     * @return The requested connection.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static Connection getConnection()
        throws TorqueException
    {
        return getInstance().getConnection();
    }

    /**
     * This method returns a Connecton using the given database name.
     *
     * @param name The database name.
     * @return a database connection
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static Connection getConnection(String name)
        throws TorqueException
    {
        return getInstance().getConnection(name);
    }

    /**
     * This method returns a Connecton using the given parameters.
     * You should only use this method if you need user based access to the
     * database!
     *
     * @param name The database name.
     * @param username The name of the database user.
     * @param password The password of the database user.
     * @return A Connection.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static Connection getConnection(String name, String username,
            String password)
            throws TorqueException
    {
        return getInstance().getConnection(name, username, password);
    }
    /**
     * Returns database adapter for a specific connection pool.
     *
     * @param name A pool name.
     * @return The corresponding database adapter.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static DB getDB(String name) throws TorqueException
    {
        return getInstance().getDB(name);
    }

    /**
     * Returns the name of the default database.
     *
     * @return name of the default DB, or null if Torque is not initialized yet
     */
    public static String getDefaultDB()
    {
        return getInstance().getDefaultDB();
    }

    /**
     * Closes a connection.
     *
     * @param con A Connection to close.
     */
    public static void closeConnection(Connection con)
    {
        getInstance().closeConnection(con);
    }

    /**
     * Sets the current schema for a database connection
     *
     * @param name The database name.
     * @param schema The current schema name
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static void setSchema(String name, String schema)
            throws TorqueException
    {
        getInstance().setSchema(name, schema);
    }

    /**
     * This method returns the current schema for a database connection
     *
     * @param name The database name.
     * @return The current schema name. Null means, no schema has been set.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static String getSchema(String name)
        throws TorqueException
    {
        return getInstance().getSchema(name);
    }

    /**
     * Returns the database for the given key.
     *
     * @param name The database name.
     * @return the Database for the given name, or null if no database exists
     *         for the given name.
     * @throws TorqueException if Torque is not yet initialized.
     */
    public static Database getDatabase(String name) throws TorqueException
    {
        return getInstance().getDatabase(name);
    }

    /**
     * Returns a Map containing all Databases registered to Torque.
     * The key of the Map is the name of the database, and the value is the
     * database instance. <br/>
     * Note that in the very special case where a new database which
     * is not configured in Torque's configuration gets known to Torque
     * at a later time, the returned map may change, and there is no way to
     * protect you against this. However, Databases should be initialized
     * in the init() method, so this will not happen if Torque is used
     * properly.
     *
     * @return a Map containing all Databases known to Torque, never null.
     * @throws TorqueException if Torque is not yet initialized.
     */
    public static Map getDatabases() throws TorqueException
    {
        return getInstance().getDatabases();
    }
}
