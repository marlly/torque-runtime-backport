package org.apache.turbine.util.db.nqm;

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

// Java Core Classes
import java.util.Set;
import java.util.HashSet;
import java.util.Vector;

// Turbine Classes
import org.apache.turbine.util.db.adapter.DB;
import org.apache.turbine.util.StringStackBuffer;

/**
 *
 * @author <a href="mailto:fedor.karpelevitch@home.com">Fedor Karpelevitch</a>
 * @version $Id$
 */
public class Query extends Statement
{
    public static final String SELECT = SQLConstants.SELECT;
    public static final String COMMA = SQLConstants.COMMA;
    public static final String ASTERISK = SQLConstants.ASTERISK;
    public static final String DOT = SQLConstants.DOT;
    public static final String ORDER_BY  = SQLConstants.ORDER_BY;

    private Condition condition = null;
    private Set tables = new HashSet();
    private Vector selectColumns = new Vector();
    private String selectTable = null;
    private Vector orderBys = new Vector();

    public Query()
    {
        super();
    }

    public Query(Condition condition)
    {
        super(condition);
    }

    public Query setSelectTable(String selectTable)
    {
        this.selectTable = selectTable;
        return this;
    }

    public Query addSelectColumn(String selectColumn)
    {
        this.selectColumns.add(selectColumn);
        return this;
    }

    public Query addOrderByColumn(String column)
    {
        return addOrderByColumn(new OrderBy(column));
    }

    public Query addOrderByColumn(String column, String direction)
    {
        return addOrderByColumn(new OrderBy(column, direction));
    }

    public Query addOrderByColumn(OrderBy orderBy)
    {
        orderBys.add(orderBy);
        return this;
    }

    public StringStackBuffer toSQL(DB db)
    {
        StringStackBuffer query = new StringStackBuffer();
        query.add(SELECT);
        if (!selectColumns.isEmpty())
        {
            query.add((String)selectColumns.get(0));
            for (int i=1; i<selectColumns.size(); i++)
            {
                query.add(COMMA)
                    .add((String)selectColumns.get(i));
            }
        }
        else
        {
            if (selectTable!=null)
            {
                query.add(selectTable).add(DOT);
            }
            query.add(ASTERISK);
        }
        query.addAll(fromClause(db));
        query.addAll(whereClause(db));
        if (!orderBys.isEmpty())
        {
            query.add(ORDER_BY).addAll(((OrderBy)orderBys.get(0)).toSQL(db));
            for(int i=1; i<orderBys.size(); i++)
            {
                query.add(COMMA).addAll(((OrderBy)orderBys.get(i)).toSQL(db));
            }
        }
        return query;
    }

    public class OrderBy implements SQLCode
    {
        public static final String ASC = SQLConstants.ASC;
        public static final String DESC = SQLConstants.DESC;

        private String column;
        private String direction;

        public OrderBy(String column)
        {
            this(column, ASC);
        }

        public OrderBy(String column, String direction)
        {
            this.column = column;
            this.direction = direction;
        }

        public StringStackBuffer toSQL(DB db)
        {
            return new StringStackBuffer().add(column).add(direction);
        }
    }
}
