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
 * This code should be used for an Informix database pool.
 *
 * @author <a href="mailto:jon@latchkey.com">Jon S. Stevens</a>
 * @author <a href="mailto:bpm@ec-group.com">Brian P Millett</a>
 * @version $Id$
 */
public class DBInformix extends AbstractDBAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 2599963509284952957L;

    /**
     * Empty constructor.
     */
    protected DBInformix()
    {
    }

    /**
     * This method is used to ignore case.  Problem is that Informix
     * does not have an UPPER function.  So the best would be to do
     * nothing.
     *
     * @param in The string to transform to upper case.
     * @return The upper case string.
     */
    public String toUpperCase(String in)
    {
        return in;
    }

    /**
     * This method is used to ignore case.  Problem is that Informix
     * does not have an UPPER function.  So the best would be to do
     * nothing.
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
     * The method is used to lock a table.
     *
     * @param con The JDBC connection to use.
     * @param table The name of the table to lock.
     * @exception SQLException No Statement could be created or executed.
     */
    public void lockTable(Connection con, String table) throws SQLException
    {
        Statement statement = con.createStatement();

        StringBuffer stmt = new StringBuffer();
        stmt.append("LOCK TABLE ")
        .append(table)
        .append(" IN EXCLUSIVE MODE");

        statement.executeQuery(stmt.toString());
    }

    /**
     * The method is used to unlock a table.
     *
     * @param con The JDBC connection to use.
     * @param table The name of the table to unlock.
     * @exception SQLException No Statement could be created or executed.
     */
    public void unlockTable(Connection con, String table) throws SQLException
    {
        // Tables in Informix are unlocked when a commit is issued.
        // The user may have issued a commit but do it here to be
        // sure.
        con.commit();
    }
}
