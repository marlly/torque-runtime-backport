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

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

import org.apache.torque.TorqueException;
import org.apache.torque.util.Query;

/**
 * <code>DB</code> defines the interface for a Torque database
 * adapter.  Support for new databases is added by implementing this
 * interface. A couple of default settings is provided by
 * subclassing <code>AbstractDBAdapter</code>. The new database adapter
 * and its corresponding JDBC driver need to be registered in the service
 * configuration file.
 *
 * <p>The Torque database adapters exist to present a uniform
 * interface to database access across all available databases.  Once
 * the necessary adapters have been written and configured,
 * transparent swapping of databases is theoretically supported with
 * <i>zero code changes</i> and minimal configuration file
 * modifications.
 *
 * All database adapters need to be thread safe, as they are instantiated
 * only once fore a given configured database and may be accessed
 * simultaneously from several threads.
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
 * @author <a href="mailto:tv@apache.org">Thomas Vandahl</a>
 * @version $Id$
 */
public interface DB extends Serializable, IDMethod
{
    /** Database does not support limiting result sets.
     *  @deprecated This should not be exposed to the outside
     */
    int LIMIT_STYLE_NONE = 0;

    /** <code>SELECT ... LIMIT <limit>, [&lt;offset&gt;]</code>
     *  @deprecated This should not be exposed to the outside
     */
    int LIMIT_STYLE_POSTGRES = 1;

    /** <code>SELECT ... LIMIT [<offset>, ] &lt;offset&gt;</code>
     *  @deprecated This should not be exposed to the outside
     */
    int LIMIT_STYLE_MYSQL = 2;

    /** <code>SET ROWCOUNT &lt;offset&gt; SELECT ... SET ROWCOUNT 0</code>
     *  @deprecated This should not be exposed to the outside
     */
    int LIMIT_STYLE_SYBASE = 3;

    /** <code><pre>SELECT ... WHERE ... AND ROWNUM < <limit></pre></code>
     *  @deprecated This should not be exposed to the outside
     */
    int LIMIT_STYLE_ORACLE = 4;

    /** <code><pre>SELECT ... WHERE ... AND ROW_NUMBER() OVER() < <limit></pre></code>
     *  @deprecated This should not be exposed to the outside
     */
    int LIMIT_STYLE_DB2 = 5;

    /**
     * Key for the configuration which contains database adapters.
     */
    String ADAPTER_KEY = "adapter";

    /**
     * Key for the configuration which contains database drivers.
     */
    String DRIVER_KEY = "driver";

    /**
     * This method is used to ignore case.
     *
     * @param in The string to transform to upper case.
     * @return The upper case string.
     */
    String toUpperCase(String in);

    /**
     * Returns the character used to indicate the beginning and end of
     * a piece of text used in a SQL statement (generally a single
     * quote).
     *
     * @return The text delimeter.
     */
    char getStringDelimiter();

    /**
     * Returns the constant from the {@link
     * org.apache.torque.adapter.IDMethod} interface denoting which
     * type of primary key generation method this type of RDBMS uses.
     *
     * @return IDMethod constant
     */
    String getIDMethodType();

    /**
     * Returns SQL used to get the most recently inserted primary key.
     * Databases which have no support for this return
     * <code>null</code>.
     *
     * @param obj Information used for key generation.
     * @return The most recently inserted database key.
     */
    String getIDMethodSQL(Object obj);

    /**
     * Locks the specified table.
     *
     * @param con The JDBC connection to use.
     * @param table The name of the table to lock.
     * @throws SQLException No Statement could be created or executed.
     */
    void lockTable(Connection con, String table)
            throws SQLException;

    /**
     * Unlocks the specified table.
     *
     * @param con The JDBC connection to use.
     * @param table The name of the table to unlock.
     * @throws SQLException No Statement could be created or executed.
     */
    void unlockTable(Connection con, String table)
            throws SQLException;

    /**
     * Modifies a SQL snippet such that its case is ignored by the database.
     * The SQL snippet can be a column name (like AURHOR.NAME), an
     * quoted explicit sql string (like 'abc') or any other sql value (like a
     * number etc.).
     *
     * @param in The SQL snippet whose case to ignore.
     * @return The string in a case that can be ignored.
     */
    String ignoreCase(String in);

    /**
     * This method is used to ignore case in an ORDER BY clause.
     * Usually it is the same as ignoreCase, but some databases
     * (Interbase for example) does not use the same SQL in ORDER BY
     * and other clauses.
     *
     * @param in The string whose case to ignore.
     * @return The string in a case that can be ignored.
     */
    String ignoreCaseInOrderBy(String in);

    /**
     * This method is used to check whether the database natively
     * supports limiting the size of the resultset.
     *
     * @return True if the database natively supports limiting the
     * size of the resultset.
     */
    boolean supportsNativeLimit();

    /**
     * This method is used to check whether the database natively
     * supports returning results starting at an offset position other
     * than 0.
     *
     * @return True if the database natively supports returning
     * results starting at an offset position other than 0.
     */
    boolean supportsNativeOffset();

    /**
     * This method is used to generate the database specific query
     * extension to limit the number of record returned.
     *
     * @param query The query to modify
     * @param offset the offset Value
     * @param limit the limit Value
     *
     * @throws TorqueException if any error occurs when building the query
     */
    void generateLimits(Query query, int offset, int limit)
        throws TorqueException;

    /**
    * Whether backslashes (\) should be escaped in explicit SQL strings.
    * If true is returned, a BACKSLASH will be changed to "\\". If false
    * is returned, a BACKSLASH will be left as "\".
    *
    * @return true if the database needs to escape backslashes
    *         in SqlExpressions.
    */

    boolean escapeText();

    /**
     * This method is used to check whether the database supports
     * limiting the size of the resultset.
     *
     * @return The limit style for the database.
     * @deprecated This should not be exposed to the outside
     */
    int getLimitStyle();

    /**
     * This method is used to format any date string.
     * Database can use different default date formats.
     *
     * @param date the Date to format
     * @return The proper date formatted String.
     */
    String getDateString(Date date);

    /**
     * This method is used to format a boolean string.
     *
     * @param b the Boolean to format
     * @return The proper date formatted String.
     */
    String getBooleanString(Boolean b);

    /**
     * Whether ILIKE should be used for case insensitive like clauses.
     *
     * @return true if ilike should be used for case insensitive likes,
     *         false if ignoreCase should be applied to the compared strings.
     */
    boolean useIlike();

    /**
     * Whether an escape clause in like should be used.
     * Example : select * from AUTHOR where AUTHOR.NAME like '\_%' ESCAPE '\';
     *
     * @return whether the escape clause should be appended or not.
     */
    boolean useEscapeClauseForLike();
}
