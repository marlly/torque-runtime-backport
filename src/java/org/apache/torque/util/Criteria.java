package org.apache.torque.util;

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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.OrderedMap;
import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.torque.Torque;
import org.apache.torque.TorqueException;
import org.apache.torque.adapter.DB;
import org.apache.torque.om.DateKey;
import org.apache.torque.om.ObjectKey;

/**
 * This is a utility class that is used for retrieving different types
 * of values from a hashtable based on a simple name string.  This
 * class is meant to minimize the amount of casting that needs to be
 * done when working with Hashtables.
 *
 * NOTE: other methods will be added as needed and as time permits.
 *
 * @author <a href="mailto:frank.kim@clearink.com">Frank Y. Kim</a>
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @author <a href="mailto:bmclaugh@algx.net">Brett McLaughlin</a>
 * @author <a href="mailto:eric@dobbse.net">Eric Dobbs</a>
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @author <a href="mailto:sam@neurogrid.com">Sam Joseph</a>
 * @author <a href="mailto:mpoeschl@marmot.at">Martin Poeschl</a>
 * @author <a href="mailto:fischer@seitenbau.de">Thomas Fischer</a>
 * @author <a href="mailto:seade@backstagetech.com.au">Scott Eade</a>
 * @author <a href="mailto:tv@apache.org">Thomas Vandahl</a>
 * @version $Id$
 */
public class Criteria extends Hashtable
{
    /** Serial version. */
    private static final long serialVersionUID = -9001666575933085601L;

    /** Comparison type. */
    public static final SqlEnum EQUAL = SqlEnum.EQUAL;

    /** Comparison type. */
    public static final SqlEnum NOT_EQUAL = SqlEnum.NOT_EQUAL;

    /** Comparison type. */
    public static final SqlEnum ALT_NOT_EQUAL = SqlEnum.ALT_NOT_EQUAL;

    /** Comparison type. */
    public static final SqlEnum GREATER_THAN = SqlEnum.GREATER_THAN;

    /** Comparison type. */
    public static final SqlEnum LESS_THAN = SqlEnum.LESS_THAN;

    /** Comparison type. */
    public static final SqlEnum GREATER_EQUAL = SqlEnum.GREATER_EQUAL;

    /** Comparison type. */
    public static final SqlEnum LESS_EQUAL = SqlEnum.LESS_EQUAL;

    /** Comparison type. */
    public static final SqlEnum LIKE = SqlEnum.LIKE;

    /** Comparison type. */
    public static final SqlEnum NOT_LIKE = SqlEnum.NOT_LIKE;

    /** Comparison type. */
    public static final SqlEnum ILIKE = SqlEnum.ILIKE;

    /** Comparison type. */
    public static final SqlEnum NOT_ILIKE = SqlEnum.NOT_ILIKE;

    /** Comparison type. */
    public static final SqlEnum CUSTOM = SqlEnum.CUSTOM;

    /** Comparison type. */
    public static final SqlEnum DISTINCT = SqlEnum.DISTINCT;

    /** Comparison type. */
    public static final SqlEnum IN = SqlEnum.IN;

    /** Comparison type. */
    public static final SqlEnum NOT_IN = SqlEnum.NOT_IN;

    /** Comparison type. */
    public static final SqlEnum ALL = SqlEnum.ALL;

    /** Comparison type. */
    public static final SqlEnum JOIN = SqlEnum.JOIN;

    /** &quot;Order by&quot; qualifier - ascending */
    private static final SqlEnum ASC = SqlEnum.ASC;

    /** &quot;Order by&quot; qualifier - descending */
    private static final SqlEnum DESC = SqlEnum.DESC;

    /** &quot;IS NULL&quot; null comparison */
    public static final SqlEnum ISNULL = SqlEnum.ISNULL;

    /** &quot;IS NOT NULL&quot; null comparison */
    public static final SqlEnum ISNOTNULL = SqlEnum.ISNOTNULL;

    /** &quot;CURRENT_DATE&quot; ANSI SQL function */
    public static final SqlEnum CURRENT_DATE = SqlEnum.CURRENT_DATE;

    /** &quot;CURRENT_TIME&quot; ANSI SQL function */
    public static final SqlEnum CURRENT_TIME = SqlEnum.CURRENT_TIME;

    /** &quot;LEFT JOIN&quot; SQL statement */
    public static final SqlEnum LEFT_JOIN = SqlEnum.LEFT_JOIN;

    /** &quot;RIGHT JOIN&quot; SQL statement */
    public static final SqlEnum RIGHT_JOIN = SqlEnum.RIGHT_JOIN;

    /** &quot;INNER JOIN&quot; SQL statement */
    public static final SqlEnum INNER_JOIN = SqlEnum.INNER_JOIN;

    private static final int DEFAULT_CAPACITY = 10;

    private boolean ignoreCase = false;
    private boolean singleRecord = false;
    private boolean cascade = false;
    private UniqueList selectModifiers = new UniqueList();
    private UniqueList selectColumns = new UniqueList();
    private UniqueList orderByColumns = new UniqueList();
    private UniqueList groupByColumns = new UniqueList();
    private Criterion having = null;
    private OrderedMap asColumns = ListOrderedMap.decorate(new HashMap());
    private transient List joins = new ArrayList(3);

    /** The name of the database. */
    private String dbName;

    /** The name of the database as given in the contructor. */
    private String originalDbName;

    /**
     * To limit the number of rows to return.  <code>-1</code> means return all
     * rows.
     */
    private int limit = -1;

    /** To start the results at a row other than the first one. */
    private int offset = 0;

    private HashMap aliases = new HashMap(8);

    private boolean useTransaction = false;

    /** the log. */
    private static Log log = LogFactory.getLog(Criteria.class);

