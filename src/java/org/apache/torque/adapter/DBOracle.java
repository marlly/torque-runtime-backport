package org.apache.torque.adapter;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This code should be used for an Oracle database pool.
 *
 * @author <a href="mailto:jon@clearink.com">Jon S. Stevens</a>
 * @author <a href="mailto:bmclaugh@algx.net">Brett McLaughlin</a>
 * @author <a href="mailto:bschneider@vecna.com">Bill Schneider</a>
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @version $Id$
 */
public class DBOracle extends DB
{
    /** date format used in getDateString() */
    private static final String DATE_FORMAT = "dd-MM-yyyy HH:mm:ss";

    /**
     * Empty constructor.
     */
    protected DBOracle()
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
     * This method is used to format any date string.
     *
     * @param date the Date to format
     * @return The date formatted String for Oracle.
     */
    public String getDateString(Date date) 
    {
        return "TO_DATE('" + new SimpleDateFormat(DATE_FORMAT).format(date)
                + "', 'DD-MM-YYYY HH24:MI:SS')";
    }

    /**
     * @see org.apache.torque.adapter.DB#getIDMethodType()
     */
    public String getIDMethodType()
    {
        return SEQUENCE;
    }

    /**
     * Returns the next key from a sequence.  Uses the following
     * implementation:
     *
     * <blockquote><code><pre>
     * select sequenceName.nextval from dual
     * </pre></code></blockquote>
     *
     * @param sequenceName The name of the sequence (should be of type
     * <code>String</code>).
     * @return SQL to retreive the next database key.
     * @see org.apache.torque.adapter.DB#getIDMethodSQL(Object)
     */
    public String getIDMethodSQL(Object sequenceName)
    {
        return ("select " + sequenceName + ".nextval from dual");
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
     * @exception SQLException No Statement could be created or executed.
     */
    public void unlockTable(Connection con, String table) throws SQLException
    {
        // Tables in Oracle are unlocked when a commit is issued.  The
        // user may have issued a commit but do it here to be sure.
        con.commit();
    }

    /**
     * This method is used to check whether the database natively
     * supports limiting the size of the resultset.
     *
     * @return True.
     */
    public boolean supportsNativeLimit()
    {
        return true;
    }

    /**
     * This method is used to check whether the database supports
     * limiting the size of the resultset.
     *
     * @return LIMIT_STYLE_ORACLE.
     */
    public int getLimitStyle()
    {
        return DB.LIMIT_STYLE_ORACLE;
    }

    /**
     * This method is for the SqlExpression.quoteAndEscape rules.  The rule is,
     * any string in a SqlExpression with a BACKSLASH will either be changed to
     * "\\" or left as "\".  SapDB does not need the escape character.
     *
     * @return false.
     */
    public boolean escapeText()
    {
        return false;
    }
}
