package org.apache.torque.util;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import junit.framework.TestCase;

/**
 * Tests for Query
 *
 * @author <a href="mailto:mpoeschl@marmot.at">Martin Poeschl</a>
 * @author <a href="mailto:fischer@seitenbau.de">Thomas Fischer</a>
 * @version $Id$
 */
public class QueryTest extends TestCase
{

    /**
     * Constructor for QueryTest.
     * @param arg0
     */
    public QueryTest(String arg0)
    {
        super(arg0);
    }

    /**
     * Test for String toString()
     */
    public void testColumns()
    {
        String expected
                = "SELECT tableA.column1, tableA.column2, tableB.column1 FROM ";
        Query query = new Query();

        UniqueList columns = new UniqueList();
        columns.add("tableA.column1");
        columns.add("tableA.column2");
        columns.add("tableB.column1");
        query.setSelectClause(columns);

        assertEquals(expected, query.toString());
    }

    /**
     * Test for String toString()
     */
    public void testToString()
    {
        String expected = "SELECT tableA.column1, tableA.column2, "
                + "tableB.column1 FROM tableA, tableB WHERE tableA.A = tableB.A"
                + " AND tableA.B = 1234";
        Query query = new Query();

        UniqueList columns = new UniqueList();
        columns.add("tableA.column1");
        columns.add("tableA.column2");
        columns.add("tableB.column1");
        query.setSelectClause(columns);

        UniqueList tables = new UniqueList();
        tables.add(new Query.FromElement("tableA", null, null));
        tables.add(new Query.FromElement("tableB", null, null));
        query.setFromClause(tables);

        UniqueList where = new UniqueList();
        where.add("tableA.A = tableB.A");
        where.add("tableA.B = 1234");
        query.setWhereClause(where);

        assertEquals(expected, query.toString());
    }
}
