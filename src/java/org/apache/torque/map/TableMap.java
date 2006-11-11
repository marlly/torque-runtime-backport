package org.apache.torque.map;

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

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.lang.StringUtils;
import org.apache.torque.adapter.IDMethod;
import org.apache.torque.oid.IdGenerator;

/**
 * TableMap is used to model a table in a database.
 *
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @author <a href="mailto:greg.monroe@dukece.com">Greg Monroe</a>
 * @version $Id$
 */
public class TableMap implements IDMethod, java.io.Serializable
{
    /** The serialVersionUID for this class. */
    private static final long serialVersionUID = -9053174532511492818L;

    /** The list of valid ID generation methods. */
    protected static final String[] VALID_ID_METHODS =
    {
        NATIVE, AUTO_INCREMENT, SEQUENCE, ID_BROKER, NO_ID_METHOD
    };

    /** The columns in the table. XML Order is preserved. */
    private Map columns;

    /** The database this table belongs to. */
    private DatabaseMap dbMap;

    /** The name of the table. */
    private String tableName;

    /** The JavaName of the table as defined in XML */
    private String javaName;

    /** The prefix on the table name. */
    private String prefix;

    /** The primary key generation method. */
    private String primaryKeyMethod = NO_ID_METHOD;

    /** The table description info. */
    private String description = "";

    /** The Peer Class for this table. */
    private Class peerClass;

    /** The OM Root Class for this table. */
    private Class omClass;

    /** Whether any column uses Inheritance. */
    private boolean useInheritance = false;

    /** Whether cache managers are used. */
    private boolean useManager = false;

    /** The associated cache manager class. */
    private Class managerClass;

    /**
     * Object to store information that is needed if the
     * for generating primary keys.
     */
    private Object pkInfo = null;

    /**
     * Required by proxy. Not used.
     */
    public TableMap()
    {
    }

    /**
     * Constructor.
     *
     * @param tableName The name of the table.
     * @param numberOfColumns The number of columns in the table.
     * @param containingDB A DatabaseMap that this table belongs to.
     */
    public TableMap(String tableName,
                    int numberOfColumns,
                    DatabaseMap containingDB)
    {
        this.tableName = tableName;
        dbMap = containingDB;
        columns = Collections.synchronizedMap(new ListOrderedMap());
    }

    /**
     * Constructor.
     *
     * @param tableName The name of the table.
     * @param containingDB A DatabaseMap that this table belongs to.
     */
    public TableMap(String tableName, DatabaseMap containingDB)
    {
        this.tableName = tableName;
        dbMap = containingDB;
        columns = Collections.synchronizedMap(new ListOrderedMap());
    }

    /**
     * Constructor.
     *
     * @param tableName The name of the table.
     * @param prefix The prefix for the table name (ie: SCARAB for
     * SCARAB_PROJECT).
     * @param containingDB A DatabaseMap that this table belongs to.
     */
    public TableMap(String tableName,
                    String prefix,
                    DatabaseMap containingDB)
    {
        this.tableName = tableName;
        this.prefix = prefix;
        dbMap = containingDB;
        columns = Collections.synchronizedMap(new ListOrderedMap());
    }

    /**
     * Does this table contain the specified column?
     *
     * @param column A ColumnMap.
     * @return True if the table contains the column.
     */
    public boolean containsColumn(ColumnMap column)
    {
        return containsColumn(column.getColumnName());
    }

    /**
     * Does this table contain the specified column?
     *
     * @param name A String with the name of the column.
     * @return True if the table contains the column.
     */
    public boolean containsColumn(String name)
    {
        if (name.indexOf('.') > 0)
        {
            name = name.substring(name.indexOf('.') + 1);
        }
        return columns.containsKey(name);
    }

    /**
     * Get the DatabaseMap containing this TableMap.
     *
     * @return A DatabaseMap.
     */
    public DatabaseMap getDatabaseMap()
    {
        return dbMap;
    }