    /**
     * Creates a new instance with the default capacity.
     */
    public Criteria()
    {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Creates a new instance with the specified capacity.
     *
     * @param initialCapacity An int.
     */
    public Criteria(int initialCapacity)
    {
        this(Torque.getDefaultDB(), initialCapacity);
    }

    /**
     * Creates a new instance with the default capacity which corresponds to
     * the specified database.
     *
     * @param dbName The dabase name.
     */
    public Criteria(String dbName)
    {
        this(dbName, DEFAULT_CAPACITY);
    }

    /**
     * Creates a new instance with the specified capacity which corresponds to
     * the specified database.
     *
     * @param dbName          The dabase name.
     * @param initialCapacity The initial capacity.
     */
    public Criteria(String dbName, int initialCapacity)
    {
        super(initialCapacity);
        this.dbName = dbName;
        this.originalDbName = dbName;
    }

    /**
     * Brings this criteria back to its initial state, so that it
     * can be reused as if it was new. Except if the criteria has grown in
     * capacity, it is left at the current capacity.
     */
    public void clear()
    {
        super.clear();
        ignoreCase = false;
        singleRecord = false;
        cascade = false;
        selectModifiers.clear();
        selectColumns.clear();
        orderByColumns.clear();
        groupByColumns.clear();
        having = null;
        asColumns.clear();
        joins.clear();
        dbName = originalDbName;
        offset = 0;
        limit = -1;
        aliases.clear();
        useTransaction = false;
    }

    /**
     * Add an AS clause to the select columns. Usage:
     * <p>
     * <code>
     *
     * Criteria myCrit = new Criteria();
     * myCrit.addAsColumn(&quot;alias&quot;, &quot;ALIAS(&quot;+MyPeer.ID+&quot;)&quot;);
     *
     * </code>
     *
     * @param name  wanted Name of the column
     * @param clause SQL clause to select from the table
     *
     * If the name already exists, it is replaced by the new clause.
     *
     * @return A modified Criteria object.
     */
    public Criteria addAsColumn(String name, String clause)
    {
        asColumns.put(name, clause);
        return this;
    }

    /**
     * Get the column aliases.
     *
     * @return A Map which map the column alias names
     * to the alias clauses.
     */
    public Map getAsColumns()
    {
        return asColumns;
    }

    /**
     * Get the table aliases.
     *
     * @return A Map which maps the table alias names to the actual table names.
     */
    public Map getAliases()
    {
        return aliases;
    }

    /**
     * Allows one to specify an alias for a table that can
     * be used in various parts of the SQL.
     *
     * @param alias a <code>String</code> value
     * @param table a <code>String</code> value
     */
    public void addAlias(String alias, String table)
    {
        aliases.put(alias, table);
    }

    /**
     * Returns the table name associated with an alias.
     *
     * @param alias a <code>String</code> value
     * @return a <code>String</code> value
     */
    public String getTableForAlias(String alias)
    {
        return (String) aliases.get(alias);
    }

    /**
     * Does this Criteria Object contain the specified key?
     *
     * @param table The name of the table.
     * @param column The name of the column.
     * @return True if this Criteria Object contain the specified key.
     */
    public boolean containsKey(String table, String column)
    {
        return containsKey(table + '.' + column);
    }

    /**
     * Convenience method to return value as a boolean.
     *
     * @param column String name of column.
     * @return A boolean.
     */
    public boolean getBoolean(String column)
    {
        return ((Boolean) getCriterion(column).getValue()).booleanValue();
    }

    /**
     * Convenience method to return value as a boolean.
     *
     * @param table String name of table.
     * @param column String name of column.
     * @return A boolean.
     */
    public boolean getBoolean(String table, String column)
    {
        return getBoolean(new StringBuffer(table.length() + column.length() + 1)
                .append(table).append('.').append(column)
                .toString());
    }

    /**
     * Will force the sql represented by this criteria to be executed within
     * a transaction.  This is here primarily to support the oid type in
     * postgresql.  Though it can be used to require any single sql statement
     * to use a transaction.
     */
    public void setUseTransaction(boolean v)
    {
        useTransaction = v;
    }

    /**
     * called by BasePeer to determine whether the sql command specified by
     * this criteria must be wrapped in a transaction.
     *
     * @return a <code>boolean</code> value
     */
    protected boolean isUseTransaction()
    {
        return useTransaction;
    }

    /**
     * Method to return criteria related to columns in a table.
     *
     * @param column String name of column.
     * @return A Criterion.
     */
    public Criterion getCriterion(String column)
    {
        return (Criterion) super.get(column);
    }

    /**
     * Method to return criteria related to a column in a table.
     *
     * @param table String name of table.
     * @param column String name of column.
     * @return A Criterion.
     */
    public Criterion getCriterion(String table, String column)
    {
        return getCriterion(
                new StringBuffer(table.length() + column.length() + 1)
                .append(table).append('.').append(column)
                .toString());
    }

    /**
     * Method to return criterion that is not added automatically
     * to this Criteria.  This can be used to chain the
     * Criterions to form a more complex where clause.
     *
     * @param column String full name of column (for example TABLE.COLUMN).
     * @return A Criterion.
     */
    public Criterion getNewCriterion(String column, Object value,
            SqlEnum comparison)
    {
        return new Criterion(column, value, comparison);
    }

    /**
     * Method to return criterion that is not added automatically
     * to this Criteria.  This can be used to chain the
     * Criterions to form a more complex where clause.
     *
     * @param table String name of table.
     * @param column String name of column.
     * @return A Criterion.
     */
    public Criterion getNewCriterion(String table, String column,
            Object value, SqlEnum comparison)
    {
        return new Criterion(table, column, value, comparison);
    }

    /**
     * This method adds a prepared Criterion object to the Criteria.
     * You can get a new, empty Criterion object with the
     * getNewCriterion() method. If a criterion for the requested column
     * already exists, it is replaced. This is used as follows:
     *
     * <p>
     * <code>
     * Criteria crit = new Criteria();
     * Criteria.Criterion c = crit
     * .getNewCriterion(BasePeer.ID, new Integer(5), Criteria.LESS_THAN);
     * crit.add(c);
     * </code>
     *
     * @param c A Criterion object
     *
     * @return A modified Criteria object.
     */
    public Criteria add(Criterion c)
    {
        StringBuffer sb = new StringBuffer(c.getTable().length()
                + c.getColumn().length() + 1);
        sb.append(c.getTable());
        sb.append('.');
        sb.append(c.getColumn());
        super.put(sb.toString(), c);
        return this;
    }

    /**
     * Method to return a String table name.
     *
     * @param name A String with the name of the key.
     * @return A String with the value of the object at key.
     */
    public String getColumnName(String name)
    {
        return getCriterion(name).getColumn();
    }

    /**
     * Method to return a comparison String.
     *
     * @param key String name of the key.
     * @return A String with the value of the object at key.
     */
    public SqlEnum getComparison(String key)
    {
        return getCriterion(key).getComparison();
    }

    /**
     * Method to return a comparison String.
     *
     * @param table String name of table.
     * @param column String name of column.
     * @return A String with the value of the object at key.
     */
    public SqlEnum getComparison(String table, String column)
    {
        return getComparison(
                new StringBuffer(table.length() + column.length() + 1)
                .append(table).append('.').append(column)
                .toString());
    }

    /**
     * Convenience method to return a Date.
     *
     * @param name column name (TABLE.COLUMN)
     * @return A java.util.Date with the value of object at key.
     */
    public java.util.Date getDate(String name)
    {
        return (java.util.Date) getCriterion(name).getValue();
    }

    /**
     * Convenience method to return a Date.
     *
     * @param table String name of table.
     * @param column String name of column.
     * @return A java.util.Date with the value of object at key.
     */
    public java.util.Date getDate(String table, String column)
    {
        return getDate(
                new StringBuffer(table.length() + column.length() + 1)
                .append(table).append('.').append(column)
                .toString());
    }

    /**
     * Get the Database(Map) name.
     *
     * @return A String with the Database(Map) name.  By default, this
     * is PoolBrokerService.DEFAULT.
     */
    public String getDbName()
    {
        return dbName;
    }

    /**
     * Set the DatabaseMap name.  If <code>null</code> is supplied, uses value
     * provided by <code>Torque.getDefaultDB()</code>.
     *
     * @param dbName A String with the Database(Map) name.
     */
    public void setDbName(String dbName)
    {
        this.dbName = (dbName == null ? Torque.getDefaultDB() : dbName.trim());
    }

    /**
     * Convenience method to return a double.
     *
     * @param name A String with the name of the key.
     * @return A double with the value of object at key.
     */
    public double getDouble(String name)
    {
        Object obj = getCriterion(name).getValue();
        if (obj instanceof String)
        {
            return new Double((String) obj).doubleValue();
        }
        return ((Double) obj).doubleValue();
    }

    /**
     * Convenience method to return a double.
     *
     * @param table String name of table.
     * @param column String name of column.
     * @return A double with the value of object at key.
     */
    public double getDouble(String table, String column)
    {
        return getDouble(new StringBuffer(table.length() + column.length() + 1)
                .append(table).append('.').append(column)
                .toString());
    }

    /**
     * Convenience method to return a float.
     *
     * @param name A String with the name of the key.
     * @return A float with the value of object at key.
     */
    public float getFloat(String name)
    {
        Object obj = getCriterion(name).getValue();
        if (obj instanceof String)
        {
            return new Float((String) obj).floatValue();
        }
        return ((Float) obj).floatValue();
    }

    /**
     * Convenience method to return a float.
     *
     * @param table String name of table.
     * @param column String name of column.
     * @return A float with the value of object at key.
     */
    public float getFloat(String table, String column)
    {
        return getFloat(new StringBuffer(table.length() + column.length() + 1)
                .append(table).append('.').append(column)
                .toString());
    }

    /**
     * Convenience method to return an Integer.
     *
     * @param name A String with the name of the key.
     * @return An Integer with the value of object at key.
     */
    public Integer getInteger(String name)
    {
        Object obj = getCriterion(name).getValue();
        if (obj instanceof String)
        {
            return new Integer((String) obj);
        }
        return ((Integer) obj);
    }

    /**
     * Convenience method to return an Integer.
     *
     * @param table String name of table.
     * @param column String name of column.
     * @return An Integer with the value of object at key.
     */
    public Integer getInteger(String table, String column)
    {
        return getInteger(
                new StringBuffer(table.length() + column.length() + 1)
                .append(table).append('.').append(column)
                .toString());
    }

    /**
     * Convenience method to return an int.
     *
     * @param name A String with the name of the key.
     * @return An int with the value of object at key.
     */
    public int getInt(String name)
    {
        Object obj = getCriterion(name).getValue();
        if (obj instanceof String)
        {
            return new Integer((String) obj).intValue();
        }
        return ((Integer) obj).intValue();
    }

    /**
     * Convenience method to return an int.
     *
     * @param table String name of table.
     * @param column String name of column.
     * @return An int with the value of object at key.
     */
    public int getInt(String table, String column)
    {
        return getInt(
                new StringBuffer(table.length() + column.length() + 1)
                .append(table).append('.').append(column)
                .toString());
    }

    /**
     * Convenience method to return a BigDecimal.
     *
     * @param name A String with the name of the key.
     * @return A BigDecimal with the value of object at key.
     */
    public BigDecimal getBigDecimal(String name)
    {
        Object obj = getCriterion(name).getValue();
        if (obj instanceof String)
        {
            return new BigDecimal((String) obj);
        }
        return (BigDecimal) obj;
    }

    /**
     * Convenience method to return a BigDecimal.
     *
     * @param table String name of table.
     * @param column String name of column.
     * @return A BigDecimal with the value of object at key.
     */
    public BigDecimal getBigDecimal(String table, String column)
    {
        return getBigDecimal(
                new StringBuffer(table.length() + column.length() + 1)
                .append(table).append('.').append(column)
                .toString());
    }

    /**
     * Convenience method to return a long.
     *
     * @param name A String with the name of the key.
     * @return A long with the value of object at key.
     */
    public long getLong(String name)
    {
        Object obj = getCriterion(name).getValue();
        if (obj instanceof String)
        {
            return new Long((String) obj).longValue();
        }
        return ((Long) obj).longValue();
    }

    /**
     * Convenience method to return a long.
     *
     * @param table String name of table.
     * @param column String name of column.
     * @return A long with the value of object at key.
     */
    public long getLong(String table, String column)
    {
        return getLong(
                new StringBuffer(table.length() + column.length() + 1)
                .append(table).append('.').append(column)
                .toString());
    }

    /**
     * Convenience method to return a String.
     *
     * @param name A String with the name of the key.
     * @return A String with the value of object at key.
     */
    public String getString(String name)
    {
        return (String) getCriterion(name).getValue();
    }

    /**
     * Convenience method to return a String.
     *
     * @param table String name of table.
     * @param column String name of column.
     * @return A String with the value of object at key.
     */
    public String getString(String table, String column)
    {
        return getString(
                new StringBuffer(table.length() + column.length() + 1)
                .append(table).append('.').append(column)
                .toString());
    }

    /**
     * Method to return a String table name.
     *
     * @param name A String with the name of the key.
     * @return A String with the value of object at key.
     */
    public String getTableName(String name)
    {
        return getCriterion(name).getTable();
    }

    /**
     * Convenience method to return a List.
     *
     * @param name A String with the name of the key.
     * @return A List with the value of object at key.
     */
    public List getList(String name)
    {
        return (List) getCriterion(name).getValue();
    }

    /**
     * Convenience method to return a List.
     *
     * @param table String name of table.
     * @param column String name of column.
     * @return A List with the value of object at key.
     */
    public List getList(String table, String column)
    {
        return getList(
                new StringBuffer(table.length() + column.length() + 1)
                .append(table).append('.').append(column)
                .toString());
    }

    /**
     * Method to return the value that was added to Criteria.
     *
     * @param name A String with the name of the key.
     * @return An Object with the value of object at key.
     */
    public Object getValue(String name)
    {
        return getCriterion(name).getValue();
    }

    /**
     * Method to return the value that was added to Criteria.
     *
     * @param table String name of table.
     * @param column String name of column.
     * @return An Object with the value of object at key.
     */
    public Object getValue(String table, String column)
    {
        return getValue(
                new StringBuffer(table.length() + column.length() + 1)
                .append(table).append('.').append(column)
                .toString());
    }

    /**
     * Convenience method to return an ObjectKey.
     *
     * @param name A String with the name of the key.
     * @return An ObjectKey with the value of object at key.
     */
    public ObjectKey getObjectKey(String name)
    {
        return (ObjectKey) getCriterion(name).getValue();
    }

    /**
     * Convenience method to return an ObjectKey.
     *
     * @param table String name of table.
     * @param column String name of column.
     * @return A String with the value of object at key.
     */
    public ObjectKey getObjectKey(String table, String column)
    {
        return getObjectKey(
                new StringBuffer(table.length() + column.length() + 1)
                .append(table).append('.').append(column)
                .toString());
    }

    /**
     * Overrides Hashtable get, so that the value placed in the
     * Criterion is returned instead of the Criterion.
     *
     * @param key An Object.
     * @return An Object.
     */
    public Object get(Object key)
    {
        return getValue((String) key);
    }

    /**
     * Overrides Hashtable put, so that this object is returned
     * instead of the value previously in the Criteria object.
     * The reason is so that it more closely matches the behavior
     * of the add() methods. If you want to get the previous value
     * then you should first Criteria.get() it yourself. Note, if
     * you attempt to pass in an Object that is not a String, it will
     * throw a NPE. The reason for this is that none of the add()
     * methods support adding anything other than a String as a key.
     *
     * @param key An Object. Must be instanceof String!
     * @param value An Object.
     * @throws NullPointerException if key != String or key/value is null.
     * @return Instance of self.
     */
    public Object put(Object key, Object value)
    {
        if (!(key instanceof String))
        {
            throw new NullPointerException(
                    "Criteria: Key must be a String object.");
        }
        return add((String) key, value);
    }

    /**
     * Copies all of the mappings from the specified Map to this Criteria
     * These mappings will replace any mappings that this Criteria had for any
     * of the keys currently in the specified Map.
     *
     * if the map was another Criteria, its attributes are copied to this
     * Criteria, overwriting previous settings.
     *
     * @param t Mappings to be stored in this map.
     */
    public synchronized void putAll(Map t)
    {
        Iterator i = t.entrySet().iterator();
        while (i.hasNext())
        {
            Map.Entry e = (Map.Entry) i.next();
            Object val = e.getValue();
            if (val instanceof Criteria.Criterion)
            {
                super.put(e.getKey(), val);
            }
            else
            {
                put(e.getKey(), val);
            }
        }
        if (t instanceof Criteria)
        {
            Criteria c = (Criteria) t;
            this.joins = c.joins;
        }
        /* this would make a copy, not included
           but might want to use some of it.
           if (t instanceof Criteria)
           {
           Criteria c = (Criteria)t;
           this.ignoreCase = c.ignoreCase;
           this.singleRecord = c.singleRecord;
           this.cascade = c.cascade;
           this.selectModifiers = c.selectModifiers;
           this.selectColumns = c.selectColumns;
           this.orderByColumns = c.orderByColumns;
           this.dbName = c.dbName;
           this.limit = c.limit;
           this.offset = c.offset;
           this.aliases = c.aliases;
           }
        */
    }

    /**
     * This method adds a new criterion to the list of criterias. If a
     * criterion for the requested column already exists, it is
     * replaced. This is used as follows:
     *
     * <p>
     * <code>
     * Criteria crit = new Criteria().add(&quot;column&quot;,
     *                                      &quot;value&quot;);
     * </code>
     *
     * An EQUAL comparison is used for column and value.
     *
     * The name of the table must be used implicitly in the column name,
     * so the Column name must be something like 'TABLE.id'. If you
     * don't like this, you can use the add(table, column, value) method.
     *
     * @param column The column to run the comparison on
     * @param value An Object.
     *
     * @return A modified Criteria object.
     */
    public Criteria add (String column, Object value)
    {
        add(column, value, EQUAL);
        return this;
    }

    /**
     * This method adds a new criterion to the list of criterias.
     * If a criterion for the requested column already exists, it is
     * replaced. If is used as follow:
     *
     * <p>
     * <code>
     * Criteria crit = new Criteria().add(&quot;column&quot;,
     *                                      &quot;value&quot;
     *                                      Criteria.GREATER_THAN);
     * </code>
     *
     * Any comparison can be used.
     *
     * The name of the table must be used implicitly in the column name,
     * so the Column name must be something like 'TABLE.id'. If you
     * don't like this, you can use the add(table, column, value) method.
     *
     * @param column The column to run the comparison on
     * @param value An Object.
     * @param comparison A String.
     *
     * @return A modified Criteria object.
     */
    public Criteria add(String column, Object value, SqlEnum comparison)
    {
        super.put(column, new Criterion(column, value, comparison));
        return this;
    }

    /**
     * This method adds a new criterion to the list of criterias.
     * If a criterion for the requested column already exists, it is
     * replaced. If is used as follows:
     *
     * <p>
     * <code>
     * Criteria crit = new Criteria().add(&quot;table&quot;,
     *                                      &quot;column&quot;,
     *                                      &quot;value&quot;);
     * </code>
     *
     * An EQUAL comparison is used for column and value.
     *
     * @param table Name of the table which contains the column
     * @param column The column to run the comparison on
     * @param value An Object.
     *
     * @return A modified Criteria object.
     */
    public Criteria add(String table, String column, Object value)
    {
        add(table, column, value, EQUAL);
        return this;
    }

    /**
     * This method adds a new criterion to the list of criterias.
     * If a criterion for the requested column already exists, it is
     * replaced. If is used as follows:
     *
     * <p>
     * <code>
     * Criteria crit = new Criteria().add(&quot;table&quot;,
     *                                      &quot;column&quot;,
     *                                      &quot;value&quot;,
     *                                      Criteria.GREATER_THAN);
     * </code>
     *
     * Any comparison can be used.
     *
     * @param table Name of table which contains the column
     * @param column The column to run the comparison on
     * @param value An Object.
     * @param comparison String describing how to compare the column with
     *        the value
     * @return A modified Criteria object.
     */
    public Criteria add(String table,
            String column,
            Object value,
            SqlEnum comparison)
    {
        StringBuffer sb = new StringBuffer(table.length()
                + column.length() + 1);
        sb.append(table);
        sb.append('.');
        sb.append(column);
        super.put(sb.toString(),
                new Criterion(table, column, value, comparison));
        return this;
    }

    /**
     * Convenience method to add a boolean to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * add(column, new Boolean(value), EQUAL);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value A Boolean.
     *
     * @return A modified Criteria object.
     */
    public Criteria add(String column, boolean value)
    {
        add(column, (value ? Boolean.TRUE : Boolean.FALSE));
        return this;
    }

    /**
     * Convenience method to add a boolean to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * add(column, new Boolean(value), comparison);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value A Boolean.
     * @param comparison String describing how to compare the column with
     *        the value
     * @return A modified Criteria object.
     */
    public Criteria add(String column, boolean value, SqlEnum comparison)
    {
        add(column, new Boolean(value), comparison);
        return this;
    }

    /**
     * Convenience method to add an int to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * add(column, new Integer(value), EQUAL);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value An int.
     * @return A modified Criteria object.
     */
    public Criteria add(String column, int value)
    {
        add(column, new Integer(value));
        return this;
    }

    /**
     * Convenience method to add an int to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * add(column, new Integer(value), comparison);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value An int.
     * @param comparison String describing how to compare the column with
     *        the value
     * @return A modified Criteria object.
     */
    public Criteria add(String column, int value, SqlEnum comparison)
    {
        add(column, new Integer(value), comparison);
        return this;
    }

    /**
     * Convenience method to add a long to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * add(column, new Long(value), EQUAL);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value A long.
     * @return A modified Criteria object.
     */
    public Criteria add(String column, long value)
    {
        add(column, new Long(value));
        return this;
    }

    /**
     * Convenience method to add a long to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * add(column, new Long(value), comparison);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value A long.
     * @param comparison String describing how to compare the column with
     *        the value
     * @return A modified Criteria object.
     */
    public Criteria add(String column, long value, SqlEnum comparison)
    {
        add(column, new Long(value), comparison);
        return this;
    }

    /**
     * Convenience method to add a float to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * add(column, new Float(value), EQUAL);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value A float.
     * @return A modified Criteria object.
     */
    public Criteria add(String column, float value)
    {
        add(column, new Float(value));
        return this;
    }

    /**
     * Convenience method to add a float to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * add(column, new Float(value), comparison);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value A float.
     * @param comparison String describing how to compare the column with
     *        the value
     * @return A modified Criteria object.
     */
    public Criteria add(String column, float value, SqlEnum comparison)
    {
        add(column, new Float(value), comparison);
        return this;
    }

    /**
     * Convenience method to add a double to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * add(column, new Double(value), EQUAL);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value A double.
     * @return A modified Criteria object.
     */
    public Criteria add(String column, double value)
    {
        add(column, new Double(value));
        return this;
    }

    /**
     * Convenience method to add a double to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * add(column, new Double(value), comparison);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value A double.
     * @param comparison String describing how to compare the column with
     *        the value
     * @return A modified Criteria object.
     */
    public Criteria add(String column, double value, SqlEnum comparison)
    {
        add(column, new Double(value), comparison);
        return this;
    }

    /**
     * Convenience method to add a Date object specified by
     * year, month, and date into the Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * add(column, new GregorianCalendar(year, month,date), EQUAL);
     * </code>
     *
     * @param column A String value to use as column.
     * @param year An int with the year.
     * @param month An int with the month. Month value is 0-based.
     *        e.g., 0 for January
     * @param date An int with the date.
     * @return A modified Criteria object.
     */
    public Criteria addDate(String column, int year, int month, int date)
    {
        add(column, new GregorianCalendar(year, month, date).getTime());
        return this;
    }

    /**
     * Convenience method to add a Date object specified by
     * year, month, and date into the Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * add(column, new GregorianCalendar(year, month,date), comparison);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param year An int with the year.
     * @param month An int with the month. Month value is 0-based.
     *        e.g., 0 for January
     * @param date An int with the date.
     * @param comparison String describing how to compare the column with
     *        the value
     * @return A modified Criteria object.
     */
    public Criteria addDate(String column, int year, int month, int date,
            SqlEnum comparison)
    {
        add(column, new GregorianCalendar(year, month, date).getTime(),
                comparison);
        return this;
    }

    /**
     * This is the way that you should add a join of two tables.  For
     * example:
     *
     * <p>
     * AND PROJECT.PROJECT_ID=FOO.PROJECT_ID
     * <p>
     *
     * left = PROJECT.PROJECT_ID
     * right = FOO.PROJECT_ID
     *
     * @param left A String with the left side of the join.
     * @param right A String with the right side of the join.
     * @return A modified Criteria object.
     */
    public Criteria addJoin(String left, String right)
    {
        return addJoin(left, right, null);
    }

    /**
     * This is the way that you should add a join of two tables.  For
     * example:
     *
     * <p>
     * PROJECT LEFT JOIN FOO ON PROJECT.PROJECT_ID=FOO.PROJECT_ID
     * <p>
     *
     * left = &quot;PROJECT.PROJECT_ID&quot;
     * right = &quot;FOO.PROJECT_ID&quot;
     * operator = Criteria.LEFT_JOIN
     *
     * @param left A String with the left side of the join.
     * @param right A String with the right side of the join.
     * @param operator The operator used for the join: must be one of null,
     *        Criteria.LEFT_JOIN, Criteria.RIGHT_JOIN, Criteria.INNER_JOIN
     * @return A modified Criteria object.
     */
    public Criteria addJoin(String left, String right, SqlEnum operator)
    {
        joins.add(new Join(left, right, operator));

        return this;
    }

    /**
     * get the List of Joins.  This method is meant to
     * be called by BasePeer.
     * @return a List which contains objects of type Join.
     *         If the criteria does not contains any joins, the list is empty
     */
    public List getJoins()
    {
        return joins;
    }

    /**
     * get one side of the set of possible joins.  This method is meant to
     * be called by BasePeer.
     *
     * @deprecated This method is no longer used by BasePeer.
     */
    public List getJoinL()
    {
        throw new RuntimeException("getJoinL() in Criteria is no longer supported!");
    }

    /**
     * get one side of the set of possible joins.  This method is meant to
     * be called by BasePeer.
     *
     * @deprecated This method is no longer used by BasePeer.
     */
    public List getJoinR()
    {
        throw new RuntimeException("getJoinL() in Criteria is no longer supported!");
    }

    /**
     * Adds an 'IN' clause with the criteria supplied as an Object
     * array.  For example:
     *
     * <p>
     * FOO.NAME IN ('FOO', 'BAR', 'ZOW')
     * <p>
     *
     * where 'values' contains three objects that evaluate to the
     * respective strings above when .toString() is called.
     *
     * If a criterion for the requested column already exists, it is
     * replaced.
     *
     * @param column The column to run the comparison on
     * @param values An Object[] with the allowed values.
     * @return A modified Criteria object.
     */
    public Criteria addIn(String column, Object[] values)
    {
        add(column, (Object) values, Criteria.IN);
        return this;
    }

    /**
     * Adds an 'IN' clause with the criteria supplied as an int array.
     * For example:
     *
     * <p>
     * FOO.ID IN ('2', '3', '7')
     * <p>
     *
     * where 'values' contains those three integers.
     *
     * If a criterion for the requested column already exists, it is
     * replaced.
     *
     * @param column The column to run the comparison on
     * @param values An int[] with the allowed values.
     * @return A modified Criteria object.
     */
    public Criteria addIn(String column, int[] values)
    {
        add(column, (Object) values, Criteria.IN);
        return this;
    }

    /**
     * Adds an 'IN' clause with the criteria supplied as a List.
     * For example:
     *
     * <p>
     * FOO.NAME IN ('FOO', 'BAR', 'ZOW')
     * <p>
     *
     * where 'values' contains three objects that evaluate to the
     * respective strings above when .toString() is called.
     *
     * If a criterion for the requested column already exists, it is
     * replaced.
     *
     * @param column The column to run the comparison on
     * @param values A List with the allowed values.
     * @return A modified Criteria object.
     */
    public Criteria addIn(String column, List values)
    {
        add(column, (Object) values, Criteria.IN);
        return this;
    }

    /**
     * Adds a 'NOT IN' clause with the criteria supplied as an Object
     * array.  For example:
     *
     * <p>
     * FOO.NAME NOT IN ('FOO', 'BAR', 'ZOW')
     * <p>
     *
     * where 'values' contains three objects that evaluate to the
     * respective strings above when .toString() is called.
     *
     * If a criterion for the requested column already exists, it is
     * replaced.
     *
     * @param column The column to run the comparison on
     * @param values An Object[] with the disallowed values.
     * @return A modified Criteria object.
     */
    public Criteria addNotIn(String column, Object[] values)
    {
        add(column, (Object) values, Criteria.NOT_IN);
        return this;
    }

    /**
     * Adds a 'NOT IN' clause with the criteria supplied as an int
     * array.  For example:
     *
     * <p>
     * FOO.ID NOT IN ('2', '3', '7')
     * <p>
     *
     * where 'values' contains those three integers.
     *
     * If a criterion for the requested column already exists, it is
     * replaced.
     *
     * @param column The column to run the comparison on
     * @param values An int[] with the disallowed values.
     * @return A modified Criteria object.
     */
    public Criteria addNotIn(String column, int[] values)
    {
        add(column, (Object) values, Criteria.NOT_IN);
        return this;
    }

    /**
     * Adds a 'NOT IN' clause with the criteria supplied as a List.
     * For example:
     *
     * <p>
     * FOO.NAME NOT IN ('FOO', 'BAR', 'ZOW')
     * <p>
     *
     * where 'values' contains three objects that evaluate to the
     * respective strings above when .toString() is called.
     *
     * If a criterion for the requested column already exists, it is
     * replaced.
     *
     * @param column The column to run the comparison on
     * @param values A List with the disallowed values.
     * @return A modified Criteria object.
     */
    public Criteria addNotIn(String column, List values)
    {
        add(column, (Object) values, Criteria.NOT_IN);
        return this;
    }

    /**
     * Adds &quot;ALL &quot; to the SQL statement.
     */
    public void setAll()
    {
        selectModifiers.add(ALL.toString());
    }

    /**
     * Adds &quot;DISTINCT &quot; to the SQL statement.
     */
    public void setDistinct()
    {
        selectModifiers.add(DISTINCT.toString());
    }

    /**
     * Sets ignore case.
     *
     * @param b True if case should be ignored.
     * @return A modified Criteria object.
     */
    public Criteria setIgnoreCase(boolean b)
    {
        ignoreCase = b;
        return this;
    }

    /**
     * Is ignore case on or off?
     *
     * @return True if case is ignored.
     */
    public boolean isIgnoreCase()
    {
        return ignoreCase;
    }

    /**
     * Set single record?  Set this to <code>true</code> if you expect the query
     * to result in only a single result record (the default behaviour is to
     * throw a TorqueException if multiple records are returned when the query
     * is executed).  This should be used in situations where returning multiple
     * rows would indicate an error of some sort.  If your query might return
     * multiple records but you are only interested in the first one then you
     * should be using setLimit(1).
     *
     * @param b set to <code>true</code> if you expect the query to select just
     * one record.
     * @return A modified Criteria object.
     */
    public Criteria setSingleRecord(boolean b)
    {
        singleRecord = b;
        return this;
    }

    /**
     * Is single record?
     *
     * @return True if a single record is being returned.
     */
    public boolean isSingleRecord()
    {
        return singleRecord;
    }

    /**
     * Set cascade.
     *
     * @param b True if cascade is set.
     * @return A modified Criteria object.
     */
    public Criteria setCascade(boolean b)
    {
        cascade = b;
        return this;
    }

    /**
     * Is cascade set?
     *
     * @return True if cascade is set.
     */
    public boolean isCascade()
    {
        return cascade;
    }

    /**
     * Set limit.
     *
     * @param limit An int with the value for limit.
     * @return A modified Criteria object.
     */
    public Criteria setLimit(int limit)
    {
        this.limit = limit;
        return this;
    }

    /**
     * Get limit.
     *
     * @return An int with the value for limit.
     */
    public int getLimit()
    {
        return limit;
    }

    /**
     * Set offset.
     *
     * @param offset An int with the value for offset.
     * @return A modified Criteria object.
     */
    public Criteria setOffset(int offset)
    {
        this.offset = offset;
        return this;
    }

    /**
     * Get offset.
     *
     * @return An int with the value for offset.
     */
    public int getOffset()
    {
        return offset;
    }

    /**
     * Add select column.
     *
     * @param name A String with the name of the select column.
     * @return A modified Criteria object.
     */
    public Criteria addSelectColumn(String name)
    {
        selectColumns.add(name);
        return this;
    }

    /**
     * Get select columns.
     *
     * @return An StringStack with the name of the select
     * columns.
     */
    public UniqueList getSelectColumns()
    {
        return selectColumns;
    }

    /**
     * Get select modifiers.
     *
     * @return An UniqueList with the select modifiers.
     */
    public UniqueList getSelectModifiers()
    {
        return selectModifiers;
    }

    /**
     * Add group by column name.
     *
     * @param groupBy The name of the column to group by.
     * @return A modified Criteria object.
     */
    public Criteria addGroupByColumn(String groupBy)
    {
        groupByColumns.add(groupBy);
        return this;
    }

    /**
     * Add order by column name, explicitly specifying ascending.
     *
     * @param name The name of the column to order by.
     * @return A modified Criteria object.
     */
    public Criteria addAscendingOrderByColumn(String name)
    {
        orderByColumns.add(name + ' ' + ASC);
        return this;
    }

    /**
     * Add order by column name, explicitly specifying descending.
     *
     * @param name The name of the column to order by.
     * @return A modified Criteria object.
     */
    public Criteria addDescendingOrderByColumn(String name)
    {
        orderByColumns.add(name + ' ' + DESC);
        return this;
    }

    /**
     * Get order by columns.
     *
     * @return An UniqueList with the name of the order columns.
     */
    public UniqueList getOrderByColumns()
    {
        return orderByColumns;
    }

    /**
     * Get group by columns.
     *
     * @return An UniqueList with the name of the groupBy clause.
     */
    public UniqueList getGroupByColumns()
    {
        return groupByColumns;
    }

    /**
     * Get Having Criterion.
     *
     * @return A Criterion that is the having clause.
     */
    public Criterion getHaving()
    {
        return having;
    }

    /**
     * Remove an object from the criteria.
     *
     * @param key A String with the key to be removed.
     * @return The removed object.
     */
    public Object remove(String key)
    {
        Object foo = super.remove(key);
        if (foo instanceof Criterion)
        {
            return ((Criterion) foo).getValue();
        }
        return foo;
    }

    /**
     * Build a string representation of the Criteria.
     *
     * @return A String with the representation of the Criteria.
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer("Criteria:: ");
        Iterator it = keySet().iterator();
        while (it.hasNext())
        {
            String key = (String) it.next();
            sb.append(key).append("<=>")
                    .append(super.get(key).toString()).append(":  ");
        }

        try
        {
            sb.append("\nCurrent Query SQL (may not be complete or applicable): ")
                    .append(BasePeer.createQueryDisplayString(this));
        }
        catch (Exception exc)
        {
            log.debug("Exception when evaluating a Criteria", exc);
        }

        return sb.toString();
    }

    /**
     * This method checks another Criteria to see if they contain
     * the same attributes and hashtable entries.
     */
    public boolean equals(Object crit)
    {
        boolean isEquiv = false;
        if (crit == null || !(crit instanceof Criteria))
        {
            isEquiv = false;
        }
        else if (this == crit)
        {
            isEquiv = true;
        }
        else if (this.size() == ((Criteria) crit).size())
        {
            Criteria criteria = (Criteria) crit;
            if (this.offset == criteria.getOffset()
                    && this.limit == criteria.getLimit()
                    && ignoreCase == criteria.isIgnoreCase()
                    && singleRecord == criteria.isSingleRecord()
                    && cascade == criteria.isCascade()
                    && dbName.equals(criteria.getDbName())
                    && selectModifiers.equals(criteria.getSelectModifiers())
                    && selectColumns.equals(criteria.getSelectColumns())
                    && orderByColumns.equals(criteria.getOrderByColumns())
                    && aliases.equals(criteria.getAliases())
                    && asColumns.equals(criteria.getAsColumns())
                    && joins.equals(criteria.getJoins())
                )
            {
                isEquiv = true;
                for (Iterator it = criteria.keySet().iterator(); it.hasNext();)
                {
                    String key = (String) it.next();
                    if (this.containsKey(key))
                    {
                        Criterion a = this.getCriterion(key);
                        Criterion b = criteria.getCriterion(key);
                        if (!a.equals(b))
                        {
                            isEquiv = false;
                            break;
                        }
                    }
                    else
                    {
                        isEquiv = false;
                        break;
                    }
                }
            }
        }
        return isEquiv;
    }

    /**
     * Returns the hash code value for this Join.
     *
     * @return a hash code value for this object.
     */
    public int hashCode()
    {
        int result = 16;
        result = 37 * result + offset;
        result = 37 * result + limit;
        result = 37 * result + (ignoreCase ? 0 : 1);
        result = 37 * result + (singleRecord ? 0 : 1);
        result = 37 * result + (cascade ? 0 : 1);
        result = 37 * result + dbName.hashCode();
        result = 37 * result + selectModifiers.hashCode();
        result = 37 * result + selectColumns.hashCode();
        result = 37 * result + orderByColumns.hashCode();
        result = 37 * result + aliases.hashCode();
        result = 37 * result + asColumns.hashCode();
        result = 37 * result + joins.hashCode();
        result = 37 * result + super.hashCode();
        return result;
    }

    /*
     * ------------------------------------------------------------------------
     *
     * Start of the "and" methods
     *
     * ------------------------------------------------------------------------
     */

    /**
     * This method adds a prepared Criterion object to the Criteria as a having
     * clause. You can get a new, empty Criterion object with the
     * getNewCriterion() method.
     *
     * <p>
     * <code>
     * Criteria crit = new Criteria();
     * Criteria.Criterion c = crit.getNewCriterion(BasePeer.ID, new Integer(5),
     *         Criteria.LESS_THAN);
     * crit.addHaving(c);
     * </code>
     *
     * @param having A Criterion object
     * @return A modified Criteria object.
     */
    public Criteria addHaving(Criterion having)
    {
        this.having = having;
        return this;
    }

    /**
     * This method adds a prepared Criterion object to the Criteria.
     * You can get a new, empty Criterion object with the
     * getNewCriterion() method. If a criterion for the requested column
     * already exists, it is &quot;AND&quot;ed to the existing criterion.
     * This is used as follows:
     *
     * <p>
     * <code>
     * Criteria crit = new Criteria();
     * Criteria.Criterion c = crit.getNewCriterion(BasePeer.ID, new Integer(5),
     *         Criteria.LESS_THAN);
     * crit.and(c);
     * </code>
     *
     * @param c A Criterion object
     * @return A modified Criteria object.
     */
    public Criteria and(Criterion c)
    {
        Criterion oc = getCriterion(c.getTable() + '.' + c.getColumn());

        if (oc == null)
        {
            add(c);
        }
        else
        {
            oc.and(c);
        }
        return this;
    }

    /**
     * This method adds a new criterion to the list of criterias. If a
     * criterion for the requested column already exists, it is
     * &quot;AND&quot;ed to the existing criterion. This is used as follows:
     *
     * <p>
     * <code>
     * Criteria crit = new Criteria().and(&quot;column&quot;,
     *                                      &quot;value&quot;);
     * </code>
     *
     * An EQUAL comparison is used for column and value.
     *
     * The name of the table must be used implicitly in the column name,
     * so the Column name must be something like 'TABLE.id'. If you
     * don't like this, you can use the and(table, column, value) method.
     *
     * @param column The column to run the comparison on
     * @param value An Object.
     *
     * @return A modified Criteria object.
     */
    public Criteria and(String column, Object value)
    {
        and(column, value, EQUAL);
        return this;
    }

    /**
     * This method adds a new criterion to the list of criterias.
     * If a criterion for the requested column already exists, it is
     * &quot;AND&quot;ed to the existing criterion. If is used as follow:
     *
     * <p>
     * <code>
     * Criteria crit = new Criteria().and(&quot;column&quot;,
     *                                      &quot;value&quot;
     *                                      Criteria.GREATER_THAN);
     * </code>
     *
     * Any comparison can be used.
     *
     * The name of the table must be used implicitly in the column name,
     * so the Column name must be something like 'TABLE.id'. If you
     * don't like this, you can use the and(table, column, value) method.
     *
     * @param column The column to run the comparison on
     * @param value An Object.
     * @param comparison A String.
     *
     * @return A modified Criteria object.
     */
    public Criteria and(String column, Object value, SqlEnum comparison)
    {
        Criterion oc = getCriterion(column);
        Criterion nc = new Criterion(column, value, comparison);

        if (oc == null)
        {
            super.put(column, nc);
        }
        else
        {
            oc.and(nc);
        }
        return this;
    }

    /**
     * This method adds a new criterion to the list of criterias.
     * If a criterion for the requested column already exists, it is
     * &quot;AND&quot;ed to the existing criterion. If is used as follows:
     *
     * <p>
     * <code>
     * Criteria crit = new Criteria().and(&quot;table&quot;,
     *                                      &quot;column&quot;,
     *                                      &quot;value&quot;);
     * </code>
     *
     * An EQUAL comparison is used for column and value.
     *
     * @param table Name of the table which contains the column
     * @param column The column to run the comparison on
     * @param value An Object.
     * @return A modified Criteria object.
     */
    public Criteria and(String table, String column, Object value)
    {
        and(table, column, value, EQUAL);
        return this;
    }

    /**
     * This method adds a new criterion to the list of criterias.
     * If a criterion for the requested column already exists, it is
     * &quot;AND&quot;ed to the existing criterion. If is used as follows:
     *
     * <p>
     * <code>
     * Criteria crit = new Criteria().and(&quot;table&quot;,
     *                                      &quot;column&quot;,
     *                                      &quot;value&quot;,
     *                                      &quot;Criterion.GREATER_THAN&quot;);
     * </code>
     *
     * Any comparison can be used.
     *
     * @param table Name of table which contains the column
     * @param column The column to run the comparison on
     * @param value An Object.
     * @param comparison String describing how to compare the column with
     *        the value
     * @return A modified Criteria object.
     */
    public Criteria and(String table, String column, Object value,
            SqlEnum comparison)
    {
        StringBuffer sb = new StringBuffer(table.length()
                + column.length() + 1);
        sb.append(table);
        sb.append('.');
        sb.append(column);

        Criterion oc = getCriterion(table, column);
        Criterion nc = new Criterion(table, column, value, comparison);

        if (oc == null)
        {
            super.put(sb.toString(), nc);
        }
        else
        {
            oc.and(nc);
        }
        return this;
    }

    /**
     * Convenience method to add a boolean to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * and(column, new Boolean(value), EQUAL);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value A Boolean.
     * @return A modified Criteria object.
     */
    public Criteria and(String column, boolean value)
    {
        and(column, new Boolean(value));
        return this;
    }

    /**
     * Convenience method to add a boolean to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * and(column, new Boolean(value), comparison);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value A Boolean.
     * @param comparison String describing how to compare the column
     * with the value
     * @return A modified Criteria object.
     */
    public Criteria and(String column, boolean value, SqlEnum comparison)
    {
        and(column, new Boolean(value), comparison);
        return this;
    }

    /**
     * Convenience method to add an int to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * and(column, new Integer(value), EQUAL);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value An int.
     * @return A modified Criteria object.
     */
    public Criteria and(String column, int value)
    {
        and(column, new Integer(value));
        return this;
    }

    /**
     * Convenience method to add an int to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * and(column, new Integer(value), comparison);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value An int.
     * @param comparison String describing how to compare the column with the value
     * @return A modified Criteria object.
     */
    public Criteria and(String column, int value, SqlEnum comparison)
    {
        and(column, new Integer(value), comparison);
        return this;
    }

    /**
     * Convenience method to add a long to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * and(column, new Long(value), EQUAL);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value A long.
     * @return A modified Criteria object.
     */
    public Criteria and(String column, long value)
    {
        and(column, new Long(value));
        return this;
    }

    /**
     * Convenience method to add a long to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * and(column, new Long(value), comparison);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value A long.
     * @param comparison String describing how to compare the column with
     *        the value
     * @return A modified Criteria object.
     */
    public Criteria and(String column, long value, SqlEnum comparison)
    {
        and(column, new Long(value), comparison);
        return this;
    }

    /**
     * Convenience method to add a float to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * and(column, new Float(value), EQUAL);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value A float.
     * @return A modified Criteria object.
     */
    public Criteria and(String column, float value)
    {
        and(column, new Float(value));
        return this;
    }

    /**
     * Convenience method to add a float to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * and(column, new Float(value), comparison);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value A float.
     * @param comparison String describing how to compare the column with
     *        the value
     * @return A modified Criteria object.
     */
    public Criteria and(String column, float value, SqlEnum comparison)
    {
        and(column, new Float(value), comparison);
        return this;
    }

    /**
     * Convenience method to add a double to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * and(column, new Double(value), EQUAL);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value A double.
     * @return A modified Criteria object.
     */
    public Criteria and(String column, double value)
    {
        and(column, new Double(value));
        return this;
    }

    /**
     * Convenience method to add a double to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * and(column, new Double(value), comparison);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value A double.
     * @param comparison String describing how to compare the column with
     *        the value
     * @return A modified Criteria object.
     */
    public Criteria and(String column, double value, SqlEnum comparison)
    {
        and(column, new Double(value), comparison);
        return this;
    }

    /**
     * Convenience method to add a Date object specified by
     * year, month, and date into the Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * and(column, new GregorianCalendar(year, month,date), EQUAL);
     * </code>
     *
     * @param column A String value to use as column.
     * @param year An int with the year.
     * @param month An int with the month.
     * @param date An int with the date.
     * @return A modified Criteria object.
     */
    public Criteria andDate(String column, int year, int month, int date)
    {
        and(column, new GregorianCalendar(year, month, date).getTime());
        return this;
    }

    /**
     * Convenience method to add a Date object specified by
     * year, month, and date into the Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * and(column, new GregorianCalendar(year, month,date), comparison);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param year An int with the year.
     * @param month An int with the month.
     * @param date An int with the date.
     * @param comparison String describing how to compare the column with
     *        the value
     * @return A modified Criteria object.
     */
    public Criteria andDate(String column, int year, int month, int date,
            SqlEnum comparison)
    {
        and(column, new GregorianCalendar(year, month, date).getTime(), comparison);
        return this;
    }

    /**
     * Adds an 'IN' clause with the criteria supplied as an Object array.
     * For example:
     *
     * <p>
     * FOO.NAME IN ('FOO', 'BAR', 'ZOW')
     * <p>
     *
     * where 'values' contains three objects that evaluate to the
     * respective strings above when .toString() is called.
     *
     * If a criterion for the requested column already exists, it is
     * &quot;AND&quot;ed to the existing criterion.
     *
     * @param column The column to run the comparison on
     * @param values An Object[] with the allowed values.
     * @return A modified Criteria object.
     */
    public Criteria andIn(String column, Object[] values)
    {
        and(column, (Object) values, Criteria.IN);
        return this;
    }

    /**
     * Adds an 'IN' clause with the criteria supplied as an int array.
     * For example:
     *
     * <p>
     * FOO.ID IN ('2', '3', '7')
     * <p>
     *
     * where 'values' contains those three integers.
     *
     * If a criterion for the requested column already exists, it is
     * &quot;AND&quot;ed to the existing criterion.
     *
     * @param column The column to run the comparison on
     * @param values An int[] with the allowed values.
     * @return A modified Criteria object.
     */
    public Criteria andIn(String column, int[] values)
    {
        and(column, (Object) values, Criteria.IN);
        return this;
    }

    /**
     * Adds an 'IN' clause with the criteria supplied as a List.
     * For example:
     *
     * <p>
     * FOO.NAME IN ('FOO', 'BAR', 'ZOW')
     * <p>
     *
     * where 'values' contains three objects that evaluate to the
     * respective strings above when .toString() is called.
     *
     * If a criterion for the requested column already exists, it is
     * &quot;AND&quot;ed to the existing criterion.
     *
     * @param column The column to run the comparison on
     * @param values A List with the allowed values.
     * @return A modified Criteria object.
     */
    public Criteria andIn(String column, List values)
    {
        and(column, (Object) values, Criteria.IN);
        return this;
    }

    /**
     * Adds a 'NOT IN' clause with the criteria supplied as an Object
     * array.  For example:
     *
     * <p>
     * FOO.NAME NOT IN ('FOO', 'BAR', 'ZOW')
     * <p>
     *
     * where 'values' contains three objects that evaluate to the
     * respective strings above when .toString() is called.
     *
     * If a criterion for the requested column already exists, it is
     * &quot;AND&quot;ed to the existing criterion.
     *
     * @param column The column to run the comparison on
     * @param values An Object[] with the disallowed values.
     * @return A modified Criteria object.
     */
    public Criteria andNotIn(String column, Object[] values)
    {
        and(column, (Object) values, Criteria.NOT_IN);
        return this;
    }

    /**
     * Adds a 'NOT IN' clause with the criteria supplied as an int
     * array.  For example:
     *
     * <p>
     * FOO.ID NOT IN ('2', '3', '7')
     * <p>
     *
     * where 'values' contains those three integers.
     *
     * If a criterion for the requested column already exists, it is
     * &quot;AND&quot;ed to the existing criterion.
     *
     * @param column The column to run the comparison on
     * @param values An int[] with the disallowed values.
     * @return A modified Criteria object.
     */
    public Criteria andNotIn(String column, int[] values)
    {
        and(column, (Object) values, Criteria.NOT_IN);
        return this;
    }

    /**
     * Adds a 'NOT IN' clause with the criteria supplied as a List.
     * For example:
     *
     * <p>
     * FOO.NAME NOT IN ('FOO', 'BAR', 'ZOW')
     * <p>
     *
     * where 'values' contains three objects that evaluate to the
     * respective strings above when .toString() is called.
     *
     * If a criterion for the requested column already exists, it is
     * &quot;AND&quot;ed to the existing criterion.
     *
     * @param column The column to run the comparison on
     * @param values A List with the disallowed values.
     * @return A modified Criteria object.
     */
    public Criteria andNotIn(String column, List values)
    {
        and(column, (Object) values, Criteria.NOT_IN);
        return this;
    }

    /*
     * ------------------------------------------------------------------------
     *
     * Start of the "or" methods
     *
     * ------------------------------------------------------------------------
     */

    /**
     * This method adds a prepared Criterion object to the Criteria.
     * You can get a new, empty Criterion object with the
     * getNewCriterion() method. If a criterion for the requested column
     * already exists, it is &quot;OR&quot;ed to the existing criterion.
     * This is used as follows:
     *
     * <p>
     * <code>
     * Criteria crit = new Criteria();
     * Criteria.Criterion c = crit.getNewCriterion(BasePeer.ID, new Integer(5), Criteria.LESS_THAN);
     * crit.or(c);
     * </code>
     *
     * @param c A Criterion object
     * @return A modified Criteria object.
     */
    public Criteria or(Criterion c)
    {
        Criterion oc = getCriterion(c.getTable() + '.' + c.getColumn());

        if (oc == null)
        {
            add(c);
        }
        else
        {
            oc.or(c);
        }
        return this;
    }

    /**
     * This method adds a new criterion to the list of criterias. If a
     * criterion for the requested column already exists, it is
     * &quot;OR&quot;ed to the existing criterion. This is used as follows:
     *
     * <p>
     * <code>
     * Criteria crit = new Criteria().or(&quot;column&quot;,
     *                                      &quot;value&quot;);
     * </code>
     *
     * An EQUAL comparison is used for column and value.
     *
     * The name of the table must be used implicitly in the column name,
     * so the Column name must be something like 'TABLE.id'. If you
     * don't like this, you can use the or(table, column, value) method.
     *
     * @param column The column to run the comparison on
     * @param value An Object.
     *
     * @return A modified Criteria object.
     */
    public Criteria or(String column, Object value)
    {
        or(column, value, EQUAL);
        return this;
    }

    /**
     * This method adds a new criterion to the list of criterias.
     * If a criterion for the requested column already exists, it is
     * &quot;OR&quot;ed to the existing criterion. If is used as follow:
     *
     * <p>
     * <code>
     * Criteria crit = new Criteria().or(&quot;column&quot;,
     *                                      &quot;value&quot;
     *                                      &quot;Criterion.GREATER_THAN&quot;);
     * </code>
     *
     * Any comparison can be used.
     *
     * The name of the table must be used implicitly in the column name,
     * so the Column name must be something like 'TABLE.id'. If you
     * don't like this, you can use the or(table, column, value) method.
     *
     * @param column The column to run the comparison on
     * @param value An Object.
     * @param comparison A String.
     * @return A modified Criteria object.
     */
    public Criteria or(String column, Object value, SqlEnum comparison)
    {
        Criterion oc = getCriterion(column);
        Criterion nc = new Criterion(column, value, comparison);

        if (oc == null)
        {
            super.put(column, nc);
        }
        else
        {
            oc.or(nc);
        }
        return this;
    }

    /**
     * This method adds a new criterion to the list of criterias.
     * If a criterion for the requested column already exists, it is
     * &quot;OR&quot;ed to the existing criterion. If is used as follows:
     *
     * <p>
     * <code>
     * Criteria crit = new Criteria().or(&quot;table&quot;,
     *                                      &quot;column&quot;,
     *                                      &quot;value&quot;);
     * </code>
     *
     * An EQUAL comparison is used for column and value.
     *
     * @param table Name of the table which contains the column
     * @param column The column to run the comparison on
     * @param value An Object.
     * @return A modified Criteria object.
     */
    public Criteria or(String table, String column, Object value)
    {
        or(table, column, value, EQUAL);
        return this;
    }

    /**
     * This method adds a new criterion to the list of criterias.
     * If a criterion for the requested column already exists, it is
     * &quot;OR&quot;ed to the existing criterion. If is used as follows:
     *
     * <p>
     * <code>
     * Criteria crit = new Criteria().or(&quot;table&quot;,
     *                                      &quot;column&quot;,
     *                                      &quot;value&quot;,
     *                                      &quot;Criterion.GREATER_THAN&quot;);
     * </code>
     *
     * Any comparison can be used.
     *
     * @param table Name of table which contains the column
     * @param column The column to run the comparison on
     * @param value An Object.
     * @param comparison String describing how to compare the column with the value
     * @return A modified Criteria object.
     */
    public Criteria or(String table, String column, Object value,
            SqlEnum comparison)
    {
        StringBuffer sb = new StringBuffer(table.length() + column.length() + 1);
        sb.append(table);
        sb.append('.');
        sb.append(column);

        Criterion oc = getCriterion(table, column);
        Criterion nc = new Criterion(table, column, value, comparison);
        if (oc == null)
        {
            super.put(sb.toString(), nc);
        }
        else
        {
            oc.or(nc);
        }
        return this;
    }

    /**
     * Convenience method to add a boolean to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * or(column, new Boolean(value), EQUAL);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value A Boolean.
     * @return A modified Criteria object.
     */
    public Criteria or(String column, boolean value)
    {
        or(column, new Boolean(value));
        return this;
    }

    /**
     * Convenience method to add a boolean to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * or(column, new Boolean(value), comparison);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value A Boolean.
     * @param comparison String describing how to compare the column
     * with the value
     * @return A modified Criteria object.
     */
    public Criteria or(String column, boolean value, SqlEnum comparison)
    {
        or(column, new Boolean(value), comparison);
        return this;
    }

    /**
     * Convenience method to add an int to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * or(column, new Integer(value), EQUAL);
     * </code>
     *
     *
     * @param column The column to run the comparison on
     * @param value An int.
     * @return A modified Criteria object.
     */
    public Criteria or(String column, int value)
    {
        or(column, new Integer(value));
        return this;
    }

    /**
     * Convenience method to add an int to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * or(column, new Integer(value), comparison);
     * </code>
     *
     *
     * @param column The column to run the comparison on
     * @param value An int.
     * @param comparison String describing how to compare the column
     * with the value
     * @return A modified Criteria object.
     */
    public Criteria or(String column, int value, SqlEnum comparison)
    {
        or(column, new Integer(value), comparison);
        return this;
    }

    /**
     * Convenience method to add a long to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * or(column, new Long(value), EQUAL);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value A long.
     * @return A modified Criteria object.
     */
    public Criteria or(String column, long value)
    {
        or(column, new Long(value));
        return this;
    }

    /**
     * Convenience method to add a long to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * or(column, new Long(value), comparison);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value A long.
     * @param comparison String describing how to compare the column
     * with the value
     * @return A modified Criteria object.
     */
    public Criteria or(String column, long value, SqlEnum comparison)
    {
        or(column, new Long(value), comparison);
        return this;
    }

    /**
     * Convenience method to add a float to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * or(column, new Float(value), EQUAL);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value A float.
     * @return A modified Criteria object.
     */
    public Criteria or(String column, float value)
    {
        or(column, new Float(value));
        return this;
    }

    /**
     * Convenience method to add a float to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * or(column, new Float(value), comparison);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value A float.
     * @param comparison String describing how to compare the column
     * with the value
     * @return A modified Criteria object.
     */
    public Criteria or(String column, float value, SqlEnum comparison)
    {
        or(column, new Float(value), comparison);
        return this;
    }

    /**
     * Convenience method to add a double to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * or(column, new Double(value), EQUAL);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value A double.
     * @return A modified Criteria object.
     */
    public Criteria or(String column, double value)
    {
        or(column, new Double(value));
        return this;
    }

    /**
     * Convenience method to add a double to Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * or(column, new Double(value), comparison);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param value A double.
     * @param comparison String describing how to compare the column
     * with the value
     * @return A modified Criteria object.
     */
    public Criteria or(String column, double value, SqlEnum comparison)
    {
        or(column, new Double(value), comparison);
        return this;
    }

    /**
     * Convenience method to add a Date object specified by
     * year, month, and date into the Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * or(column, new GregorianCalendar(year, month,date), EQUAL);
     * </code>
     *
     * @param column A String value to use as column.
     * @param year An int with the year.
     * @param month An int with the month.
     * @param date An int with the date.
     * @return A modified Criteria object.
     */
    public Criteria orDate(String column, int year, int month, int date)
    {
        or(column, new GregorianCalendar(year, month, date));
        return this;
    }

    /**
     * Convenience method to add a Date object specified by
     * year, month, and date into the Criteria.
     * Equal to
     *
     * <p>
     * <code>
     * or(column, new GregorianCalendar(year, month,date), comparison);
     * </code>
     *
     * @param column The column to run the comparison on
     * @param year An int with the year.
     * @param month An int with the month.
     * @param date An int with the date.
     * @param comparison String describing how to compare the column
     * with the value
     * @return A modified Criteria object.
     */
    public Criteria orDate(String column, int year, int month, int date,
            SqlEnum comparison)
    {
        or(column, new GregorianCalendar(year, month, date), comparison);
        return this;
    }

    /**
     * Adds an 'IN' clause with the criteria supplied as an Object
     * array.  For example:
     *
     * <p>
     * FOO.NAME IN ('FOO', 'BAR', 'ZOW')
     * <p>
     *
     * where 'values' contains three objects that evaluate to the
     * respective strings above when .toString() is called.
     *
     * If a criterion for the requested column already exists, it is
     * &quot;OR&quot;ed to the existing criterion.
     *
     * @param column The column to run the comparison on
     * @param values An Object[] with the allowed values.
     * @return A modified Criteria object.
     */
    public Criteria orIn(String column, Object[] values)
    {
        or(column, (Object) values, Criteria.IN);
        return this;
    }

    /**
     * Adds an 'IN' clause with the criteria supplied as an int array.
     * For example:
     *
     * <p>
     * FOO.ID IN ('2', '3', '7')
     * <p>
     *
     * where 'values' contains those three integers.
     *
     * If a criterion for the requested column already exists, it is
     * &quot;OR&quot;ed to the existing criterion.
     *
     * @param column The column to run the comparison on
     * @param values An int[] with the allowed values.
     * @return A modified Criteria object.
     */
    public Criteria orIn(String column, int[] values)
    {
        or(column, (Object) values, Criteria.IN);
        return this;
    }

    /**
     * Adds an 'IN' clause with the criteria supplied as a List.
     * For example:
     *
     * <p>
     * FOO.NAME IN ('FOO', 'BAR', 'ZOW')
     * <p>
     *
     * where 'values' contains three objects that evaluate to the
     * respective strings above when .toString() is called.
     *
     * If a criterion for the requested column already exists, it is
     * &quot;OR&quot;ed to the existing criterion.
     *
     * @param column The column to run the comparison on
     * @param values A List with the allowed values.
     * @return A modified Criteria object.
     */
    public Criteria orIn(String column, List values)
    {
        or(column, (Object) values, Criteria.IN);
        return this;
    }

    /**
     * Adds a 'NOT IN' clause with the criteria supplied as an Object
     * array.  For example:
     *
     * <p>
     * FOO.NAME NOT IN ('FOO', 'BAR', 'ZOW')
     * <p>
     *
     * where 'values' contains three objects that evaluate to the
     * respective strings above when .toString() is called.
     *
     * If a criterion for the requested column already exists, it is
     * &quot;OR&quot;ed to the existing criterion.
     *
     * @param column The column to run the comparison on
     * @param values An Object[] with the disallowed values.
     * @return A modified Criteria object.
     */
    public Criteria orNotIn(String column, Object[] values)
    {
        or(column, (Object) values, Criteria.NOT_IN);
        return this;
    }

    /**
     * Adds a 'NOT IN' clause with the criteria supplied as an int
     * array.  For example:
     *
     * <p>
     * FOO.ID NOT IN ('2', '3', '7')
     * <p>
     *
     * where 'values' contains those three integers.
     *
     * If a criterion for the requested column already exists, it is
     * &quot;OR&quot;ed to the existing criterion.
     *
     * @param column The column to run the comparison on
     * @param values An int[] with the disallowed values.
     * @return A modified Criteria object.
     */
    public Criteria orNotIn(String column, int[] values)
    {
        or(column, (Object) values, Criteria.NOT_IN);
        return this;
    }

    /**
     * Adds a 'NOT IN' clause with the criteria supplied as a List.
     * For example:
     *
     * <p>
     * FOO.NAME NOT IN ('FOO', 'BAR', 'ZOW')
     * <p>
     *
     * where 'values' contains three objects that evaluate to the
     * respective strings above when .toString() is called.
     *
     * If a criterion for the requested column already exists, it is
     * &quot;OR&quot;ed to the existing criterion.
     *
     * @param column The column to run the comparison on
     * @param values A List with the disallowed values.
     * @return A modified Criteria object.
     */
    public Criteria orNotIn(String column, List values)
    {
        or(column, (Object) values, Criteria.NOT_IN);
        return this;
    }

    /**
     * Serializes this Criteria.
     *
     * @param s The output stream.
     * @throws IOException if an IO error occurs.
     */
    private void writeObject(ObjectOutputStream s) throws IOException
    {
        s.defaultWriteObject();

        // Joins need to be serialized manually.
        ArrayList serializableJoins = null;
        if (!joins.isEmpty())
        {
            serializableJoins = new ArrayList(joins.size());

            for (Iterator jonisIter = joins.iterator(); jonisIter.hasNext();)
            {
                Join join = (Join) jonisIter.next();

                ArrayList joinContent = new ArrayList(3);
                joinContent.add(join.getLeftColumn());
                joinContent.add(join.getRightColumn());
                joinContent.add(join.getJoinType());

                serializableJoins.add(joinContent);
            }
        }

        s.writeObject(serializableJoins);
    }

    /**
     * Deserialize a Criteria.
     *
     * @param s The input stream.
     * @throws IOException if an IO error occurs.
     * @throws ClassNotFoundException if the class cannot be located.
     */
    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException
    {
        s.defaultReadObject();

        // Criteria.put() differs somewhat from Hashtable.put().
        // This necessitates some corrective behavior upon deserialization.
        for (Iterator iter = keySet().iterator(); iter.hasNext();)
        {
            Object key = iter.next();
            Object value = get(key);
            if (value instanceof Criteria.Criterion)
            {
                super.put(key, value);
            }
        }

        // Joins need to be deserialized manually.
        this.joins = new ArrayList(3);
        
        ArrayList joins = (ArrayList) s.readObject();
        if (joins != null)
        {
            for (int i = 0; i < joins.size(); i++)
            {
                ArrayList joinContent = (ArrayList) joins.get(i);

                String leftColumn = (String) joinContent.get(0);
                String rightColumn = (String) joinContent.get(1);
                SqlEnum joinType = null;
                Object joinTypeObj = joinContent.get(2);
                if (joinTypeObj != null)
                {
                    joinType = (SqlEnum) joinTypeObj;
                }
                addJoin(leftColumn, rightColumn, joinType);
            }
        }
    }

    /**
     * This is an inner class that describes an object in the criteria.
     */
    public final class Criterion implements Serializable
    {
        /** Serial version. */
        private static final long serialVersionUID = 7157097965404611710L;

        public static final String AND = " AND ";
        public static final String OR = " OR ";

        /** Value of the CO. */
        private Object value;

        /** Comparison value. */
        private SqlEnum comparison;

        /** Table name. */
        private String table;

        /** Column name. */
        private String column;

        /** flag to ignore case in comparision */
        private boolean ignoreStringCase = false;

        /**
         * The DB adaptor which might be used to get db specific
         * variations of sql.
         */
        private DB db;

        /**
         * other connected criteria and their conjunctions.
         */
        private List clauses = new ArrayList();
        private List conjunctions = new ArrayList();

        /**
         * Creates a new instance, initializing a couple members.
         */
        private Criterion(Object val, SqlEnum comp)
        {
            this.value = val;
            this.comparison = comp;
        }

        /**
         * Create a new instance.
         *
         * @param table A String with the name of the table.
         * @param column A String with the name of the column.
         * @param val An Object with the value for the Criteria.
         * @param comp A String with the comparison value.
         */
        Criterion(String table, String column, Object val, SqlEnum comp)
        {
            this(val, comp);
            this.table = (table == null ? "" : table);
            this.column = (column == null ? "" : column);
        }

        /**
         * Create a new instance.
         *
         * @param tableColumn A String with the full name of the
         * column.
         * @param val An Object with the value for the Criteria.
         * @param comp A String with the comparison value.
         */
        Criterion(String tableColumn, Object val, SqlEnum comp)
        {
            this(val, comp);
            int dot = tableColumn.lastIndexOf('.');
            if (dot == -1)
            {
                table = "";
                column = tableColumn;
            }
            else
            {
                table = tableColumn.substring(0, dot);
                column = tableColumn.substring(dot + 1);
            }
        }

        /**
         * Create a new instance.
         *
         * @param table A String with the name of the table.
         * @param column A String with the name of the column.
         * @param val An Object with the value for the Criteria.
         */
        Criterion(String table, String column, Object val)
        {
            this(table, column, val, EQUAL);
        }

        /**
         * Create a new instance.
         *
         * @param tableColumn A String with the full name of the
         * column.
         * @param val An Object with the value for the Criteria.
         */
        Criterion(String tableColumn, Object val)
        {
            this(tableColumn, val, EQUAL);
        }

        /**
         * Get the column name.
         *
         * @return A String with the column name.
         */
        public String getColumn()
        {
            return this.column;
        }

        /**
         * Set the table name.
         *
         * @param name A String with the table name.
         */
        public void setTable(String name)
        {
            this.table = name;
        }

        /**
         * Get the table name.
         *
         * @return A String with the table name.
         */
        public String getTable()
        {
            return this.table;
        }

        /**
         * Get the comparison.
         *
         * @return A String with the comparison.
         */
        public SqlEnum getComparison()
        {
            return this.comparison;
        }

        /**
         * Get the value.
         *
         * @return An Object with the value.
         */
        public Object getValue()
        {
            return this.value;
        }

        /**
         * Set the value of the criterion.
         *
         * @param value the new value.
         */
        public void setValue(Object value)
        {
            this.value = value;
        }

        /**
         * Get the value of db.
         * The DB adaptor which might be used to get db specific
         * variations of sql.
         * @return value of db.
         */
        public DB getDb()
        {
            DB db = null;
            if (this.db == null)
            {
                // db may not be set if generating preliminary sql for
                // debugging.
                try
                {
                    db = Torque.getDB(getDbName());
                }
                catch (Exception e)
                {
                    // we are only doing this to allow easier debugging, so
                    // no need to throw up the exception, just make note of it.
                    log.error(
                            "Could not get a DB adapter, so sql may be wrong");
                }
            }
            else
            {
                db = this.db;
            }

            return db;
        }

        /**
         * Set the value of db.
         * The DB adaptor might be used to get db specific
         * variations of sql.
         * @param v  Value to assign to db.
         */
        public void setDB(DB v)
        {
            this.db = v;

            for (int i = 0; i < this.clauses.size(); i++)
            {
                ((Criterion) (clauses.get(i))).setDB(v);
            }
        }

        /**
         * Sets ignore case.
         *
         * @param b True if case should be ignored.
         * @return A modified Criteria object.
         */
        public Criterion setIgnoreCase(boolean b)
        {
            ignoreStringCase = b;
            return this;
        }

        /**
         * Is ignore case on or off?
         *
         * @return True if case is ignored.
         */
        public boolean isIgnoreCase()
        {
            return ignoreStringCase;
        }

        /**
         *  get the list of clauses in this Criterion
         */
        private List getClauses()
        {
            return clauses;
        }

        /**
         *  get the list of conjunctions in this Criterion
         */
        private List getConjunctions()
        {
            return conjunctions;
        }

        /**
         * Append an AND Criterion onto this Criterion's list.
         */
        public Criterion and(Criterion criterion)
        {
            this.clauses.add(criterion);
            this.conjunctions.add(AND);
            return this;
        }

        /**
         * Append an OR Criterion onto this Criterion's list.
         */
        public Criterion or(Criterion criterion)
        {
            this.clauses.add(criterion);
            this.conjunctions.add(OR);
            return this;
        }

        /**
         * Appends a representation of the Criterion onto the buffer.
         */
        public void appendTo(StringBuffer sb) throws TorqueException
        {
            //
            // it is alright if value == null
            //

            if (column == null)
            {
                return;
            }

            Criterion clause = null;
            for (int j = 0; j < this.clauses.size(); j++)
            {
                sb.append('(');
            }
            if (CUSTOM == comparison)
            {
                if (value != null && !"".equals(value))
                {
                    sb.append((String) value);
                }
            }
            else
            {
                String field = null;
                if  (table == null)
                {
                    field = column;
                }
                else
                {
                    field = new StringBuffer(
                            table.length() + 1 + column.length())
                            .append(table).append('.').append(column)
                            .toString();
                }
                SqlExpression.build(field, value, comparison,
                        ignoreStringCase || ignoreCase, getDb(), sb);
            }

            for (int i = 0; i < this.clauses.size(); i++)
            {
                sb.append(this.conjunctions.get(i));
                clause = (Criterion) (this.clauses.get(i));
                clause.appendTo(sb);
                sb.append(')');
            }
        }

        /**
         * Appends a Prepared Statement representation of the Criterion
         * onto the buffer.
         *
         * @param sb The stringbuffer that will receive the Prepared Statement
         * @param params A list to which Prepared Statement parameters
         * will be appended
         */
        public void appendPsTo(StringBuffer sb, List params)
        {
            if (column == null || value == null)
            {
                return;
            }

            DB db = getDb();

            for (int j = 0; j < this.clauses.size(); j++)
            {
                sb.append('(');
            }
            if (CUSTOM == comparison)
            {
                if (!"".equals(value))
                {
                    sb.append((String) value);
                }
            }
            else
            {
                String field = null;
                if (table == null)
                {
                    field = column;
                }
                else
                {
                    field = new StringBuffer(
                            table.length() + 1 + column.length())
                            .append(table).append('.').append(column)
                            .toString();
                }

                if (comparison.equals(Criteria.IN)
                        || comparison.equals(Criteria.NOT_IN))
                {
                    sb.append(field)
                            .append(comparison);

                    UniqueList inClause = new UniqueList();

                    if (value instanceof List)
                    {
                        value = ((List) value).toArray (new Object[0]);
                    }

                    for (int i = 0; i < Array.getLength(value); i++)
                    {
                        Object item = Array.get(value, i);

                        inClause.add(SqlExpression.processInValue(item,
                                             ignoreStringCase || ignoreCase,
                                             db));
                    }

                    StringBuffer inString = new StringBuffer();
                    inString.append('(').append(StringUtils.join(
                                                        inClause.iterator(), (","))).append(')');
                    sb.append(inString.toString());
                }
                else
                {
                    if (ignoreStringCase || ignoreCase)
                    {
                        sb.append(db.ignoreCase(field))
                                .append(comparison)
                                .append(db.ignoreCase("?"));
                    }
                    else
                    {
                        sb.append(field)
                                .append(comparison)
                                .append(" ? ");
                    }

                    if (value instanceof java.util.Date)
                    {
                        params.add(new java.sql.Date(
                                           ((java.util.Date) value).getTime()));
                    }
                    else if (value instanceof DateKey)
                    {
                        params.add(new java.sql.Date(
                                           ((DateKey) value).getDate().getTime()));
                    }
                    else if (value instanceof Integer)
                    {
                        params.add(value);
                    }
                    else
                    {
                        params.add(value.toString());
                    }
                }
            }

            for (int i = 0; i < this.clauses.size(); i++)
            {
                sb.append(this.conjunctions.get(i));
                Criterion clause = (Criterion) (this.clauses.get(i));
                clause.appendPsTo(sb, params);
                sb.append(')');
            }
        }

        /**
         * Build a string representation of the Criterion.
         *
         * @return A String with the representation of the Criterion.
         */
        public String toString()
        {
            //
            // it is alright if value == null
            //
            if (column == null)
            {
                return "";
            }

            StringBuffer expr = new StringBuffer(25);
            try
            {
                appendTo(expr);
            }
            catch (TorqueException e)
            {
                return "Criterion cannot be evaluated";
            }
            return expr.toString();
        }

        /**
         * This method checks another Criteria.Criterion to see if they contain
         * the same attributes and hashtable entries.
         */
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }

            if ((obj == null) || !(obj instanceof Criterion))
            {
                return false;
            }

            Criterion crit = (Criterion) obj;

            boolean isEquiv = ((table == null && crit.getTable() == null)
                    || (table != null && table.equals(crit.getTable()))
                               )
                    && column.equals(crit.getColumn())
                    && comparison.equals(crit.getComparison());

            // we need to check for value equality
            if (isEquiv)
            {
                Object b = crit.getValue();
                if (value instanceof Object[] && b instanceof Object[])
                {
                    isEquiv &= Arrays.equals((Object[]) value, (Object[]) b);
                }
                else if (value instanceof int[] && b instanceof int[])
                {
                    isEquiv &= Arrays.equals((int[]) value, (int[]) b);
                }
                else
                {
                    isEquiv &= value.equals(b);
                }
            }

            // check chained criterion

            isEquiv &= this.clauses.size() == crit.getClauses().size();
            for (int i = 0; i < this.clauses.size(); i++)
            {
                isEquiv &=  ((String) (conjunctions.get(i)))
                        .equals((String) (crit.getConjunctions().get(i)));
                isEquiv &=  ((Criterion) (clauses.get(i)))
                        .equals((Criterion) (crit.getClauses().get(i)));
            }

            return isEquiv;
        }

