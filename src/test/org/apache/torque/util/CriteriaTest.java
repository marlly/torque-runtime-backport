package org.apache.torque.util;

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

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.SerializationUtils;
import org.apache.torque.BaseTestCase;
import org.apache.torque.Torque;
import org.apache.torque.TorqueException;
import org.apache.torque.adapter.DBFactory;
import org.apache.torque.map.ColumnMap;
import org.apache.torque.map.DatabaseMap;
import org.apache.torque.map.TableMap;
import org.apache.torque.util.Criteria.Criterion;
import org.apache.torque.util.Criteria.Join;

/**
 * Test class for Criteria.
 *
 * @author <a href="mailto:celkins@scardini.com">Christopher Elkins</a>
 * @author <a href="mailto:sam@neurogrid.com">Sam Joseph</a>
 * @author <a href="mailto:seade@backstagetech.com.au">Scott Eade</a>
 * @version $Id$
 */
public class CriteriaTest extends BaseTestCase
{

    /** The criteria to use in the test. */
    private Criteria c;

    /**
     * Creates a new instance.
     *
     * @param name the name of the test to run
     */
    public CriteriaTest(String name)
    {
        super(name);
    }

    /**
     * Initializes the criteria.
     */
    public void setUp()
    {
        super.setUp();
        c = new Criteria();
    }

    /**
     * Test basic adding of strings.
     */
    public void testAddString()
    {
        final String table = "myTable";
        final String column = "myColumn";
        final String value = "myValue";

        // Add the string
        c.add(table, column, (Object) value);

        // Verify that the key exists
        assertTrue(c.containsKey(table, column));

        // Verify that what we get out is what we put in
        assertTrue(c.getString(table, column).equals(value));
    }

    /**
     * test various properties of Criterion and nested criterion
     */
    public void testNestedCriterion()
    {
        final String table2 = "myTable2";
        final String column2 = "myColumn2";
        final String value2 = "myValue2";

        final String table3 = "myTable3";
        final String column3 = "myColumn3";
        final String value3 = "myValue3";

        final String table4 = "myTable4";
        final String column4 = "myColumn4";
        final String value4 = "myValue4";

        final String table5 = "myTable5";
        final String column5 = "myColumn5";
        final String value5 = "myValue5";

        Criteria.Criterion crit2 =
            c.getNewCriterion(table2, column2, (Object) value2, Criteria.EQUAL);
        Criteria.Criterion crit3 =
            c.getNewCriterion(table3, column3, (Object) value3, Criteria.EQUAL);
        Criteria.Criterion crit4 =
            c.getNewCriterion(table4, column4, (Object) value4, Criteria.EQUAL);
        Criteria.Criterion crit5 =
            c.getNewCriterion(table5, column5, (Object) value5, Criteria.EQUAL);

        crit2.and(crit3).or(crit4.and(crit5));
        String expect =
            "((myTable2.myColumn2='myValue2' "
                + "AND myTable3.myColumn3='myValue3') "
            + "OR (myTable4.myColumn4='myValue4' "
                + "AND myTable5.myColumn5='myValue5'))";
        String result = crit2.toString();
        assertEquals(expect, result);

        Criteria.Criterion crit6 =
            c.getNewCriterion(table2, column2, (Object) value2, Criteria.EQUAL);
        Criteria.Criterion crit7 =
            c.getNewCriterion(table3, column3, (Object) value3, Criteria.EQUAL);
        Criteria.Criterion crit8 =
            c.getNewCriterion(table4, column4, (Object) value4, Criteria.EQUAL);
        Criteria.Criterion crit9 =
            c.getNewCriterion(table5, column5, (Object) value5, Criteria.EQUAL);

        crit6.and(crit7).or(crit8).and(crit9);
        expect =
            "(((myTable2.myColumn2='myValue2' "
                    + "AND myTable3.myColumn3='myValue3') "
                + "OR myTable4.myColumn4='myValue4') "
                    + "AND myTable5.myColumn5='myValue5')";
        result = crit6.toString();
        assertEquals(expect, result);

        // should make sure we have tests for all possibilities

        Criteria.Criterion[] crita = crit2.getAttachedCriterion();

        assertEquals(crit2, crita[0]);
        assertEquals(crit3, crita[1]);
        assertEquals(crit4, crita[2]);
        assertEquals(crit5, crita[3]);

        List tables = crit2.getAllTables();

        assertEquals(crit2.getTable(), tables.get(0));
        assertEquals(crit3.getTable(), tables.get(1));
        assertEquals(crit4.getTable(), tables.get(2));
        assertEquals(crit5.getTable(), tables.get(3));

        // simple confirmations that equality operations work
        assertTrue(crit2.hashCode() == crit2.hashCode());
        assertEquals(crit2.toString(), crit2.toString());
    }

