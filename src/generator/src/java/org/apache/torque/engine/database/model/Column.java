package org.apache.torque.engine.database.model;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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
import java.util.List;

import org.apache.commons.lang.StringUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.torque.engine.EngineException;

import org.xml.sax.Attributes;

/**
 * A Class for holding data about a column used in an Application.
 *
 * @author <a href="mailto:leon@opticode.co.za">Leon Messerschmidt</a>
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:jon@latchkey.com">Jon S. Stevens</a>
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @author <a href="mailto:byron_foster@byron_foster@yahoo.com>Byron Foster</a>
 * @author <a href="mailto:mpoeschl@marmot.at>Martin Poeschl</a>
 * @version $Id$
 */
public class Column
{
    private static final String DEFAULT_TYPE = "VARCHAR";
    /** Logging class from commons.logging */
    private static Log log = LogFactory.getLog(Column.class);
    private String name;
    private String description;
    private String javaName = null;
    private String javaNamingMethod;
    private boolean isNotNull = false;
    private String size;
    private String scale;
    /** type as defined in schema.xml */
    private String torqueType;
    private String javaType;
    private Table parentTable;
    private int position;
    private boolean isPrimaryKey = false;
    private boolean isUnique = false;
    private boolean isAutoIncrement = false;
    private String defaultValue;
    private List referrers;
    // only one type is supported currently, which assumes the
    // column either contains the classnames or a key to
    // classnames specified in the schema.  Others may be
    // supported later.
    private String inheritanceType;
    private boolean isInheritance;
    private boolean isEnumeratedClasses;
    private List inheritanceList;
    private boolean needsTransactionInPostgres;

    /** class name to do input validation on this column */
    private String inputValidator = null;

    /**
     * Creates a new instance with a <code>null</code> name.
     */
    public Column()
    {
        this(null);
    }

    /**
     * Creates a new column and set the name
     *
     * @param name column name
     */
    public Column(String name)
    {
        this.name = name;
    }

    /**
     * Return a comma delimited string listing the specified columns.
     *
     * @param columns Either a list of <code>Column</code> objects, or
     * a list of <code>String</code> objects with column names.
     */
    public static String makeList(List columns)
    {
        Object obj = columns.get(0);
        boolean isColumnList = (obj instanceof Column);
        if (isColumnList)
        {
            obj = ((Column) obj).getName();
        }
        StringBuffer buf = new StringBuffer((String) obj);
        for (int i = 1; i < columns.size(); i++)
        {
            obj = columns.get(i);
            if (isColumnList)
            {
                obj = ((Column) obj).getName();
            }
            buf.append(", ").append(obj);
        }
        return buf.toString();
    }

    /**
     * Imports a column from an XML specification
     */
    public void loadFromXML(Attributes attrib)
    {
        String dom = attrib.getValue("domain");
        if (StringUtils.isNotEmpty(dom)) 
        {
            Domain domain = getTable().getDatabase().getDomain(dom);
            size = domain.getSize();
            scale = domain.getScale();
            setType(domain.getType().getName());
            defaultValue = domain.getDefaultValue();
        } 
        else
        {
            setType(DEFAULT_TYPE);
        }
        //Name
        name = attrib.getValue("name");

        javaName = attrib.getValue("javaName");
        javaType = attrib.getValue("javaType");
        if (javaType != null && javaType.length() == 0)
        {
            javaType = null;
        }

        // retrieves the method for converting from specified name to
        // a java name.
        javaNamingMethod = attrib.getValue("javaNamingMethod");
        if (javaNamingMethod == null)
        {
            javaNamingMethod
                    = parentTable.getDatabase().getDefaultJavaNamingMethod();
        }

        //Primary Key
        String primaryKey = attrib.getValue("primaryKey");
        //Avoid NullPointerExceptions on string comparisons.
        isPrimaryKey = ("true".equals (primaryKey));

        // If this column is a primary key then it can't be null.
        if ("true".equals (primaryKey))
        {
            isNotNull = true;
        }

        // HELP: Should primary key, index, and/or idMethod="native"
        // affect isNotNull?  If not, please document why here.
        String notNull = attrib.getValue("required");
        isNotNull = (notNull != null && "true".equals(notNull));

        //AutoIncrement/Sequences
        String autoIncrement = attrib.getValue("autoIncrement");
        isAutoIncrement = ("true".equals(autoIncrement));

        //Default column value.
        defaultValue = StringUtils.defaultString(
                attrib.getValue("default"), defaultValue);

        size = StringUtils.defaultString(attrib.getValue("size"), size);
        scale = StringUtils.defaultString(attrib.getValue("scale"), scale);

        setType(StringUtils.defaultString(attrib.getValue("type"), torqueType));

        inheritanceType = attrib.getValue("inheritance");
        isInheritance = (inheritanceType != null
                && !inheritanceType.equals("false"));

        this.inputValidator = attrib.getValue("inputValidator");
        description = attrib.getValue("description");
    }

