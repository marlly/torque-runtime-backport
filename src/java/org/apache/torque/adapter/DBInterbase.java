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
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * This code should be used for an Interbase database pool.
 *
 * @author <a href="mailto:frank@opticode.co.za">Frank Conradie</a>
 * @version $Id$
 */
public class DBInterbase extends AbstractDBAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -6709312389168248070L;

    /**
     * The format in which interbase expects dates (with time).
     */
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

    /**
     * This method is for the SqlExpression.quoteAndEscape rules.  The rule is,
     * any string in a SqlExpression with a BACKSLASH will either be changed to
     * "\\" (if the method returns true) or left as "\" (if the method returns
     * false).
     *
     * @return false.
     */
    public boolean escapeText()
    {
        return false;
    }

    /**
     * Whether an escape clause in like should be used.
     * Example : select * from AUTHOR where AUTHOR.NAME like '\_%' ESCAPE '\';
     *
     * Interbase needs this, so this implementation always returns
     * <code>true</code>.
     *
     * @return whether the escape clause should be appended or not.
     */
    public boolean useEscapeClauseForLike()
    {
        return true;
    }
}
