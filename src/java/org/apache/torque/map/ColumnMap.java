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

/**
 * ColumnMap is used to model a column of a table in a database.
 *
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @version $Id$
 */
public class ColumnMap implements java.io.Serializable
{
    /** Type of the column. */
    private Object type = null;

    /** Size of the column. */
    private int size = 0;

    /** Is it a primary key? */
    private boolean pk = false;

    /** Is null value allowed ?*/
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
     * Constructor.
     *
     * @param name The name of the column.
     * @param containingTable TableMap of the table this column is in.
     */
    public ColumnMap(String name, TableMap containingTable)
    {
        this.columnName = name;
        table = containingTable;
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
    public void setType (Object type)
    {
        this.type = type;
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
            relatedColumnName = columnName;
        }
        else
        {
            relatedTableName = "";
            relatedColumnName = "";
        }
    }

    /**
     * Get the type of this column.
     *
     * @return An Object specifying the type.
     */
    public Object getType()
    {
        return type;
    }

    /**
     * Get the size of this column.
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
}
