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

/**
 * This DatabaseHandler is used when you do not have a database
 * installed.
 *
 * @author <a href="mailto:jon@clearink.com">Jon S. Stevens</a>
 * @author <a href="mailto:bmclaugh@algx.net">Brett McLaughlin</a>
 * @version $Id$
 */
public class DBNone extends AbstractDBAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -285009315025818009L;

    /**
     * Empty protected constructor.
     */
    protected DBNone()
    {
    }

    /**
     * @return null
     */
    public Connection getConnection()
    {
        return null;
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
}