    /**
     * Returns table.column
     */
    public String getFullyQualifiedName()
    {
        return (parentTable.getName() + '.' + name);
    }

    /**
     * Get the name of the column
     */
    public String getName()
    {
        return name;
    }

    /**
     * Set the name of the column
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
     * Get name to use in Java sources to build method names.
     * 
     * @return the capitalised javaName
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
                log.error(e, e);
            }
        }
        return StringUtils.capitalize(javaName);
    }
    
    /**
     * Get variable name to use in Java sources (= uncapitalised java name)
     */    
    public String getUncapitalisedJavaName()
    {
        return StringUtils.uncapitalize(getJavaName());
    }

    /**
     * Set name to use in Java sources
     */
    public void setJavaName(String javaName)
    {
        this.javaName = javaName;
    }

    /**
     * Get type to use in Java sources
     */
    public String getJavaType()
    {
        return javaType;
    }

    /**
     * Get the location of this column within the table (one-based).
     * @return value of position.
     */
    public int getPosition()
    {
        return position;
    }

    /**
     * Get the location of this column within the table (one-based).
     * @param v  Value to assign to position.
     */
    public void setPosition(int  v)
    {
        this.position = v;
    }

    /**
     * Set the parent Table of the column
     */
    public void setTable(Table parent)
    {
        parentTable = parent;
    }

    /**
     * Get the parent Table of the column
     */
    public Table getTable()
    {
        return parentTable;
    }

    /**
     * Returns the Name of the table the column is in
     */
    public String getTableName()
    {
        return parentTable.getName();
    }

    /**
     * A utility function to create a new column
     * from attrib and add it to this table.
     */
    public Inheritance addInheritance(Attributes attrib)
    {
        Inheritance inh = new Inheritance();
        inh.loadFromXML (attrib);
        addInheritance(inh);

        return inh;
    }

    /**
     * Adds a new inheritance definition to the inheritance list and set the
     * parent column of the inheritance to the current column
     */
    public void addInheritance(Inheritance inh)
    {
        inh.setColumn(this);
        if (inheritanceList == null)
        {
            inheritanceList = new ArrayList();
            isEnumeratedClasses = true;
        }
        inheritanceList.add(inh);
    }

    /**
     * Get the inheritance definitions.
     */
    public List getChildren()
    {
        return inheritanceList;
    }

    /**
     * Determine if this column is a normal property or specifies a
     * the classes that are represented in the table containing this column.
     */
    public boolean isInheritance()
    {
        return isInheritance;
    }

    /**
     * Determine if possible classes have been enumerated in the xml file.
     */
    public boolean isEnumeratedClasses()
    {
        return isEnumeratedClasses;
    }

    /**
     * Return the isNotNull property of the column
     */
    public boolean isNotNull()
    {
        return isNotNull;
    }

    /**
     * Set the isNotNull property of the column
     */
    public void setNotNull(boolean status)
    {
        isNotNull = status;
    }

    /**
     * Set if the column is a primary key or not
     */
    public void setPrimaryKey(boolean pk)
    {
        isPrimaryKey = pk;
    }

    /**
     * Return true if the column is a primary key
     */
    public boolean isPrimaryKey()
    {
        return isPrimaryKey;
    }

    /**
     * Set true if the column is UNIQUE
     */
    public void setUnique (boolean u)
    {
        isUnique = u;
    }

    /**
     * Get the UNIQUE property
     */
    public boolean isUnique()
    {
        return isUnique;
    }

    /**
     * Return true if the column requires a transaction in Postgres
     */
    public boolean requiresTransactionInPostgres()
    {
        return needsTransactionInPostgres;
    }

    /**
     * Utility method to determine if this column is a foreign key.
     */
    public boolean isForeignKey()
    {
        return (getForeignKey() != null);
    }

    /**
     * Determine if this column is a foreign key that refers to the
     * same table as another foreign key column in this table.
     */
    public boolean isMultipleFK()
    {
        ForeignKey fk = getForeignKey();
        if (fk != null)
        {
            ForeignKey[] fks = parentTable.getForeignKeys();
            for (int i = 0; i < fks.length; i++)
            {
                if (fks[i].getForeignTableName()
                        .equals(fk.getForeignTableName())
                        && !fks[i].getLocalColumns().contains(this.name))
                {
                    return true;
                }
            }
        }

        // No multiple foreign keys.
        return false;
    }

