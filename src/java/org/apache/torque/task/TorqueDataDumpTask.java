package org.apache.torque.task;

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

import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.NoSuchElementException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.velocity.context.Context;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.texen.ant.TexenTask;
import org.apache.torque.engine.database.model.AppData;
import org.apache.torque.engine.database.model.Database;
import org.apache.torque.engine.database.transform.XmlToAppData;

/**
 *  An extended Texen task used for dumping data from db into XML
 *
 * @author <a href="mailto:fedor.karpelevitch@home.com">  Fedor Karpelevitch  </a>
 * @author <a href="jvanzyl@zenplex.com">Jason van Zyl</a>
 * @author <a href="dlr@finemaltcoding.com">Daniel Rall</a>
 * @version $Id$
 */
public class TorqueDataDumpTask
    extends TorqueDataModelTask
{
    /**
     * Database name.
     */
    private String databaseName;

    /**
     *  Database URL used for JDBC connection.
     */
    private String databaseUrl;

    /**
     *  Database driver used for JDBC connection.
     */
    private String databaseDriver;

    /**
     *  Database user used for JDBC connection.
     */
    private String databaseUser;

    /**
     *  Database password used for JDBC connection.
     */
    private String databasePassword;

    /**
     * The database connection used to retrieve the data to dump.
     */
    private Connection conn;

    /**
     * The statement used to acquire the data to dump.
     */
    private Statement stmt;

    /**
     *  Get the database name to dump
     *
     * @return  The DatabaseName value
     */
    public String getDatabaseName()
    {
        return databaseName;
    }

    /**
     *  Set the database name
     *
     * @param  v The new DatabaseName value
     */
    public void setDatabaseName(String v)
    {
        databaseName = v;
    }

    /**
     *  Get the database url
     *
     * @return  The DatabaseUrl value
     */
    public String getDatabaseUrl()
    {
        return databaseUrl;
    }

    /**
     *  Set the database url
     *
     * @param  v The new DatabaseUrl value
     */
    public void setDatabaseUrl(String v)
    {
        databaseUrl = v;
    }

    /**
     *  Get the database driver name
     *
     * @return  String database driver name
     */
    public String getDatabaseDriver()
    {
        return databaseDriver;
    }

    /**
     *  Set the database driver name
     *
     * @param  v The new DatabaseDriver value
     */
    public void setDatabaseDriver(String v)
    {
        databaseDriver = v;
    }

    /**
     *  Get the database user
     *
     * @return  String database user
     */
    public String getDatabaseUser()
    {
        return databaseUser;
    }

    /**
     *  Set the database user
     *
     * @param  v The new DatabaseUser value
     */
    public void setDatabaseUser(String v)
    {
        databaseUser = v;
    }

    /**
     *  Get the database password
     *
     * @return  String database password
     */
    public String getDatabasePassword()
    {
        return databasePassword;
    }

    /**
     *  Set the database password
     *
     * @param  v The new DatabasePassword value
     */
    public void setDatabasePassword(String v)
    {
        databasePassword = v;
    }

    /**
     *  Initializes initial context
     *
     * @return  Description of the Returned Value
     */
    public Context initControlContext()
        throws Exception
    {
        super.initControlContext();
        
        context.put("dataset", "all");

        // FIXME: Use Ant log() method
        System.err.println("Your DB settings are:");
        System.err.println("driver : " + databaseDriver);
        System.err.println("URL : " + databaseUrl);
        System.err.println("user : " + databaseUser);
        System.err.println("password : " + databasePassword);

        try
        {
            Class.forName(databaseDriver);
            // FIXME: Use Ant log() method
            System.err.println("DB driver sucessfuly instantiated");

            conn = DriverManager.getConnection(
                    databaseUrl, databaseUser, databasePassword);

            // FIXME: Use Ant log() method
            System.err.println("DB connection established");
            context.put("tableTool", new TableTool(conn));
        }
        catch (SQLException se)
        {
            System.err.println("SQLException while connecting to DB:");
            se.printStackTrace();
        }
        catch (ClassNotFoundException cnfe)
        {
            System.err.println("cannot load driver:");
            cnfe.printStackTrace();
        }

        return context;
    }

    /**
     * Closes <code>rs</code> and <code>conn</code>, overriding the
     * <code>cleanup()</code> hook method in <code>TexenTask</code>.
     *
     * @exception Exception Database problem while closing resource.
     */
    protected void cleanup()
        throws Exception
    {
        if (stmt != null)
        {
            stmt.close();
        }

        if (conn != null)
        {
            conn.close();
        }
    }

    /**
     *  A nasty do-it-all tool class. It serves as:
     *  <ul>
     *  <li>context tool to fetch a table iterator</li>
     *  <li>the abovenamed iterator which iterates over the table</li>
     *  <li>getter for the table fields</li>
     *  </ul>
     *
     * @author  fedor
     */
    public class TableTool implements Iterator
    {
        private Connection conn;
        private ResultSet rs;
        private boolean isEmpty;


        /**
         *  Constructor for the TableTool object
         *
         * @param  conn Description of Parameter
         */
        public TableTool(Connection conn)
        {
            this.conn = conn;
        }


        /**
         *  Constructor for the TableTool object
         *
         * @param  rs Description of Parameter
         * @exception  SQLException Description of Exception
         */
        public TableTool(ResultSet rs) throws SQLException
        {
            this.rs = rs;
            this.isEmpty = !rs.isBeforeFirst();
        }


        /**
         *  Fetches an <code>Iterator</code> for the data in the named
         *  table.
         *
         * @param  tableName Description of Parameter
         * @return  Description of the Returned Value
         * @exception  SQLException Description of Exception
         */
        public TableTool fetch(String tableName) throws SQLException
        {
            System.err.println();
            System.err.print("fetching table " + tableName);
            // Set Statement object in associated TorqueDataDump
            // instance
            stmt = conn.createStatement();
            return new TableTool
                (stmt.executeQuery("SELECT * FROM " + tableName));
        }


        /**
         *  Description of the Method
         *
         * @return  Description of the Returned Value
         */
        public boolean hasNext()
        {
            try
            {
                return !(rs.isLast() || this.isEmpty);
            }
            catch (SQLException se)
            {
                System.err.println("SQLException :");
                se.printStackTrace();
            }
            return false;
        }


        /**
         *  Description of the Method
         *
         * @return  Description of the Returned Value
         * @exception  NoSuchElementException Description of Exception
         */
        public Object next() throws NoSuchElementException
        {
            try
            {
                System.err.print(".");
                rs.next();
            }
            catch (SQLException se)
            {
                System.err.println("SQLException while iterating:");
                se.printStackTrace();
                throw new NoSuchElementException(se.getMessage());
            }
            return this;
        }


        /**
         *  Description of the Method
         *
         * @param  columnName Description of Parameter
         * @return  Description of the Returned Value
         */
        public String get(String columnName)
        {
            try
            {
                return rs.getString(columnName);
            }
            catch (SQLException se)
            {
                System.err.println("SQLException fetching value " +
                        columnName + ":" + se.getMessage());
            }
            return null;
        }


        /**
         *  Description of the Method
         *
         * @exception  UnsupportedOperationException Description of Exception
         */
        public void remove() throws UnsupportedOperationException
        {
            throw new UnsupportedOperationException();
        }
    }
}
