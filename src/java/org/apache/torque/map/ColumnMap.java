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

import org.apache.commons.collections.map.ListOrderedMap;

/**
 * ColumnMap is used to model a column of a table in a database.
 * <p>
 * Note that this information should be set via the <Table>MapBuilder class and
 * not changed by applications. The set methods are only public because this
 * class needs them.
 *
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @author <a href="mailto:greg.monroe@dukece.com">Greg Monroe</a>
 * @version $Id$
 */
public class ColumnMap implements java.io.Serializable
{
    /** The serialVersionUID for this class. */
    private static final long serialVersionUID = -5971184507395399165L;

    /** A sample object having the same java Type as the column. */
    private Object type = null;

    /** The name of the Torque Type of the column. */
    private String torqueType = null;

    /** Should object type be converted to primitive. */
    private boolean usePrimitive = true;

    /** Size of the column. */
    private int size = 0;

    /** Scale of the column */
    private int scale = 0;

    /** Is it a primary key? */
    private boolean pk = false;

    /** Is null value allowed ? */
    private boolean notNull = false;

    /** Name of the table that this column is related to. */
    private String relatedTableName = "";

    /** Name of the column that this column is related to. */
    private String relatedColumnName = "";

    /** The TableMap for this column. */
    private TableMap table;

    /** The name of the column. */
    private String columnName;

    /**
     * The Java Name of this column as defined in XML or created by the
     * generator code.
     */
    private String javaName;

    /** Is this column an autoincrement column ? */
    private boolean autoIncrement = false;

    /** Column description info (if any). */
    private String description = "";

    /** is Column protected ? */
    private boolean isProtected = false;

    /**
     * String representing the default value defined for field. Note that
     * default is a keyword, so defaultValue is used to store the value for the
     * get/setDefault() methods.
     */
    private String defaultValue = null;

    /** Inheritance type used. */
    private String inheritance = "false";

    /**
     * Does column uses Inheritance subclasses? Note that this is tied to the
     * TableMap useInheritance thru the set function.
     */
    private boolean useInheritance;

    /** Associated of inheritance maps. */
    private Map inheritanceMaps = Collections
            .synchronizedMap(new ListOrderedMap());

    /** Input validator class name. (in DTD but not used?) */
    private String inputValidator;

    /** Java naming method the generator used. */
    private String javaNamingMethod;

    /** Java type string specified in XML. */
    private String javaType;

    /** Column position in the table (one based). */
    private int position = -1;

    /**
     * Constructor.
     *
     * @param name The name of the column.
     * @param containingTable TableMap of the table this column is in.
     */
    public ColumnMap(String name, TableMap containingTable)
    {
        table = containingTable;
        this.columnName = normalizeName(name);
    }

    /**
     * Makes sure that the column names don't include table prefixes. E.g.,
     * SCARAB_PROJECT.PROJECT_ID should be PROJECT_ID.
     *
     * @param name The name to check
     * @return The corrected name if needed or the same name if not.
     */
    protected String normalizeName(String name)
    {
        if (name.indexOf('.') > 0)
        {
            return name.substring(name.lastIndexOf('.') + 1);
        }
        return name;
    }

    /**
     * Get the name of a column.
     *
     * @return A String with the column name.
     */
    public String getColumnName()
    {
        return columnName;
    }

    /**
     * Get the table name + column name.
     *
     * @return A String with the full column name.
     */
    public String getFullyQualifiedName()
    {
        return table.getName() + "." + columnName;
    }

    /**
     * Get the name of the table this column is in.
     *
     * @return A String with the table name.
     */
    public String getTableName()
    {
        return table.getName();
    }

    /**
     * Set the type of this column.
     *
     * @param type An Object specifying the type.
     */
    public void setType(Object type)
    {
        this.type = type;
    }

    /**
     * Set the Torque type of this column.
     *
     * @param torqueType the Torque type of the column.
     */
    public void setTorqueType(String torqueType)
    {
        this.torqueType = torqueType;
    }

    /**
     * Set the size of this column.
     *
     * @param size An int specifying the size.
     */
    public void setSize(int size)
    {
        this.size = size;
    }

    /**
     * Set if this column is a primary key or not.
     *
     * @param pk True if column is a primary key.
     */
    public void setPrimaryKey(boolean pk)
    {
        this.pk = pk;
    }

    /**
     * Set if this column may be null.
     *
     * @param nn True if column may be null.
     */
    public void setNotNull(boolean nn)
    {
        this.notNull = nn;
    }