    /**
     * get the foreign key object for this column
     * if it is a foreign key or part of a foreign key
     */
    public ForeignKey getForeignKey()
    {
        return parentTable.getForeignKey (this.name);
    }

    /**
     * Utility method to get the related table of this column if it is a foreign
     * key or part of a foreign key
     */
    public String getRelatedTableName()
    {
        ForeignKey fk = getForeignKey();
        return (fk == null ? null : fk.getForeignTableName());
    }


    /**
     * Utility method to get the related column of this local column if this
     * column is a foreign key or part of a foreign key.
     */
    public String getRelatedColumnName()
    {
        ForeignKey fk = getForeignKey();
        if (fk == null)
        {
            return null;
        }
        else
        {
            return fk.getLocalForeignMapping().get(this.name).toString();
        }
    }

    /**
     * Adds the foreign key from another table that refers to this column.
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
     * Get list of references to this column.
     */
    public List getReferrers()
    {
        if (referrers == null)
        {
            referrers = new ArrayList(5);
        }
        return referrers;
    }

    /**
     * Returns the colunm type
     */
    public void setType(String torqueType)
    {
        this.torqueType = torqueType;
        if (torqueType.equals("VARBINARY") || torqueType.equals("BLOB"))
        {
            needsTransactionInPostgres = true;
        }
    }

    /**
     * Returns the column jdbc type as an object
     */
    public Object getType()
    {
        return TypeMap.getJdbcType(torqueType);
    }

    /**
     * Returns the column type as given in the schema as an object
     */
    public Object getTorqueType()
    {
        return torqueType;
    }

    /**
     * Utility method to see if the column is a string
     * 
     * @deprecated will be removed after the 3.2 release
     */
    public boolean isString()
    {
        return (torqueType.indexOf ("CHAR") != -1);
    }

    /**
     * Utility method to return the value as an element to be usable
     * in an SQL insert statement. This is used from the SQL loader task
     */
    public boolean needEscapedValue()
    {
        return (torqueType != null) &&
                ( torqueType.equals("VARCHAR")
                        || torqueType.equals("LONGVARCHAR")
                        || torqueType.equals("DATE")
                        || torqueType.equals("DATETIME")
                        || torqueType.equals("TIMESTAMP")
                        || torqueType.equals("CHAR")
                        || torqueType.equals("CLOB"));
    }

    /**
     * String representation of the column. This is an xml representation.
     *
     * @return string representation in xml
     */
    public String toString()
    {
        StringBuffer result = new StringBuffer();
        result.append("    <column name=\"").append(name).append('"');

        if (javaName != null)
        {
            result.append(" javaName=\"").append(javaName).append('"');
        }

        if (isPrimaryKey)
        {
            result.append(" primaryKey=\"").append(isPrimaryKey).append('"');
        }

        if (isNotNull)
        {
            result.append(" required=\"true\"");
        }
        else
        {
            result.append(" required=\"false\"");
        }

        result.append(" type=\"").append (torqueType).append('"');

        if (size != null)
        {
            result.append(" size=\"").append(size).append('"');
        }

        if (defaultValue != null)
        {
            result.append(" default=\"").append(defaultValue).append('"');
        }

        if (isInheritance())
        {
            result.append(" inheritance=\"").append(inheritanceType)
                .append('"');
        }

        // Close the column.
        result.append(" />\n");

        return result.toString();
    }

    /**
     * Returns the size of the column
     */
    public String getSize()
    {
        return size;
    }

    /**
     * Set the size of the column
     */
    public void setSize(String newSize)
    {
        size = newSize;
    }

    /**
     * Returns the scale of the column
     */
    public String getScale()
    {
        return scale;
    }

    /**
     * Set the scale of the column
     */
    public void setScale(String newScale)
    {
        scale = newScale;
    }
    
    /**
     * Return the size and scale in brackets for use in an sql schema.
     * 
     * @return size and scale or an empty String if there are no values 
     *         available.
     */
    public String printSize()
    {
        if (size != null && scale != null) 
        {
            return '(' + size + ',' + scale + ')';
        }
        else if (size != null) 
        {
            return '(' + size + ')';
        }
        else
        {
            return "";
        }
    }

    /**
     * Return a string that will give this column a default value.
     * <p>
     * TODO: Properly SQL-escape text values.
     */
     public String getDefaultSetting()
     {
         StringBuffer dflt = new StringBuffer(0);
         if (defaultValue != null)
         {
             dflt.append("default ");
             if (TypeMap.isTextType(torqueType))
             {
                 // TODO: Properly SQL-escape the text.
                 dflt.append('\'').append(defaultValue).append('\'');
             }
             else
             {
                 dflt.append(defaultValue);
             }
         }
         return dflt.toString();
     }

