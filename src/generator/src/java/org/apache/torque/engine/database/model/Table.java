package org.apache.torque.engine.database.model;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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
import org.apache.commons.lang.StringUtils;
import org.apache.torque.engine.EngineException;
import org.xml.sax.Attributes;

/**
 * Data about a table used in an application.
 *
 * @author <a href="mailto:leon@opticode.co.za">Leon Messerschmidt</a>
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:mpoeschl@marmot.at>Martin Poeschl</a>
 * @author <a href="mailto:jmcnally@collab.net>John McNally</a>
 * @author <a href="mailto:dlr@collab.net>Daniel Rall</a>
 * @author <a href="mailto:byron_foster@byron_foster@yahoo.com>Byron Foster</a>
 * @version $Id$
 */
public class Table implements IDMethod
{
    /** enables debug output */
    private static final boolean DEBUG = false;

    //private AttributeListImpl attributes;
    private List columnList;
    private List foreignKeys;
    private List indices;
    private List unices;
    private List idMethodParameters;
    private String name;
    private String description;
    private String javaName;
    private String idMethod;
    private String javaNamingMethod;
    private Database tableParent;
    private List referrers;
    private List foreignTableNames;
    private boolean containsForeignPK;
    private Column inheritanceColumn;
    private boolean skipSql;
    private boolean abstractValue;
    private String alias;
    private String enterface;
    private String pkg;
    private String baseClass;
    private String basePeer;
    private Hashtable columnsByName;
    private Hashtable columnsByJavaName;
    private boolean needsTransactionInPostgres;
    private boolean heavyIndexing;
    private boolean forReferenceOnly;


    /**
     * Default Constructor
     */
    public Table()
    {
        this(null);
    }

    /**
     * Constructs a table object with a name
     *
     * @param name table name
     */
    public Table(String name)
    {
        this.name = name;
        columnList = new ArrayList();
        foreignKeys = new ArrayList(5);
        indices = new ArrayList(5);
        unices = new ArrayList(5);
        columnsByName = new Hashtable();
        columnsByJavaName = new Hashtable();
    }

    /**
     * Load the table object from an xml tag.
     *
     * @param attrib xml attributes
     * @param defaultIdMethod defined at db level
     */
    public void loadFromXML(Attributes attrib, String defaultIdMethod)
    {
        name = attrib.getValue("name");
        javaName = attrib.getValue("javaName");
        idMethod = attrib.getValue("idMethod");

        // retrieves the method for converting from specified name to
        // a java name.
        javaNamingMethod = attrib.getValue("javaNamingMethod");
        if (javaNamingMethod == null)
        {
            javaNamingMethod = getDatabase().getDefaultJavaNamingMethod();
        }

        if ("null".equals(idMethod))
        {
            idMethod = defaultIdMethod;
        }
        if ("autoincrement".equals(idMethod) || "sequence".equals(idMethod))
        {
            System.out.println("The value '" + idMethod + "' for Torque's "
                    + "table.idMethod attribute has been deprecated in favor "
                    + "of '" + NATIVE + "'.  Please adjust your "
                    + "Torque XML schema accordingly.");
            idMethod = NATIVE;
        }
        skipSql = "true".equals(attrib.getValue("skipSql"));
        // pkg = attrib.getValue("package");
        abstractValue = "true".equals(attrib.getValue("abstract"));
        baseClass = attrib.getValue("baseClass");
        basePeer = attrib.getValue("basePeer");
        alias = attrib.getValue("alias");
        heavyIndexing = "true".equals(attrib.getValue("heavyIndexing"))
                || (!"false".equals(attrib.getValue("heavyIndexing"))
                && getDatabase().isHeavyIndexing());
        description = attrib.getValue("description");
        enterface = attrib.getValue("interface");
    }

    /**
     * <p>A hook for the SAX XML parser to call when this table has
     * been fully loaded from the XML, and all nested elements have
     * been processed.</p>
     *
     * <p>Performs heavy indexing and naming of elements which weren't
     * provided with a name.</p>
     */
    public void doFinalInitialization()
    {
        // Heavy indexing must wait until after all columns composing
        // a table's primary key have been parsed.
        if (heavyIndexing)
        {
            doHeavyIndexing();
        }

        // Name any indices which are missing a name using the
        // appropriate algorithm.
        doNaming();
    }