    /**
     * Returns true if this tableMap contains a column with object
     * data.  If the type of the column is not a string, a number or a
     * date, it is assumed that it is object data.
     *
     * @return True if map contains a column with object data.
     */
    public boolean containsObjectColumn()
    {
        synchronized (columns)
        {
            Iterator it = columns.values().iterator();
            while (it.hasNext())
            {
                Object theType = ((ColumnMap) it.next()).getType();
            if (!(theType instanceof String || theType instanceof Number
                    || theType instanceof java.util.Date))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the name of the Table.
     *
     * @return A String with the name of the table.
     */
    public String getName()
    {
        return tableName;
    }

    /**
     * Get the Java name of the table as defined in XML.
     *
     * @return A String with the Java name of the table.
     */
    public String getJavaName()
    {
        return javaName;
    }

    /**
     * Set the Java name of the table as defined by generator/XML.
     *
     * @param value A String with the Java name of the table.
     */
    public void setJavaName(String value)
    {
        this.javaName = value;
    }

    /**
     * Get table prefix name.
     *
     * @return A String with the prefix.
     */
    public String getPrefix()
    {
        return this.prefix;
    }

    /**
     * Set table prefix name.
     *
     * @param prefix The prefix for the table name (ie: SCARAB for
     * SCARAB_PROJECT).
     */
    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }

    /**
     * Get the method used to generate primary keys for this table.
     *
     * @return A String with the method.
     */
    public String getPrimaryKeyMethod()
    {
        return primaryKeyMethod;
    }

    /**
     * Get the value of idGenerator.
     * @return value of idGenerator.
     * @deprecated use DatabaseInfo.getIdGenerator(getPrimaryKeyMethod())
     *             instead. Will be removed in a future version of Torque.
     */
    public IdGenerator getIdGenerator()
    {
        return getDatabaseMap().getIdGenerator(primaryKeyMethod);
    }

    /**
     * Get the information used to generate a primary key
     *
     * @return An Object.
     */
    public Object getPrimaryKeyMethodInfo()
    {
        return pkInfo;
    }

    /**
     * Get a ColumnMap[] of the columns in this table.
     *
     * @return A ColumnMap[].
     */
    public ColumnMap[] getColumns()
    {
        ColumnMap[] tableColumns = new ColumnMap[columns.size()];
        synchronized (columns)
        {
            Iterator it = columns.values().iterator();
            int i = 0;
            while (it.hasNext())
            {
                tableColumns[i++] = (ColumnMap) it.next();
            }
        }
        return tableColumns;
    }

    /**
     * Get a ColumnMap for the named table.
     *
     * @param name A String with the name of the table.
     * @return A ColumnMap.
     */
    public ColumnMap getColumn(String name)
    {
        try
        {
            return (ColumnMap) columns.get(name);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Add a pre-created column to this table.  It will replace any
     * existing column.
     *
     * @param cmap A ColumnMap.
     */
    public void addColumn(ColumnMap cmap)
    {
        columns.put(cmap.getColumnName(), cmap);
    }

    /**
     * Add a column to this table of a certain type.
     *
     * @param columnName A String with the column name.
     * @param type An Object specifying the type.
     * @deprecated Associated Column maps should be populated using it's
     *             set methods, then added to table via addColumn(ColumnMap).
     *             This method will be removed in a future version of Torque.
     */
    public void addColumn(String columnName, Object type)
    {
        addColumn(columnName, type, false, null, null, 0);
    }

    /**
     * Add a column to this table of a certain type, size, and scale.
     *
     * @param columnName A String with the column name.
     * @param type An Object specifying the type.
     * @param size An int specifying the size.
     * @param scale An int specifying the scale.
     * @deprecated Associated Column maps should be populated using it's set
     *             methods, then added to table via addColumn(ColumnMap).
     *             This method will be removed in a future version of Torque.
     */
    public void addColumn(String columnName, Object type, int size, int scale)
    {
        addColumn(columnName, type, false, null, null, size, scale);
    }

    /**
     * Add a column to this table of a certain type and size.
     *
     * @param columnName A String with the column name.
     * @param type An Object specifying the type.
     * @param size An int specifying the size.
     * @deprecated Associated Column maps should be populated using it's set
     *             methods, then added to table via addColumn(ColumnMap).
     *             This method will be removed in a future version of Torque.
     */
    public void addColumn(String columnName, Object type, int size)
    {
        addColumn(columnName, type, false, null, null, size);
    }

    /**
     * Add a primary key column to this Table.
     *
     * @param columnName A String with the column name.
     * @param type An Object specifying the type.
     * @deprecated Associated Column maps should be populated using it's set
     *             methods, then added to table via addColumn(ColumnMap).
     *             This method will be removed in a future version of Torque.
     */
    public void addPrimaryKey(String columnName, Object type)
    {
        addColumn(columnName, type, true, null, null, 0);
    }

    /**
     * Add a primary key column to this Table.
     *
     * @param columnName A String with the column name.
     * @param type An Object specifying the type.
     * @param size An int specifying the size.
     * @deprecated Associated Column maps should be populated using it's set
     *             methods, then added to table via addColumn(ColumnMap).
     *             This method will be removed in a future version of Torque.
     */
    public void addPrimaryKey(String columnName, Object type, int size)
    {
        addColumn(columnName, type, true, null, null, size);
    }

    /**
     * Add a foreign key column to the table.
     *
     * @param columnName A String with the column name.
     * @param type An Object specifying the type.
     * @param fkTable A String with the foreign key table name.
     * @param fkColumn A String with the foreign key column name.
     * @deprecated Associated Column maps should be populated using it's set
     *             methods, then added to table via addColumn(ColumnMap).
     *             This method will be removed in a future version of Torque.
     */
    public void addForeignKey(String columnName,
                              Object type,
                              String fkTable,
                              String fkColumn)
    {
        addColumn(columnName, type, false, fkTable, fkColumn, 0);
    }

    /**
     * Add a foreign key column to the table.
     *
     * @param columnName A String with the column name.
     * @param type An Object specifying the type.
     * @param fkTable A String with the foreign key table name.
     * @param fkColumn A String with the foreign key column name.
     * @param size An int specifying the size.
     * @deprecated Associated Column maps should be populated using it's set
     *             methods, then added to table via addColumn(ColumnMap).
     *             This method will be removed in a future version of Torque.
     */
    public void addForeignKey(String columnName,
                              Object type,
                              String fkTable,
                               String fkColumn,
                               int size)
    {
        addColumn(columnName, type, false, fkTable, fkColumn, size);
    }

    /**
     * Add a foreign primary key column to the table.
     *
     * @param columnName A String with the column name.
     * @param type An Object specifying the type.
     * @param fkTable A String with the foreign key table name.
     * @param fkColumn A String with the foreign key column name.
     * @deprecated Associated Column maps should be populated using it's set
     *             methods, then added to table via addColumn(ColumnMap).
     *             This method will be removed in a future version of Torque.
     */
    public void addForeignPrimaryKey(String columnName,
                                     Object type,
                                     String fkTable,
                                     String fkColumn)
    {
        addColumn(columnName, type, true, fkTable, fkColumn, 0);
    }

    /**
     * Add a foreign primary key column to the table.
     *
     * @param columnName A String with the column name.
     * @param type An Object specifying the type.
     * @param fkTable A String with the foreign key table name.
     * @param fkColumn A String with the foreign key column name.
     * @param size An int specifying the size.
     * @deprecated Associated Column maps should be populated using it's set
     *             methods, then added to table via addColumn(ColumnMap).
     *             This method will be removed in a future version of Torque.
     */
    public void addForeignPrimaryKey(String columnName,
                                     Object type,
                                     String fkTable,
                                     String fkColumn,
                                     int size)
    {
        addColumn(columnName, type, true, fkTable, fkColumn, size);
    }

    /**
     * Add a column to the table.
     *
     * @param name A String with the column name.
     * @param type An Object specifying the type.
     * @param pk True if column is a primary key.
     * @param fkTable A String with the foreign key table name.
     * @param fkColumn A String with the foreign key column name.
     * @param size An int specifying the size.
     * @deprecated Associated Column maps should be populated using it's set
     *             methods, then added to table via addColumn(ColumnMap).
     *             This method will be removed in a future version of Torque.
     */
    private void addColumn(String name,
                           Object type,
                           boolean pk,
                           String fkTable,
                           String fkColumn,
                           int size)
    {
        addColumn(name, type, pk, fkTable, fkColumn, size, 0);
    }

    /**
     * Add a column to the table.
     *
     * @param name A String with the column name.
     * @param type An Object specifying the type.
     * @param pk True if column is a primary key.
     * @param fkTable A String with the foreign key table name.
     * @param fkColumn A String with the foreign key column name.
     * @param size An int specifying the size.
     * @param scale An int specifying the scale.
     * @deprecated Associated Column maps should be populated using it's set
     *             methods, then added to table via addColumn(ColumnMap).
     *             This method will be removed in a future version of Torque.
     */
    private void addColumn(String name,
                           Object type,
                           boolean pk,
                           String fkTable,
                           String fkColumn,
                           int size,
                           int scale)
    {
        // If the tablename is prefixed with the name of the column,
        // remove it ie: SCARAB_PROJECT.PROJECT_ID remove the
        // SCARAB_PROJECT.
        if (name.indexOf('.') > 0 && name.indexOf(getName()) != -1)
        {
            name = name.substring(getName().length() + 1);
        }
        if (fkTable != null && fkTable.length() > 0 && fkColumn != null
                && fkColumn.length() > 0)
        {
            if (fkColumn.indexOf('.') > 0 && fkColumn.indexOf(fkTable) != -1)
            {
                fkColumn = fkColumn.substring(fkTable.length() + 1);
            }
        }
        ColumnMap col = new ColumnMap(name, this);
        col.setType(type);
        col.setPrimaryKey(pk);
        col.setForeignKey(fkTable, fkColumn);
        col.setSize(size);
        col.setScale(scale);
        columns.put(name, col);
    }

    /**
     * Sets the method used to generate a key for this table.  Valid
     * values are as specified in the {@link
     * org.apache.torque.adapter.IDMethod} interface.
     *
     * @param method The ID generation method type name.
     */
    public void setPrimaryKeyMethod(String method)
    {
        primaryKeyMethod = NO_ID_METHOD;

        // Validate ID generation method.
        for (int i = 0; i < VALID_ID_METHODS.length; i++)
        {
            if (VALID_ID_METHODS[i].equalsIgnoreCase(method))
            {
                primaryKeyMethod = method;
                break;
            }
        }
        if (ID_BROKER.equalsIgnoreCase(method))
        {
            getDatabaseMap().startIdBroker();
        }
    }

    /**
     * Sets the pk information needed to generate a key
     *
     * @param pkInfo information needed to generate a key
     */
    public void setPrimaryKeyMethodInfo(Object pkInfo)
    {
        this.pkInfo = pkInfo;
    }

    //---Utility methods for doing intelligent lookup of table names

    /**
     * Tell me if i have PREFIX in my string.
     *
     * @param data A String.
     * @return True if prefix is contained in data.
     */
    private boolean hasPrefix(String data)
    {
        return (data.indexOf(getPrefix()) != -1);
    }

    /**
     * Removes the PREFIX.
     *
     * @param data A String.
     * @return A String with data, but with prefix removed.
     */
    private String removePrefix(String data)
    {
        return data.substring(getPrefix().length());
    }

    /**
     * Removes the PREFIX, removes the underscores and makes
     * first letter caps.
     *
     * SCARAB_FOO_BAR becomes FooBar.
     *
     * @param data A String.
     * @return A String with data processed.
     */
    public final String removeUnderScores(String data)
    {
        String tmp = null;
        StringBuffer out = new StringBuffer();
        if (hasPrefix(data))
        {
            tmp = removePrefix(data);
        }
        else
        {
            tmp = data;
        }

        StringTokenizer st = new StringTokenizer(tmp, "_");
        while (st.hasMoreTokens())
        {
            String element = ((String) st.nextElement()).toLowerCase();
            out.append(StringUtils.capitalize(element));
        }
        return out.toString();
    }

    /**
     * Returns the table description info.
     *
     * @return Returns the description.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Sets the table description.
     *
     * @param description The description to set.
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * Returns the OM class for this table.
     *
     * @return the OM class.
     */
    public Class getOMClass()
    {
        return omClass;
    }

    /**
     * Sets the OM root class for this table.
     *
     * @param omClass The OM root class for this table.
     */
    public void setOMClass(Class omClass)
    {
        this.omClass = omClass;
    }

    /**
     * Returns the Peer Class for this table.
     *
     * @return The peerClass for this table.
     */
    public Class getPeerClass()
    {
        return peerClass;
    }

    /**
     * Sets the Peer class for this table.
     *
     * @param peerClass The peerClass to set.
     */
    public void setPeerClass(Class peerClass)
    {
        this.peerClass = peerClass;
    }

    /**
     * Returns the database map for this table.
     *
     * @return the database map for this table.
     */
    public DatabaseMap getDbMap()
    {
        return dbMap;
    }

    /**
     * Returns whether this table uses inheritance.
     *
     * @return whether inheritance is used.
     */
    public boolean isUseInheritance()
    {
        return useInheritance;
    }

    /**
     * Sets whether this table uses inheritance.
     *
     * @param useInheritance whether this table uses inheritance.
     */
    public void setUseInheritance(boolean useInheritance)
    {
        this.useInheritance = useInheritance;
    }

    /**
     * Returns whether managers are used for this table.
     *
     * @return whether managers are used for this table.
     */
    public boolean isUseManager()
    {
        return useManager;
    }

    /**
     * Sets whether managers are used for this table.
     *
     * @param useManager whether managers are used for this table.
     */
    public void setUseManager(boolean useManager)
    {
        this.useManager = useManager;
    }

    /**
     * Returns the manager class for this table.
     *
     * @return the managerClass.
     */
    public Class getManagerClass()
    {
        return managerClass;
    }

    /**
     * Sets the manager class for this table.
     *
     * @param managerClass the manager class for this table.
     */
    public void setManagerClass(Class managerClass)
    {
        this.managerClass = managerClass;
    }
}
