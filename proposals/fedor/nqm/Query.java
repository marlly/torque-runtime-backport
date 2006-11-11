package org.apache.turbine.util.db.nqm;

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
