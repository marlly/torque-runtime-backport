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

import java.util.Date;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * <code>DB</code> defines the interface for a Turbine database
 * adapter.  Support for new databases is added by subclassing
 * <code>DB</code> and implementing its abstract interface, and by
 * registering the new database adapter and its corresponding
 * JDBC driver in the service configuration file.
 *
 * <p>The Turbine database adapters exist to present a uniform
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
     * @see #getIDMethodSQL(Object obj)
     * @deprecated Use getIDMethodSQL(Object) instead.
     */
    public String getIdSqlForAutoIncrement(Object obj)
    {
        return getIDMethodSQL(obj);
    }

    /**
     * @see #getIDMethodSQL(Object obj)
     * @deprecated Use getIDMethodSQL(Object) instead.
     */
    public String getSequenceSql(Object obj)
    {
        return getIDMethodSQL(obj);
    }

    /**
     * Locks the specified table.
     *
     * @param con The JDBC connection to use.
     * @param table The name of the table to lock.
     * @exception SQLException
     */
    public abstract void lockTable(Connection con,
                                   String table)
        throws SQLException;

    /**
     * Unlocks the specified table.
     *
     * @param con The JDBC connection to use.
     * @param table The name of the table to unlock.
     * @exception SQLException
     */
    public abstract void unlockTable(Connection con,
                                     String table)
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

    /* *
     * Sets the JDBC driver used by this adapter.
     *
     * @param newDriver The fully-qualified class name of the JDBC
     * driver to use.
     * /
    public void setJDBCDriver(String newDriver)
    {
        JDBCDriver = newDriver;
    }

    /**
     * This method is used to chek whether writing large objects to
     * the DB requires a transaction.  Since this is only true for
     * Postgres, only the DBPostgres needs to override this method and
     * return true.
     *
     * @return True if writing large objects to the DB requires a transaction.
     * @deprecated The hack involving an oid mapping for VARBINARY
     * which necessitated use of this method for Postgres has been
     * obviated by use of the Postgres bytea data type.
     */
    public boolean objectDataNeedsTrans()
    {
        return false;
    }

    /**
     * This method is used to chek whether the database natively
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
     * This method is used to chek whether the database natively
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
     * This method is used to chek whether the database supports
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
     * @return The proper date formated String.
     * @deprecated use getDateString(java.util.Date)
     */
    public String getDateString(String dateString)
    {
        return getStringDelimiter() + dateString + getStringDelimiter();
    }

    /**
     * This method is used to format any date string.
     * Database can use different default date formats.
     *
     * @return The proper date formatted String.
     */
    public String getDateString(Date date)
    {
        Timestamp ts = null;
        if ( date instanceof Timestamp )
        {
            ts = (Timestamp)date;
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
     * @return The proper date formatted String.
     */
    public String getBooleanString(Boolean b)
    {
        return b.equals(Boolean.TRUE) ? "1" : "0";
    }
}
