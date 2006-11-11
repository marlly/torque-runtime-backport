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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * This is used to connect to an embedded Apache Derby Database using
 * the supplied JDBC driver.
 *
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @version $Id$
 */
public class DBDerby extends AbstractDBAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 6265962681516206415L;

    /**
     * Empty constructor.
     */
    protected DBDerby()
    {
    }

    /**
     * This method is used to ignore case.
     *
     * @param str The string to transform to upper case.
     * @return The upper case string.
     */
    public String toUpperCase(String str)
    {
        return new StringBuffer("UPPER(")
                .append(str)
                .append(")")
                .toString();
    }

    /**
     * This method is used to ignore case.
     *
     * @param str The string whose case to ignore.
     * @return The string in a case that can be ignored.
     */
    public String ignoreCase(String str)
    {
        return toUpperCase(str);
    }

    /**
     * @see org.apache.torque.adapter.DB#getIDMethodType()
     */
    public String getIDMethodType()
    {
        return AUTO_INCREMENT;
    }

    /**
     * Returns the SQL to get the database key of the last row
     * inserted, which in this case is
     * <code>VALUES IDENTITY_VAL_LOCAL()</code>.
     *
     * @see org.apache.torque.adapter.DB#getIDMethodSQL(Object obj)
     */
    public String getIDMethodSQL(Object obj)
    {
        return "VALUES IDENTITY_VAL_LOCAL()";
    }

    /**
     * Locks the specified table.
     *
     * @param con The JDBC connection to use.
     * @param table The name of the table to lock.
     * @exception SQLException No Statement could be created or executed.
     */
    public void lockTable(Connection con, String table)
            throws SQLException
    {
        Statement statement = con.createStatement();
        StringBuffer stmt = new StringBuffer();
        stmt.append("LOCK TABLE ")
                .append(table).append(" IN EXCLUSIVE MODE");
        statement.executeUpdate(stmt.toString());
    }

    /**
     * Unlocks the specified table.
     *
     * @param con The JDBC connection to use.
     * @param table The name of the table to unlock.
     * @exception SQLException No Statement could be created or executed.
     */
    public void unlockTable(Connection con, String table)
            throws SQLException
    {
    }

    /**
     * Whether backslashes (\) should be escaped in explicit SQL strings.
     * If true is returned, a BACKSLASH will be changed to "\\". If false
     * is returned, a BACKSLASH will be left as "\".
     *
     * As derby does not need escaping of Backslashes, this method always
     * returns false.
     *
     * @return true if the database needs to escape backslashes
     *         in SqlExpressions.
     */

     public boolean escapeText()
     {
         return false;
     }

     /**
     * Whether an escape clause in like should be used.
     * Example : select * from AUTHOR where AUTHOR.NAME like '\_%' ESCAPE '\';
     *
     * Derby needs this, so this implementation always returns
     * <code>true</code>.
     *
     * @return whether the escape clause should be appended or not.
     */
    public boolean useEscapeClauseForLike()
    {
        return true;
    }
}
