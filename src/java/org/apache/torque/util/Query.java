package org.apache.torque.util;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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

import org.apache.commons.collections.StringStack;

/**
 * Used to assemble an SQL SELECT query.  Attributes exist for the
 * sections of a SELECT: modifiers, columns, from clause, where
 * clause, and order by clause.  The various parts of the query are
 * appended to buffers which only accept unique entries.  This class
 * is used primarily by BasePeer.
 *
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @author <a href="mailto:sam@neurogrid.com">Sam Joseph</a>
 * @version $Id$
 */
public class Query
{
    private static final String SELECT = "SELECT ";
    private static final String FROM = " FROM ";
    private static final String WHERE = " WHERE ";
    private static final String AND = " AND ";
    private static final String OR = " OR ";
    private static final String ORDER_BY = " ORDER BY ";
    private static final String GROUP_BY = " GROUP BY ";
    private static final String HAVING = " HAVING ";
    private static final String IN = " IN ";
    private static final String BETWEEN = " BETWEEN ";
    private static final String LIMIT = " LIMIT ";
    private static final String ROWCOUNT = " SET ROWCOUNT ";

    private StringStack selectModifiers = new StringStack();
    private StringStack selectColumns = new StringStack();
    private StringStack fromTables = new StringStack();
    private StringStack whereCriteria = new StringStack();
    private StringStack orderByColumns = new StringStack();
    private StringStack groupByColumns = new StringStack();
    private String having;
    private String limit;
    private String rowcount;

    /**
     * Retrieve the modifier buffer in order to add modifiers to this
     * query.  E.g. DISTINCT and ALL.
     *
     * @return A StringStack used to add modifiers.
     */
    public StringStack getSelectModifiers()
    {
        return selectModifiers;
    }

    /**
     * Retrieve the columns buffer in order to specify which columns
     * are returned in this query.
     *
     *
     * @return A StringStack used to add columns to be selected.
     */
    public StringStack getSelectClause()
    {
        return selectColumns;
    }

    /**
     * Retrieve the from buffer in order to specify which tables are
     * involved in this query.
     *
     *
     * @return A StringStack used to add tables involved in the
     * query.
     */
    public StringStack getFromClause()
    {
        return fromTables;
    }

    /**
     * Retrieve the where buffer in order to specify the selection
     * criteria E.g. column_a=3.  Expressions added to the buffer will
     * be separated using AND.
     *
     * @return A StringStack used to add selection criteria.
     */
    public StringStack getWhereClause()
    {
        return whereCriteria;
    }

    /**
     * Retrieve the order by columns buffer in order to specify which
     * columns are used to sort the results of the query.
     *
     * @return A StringStack used to add columns to sort on.
     */
    public StringStack getOrderByClause()
    {
        return orderByColumns;
    }

    /**
     * Retrieve the group by columns buffer in order to specify which
     * columns are used to group the results of the query.
     *
     * @return A StringStack used to add columns to group on.
     */
    public StringStack getGroupByClause()
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
        if ( rowcount != null )
            stmt.append(ROWCOUNT)
                .append(rowcount)
                .append(" ");
        stmt.append(SELECT)
            .append(selectModifiers.toString(" "))
            .append(selectColumns.toString(", "))
            .append(FROM)
            .append(fromTables.toString(", "));
        if ( !whereCriteria.empty() )
            stmt.append(WHERE)
                .append(whereCriteria.toString( AND ));
        if ( !orderByColumns.empty() )
            stmt.append(ORDER_BY)
                .append(orderByColumns.toString(", "));
        if ( !groupByColumns.empty() )
            stmt.append(GROUP_BY)
                .append(groupByColumns.toString(", "));
        if ( having != null )
            stmt.append(HAVING)
                .append(having);
        if ( limit != null )
            stmt.append(LIMIT)
                .append(limit);
        if ( rowcount != null )
            stmt.append(ROWCOUNT)
                .append("0");
        return stmt.toString();
    }
}
