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
 * A Class for information about foreign keys of a table
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
    private final String NONE    = "NONE";
    private final String SETNULL = "SETNULL";

    /**
     * Default Constructor
     */
    public ForeignKey()
    {
    }

    /**
     * Imports foreign key from an XML specification
     */
    public void loadFromXML (Attributes attrib)
    {
        foreignTableName = attrib.getValue("foreignTable");
        name = attrib.getValue("name");
        onUpdate = attrib.getValue("onUpdate");
        onDelete = attrib.getValue("onDelete");
        onUpdate = normalizeFKey(onUpdate);
        onDelete = normalizeFKey(onDelete);
    }

    /**
     * normailizes the input of onDelete, onUpdate
     * attributes
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
            attrib =  "SET NULL";
        }
        return attrib;
    }

    /**
     * returns whether or not the onUpdate attribute is set
     */
    public boolean hasOnUpdate()
    {
       return ! onUpdate.equals(NONE);
    }

    /**
     * returns whether or not the onDelete attribute is set
     */
    public boolean hasOnDelete()
    {
       return ! onDelete.equals(NONE);
    }

    /**
     * returns the onUpdate attribute
     */
    public String getOnUpdate()
    {
       return onUpdate;
    }

    /**
     * returns the onDelete attribute
     */
    public String getOnDelete()
    {
       return onDelete;
    }

    /**
     * sets the onDelete attribute
     */
    public void setOnDelete(String value)
    {
       onDelete = normalizeFKey(value);
    }

    /**
     * sets the onUpdate attribute
     */
    public void setOnUpdate(String value)
    {
       onUpdate = normalizeFKey(value);
    }

    /**
     * Returns the name attribute.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the name attribute.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Get the foreignTableName of the FK
     */
    public String getForeignTableName()
    {
        return foreignTableName;
    }

    /**
     * Set the foreignTableName of the FK
     */
    public void setForeignTableName(String tableName)
    {
        foreignTableName = tableName;
    }

    /**
     * Set the parent Table of the foreign key
     */
    public void setTable(Table parent)
    {
        parentTable = parent;
    }

    /**
     * Get the parent Table of the foreign key
     */
    public Table getTable()
    {
        return parentTable;
    }

    /**
     * Returns the Name of the table the foreign key is in
     */
    public String getTableName()
    {
        return parentTable.getName();
    }

    /**
     *  adds a new reference entry to the foreign key
     */
    public void addReference(Attributes attrib)
    {
        addReference(attrib.getValue("local"), attrib.getValue("foreign"));
    }

    /**
     *  adds a new reference entry to the foreign key
     */
    public void addReference(String local, String foreign)
    {
        localColumns.add(local);
        foreignColumns.add(foreign);
    }

    /**
     * Return a comma delimited string of local column names
     */
    public String getLocalColumnNames()
    {
        return Column.makeList(getLocalColumns());
    }

    /**
     * Return a comma delimited string of foreign column names
     */
    public String getForeignColumnNames()
    {
        return Column.makeList(getForeignColumns());
    }

    /**
     * Return the vector of local columns.  You should not edit
     * this vector.
     */
    public List getLocalColumns()
    {
        return localColumns;
    }

    /**
     * Utility method to get local column to foreign column
     * mapping for this foreign key.
     */
    public Hashtable getLocalForeignMapping()
    {
        Hashtable h = new Hashtable();

        for (int i=0; i<localColumns.size(); i++)
        {
            h.put (localColumns.get(i),foreignColumns.get(i));
        }

        return h;
    }

    /**
     * Return the vector of local columns.  You should not edit
     * this vector.
     */
    public List getForeignColumns()
    {
        return foreignColumns;
    }

    /**
     * Utility method to get local column to foreign column
     * mapping for this foreign key.
     */
    public Hashtable getForeignLocalMapping()
    {
        Hashtable h = new Hashtable();

        for (int i = 0; i < localColumns.size(); i++)
        {
            h.put (foreignColumns.get(i),localColumns.get(i));
        }

        return h;
    }

    /**
     * String representation of the foreign key. This
     * is an xml representation.
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