    /**
     * Tests &lt;= and =&gt;.
     */
    public void testBetweenCriterion()
    {
        Criteria.Criterion cn1 =
            c.getNewCriterion(
                "INVOICE.COST",
                new Integer(1000),
                Criteria.GREATER_EQUAL);
        Criteria.Criterion cn2 =
            c.getNewCriterion(
                "INVOICE.COST",
                new Integer(5000),
                Criteria.LESS_EQUAL);
        c.add(cn1.and(cn2));
        String expect =
            "SELECT  FROM INVOICE WHERE "
            + "(INVOICE.COST>=1000 AND INVOICE.COST<=5000)";
        String result = null;
        try
        {
            result = BasePeer.createQueryString(c);
        }
        catch (TorqueException e)
        {
            fail("TorqueException thrown in BasePeer.createQueryString()");
        }

        assertEquals(expect, result);
    }

    /**
     * Verify that AND and OR criterion are nested correctly.
     */
    public void testPrecedence()
    {
        Criteria.Criterion cn1 =
            c.getNewCriterion("INVOICE.COST", "1000", Criteria.GREATER_EQUAL);
        Criteria.Criterion cn2 =
            c.getNewCriterion("INVOICE.COST", "2000", Criteria.LESS_EQUAL);
        Criteria.Criterion cn3 =
            c.getNewCriterion("INVOICE.COST", "8000", Criteria.GREATER_EQUAL);
        Criteria.Criterion cn4 =
            c.getNewCriterion("INVOICE.COST", "9000", Criteria.LESS_EQUAL);
        c.add(cn1.and(cn2));
        c.or(cn3.and(cn4));

        String expect =
            "SELECT  FROM INVOICE WHERE "
            + "((INVOICE.COST>='1000' AND INVOICE.COST<='2000') "
            + "OR (INVOICE.COST>='8000' AND INVOICE.COST<='9000'))";

        String result = null;
        try
        {
            result = BasePeer.createQueryString(c);
        }
        catch (TorqueException e)
        {
            fail("TorqueException thrown in BasePeer.createQueryString()");
        }

        assertEquals(expect, result);
    }

    /**
     * Test Criterion.setIgnoreCase().
     * As the output is db specific the test just prints the result to
     * System.out
     */
    public void testCriterionIgnoreCase()
    {
        Criteria myCriteria = new Criteria();

        Criteria.Criterion expected = myCriteria.getNewCriterion(
                "TABLE.COLUMN", (Object)"FoObAr", Criteria.LIKE);
        Criteria.Criterion result = expected.setIgnoreCase(true);
        assertEquals("Criterion mis-match after calling setIgnoreCase(true)",
                     expected.toString(), result.toString());
    }

    /**
     * Test that true is evaluated correctly.
     */
    public void testBoolean()
    {
        Criteria c = new Criteria().add("TABLE.COLUMN", true);

        String expect = "SELECT  FROM TABLE WHERE TABLE.COLUMN=1";

        String result = null;
        try
        {
            result = BasePeer.createQueryString(c);
        }
        catch (TorqueException e)
        {
            fail("TorqueException thrown in BasePeer.createQueryString()");
        }

        assertEquals(expect, result);

        // test the postgresql variation
        c = new Criteria();
        Criteria.Criterion cc =
            c.getNewCriterion("TABLE.COLUMN", Boolean.TRUE, Criteria.EQUAL);

        Configuration conf = new BaseConfiguration();
        conf.addProperty("driver", "org.postgresql.Driver");
        try
        {
            cc.setDB(DBFactory.create("org.postgresql.Driver"));
        }
        catch (Exception e)
        {
            fail("Exception thrown in DBFactory");
        }

        assertEquals("TABLE.COLUMN=TRUE", cc.toString());
    }

    /**
     * testcase for addDate()
     */
    public void testAddDate()
    {
        Criteria c = new Criteria();
        c.addDate("TABLE.DATE_COLUMN", 2003, 0, 22);

        String expect = "SELECT  FROM TABLE WHERE TABLE.DATE_COLUMN='20030122000000'";

        String result = null;
        try
        {
            result = BasePeer.createQueryString(c);
        }
        catch (TorqueException e)
        {
            e.printStackTrace();
            fail("TorqueException thrown in BasePeer.createQueryString()");
        }
        assertEquals(expect, result);
    }

