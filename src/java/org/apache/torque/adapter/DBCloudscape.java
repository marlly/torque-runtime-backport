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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.StringTokenizer;

/**
 * This is used to connect to Cloudscape SQL databases.
 *
 * @author <a href="mailto:taylor@apache.org">David Sean Taylor</a>
 * @version $Id$
 */
public class DBCloudscape
    extends DB
{
    private String url;
    private String username;
    private String password;
    private static final String QUALIFIER = ".";

    /**
     * Constructor.
     */
    protected DBCloudscape()
    {
    }

    /**
     * Returns a JDBC <code>Connection</code> from the
     * <code>DriverManager</code>.
     *
     * @return A JDBC <code>Connection</code> object for this
     * database.
     * @exception SQLException
     */
    public Connection getConnection()
        throws SQLException
    {
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Performs basic initialization.  Calls Class.forName() to assure
     * that the JDBC driver for this adapter can be loaded.
     *
     * @param url The URL of the database to connect to.
     * @param username The name of the user to use when connecting.
     * @param password The user's password.
     * @exception Exception The JDBC driver could not be loaded or
     * instantiated.
     */
    public void init(String url,
                     String username,
                     String password)
        throws Exception
    {
        this.url = url;
        this.username = username;
        this.password = password;

        Class.forName(getJDBCDriver()).newInstance();
    }

    /**
     * This method is used to ignore case.
     *
     * @param in The string to transform to upper case.
     * @return The upper case string.
     */
    public String toUpperCase(String in)
    {
        return in;
    }

    /**
     * This method is used to ignore case.
     *
     * @param in The string whose case to ignore.
     * @return The string in a case that can be ignored.
     */
    public String ignoreCase(String in)
    {
        return in;
    }

    /**
     * @see org.apache.torque.adapter.DB#getIDMethodType()
     */
    public String getIDMethodType()
    {
        return AUTO_INCREMENT;
    }

    /**
     * @see org.apache.torque.adapter.DB#getIDMethodSQL(Object obj)
     */
    public String getIDMethodSQL(Object obj)
    {
        StringBuffer sql = new StringBuffer(132);
        sql.append("select distinct ConnectionInfo.lastAutoincrementValue(");

        String qualifiedIdentifier = (String)obj;

        StringTokenizer tokenizer = new StringTokenizer(qualifiedIdentifier, QUALIFIER);
        int count = tokenizer.countTokens();

        String schema, table, column;

        System.out.println("qi = " + qualifiedIdentifier);
        // no qualifiers, its simply a column name
        switch (count)
        {
        case 0:
            return ""; // not valid -- we need the column name and table name 
        case 1:
            return ""; // not valid -- we need the table name to select from

        case 2:
            table = tokenizer.nextToken();
            column = tokenizer.nextToken();
            sql.append("'APP', '");
            sql.append(table);
            break;

        case 3:
            schema = tokenizer.nextToken();
            table = tokenizer.nextToken();
            column = tokenizer.nextToken();
            sql.append("'");
            sql.append(schema);
            sql.append("', '");
            sql.append(table);
            break;

        default:
            return ""; // not valid
        }

        sql.append("', '");
        sql.append(column);
        sql.append("') FROM ");
        sql.append(table);
        
        System.out.println(sql.toString());
        return sql.toString();
    }

    /**
     * Locks the specified table.
     *
     * @param con The JDBC connection to use.
     * @param table The name of the table to lock.
     * @exception SQLException
     */
    public void lockTable(Connection con,
                          String table)
        throws SQLException
    {
    }

    /**
     * Unlocks the specified table.
     *
     * @param con The JDBC connection to use.
     * @param table The name of the table to unlock.
     * @exception SQLException
     */
    public void unlockTable(Connection con,
                            String table)
        throws SQLException
    {
    }
}
