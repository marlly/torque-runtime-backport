package org.apache.torque.engine.database.model;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import org.xml.sax.Attributes;

/**
 * A class for information about foreign keys of a table.
 *
 * @author <a href="mailto:fedor.karpelevitch@home.com">Fedor</a>
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @version $Id$
 */
public class ForeignKey
{
    private String foreignTableName;
    private String name;
    private String onUpdate;
    private String onDelete;
    private Table parentTable;
    private List localColumns = new ArrayList(3);
    private List foreignColumns = new ArrayList(3);

    // the uppercase equivalent of the onDelete/onUpdate values in the dtd
    private static final String NONE    = "NONE";
    private static final String SETNULL = "SETNULL";

    /**
     * Imports foreign key from an XML specification
     *
     * @param attrib the xml attributes
     */
    public void loadFromXML(Attributes attrib)
    {
        foreignTableName = attrib.getValue("foreignTable");
        name = attrib.getValue("name");
        onUpdate = attrib.getValue("onUpdate");
        onDelete = attrib.getValue("onDelete");
        onUpdate = normalizeFKey(onUpdate);
        onDelete = normalizeFKey(onDelete);
    }

    /**
     * Normalizes the input of onDelete, onUpdate attributes
     *
     * @param attrib the attribute to normalize
     * @return nomalized form
     */
    private String normalizeFKey(String attrib)
    {
        if (attrib == null)
        {
            attrib = NONE;
        }

        attrib = attrib.toUpperCase();
        if (attrib.equals(SETNULL))
        {
            attrib = "SET NULL";
        }
        return attrib;
    }

    /**
     * Returns whether or not the onUpdate attribute is set
     *
     * @return true if the onUpdate attribute is set
     */
    public boolean hasOnUpdate()
    {
       return !onUpdate.equals(NONE);
    }

    /**
     * Returns whether or not the onDelete attribute is set
     *
     * @return true if the onDelete attribute is set
     */
    public boolean hasOnDelete()
    {
       return !onDelete.equals(NONE);
    }

    /**
     * Returns the onUpdate attribute
     *
     * @return the onUpdate attribute
     */
    public String getOnUpdate()
    {
       return onUpdate;
    }

    /**
     * Returns the onDelete attribute
     *
     * @return the onDelete attribute
     */
    public String getOnDelete()
    {
       return onDelete;
    }

    /**
     * Sets the onDelete attribute
     *
     * @param value the onDelete attribute
     */
    public void setOnDelete(String value)
    {
       onDelete = normalizeFKey(value);
    }

    /**
     * Sets the onUpdate attribute
     *
     * @param value the onUpdate attribute
     */
    public void setOnUpdate(String value)
    {
       onUpdate = normalizeFKey(value);
    }

    /**
     * Returns the name attribute.
     *
     * @return the name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the name attribute.
     *
     * @param name the name
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Get the foreignTableName of the FK
     *
     * @return the name of the foreign table
     */
    public String getForeignTableName()
    {
        return foreignTableName;
    }

    /**
     * Set the foreignTableName of the FK
     *
     * @param tableName the name of the foreign table
     */
    public void setForeignTableName(String tableName)
    {
        foreignTableName = tableName;
    }

    /**
     * Set the parent Table of the foreign key
     *
     * @param parent the table
     */
    public void setTable(Table parent)
    {
        parentTable = parent;
    }

    /**
     * Get the parent Table of the foreign key
     *
     * @return the parent table
     */
    public Table getTable()
    {
        return parentTable;
    }

    /**
     * Returns the name of the table the foreign key is in
     *
     * @return the name of the table
     */
    public String getTableName()
    {
        return parentTable.getName();
    }

    /**
     * Adds a new reference entry to the foreign key
     *
     * @param attrib the xml attributes
     */
    public void addReference(Attributes attrib)
    {
        addReference(attrib.getValue("local"), attrib.getValue("foreign"));
    }

    /**
     * Adds a new reference entry to the foreign key
     *
     * @param local name of the local column
     * @param foreign name of the foreign column
     */
    public void addReference(String local, String foreign)
    {
        localColumns.add(local);
        foreignColumns.add(foreign);
    }

    /**
     * Returns a comma delimited string of local column names
     *
     * @return the local column names
     */
    public String getLocalColumnNames()
    {
        return Column.makeList(getLocalColumns());
    }

    /**
     * Returns a comma delimited string of foreign column names
     *
     * @return the foreign column names
     */
    public String getForeignColumnNames()
    {
        return Column.makeList(getForeignColumns());
    }

    /**
     * Returns the list of local column names. You should not edit this List.
     *
     * @return the local columns
     */
    public List getLocalColumns()
    {
        return localColumns;
    }

    /**
     * Utility method to get local column names to foreign column names
     * mapping for this foreign key.
     *
     * @return table mapping foreign names to local names
     */
    public Hashtable getLocalForeignMapping()
    {
        Hashtable h = new Hashtable();

        for (int i = 0; i < localColumns.size(); i++)
        {
            h.put(localColumns.get(i), foreignColumns.get(i));
        }

        return h;
    }

    /**
     * Returns the list of foreign column names. You should not edit this List.
     *
     * @return the foreign columns
     */
    public List getForeignColumns()
    {
        return foreignColumns;
    }

    /**
     * Utility method to get foreign column names to local column names
     * mapping for this foreign key.
     *
     * @return table mapping local names to foreign names
     */
    public Hashtable getForeignLocalMapping()
    {
        Hashtable h = new Hashtable();

        for (int i = 0; i < localColumns.size(); i++)
        {
            h.put (foreignColumns.get(i), localColumns.get(i));
        }

        return h;
    }

    /**
     * String representation of the foreign key. This is an xml representation.
     *
     * @return string representation in xml
     */
    public String toString()
    {
        StringBuffer result = new StringBuffer();
        result.append("    <foreign-key foreignTable=\"")
            .append(getForeignTableName())
            .append("\" name=\"")
            .append(getName())
            .append("\">\n");

        for (int i = 0; i < localColumns.size(); i++)
        {
            result.append("        <reference local=\"")
                .append(localColumns.get(i))
                .append("\" foreign=\"")
                .append(foreignColumns.get(i))
                .append("\"/>\n");
        }
        result.append("    </foreign-key>\n");
        return result.toString();
    }
}