        /**
         * Returns a hash code value for the object.
         */
        public int hashCode()
        {
            int h = value.hashCode() ^ comparison.hashCode();

            if (table != null)
            {
                h ^= table.hashCode();
            }

            if (column != null)
            {
                h ^= column.hashCode();
            }

            for (int i = 0; i < this.clauses.size(); i++)
            {
                h ^= ((Criterion) (clauses.get(i))).hashCode();
            }

            return h;
        }

        /**
         * get all tables from nested criterion objects
         *
         * @return the list of tables
         */
        public List getAllTables()
        {
            UniqueList tables = new UniqueList();
            addCriterionTable(this, tables);
            return tables;
        }

        /**
         * method supporting recursion through all criterions to give
         * us a StringStack of tables from each criterion
         */
        private void addCriterionTable(Criterion c, UniqueList s)
        {
            if (c != null)
            {
                s.add(c.getTable());
                for (int i = 0; i < c.getClauses().size(); i++)
                {
                    addCriterionTable((Criterion) (c.getClauses().get(i)), s);
                }
            }
        }

        /**
         * get an array of all criterion attached to this
         * recursing through all sub criterion
         */
        public Criterion[] getAttachedCriterion()
        {
            ArrayList crits = new ArrayList();
            traverseCriterion(this, crits);
            Criterion[] crita = new Criterion[crits.size()];
            for (int i = 0; i < crits.size(); i++)
            {
                crita[i] = (Criterion) crits.get(i);
            }

            return crita;
        }