    /**
     * <p>Adds extra indices for multi-part primary key columns.</p>
     *
     * <p>For databases like MySQL, values in a where clause much
     * match key part order from the left to right.  So, in the key
     * definition <code>PRIMARY KEY (FOO_ID, BAR_ID)</code>,
     * <code>FOO_ID</code> <i>must</i> be the first element used in
     * the <code>where</code> clause of the SQL query used against
     * this table for the primary key index to be used.  This feature
     * could cause problems under MySQL with heavily indexed tables,
     * as MySQL currently only supports 16 indices per table (i.e. it
     * might cause too many indices to be created).</p>
     *
     * <p>See <a href="http://www.mysql.com/doc/E/X/EXPLAIN.html">the
     * manual</a> for a better description of why heavy indexing is
     * useful for quickly searchable database tables.</p>
     */
    private void doHeavyIndexing()
    {
        if (DEBUG)
        {
            System.out.println("doHeavyIndex() called on table " + name);
        }

        List pk = getPrimaryKey();
        int size = pk.size();

        try
        {
            // We start at an offset of 1 because the entire column
            // list is generally implicitly indexed by the fact that
            // it's a primary key.
            for (int i = 1; i < size; i++)
            {
                addIndex(new Index(this, pk.subList(i, size)));
            }
        }
        catch (EngineException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Names composing objects which haven't yet been named.  This
     * currently consists of foreign-key and index entities.
     */
    private void doNaming()
    {
        int i;
        int size;
        String name;

        // Assure names are unique across all databases.
        try
        {
            for (i = 0, size = foreignKeys.size(); i < size; i++)
            {
                ForeignKey fk = (ForeignKey) foreignKeys.get(i);
                name = fk.getName();
                if (StringUtils.isEmpty(name))
                {
                    name = acquireConstraintName("FK", i + 1);
                    fk.setName(name);
                }
            }

            for (i = 0, size = indices.size(); i < size; i++)
            {
                Index index = (Index) indices.get(i);
                name = index.getName();
                if (StringUtils.isEmpty(name))
                {
                    name = acquireConstraintName("I", i + 1);
                    index.setName(name);
                }
            }

            // NOTE: Most RDBMSes can apparently name unique column
            // constraints/indices themselves (using MySQL and Oracle
            // as test cases), so we'll assume that we needn't add an
            // entry to the system name list for these.
        }
        catch (EngineException nameAlreadyInUse)
        {
            nameAlreadyInUse.printStackTrace();
        }
    }

    /**
     * Macro to a constraint name.
     *
     * @param nameType constraint type
     * @param nbr unique number for this constraint type
     * @return unique name for constraint
     * @throws EngineException
     */
    private final String acquireConstraintName(String nameType, int nbr)
            throws EngineException
    {
        List inputs = new ArrayList(4);
        inputs.add(getDatabase());
        inputs.add(getName());
        inputs.add(nameType);
        inputs.add(new Integer(nbr));
        return NameFactory.generateName(NameFactory.CONSTRAINT_GENERATOR,
                                        inputs);
    }

    /**
     * Gets the value of base class for classes produced from this table.
     *
     * @return The base class for classes produced from this table.
     */
    public String getBaseClass()
    {
        if (isAlias() && baseClass == null)
        {
            return alias;
        }
        else if (baseClass == null)
        {
            return getDatabase().getBaseClass();
        }
        else
        {
            return baseClass;
        }
    }

    /**
     * Set the value of baseClass.
     * @param v  Value to assign to baseClass.
     */
    public void setBaseClass(String v)
    {
        this.baseClass = v;
    }

    /**
     * Get the value of basePeer.
     * @return value of basePeer.
     */
    public String getBasePeer()
    {
        if (isAlias() && basePeer == null)
        {
            return alias + "Peer";
        }
        else if (basePeer == null)
        {
            return getDatabase().getBasePeer();
        }
        else
        {
            return basePeer;
        }
    }

    /**
     * Set the value of basePeer.
     * @param v  Value to assign to basePeer.
     */
    public void setBasePeer(String v)
    {
        this.basePeer = v;
    }

    /**
     * A utility function to create a new column from attrib and add it to this
     * table.
     *
     * @param attrib xml attributes for the column to add
     * @return the added column
     */
    public Column addColumn(Attributes attrib)
    {
        Column col = new Column();
        col.setTable(this);
        col.loadFromXML(attrib);
        addColumn(col);
        return col;
    }

    /**
     * Adds a new column to the column list and set the
     * parent table of the column to the current table
     *
     * @param col the column to add
     */
    public void addColumn(Column col)
    {
        col.setTable (this);
        if (col.isInheritance())
        {
            inheritanceColumn = col;
        }
        columnList.add(col);
        columnsByName.put(col.getName(), col);
        columnsByJavaName.put(col.getJavaName(), col);
        col.setPosition(columnList.size());
        needsTransactionInPostgres |= col.requiresTransactionInPostgres();
    }

    /**
     * A utility function to create a new foreign key
     * from attrib and add it to this table.
     *
     * @param attrib the xml attributes
     * @return the created ForeignKey
     */
    public ForeignKey addForeignKey(Attributes attrib)
    {
        ForeignKey fk = new ForeignKey();
        fk.loadFromXML(attrib);
        addForeignKey(fk);
        return fk;
    }

    /**
     * Gets the column that subclasses of the class representing this
     * table can be produced from.
     */
    public Column getChildrenColumn()
    {
        return inheritanceColumn;
    }

    /**
     * Get the objects that can be created from this table.
     */
    public List getChildrenNames()
    {
        if (inheritanceColumn == null
                || !inheritanceColumn.isEnumeratedClasses())
        {
            return null;
        }
        List children = inheritanceColumn.getChildren();
        List names = new ArrayList(children.size());
        for (int i = 0; i < children.size(); i++)
        {
            names.add(((Inheritance) children.get(i)).getClassName());
        }
        return names;
    }

    /**
     * Adds the foreign key from another table that refers to this table.
     *
     * @param fk A foreign key refering to this table
     */
    public void addReferrer(ForeignKey fk)
    {
        if (referrers == null)
        {
            referrers = new ArrayList(5);
        }
        referrers.add(fk);
    }

    /**
     * Get list of references to this table.
     *
     * @return A list of references to this table
     */
    public List getReferrers()
    {
        return referrers;
    }

    /**
     * Set whether this table contains a foreign PK
     *
     * @param b
     */
    public void setContainsForeignPK(boolean b)
    {
        containsForeignPK = b;
    }

    /**
     * Determine if this table contains a foreign PK
     */
    public boolean getContainsForeignPK()
    {
        return containsForeignPK;
    }

    /**
     * A list of tables referenced by foreign keys in this table
     *
     * @return A list of tables
     */
    public List getForeignTableNames()
    {
        if (foreignTableNames == null)
        {
            foreignTableNames = new ArrayList(1);
        }
        return foreignTableNames;
    }

    /**
     * Adds a new FK to the FK list and set the
     * parent table of the column to the current table
     *
     * @param fk A foreign key
     */
    public void addForeignKey(ForeignKey fk)
    {
        fk.setTable (this);
        foreignKeys.add(fk);

        if (foreignTableNames == null)
        {
            foreignTableNames = new ArrayList(5);
        }
        if (foreignTableNames.contains(fk.getForeignTableName()))
        {
            foreignTableNames.add(fk.getForeignTableName());
        }
    }

    /**
     * Return true if the column requires a transaction in Postgres
     */
    public boolean requiresTransactionInPostgres()
    {
        return needsTransactionInPostgres;
    }

    /**
     * A utility function to create a new id method parameter
     * from attrib and add it to this table.
     */
    public IdMethodParameter addIdMethodParameter(Attributes attrib)
    {
        IdMethodParameter imp = new IdMethodParameter();
        imp.loadFromXML(attrib);
        addIdMethodParameter(imp);
        return imp;
    }


    /**
     * Adds a new ID method parameter to the list and sets the parent
     * table of the column associated with the supplied parameter to this table.
     *
     * @param imp The column to add as an ID method parameter.
     */
    public void addIdMethodParameter(IdMethodParameter imp)
    {
        imp.setTable(this);
        if (idMethodParameters == null)
        {
            idMethodParameters = new ArrayList(2);
        }
        idMethodParameters.add(imp);
    }

    /**
     * Adds a new index to the index list and set the
     * parent table of the column to the current table
     */
    public void addIndex(Index index)
    {
        index.setTable (this);
        indices.add(index);
    }

    /**
     * A utility function to create a new index
     * from attrib and add it to this table.
     */
    public Index addIndex(Attributes attrib)
    {
        Index index = new Index();
        index.loadFromXML(attrib);
        addIndex(index);
        return index;
    }

    /**
     * Adds a new Unique to the Unique list and set the
     * parent table of the column to the current table
     */
    public void addUnique(Unique unique)
    {
        unique.setTable(this);
        unices.add(unique);
    }

    /**
     * A utility function to create a new Unique
     * from attrib and add it to this table.
     *
     * @param attrib the xml attributes
     */
    public Unique addUnique(Attributes attrib)
    {
        Unique unique = new Unique();
        unique.loadFromXML(attrib);
        addUnique(unique);
        return unique;
    }

    /**
     * Get the name of the Table
     */
    public String getName()
    {
        return name;
    }

    /**
     * Set the name of the Table
     */
    public void setName(String newName)
    {
        name = newName;
    }

    /**
     * Get the description for the Table
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Set the description for the Table
     *
     * @param newDescription description for the Table
     */
    public void setDescription(String newDescription)
    {
        description = newDescription;
    }

    /**
     * Get name to use in Java sources
     */
    public String getJavaName()
    {
        if (javaName == null)
        {
            List inputs = new ArrayList(2);
            inputs.add(name);
            inputs.add(javaNamingMethod);
            try
            {
                javaName = NameFactory.generateName(NameFactory.JAVA_GENERATOR,
                                                    inputs);
            }
            catch (EngineException e)
            {
                e.printStackTrace();
            }
        }
        return javaName;
    }

    /**
     * Set name to use in Java sources
     */
    public void setJavaName(String javaName)
    {
        this.javaName = javaName;
    }

    /**
     * Get the method for generating pk's
     */
    public String getIdMethod()
    {
        if (idMethod == null)
        {
            return IDMethod.NO_ID_METHOD;
        }
        else
        {
            return idMethod;
        }
    }

    /**
     * Set the method for generating pk's
     */
    public void setIdMethod(String idMethod)
    {
        this.idMethod = idMethod;
    }

    /**
     * Skip generating sql for this table (in the event it should
     * not be created from scratch).
     * @return value of skipSql.
     */
    public boolean isSkipSql()
    {
        return (skipSql || isAlias() || isForReferenceOnly());
    }

    /**
     * Set whether this table should have its creation sql generated.
     * @param v  Value to assign to skipSql.
     */
    public void setSkipSql(boolean  v)
    {
        this.skipSql = v;
    }

    /**
     * JavaName of om object this entry references.
     * @return value of external.
     */
    public String getAlias()
    {
        return alias;
    }

    /**
     * Is this table specified in the schema or is there just
     * a foreign key reference to it.
     * @return value of external.
     */
    public boolean isAlias()
    {
        return (alias != null);
    }

    /**
     * Set whether this table specified in the schema or is there just
     * a foreign key reference to it.
     * @param v  Value to assign to alias.
     */
    public void setAlias(String v)
    {
        this.alias = v;
    }


    /**
     * Interface which objects for this table will implement
     * @return value of interface.
     */
    public String getInterface()
    {
        return enterface;
    }

    /**
     * Interface which objects for this table will implement
     * @param v  Value to assign to interface.
     */
    public void setInterface(String  v)
    {
        this.enterface = v;
    }

    /**
     * When a table is abstract, it marks the business object class that is
     * generated as being abstract. If you have a table called "FOO", then the
     * Foo BO will be <code>public abstract class Foo</code>
     * This helps support class hierarchies
     *
     * @return value of abstractValue.
     */
    public boolean isAbstract()
    {
        return abstractValue;
    }

    /**
     * When a table is abstract, it marks the business object
     * class that is generated as being abstract. If you have a
     * table called "FOO", then the Foo BO will be
     * <code>public abstract class Foo</code>
     * This helps support class hierarchies
     *
     * @param v  Value to assign to abstractValue.
     */
    public void setAbstract(boolean  v)
    {
        this.abstractValue = v;
    }

    /**
     * Get the value of package.
     *
     * @return value of package.
     */
    public String getPackage()
    {
        if (pkg != null)
        {
            return pkg;
        }
        else
        {
            return this.getDatabase().getPackage();
        }
    }

    /**
     * Set the value of package.
     *
     * @param v  Value to assign to package.
     */
    public void setPackage(String v)
    {
        this.pkg = v;
    }

    /**
     * Returns an Array containing all the columns in the table
     */
    public Column[] getColumns()
    {
        int size = columnList.size();
        Column[] tbls = new Column[size];
        for (int i = 0; i < size; i++)
        {
            tbls[i] = (Column) columnList.get(i);
        }
        return tbls;
    }

    /**
     * Utility method to get the number of columns in this table
     */
    public int getNumColumns()
    {
        return columnList.size();
    }

    /**
     * Returns an Array containing all the FKs in the table
     */
    public ForeignKey[] getForeignKeys()
    {
        int size = foreignKeys.size();
        ForeignKey[] tbls = new ForeignKey[size];
        for (int i = 0; i < size; i++)
        {
            tbls[i] = (ForeignKey) foreignKeys.get(i);
        }
        return tbls;
    }

    /**
     * Returns a Collection of parameters relevant for the chosen
     * id generation method.
     */
    public List getIdMethodParameters()
    {
        return idMethodParameters;
    }

    /**
     * A name to use for creating a sequence if one is not specified.
     *
     * @return name of the sequence
     */
    public String getSequenceName()
    {
        String result = null;
        if (getIdMethod().equals(NATIVE))
        {
            List idMethodParams = getIdMethodParameters();
            if (idMethodParams == null)
            {
                result = getName() + "_SEQ";
            }
            else
            {
                result = ((IdMethodParameter) idMethodParams.get(0)).getValue();
            }
        }
        return result;
    }

    /**
     * Returns an Array containing all the indices in the table
     *
     * @return An array containing all the indices
     */
    public Index[] getIndices()
    {
        int size = indices.size();
        Index[] tbls = new Index[size];
        for (int i = 0; i < size; i++)
        {
            tbls[i] = (Index) indices.get(i);
        }
        return tbls;
    }

    /**
     * Returns an Array containing all the UKs in the table
     *
     * @return An array containing all the UKs
     */
    public Unique[] getUnices()
    {
        int size = unices.size();
        Unique[] tbls = new Unique[size];
        for (int i = 0; i < size; i++)
        {
            tbls[i] = (Unique) unices.get(i);
        }
        return tbls;
    }

    /**
     * Returns a specified column.
     *
     * @param name name of the column
     * @return Return a Column object or null if it does not exist.
     */
    public Column getColumn(String name)
    {
        return (Column) columnsByName.get(name);
    }

    /**
     * Returns a specified column.
     *
     * @param javaName java name of the column
     * @return Return a Column object or null if it does not exist.
     */
    public Column getColumnByJavaName(String javaName)
    {
        return (Column) columnsByJavaName.get(javaName);
    }

    /**
     * Return the first foreign key that includes col in it's list
     * of local columns.  Eg. Foreign key (a,b,c) refrences tbl(x,y,z)
     * will be returned of col is either a,b or c.
     *
     * @param col column name included in the key
     * @return Return a Column object or null if it does not exist.
     */
    public ForeignKey getForeignKey(String col)
    {
        ForeignKey firstFK = null;
        for (Iterator iter = foreignKeys.iterator(); iter.hasNext();)
        {
            ForeignKey key = (ForeignKey) iter.next();
            if (key.getLocalColumns().contains(col))
            {
                if (firstFK == null)
                {
                    firstFK = key;
                }
                else
                {
                    //System.out.println(col+" is in multiple FKs.  This is not"
                    //                   + " being handled properly.");
                    //throw new IllegalStateException("Cannot call method if " +
                    //    "column is referenced multiple times");
                }
            }
        }
        return firstFK;
    }

    /**
     * Returns true if the table contains a specified column
     *
     * @param col the column
     * @return true if the table contains the column
     */
    public boolean containsColumn(Column col)
    {
        return columnList.contains(col);
    }

    /**
     * Returns true if the table contains a specified column
     *
     * @param name name of the column
     * @return true if the table contains the column
     */
    public boolean containsColumn(String name)
    {
        return (getColumn(name) != null);
    }

    /**
     * Set the parent of the table
     *
     * @param parent the parant database
     */
    public void setDatabase(Database parent)
    {
        tableParent = parent;
    }

    /**
     * Get the parent of the table
     *
     * @return the parant database
     */
    public Database getDatabase()
    {
        return tableParent;
    }

    /**
     * Flag to determine if code/sql gets created for this table.
     * Table will be skipped, if return true.
     * @return value of forReferenceOnly.
     */
    public boolean isForReferenceOnly()
    {
        return forReferenceOnly;
    }

    /**
     * Flag to determine if code/sql gets created for this table.
     * Table will be skipped, if set to true.
     * @param v  Value to assign to forReferenceOnly.
     */
    public void setForReferenceOnly(boolean  v)
    {
        this.forReferenceOnly = v;
    }

    /**
     * Returns a XML representation of this table.
     *
     * @return XML representation of this table
     */
    public String toString()
    {
        StringBuffer result = new StringBuffer();

        result.append ("<table name=\"")
              .append(name)
              .append('\"');

        if (javaName != null)
        {
            result.append(" javaName=\"")
                  .append(javaName)
                  .append('\"');
        }

        if (idMethod != null)
        {
            result.append(" idMethod=\"")
                  .append(idMethod)
                  .append('\"');
        }

        if (skipSql)
        {
            result.append(" skipSql=\"")
                  .append(new Boolean(skipSql))
                  .append('\"');
        }

        if (abstractValue)
        {
            result.append(" abstract=\"")
                  .append(new Boolean(abstractValue))
                  .append('\"');
        }

        if (baseClass != null)
        {
            result.append(" baseClass=\"")
                  .append(baseClass)
                  .append('\"');
        }

        if (basePeer != null)
        {
            result.append(" basePeer=\"")
                  .append(basePeer)
                  .append('\"');
        }

        result.append(">\n");

        if (columnList != null)
        {
            for (Iterator iter = columnList.iterator(); iter.hasNext();)
            {
                result.append(iter.next());
            }
        }

        if (foreignKeys != null)
        {
            for (Iterator iter = foreignKeys.iterator(); iter.hasNext();)
            {
                result.append(iter.next());
            }
        }

        if (idMethodParameters != null)
        {
            Iterator iter = idMethodParameters.iterator();
            while (iter.hasNext())
            {
                result.append(iter.next());
            }
        }

        result.append ("</table>\n");

        return result.toString();
    }

    /**
     * Returns the collection of Columns which make up the single primary
     * key for this table.
     *
     * @return A list of the primary key parts.
     */
    public List getPrimaryKey()
    {
        List pk = new ArrayList(columnList.size());

        Iterator iter = columnList.iterator();
        while (iter.hasNext())
        {
            Column col = (Column) iter.next();
            if (col.isPrimaryKey())
            {
                pk.add(col);
            }
        }
        return pk;
    }

    /**
     * Determine whether this table has a primary key.
     *
     * @return Whether this table has any primary key parts.
     */
    public boolean hasPrimaryKey()
    {
        return (getPrimaryKey().size() > 0);
    }

    /**
     * Returns all parts of the primary key, separated by commas.
     *
     * @return A CSV list of primary key parts.
     */
    public String printPrimaryKey()
    {
        return printList(columnList);
    }

    /**
     * Returns the elements of the list, separated by commas.
     *
     * @param list a list of Columns
     * @return A CSV list.
     */
    private String printList(List list)
    {
        StringBuffer result = new StringBuffer();
        boolean comma = false;
        for (Iterator iter = list.iterator(); iter.hasNext();)
        {
            Column col = (Column) iter.next();
            if (col.isPrimaryKey())
            {
                if (comma)
                {
                    result.append(',');
                }
                else
                {
                    comma = true;
                }
                result.append(col.getName());
            }
        }
        return result.toString();
    }
}