    /**
     * Set a string that will give this column a default value.
     */
    public void setDefaultValue(String def)
    {
        defaultValue = def;
    }

    /**
     * Get a string that will give this column a default value.
     */
    public String getDefaultValue()
    {
        return defaultValue;
    }

    /**
     * Returns the class name to do input validation
     */
    public String getInputValidator()
    {
       return this.inputValidator;
    }

    /**
     * Return auto increment/sequence string for the target database. We need to
     * pass in the props for the target database!
     */
    public boolean isAutoIncrement()
    {
        return isAutoIncrement;
    }

    /**
     * Set the auto increment value.
     * Use isAutoIncrement() to find out if it is set or not.
     */
    public void setAutoIncrement(boolean value)
    {
        isAutoIncrement = value;
    }

    /**
     * Set the column type from a string property
     * (normally a string from an sql input file)
     */
    public void setTypeFromString (String typeName, String size)
    {
        String tn = typeName.toUpperCase();
        setType(tn);

        if (size != null)
        {
            this.size = size;
        }

        if (tn.indexOf ("CHAR") != -1)
        {
            torqueType = "VARCHAR";
        }
        else if (tn.indexOf ("INT") != -1)
        {
            torqueType = "INTEGER";
        }
        else if (tn.indexOf ("FLOAT") != -1)
        {
            torqueType = "FLOAT";
        }
        else if (tn.indexOf ("DATE") != -1)
        {
            torqueType = "DATE";
        }
        else if (tn.indexOf ("TIME") != -1)
        {
            torqueType = "TIMESTAMP";
        }
        else if (tn.indexOf ("BINARY") != -1)
        {
            torqueType = "LONGVARBINARY";
        }
        else
        {
            torqueType = "VARCHAR";
        }
    }

    /**
     * Return a string representation of the
     * Java object which corresponds to the JDBC
     * type of this column. Use in the generation
     * of MapBuilders.
     */
    public String getJavaObject()
    {
        return TypeMap.getJavaObject(torqueType);
    }

    /**
     * Return a string representation of the primitive java type which
     * corresponds to the JDBC type of this column.
     *
     * @return string representation of the primitive java type
     */
    public String getJavaPrimitive()
    {
        return TypeMap.getJavaNative(torqueType);
    }

    /**
     * Return a string representation of the native java type which corresponds
     * to the JDBC type of this column. Use in the generation of Base objects.
     * This method is used by torque, so it returns Key types for primaryKey and
     * foreignKey columns
     *
     * @return java datatype used by torque
     */
    public String getJavaNative()
    {
        String jtype = TypeMap.getJavaNativeObject(torqueType);
        if (isUsePrimitive())
        {
            jtype = TypeMap.getJavaNative(torqueType);
        }

        return jtype;
    }

    /**
     * Return Village asX() method which corresponds to the JDBC type
     * which represents this column.
     */
    public String getVillageMethod()
    {
        String vmethod = TypeMap.getVillageObjectMethod(torqueType);
        if (isUsePrimitive())
        {
            vmethod = TypeMap.getVillageMethod(torqueType);
        }

        return vmethod;
    }

    /**
     * Return ParameterParser getX() method which
     * corresponds to the JDBC type which represents this column.
     */
    public String getParameterParserMethod()
    {
        return TypeMap.getPPMethod(torqueType);
    }

    /**
     * Returns true if the column type is boolean in the
     * java object and a numeric (1 or 0) in the db.
     */
    public boolean isBooleanInt()
    {
        return TypeMap.isBooleanInt(torqueType);
    }

    /**
     * Returns true if the column type is boolean in the
     * java object and a String ("Y" or "N") in the db.
     */
    public boolean isBooleanChar()
    {
        return TypeMap.isBooleanChar(torqueType);
    }

    /**
     * returns true, if the columns java native type is an
     * boolean, byte, short, int, long, float, double, char
     */
    public boolean isPrimitive()
    {
        String t = getJavaNative();
        return "boolean".equals(t)
            || "byte".equals(t)
            || "short".equals(t)
            || "int".equals(t)
            || "long".equals(t)
            || "float".equals(t)
            || "double".equals(t)
            || "char".equals(t);
    }

    public boolean isUsePrimitive()
    {
        String s = getJavaType();
        return (s != null && s.equals("primitive"))
            || (s == null && !"object".equals(
               getTable().getDatabase().getDefaultJavaType()));
    }
}
