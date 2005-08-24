package org.apache.torque;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.torque.adapter.DB;
import org.apache.torque.adapter.DBFactory;
import org.apache.torque.dsfactory.AbstractDataSourceFactory;
import org.apache.torque.dsfactory.DataSourceFactory;
import org.apache.torque.manager.AbstractBaseManager;
import org.apache.torque.map.DatabaseMap;
import org.apache.torque.map.TableMap;
import org.apache.torque.oid.IDBroker;
import org.apache.torque.oid.IDGeneratorFactory;
import org.apache.torque.util.BasePeer;

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
 * @version $Id$
 */
public class TorqueInstance
{
    /** Logging */
    private static Log log = LogFactory.getLog(TorqueInstance.class);

    /** The db name that is specified as the default in the property file */
    private String defaultDBName = null;

    /** The global cache of database maps */
    private Map dbMaps;

    /** The cache of DataSourceFactory's */
    private Map dsFactoryMap;

    /** The cache of DB adapter keys */
    private Map adapterMap;

    /** A repository of Manager instances. */
    private Map managers;

    /** Torque-specific configuration. */
    private Configuration conf;

    /** flag to set to true once this class has been initialized */
    private boolean isInit = false;

    /**
     * Store mapbuilder classnames for peers that have been referenced prior
     * to Torque being initialized.  This can happen if torque om/peer objects
     * are serialized then unserialized prior to Torque being reinitialized.
     * This condition exists in a normal catalina restart.
     */
    private List mapBuilders = null;

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

        dbMaps = new HashMap();
        for (Iterator i = mapBuilders.iterator(); i.hasNext();)
        {
            //this will add any maps in this builder to the proper database map
            BasePeer.getMapBuilder((String) i.next());
        }
        // any further mapBuilders will be called/built on demand
        mapBuilders = null;

        // setup manager mappings
        initManagerMappings(conf);