        /**
         * method supporting recursion through all criterions to give
         * us an ArrayList of them
         */
        private void traverseCriterion(Criterion c, ArrayList a)
        {
            if (c != null)
            {
                a.add(c);
                for (int i = 0; i < c.getClauses().size(); i++)
                {
                    traverseCriterion((Criterion) (c.getClauses().get(i)), a);
                }
            }
        }
    } // end of inner class Criterion

    /**
     * Data object to describe a join between two tables, for example
     * <pre>
     * table_a LEFT JOIN table_b ON table_a.id = table_b.a_id
     * </pre>
     * The class is immutable. Because the class is also used by
     * {@link org.apache.torque.util.BasePeer}, it is visible from the package.
     */
    public static class Join
    {
        /** the left column of the join condition */
        private String leftColumn = null;

        /** the right column of the join condition */
        private String rightColumn = null;

        /** the type of the join (LEFT JOIN, ...), or null */
        private SqlEnum joinType = null;

        /**
         * Constructor
         * @param leftColumn the left column of the join condition;
         *        might contain an alias name
         * @param rightColumn the right column of the join condition
         *        might contain an alias name
         * @param joinType the type of the join. Valid join types are
         *        null (adding the join condition to the where clause),
         *        SqlEnum.LEFT_JOIN, SqlEnum.RIGHT_JOIN, and SqlEnum.INNER_JOIN
         */
        public Join(
                final String leftColumn,
                final String rightColumn,
                final SqlEnum joinType)
        {
            this.leftColumn = leftColumn;
            this.rightColumn = rightColumn;
            this.joinType = joinType;
        }

        /**
         * @return the type of the join, i.e. SqlEnum.LEFT_JOIN, ...,
         *         or null for adding the join condition to the where Clause
         */
        public final SqlEnum getJoinType()
        {
            return joinType;
        }

        /**
         * @return the left column of the join condition
         */
        public final String getLeftColumn()
        {
            return leftColumn;
        }

        /**
         * @return the right column of the join condition
         */
        public final String getRightColumn()
        {
            return rightColumn;
        }

        /**
         * returns a String representation of the class,
         * mainly for debuggung purposes
         * @return a String representation of the class
         */
        public String toString()
        {
            StringBuffer result = new StringBuffer();
            if (joinType != null)
            {
                result.append(joinType)
                        .append(" : ");
            }
            result.append(leftColumn)
                    .append("=")
                    .append(rightColumn)
                    .append(" (ignoreCase not considered)");

            return result.toString();
        }

        /**
         * This method checks another Criteria.Join to see if they contain the
         * same attributes.
         */
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }

            if ((obj == null) || !(obj instanceof Join))
            {
                return false;
            }

            Join join = (Join) obj;

            return ObjectUtils.equals(leftColumn, join.getLeftColumn())
                    && ObjectUtils.equals(rightColumn, join.getRightColumn())
                    && ObjectUtils.equals(joinType, join.getJoinType());
        }

        /**
         * Returns the hash code value for this Join.
         *
         * @return a hash code value for this object.
         */
        public int hashCode()
        {
            int result = 13;
            result = 37 * result + leftColumn.hashCode();
            result = 37 * result + rightColumn.hashCode();
            result = 37 * result + (null == joinType ? 0 : joinType.hashCode());
            return result;
        }

    } // end of inner class Join
}
