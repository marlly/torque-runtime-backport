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
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.xml.sax.Attributes;

import org.apache.torque.Torque;
import org.apache.torque.engine.EngineException;
import org.apache.torque.adapter.IDMethod;


/**
 * A class for holding application data structures.
 *
 * @author <a href="mailto:leon@opticode.co.za>Leon Messerschmidt</a>
 * @author <a href="mailto:jmcnally@collab.net>John McNally</a>
 * @author <a href="mailto:mpoeschl@marmot.at>Martin Poeschl</a>
 * @author <a href="mailto:dlr@collab.net>Daniel Rall</a>
 * @author <a href="mailto:byron_foster@byron_foster@yahoo.com>Byron Foster</a>
 * @version $Id$
 */
public class Database
{
    private String databaseType = null;
    private List tableList = new ArrayList(100);
    private Column curColumn;
    private String name;
    private String pkg;
    private String baseClass;
    private String basePeer;
    private String defaultIdMethod;
    private String defaultJavaType;
    private String defaultJavaNamingMethod;
    private AppData dbParent;
    private Hashtable tablesByName = new Hashtable();
    private Hashtable tablesByJavaName = new Hashtable();
    private boolean heavyIndexing;

    /**
     * Creates a new instance with unset attributes.
     */
    public Database()
    {
    }

    /**
     * Load the database object from an xml tag.
     */
    public void loadFromXML(Attributes attrib)
    {
        setName(attrib.getValue("name"));
        pkg = attrib.getValue("package");
        baseClass = attrib.getValue("baseClass");
        basePeer = attrib.getValue("basePeer");
        defaultJavaType = attrib.getValue("defaultJavaType");
        defaultIdMethod = attrib.getValue("defaultIdMethod");
        defaultJavaNamingMethod = attrib.getValue("defaultJavaNamingMethod");
        if (defaultJavaNamingMethod == null)
        {
            defaultJavaNamingMethod = NameGenerator.CONV_METHOD_UNDERSCORE;
        }
        heavyIndexing = "true".equals(attrib.getValue("heavyIndexing"));
    }

    /**
     * Get the name of the Database
     */
    public String getName()
    {
        return name;
    }

    /**
     * Set the name of the Database
     */
    public void setName(String name)
    {
        this.name = (name == null ? Torque.getDefaultDB() : name);
    }

    /**
     * Get the value of package.
     * @return value of package.
     */
    public String getPackage()
    {
        return pkg;
    }

    /**
     * Set the value of package.
     * @param v  Value to assign to package.
     */
    public void setPackage(String  v)
    {
        this.pkg = v;
    }

    /**
     * Get the value of baseClass.
     * @return value of baseClass.
     */
    public String getBaseClass()
    {
        if ( baseClass == null )
        {
            return "BaseObject";
        }
        return baseClass;
    }

    /**
     * Set the value of baseClass.
     * @param v  Value to assign to baseClass.
     */
    public void setBaseClass(String  v)
    {
        this.baseClass = v;
    }

    /**
     * Get the value of basePeer.
     * @return value of basePeer.
     */
    public String getBasePeer()
    {
        if ( basePeer == null )
        {
            return "BasePeer";
        }
        return basePeer;
    }

    /**
     * Set the value of basePeer.
     * @param v Value to assign to basePeer.
     */
    public void setBasePeer(String  v)
    {
        this.basePeer = v;
    }

    /**
     * Get the value of defaultIdMethod.
     * @return value of defaultIdMethod.
     */
    public String getDefaultIdMethod()
    {
        return defaultIdMethod;
    }

    /**
     * Set the value of defaultIdMethod.
     * @param v Value to assign to defaultIdMethod.
     */
    public void setDefaultIdMethod(String  v)
    {
        this.defaultIdMethod = v;
    }

    /**
     * Get type to use in Java sources (primitive || object)
     */
    public String getDefaultJavaType()
    {
        return defaultJavaType;
    }

    /**
     * Get the value of defaultJavaNamingMethod which specifies the
     * method for converting schema names for table and column to Java names.
     * @return The default naming conversion used by this database.
     */
    public String getDefaultJavaNamingMethod()
    {
        return defaultJavaNamingMethod;
    }

