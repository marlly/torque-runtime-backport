package org.apache.torque.engine.database;

import junit.framework.*;
import org.apache.torque.engine.database.transform.*;
import org.apache.torque.engine.database.model.*;

public class TestPackageHandling extends TestCase
{
    private XmlToAppData xmlToAppData = null;
    private AppData appData = null;

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
    {
        xmlToAppData = new XmlToAppData("mysql", "defaultpackage", null);
        appData = xmlToAppData.parseFile(
            "src/test/org/apache/torque/engine/database/package-schema.xml");
        Database db = appData.getDatabase("packagedb");
        assertEquals("defaultpackage", db.getPackage());
        Table table = db.getTable("table_a");
        assertEquals("defaultpackage", table.getPackage());
    }

    /**
     * test if the tables get the package name from the database tag
     */
    public void testDatabasePackageName()
    {
        xmlToAppData = new XmlToAppData("mysql", "defaultpackage", null);
        appData = xmlToAppData.parseFile(
            "src/test/org/apache/torque/engine/database/package2-schema.xml");
        Database db = appData.getDatabase("packagedb2");
        assertEquals("packagefromdb", db.getPackage());
        Table table = db.getTable("table_a");
        assertEquals("packagefromdb", table.getPackage());
    }

}
