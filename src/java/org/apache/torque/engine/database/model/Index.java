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
import java.util.Iterator;
import java.util.List;
import org.apache.torque.engine.EngineException;
import org.xml.sax.Attributes;

/**
 * Information about indices of a table.
 *
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:dlr@finemaltcoding.com>Daniel Rall</a>
 * @version $Id$
 */
public class Index
{
    /** enables debug output */
    private static final boolean DEBUG = false;
    /** name of the index */
    private String indexName;
    /** table */
    private Table parentTable;
    /** columns */
    private List indexColumns;

    /**
     * Creates a new instance with default characteristics (no name or
     * parent table, small column list size allocation, non-unique).
     */
    public Index()
    {
        indexColumns = new ArrayList(3);
    }

    /**
     * Creates a new instance for the list of columns composing an
     * index.  Otherwise performs as {@link #Index()}.
     *
     * @param table The table this index is associated with.
     * @param indexColumns The list of {@link
     * org.apache.torque.engine.database.model.Column} objects which
     * make up this index.  Cannot be empty.
     * @exception EngineException Error generating name.
     * @see #Index()
     */
    protected Index(Table table, List indexColumns)
        throws EngineException
    {
        this();
        setTable(table);
        if (indexColumns.size() > 0)
        {
            this.indexColumns = indexColumns;
            createName();

            if (DEBUG)
            {
                System.out.println("Created Index named " + getName()
                        + " with " + indexColumns.size() + " columns");
            }
        }
        else
        {
            throw new EngineException("Cannot create a new Index using an "
                    + "empty list Column object");
        }
    }

    /**
     * Creates a name for the index using the NameFactory.
     *
     * @throws EngineException if the name could not be created
     */
    private void createName() throws EngineException
    {
        Table table = getTable();
        List inputs = new ArrayList(4);
        inputs.add(table.getDatabase());
        inputs.add(table.getName());
        if (isUnique())
        {
            inputs.add("U");
        }
        else
        {
            inputs.add("I");
        }
        // ASSUMPTION: This Index not yet added to the list.
        inputs.add(new Integer(table.getIndices().length + 1));
        indexName = NameFactory.generateName(
                NameFactory.CONSTRAINT_GENERATOR, inputs);
    }

    /**
     * Imports index from an XML specification
     *
     * @param attrib the xml attributes
     */
    public void loadFromXML(Attributes attrib)
    {
        indexName = attrib.getValue("name");
    }

    /**
     * Returns the uniqueness of this index.
     *
     * @return the uniqueness of this index
     */
    public boolean isUnique()
    {
        return false;
    }

    /**
     * Gets the name of this index.
     *
     * @return the name of this index
     */
    public String getName()
    {
        if (indexName == null)
        {
            try
            {
                // generate an index name if we don't have a supplied one
                createName();
            }
            catch (EngineException e)
            {
                // still no name
            }
        }
        return indexName;
    }

    /**
     * Set the name of this index.
     *
     * @param name the name of this index
     */
    public void setName(String name)
    {
        this.indexName = name;
    }

    /**
     * Set the parent Table of the index
     *
     * @param parent the table
     */
    public void setTable(Table parent)
    {
        parentTable = parent;
    }

    /**
     * Get the parent Table of the index
     *
     * @return the table
     */
    public Table getTable()
    {
        return parentTable;
    }

    /**
     * Returns the Name of the table the index is in
     *
     * @return the name of the table
     */
    public String getTableName()
    {
        return parentTable.getName();
    }

    /**
     * Adds a new column to an index.
     *
     * @param attrib xml attributes for the column
     */
    public void addColumn(Attributes attrib)
    {
        indexColumns.add(attrib.getValue("name"));
    }

    /**
     * Return a comma delimited string of the columns which compose this index.
     *
     * @return a list of column names
     */
    public String getColumnList()
    {
        return Column.makeList(getColumns());
    }

    /**
     * Return the list of local columns. You should not edit this list.
     *
     * @return a list of columns
     */
    public List getColumns()
    {
        return indexColumns;
    }

    /**
     * Returns the list of names of the columns referenced by this
     * index.  Slightly over-allocates the list's buffer (just in case
     * more elements are going to be added, such as when a name is
     * being generated).  Feel free to modify this list.
     *
     * @return a list of column names
     */
    protected List getColumnNames()
    {
        List names = new ArrayList(indexColumns.size() + 2);
        Iterator i = getColumns().iterator();
        while (i.hasNext())
        {
            Column c = (Column) i.next();
            names.add(c.getName());
        }
        return names;
    }

    /**
     * String representation of the index. This is an xml representation.
     *
     * @return a xml representation
     */
    public String toString()
    {
        StringBuffer result = new StringBuffer();
        result.append(" <index name=\"")
              .append(getName())
              .append("\"");

        result.append(">\n");

        for (int i = 0; i < indexColumns.size(); i++)
        {
            result.append("  <index-column name=\"")
                .append(indexColumns.get(i))
                .append("\"/>\n");
        }
        result.append(" </index>\n");
        return result.toString();
    }
}
