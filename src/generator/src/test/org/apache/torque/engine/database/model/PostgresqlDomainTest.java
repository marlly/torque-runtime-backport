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
 *    "Apache Torque" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Torque", nor may "Apache" appear in their name, without
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

import junit.framework.TestCase;

import org.apache.torque.engine.database.transform.XmlToAppData;

/**
 * Tests for domain handling (for Postgresql).
 *
 * @author <a href="mailto:mpoeschl@marmot.at>Martin Poeschl</a>
 * @version $Id$
 */
public class PostgresqlDomainTest extends TestCase
{
    private XmlToAppData xmlToAppData = null;
    private Database db = null;

    public PostgresqlDomainTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        xmlToAppData = new XmlToAppData("postgresql", "defaultpackage");
        db = xmlToAppData.parseFile(
            "src/test/org/apache/torque/engine/database/model/domaintest-schema.xml");
    }

    protected void tearDown() throws Exception
    {
        xmlToAppData = null;
        super.tearDown();
    }
  
    /**
     * test if the tables get the package name from the properties file
     */
    public void testDomainColumn() throws Exception
    {
        Table table = db.getTable("product");
        Column name = table.getColumn("name");
        assertEquals("VARCHAR", name.getDomain().getSqlType());
        assertEquals("40", name.getSize());
        assertEquals("name VARCHAR(40)  ", name.getSqlString());
        Column price = table.getColumn("price");
        assertEquals("NUMERIC", price.getTorqueType());
        assertEquals("NUMERIC", price.getDomain().getSqlType());
        assertEquals("10", price.getSize());
        assertEquals("2", price.getScale());
        assertEquals("0", price.getDefaultValue());
        assertEquals("(10,2)", price.printSize());
        assertEquals("price NUMERIC(10,2) default 0  ", price.getSqlString());
    }
    
    /**
     * test if the tables get the package name from the properties file
     */
    public void testExtendedDomainColumn() throws Exception
    {
        Table table = db.getTable("article");
        Column price = table.getColumn("price");
        assertEquals("NUMERIC", price.getTorqueType());
        assertEquals("NUMERIC", price.getDomain().getSqlType());
        assertEquals("12", price.getSize());
        assertEquals("2", price.getScale());
        assertEquals("1000", price.getDefaultValue());
        assertEquals("(12,2)", price.printSize());
        assertEquals("price NUMERIC(12,2) default 1000  ", price.getSqlString());
    }
    
    public void testDecimalColumn() throws Exception
    {
        Table table = db.getTable("article");
        Column col = table.getColumn("decimal_col");
        assertEquals("DECIMAL", col.getTorqueType());
        assertEquals("DECIMAL", col.getDomain().getSqlType());
        assertEquals("10", col.getSize());
        assertEquals("3", col.getScale());
        assertEquals("(10,3)", col.printSize());
        assertEquals("decimal_col DECIMAL(10,3)  ", col.getSqlString());
    }

    public void testDateColumn() throws Exception
    {
        Table table = db.getTable("article");
        Column col = table.getColumn("date_col");
        assertEquals("DATE", col.getTorqueType());
        assertEquals("DATE", col.getDomain().getSqlType());
        assertEquals("", col.printSize());
        assertEquals("date_col DATE  ", col.getSqlString());
    }

    public void testNativeAutoincrement() throws Exception
    {
        Table table = db.getTable("native");
        Column col = table.getColumn("native_id");
        assertEquals("SERIAL", col.getAutoIncrementString());
        // TODO sequence or identity??
//        assertEquals("native_id SERIAL", col.getSqlString());
        col = table.getColumn("name");
        assertEquals("", col.getAutoIncrementString());
    }    

    public void testIdBrokerAutoincrement() throws Exception
    {
        Table table = db.getTable("article");
        Column col = table.getColumn("article_id");
        assertEquals("", col.getAutoIncrementString());
        assertEquals("article_id INTEGER NOT NULL ", col.getSqlString());
        col = table.getColumn("name");
        assertEquals("", col.getAutoIncrementString());
    }    
    
    public void testBooleanint() throws Exception
    {
        Table table = db.getTable("types");
        Column col = table.getColumn("cbooleanint");
        assertEquals("", col.getAutoIncrementString());
        assertEquals("BOOLEANINT", col.getTorqueType());
        assertEquals("INT2", col.getDomain().getSqlType());
        assertEquals("cbooleanint INT2  ", col.getSqlString());
    }    
    
}