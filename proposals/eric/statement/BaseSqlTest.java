package org.apache.turbine.util.db.statement;

/*
 * Copyright 2003-2004 The Apache Software Foundation.
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

import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.turbine.test.BaseTestCase;
import org.apache.turbine.util.db.statement.BaseSql;

/**
 * Test class for BaseSql.
 *
 * @author <a href="mailto:eric@dobbse.net">Eric Dobbs</a>
 * @version $Id$
 */
public class BaseSqlTest extends BaseTestCase
{
    BaseSql s;

    /**
     * Creates a new instance.
     */
    public BaseSqlTest(String name)
    {
        super(name);

        s = new BaseSql();
    }

    /**
     * Creates a test suite for this class.
     *
     * @return A test suite for this class.
     */
    public static Test suite()
    {
        return new TestSuite(BaseSqlTest.class);
    }

    public void testLeftMiddleRight()
    {
        String result = s.leftMiddleRight("set (",
                                          "column=30",
                                          ")");
        String expect = "set (column=30)";
        assert(result.equals(expect));
    }

    public void testLeftRightListConnector()
    {
        ArrayList list = new ArrayList(3);
        list.add("columnA");
        list.add("columnB");
        list.add("columnC");
        String result = s.leftRightListConnector("select ",
                                                 "",
                                                 list,
                                                 ", ");
        String expect = "select columnA, columnB, columnC";
        assert(result.equals(expect));
    }

    public void testGetEquals()
    {
        String result = s.getEquals("table.column",
                                    "25");
        String expect = "(table.column=25)";
        assert(result.equals(expect));
    }

    public void testGetNotEquals()
    {
        String result = s.getNotEquals("table.column",
                                       "25");
        String expect = "(table.column!=25)";
        assert(result.equals(expect));
    }

    public void testGetLessThan()
    {
        String result = s.getLessThan("table.column",
                                      "25");
        String expect = "(table.column<25)";
        assert(result.equals(expect));
    }

    public void testGetGreaterThan()
    {
        String result = s.getGreaterThan("table.column",
                                         "25");
        String expect = "(table.column>25)";
        assert(result.equals(expect));
    }

    public void testGetLessEqualThan()
    {
        String result = s.getLessEqual("table.column",
                                       "25");
        String expect = "(table.column<=25)";
        assert(result.equals(expect));
    }

    public void testGetGreaterEqual()
    {
        String result = s.getGreaterEqual("table.column",
                                          "25");
        String expect = "(table.column>=25)";
        assert(result.equals(expect));
    }

    public void testGetIsNull()
    {
        String result = s.getIsNull("this");
        String expect = "(this IS NULL)";
        assert(result.equals(expect));
    }

    public void testGetIn()
    {
        ArrayList list = new ArrayList();
        list.add("'foo'");
        list.add("'bar'");
        list.add("'baz'");
        String result = s.getIn("this",list);
        String expect = "(this IN ('foo', 'bar', 'baz'))";
        assert(result.equals(expect));
    }

    public void testGetNotIn()
    {
        ArrayList list = new ArrayList();
        list.add("'foo'");
        list.add("'bar'");
        list.add("'baz'");
        String result = s.getNotIn("this",list);
        String expect = "(this NOT IN ('foo', 'bar', 'baz'))";
        assert(result.equals(expect));
    }

    public void testGetLike()
    {
        String result = s.getLike("this",
                                  "'that%'");
        String expect = "(this LIKE 'that%')";
        assert(result.equals(expect));
    }

    public void testGetAnd()
    {
        String result = s.getAnd("this",
                                 "that");
        String expect = "(this AND that)";
        assert(result.equals(expect));
    }

    public void testGetOr()
    {
        String result = s.getOr("this",
                                "that");
        String expect = "(this OR that)";
        assert(result.equals(expect));
    }

    public void testGetNot()
    {
        String result = s.getNot("this");
        String expect = "( NOT this)";
        assert(result.equals(expect));
    }

    public void testGetAscending()
    {
        String result = s.getAscending("this");
        String expect = "this ASC";
        assert(result.equals(expect));
    }

    public void testGetDescending()
    {
        String result = s.getDescending("this");
        String expect = "this DESC";
        assert(result.equals(expect));
    }

    public void testGetInsert()
    {
        ArrayList list = new ArrayList();
        list.add("column1");
        list.add("column2");
        list.add("column3");
        String result = s.getInsert("table",list);
        String expect = "INSERT INTO table (column1, column2, column3)";
        assert(result.equals(expect));
    }

    public void testGetValues()
    {
        ArrayList list = new ArrayList();
        list.add(s.quoteAndEscapeText("value1"));
        list.add(s.quoteAndEscapeText("value2"));
        list.add(s.quoteAndEscapeText("value3"));
        String result = s.getValues(list);
        String expect = " VALUES ('value1', 'value2', 'value3')";
        assert(result.equals(expect));
    }


