package org.apache.torque.adapter;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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

        // add some short names to be used when drivers are not used
        adapters.put("as400", DBDB2400.class);
        adapters.put("db2app", DBDB2App.class);
        adapters.put("db2net", DBDB2Net.class);
        adapters.put("cloudscape", DBCloudscape.class);
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

        adapters.put("", DBNone.class);
    }

    /**
     * Creates a new instance of the Turbine database adapter associated
     * with the specified JDBC driver or adapter key.
     *
     * @param driver The fully-qualified name of the JDBC driver to
     * create a new adapter instance for or a shorter form adapter key.
     * @return An instance of a Turbine database adapter.
     * @throws InstantiationException throws if the JDBC driver could not be
     *      instantiated
     */
    public static DB create(String driver)
        throws InstantiationException
    {
        Class adapterClass = (Class) adapters.get(driver);

        if (adapterClass != null)
        {
            try
            {
                DB adapter = (DB) adapterClass.newInstance();
                // adapter.setJDBCDriver(driver);
                return adapter;
            }
            catch (IllegalAccessException e)
            {
                throw new InstantiationException(
                    "Could not instantiate adapter for JDBC driver: "
                    + driver
                    + ": Assure that adapter bytecodes are in your classpath");
            }
        }
        else
        {
            throw new InstantiationException(
                "Unknown JDBC driver: "
                + driver
                + ": Check your configuration file");
        }
    }
}
