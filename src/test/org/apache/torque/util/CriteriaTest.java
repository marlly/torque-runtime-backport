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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.torque.BaseTestCase;
import org.apache.torque.TorqueException;
import org.apache.torque.util.BasePeer;
import org.apache.torque.util.Criteria;

/**
 * Test class for Criteria.
 *
 * @author <a href="mailto:celkins@scardini.com">Christopher Elkins</a>
 * @author <a href="mailto:sam@neurogrid.com">Sam Joseph</a>
 * @version $Id$
 */
public class CriteriaTest extends BaseTestCase
{
    private Criteria c;

    /**
     * Creates a new instance.
     */
    public CriteriaTest(String name)
    {
        super(name);
    }

    public void setUp()
    {
        super.setUp();
        c = new Criteria();
    }

    public void tearDown()
    {
        c = null;
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
        c.add(table, column, (Object)value);

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
            c.getNewCriterion(table2,column2,(Object)value2,Criteria.EQUAL);
        Criteria.Criterion crit3 =
            c.getNewCriterion(table3,column3,(Object)value3,Criteria.EQUAL);
        Criteria.Criterion crit4 =
            c.getNewCriterion(table4,column4,(Object)value4,Criteria.EQUAL);
        Criteria.Criterion crit5 =
            c.getNewCriterion(table5,column5,(Object)value5,Criteria.EQUAL);
        
        
        crit2.and(crit3).or(crit4.and(crit5));
        String expect = "((myTable2.myColumn2='myValue2' AND myTable3.myColumn3='myValue3') OR (myTable4.myColumn4='myValue4' AND myTable5.myColumn5='myValue5'))";
        String result = crit2.toString();
        assertEquals(expect,result);
        
        Criteria.Criterion crit6 =
            c.getNewCriterion(table2,column2,(Object)value2,Criteria.EQUAL);
        Criteria.Criterion crit7 =
            c.getNewCriterion(table3,column3,(Object)value3,Criteria.EQUAL);
        Criteria.Criterion crit8 =
            c.getNewCriterion(table4,column4,(Object)value4,Criteria.EQUAL);
        Criteria.Criterion crit9 =
            c.getNewCriterion(table5,column5,(Object)value5,Criteria.EQUAL);

        
        crit6.and(crit7).or(crit8).and(crit9);
        expect = "(((myTable2.myColumn2='myValue2' AND myTable3.myColumn3='myValue3') OR myTable4.myColumn4='myValue4') AND myTable5.myColumn5='myValue5')";
        result = crit6.toString();
        assertEquals(expect,result);
        

        // should make sure we have tests for all possibilities
        
        Criteria.Criterion[] crita = crit2.getAttachedCriterion();
        
        assertEquals(crit2,crita[0]);
        assertEquals(crit3,crita[1]);
        assertEquals(crit4,crita[2]);
        assertEquals(crit5,crita[3]);
        
        String[] tables = crit2.getAllTables();
               
        assertEquals(crit2.getTable(),tables[0]);
        assertEquals(crit3.getTable(),tables[1]);
        assertEquals(crit4.getTable(),tables[2]);
        assertEquals(crit5.getTable(),tables[3]);
        
        // simple confirmations that equality operations work
        assertTrue(crit2.hashCode() == crit2.hashCode());
        assertEquals(crit2.toString(),crit2.toString());
    }

    public void testBetweenCriterion()
    {
        Criteria.Criterion cn1 = c.getNewCriterion("INVOICE.COST",
                                                   new Integer(1000),
                                                   Criteria.GREATER_EQUAL);
        Criteria.Criterion cn2 = c.getNewCriterion("INVOICE.COST",
                                                   new Integer(5000),
                                                   Criteria.LESS_EQUAL);
        c.add(cn1.and(cn2));
        String expect = "SELECT  FROM INVOICE WHERE (INVOICE.COST>=1000 AND INVOICE.COST<=5000)";
        String result = null;
        try
        {
            result = BasePeer.createQueryString(c);
        }
        catch (TorqueException e)
        {
            fail("TorqueException thrown in BasePeer.createQueryString()");
        }

        assertEquals(expect,result);
    }

    /**
     * Verify that AND and OR criterion are nested correctly.
     */
    public void testPrecedence()
    {
        Criteria.Criterion cn1 = c.getNewCriterion("INVOICE.COST",
                                                   "1000",
                                                   Criteria.GREATER_EQUAL);
        Criteria.Criterion cn2 = c.getNewCriterion("INVOICE.COST",
                                                   "2000",
                                                   Criteria.LESS_EQUAL);
        Criteria.Criterion cn3 = c.getNewCriterion("INVOICE.COST",
                                                   "8000",
                                                   Criteria.GREATER_EQUAL);
        Criteria.Criterion cn4 = c.getNewCriterion("INVOICE.COST",
                                                   "9000",
                                                   Criteria.LESS_EQUAL);
        c.add(cn1.and(cn2));
        c.or(cn3.and(cn4));

        String expect = "SELECT  FROM INVOICE WHERE ((INVOICE.COST>='1000' AND INVOICE.COST<='2000') OR (INVOICE.COST>='8000' AND INVOICE.COST<='9000'))";

        String result = null;
        try
        {
            result = BasePeer.createQueryString(c);
        }
        catch (TorqueException e)
        {
            fail("TorqueException thrown in BasePeer.createQueryString()");
        }

        assertEquals(expect,result);
    }
}
