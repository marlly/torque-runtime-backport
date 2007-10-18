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
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;
import org.apache.torque.util.Query;
import org.apache.torque.util.SqlExpression;

/**
 * This is used to connect to a Sybase database using Sybase's
 * JConnect JDBC driver.
 *
 * <B>NOTE:</B><I>Currently JConnect does not implement the required
 * methods for ResultSetMetaData, and therefore the village API's may
 * not function.  For connection pooling, everything works.</I>
 *
 * @author <a href="mailto:ekkerbj@netscape.net">Jeff Brekke</a>
 * @version $Id$
 */
public class DBSybase extends AbstractDBAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 4782996646843056810L;

    /** date format */
    private static final String DATE_FORMAT = "yyyyMMdd HH:mm:ss";

    /**
     * Empty constructor.
     */
    protected DBSybase()
    {
    }

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
     * @see org.apache.torque.adapter.DB#getIDMethodType()
     */
    public String getIDMethodType()
    {
        return AUTO_INCREMENT;
    }

    /**
     * Returns the last value from an identity column (available on a
     * per-session basis from the global variable
     * <code>@@identity</code>).
     *
     * @see org.apache.torque.adapter.DB#getIDMethodSQL(Object obj)
     */
    public String getIDMethodSQL(Object unused)
    {
        return "select @@identity";
    }

    /**
     * Locks the specified table.
     *
     * @param con The JDBC connection to use.
     * @param table The name of the table to lock.
     * @throws SQLException No Statement could be created or executed.
     */
    public void lockTable(Connection con, String table) throws SQLException
    {
        Statement statement = con.createStatement();

        StringBuffer stmt = new StringBuffer();
        stmt.append("SELECT next_id FROM ")
        .append(table)
        .append(" FOR UPDATE");

        statement.executeQuery(stmt.toString());
    }

    /**
     * Unlocks the specified table.
     *
     * @param con The JDBC connection to use.
     * @param table The name of the table to unlock.
     * @throws SQLException No Statement could be created or executed.
     */
    public void unlockTable(Connection con, String table) throws SQLException
    {
        // Tables in Sybase are unlocked when a commit is issued.  The
        // user may have issued a commit but do it here to be sure.
        con.commit();
    }

    /**
     * This method is used to chek whether the database supports
     * limiting the size of the resultset.
     *
     * @return LIMIT_STYLE_SYBASE.
     * @deprecated This should not be exposed to the outside
     */
    public int getLimitStyle()
    {
        return DB.LIMIT_STYLE_SYBASE;
    }

    /**
     * Return true for Sybase
     * @see org.apache.torque.adapter.AbstractDBAdapter#supportsNativeLimit()
     */
    public boolean supportsNativeLimit()
    {
        return true;
    }

    /**
     * Modify a query to add limit and offset values for Sybase.
     *
     * @param query The query to modify
     * @param offset the offset Value
     * @param limit the limit Value
     *
     * @throws TorqueException if any error occurs when building the query
     */
    public void generateLimits(Query query, int offset, int limit)
        throws TorqueException
    {
        if ( limit < 0 && offset >= 0 ) { // Offset only test
            return;
        }
        if (limit + offset > 0)
        {
            query.setRowcount(String.valueOf(limit + offset));
        }
        else if (limit + offset == 0)
        {
            // This is necessary to create the empty result set that Torque expects
            query.getWhereClause().add(SqlExpression.build("1", new Integer(0), Criteria.EQUAL));
        }
    }

    /**
     * This method overrides the JDBC escapes used to format dates
     * using a <code>DateFormat</code>.  As of version 11, the Sybase
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
     * Determines whether backslashes (\) should be escaped in explicit SQL
     * strings. If true is returned, a BACKSLASH will be changed to "\\".
     * If false is returned, a BACKSLASH will be left as "\".
     *
     * Sybase (and MSSQL) doesn't define a default escape character,
     * so false is returned.
     *
     * @return false
     * @see org.apache.torque.adapter.DB#escapeText()
     */
    public boolean escapeText()
    {
        return false;
    }

    /**
     * Whether an escape clause in like should be used.
     * Example : select * from AUTHOR where AUTHOR.NAME like '\_%' ESCAPE '\';
     *
     * Sybase needs this, so this implementation always returns
     * <code>true</code>.
     *
     * @return whether the escape clause should be appended or not.
     */
    public boolean useEscapeClauseForLike()
    {
        return true;
    }
}
