package org.apache.torque.engine.database.model;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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

import org.apache.commons.lang.StringUtils;
import org.apache.torque.engine.platform.Platform;
import org.xml.sax.Attributes;

/**
 * A Class for holding data about a column used in an Application.
 *
 * @author <a href="mailto:mpoeschl@marmot.at>Martin Poeschl</a>
 * @version $Id$
 */
public class Domain
{
    private String name;
    private String description;
    private String size;
    private String scale;
    /** type as defined in schema.xml */
    private SchemaType torqueType;
    private String sqlType;
    private String defaultValue;
    
    /**
     * Creates a new instance with a <code>null</code> name.
     */
    public Domain()
    {
        this.name = null;
    }

    /**
     * Creates a new Domain and set the name
     *
     * @param name column name
     */
    public Domain(String name)
    {
        this.name = name;
    }
    
    /**
     * Creates a new Domain and set the name
     */
    public Domain(SchemaType type)
    {
        this.name = null;
        this.torqueType = type;
        this.sqlType = type.getName();
    }

    /**
     * Creates a new Domain and set the name
     */
    public Domain(SchemaType type, String sqlType)
    {
        this.name = null;
        this.torqueType = type;
        this.sqlType = sqlType;
    }
    
    /**
     * Creates a new Domain and set the name
     */
    public Domain(SchemaType type, String sqlType, String size, String scale)
    {
        this.name = null;
        this.torqueType = type;
        this.sqlType = sqlType;
        this.size = size;
        this.scale = scale;
    }

    /**
     * Creates a new Domain and set the name
     */
    public Domain(SchemaType type, String sqlType, String size)
    {
        this.name = null;
        this.torqueType = type;
        this.sqlType = sqlType;
        this.size = size;
    }
    
    public Domain(Domain domain)
    {
        copy(domain);
    }
    
    public void copy(Domain domain)
    {
        this.defaultValue = domain.getDefaultValue();
        this.description = domain.getDescription();
        this.name = domain.getName();
        this.scale = domain.getScale();
        this.size = domain.getSize();
        this.sqlType = domain.getSqlType();
        this.torqueType = domain.getType();
    }
    
    /**
     * Imports a column from an XML specification
     */
    public void loadFromXML(Attributes attrib, Platform platform)
    {
        SchemaType schemaType = SchemaType.getEnum(attrib.getValue("type"));
        copy(platform.getDomainForSchemaType(schemaType));
        //Name
        name = attrib.getValue("name");
        //Default column value.
        defaultValue = attrib.getValue("default");
        size = attrib.getValue("size");
        scale = attrib.getValue("scale");

        description = attrib.getValue("description");
    }

    /**
     * @return Returns the description.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @param description The description to set.
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return Returns the name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name The name to set.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return Returns the scale.
     */
    public String getScale()
    {
        return scale;
    }

    /**
     * @param scale The scale to set.
     */
    public void setScale(String scale)
    {
        this.scale = scale;
    }
    
    /**
     * Replaces the size if the new value is not null.
     * 
     * @param value The size to set.
     */
    public void replaceScale(String value)
    {
        this.scale = StringUtils.defaultString(value, getScale());
    }

    /**
     * @return Returns the size.
     */
    public String getSize()
    {
        return size;
    }

    /**
     * @param size The size to set.
     */
    public void setSize(String size)
    {
        this.size = size;
    }
    
    /**
     * Replaces the size if the new value is not null.
     * 
     * @param value The size to set.
     */
    public void replaceSize(String value)
    {
        this.size = StringUtils.defaultString(value, getSize());
    }

    /**
     * @return Returns the torqueType.
     */
    public SchemaType getType()
    {
        return torqueType;
    }

    /**
     * @param torqueType The torqueType to set.
     */
    public void setType(SchemaType torqueType)
    {
        this.torqueType = torqueType;
    }

    /**
     * @param torqueType The torqueType to set.
     */
    public void setType(String torqueType)
    {
        this.torqueType = SchemaType.getEnum(torqueType);
    }
    
    /**
     * Replaces the default value if the new value is not null.
     * 
     * @param value The defaultValue to set.
     */
    public void replaceType(String value)
    {
        this.torqueType = SchemaType.getEnum(
                StringUtils.defaultString(value, getType().getName()));
    }
    
    /**
     * @return Returns the defaultValue.
     */
    public String getDefaultValue()
    {
        return defaultValue;
    }

    /**
     * Return a string that will give this column a default value.
     * @deprecated
     */
    public String getDefaultSetting()
    {
        StringBuffer dflt = new StringBuffer(0);
        if (getDefaultValue() != null)
        {
            dflt.append("default ");
            if (TypeMap.isTextType(getType()))
            {
                // TODO: Properly SQL-escape the text.
                dflt.append('\'').append(getDefaultValue()).append('\'');
            }
            else
            {
                dflt.append(getDefaultValue());
            }
        }
        return dflt.toString();
    } 
     
    /**
     * @param defaultValue The defaultValue to set.
     */
    public void setDefaultValue(String defaultValue)
    {
        this.defaultValue = defaultValue;
    }
    
    /**
     * Replaces the default value if the new value is not null.
     * 
     * @param value The defaultValue to set.
     */
    public void replaceDefaultValue(String value)
    {
        this.defaultValue = StringUtils.defaultString(value, getDefaultValue());
    }

    /**
     * @return Returns the sqlType.
     */
    public String getSqlType() 
    {
        return sqlType;
    }

    /**
     * @param sqlType The sqlType to set.
     */
    public void setSqlType(String sqlType) 
    {
        this.sqlType = sqlType;
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

}