        isInit = true;
    }
    
    
    /**
     * initializes the name of the default database
     * @param conf the configuration representing the torque section 
     *        of the properties file
     * @throws TorqueException if the appropriate key is not set
     */
    private final void initDefaultDbName(Configuration conf)
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
     *
     * @param conf the Configuration representing the torque section of the
     *        properties file
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    private final void initAdapters(Configuration conf)
            throws TorqueException
    {
        log.debug("initAdapters(" + conf + ")");
        adapterMap = new HashMap();

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
            for (Iterator it = c.getKeys(); it.hasNext(); )
            {
                String key = (String) it.next();
                if (key.endsWith(DB.ADAPTER_KEY))
                {
                    String adapter = c.getString(key);
                    String handle = key.substring(0, key.indexOf('.'));
                    DB db = DBFactory.create(adapter);
                    // register the adapter for this name
                    adapterMap.put(handle, db);
                    log.debug("Adding " + adapter + " -> " + handle + " as Adapter");
                }
            }
        }
        catch (Exception e)
        {
            log.error("Error reading configuration seeking database "
                      + "adapters", e);
            throw new TorqueException(e);
        }
        
        if (adapterMap.get(Torque.getDefaultDB()) == null)
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
     *
     * @param conf the Configuration representing the properties file
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    private void initDataSourceFactories(Configuration conf)
            throws TorqueException
    {
        log.debug("initDataSourceFactories(" + conf + ")");
        dsFactoryMap = new HashMap();
        
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
                    dsFactoryMap.put(handle, dsf);
                }
            }
        }
        catch (Exception e)
        {
            log.error("Error reading adapter configuration", e);
            throw new TorqueException(e);
        }

        if (dsFactoryMap.get(Torque.getDefaultDB()) == null)
        {
            String error = "Invalid configuration : "
                    + "No DataSourceFactory definition for default DB found. "
                    + "A DataSourceFactory must be defined under the key"
                    + Torque.TORQUE_KEY
                    + "."
                    + DataSourceFactory.DSFACTORY_KEY
                    + "."
                    + Torque.getDefaultDB()
                    + "."
                    + DataSourceFactory.FACTORY_KEY;
            log.error(error);
            throw new TorqueException(error);
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
            Configuration conf = new PropertiesConfiguration(configFile);

            log.debug("Config Object is " + conf);
            init(conf);
        }
        catch (ConfigurationException e)
        {
            throw new TorqueException(e);
        }
    }

    /**
     * Initialization of Torque with a properties file.
     *
     * @param conf The Torque configuration.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public void init(Configuration conf)
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
     *
     * @param conf the Configuration
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
        if (dbMaps != null)
        {
            for (Iterator it = dbMaps.values().iterator(); it.hasNext();)
            {
                DatabaseMap map = (DatabaseMap) it.next();
                IDBroker idBroker = map.getIDBroker();
                if (idBroker != null)
                {
                    idBroker.stop();
                }
            }
        }
        TorqueException exception = null;
        for (Iterator it = dsFactoryMap.keySet().iterator(); it.hasNext();)
        {
            Object dsfKey = it.next();
            DataSourceFactory dsf 
                    = (DataSourceFactory) dsFactoryMap.get(dsfKey);
            try 
            {
                dsf.close();
                it.remove();
            }
            catch (TorqueException e)
            {
                log.error("Error while closing the DataSourceFactory "
                        + dsfKey, 
                        e);
                if (exception == null)
                {
                	exception = e;
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
        mapBuilders = Collections.synchronizedList(new ArrayList());
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

        if (dbMaps == null)
        {
            throw new TorqueException("Torque was not initialized properly.");
        }

        synchronized (dbMaps)
        {
            DatabaseMap map = (DatabaseMap) dbMaps.get(name);
            if (map == null)
            {
                // Still not there.  Create and add.
                map = initDatabaseMap(name);
            }
            return map;
        }
    }

    /**
     * Creates and initializes the mape for the named database.
     * Assumes that <code>dbMaps</code> member is sync'd.
     *
     * @param name The name of the database to map.
     * @return The desired map.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    private final DatabaseMap initDatabaseMap(String name)
            throws TorqueException
    {
        DatabaseMap map = new DatabaseMap(name);

        // Add info about IDBroker's table.
        setupIdTable(map);

        // Setup other ID generators for this map.
        try
        {
            String key = getDatabaseProperty(name, "adapter");
            if (StringUtils.isEmpty(key))
            {
                key = getDatabaseProperty(name, "driver");
            }
            DB db = DBFactory.create(key);
            for (int i = 0; i < IDGeneratorFactory.ID_GENERATOR_METHODS.length;
                 i++)
            {
                map.addIdGenerator(IDGeneratorFactory.ID_GENERATOR_METHODS[i],
                        IDGeneratorFactory.create(db, name));
            }
        }
        catch (java.lang.InstantiationException e)
        {
            throw new TorqueException(e);
        }

        // Avoid possible ConcurrentModificationException by
        // constructing a copy of dbMaps.
        Map newMaps = new HashMap(dbMaps);
        newMaps.put(name, map);
        dbMaps = newMaps;

        return map;
    }

    /**
     * Register a MapBuilder
     *
     * @param className the MapBuilder
     */
    public void registerMapBuilder(String className)
    {
        mapBuilders.add(className);
    }

    /**
     * Returns the specified property of the given database, or the empty
     * string if no value is set for the property.
     *
     * @param db   The name of the database whose property to get.
     * @param prop The name of the property to get.
     * @return     The property's value.
     */
    private String getDatabaseProperty(String db, String prop)
    {
        return conf.getString(new StringBuffer("database.")
                .append(db)
                .append('.')
                .append(prop)
                .toString(), "");
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
    private final void setupIdTable(DatabaseMap map)
    {
        map.setIdTable("ID_TABLE");
        TableMap tMap = map.getIdTable();
        tMap.addPrimaryKey("ID_TABLE_ID", new Integer(0));
        tMap.addColumn("TABLE_NAME", "");
        tMap.addColumn("NEXT_ID", new Integer(0));
        tMap.addColumn("QUANTITY", new Integer(0));
    }

    /**
     * This method returns a Connection from the default pool.
     *
     * @return The requested connection.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public Connection getConnection()
            throws TorqueException
    {
        return getConnection(getDefaultDB());
    }

    /**
     *
     * @param name The database name.
     * @return a database connection
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public Connection getConnection(String name)
            throws TorqueException
    {
        Connection con = null;
        DataSourceFactory dsf = null;

        try
        {
            return getDataSourceFactory(name).getDataSource().getConnection();
        }
        catch(SQLException se)
        {
            throw new TorqueException(se);
        }
    }

    /**
     * Returns a DataSourceFactory 
     *
     * @param name Name of the DSF to get
     * @return A DataSourceFactory object
     */
    protected DataSourceFactory getDataSourceFactory(String name)
            throws TorqueException
    {
    	if (!isInit()) 
    	{
            throw new TorqueException("Torque is not initialized.");    		
    	}

    	DataSourceFactory dsf = null;

        try
        {
            dsf = (DataSourceFactory) dsFactoryMap.get(name);
        }
        catch (Exception e)
        {
            throw new TorqueException(e);
        }
            
        if (dsf == null)
        {
            throw new NullPointerException(
                    "There was no DataSourceFactory "
                    + "configured for the connection " + name);
        }

        return dsf;
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
    public Connection getConnection(String name, String username,
            String password)
            throws TorqueException
    {
        try
        {
            return getDataSourceFactory(name).getDataSource().getConnection(username, password);
        }
        catch(SQLException se)
        {
            throw new TorqueException(se);
        }
    }

    /**
     * Returns database adapter for a specific connection pool.
     *
     * @param name A pool name.
     * @return The corresponding database adapter.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public DB getDB(String name) throws TorqueException
    {
        return (DB) adapterMap.get(name);
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
     * @param schema The current schema name
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public void setSchema(String name, String schema)
            throws TorqueException
    {
        getDataSourceFactory(name).setSchema(schema);
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
        return getDataSourceFactory(name).getSchema();
    }

}
