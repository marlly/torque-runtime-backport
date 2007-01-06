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
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.torque.adapter.DB;
import org.apache.torque.adapter.DBFactory;
import org.apache.torque.dsfactory.DataSourceFactory;
import org.apache.torque.manager.AbstractBaseManager;
import org.apache.torque.map.DatabaseMap;
import org.apache.torque.map.MapBuilder;
import org.apache.torque.oid.IDBroker;
import org.apache.torque.oid.IDGeneratorFactory;

/**
 * The core of Torque's implementation.  Both the classic {@link
 * org.apache.torque.Torque} static wrapper and the {@link
 * org.apache.torque.avalon.TorqueComponent} <a
 * href="http://avalon.apache.org/">Avalon</a> implementation leverage
 * this class.
 *
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @author <a href="mailto:magnus@handtolvur.is">Magn�s ��r Torfason</a>
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:Rafal.Krzewski@e-point.pl">Rafal Krzewski</a>
 * @author <a href="mailto:mpoeschl@marmot.at">Martin Poeschl</a>
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @author <a href="mailto:kschrader@karmalab.org">Kurt Schrader</a>
 * @author <a href="mailto:tv@apache.org">Thomas Vandahl</a>
 * @version $Id$
 */
public class TorqueInstance
{
    /** Logging */
    private static Log log = LogFactory.getLog(TorqueInstance.class);

    /** A constant for <code>default</code>. */
    private static final String DEFAULT_NAME = "default";

    /** The db name that is specified as the default in the property file */
    private String defaultDBName = null;

    /**
     * The Map which contains all known databases. All iterations over the map
     * and other operations where the databaase map needs to stay
     * in a defined state must be synchronized to this map.
     */
    private Map databases = Collections.synchronizedMap(new HashMap());

    /** A repository of Manager instances. */
    private Map managers;

    /** Torque-specific configuration. */
    private Configuration conf;

    /** flag to set to true once this class has been initialized */
    private boolean isInit = false;

    /**
     * a flag which indicates whether the DataSourceFactory in the database
     * named <code>DEFAULT</code> is a reference to another
     * DataSourceFactory. This is important to know when closing the
     * DataSourceFactories on shutdown();
     */
    private boolean defaultDsfIsReference = false;

    /**
     * Store mapbuilder classnames for peers that have been referenced prior
     * to Torque being initialized.  This can happen if torque om/peer objects
     * are serialized then unserialized prior to Torque being reinitialized.
     * This condition exists in a normal catalina restart.
     */
    private Map mapBuilderCache = null;

    /**
     * Creates a new instance with default configuration.
     *
     * @see #resetConfiguration()
     */
    public TorqueInstance()
    {
        resetConfiguration();
    }

    /**
     * Initializes this instance of Torque.
     *
     * @see org.apache.stratum.lifecycle.Initializable
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    private synchronized void initialize() throws TorqueException
    {
        log.debug("initialize()");

        if (isInit)
        {
            log.debug("Multiple initializations of Torque attempted");
            return;
        }

        if (conf == null || conf.isEmpty())
        {
            throw new TorqueException("Torque cannot be initialized without "
                    + "a valid configuration. Please check the log files "
                    + "for further details.");
        }

        // Now that we have dealt with processing the log4j properties
        // that may be contained in the configuration we will make the
        // configuration consist only of the remain torque specific
        // properties that are contained in the configuration. First
        // look for properties that are in the "torque" namespace.

        Configuration subConf = conf.subset(Torque.TORQUE_KEY);
        if (subConf == null || subConf.isEmpty())
        {
            String error = ("Invalid configuration. No keys starting with "
                    + Torque.TORQUE_KEY
                    + " found in configuration");
            log.error(error);
            throw new TorqueException(error);
        }
        setConfiguration(subConf);

        initDefaultDbName(conf);
        initAdapters(conf);
        initDataSourceFactories(conf);

        // setup manager mappings
        initManagerMappings(conf);

        isInit = true;

        // re-build any MapBuilders that may have gone lost during serialization 
        synchronized (mapBuilderCache)
        {
            for (Iterator i = mapBuilderCache.entrySet().iterator(); i.hasNext();)
            {
                Map.Entry entry = (Map.Entry)i.next();
                
                if (null == entry.getValue())
                {
                    try
                    {
                        // create and build the MapBuilder
                        MapBuilder builder = (MapBuilder) Class.forName((String) entry.getKey()).newInstance();
        
                        if (!builder.isBuilt())
                        {
                            builder.doBuild();
                        }
    
                        entry.setValue(builder);
                    }
                    catch (Exception e)
                    {
                        throw new TorqueException(e);
                    }
                }
            }
        }
    }


    /**
     * Initializes the name of the default database and
     * associates the database with the name <code>DEFAULT_NAME</code>
     * to the default database.
     *
     * @param conf the configuration representing the torque section.
     *        of the properties file.
     * @throws TorqueException if the appropriate key is not set.
     */
    private void initDefaultDbName(Configuration conf)
            throws TorqueException
    {
        // Determine default database name.
        defaultDBName =
                conf.getString(
                        Torque.DATABASE_KEY
                        + "."
                        + Torque.DEFAULT_KEY);
        if (defaultDBName == null)
        {
            String error = "Invalid configuration: Key "
                    + Torque.TORQUE_KEY
                    + "."
                    + Torque.DATABASE_KEY
                    + "."
                    + Torque.DEFAULT_KEY
                    + " not set";
            log.error(error);
            throw new TorqueException(error);
        }
    }

