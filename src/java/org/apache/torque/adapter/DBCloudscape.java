package org.apache.torque.adapter;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.StringTokenizer;

/**
 * This is used to connect to Cloudscape SQL databases.
 *
 * @author <a href="mailto:taylor@apache.org">David Sean Taylor</a>
 * @version $Id$
 */
public class DBCloudscape extends AbstractDBAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -7475830417640153351L;

    /** qualifier */
    private static final String QUALIFIER = ".";

    /**
     * Constructor.
     */
    protected DBCloudscape()
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
        return AUTO_INCREMENT;
    }

    /**
     * @see org.apache.torque.adapter.DB#getIDMethodSQL(Object obj)
     */
    public String getIDMethodSQL(Object obj)
    {
        StringBuffer sql = new StringBuffer(132);
        sql.append("select distinct ConnectionInfo.lastAutoincrementValue(");

        String qualifiedIdentifier = (String) obj;

        StringTokenizer tokenizer = new StringTokenizer(qualifiedIdentifier,
                QUALIFIER);
        int count = tokenizer.countTokens();

        String schema, table, column;

        System.out.println("qi = " + qualifiedIdentifier);
        // no qualifiers, its simply a column name
        switch (count)
        {
        case 0:
            return ""; // not valid -- we need the column name and table name
        case 1:
            return ""; // not valid -- we need the table name to select from

        case 2:
            table = tokenizer.nextToken();
            column = tokenizer.nextToken();
            sql.append("'APP', '");
            sql.append(table);
            break;

        case 3:
            schema = tokenizer.nextToken();
            table = tokenizer.nextToken();
            column = tokenizer.nextToken();
            sql.append("'");
            sql.append(schema);
            sql.append("', '");
            sql.append(table);
            break;

        default:
            return ""; // not valid
        }

        sql.append("', '");
        sql.append(column);
        sql.append("') FROM ");
        sql.append(table);

        System.out.println(sql.toString());
        return sql.toString();
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
