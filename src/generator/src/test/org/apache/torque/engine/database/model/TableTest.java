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

import java.util.List;

import junit.framework.TestCase;

import org.apache.torque.engine.database.transform.XmlToAppData;

/**
 * Tests for package handling.
 *
 * @author <a href="mailto:mpoeschl@marmot.at>Martin Poeschl</a>
 * @version $Id$
 */
public class TableTest extends TestCase
{
    private XmlToAppData xmlToAppData = null;
    private AppData appData = null;

    public TableTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        xmlToAppData = new XmlToAppData("mysql", "defaultpackage");
        appData = xmlToAppData.parseFile(
            "src/test/org/apache/torque/engine/database/model/tabletest-schema.xml");
    }

    protected void tearDown() throws Exception
    {
        xmlToAppData = null;
        super.tearDown();
    }

    /**
     * test if the tables get the package name from the properties file
     */
    public void testIdMethodHandling() throws Exception
    {
        Database db = appData.getDatabase("iddb");
        assertEquals(IDMethod.ID_BROKER, db.getDefaultIdMethod());
        Table table = db.getTable("table_idbroker");
        assertEquals(IDMethod.ID_BROKER, table.getIdMethod());
		Table table2 = db.getTable("table_native");
		assertEquals(IDMethod.NATIVE, table2.getIdMethod());
    }
    
    public void testSinglePk() throws Exception
    {
        Database db = appData.getDatabase("iddb");
        Table table = db.getTable("singlepk");
        List pks = table.getPrimaryKey();
        assertTrue(pks.size() == 1);
        Column col = (Column) pks.get(0);
        assertEquals(col.getName(), "singlepk_id");        
    }
    
    public void testMultiPk() throws Exception
    {
        Database db = appData.getDatabase("iddb");
        Table table = db.getTable("multipk");
        List pks = table.getPrimaryKey();
        assertTrue(pks.size() == 2);
        Column cola = (Column) pks.get(0);
        assertEquals(cola.getName(), "multipk_a");        
        Column colb = (Column) pks.get(1);
        assertEquals(colb.getName(), "multipk_b");        
    }
 
    public void testSingleFk() throws Exception
    {
        Database db = appData.getDatabase("iddb");
        Table table = db.getTable("singlefk");
        ForeignKey[] fks = table.getForeignKeys();
        assertTrue(fks.length == 1);
        ForeignKey fk = fks[0];
        assertEquals(fk.getForeignTableName(), "singlepk");
        assertTrue(fk.getForeignColumns().size() == 1);
    }

    public void testMultiFk() throws Exception
    {
        Database db = appData.getDatabase("iddb");
        Table table = db.getTable("multifk");
        ForeignKey[] fks = table.getForeignKeys();
        assertTrue(fks.length == 1);
        ForeignKey fk = fks[0];
        assertEquals(fk.getForeignTableName(), "multipk");
        assertTrue(fk.getForeignColumns().size() == 2);
    }
    
    public void testReferrers() throws Exception
    {
        Database db = appData.getDatabase("iddb");
        Table table = db.getTable("singlepk");
        List refs = table.getReferrers();
        assertTrue(refs.size() == 1);
        ForeignKey fk = (ForeignKey) refs.get(0);
        assertEquals(fk.getTableName(), "singlefk");        
    }
    
}
