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

import org.apache.torque.util.Query;

/**
 * This is used to connect via the Application-Driver to DB2
 * databases.
 *
 * <a href="http://www-306.ibm.com/software/data/db2/">http://www-306.ibm.com/software/data/db2/</a>
 *
 * @author <a href="mailto:hakan42@gmx.de">Hakan Tandogan</a>
 * @author <a href="mailto:vido@ldh.org">Augustin Vidovic</a>
 * @version $Id$
 */
public class DBDB2App extends AbstractDBAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -3097347241360840675L;

    /**
     * Empty constructor.
     */
    protected DBDB2App()
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

    /**
     * This method is used to check whether the database supports
     * limiting the size of the resultset.
     *
     * @return LIMIT_STYLE_DB2.
     * @deprecated This should not be exposed to the outside
     */
    public int getLimitStyle()
    {
        return DB.LIMIT_STYLE_DB2;
    }

    /**
     * Return true for DB2
     * @see org.apache.torque.adapter.AbstractDBAdapter#supportsNativeLimit()
     */
    public boolean supportsNativeLimit()
    {
        return true;
    }

    /**
     * Return true for DB2
     * @see org.apache.torque.adapter.AbstractDBAdapter#supportsNativeOffset()
     */
    public boolean supportsNativeOffset()
    {
        return true;
    }

    /**
     * Build DB2 (OLAP) -style query with limit or offset.
     * If the original SQL is in variable: query then the requlting
     * SQL looks like this:
     * <pre>
     * SELECT B.* FROM (
     *          SELECT A.*, row_number() over() as TORQUE$ROWNUM FROM (
     *                  query
     *          ) A
     *     ) B WHERE B.TORQUE$ROWNUM > offset AND B.TORQUE$ROWNUM
     *     <= offset + limit
     * </pre>
     *
     * @param query The query to modify
     * @param offset the offset Value
     * @param limit the limit Value
     */
    public void generateLimits(Query query, int offset, int limit)
    {
        StringBuffer preLimit = new StringBuffer()
        .append("SELECT B.* FROM ( ")
        .append("SELECT A.*, row_number() over() AS TORQUE$ROWNUM FROM ( ");

        StringBuffer postLimit = new StringBuffer()
                .append(" ) A ")
                .append(" ) B WHERE ");

        if (offset > 0)
        {
            postLimit.append(" B.TORQUE$ROWNUM > ")
                    .append(offset);

            if (limit >= 0)
            {
                postLimit.append(" AND B.TORQUE$ROWNUM <= ")
                        .append(offset + limit);
            }
        }
        else
        {
            postLimit.append(" B.TORQUE$ROWNUM <= ")
                    .append(limit);
        }

        query.setPreLimit(preLimit.toString());
        query.setPostLimit(postLimit.toString());
        query.setLimit(null);
    }
}
