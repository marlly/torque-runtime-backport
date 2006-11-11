package org.apache.torque;

import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.torque.adapter.DB;
import org.apache.torque.dsfactory.DataSourceFactory;
import org.apache.torque.map.DatabaseMap;
import org.apache.torque.map.MapBuilder;
import org.apache.torque.map.TableMap;
import org.apache.torque.util.BasePeer;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Tests the TorqueInstance Class.
 *
 * @author <a href="mailto:fischer@seitenbau.de">Thomas Fischer</a>
 * @version $Id$
 */
public class TorqueInstanceTest extends TestCase
{
    /** The name of the "default" dataSourceFactory" as used by Turbine */
    private static final String DEFAULT_NAME = "default";

    /**
     * The name of the "turbine" dataSourceFactory"
     * as used by the Turbine configuration
     */
    private static final String TURBINE_NAME = "turbine";

    /**
     * Creates a new instance.
     *
     * @param name the name of the test case to run
     */
    public TorqueInstanceTest(String name)
    {
        super(name);
    }

    /**
     * Tests whether registration of Map Builders works before and after
     * initialisation of Torque.
     * @throws Exception if an error occurs during the Test.
     */
    public void testClassLoading() throws Exception
    {
        Torque.registerMapBuilder(MapBuilderA.CLASS_NAME);
        Torque.init(getConfiguration());
        BasePeer.getMapBuilder(MapBuilderB.CLASS_NAME);

        DatabaseMap databaseMap = Torque.getDatabaseMap(Torque.getDefaultDB());
        assertNotNull(databaseMap.getTable(MapBuilderA.TABLE_NAME));
        assertNotNull(databaseMap.getTable(MapBuilderB.TABLE_NAME));
    }

    /**
     * Tests whether an external adapter is loaded correctly.
     * @throws Exception if an error occurs during the Test.
     */
    public void testExternalAdapter() throws Exception
    {
        DB adapter = Torque.getDatabase(TURBINE_NAME).getAdapter();
        assertNotNull(adapter);
    }

    /**
     * Checks whether a DataSourceFactory with the name
     * <code>DEFAULT_NAME</code> is defined. (TRQS 322)
     * @throws Exception if an error occurs during the Test.
     */
    public void testDefaultDataSourceFactory() throws Exception
    {
        DataSourceFactory defaultDataSourceFactory
                = Torque.getInstance().getDataSourceFactory(DEFAULT_NAME);
        assertNotNull(
                "The DataSourceFactory for Key "
                + DEFAULT_NAME
                + " should not be null",
                defaultDataSourceFactory);
        DataSourceFactory turbineDataSourceFactory
                = Torque.getInstance().getDataSourceFactory(DEFAULT_NAME);
        assertSame("The default DataSourceFactory "
                + "and the turbine DataSourceFactory "
                + "are not the same object",
                defaultDataSourceFactory,
                turbineDataSourceFactory);
    }

    /**
     * Tests whether the databaseInfo objects are filled correctly.
     * @throws Exception if an error occurs during the Test.
     */
    public void testDatabases() throws Exception
    {
        //Torque.init(getConfiguration());
        Map databases = Torque.getDatabases();
        // check whether all expected databases are contained in the Map
        assertEquals(
                "Databases should contain 2 Databases, not "
                    + databases.size(),
                databases.size(),
                2);

        // check that the default database and the turbine database
        // refer to the same object
        Database defaultDatabase = Torque.getDatabase(DEFAULT_NAME);
        Database turbineDatabase = Torque.getDatabase(TURBINE_NAME);

        assertNotSame("The default database and the turbine database "
                        + "are the same object",
                    defaultDatabase,
                    turbineDatabase);
    }

    public void testShutdown() throws Exception
    {
        // because we have not properly initialized the DataSourceFactory,
        // closing the DatasourceFactory down would result in an error.
        // So we have to remove the reference to the DatasourceFactory.
        Torque.getDatabase(TURBINE_NAME).setDataSourceFactory(null);

        Torque.shutdown();
        assertFalse("Torque.isInit() should return false after shutdown",
                Torque.isInit());
        try
        {
            Torque.getDatabases();
            fail("Torque.getDatabases() should throw an Exception "
                    + "after shutdown");
        }
        catch (Exception e)
        {
        }
    }

    /**
     * Reads and returns the configuration out of the configuration file.
     * @return
     * @throws ConfigurationException
     */
    private Configuration getConfiguration() throws ConfigurationException
    {
        Configuration conf
                = new PropertiesConfiguration(BaseTestCase.CONFIG_FILE);
        return conf;
    }

    /**
     * The base class for the Map Builders used in this testbed.
     */
    public abstract static class MapBuilderBase implements MapBuilder
    {

        /** The name of the associated table. */
        private String tableName;

        /** The database map. */
        private DatabaseMap dbMap = null;

        /**
         * Constructs a MapBuilder.
         * @param tableName the name of the table to register.
         */
        public MapBuilderBase(String tableName)
        {
            this.tableName = tableName;
        }

        /**
         * Tells us if this DatabaseMapBuilder is built so that we
         * don't have to re-build it every time.
         *
         * @return true if this DatabaseMapBuilder is built
         */
        public boolean isBuilt()
        {
            return (dbMap != null);
        }

        /**
         * Gets the databasemap this map builder built.
         *
         * @return the databasemap
         */
        public DatabaseMap getDatabaseMap()
        {
            return this.dbMap;
        }

        /**
         * Builds the DatabaseMap.
         *
         * @throws TorqueException in an error occurs during building.
         */
        public void doBuild() throws TorqueException
        {
            dbMap = Torque.getDatabaseMap(TURBINE_NAME);

            dbMap.addTable(tableName);
            TableMap tMap = dbMap.getTable(tableName);

            tMap.setPrimaryKeyMethod(TableMap.NATIVE);

            tMap.setPrimaryKeyMethodInfo(tableName);

            tMap.addPrimaryKey(tableName + "ID", new Integer(0));
            tMap.addColumn(tableName + "NAME", "", 50 );
        }
    }

    /**
     * Map builder implementation for testing.
     */
    public static class MapBuilderA extends MapBuilderBase implements MapBuilder
    {
        /** The name of this class. */
        public static final String CLASS_NAME =
            MapBuilderA.class.getName();

        /** The name of the associated table. */
        public static final String TABLE_NAME = "a";

        public MapBuilderA()
        {
            super(TABLE_NAME);
        }
    }

    /**
     * Second map builder implementation for testing.
     */
    public static class MapBuilderB extends MapBuilderBase implements MapBuilder
    {
        /** The name of this class. */
        public static final String CLASS_NAME =
            MapBuilderB.class.getName();

        /** The name of the associated table. */
        public static final String TABLE_NAME = "b";

        public MapBuilderB()
        {
            super(TABLE_NAME);
        }
    }
}