    /**
     * testcase for andDate()
     * issue TORQUE-42
     */
    public void testAndDate()
    {
        Criteria c = new Criteria();
        c.addDate("TABLE.DATE_COLUMN", 2003, 0, 22, Criteria.GREATER_THAN);
        c.andDate("TABLE.DATE_COLUMN", 2004, 0, 22, Criteria.LESS_THAN);

        String expect = "SELECT  FROM TABLE WHERE (TABLE.DATE_COLUMN>'20030122000000' AND TABLE.DATE_COLUMN<'20040122000000')";

        String result = null;
        try
        {
            result = BasePeer.createQueryString(c);
        }
        catch (TorqueException e)
        {
            e.printStackTrace();
            fail("TorqueException thrown in BasePeer.createQueryString()");
        }
        assertEquals(expect, result);
    }

    /**
     * testcase for add(Date)
     */
    public void testDateAdd()
    {
        Calendar cal = Calendar.getInstance();
        cal.set(2003, 0, 22, 0, 0, 0);
        Date date = cal.getTime();
        Criteria c = new Criteria();
        c.add("TABLE.DATE_COLUMN", date);

        String expect = "SELECT  FROM TABLE WHERE TABLE.DATE_COLUMN='20030122000000'";

        String result = null;
        try
        {
            result = BasePeer.createQueryString(c);
        }
        catch (TorqueException e)
        {
            e.printStackTrace();
            fail("TorqueException thrown in BasePeer.createQueryString()");
        }
        assertEquals(expect, result);
    }

    public void testCurrentDate()
    {
        Criteria c = new Criteria()
                .add("TABLE.DATE_COLUMN", Criteria.CURRENT_DATE)
                .add("TABLE.TIME_COLUMN", Criteria.CURRENT_TIME);

        String expect = "SELECT  FROM TABLE WHERE TABLE.TIME_COLUMN=CURRENT_TIME AND TABLE.DATE_COLUMN=CURRENT_DATE";

        String result = null;
        try
        {
            result = BasePeer.createQueryString(c);
        }
        catch (TorqueException e)
        {
            e.printStackTrace();
            fail("TorqueException thrown in BasePeer.createQueryString()");
        }

        assertEquals(expect,result);
    }

    public void testCountAster()
    {
        Criteria c = new Criteria()
                .addSelectColumn("COUNT(*)")
                .add("TABLE.DATE_COLUMN", Criteria.CURRENT_DATE)
                .add("TABLE.TIME_COLUMN", Criteria.CURRENT_TIME);

        String expect = "SELECT COUNT(*) FROM TABLE WHERE TABLE.TIME_COLUMN=CURRENT_TIME AND TABLE.DATE_COLUMN=CURRENT_DATE";

        String result = null;
        try
        {
            result = BasePeer.createQueryString(c);
        }
        catch (TorqueException e)
        {
            e.printStackTrace();
            fail("TorqueException thrown in BasePeer.createQueryString()");
        }

        assertEquals(expect,result);

    }

    /**
     * This test case has been written to try out the fix applied to resolve
     * TRQS73 - i.e. ensuring that Criteria.toString() does not alter any limit
     * or offset that may be stored in the Criteria object.  This testcase
     * could actually pass without the fix if the database in use does not
     * support native limits and offsets.
     */
    public void testCriteriaToStringOffset()
    {
        Criteria c = new Criteria()
                .add("TABLE.DATE_COLUMN", Criteria.CURRENT_DATE)
                .setOffset(3)
                .setLimit(5);

        String toStringExpect = "Criteria:: TABLE.DATE_COLUMN<=>TABLE.DATE_COLUMN=CURRENT_DATE:  "
                + "\nCurrent Query SQL (may not be complete or applicable): "
                + "SELECT  FROM TABLE WHERE TABLE.DATE_COLUMN=CURRENT_DATE LIMIT 5 OFFSET 3";

        String cString = c.toString();
        //System.out.println(cString);
        assertEquals(toStringExpect, cString);

        // Note that this is intentionally the same as above as the behaviour is
        // only observed on subsequent invocations of toString().
        cString = c.toString();
        //System.out.println(cString);
        assertEquals(toStringExpect, cString);
    }

    /**
     * TORQUE-87
     */
    public void testCriteriaWithOffsetNoLimit()
    {
        Criteria c = new Criteria()
        .add("TABLE.DATE_COLUMN", Criteria.CURRENT_DATE)
        .setOffset(3);
        
        String toStringExpect = "Criteria:: TABLE.DATE_COLUMN<=>TABLE.DATE_COLUMN=CURRENT_DATE:  "
            + "\nCurrent Query SQL (may not be complete or applicable): "
            + "SELECT  FROM TABLE WHERE TABLE.DATE_COLUMN=CURRENT_DATE LIMIT 18446744073709551615 OFFSET 3";
        
        String cString = c.toString();
        //System.out.println(cString);
        assertEquals(toStringExpect, cString);
        
        // Note that this is intentionally the same as above as the behaviour is
        // only observed on subsequent invocations of toString().
        cString = c.toString();
        //System.out.println(cString);
        assertEquals(toStringExpect, cString);
    }
    