    /**
     * Set the foreign key for this column.
     *
     * @param fullyQualifiedName The name of the table.column that is
     * foreign.
     */
    public void setForeignKey(String fullyQualifiedName)
    {
        if (fullyQualifiedName != null && fullyQualifiedName.length() > 0)
        {
            relatedTableName = fullyQualifiedName.substring(
                    0, fullyQualifiedName.indexOf('.'));
            relatedColumnName = fullyQualifiedName.substring(
                    fullyQualifiedName.indexOf('.') + 1);
        }
        else
        {
            relatedTableName = "";
            relatedColumnName = "";
        }
    }

    /**
     * Set the foreign key for this column.
     *
     * @param tableName The name of the table that is foreign.
     * @param columnName The name of the column that is foreign.
     */
    public void setForeignKey(String tableName, String columnName)
    {
        if (tableName != null && tableName.length() > 0 && columnName != null
                && columnName.length() > 0)
        {
            relatedTableName = tableName;
            relatedColumnName = normalizeName(columnName);
        }
        else
        {
            relatedTableName = "";
            relatedColumnName = "";
        }
    }

    /**
     * Get the type of this column. Note that if usePrimitive is true, this may
     * need to be converted.
     *
     * @return An Object specifying the type.
     */
    public Object getType()
    {
        return type;
    }

    /**
     * Get the name of the Torque type of this column.
     *
     * @return The name of the Torque type of this column.
     */
    public String getTorqueType()
    {
        return torqueType;
    }

    /**
     * The "precision" value from the XML
     * size="&lt;precision&gt;[,&lt;scale&gt;]"
     * attribute. Where [,&lt;scale&gt;] is optional.
     *
     * If the size attribute has not been set in the XML, it will return 0.
     * <p>
     *
     * Note that the size="P,S" format should be replaced with size="P"
     * scale="S".
     *
     * @return An int specifying the size.
     */
    public int getSize()
    {
        return size;
    }

    /**
     * Is this column a primary key?
     *
     * @return True if column is a primary key.
     */
    public boolean isPrimaryKey()
    {
        return pk;
    }

    /**
     * Is null value allowed ?
     *
     * @return True if column may be null.
     */
    public boolean isNotNull()
    {
        return (notNull || isPrimaryKey());
    }

    /**
     * Is this column a foreign key?
     *
     * @return True if column is a foreign key.
     */
    public boolean isForeignKey()
    {
        return (relatedTableName != null && relatedTableName.length() > 0);
    }

    /**
     * Get the table.column that this column is related to.
     *
     * @return A String with the full name for the related column.
     */
    public String getRelatedName()
    {
        return relatedTableName + "." + relatedColumnName;
    }

    /**
     * Get the table name that this column is related to.
     *
     * @return A String with the name for the related table.
     */
    public String getRelatedTableName()
    {
        return relatedTableName;
    }

    /**
     * Get the column name that this column is related to.
     *
     * @return A String with the name for the related column.
     */
    public String getRelatedColumnName()
    {
        return relatedColumnName;
    }

    /**
     * Gets the scale set for this column (if any) as set in the XML database
     * definition. E.g., the value of the scale attribute or the scale portion
     * of a size="P,S" attribute. (Note: size="P,S" format is being
     * deprecated!).
     *
     * @return Returns the scale.
     */
    public int getScale()
    {
        return scale;
    }

    /**
     * @param scale The scale to set.
     */
    public void setScale(int scale)
    {
        this.scale = scale;
    }

    /**
     * Gets the Java Name for this column as defined in XML or created by
     * generator code.
     *
     * @return the Java Name.
     */
    public String getJavaName()
    {
        return this.javaName;
    }

    /**
     * Sets the Java Name for this column.
     *
     * @param name the Java Name.
     */
    public void setJavaName(String name)
    {
        this.javaName = name;
    }

    /**
     * Returns whether this column is an autoincrement column.
     *
     * @return true if this column is an autoIncrement column, false otherwise.
     */
    public boolean isAutoIncrement()
    {
        return autoIncrement;
    }

    /**
     * Sets whether this column is an autoincrement column.
     *
     * @param autoIncrement whether this colimn is an autoincrement column.
     */
    public void setAutoIncrement(boolean autoIncrement)
    {
        this.autoIncrement = autoIncrement;
    }

    /**
     * A string representing the default value defined for this column.
     *
     * @return The default value of this column, if any.
     */
    public String getDefault()
    {
        return defaultValue;
    }