    /**
     * Set the value of defaultJavaNamingMethod.
     * @param v The default naming conversion for this database to use.
     */
    public void setDefaultJavaNamingMethod(String  v)
    {
        this.defaultJavaNamingMethod = v;
    }

    /**
     * Get the value of heavyIndexing.
     * @return value of heavyIndexing.
     */
    public boolean isHeavyIndexing()
    {
        return heavyIndexing;
    }

    /**
     * Set the value of heavyIndexing.
     * @param v  Value to assign to heavyIndexing.
     */
    public void setHeavyIndexing(boolean  v)
    {
        this.heavyIndexing = v;
    }

    /**
     * Return an array of all tables
     */
    public Table[] getTables()
    {
        int size = tableList.size();
        Table[] tbls = new Table[size];
        for (int i = 0; i < size; i++)
        {
            tbls[i] = (Table) tableList.get(i);
        }
        return tbls;
    }

    /**
     * Return the table with the specified name.
     * @return A Table object.  If it does not exist it returns null
     */
    public Table getTable(String name)
    {
        return (Table) tablesByName.get(name);
    }

    /**
     * Return the table with the specified javaName.
     * @return A Table object.  If it does not exist it returns null
     */
    public Table getTableByJavaName(String javaName)
    {
        return (Table) tablesByJavaName.get(javaName);
    }

    /**
     * An utility method to add a new table from an xml attribute.
     */
    public Table addTable(Attributes attrib)
    {
        Table tbl = new Table();
        tbl.setDatabase(this);
        tbl.loadFromXML(attrib, this.getDefaultIdMethod());
        addTable(tbl);
        return tbl;
    }

    /**
     * Add a table to the list and sets the Database property to this Database
     */
    public void addTable(Table tbl)
    {
        tbl.setDatabase(this);
        tableList.add(tbl);
        tablesByName.put(tbl.getName(), tbl);
        tablesByJavaName.put(tbl.getJavaName(), tbl);
    }

    /**
     * Set the parent of the database
     */
    public void setAppData(AppData parent)
    {
        dbParent = parent;
    }

    /**
     * Get the parent of the table
     */
    public AppData getAppData()
    {
        return dbParent;
    }

    protected String getDatabaseType()
    {
        return databaseType;
    }

    public void setDatabaseType(String databaseType)
    {
        this.databaseType = databaseType;
    }

    /**
     * Returns the value of the named property from this database's
     * <code>db.props</code> file.
     *
     * @param name The name of the property to retrieve the value of.
     * @return The value of the specified property.
     * @exception EngineException Couldn't access properties.
     */
    protected String getProperty(String name)
        throws EngineException
    {
        Properties p = getAppData().getIdiosyncrasies(databaseType);
        return (p == null ? null : p.getProperty(name));
    }

    /**
     * Determines if this database will be using the
     * <code>IDMethod.ID_BROKER</code> to create ids for torque OM
     * objects.
     * @return true if there is at least one table in this database that
     * uses the <code>IDMethod.ID_BROKER</code> method of generating
     * ids. returns false otherwise.
     */
    public boolean requiresIdTable()
    {
        Table table[] = getTables();
        for (int i = 0; i < table.length; i++)
        {
            if (table[i].getIdMethod().equals(IDMethod.ID_BROKER))
            {
                return true;
            }
        }
        return false;
    }


    /**
     * Creats a string representation of this Database.
     * The representation is given in xml format.
     */
    public String toString()
    {
        StringBuffer result = new StringBuffer();

        result.append("<database name=\"").append(getName()).append('"')
            .append(" package=\"").append(getPackage()).append('"')
            .append(" defaultIdMethod=\"").append(getDefaultIdMethod())
            .append('"')
            .append(" baseClass=\"").append(getBaseClass()).append('"')
            .append(" basePeer=\"").append(getBasePeer()).append('"')
            .append(">\n");

        for (Iterator i = tableList.iterator(); i.hasNext(); )
        {
            result.append(i.next());
        }

        result.append("</database>");
        return result.toString();
    }
}