    /**
     * This test case has been written to try out the fix applied to resolve
     * TRQS73 - i.e. ensuring that Criteria.toString() does not alter any limit
     * or offset that may be stored in the Criteria object.  This testcase
     * could actually pass without the fix if the database in use does not
     * support native limits and offsets.
     */
    public void testCriteriaToStringLimit()
    {
        Criteria c = new Criteria()
                .add("TABLE.DATE_COLUMN", Criteria.CURRENT_DATE)
                .setLimit(5);

        String toStringExpect = "Criteria:: TABLE.DATE_COLUMN<=>TABLE.DATE_COLUMN=CURRENT_DATE:  "
                + "\nCurrent Query SQL (may not be complete or applicable): "
                + "SELECT  FROM TABLE WHERE TABLE.DATE_COLUMN=CURRENT_DATE LIMIT 5";

        String cString = c.toString();
        //System.out.println(cString);
        assertEquals(toStringExpect, cString);

        // Note that this is intentionally the same as above as the behaviour is
        // only observed on subsequent invocations of toString().
        cString = c.toString();
        //System.out.println(cString);
        assertEquals(toStringExpect, cString);
    }

    /**
     * This test case verifies if the Criteria.LIKE comparison type will
     * get replaced through Criteria.EQUAL if there are no SQL wildcards
     * in the given value.
     */
    public void testLikeWithoutWildcards()
    {
        Criteria c = new Criteria();
        c.add("TABLE.COLUMN", (Object) "no wildcards", Criteria.LIKE);

        String expect = "SELECT  FROM TABLE WHERE TABLE.COLUMN = 'no wildcards'";

        String result = null;
        try
        {
            result = BasePeer.createQueryString(c);
        }
        catch (TorqueException e)
        {
            e.printStackTrace();
            fail("TorqueException thrown in BasePeer.createQueryString()");
        }

        assertEquals(expect, result);
    }

    /**
     * This test case verifies if the Criteria.NOT_LIKE comparison type will
     * get replaced through Criteria.NOT_EQUAL if there are no SQL wildcards
     * in the given value.
     */
    public void testNotLikeWithoutWildcards()
    {
        Criteria c = new Criteria();
        c.add("TABLE.COLUMN", (Object) "no wildcards", Criteria.NOT_LIKE);

        String firstExpect = "SELECT  FROM TABLE WHERE TABLE.COLUMN != 'no wildcards'";
        String secondExpect = "SELECT  FROM TABLE WHERE TABLE.COLUMN <> 'no wildcards'";

        String result = null;
        try
        {
            result = BasePeer.createQueryString(c);
        }
        catch (TorqueException e)
        {
            e.printStackTrace();
            fail("TorqueException thrown in BasePeer.createQueryString()");
        }

        assertTrue(result.equals(firstExpect) || result.equals(secondExpect));
    }

