package org.apache.torque.map;

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

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.lang.StringUtils;
import org.apache.torque.TorqueException;
import org.apache.torque.adapter.IDMethod;
import org.apache.torque.oid.IDBroker;
import org.apache.torque.oid.IdGenerator;

/**
 * DatabaseMap is used to model a database.
 *
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @author <a href="mailto:dlr@collab.net">Daniel Rall</a>
 * @author <a href="mailto:greg.monroe@dukece.com">Greg Monroe</a>
 * @version $Id$
 */
public class DatabaseMap implements java.io.Serializable
{
    /**
     * The character used by most implementations as the separator
     * between name elements.
     */
    char STD_SEPARATOR_CHAR = '_';

    /**
     * The character which separates the schema name from the table name
     */
    char SCHEMA_SEPARATOR_CHAR = '.';
    
    /** 
     * Format used to create create the class name for initializing a DB 
     * specific map 
     */
    public static final String INIT_CLASS_NAME_FORMAT = 
                                "org.apache.torque.linkage.{0}MapInit";
    
    public static String[] eMsgs = { 
        "Invalid Torque OM setup for Database \"{0}\".\n"+
            "Database Map initialization class, \"{1}\","+" " +
            "could not be found in your classpath.",
        "Invalid Torque OM setup for Database \"{0}\".\n"+
            "A class that the Database Map initialization class, \"{1}\", "+
            "depends on could not be found.",
        "Invalid Torque OM setup for Database \"{0}\".\n"+
            "Something unexpected happened doing Class.forName(\"{1}\").  "+
            "See the nested exception for details.",
        "Invalid Torque OM setup for Database \"{0}\".\n"+
            "An error occured invoking the init() method in class, \"{1}\""
    };
    
    /** The serialVersionUID for this class. */
    private static final long serialVersionUID = 955251837095032274L;

    /** Name of the database. */
    private String name;

    /** Name of the tables in the database. */
    private Map tables;

    /**
     * A special table used to generate primary keys for the other
     * tables.
     */
    private TableMap idTable = null;

    /** The IDBroker that goes with the idTable. */
    private IDBroker idBroker = null;

    /** The IdGenerators, keyed by type of idMethod. */
    private HashMap idGenerators;
    
    /** Flag indicating that all tables have been loaded via initialize() */
    boolean isInitialized = false;

    /**
     * Constructs a new DatabaseMap.
     */
    public DatabaseMap()
    {
        tables = Collections.synchronizedMap(new ListOrderedMap());
        idGenerators = new HashMap(6);
    }

    /**
     * Constructor.
     *
     * @param name Name of the database.
     * @param numberOfTables Number of tables in the database.
     * @deprecated use DatabaseMap() instead. Will be removed
     *             in a future version of Torque.
     */
    public DatabaseMap(String name, int numberOfTables)
    {
        this.name = name;
        tables = Collections.synchronizedMap(new ListOrderedMap());
        idGenerators = new HashMap(6);
    }

