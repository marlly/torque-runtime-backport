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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.enum.Enum;

/**
 * Enum for types used in Torque schema.xml files.
 *
 * @author <a href="mailto:mpoeschl@marmot.at>Martin Poeschl</a>
 * @version $Id$
 */
public class SchemaType extends Enum 
{
    public static final SchemaType BIT = new SchemaType("BIT");
    public static final SchemaType TINYINT = new SchemaType("TINYINT");
    public static final SchemaType SMALLINT = new SchemaType("SMALLINT");
    public static final SchemaType INTEGER = new SchemaType("INTEGER");
    public static final SchemaType BIGINT = new SchemaType("BIGINT");
    public static final SchemaType FLOAT = new SchemaType("FLOAT");
    public static final SchemaType REAL = new SchemaType("REAL");
    public static final SchemaType NUMERIC = new SchemaType("NUMERIC");
    public static final SchemaType DECIMAL = new SchemaType("DECIMAL");
    public static final SchemaType CHAR = new SchemaType("CHAR");
    public static final SchemaType VARCHAR = new SchemaType("VARCHAR");
    public static final SchemaType LONGVARCHAR = new SchemaType("LONGVARCHAR");
    public static final SchemaType DATE = new SchemaType("DATE");
    public static final SchemaType TIME = new SchemaType("TIME");
    public static final SchemaType TIMESTAMP = new SchemaType("TIMESTAMP");
    public static final SchemaType BINARY = new SchemaType("BINARY");
    public static final SchemaType VARBINARY = new SchemaType("VARBINARY");
    public static final SchemaType LONGVARBINARY = new SchemaType("LONGVARBINARY");
    public static final SchemaType NULL = new SchemaType("NULL");
    public static final SchemaType OTHER = new SchemaType("OTHER");
    public static final SchemaType JAVA_OBJECT = new SchemaType("JAVA_OBJECT");
    public static final SchemaType DISTINCT = new SchemaType("DISTINCT");
    public static final SchemaType STRUCT = new SchemaType("STRUCT");
    public static final SchemaType ARRAY = new SchemaType("ARRAY");
    public static final SchemaType BLOB = new SchemaType("BLOB");
    public static final SchemaType CLOB = new SchemaType("CLOB");
    public static final SchemaType REF = new SchemaType("REF");
    public static final SchemaType BOOLEANINT = new SchemaType("BOOLEANINT");
    public static final SchemaType BOOLEANCHAR = new SchemaType("BOOLEANCHAR");
    public static final SchemaType DOUBLE = new SchemaType("DOUBLE");
    
    private SchemaType(String type) 
    {
        super(type);
    }
 
    public static SchemaType getEnum(String type) 
    {
        return (SchemaType) getEnum(SchemaType.class, type);
    }
 
    public static Map getEnumMap() 
    {
        return getEnumMap(SchemaType.class);
    }
 
    public static List getEnumList() 
    {
        return getEnumList(SchemaType.class);
    }
 
    public static Iterator iterator() 
    {
        return iterator(SchemaType.class);
    }

}
