package org.apache.torque.util;

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

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.torque.BaseTestCase;
import org.apache.torque.TorqueException;
import org.apache.torque.adapter.DBFactory;

/**
 * Test class for Criteria.
 *
 * @author <a href="mailto:celkins@scardini.com">Christopher Elkins</a>
 * @author <a href="mailto:sam@neurogrid.com">Sam Joseph</a>
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

        String[] tables = crit2.getAllTables();

        assertEquals(crit2.getTable(), tables[0]);
        assertEquals(crit3.getTable(), tables[1]);
        assertEquals(crit4.getTable(), tables[2]);
        assertEquals(crit5.getTable(), tables[3]);

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

        Criteria.Criterion myCriterion = myCriteria.getNewCriterion(
                "TABLE.COLUMN", (Object)"FoObAr", Criteria.LIKE);
        System.out.println("before setIgnoreCase: " + myCriterion);

        Criteria.Criterion ignoreCriterion = myCriterion.setIgnoreCase(true);
        System.out.println("after setIgnoreCase: " + ignoreCriterion);
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

        assertEquals("TABLE.COLUMN=1", cc.toString());
    }

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
        System.out.println(result);
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
                + "SELECT  FROM TABLE WHERE TABLE.DATE_COLUMN=CURRENT_DATE LIMIT 3, 5";

        String cString = c.toString();
        //System.out.println(cString);
        assertEquals(cString, toStringExpect);

        // Note that this is intentially the same as above as the behaviour is
        // only observed on subsequent invocations of toString().
        cString = c.toString();
        //System.out.println(cString);
        assertEquals(cString, toStringExpect);
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
        assertEquals(cString, toStringExpect);

        // Note that this is intentially the same as above as the behaviour is
        // only observed on subsequent invocations of toString().
        cString = c.toString();
        //System.out.println(cString);
        assertEquals(cString, toStringExpect);
    }

}
