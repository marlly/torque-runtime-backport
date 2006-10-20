package org.apache.torque.adapter;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;

import org.apache.torque.util.Query;
import org.apache.torque.util.UniqueList;

/**
 * This code should be used for an Oracle database pool.
 *
 * @author <a href="mailto:jon@clearink.com">Jon S. Stevens</a>
 * @author <a href="mailto:bmclaugh@algx.net">Brett McLaughlin</a>
 * @author <a href="mailto:bschneider@vecna.com">Bill Schneider</a>
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @version $Id$
 */
public class DBOracle extends AbstractDBAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 8966976210230241194L;

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
     * This method is used to check whether the database supports
     * limiting the size of the resultset.
     *
     * @return LIMIT_STYLE_ORACLE.
     * @deprecated This should not be exposed to the outside
     */
    public int getLimitStyle()
    {
        return DB.LIMIT_STYLE_ORACLE;
    }

    /**
     * Return true for Oracle
     * @see org.apache.torque.adapter.AbstractDBAdapter#supportsNativeLimit()
     */
    public boolean supportsNativeLimit()
    {
        return true;
    }

    /**
     * Return true for Oracle
     * @see org.apache.torque.adapter.AbstractDBAdapter#supportsNativeOffset()
     */
    public boolean supportsNativeOffset()
    {
        return true;
    }

    /**
     * Build Oracle-style query with limit or offset.
     * If the original SQL is in variable: query then the requlting
     * SQL looks like this:
     * <pre>
     * SELECT B.* FROM (
     *          SELECT A.*, rownum as TORQUE$ROWNUM FROM (
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
        .append("SELECT A.*, rownum AS TORQUE$ROWNUM FROM ( ");

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
        
        // the query must not contain same column names or aliases.
        // Find double column names and aliases and create unique aliases
        // TODO: does not work for functions yet
        UniqueList selectColumns = query.getSelectClause();
        int replacementSuffix = 0;
        Set columnNames = new HashSet();
        // first pass: only remember aliased columns
        // No replacements need to take place because double aliases
        // are not allowed anyway
        // So alias names will be retained 
        for (ListIterator columnIt = selectColumns.listIterator();
                columnIt.hasNext(); )
        {
            String selectColumn = (String) columnIt.next();

            // check for sql function
            if ((selectColumn.indexOf('(') != -1)
                || (selectColumn.indexOf(')') != -1))
            {
                // Sql function. Disregard.
                continue;
            }

            // check if alias name exists
            int spacePos = selectColumn.lastIndexOf(' ');
            if (spacePos == -1)
            {
                // no alias, disregard for now
                continue;
            }

            String aliasName = selectColumn.substring(spacePos + 1);
            columnNames.add(aliasName);
        }

        // second pass. Regard ordinary columns only
        for (ListIterator columnIt = selectColumns.listIterator();
                columnIt.hasNext(); )
        {
            String selectColumn = (String) columnIt.next();

            // check for sql function
            if ((selectColumn.indexOf('(') != -1)
                || (selectColumn.indexOf(')') != -1))
            {
                // Sql function. Disregard.
                continue;
            }

            {
                int spacePos = selectColumn.lastIndexOf(' ');
                if (spacePos != -1)
                {
                    // alias, already processed in first pass
                    continue;
                }
            }
            // split into column name and tableName 
            String column;
            {
                int dotPos = selectColumn.lastIndexOf('.');
                if (dotPos != -1)
                {
                    column = selectColumn.substring(dotPos + 1);
                }
                else
                {
                    column = selectColumn;
                }
            }
            if (columnNames.contains(column))
            {
                // column needs to be aliased
                // get replacement name
                String aliasName;
                do
                {
                    aliasName = "a" + replacementSuffix;
                    ++replacementSuffix;
                }
                while (columnNames.contains(aliasName));
                
                selectColumn = selectColumn + " " + aliasName;
                columnIt.set(selectColumn);
                columnNames.add(aliasName);
            }
            else
            {
                columnNames.add(column);
            }
        }
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
