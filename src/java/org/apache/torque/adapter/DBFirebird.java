package org.apache.torque.adapter;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * This code should be used for a Firebird database pool.
 *
 * @author Joerg Friedrich
 * @version $Id: DBInterbase.java 239630 2005-08-24 12:25:32Z henning $
 */
public class DBFirebird extends AbstractDBAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -2782124791802056450L;

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:MM:ss";

    /**
     * This method is used to ignore case.
     *
     * @param in The string to transform to upper case.
     * @return The upper case string.
     */
    public String toUpperCase(String in)
    {
        return new StringBuffer("UPPER(").append(in).append(")").toString();
    }

    /**
     * This method is used to ignore case.
     *
     * @param in The string whose case to ignore.
     * @return The string in a case that can be ignored.
     */
    public String ignoreCase(String in)
    {
        return new StringBuffer("UPPER(").append(in).append(")").toString();
    }

    /**
     * This method is used to ignore case in an ORDER BY clause.
     * Usually it is the same as ignoreCase, but some databases
     * (Interbase for example) does not use the same SQL in ORDER BY
     * and other clauses.
     *
     * @param in The string whose case to ignore.
     * @return The string in a case that can be ignored.
     */
    public String ignoreCaseInOrderBy(String in)
    {
        return in;
    }

    /**
     * Gets the string delimiter (usually '\'').
     *
     * @return The delimiter.
     */
    public char getStringDelimiter()
    {
        return '\'';
    }

    /**
     * @see org.apache.torque.adapter.DB#getIDMethodType()
     */
    public String getIDMethodType()
    {
        return NO_ID_METHOD;
    }

    /**
     * @see org.apache.torque.adapter.DB#getIDMethodSQL(Object obj)
     */
    public String getIDMethodSQL(Object obj)
    {
        return null;
    }

    /**
     * Locks the specified table.
     *
     * @param con The JDBC connection to use.
     * @param table The name of the table to lock.
     * @exception SQLException No Statement could be created or executed.
     */
    public void lockTable(Connection con, String table) throws SQLException
    {
        Statement statement = con.createStatement();

        StringBuffer stmt = new StringBuffer();
        stmt.append("SET TRANSACTION ")
                .append("ISOLATION LEVEL READ COMMITTED ")
                .append("NO RECORD_VERSION WAIT ")
                .append("RESERVING ")
                .append(table)
                .append(" FOR PROTECTED WRITE");

        statement.executeQuery(stmt.toString());
    }

    /**
     * Unlocks the specified table.
     *
     * @param con The JDBC connection to use.
     * @param table The name of the table to unlock.
     * @exception SQLException No Statement could be created or executed.
     */
    public void unlockTable(Connection con, String table) throws SQLException
    {
        // Tables in Interbase are unlocked when a commit is issued.
        // The user may have issued a commit but do it here to be
        // sure.
        con.commit();
    }


    /**
     * This method overrides the JDBC escapes used to format dates
     * using a <code>DateFormat</code>.  As of version 2.0.11, the MM
     * JDBC driver does not implement JDBC 3.0 escapes.
     *
     * @param date the date to format
     * @return The properly formatted date String.
     */
    public String getDateString(Date date)
    {
        char delim = getStringDelimiter();
        return (delim + new SimpleDateFormat(DATE_FORMAT).format(date) + delim);
    }
}
