package org.apache.torque.engine.database;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import junit.framework.TestCase;

import org.apache.torque.engine.database.model.Database;
import org.apache.torque.engine.database.model.Table;
import org.apache.torque.engine.database.transform.XmlToAppData;

/**
 * Tests for package handling.
 *
 * @author <a href="mailto:mpoeschl@marmot.at>Martin Poeschl</a>
 * @version $Id$
 */
public class TestPackageHandling extends TestCase
{
    private XmlToAppData xmlToAppData = null;
    private Database database = null;

    public TestPackageHandling(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        xmlToAppData = null;
        super.tearDown();
    }

    /**
     * test if the tables get the package name from the properties file
     */
    public void testDefaultPackageName()
            throws Exception
    {
        xmlToAppData = new XmlToAppData("mysql", "defaultpackage");
        database = xmlToAppData.parseFile(
            "src/test/org/apache/torque/engine/database/package-schema.xml");
        assertEquals("defaultpackage", database.getPackage());
        Table table = database.getTable("table_a");
        assertEquals("defaultpackage", table.getPackage());
    }

    /**
     * test if the tables get the package name from the database tag
     */
    public void testDatabasePackageName()
            throws Exception
    {
        xmlToAppData = new XmlToAppData("mysql", "defaultpackage");
        database = xmlToAppData.parseFile(
            "src/test/org/apache/torque/engine/database/package2-schema.xml");
        assertEquals("packagefromdb", database.getPackage());
        Table table = database.getTable("table_a");
        assertEquals("packagefromdb", table.getPackage());
    }

}