    /**
     * Sets the default value for this column.
     *
     * @param defaultValue The defaultValue to set.
     */
    public void setDefault(String defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    /**
     * Returns the column description info.
     *
     * @return the description, if any.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Sets the description for this column.
     *
     * @param description The description to set.
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * Get the inheritance information associated with this column,
     *
     * @return Returns an array of associated inheritanceMap.
     *         The array is in XML order.
     */
    public InheritanceMap[] getInheritanceMaps()
    {
        InheritanceMap[] iMaps = new InheritanceMap[inheritanceMaps.size()];
        synchronized (inheritanceMaps)
        {
            Iterator it = inheritanceMaps.values().iterator();
            int i = 0;
            while (it.hasNext())
            {
                iMaps[i++] = (InheritanceMap) it.next();
            }
        }
        return iMaps;
    }

    /**
     * Add an associated inheritance mapping.
     *
     * @param map The inheritanceMap to associate with this column.
     */
    public void addInheritanceMap(InheritanceMap map)
    {
        setUseInheritance(true);
        this.inheritanceMaps.put(map.getKey(), map);
    }

    /**
     * Gets the inheritance type used.
     *
     * @return the inheritance type used.
     */
    public String getInheritance()
    {
        return inheritance;
    }

    /**
     * Sets the inheritance type.
     *
     * @param inheritanceType The inheritance type to set.
     */
    public void setInheritance(String inheritanceType)
    {
        this.inheritance = inheritanceType;
    }

    /**
     * Returns the input validator class name.
     * (This property is in the DTD, but currently not used by Torque?)
     *
     * @return Returns the inputValidator.
     */
    public String getInputValidator()
    {
        return inputValidator;
    }

    /**
     * Sets the input validator class name.
     *
     * @param inputValidator The inputValidator to set.
     */
    public void setInputValidator(String inputValidator)
    {
        this.inputValidator = inputValidator;
    }

    /**
     * Returns whether getters and setters are generated with the
     * access modifier "protected" rather than "public".
     *
     * @return whether the accessors should be protected rather than public.
     */
    public boolean isProtected()
    {
        return isProtected;
    }

    /**
     * Sets whether getters and setters should be generated with the
     * access modifier "protected" rather than "public".
     *
     * @param isProtected whether getters and setters for this column
     *        are protected.
     */
    public void setProtected(boolean isProtected)
    {
        this.isProtected = isProtected;
    }

    /**
     * Returns whether this column is a primary key.
     *
     * @return whether this column is a primary key.
     */
    public boolean isPk()
    {
        return pk;
    }

    /**
     * Sets whether this column is a primary key.
     *
     * @param pk whether this column is a primary key.
     */
    public void setPk(boolean pk)
    {
        this.pk = pk;
    }

    /**
     * Returns whether this column uses inheritance subclasses.
     *
     * @return true if inheritance subclasses are used, false otherwise.
     */
    public boolean isUseInheritance()
    {
        return useInheritance;
    }

    /**
     * Sets whether this column uses inheritance subclasses.
     *
     * @param useInheritance whether this column uses Inheritance subclasses.
     */
    public void setUseInheritance(boolean useInheritance)
    {
        this.useInheritance = useInheritance;
    }

    /**
     * Get the inheritance map with the specified key.
     *
     * @param key the key of the inheritance map.
     * @return the inheritance map with the specified key, or null if no
     *         inheritance map with the specified key exists in this column.
     */
    public InheritanceMap getInheritanceMap(String key)
    {
        return (InheritanceMap) inheritanceMaps.get(key);
    }

    /**
     * Returns whether this colum uses primitive values rather than objects.
     *
     * @return true if this colum uses primitive values, false if it uses
     *         objects.
     */
    public boolean isUsePrimitive()
    {
        return usePrimitive;
    }

    /**
     * Sets whether this colum uses primitive values rather than objects.
     *
     * @param usePrimitive whether primitive objects are used
     *        rather than objects.
     */
    public void setUsePrimitive(boolean usePrimitive)
    {
        this.usePrimitive = usePrimitive;
    }

    /**
     * Returns the Java naming method for this column.
     *
     * @return the javaNamingMethod for this column.
     */
    public String getJavaNamingMethod()
    {
        return javaNamingMethod;
    }

    /**
     * Sets the java naming method for this column.
     *
     * @param javaNamingMethod The javaNamingMethod to set.
     */
    public void setJavaNamingMethod(String javaNamingMethod)
    {
        this.javaNamingMethod = javaNamingMethod;
    }

    /**
     * Returns the map for the table this column belongs to.
     *
     * @return the table map for this column.
     */
    public TableMap getTable()
    {
        return table;
    }

    /**
     * Returns the position (one based) of this column in the table.
     * XML order is preserved.
     *
     * @return The position of this column, one-based.
     */
    public int getPosition()
    {
        return position;
    }

    /**
     * Sets the position (one based) of this column in the table.
     *
     * @param position The position to set.
     */
    public void setPosition(int position)
    {
        this.position = position;
    }

    /**
     * Returns the java type of this column.
     *
     * @return the javaType.
     */
    public String getJavaType()
    {
        return javaType;
    }

    /**
     * Sets the java type of this column.
     *
     * @param javaType The javaType to set.
     */
    public void setJavaType(String javaType)
    {
        this.javaType = javaType;
    }
}
