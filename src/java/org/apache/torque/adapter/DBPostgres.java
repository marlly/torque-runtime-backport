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

/**
 * This is used to connect to PostgresQL databases.
 *
 * <a href="http://www.postgresql.org/">http://www.postgresql.org/</a>
 *
 * @author <a href="mailto:hakan42@gmx.de">Hakan Tandogan</a>
 * @version $Id$
 */
public class DBPostgres extends DB
{
    /**
     * Empty constructor.
     */
    protected DBPostgres()
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
        String s = new StringBuffer("UPPER(").append(in).append(")").toString();
        return s;
    }

    /**
     * This method is used to ignore case.
     *
     * @param in The string whose case to ignore.
     * @return The string in a case that can be ignored.
     */
    public String ignoreCase(String in)
    {
        String s = new StringBuffer("UPPER(").append(in).append(")").toString();
        return s;
    }

    /**
     * @see org.apache.torque.adapter.DB#getIDMethodType()
     */
    public String getIDMethodType()
    {
        return AUTO_INCREMENT;
    }

    /**
     * @param name The name of the field (should be of type
     *      <code>String</code>).
     * @return SQL to retreive the next database key.
     * @see org.apache.torque.adapter.DB#getIDMethodSQL(Object)
     */
    public String getIDMethodSQL(Object name)
    {
        return ("select currval('" + name + "')");
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
    }

    /**
     * This method is used to chek whether the database natively
     * supports limiting the size of the resultset.
     *
     * @return True.
     */
    public boolean supportsNativeLimit()
    {
        return true;
    }

    /**
     * This method is used to chek whether the database natively
     * supports returning results starting at an offset position other
     * than 0.
     *
     * @return True.
     */
    public boolean supportsNativeOffset()
    {
        return true;
    }

    /**
     * This method is used to chek whether the database supports
     * limiting the size of the resultset.
     *
     * @return LIMIT_STYLE_POSTGRES.
     */
    public int getLimitStyle()
    {
        return DB.LIMIT_STYLE_POSTGRES;
    }

    /**
     * Override the default behavior to associate b with null?
     *
     * @see DB#getBooleanString(Boolean)
     */
    public String getBooleanString(Boolean b)
    {
        return (b == null) ? "0" : (Boolean.TRUE.equals(b) ? "1" : "0");
    }
}