    /**
     * Constructor.
     *
     * @param name Name of the database.
     * @deprecated use DatabaseMap() instead. Will be removed
     *             in a future version of Torque.
     */
    public DatabaseMap(String name)
    {
        this.name = name;
        tables = Collections.synchronizedMap(new ListOrderedMap());
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
     * @deprecated Will be removed in a future version of Torque.
     *             Use DatabaseInfo#getIdBroker() instead 
     *             to access the IDBroker.
     */
    public IDBroker getIDBroker()
    {
        return idBroker;
    }

    /**
     * Get the name of this database.
     *
     * @return A String.
     * @deprecated Will be removed in a future version of Torque. 
     *             Use the name of the corresponding database instead.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Get a TableMap for the table by name. <p>
     *
     * Note that by default Torque uses lazy initialization to minimize
     * memory usage and startup time.  However, if an OM or PEER class 
     * has not called the table's MapBuilder class, it will not be here. 
     * See the optional initialize method if you need full OM Mapping.<p>
     *
     * @param name Name of the table.
     * @return A TableMap, null if the table was not found.
     */
    public TableMap getTable(String name)
    {
        return (TableMap) tables.get(name);
    }

    /**
     * Get a TableMap[] of all of the tables in the database.<P>
     * 
     * Note that by default Torque uses lazy initialization to minimize
     * memory usage and startup time.  However, if an OM or PEER class 
     * has not called the table's MapBuilder class, it will not be here. 
     * See the optional initialize method if you need full OM Mapping.<p>
     *
     * @return A TableMap[].
     */
    public TableMap[] getTables()
    {
        TableMap[] dbTables = new TableMap[tables.size()];
        synchronized (tables)
        {
            Iterator it = tables.values().iterator();
            int i = 0;
            while (it.hasNext())
            {
                dbTables[i++] = (TableMap) it.next() ;
            }
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
     * @deprecated use DatabaseInfo.addGenerator() instead.
     *             Will be removed in a future version of Torque.
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
     * @deprecated use DatabaseInfo.getIdGenerator() instead.
     *             Will be removed in a future version of Torque.
     */
    public IdGenerator getIdGenerator(String type)
    {
        return (IdGenerator) idGenerators.get(type);
    }
    
    /**
     * Creates the Idbroker for this DatabaseMap.
     * If an IDBroker already exists for the DatabaseMap, the method 
     * does nothing.
     * @return true if a new IdBroker was created, false otherwise.
     * @deprecated Will be removed in a future version of Torque. 
     *             Use DatabaseInfo.startIdBroker() instead.
     */
    public synchronized boolean startIdBroker()
    {
        if (idBroker == null)
        {
            setIdTable("ID_TABLE");
            TableMap tMap = getIdTable();
            tMap.addPrimaryKey("ID_TABLE_ID", new Integer(0));
            tMap.addColumn("TABLE_NAME", "");
            tMap.addColumn("NEXT_ID", new Integer(0));
            tMap.addColumn("QUANTITY", new Integer(0));
            idBroker = new IDBroker(idTable);
            addIdGenerator(IDMethod.ID_BROKER, idBroker);
            return true;
        }
        return false;
    }
    
    /**
     * Fully populate this DatabaseMap with all the TablesMaps.  This
     * is only needed if the application needs to use the complete OM
     * mapping information.  Otherwise, the OM Mapping information
     * will be populated as needed by OM and Peer classes.  An example
     * of how to initialize the map info from the application:<p>
     * 
     *   <code>
     *   DatabaseMap dbMap = Torque.getDatabaseMap( dbName );
     *   try {
     *      dbMap.initialize();
     *   } catch ( TorqueException e ) {
     *      ... error handling
     *   }
     *   </code>
     * 
     * Note that Torque database names are case sensitive and this DB 
     * map must be retrieved with the exact name used in the XML schema.<p>
     * 
     * This uses Java reflection methods to locate and run the 
     * init() method of a class generated in the org.apache.torque.linkage
     * package with a name based on the XML Database name value, e.g.
     * org.apache.torque.linkage.DefaultMapInit<p>  
     * 
     * Some misconfiguration situations that could cause this method to fail
     * are:<p>
     * 
     * It was used with a Torque OM set of classes generated by V3.2 or older;
     * <br>
     * The class(es) in the org.apache.torque.linkage package were not included 
     * with the other generated class files (e.g. the jar file creation process
     * only included com.* and not org.* files).<p>
     * 
     * @throws TorqueException If an error is encountered locating and calling 
     *                          the init method.
     */
    public synchronized void initialize() throws TorqueException
    {
        if (isInitialized) 
        {
            return;
        }        
        String initClassName = MessageFormat.format(INIT_CLASS_NAME_FORMAT, 
                new Object[] { javanameMethod(getName()) });
        
        Class initClass = null;
        try 
        {
            initClass = Class.forName(initClassName);
        }
        catch (ClassNotFoundException e)
        {
            throw new TorqueException(MessageFormat.format(eMsgs[0], 
                    new Object[] { getName(), initClassName }), e);
        }
        catch (LinkageError e)
        {
            throw new TorqueException(MessageFormat.format(eMsgs[1], 
                    new Object[] { getName(), initClassName }), e);
        }
        catch (Throwable e)
        {
            throw new TorqueException(MessageFormat.format(eMsgs[2], 
                    new Object[] { getName(), initClassName }), e);
        }
        try
        {
            Method initMethod = initClass.getMethod("init", (Class []) null);
            initMethod.invoke(null, (Object []) null);
        }
        catch (Exception e)
        {
            throw new TorqueException(MessageFormat.format(eMsgs[3], 
                    new Object[] { getName(), initClassName }), e);
        }
        isInitialized = true;
    }
    
    /**
     * Converts a database schema name to java object name.  Operates
     * same as underscoreMethod but does not convert anything to
     * lowercase.  This must match the javaNameMethod in the 
     * JavaNameGenerator class in Generator code. 
     *
     * @param schemaName name to be converted.
     * @return converted name.
     * 
     * @see org.apache.torque.engine.database.model.NameGenerator
     */
    protected String javanameMethod(String schemaName)
    {
        StringBuffer name = new StringBuffer();
        StringTokenizer tok = new StringTokenizer
            (schemaName, String.valueOf(STD_SEPARATOR_CHAR));
        while (tok.hasMoreTokens())
        {
            String namePart = (String) tok.nextElement();
            name.append(StringUtils.capitalize(namePart));
        }

        // remove the SCHEMA_SEPARATOR_CHARs and capitalize
        // the tokens
        schemaName = name.toString();
        name = new StringBuffer();

        tok = new StringTokenizer
            (schemaName, String.valueOf(SCHEMA_SEPARATOR_CHAR));
        while (tok.hasMoreTokens())
        {
            String namePart = (String) tok.nextElement();
            name.append(StringUtils.capitalize(namePart));
        }
        return name.toString();
    }
}