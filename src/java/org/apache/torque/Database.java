package org.apache.torque;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import org.apache.torque.adapter.DB;
import org.apache.torque.dsfactory.DataSourceFactory;
import org.apache.torque.map.DatabaseMap;
import org.apache.torque.oid.IDBroker;
import org.apache.torque.oid.IdGenerator;

/**
 * Bundles all information about a database. This includes the database adapter,
 * the database Map and the Data Source Factory.
 */
public class Database
{
    /** 
     * The name of the database. Must be the same as the key in Torque's 
     * databaseMap.
     */
    private String name;
    
    /**
     * The Database adapter which encapsulates database-specific peculiarities. 
     */
    private DB adapter;
    
    /**
     * the Map of this database.
     */
    private DatabaseMap databaseMap;
    
    /**
     * The DataSourceFactory to optain connections to this database. 
     */
    private DataSourceFactory dataSourceFactory;
    
    /**
     * Creates a new Database with the given name.
     *
     * @param name the name of the database, not null.
     */
    Database(String name)
    {
        this.name = name;
    }
    
    /**
     * returns the name of the database.
     *
     * @return the name of the database. May be null.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns the adapther to this database.
     *
     * @return the adapter to this database, or null if no adapter is set.
     */
    public DB getAdapter()
    {
        return adapter;
    }

    /**
     * Sets the adapter for this database.
     *
     * @param adapter The adapter for this database, or null to remove the 
     *        current adapter from this database.
     */
    public void setAdapter(DB adapter)
    {
        this.adapter = adapter;
    }

    /**
     * Returns the database map for this database.
     * If the database map does not exist yet, it is created by this method.
     * 
     * @param adapter The database map for this database, never null.
     */
    public synchronized DatabaseMap getDatabaseMap()
    {
        if (databaseMap == null)
        {
            databaseMap = new DatabaseMap(name);
        }
        return databaseMap;
    }

    /**
     * Returns the DataSourceFactory for this database. 
     * The DataSourceFactory is responsible to create connections 
     * to this database.
     * 
     * @return the DataSourceFactory for this database, or null if no 
     *         DataSourceFactory exists for this database.
     */
    public DataSourceFactory getDataSourceFactory()
    {
        return dataSourceFactory;
    }

    /**
     * Sets the DataSourceFactory for this database.
     * The DataSourceFactory is responsible to create connections
     * to this database.
     * 
     * @param dataSourceFactory The new DataSorceFactory for this database,
     *        or null to remove the current DataSourceFactory.
     */
    public void setDataSourceFactory(DataSourceFactory dataSourceFactory)
    {
        this.dataSourceFactory = dataSourceFactory;
    }
    
    /**
     * Get the IDBroker for this database.
     *
     * @return The IDBroker for this database, or null if no IdBroker has 
     *         been started for this database.
     */
    public IDBroker getIDBroker()
    {
        if (databaseMap == null)
        {
            return null;
        }
        return databaseMap.getIDBroker();
    }
    
    /**
     * Creates the IDBroker for this DatabaseMap and starts it for the 
     * given database.
     * The information about the IdTable is stored in the databaseMap.
     * If an IDBroker already exists for the DatabaseMap, the method 
     * does nothing.
     *
     * @return true if a new IDBroker was created, false otherwise. 
     */
    public synchronized boolean startIDBroker()
    {
        DatabaseMap databaseMap = getDatabaseMap();
        if (databaseMap.getIDBroker() != null)
        {
            return false;
        }
        return databaseMap.startIdBroker();
    }

    /**
     * Returns the IdGenerator of the given type for this Database.
     * @param type The type (i.e.name) of the IdGenerator
     * @return The IdGenerator of the requested type, or null if no IdGenerator
     *         exists for the requested type.
     */
    public IdGenerator getIdGenerator(String type)
    {
        if (databaseMap == null)
        {
            return null;
        }
        return databaseMap.getIdGenerator(type);
    }

    /**
     * Adds an IdGenerator to the database.
     * @param type The type of the IdGenerator
     * @param idGen The new IdGenerator for the type, or null
     *        to remove the IdGenerator of the given type.
     */
    public void addIdGenerator(String type, IdGenerator idGen)
    {
        getDatabaseMap().addIdGenerator(type, idGen);
    }

    /**
     * Returns the database schema for this Database.
     * @return the database schema for this database, or null if no schema
     *         has been set.
     */
    public String getSchema()
    {
        DataSourceFactory dsf = getDataSourceFactory();
        if (dsf == null)
        {
            return null;
        }
        return dsf.getSchema();
    }
    
    /**
     * Sets the schema for this database. 
     * @param schema the name of the database schema to set, or null to remove
     *        the current schema.
     * @throws NullPointerException if no DatasourceFactory exists for this 
     *         database.
     */
    public void setSchema(String schema)
    {
        getDataSourceFactory().setSchema(schema);
    }
}