    /**
     * Reads the adapter settings from the configuration and
     * assigns the appropriate database adapters and Id Generators
     * to the databases.
     *
     * @param conf the Configuration representing the torque section of the
     *        properties file
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    private void initAdapters(Configuration conf)
            throws TorqueException
    {
        log.debug("initAdapters(" + conf + ")");

        Configuration c = conf.subset(Torque.DATABASE_KEY);
        if (c == null || c.isEmpty())
        {
            String error = "Invalid configuration : "
                    + "No keys starting with "
                    + Torque.TORQUE_KEY
                    + "."
                    + Torque.DATABASE_KEY
                    + " found in configuration";
            log.error(error);
            throw new TorqueException(error);
        }

        try
        {
            for (Iterator it = c.getKeys(); it.hasNext();)
            {
                String key = (String) it.next();
                if (key.endsWith(DB.ADAPTER_KEY)
                        || key.endsWith(DB.DRIVER_KEY))
                {
                    String adapter = c.getString(key);
                    String handle = key.substring(0, key.indexOf('.'));

                    DB db;

                    db = DBFactory.create(adapter);

                    // Not supported, try manually defined adapter class
                    if (db == null)
                    {
                        String adapterClassName = c.getString(key + "." + adapter + ".className", null);
                        db = DBFactory.create(adapter, adapterClassName);
                    }

                    Database database = getOrCreateDatabase(handle);

                    // register the adapter for this name
                    database.setAdapter(db);
                    log.debug("Adding " + adapter + " -> "
                            + handle + " as Adapter");

                    // add Id generators

                    // first make sure that the dtabaseMap exists for the name
                    // as the idGenerators are still stored in the database map
                    // TODO: change when the idGenerators are stored in the
                    // database
                    getDatabaseMap(handle);
                    for (int i = 0;
                            i < IDGeneratorFactory.ID_GENERATOR_METHODS.length;
                            i++)
                    {
                        database.addIdGenerator(
                                IDGeneratorFactory.ID_GENERATOR_METHODS[i],
                                IDGeneratorFactory.create(db, handle));
                    }
                }
            }
        }
        catch (InstantiationException e)
        {
            log.error("Error creating a database adapter instance", e);
            throw new TorqueException(e);
        }
        catch (TorqueException e)
        {
            log.error("Error reading configuration seeking database "
                      + "adapters", e);
            throw new TorqueException(e);
        }

        // check that at least the default database has got an adapter.
        Database defaultDatabase
                = (Database) databases.get(Torque.getDefaultDB());
        if (defaultDatabase == null
            || defaultDatabase.getAdapter() == null)
        {
            String error = "Invalid configuration : "
                    + "No adapter definition found for default DB "
                    + "An adapter must be defined under "
                    + Torque.TORQUE_KEY
                    + "."
                    + Torque.DATABASE_KEY
                    + "."
                    + Torque.getDefaultDB()
                    + "."
                    + DB.ADAPTER_KEY;
            log.error(error);
            throw new TorqueException(error);
        }
    }

    /**
     * Reads the settings for the DataSourceFactories from the configuration
     * and creates and/or cinfigures the DataSourceFactories for the databases.
     * If no DataSorceFactory is assigned to the database with the name
     * <code>DEFAULT_NAME</code>, a reference to the DataSourceFactory
     * of the default daztabase is made from the database with the name
     * <code>DEFAULT_NAME</code>.
     *
     * @param conf the Configuration representing the properties file
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    private void initDataSourceFactories(Configuration conf)
            throws TorqueException
    {
        log.debug("initDataSourceFactories(" + conf + ")");

        Configuration c = conf.subset(DataSourceFactory.DSFACTORY_KEY);
        if (c == null || c.isEmpty())
        {
            String error = "Invalid configuration: "
                    + "No keys starting with "
                    + Torque.TORQUE_KEY
                    + "."
                    + DataSourceFactory.DSFACTORY_KEY
                    + " found in configuration";
            log.error(error);
            throw new TorqueException(error);
        }

        try
        {
            for (Iterator it = c.getKeys(); it.hasNext();)
            {
                String key = (String) it.next();
                if (key.endsWith(DataSourceFactory.FACTORY_KEY))
                {
                    String classname = c.getString(key);
                    String handle = key.substring(0, key.indexOf('.'));
                    log.debug("handle: " + handle
                            + " DataSourceFactory: " + classname);
                    Class dsfClass = Class.forName(classname);
                    DataSourceFactory dsf =
                            (DataSourceFactory) dsfClass.newInstance();
                    dsf.initialize(c.subset(handle));

                    Database database = getOrCreateDatabase(handle);
                    database.setDataSourceFactory(dsf);
                }
            }
        }
        catch (RuntimeException e)
        {
            log.error("Runtime Error reading adapter configuration", e);
            throw new TorqueRuntimeException(e);
        }
        catch (Exception e)
        {
            log.error("Error reading adapter configuration", e);
            throw new TorqueException(e);
        }

        Database defaultDatabase
                = (Database) databases.get(defaultDBName);
        if (defaultDatabase == null
            || defaultDatabase.getDataSourceFactory() == null)
        {
            String error = "Invalid configuration : "
                    + "No DataSourceFactory definition for default DB found. "
                    + "A DataSourceFactory must be defined under the key"
                    + Torque.TORQUE_KEY
                    + "."
                    + DataSourceFactory.DSFACTORY_KEY
                    + "."
                    + defaultDBName
                    + "."
                    + DataSourceFactory.FACTORY_KEY;
            log.error(error);
            throw new TorqueException(error);
        }

        // As there might be a default database configured
        // to map "default" onto an existing datasource, we
        // must check, whether there _is_ really an entry for
        // the "default" in the dsFactoryMap or not. If it is
        // not, then add a dummy entry for the "default"
        //
        // Without this, you can't actually access the "default"
        // data-source, even if you have an entry like
        //
        // database.default = bookstore
        //
        // in your Torque.properties
        //

        {
            Database databaseInfoForKeyDefault
                    = getOrCreateDatabase(DEFAULT_NAME);
            if ((!defaultDBName.equals(DEFAULT_NAME))
                && databaseInfoForKeyDefault.getDataSourceFactory() == null)
            {
                log.debug("Adding the DatasourceFactory from database "
                        + defaultDBName
                        + " onto database " + DEFAULT_NAME);
                databaseInfoForKeyDefault.setDataSourceFactory(
                        defaultDatabase.getDataSourceFactory());
                this.defaultDsfIsReference = true;
            }
        }

    }

    /**
     * Initialization of Torque with a properties file.
     *
     * @param configFile The absolute path to the configuration file.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public void init(String configFile)
            throws TorqueException
    {
        log.debug("init(" + configFile + ")");
        try
        {
            Configuration configuration
                    = new PropertiesConfiguration(configFile);

            log.debug("Config Object is " + configuration);
            init(configuration);
        }
        catch (ConfigurationException e)
        {
            throw new TorqueException(e);
        }
    }

    /**
     * Initialization of Torque with a Configuration object.
     *
     * @param conf The Torque configuration.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public synchronized void init(Configuration conf)
            throws TorqueException
    {
        log.debug("init(" + conf + ")");
        setConfiguration(conf);
        initialize();
    }


    /**
     * Creates a mapping between classes and their manager classes.
     *
     * The mapping is built according to settings present in
     * properties file.  The entries should have the
     * following form:
     *
     * <pre>
     * torque.managed_class.com.mycompany.Myclass.manager= \
     *          com.mycompany.MyManagerImpl
     * services.managed_class.com.mycompany.Myotherclass.manager= \
     *          com.mycompany.MyOtherManagerImpl
     * </pre>
     *
     * <br>
     *
     * Generic ServiceBroker provides no Services.
     *
     * @param conf the Configuration representing the properties file
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    protected void initManagerMappings(Configuration conf)
            throws TorqueException
    {
        int pref = Torque.MANAGER_PREFIX.length();
        int suff = Torque.MANAGER_SUFFIX.length();

        for (Iterator it = conf.getKeys(); it.hasNext();)
        {
            String key = (String) it.next();

            if (key.startsWith(Torque.MANAGER_PREFIX)
                    && key.endsWith(Torque.MANAGER_SUFFIX))
            {
                String managedClassKey = key.substring(pref,
                        key.length() - suff);
                if (!managers.containsKey(managedClassKey))
                {
                    String managerClass = conf.getString(key);
                    log.info("Added Manager for Class: " + managedClassKey
                            + " -> " + managerClass);
                    try
                    {
                        initManager(managedClassKey, managerClass);
                    }
                    catch (TorqueException e)
                    {
                        // the exception thrown here seems to disappear.
                        // At least when initialized by Turbine, should find
                        // out why, but for now make sure it is noticed.
                        log.error("", e);
                        e.printStackTrace();
                        throw e;
                    }
                }
            }
        }
    }

    /**
     * Initialize a manager
     *
     * @param name name of the manager
     * @param className name of the manager class
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    private synchronized void initManager(String name, String className)
            throws TorqueException
    {
        AbstractBaseManager manager = (AbstractBaseManager) managers.get(name);

        if (manager == null)
        {
            if (className != null && className.length() != 0)
            {
                try
                {
                    manager = (AbstractBaseManager)
                            Class.forName(className).newInstance();
                    managers.put(name, manager);
                }
                catch (Exception e)
                {
                    throw new TorqueException("Could not instantiate "
                            + "manager associated with class: "
                            + name, e);
                }
            }
        }
    }

    /**
     * Determine whether Torque has already been initialized.
     *
     * @return true if Torque is already initialized
     */
    public boolean isInit()
    {
        return isInit;
    }