    public void testGetUpdate()
    {
        ArrayList list = new ArrayList();
        list.add(s.getSet("column1","value1"));
        list.add(s.getSet("column2","value2"));
        String result = s.getUpdate("table",list);
        String expect = "UPDATE table SET column1=value1, SET column2=value2";
        assert(result.equals(expect));
    }

    public void testGetSet()
    {
        String result = s.getSet("column","value");
        String expect = "SET column=value";
        assert(result.equals(expect));
    }

    public void testGetDelete()
    {
        String result = s.getDelete("table");
        String expect = "DELETE FROM table";
        assert(result.equals(expect));
    }

    public void testGetSelect()
    {
        ArrayList list = new ArrayList(3);
        list.add("columnA");
        list.add("columnB");
        list.add("columnC");
        String result = s.getSelect(list);
        String expect = "SELECT columnA, columnB, columnC";
        assert(result.equals(expect));
    }

    public void testGetFrom()
    {
        ArrayList list = new ArrayList(3);
        list.add("tableA");
        list.add("tableB");
        list.add("tableC");
        String result = s.getFrom(list);
        String expect = " FROM tableA, tableB, tableC";
        assert(result.equals(expect));
    }


    public void testGetWhere()
    {
        String result = s.getWhere("(column like '%foo%')");
        String expect = " WHERE (column like '%foo%')";
        assert(result.equals(expect));
    }

    public void testGetOrderBy()
    {
        ArrayList list = new ArrayList();
        list.add(s.getAscending("column1"));
        list.add(s.getDescending("column2"));
        list.add("column3");
        String result = s.getOrderBy(list);
        String expect = " ORDER BY column1 ASC, column2 DESC, column3";
        assert(result.equals(expect));
    }

    public void testGetGroupBy()
    {
        ArrayList list = new ArrayList();
        list.add(s.getAscending("column1"));
        list.add(s.getDescending("column2"));
        list.add("column3");
        String result = s.getGroupBy(list);
        String expect = " GROUP BY column1 ASC, column2 DESC, column3";
        assert(result.equals(expect));
    }

    public void testGetHaving()
    {
        String result = s.getHaving("(sum(column)>100)");
        String expect = " HAVING (sum(column)>100)";
        assert(result.equals(expect));
    }

    public void testGetCount()
    {
        String result = s.getCount("*");
        String expect = "COUNT(*)";
        assert(result.equals(expect));
    }

    public void testGetMin()
    {
        String result = s.getMin("table.column");
        String expect = "MIN(table.column)";
        assert(result.equals(expect));
    }

    public void testGetMax()
    {
        String result = s.getMax("table.column");
        String expect = "MAX(table.column)";
        assert(result.equals(expect));
    }

    public void testGetAvg()
    {
        String result = s.getAvg("table.column");
        String expect = "AVG(table.column)";
        assert(result.equals(expect));
    }

    public void testGetSum()
    {
        String result = s.getSum("table.column");
        String expect = "SUM(table.column)";
        assert(result.equals(expect));
    }

    public void testGetUpper()
    {
        String result = s.getUpper("table.column");
        String expect = "UPPER(table.column)";

        System.out.println("expect: " + expect);
        System.out.println("result: " + result);

        assert(result.equals(expect));
    }

    public void testQuoteAndEscapeText()
    {
        String result = s.quoteAndEscapeText("O'Malley's Can't be beat!");
        String expect = "'O''Malley''s Can''t be beat!'";
        assert(result.equals(expect));
    }

    public void testNestedComparison()
    {
        String result = s.getOr(
            s.getAnd(
                s.getGreaterThan("table.columnA","10"),
                s.getLessEqual("table.columnA","50")),
            s.getAnd(
                s.getGreaterThan("table.columnB","37"),
                s.getLessEqual("table.columnB","42")));
        String expect =
            "(((table.columnA>10) AND (table.columnA<=50))"
            + " OR ((table.columnB>37) AND (table.columnB<=42)))";
        assert(result.equals(expect));
    }

    public void testGetWhereWithComposite()
    {
        String result = s.getWhere(
            s.getOr(
                s.getAnd(
                    s.getGreaterEqual("columnA","25"),
                    s.getLessEqual("columnA","50")
                ),
                s.getAnd(
                    s.getGreaterEqual("columnB","10"),
                    s.getLessEqual("columnB","20")
                )
            )
        );

        String expect = " WHERE (((columnA>=25) AND (columnA<=50))"
            + " OR ((columnB>=10) AND (columnB<=20)))";

        System.out.println("expect: " + expect);
        System.out.println("result: " + result);

        assert(result.equals(expect));
    }
}
