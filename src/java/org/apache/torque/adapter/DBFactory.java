package org.apache.torque.adapter;

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

import java.util.HashMap;
import java.util.Map;

/**
 * This class creates different {@link org.apache.torque.adapter.DB}
 * objects based on specified JDBC driver name.
 *
 * @author <a href="mailto:frank.kim@clearink.com">Frank Y. Kim</a>
 * @author <a href="mailto:jon@latchkey.com">Jon S. Stevens</a>
 * @author <a href="mailto:bmclaugh@algx.net">Brett McLaughlin</a>
 * @author <a href="mailto:ralf@reswi.ruhr.de">Ralf Stranzenbach</a>
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @version $Id$
 */
public class DBFactory
{
    /**
     * JDBC driver to Torque Adapter map.
     */
    private static Map adapters = new HashMap(40);

    /**
     * Initialize the JDBC driver to Torque Adapter map.
     */
    static
    {
        adapters.put("com.ibm.as400.access.AS400JDBCDriver", DBDB2400.class);
        adapters.put("COM.ibm.db2.jdbc.app.DB2Driver", DBDB2App.class);
        adapters.put("COM.ibm.db2.jdbc.net.DB2Driver", DBDB2Net.class);
        adapters.put("COM.cloudscape.core.JDBCDriver", DBCloudscape.class);
        adapters.put("org.firebirdsql.jdbc.FBDriver", DBFirebird.class);
        adapters.put("org.hsql.jdbcDriver", DBHypersonicSQL.class);
        adapters.put("org.hsqldb.jdbcDriver", DBHypersonicSQL.class);
        adapters.put("interbase.interclient.Driver", DBInterbase.class);
        adapters.put("org.enhydra.instantdb.jdbc.idbDriver", DBInstantDB.class);
        adapters.put("com.microsoft.jdbc.sqlserver.SQLServerDriver",
            DBMSSQL.class);
        adapters.put("com.jnetdirect.jsql.JSQLDriver", DBMSSQL.class);
        adapters.put("org.gjt.mm.mysql.Driver", DBMM.class);
        adapters.put("oracle.jdbc.driver.OracleDriver", DBOracle.class);
        adapters.put("org.postgresql.Driver", DBPostgres.class);
        adapters.put("com.sap.dbtech.jdbc.DriverSapDB", DBSapDB.class);
        adapters.put("com.sybase.jdbc.SybDriver", DBSybase.class);
        adapters.put("com.sybase.jdbc2.jdbc.SybDriver", DBSybase.class);
        adapters.put("weblogic.jdbc.pool.Driver", DBWeblogic.class);
        adapters.put("org.axiondb.jdbc.AxionDriver", DBAxion.class);
        adapters.put("com.informix.jdbc.IfxDriver", DBInformix.class);
        adapters.put("sun.jdbc.odbc.JdbcOdbcDriver", DBOdbc.class);

        adapters.put("com.ibm.db2.jcc.DB2Driver", DBDerby.class);
        adapters.put("org.apache.derby.jdbc.EmbeddedDriver", DBDerby.class);


        // add some short names to be used when drivers are not used
        adapters.put("as400", DBDB2400.class);
        adapters.put("db2app", DBDB2App.class);
        adapters.put("db2net", DBDB2Net.class);
        adapters.put("cloudscape", DBCloudscape.class);
        adapters.put("firebird", DBFirebird.class);
        adapters.put("hypersonic", DBHypersonicSQL.class);
        adapters.put("interbase", DBInterbase.class);
        adapters.put("instantdb", DBInstantDB.class);
        adapters.put("mssql", DBMSSQL.class);
        adapters.put("mysql", DBMM.class);
        adapters.put("oracle", DBOracle.class);
        adapters.put("postgresql", DBPostgres.class);
        adapters.put("sapdb", DBSapDB.class);
        adapters.put("sybase", DBSybase.class);
        adapters.put("weblogic", DBWeblogic.class);
        adapters.put("axion", DBAxion.class);
        adapters.put("informix", DBInformix.class);
        adapters.put("odbc", DBOdbc.class);
        adapters.put("msaccess", DBOdbc.class);

        adapters.put("derby", DBDerby.class);

        adapters.put("", DBNone.class);
    }

    /**
     * Private constructor to prevent instantiation.
     *
     * Class contains only static methods, so no instances are needed.
     */
    private DBFactory()
    {
    }

    /**
     * Creates a new instance of the Torque database adapter associated
     * with the specified JDBC driver or adapter key.
     *
     * @param key The fully-qualified name of the JDBC driver
     *        or a shorter form adapter key.
     * @return An instance of a Torque database adapter, or null if
     *         no adapter exists for the given key.
     * @throws InstantiationException throws if the adapter could not be
     *         instantiated
     */
    public static DB create(String key)
        throws InstantiationException
    {
        Class adapterClass = (Class) adapters.get(key);

        if (adapterClass == null)
        {
            return null;
        }

        try
        {
            DB adapter = (DB) adapterClass.newInstance();
            // adapter.setJDBCDriver(driver);
            return adapter;
        }
        catch (IllegalAccessException e)
        {
            throw new InstantiationException(
                "Could not instantiate adapter for key : "
                + key
                + ": Assure that adapter classes are in your classpath");
        }
    }

    /**
     * Creates a new instance of the Torque database adapter associated
     * with the specified JDBC driver or adapter key and the class defined.
     *
     * @param key The fully-qualified name of the JDBC driver
     *        or a shorter form adapter key.
     * @param className The fully qualified name of the adapter class
     * @return An instance of a Torque database adapter.
     * @throws InstantiationException throws if the adapter could not be
     *         instantiated
     */
    public static DB create(String key, String className)
        throws InstantiationException
    {
        Class adapterClass;

        try
        {
            adapterClass = (Class) Class.forName(className);
        }
        catch (ClassNotFoundException e)
        {
            throw new InstantiationException(
                    "Could not find adapter "
                    + className
                    + " for key "
                    + key
                    + ": Check your configuration file");
        }

        try
        {
            DB adapter = (DB) adapterClass.newInstance();
            adapters.put(key, adapterClass);
            // adapter.setJDBCDriver(driver);
            return adapter;
        }
        catch (IllegalAccessException e)
        {
            throw new InstantiationException(
                "Could not instantiate adapter for key: "
                + key
                + ": Assure that adapter classes are in your classpath");
        }
    }
}
