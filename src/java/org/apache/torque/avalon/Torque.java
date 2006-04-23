package org.apache.torque.avalon;

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
import java.util.Map;

import org.apache.avalon.framework.component.Component;
import org.apache.torque.Database;
import org.apache.torque.TorqueException;
import org.apache.torque.adapter.DB;
import org.apache.torque.manager.AbstractBaseManager;
import org.apache.torque.map.DatabaseMap;

/**
 * Avalon role interface for Torque.
 *
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @author <a href="mailto:tv@apache.org">Thomas Vandahl</a>
 * @version $Id$
 */
public interface Torque
        extends Component
{
    String ROLE = Torque.class.getName();

    /*
     * ========================================================================
     *
     * Torque Methods, accessible from the Component
     *
     * ========================================================================
     */

    /**
     * Determine whether Torque has already been initialized.
     *
     * @return true if Torque is already initialized
     */
    public boolean isInit();

    /**
     * Get the configuration for this component.
     *
     * @return the Configuration
     */
    public org.apache.commons.configuration.Configuration getConfiguration();

    /**
     * This method returns a Manager for the given name.
     *
     * @param name name of the manager
     * @return a Manager
     */
    public AbstractBaseManager getManager(String name);

    /**
     * This methods returns either the Manager from the configuration file,
     * or the default one provided by the generated code.
     *
     * @param name name of the manager
     * @param defaultClassName the class to use if name has not been configured
     * @return a Manager
     */
    public AbstractBaseManager getManager(String name, String defaultClassName);

    /**
     * Returns the default database map information.
     *
     * @return A DatabaseMap.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public DatabaseMap getDatabaseMap() throws TorqueException;

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
    public DatabaseMap getDatabaseMap(String name) throws TorqueException;

    /**
     * Register a MapBuilder
     *
     * @param className the MapBuilder
     */
    public void registerMapBuilder(String className);

    /**
     * This method returns a Connection from the default pool.
     *
     * @return The requested connection.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public Connection getConnection() throws TorqueException;

    /**
     *
     * @param name The database name.
     * @return a database connection
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public Connection getConnection(String name) throws TorqueException;

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
    public Connection getConnection(String name, String username, String password)
            throws TorqueException;

    /**
     * Returns database adapter for a specific connection pool.
     *
     * @param name A pool name.
     * @return The corresponding database adapter.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public DB getDB(String name) throws TorqueException;

    /**
     * Returns the name of the default database.
     *
     * @return name of the default DB
     */
    public String getDefaultDB();

    /**
     * Closes a connection.
     *
     * @param con A Connection to close.
     */
    public void closeConnection(Connection con);

    /**
     * Sets the current schema for a database connection
     *
     * @param name The database name.
     * @param schema The current schema name
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public void setSchema(String name, String schema) throws TorqueException;

    /**
     * This method returns the current schema for a database connection
     *
     * @param name The database name.
     * @return The current schema name. Null means, no schema has been set.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public String getSchema(String name) throws TorqueException;

    /**
     * Returns the database for the key <code>databaseName</code>.
     * 
     * @param databaseName the key to get the database for.
     * @return the database for the specified key, or null if the database
     *         does not exist.
     * @throws TorqueException if Torque is not yet initialized.
     */
    public Database getDatabase(String databaseName) throws TorqueException;
    
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
    public Map getDatabases() throws TorqueException;
    
    /**
     * Returns the database for the key <code>databaseName</code>.
     * If no database is associated to the specified key,
     * a new database is created, mapped to the specified key, and returned.
     *
     * @param databaseName the key to get the database for.
     * @return the database associated with specified key, or the newly created
     *         database, never null.
     */
    public Database getOrCreateDatabase(String databaseName);
}