    /**
     * Test that serialization works.
     */
    public void testSerialization()
    {
        c.setOffset(10);
        c.setLimit(11);
        c.setIgnoreCase(true);
        c.setSingleRecord(true);
        c.setCascade(true);
        c.setDbName("myDB");
        c.setAll();
        c.setDistinct();
        c.addSelectColumn("Author.NAME");
        c.addSelectColumn("Author.AUTHOR_ID");
        c.addDescendingOrderByColumn("Author.NAME");
        c.addAscendingOrderByColumn("Author.AUTHOR_ID");
        c.addAlias("Writer", "Author");
        c.addAsColumn("AUTHOR_NAME", "Author.NAME");
        c.addJoin("Author.AUTHOR_ID", "Book.AUTHOR_ID", Criteria.INNER_JOIN);
        c.add("Author.NAME", (Object) "author%", Criteria.LIKE);

        // Some direct Criterion checks
        Criterion cn = c.getCriterion("Author.NAME");
        cn.setIgnoreCase(true);
        assertEquals("author%", cn.getValue());
        assertEquals(Criteria.LIKE, cn.getComparison());
        Criterion cnDirectClone = (Criterion) SerializationUtils.clone(cn);
        assertEquals(cn, cnDirectClone);

        // Clone the object
        Criteria cClone = (Criteria) SerializationUtils.clone(c);

        // Check the clone
        assertEquals(c.size(), cClone.size());
        assertEquals(10, cClone.getOffset());
        assertEquals(c.getOffset(), cClone.getOffset());
        assertEquals(11, cClone.getLimit());
        assertEquals(c.getLimit(), cClone.getLimit());
        assertEquals(true, cClone.isIgnoreCase());
        assertEquals(c.isIgnoreCase(), cClone.isIgnoreCase());
        assertEquals(true, cClone.isSingleRecord());
        assertEquals(c.isSingleRecord(), cClone.isSingleRecord());
        assertEquals(true, cClone.isCascade());
        assertEquals(c.isCascade(), cClone.isCascade());
        assertEquals("myDB", cClone.getDbName());
        assertEquals(c.getDbName(), cClone.getDbName());
        List selectModifiersClone = cClone.getSelectModifiers();
        assertTrue(selectModifiersClone.contains(Criteria.ALL.toString()));
        assertTrue(selectModifiersClone.contains(Criteria.DISTINCT.toString()));
        assertEquals(c.getSelectModifiers(), cClone.getSelectModifiers());
        List selectColumnsClone = cClone.getSelectColumns();
        assertTrue(selectColumnsClone.contains("Author.NAME"));
        assertTrue(selectColumnsClone.contains("Author.AUTHOR_ID"));
        assertEquals(c.getSelectColumns(), cClone.getSelectColumns());
        List orderByColumnsClone = cClone.getOrderByColumns();
        assertTrue(orderByColumnsClone.contains("Author.NAME DESC"));
        assertTrue(orderByColumnsClone.contains("Author.AUTHOR_ID ASC"));
        assertEquals(c.getOrderByColumns(), cClone.getOrderByColumns());
        Map aliasesClone = cClone.getAliases();
        assertTrue(aliasesClone.containsKey("Writer"));
        assertEquals("Author", aliasesClone.get("Writer"));
        assertEquals(c.getAliases(), cClone.getAliases());
        Map asColumnsClone = cClone.getAsColumns();
        assertTrue(asColumnsClone.containsKey("AUTHOR_NAME"));
        assertEquals("Author.NAME", asColumnsClone.get("AUTHOR_NAME"));
        assertEquals(c.getAsColumns(), cClone.getAsColumns());

        // Check Joins
        List joinsClone = cClone.getJoins();
        Join joinClone = (Join) joinsClone.get(0);
        assertEquals("Author.AUTHOR_ID", joinClone.getLeftColumn());
        assertEquals("Book.AUTHOR_ID", joinClone.getRightColumn());
        assertEquals(Criteria.INNER_JOIN, joinClone.getJoinType());
        assertEquals(c.getJoins(), cClone.getJoins());

        // Some Criterion checks
        Criterion cnClone = cClone.getCriterion("Author.NAME");
        assertEquals("author%", cnClone.getValue());
        assertEquals(Criteria.LIKE, cnClone.getComparison());
        assertEquals(cn.isIgnoreCase(), cnClone.isIgnoreCase());

        // Confirm that equals() checks all of the above.
        assertEquals(c, cClone);

        // Check hashCode() too.
        assertEquals(c.hashCode(), cClone.hashCode());
    }

    /**
     * Checks whether orderBy works.
     */
    public void testOrderBy() throws TorqueException
    {
        // we need a rudimentary databaseMap for this test case to work
        DatabaseMap dbMap = Torque.getDatabaseMap(Torque.getDefaultDB());

        TableMap tableMap = new TableMap("AUTHOR", dbMap);
        dbMap.addTable(tableMap);

        ColumnMap columnMap = new ColumnMap("NAME", tableMap);
        columnMap.setType("");
        tableMap.addColumn(columnMap);

        columnMap = new ColumnMap("AUTHOR_ID", tableMap);
        columnMap.setType(new Integer(0));
        tableMap.addColumn(columnMap);

        // check that alias'ed tables are referenced by their alias
        // name when added to the select clause.
        Criteria criteria = new Criteria();
        criteria.addSelectColumn("AUTHOR.NAME");
        criteria.addAlias("a", "AUTHOR");
        criteria.addJoin(
                "AUTHOR.AUTHOR_ID",
                "a." + "AUTHOR_ID");
        criteria.addAscendingOrderByColumn(
                "a.NAME");

        String result = BasePeer.createQueryString(criteria);
        assertEquals("SELECT AUTHOR.NAME, a.NAME "
                    + "FROM AUTHOR, AUTHOR a "
                    + "WHERE AUTHOR.AUTHOR_ID=a.AUTHOR_ID "
                    + "ORDER BY a.NAME ASC",
                result);
    }

}
