package org.apache.torque.util;

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

import org.apache.commons.lang.StringUtils;

/**
 * Used to assemble an SQL SELECT query.  Attributes exist for the
 * sections of a SELECT: modifiers, columns, from clause, where
 * clause, and order by clause.  The various parts of the query are
 * appended to buffers which only accept unique entries.  This class
 * is used primarily by BasePeer.
 *
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @author <a href="mailto:sam@neurogrid.com">Sam Joseph</a>
 * @author <a href="mailto:mpoeschl@marmot.at">Martin Poeschl</a>
 * @version $Id$
 */
public class Query
{
    private static final String SELECT = "SELECT ";
    private static final String FROM = " FROM ";
    private static final String WHERE = " WHERE ";
    private static final String AND = " AND ";
    private static final String ORDER_BY = " ORDER BY ";
    private static final String GROUP_BY = " GROUP BY ";
    private static final String HAVING = " HAVING ";
    private static final String LIMIT = " LIMIT ";
    private static final String ROWCOUNT = " SET ROWCOUNT ";

    private UniqueList selectModifiers = new UniqueList();
    private UniqueList selectColumns = new UniqueList();
    private UniqueList fromTables = new UniqueList();
    private UniqueList whereCriteria = new UniqueList();
    private UniqueList orderByColumns = new UniqueList();
    private UniqueList groupByColumns = new UniqueList();
    private String having;
    private String limit;
    private String rowcount;

    /**
     * Retrieve the modifier buffer in order to add modifiers to this
     * query.  E.g. DISTINCT and ALL.
     *
     * @return An UniqueList used to add modifiers.
     */
    public UniqueList getSelectModifiers()
    {
        return selectModifiers;
    }

    /**
     * Set the modifiers. E.g. DISTINCT and ALL.
     *
     * @param modifiers the modifiers
     */
    public void setSelectModifiers(UniqueList modifiers)
    {
        selectModifiers = modifiers;
    }

    /**
     * Retrieve the columns buffer in order to specify which columns
     * are returned in this query.
     *
     *
     * @return An UniqueList used to add columns to be selected.
     */
    public UniqueList getSelectClause()
    {
        return selectColumns;
    }

    /**
     * Set the columns.
     *
     * @param columns columns list
     */
    public void setSelectClause(UniqueList columns)
    {
        selectColumns = columns;
    }

    /**
     * Retrieve the from buffer in order to specify which tables are
     * involved in this query.
     *
     * @return An UniqueList used to add tables involved in the query.
     */
    public UniqueList getFromClause()
    {
        return fromTables;
    }

    /**
     * Set the from clause.
     *
     * @param tables the tables
     */
    public void setFromClause(UniqueList tables)
    {
        fromTables = tables;
    }

    /**
     * Retrieve the where buffer in order to specify the selection
     * criteria E.g. column_a=3.  Expressions added to the buffer will
     * be separated using AND.
     *
     * @return An UniqueList used to add selection criteria.
     */
    public UniqueList getWhereClause()
    {
        return whereCriteria;
    }

    /**
     * Set the where clause.
     *
     * @param where where clause
     */
    public void setWhereClause(UniqueList where)
    {
        whereCriteria = where;
    }

    /**
     * Retrieve the order by columns buffer in order to specify which
     * columns are used to sort the results of the query.
     *
     * @return An UniqueList used to add columns to sort on.
     */
    public UniqueList getOrderByClause()
    {
        return orderByColumns;
    }

    /**
     * Retrieve the group by columns buffer in order to specify which
     * columns are used to group the results of the query.
     *
     * @return An UniqueList used to add columns to group on.
     */
    public UniqueList getGroupByClause()
    {
        return groupByColumns;
    }

    /**
     * Set the having clause.  This is used to restrict which rows
     * are returned.
     *
     * @param having A String.
     */
    public void setHaving(String having)
    {
        this.having = having;
    }

    /**
     * Set the limit number.  This is used to limit the number of rows
     * returned by a query, and the row where the resultset starts.
     *
     * @param limit A String.
     */
    public void setLimit(String limit)
    {
        this.limit = limit;
    }

    /**
     * Set the rowcount number.  This is used to limit the number of
     * rows returned by Sybase and MS SQL/Server.
     *
     * @param rowcount A String.
     */
    public void setRowcount(String rowcount)
    {
        this.rowcount = rowcount;
    }

    /**
     * Get the having clause.  This is used to restrict which
     * rows are returned based on some condition.
     *
     * @return A String that is the having clause.
     */
    public String getHaving()
    {
        return having;
    }

    /**
     * Get the limit number.  This is used to limit the number of
     * returned by a query in Postgres.
     *
     * @return A String with the limit.
     */
    public String getLimit()
    {
        return limit;
    }

    /**
     * Get the rowcount number.  This is used to limit the number of
     * returned by a query in Sybase and MS SQL/Server.
     *
     * @return A String with the row count.
     */
    public String getRowcount()
    {
        return rowcount;
    }

    /**
     * Outputs the query statement.
     *
     * @return A String with the query statement.
     */
    public String toString()
    {
        StringBuffer stmt = new StringBuffer();
        if (rowcount != null)
        {
            stmt.append(ROWCOUNT)
                .append(rowcount)
                .append(" ");
        }
        stmt.append(SELECT)
            .append(StringUtils.join(selectModifiers.iterator(), " "))
            .append(StringUtils.join(selectColumns.iterator(), ", "))
            .append(FROM)
            .append(StringUtils.join(fromTables.iterator(), ", "));
        if (!whereCriteria.isEmpty())
        {
            stmt.append(WHERE)
                .append(StringUtils.join(whereCriteria.iterator(), AND));
        }
        if (!groupByColumns.isEmpty())
        {
            stmt.append(GROUP_BY)
                .append(StringUtils.join(groupByColumns.iterator(), ", "));
        }
        if (having != null)
        {
            stmt.append(HAVING)
                .append(having);
        }
        if (!orderByColumns.isEmpty())
        {
            stmt.append(ORDER_BY)
                .append(StringUtils.join(orderByColumns.iterator(), ", "));
        }
        if (limit != null)
        {
            stmt.append(LIMIT)
                .append(limit);
        }
        if (rowcount != null)
        {
            stmt.append(ROWCOUNT)
                .append("0");
        }
        return stmt.toString();
    }
}
