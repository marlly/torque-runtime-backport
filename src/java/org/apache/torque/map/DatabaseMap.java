package org.apache.torque.map;

/*
 * Copyright 2001,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Iterator;
import java.util.HashMap;
import java.util.Hashtable;
import org.apache.torque.adapter.IDMethod;
import org.apache.torque.oid.IDBroker;
import org.apache.torque.oid.IdGenerator;

/**
 * DatabaseMap is used to model a database.
 *
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @author <a href="mailto:dlr@collab.net">Daniel Rall</a>
 * @version $Id$
 */
public class DatabaseMap implements java.io.Serializable
{
    /** Name of the database. */
    private String name;

    /** Name of the tables in the database. */
    private Hashtable tables;

    /**
     * A special table used to generate primary keys for the other
     * tables.
     */
    private TableMap idTable = null;

    /** The IDBroker that goes with the idTable. */
    private IDBroker idBroker = null;

    /** The IdGenerators, keyed by type of idMethod. */
    private HashMap idGenerators;

    /**
     * Required by proxy. Not used.
     */
    public DatabaseMap()
    {
    }

    /**
     * Constructor.
     *
     * @param name Name of the database.
     * @param numberOfTables Number of tables in the database.
     */
    public DatabaseMap(String name, int numberOfTables)
    {
        this.name = name;
        tables = new Hashtable((int) (1.25 * numberOfTables) + 1);
        idGenerators = new HashMap(6);
    }

    /**
     * Constructor.
     *
     * @param name Name of the database.
     */
    public DatabaseMap(String name)
    {
        this.name = name;
        tables = new Hashtable();
        idGenerators = new HashMap(6);
    }

    /**
     * Does this database contain this specific table?
     *
     * @param table The TableMap representation of the table.
     * @return True if the database contains the table.
     */
    public boolean containsTable(TableMap table)
    {
        return containsTable(table.getName());
    }

    /**
     * Does this database contain this specific table?
     *
     * @param name The String representation of the table.
     * @return True if the database contains the table.
     */
    public boolean containsTable(String name)
    {
        if (name.indexOf('.') > 0)
        {
            name = name.substring(0, name.indexOf('.'));
        }
        return tables.containsKey(name);
    }

    /**
     * Get the ID table for this database.
     *
     * @return A TableMap.
     */
    public TableMap getIdTable()
    {
        return idTable;
    }

    /**
     * Get the IDBroker for this database.
     *
     * @return An IDBroker.
     */
    public IDBroker getIDBroker()
    {
        return idBroker;
    }

    /**
     * Get the name of this database.
     *
     * @return A String.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Get a TableMap for the table by name.
     *
     * @param name Name of the table.
     * @return A TableMap, null if the table was not found.
     */
    public TableMap getTable(String name)
    {
        return (TableMap) tables.get(name);
    }

    /**
     * Get a TableMap[] of all of the tables in the database.
     *
     * @return A TableMap[].
     */
    public TableMap[] getTables()
    {
        TableMap[] dbTables = new TableMap[tables.size()];
        Iterator it = tables.values().iterator();
        int i = 0;
        while (it.hasNext())
        {
            dbTables[i++] = (TableMap) it.next() ;
        }
        return dbTables;
    }

    /**
     * Add a new table to the database by name.  It creates an empty
     * TableMap that you need to populate.
     *
     * @param tableName The name of the table.
     */
    public void addTable(String tableName)
    {
        TableMap tmap = new TableMap(tableName, this);
        tables.put(tableName, tmap);
    }

    /**
     * Add a new table to the database by name.  It creates an empty
     * TableMap that you need to populate.
     *
     * @param tableName The name of the table.
     * @param numberOfColumns The number of columns in the table.
     */
    public void addTable(String tableName, int numberOfColumns)
    {
        TableMap tmap = new TableMap(tableName, numberOfColumns, this);
        tables.put(tableName, tmap);
    }

    /**
     * Add a new TableMap to the database.
     *
     * @param map The TableMap representation.
     */
    public void addTable(TableMap map)
    {
        tables.put(map.getName(), map);
    }

    /**
     * Set the ID table for this database.
     *
     * @param idTable The TableMap representation for the ID table.
     */
    public void setIdTable(TableMap idTable)
    {
        this.idTable = idTable;
        addTable(idTable);
        idBroker = new IDBroker(idTable);
        addIdGenerator(IDMethod.ID_BROKER, idBroker);
    }

    /**
     * Set the ID table for this database.
     *
     * @param tableName The name for the ID table.
     */
    public void setIdTable(String tableName)
    {
        TableMap tmap = new TableMap(tableName, this);
        setIdTable(tmap);
    }

    /**
     * Add a type of id generator for access by a TableMap.
     *
     * @param type a <code>String</code> value
     * @param idGen an <code>IdGenerator</code> value
     */
    public void addIdGenerator(String type, IdGenerator idGen)
    {
        idGenerators.put(type, idGen);
    }

    /**
     * Get a type of id generator.  Valid values are listed in the
     * {@link org.apache.torque.adapter.IDMethod} interface.
     *
     * @param type a <code>String</code> value
     * @return an <code>IdGenerator</code> value
     */
    IdGenerator getIdGenerator(String type)
    {
        return (IdGenerator) idGenerators.get(type);
    }
}
