package org.apache.torque;

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

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.stratum.lifecycle.Configurable;
import org.apache.stratum.lifecycle.Disposable;
import org.apache.stratum.lifecycle.Initializable;

import org.apache.torque.adapter.DB;
import org.apache.torque.adapter.DBFactory;
import org.apache.torque.dsfactory.DataSourceFactory;
import org.apache.torque.manager.AbstractBaseManager;
import org.apache.torque.map.DatabaseMap;
import org.apache.torque.map.TableMap;
import org.apache.torque.oid.IDBroker;
import org.apache.torque.oid.IDGeneratorFactory;
import org.apache.torque.util.BasePeer;

/**
 * The implementation of Torque.
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
    implements Initializable,
               Configurable,
               Disposable
{
    /**
     * Name of property that specifies the default map builder and map.
     */
    public static final String DATABASE_DEFAULT = "database.default";

    /**
     * A constant for <code>default</code>.
     */
    private static final String DEFAULT_NAME = "default";

    /**
     * A prefix for <code>Manager</code> properties in
     * the configuration.
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
     * The db name that is specified as the default in the property file
     */
    private static String defaultDBName;

    /**
     * The global cache of database maps
     */
    private static Map dbMaps;

    /**
     * The cache of DataSourceFactory's
     */
    private static Map dsFactoryMap;

    /**
     * The cache of DB adapter keys
     */
    private static Map adapterMap;

    /**
     * A repository of Manager instances.
     */
    protected static Map managers = new HashMap();

    /**
     * The logging logger.
     */
    private static Log log = LogFactory.getLog(Torque.class);

    /**
     * Torque-specific configuration.
     */
    private static Configuration configuration;

    /**
     * flag to set to true once this class has been initialized
     */
    private static boolean isInit = false;

    /**
     * Store mapbuilder classnames for peers that have been referenced prior
     * to Torque being initialized.  This can happen if torque om/peer objects
     * are serialized then unserialized prior to Torque being reinitialized.
     * This condition exists in a normal catalina restart.
     */
    private static List mapBuilders
            = Collections.synchronizedList(new ArrayList());


    /**
     * initialize Torque
     *
     * @see org.apache.stratum.lifecycle.Initializable
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public void initialize() throws TorqueException
    {
        if (configuration == null)
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
        Configuration originalConf = configuration;
        configuration = configuration.subset("torque");

        if (configuration == null || configuration.isEmpty())
        {
            // If there are no properties in the "torque" namespace
            // then try the "services.DatabaseService" namespace. This
            // will soon be deprecated.
            configuration = originalConf.subset("services.DatabaseService");
            if (configuration != null && !configuration.isEmpty())
            {
                /*
                logger.warn("Torque's 'services.DatabaseService.' " +
                              "configuration prefix has been deprecated in " +
                              "favor of the 'torque.' prefix");
                */
            }
            else
            {
                // Assume the original configuration already had any
                // prefixes stripped.
                configuration = originalConf;
            }
        }

        dbMaps = new HashMap();
        initAdapters(configuration);
        initDataSourceFactories(configuration);

        isInit = true;
        for (Iterator i = mapBuilders.iterator(); i.hasNext();)
        {
            //this will add any maps in this builder to the proper database map
            BasePeer.getMapBuilder((String) i.next());
        }
        // any further mapBuilders will be called/built on demand
        mapBuilders = null;

        // setup manager mappings
        initManagerMappings(configuration);
    }

    /**
     *
     * @param configuration the Configuration representing the properties file
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    private static final void initAdapters(Configuration configuration)
        throws TorqueException
    {
        log.debug("Starting initAdapters");
        adapterMap = new HashMap();
        Configuration c = configuration.subset("database");
        if (c != null)
        {
            try
            {
                Iterator i = c.getKeys();
                while (i.hasNext())
                {
                    String key = (String) i.next();
                    if (key.endsWith("adapter"))
                    {
                        String adapter = c.getString(key);
                        String handle = key.substring(0, key.indexOf('.'));
                        DB db = DBFactory.create(adapter);
                        // register the adapter for this name
                        adapterMap.put(handle, db);
                    }
                }
            }
            catch (Exception e)
            {
                log.error("", e);
                throw new TorqueException(e);
            }
        }
        else
        {
            log.warn("There were no adapters in the configuration.");
        }
    }

    /**
     *
     * @param configuration the Configuration representing the properties file
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    private static void initDataSourceFactories(Configuration configuration)
        throws TorqueException
    {
        log.debug("Starting initDSF");
        dsFactoryMap = new HashMap();
        Configuration c = configuration.subset("dsfactory");
        if (c != null)
        {
            try
            {
                Iterator i = c.getKeys();
                while (i.hasNext())
                {
                    String key = (String) i.next();
                    if (key.endsWith("factory"))
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
                log.error("", e);
                throw new TorqueException(e);
            }
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
        String defaultDB = getDefaultDB();

        if (dsFactoryMap.get(DEFAULT_NAME) == null
            && !defaultDB.equals(DEFAULT_NAME))
        {
            log.debug("Adding a dummy entry for "
                           + DEFAULT_NAME + ", mapped onto " + defaultDB);
            dsFactoryMap.put(DEFAULT_NAME, dsFactoryMap.get(defaultDB));
        }
    }

    /**
     * configure torque
     *
     * @param config Configuration
     * @see org.apache.stratum.lifecycle.Configurable
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public void configure(Configuration config) throws TorqueException
    {
        configuration = config;
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
        try
        {
            Configuration c = (Configuration)
                    new PropertiesConfiguration(configFile);
            init(c);
        }
        catch (IOException e)
        {
            throw new TorqueException(e);
        }
    }

    /**
     * Initialization of Torque with a properties file.
     *
     * @param c The Torque configuration.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static void init(Configuration c)
        throws TorqueException
    {
        Torque torque = new Torque();
        torque.configure(c);
        torque.initialize();
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
     * @param configuration the Configuration representing the properties file
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    protected static void initManagerMappings(Configuration configuration)
        throws TorqueException
    {
        int pref = MANAGER_PREFIX.length();
        int suff = MANAGER_SUFFIX.length();

        Iterator keys = configuration.getKeys();

        while (keys.hasNext())
        {
            String key = (String) keys.next();

            if (key.startsWith(MANAGER_PREFIX)
                    && key.endsWith(MANAGER_SUFFIX))
            {
                String managedClassKey = key.substring(pref,
                        key.length() - suff);
                if (!managers.containsKey(managedClassKey))
                {
                    String managerClass = configuration.getString(key);
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
    private static synchronized void initManager(String name, String className)
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
                    throw new TorqueException(
                        "Could not instantiate manager associated with class: "
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
    public static boolean isInit()
    {
        return isInit;
    }

    /**
     * Sets the configuration for Torque and all dependencies.
     *
     * @param c the Configuration
     */
    public static void setConfiguration(Configuration c)
    {
        configuration = c;
    }

    /**
     * Get the configuration for this component.
     *
     * @return the Configuration
     */
    public static Configuration getConfiguration()
    {
        return configuration;
    }

    /**
     * This method returns a Manager for the given name.
     *
     * @param name name of the manager
     * @return a Manager
     */
    public static AbstractBaseManager getManager(String name)
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
    public static AbstractBaseManager getManager(String name,
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
     * Shuts down the service, Lifecycle style
     *
     */
    public void dispose()
    {
        shutdown();
    }

    /**
     * Shuts down the service.
     *
     * This method halts the IDBroker's daemon thread in all of
     * the DatabaseMap's.
     */
    public static void shutdown()
    {
        if (dbMaps != null)
        {
            Iterator maps = dbMaps.values().iterator();
            while (maps.hasNext())
            {
                DatabaseMap map = (DatabaseMap) maps.next();
                IDBroker idBroker = map.getIDBroker();
                if (idBroker != null)
                {
                    idBroker.stop();
                }
            }
        }
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
    public static DatabaseMap getDatabaseMap(String name)
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
    private static final DatabaseMap initDatabaseMap(String name)
        throws TorqueException
    {
        DatabaseMap map = new DatabaseMap(name);

        // Add info about IDBroker's table.
        setupIdTable(map);

        // Setup other ID generators for this map.
        try
        {
            String key = getDatabaseProperty(name, "driver");
            if (key == null || key.length() == 0)
            {
                key = getDatabaseProperty(name, "adapter");
            }
            DB db = DBFactory.create(key);
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
    public static void registerMapBuilder(String className)
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
    private static String getDatabaseProperty(String db, String prop)
    {
        return configuration.getString(new StringBuffer("database.")
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
     * This method returns a Connection from the default pool.
     *
     * @return The requested connection.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static Connection getConnection()
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
    public static Connection getConnection(String name)
        throws TorqueException
    {
        Connection con = null;
        DataSourceFactory dsf = null;
        try
        {
            dsf = (DataSourceFactory) dsFactoryMap.get(name);
            con = dsf.getDataSource().getConnection();
        }
        catch (Exception e)
        {
             if (dsf == null && e instanceof NullPointerException)
             {
                 throw new NullPointerException(
                     "There was no DataSourceFactory "
                     + "configured for the connection " + name);
             }
             else
             {
                 throw new TorqueException(e);
             }
        }
        return con;
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
        Connection con = null;
        DataSourceFactory dsf = null;
        try
        {
            dsf = (DataSourceFactory) dsFactoryMap.get(name);
            con = dsf.getDataSource().getConnection(username, password);
        }
        catch (Exception e)
        {
             if (dsf == null && e instanceof NullPointerException)
             {
                 throw new NullPointerException(
                         "There was no DataSourceFactory "
                         + "configured for the connection " + name);
             }
             else
             {
                 throw new TorqueException(e);
             }
        }
        return con;
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
        return (DB) adapterMap.get(name);
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Returns the name of the default database.
     *
     * @return name of the default DB
     */
    public static String getDefaultDB()
    {
        if (configuration == null)
        {
            return DEFAULT_NAME;
        }
        else if (defaultDBName == null)
        {
            // Determine default database name.
            defaultDBName =
                configuration.getString(DATABASE_DEFAULT, DEFAULT_NAME).trim();
        }

        return defaultDBName;
    }

    /**
     * Closes a connection.
     *
     * @param con A Connection to close.
     */
    public static void closeConnection(Connection con)
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
}