    /**
     * Sets the configuration for Torque and all dependencies.
     * The prefix <code>TORQUE_KEY</code> needs to be removed from the
     * configuration keys for the provided configuration.
     *
     * @param conf the Configuration.
     */
    public void setConfiguration(Configuration conf)
    {
        log.debug("setConfiguration(" + conf + ")");
        this.conf = conf;
    }

    /**
     * Get the configuration for this component.
     *
     * @return the Configuration
     */
    public Configuration getConfiguration()
    {
        log.debug("getConfiguration() = " + conf);
        return conf;
    }

    /**
     * This method returns a Manager for the given name.
     *
     * @param name name of the manager
     * @return a Manager
     */
    public AbstractBaseManager getManager(String name)
    {
        AbstractBaseManager m = (AbstractBaseManager) managers.get(name);
        if (m == null)
        {
            log.error("No configured manager for key " + name + ".");
        }
        return m;
    }

    /**
     * This methods returns either the Manager from the configuration file,
     * or the default one provided by the generated code.
     *
     * @param name name of the manager
     * @param defaultClassName the class to use if name has not been configured
     * @return a Manager
     */
    public AbstractBaseManager getManager(String name,
            String defaultClassName)
    {
        AbstractBaseManager m = (AbstractBaseManager) managers.get(name);
        if (m == null)
        {
            log.debug("Added late Manager mapping for Class: "
                    + name + " -> " + defaultClassName);

            try
            {
                initManager(name, defaultClassName);
            }
            catch (TorqueException e)
            {
                log.error(e.getMessage(), e);
            }

            // Try again now that the default manager should be in the map
            m = (AbstractBaseManager) managers.get(name);
        }

        return m;
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
    public synchronized void shutdown()
        throws TorqueException
    {
        // stop the idbrokers
        synchronized (databases)
        {
            for (Iterator it = databases.values().iterator(); it.hasNext();)
            {
                Database database = (Database) it.next();
                IDBroker idBroker = database.getIDBroker();
                if (idBroker != null)
                {
                    idBroker.stop();
                }
            }
        }

        // shut down the data source factories
        TorqueException exception = null;
        synchronized (databases)
        {
            for (Iterator it = databases.keySet().iterator(); it.hasNext();)
            {
                Object databaseKey = it.next();

                Database database
                        = (Database) databases.get(databaseKey);
                if (DEFAULT_NAME.equals(databaseKey) && defaultDsfIsReference)
                {
                    // the DataSourceFactory of the database with the name
                    // DEFAULT_NAME is just a reference to aynother entry.
                    // Do not close because this leads to closing
                    // the same DataSourceFactory twice.
                    database.setDataSourceFactory(null);
                    break;
                }

                try
                {
                    DataSourceFactory dataSourceFactory
                            = database.getDataSourceFactory();
                    if (dataSourceFactory != null)
                    {
                        dataSourceFactory.close();
                        database.setDataSourceFactory(null);
                    }
                }
                catch (TorqueException e)
                {
                    log.error("Error while closing the DataSourceFactory "
                            + databaseKey,
                            e);
                    if (exception == null)
                    {
                        exception = e;
                    }
                }
            }
        }
        if (exception != null)
        {
            throw exception;
        }
        resetConfiguration();
    }

    /**
     * Resets some internal configuration variables to
     * their defaults.
     */
    private void resetConfiguration()
    {
        mapBuilderCache = Collections.synchronizedMap(new HashMap());
        managers = new HashMap();
        isInit = false;
    }

    /**
     * Returns the default database map information.
     *
     * @return A DatabaseMap.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public DatabaseMap getDatabaseMap()
            throws TorqueException
    {
        return getDatabaseMap(getDefaultDB());
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
    public DatabaseMap getDatabaseMap(String name)
            throws TorqueException
    {
        if (name == null)
        {
            throw new TorqueException ("DatabaseMap name was null!");
        }

        Database database = getOrCreateDatabase(name);
        return database.getDatabaseMap();
    }

    /**
     * Get the registered MapBuilders
     *
     * @return the MapBuilder cache
     * 
     */
    public Map getMapBuilders()
    {
        return mapBuilderCache;
    }

    /**
     * Register a MapBuilder
     *
     * @param className the MapBuilder
     */
    public void registerMapBuilder(String className)
    {
        mapBuilderCache.put(className, null);
    }

    /**
     * Register a MapBuilder
     *
     * @param builder the instance of the MapBuilder
     * 
     */
    public void registerMapBuilder(MapBuilder builder)
    {
        mapBuilderCache.put(builder.getClass().getName(), builder);
    }
    
    /**
     * Get a MapBuilder
     *
     * @param className of the MapBuilder
     * @return A MapBuilder, not null
     * @throws TorqueException if the Map Builder cannot be instantiated
     * 
     */
    public MapBuilder getMapBuilder(String className)
        throws TorqueException
    {
        try
        {
            MapBuilder mb = (MapBuilder)mapBuilderCache.get(className);

            if (mb == null)
            {
                mb = (MapBuilder) Class.forName(className).newInstance();
                // Cache the MapBuilder before it is built.
                mapBuilderCache.put(className, mb);
            }

            if (mb.isBuilt())
            {
                return mb;
            }

            try
            {
                mb.doBuild();
            }
            catch (Exception e)
            {
                // remove the MapBuilder from the cache if it can't be built correctly
                mapBuilderCache.remove(className);
                throw e;
            }

            return mb;
        }
        catch (Exception e)
        {
            log.error("getMapBuilder failed trying to instantiate: "
                    + className, e);
            throw new TorqueException(e);
        }
    }
    
    /**
     * This method returns a Connection from the default pool.
     *
     * @return The requested connection, never null.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public Connection getConnection()
            throws TorqueException
    {
        return getConnection(getDefaultDB());
    }

    /**
     * Returns a database connection to the database with the key
     * <code>name</code>.
     * @param name The database name.
     * @return a database connection, never null.
     * @throws TorqueException If no DataSourceFactory is configured for the
     *         named database, the connection information is wrong, or the
     *         connection cannot be returned for any other reason.
     */
    public Connection getConnection(String name)
            throws TorqueException
    {
        try
        {
            return getDatabase(name)
                    .getDataSourceFactory()
                    .getDataSource()
                    .getConnection();
        }
        catch (SQLException se)
        {
            throw new TorqueException(se);
        }
    }

    /**
     * Returns the DataSourceFactory for the database with the name
     * <code>name</code>.
     *
     * @param name The name of the database to get the DSF for.
     * @return A DataSourceFactory object, never null.
     * @throws TorqueException if Torque is not initiliaized, or
     *         no DatasourceFactory is configured for the given name.
     */
    public DataSourceFactory getDataSourceFactory(String name)
            throws TorqueException
    {
        Database database = getDatabase(name);

        DataSourceFactory dsf = null;
        if (database != null)
        {
            dsf = database.getDataSourceFactory();
        }

        if (dsf == null)
        {
            throw new TorqueException(
                    "There was no DataSourceFactory "
                    + "configured for the connection " + name);
        }

        return dsf;
    }

    /**
     * This method returns a Connection using the given parameters.
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
    public Connection getConnection(String name, String username,
            String password)
            throws TorqueException
    {
        try
        {
            return getDataSourceFactory(name)
                    .getDataSource().getConnection(username, password);
        }
        catch (SQLException se)
        {
            throw new TorqueException(se);
        }
    }

    /**
     * Returns the database adapter for a specific database.
     *
     * @param name the name of the database to get the adapter for.
     * @return The corresponding database adapter, or null if no database
     *         adapter is defined for the given database.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public DB getDB(String name) throws TorqueException
    {
        Database database = getDatabase(name);
        if (database == null)
        {
            return null;
        }
        return database.getAdapter();
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Returns the name of the default database.
     *
     * @return name of the default DB, or null if Torque is not initialized yet
     */
    public String getDefaultDB()
    {
        return defaultDBName;
    }

    /**
     * Closes a connection.
     *
     * @param con A Connection to close.
     */
    public void closeConnection(Connection con)
    {
        if (con != null)
        {
            try
            {
                con.close();
            }
            catch (SQLException e)
            {
                log.error("Error occured while closing connection.", e);
            }
        }
    }

    /**
     * Sets the current schema for a database connection
     *
     * @param name The database name.
     * @param schema The current schema name.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public void setSchema(String name, String schema)
            throws TorqueException
    {
        getOrCreateDatabase(name).setSchema(schema);
    }

    /**
     * This method returns the current schema for a database connection
     *
     * @param name The database name.
     * @return The current schema name. Null means, no schema has been set.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public String getSchema(String name)
        throws TorqueException
    {
        Database database = getDatabase(name);
        if (database == null)
        {
            return null;
        }
        return database.getSchema();
    }

    /**
     * Returns the database for the key <code>databaseName</code>.
     *
     * @param databaseName the key to get the database for.
     * @return the database for the specified key, or null if the database
     *         does not exist.
     * @throws TorqueException if Torque is not yet initialized.
     */
    public Database getDatabase(String databaseName) throws TorqueException
    {
        if (!isInit())
        {
            throw new TorqueException("Torque is not initialized.");
        }
        return (Database) databases.get(databaseName);
    }

    /**
     * Returns a Map containing all Databases registered to Torque.
     * The key of the Map is the name of the database, and the value is the
     * database instance. <br/>
     * Note that in the very special case where a new database which
     * is not configured in Torque's configuration gets known to Torque
     * at a later time, the returned map may change, and there is no way to
     * protect you against this.
     *
     * @return a Map containing all Databases known to Torque, never null.
     * @throws TorqueException if Torque is not yet initialized.
     */
    public Map getDatabases() throws TorqueException
    {
        if (!isInit())
        {
            throw new TorqueException("Torque is not initialized.");
        }
        return Collections.unmodifiableMap(databases);
    }

    /**
     * Returns the database for the key <code>databaseName</code>.
     * If no database is associated to the specified key,
     * a new database is created, mapped to the specified key, and returned.
     *
     * @param databaseName the key to get the database for.
     * @return the database associated with specified key, or the newly created
     *         database, never null.
     */
    public Database getOrCreateDatabase(String databaseName)
    {
        synchronized (databases)
        {
            Database result = (Database) databases.get(databaseName);
            if (result == null)
            {
                result = new Database(databaseName);
                databases.put(databaseName, result);
            }
            return result;
        }
    }
}
