package org.apache.torque.adapter;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import java.util.Date;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * <code>DB</code> defines the interface for a Torque database
 * adapter.  Support for new databases is added by subclassing
 * <code>DB</code> and implementing its abstract interface, and by
 * registering the new database adapter and its corresponding
 * JDBC driver in the service configuration file.
 *
 * <p>The Torque database adapters exist to present a uniform
 * interface to database access across all available databases.  Once
 * the necessary adapters have been written and configured,
 * transparent swapping of databases is theoretically supported with
 * <i>zero code changes</i> and minimal configuration file
 * modifications.
 *
 * <p>Torque uses the driver class name to find the right adapter.
 * A JDBC driver corresponding to your adapter must be added to the properties
 * file, using the fully-qualified class name of the driver. If no driver is
 * specified for your database, <code>driver.default</code> is used.
 *
 * <pre>
 * #### MySQL MM Driver
 * database.default.driver=org.gjt.mm.mysql.Driver
 * database.default.url=jdbc:mysql://localhost/DATABASENAME
 * </pre>
 *
 * @author <a href="mailto:jon@latchkey.com">Jon S. Stevens</a>
 * @author <a href="mailto:bmclaugh@algx.net">Brett McLaughlin</a>
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @author <a href="mailto:vido@ldh.org">Augustin Vidovic</a>
 * @version $Id$
 */
public abstract class DB implements Serializable, IDMethod
{
    /** Database does not support limiting result sets. */
    public static final int LIMIT_STYLE_NONE = 0;

    /** <code>SELECT ... LIMIT <limit>, [&lt;offset&gt;]</code> */
    public static final int LIMIT_STYLE_POSTGRES = 1;

    /** <code>SELECT ... LIMIT [<offset>, ] &lt;offset&gt;</code> */
    public static final int LIMIT_STYLE_MYSQL = 2;

    /** <code>SET ROWCOUNT &lt;offset&gt; SELECT ... SET ROWCOUNT 0</code> */
    public static final int LIMIT_STYLE_SYBASE = 3;

    /** <code><pre>SELECT ... WHERE ... AND ROWNUM < <limit></pre></code> */
    public static final int LIMIT_STYLE_ORACLE = 4;

    /** <code><pre>SELECT ... WHERE ... AND ROW_NUMBER() OVER() < <limit></pre></code> */
    public static final int LIMIT_STYLE_DB2 = 5;

    /**
     * Empty constructor.
     */
    protected DB()
    {
    }

    /**
     * This method is used to ignore case.
     *
     * @param in The string to transform to upper case.
     * @return The upper case string.
     */
    public abstract String toUpperCase(String in);

    /**
     * Returns the character used to indicate the beginning and end of
     * a piece of text used in a SQL statement (generally a single
     * quote).
     *
     * @return The text delimeter.
     */
    public char getStringDelimiter()
    {
        return '\'';
    }

    /**
     * Returns the constant from the {@link
     * org.apache.torque.adapter.IDMethod} interface denoting which
     * type of primary key generation method this type of RDBMS uses.
     *
     * @return IDMethod constant
     */
    public abstract String getIDMethodType();

    /**
     * Returns SQL used to get the most recently inserted primary key.
     * Databases which have no support for this return
     * <code>null</code>.
     *
     * @param obj Information used for key generation.
     * @return The most recently inserted database key.
     */
    public abstract String getIDMethodSQL(Object obj);

    /**
     * Locks the specified table.
     *
     * @param con The JDBC connection to use.
     * @param table The name of the table to lock.
     * @throws SQLException No Statement could be created or executed.
     */
    public abstract void lockTable(Connection con, String table)
            throws SQLException;

    /**
     * Unlocks the specified table.
     *
     * @param con The JDBC connection to use.
     * @param table The name of the table to unlock.
     * @throws SQLException No Statement could be created or executed.
     */
    public abstract void unlockTable(Connection con, String table)
            throws SQLException;

    /**
     * This method is used to ignore case.
     *
     * @param in The string whose case to ignore.
     * @return The string in a case that can be ignored.
     */
    public abstract String ignoreCase(String in);

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
        return ignoreCase(in);
    }

    /**
     * This method is used to check whether the database natively
     * supports limiting the size of the resultset.
     *
     * @return True if the database natively supports limiting the
     * size of the resultset.
     */
    public boolean supportsNativeLimit()
    {
        return false;
    }

    /**
     * This method is used to check whether the database natively
     * supports returning results starting at an offset position other
     * than 0.
     *
     * @return True if the database natively supports returning
     * results starting at an offset position other than 0.
     */
    public boolean supportsNativeOffset()
    {
        return false;
    }

   /**
    * This method is for the SqlExpression.quoteAndEscape rules.  The rule is,
    * any string in a SqlExpression with a BACKSLASH will either be changed to
    * "\\" or left as "\".  SapDB does not need the escape character.
    *
    * @return true if the database needs to escape text in SqlExpressions.
    */

    public boolean escapeText()
    {
        return true;
    }

    /**
     * This method is used to check whether the database supports
     * limiting the size of the resultset.
     *
     * @return The limit style for the database.
     */
    public int getLimitStyle()
    {
        return LIMIT_STYLE_NONE;
    }

    /**
     * This method is used to format any date string.
     * Database can use different default date formats.
     *
     * @param date the Date to format
     * @return The proper date formatted String.
     */
    public String getDateString(Date date)
    {
        Timestamp ts = null;
        if (date instanceof Timestamp)
        {
            ts = (Timestamp) date;
        }
        else
        {
            ts = new Timestamp(date.getTime());
        }

        return ("{ts '" + ts + "'}");
    }

    /**
     * This method is used to format a boolean string.
     *
     * @param b the Boolean to format
     * @return The proper date formatted String.
     */
    public String getBooleanString(Boolean b)
    {
        return (Boolean.TRUE.equals(b) ? "1" : "0");
    }
}
