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

import java.lang.reflect.Array;

import junit.framework.TestCase;

import org.apache.torque.adapter.DB;
import org.apache.torque.adapter.DBFactory;

/**
 * Tests for SqlExpression
 * 
 * @author <a href="mailto:mpoeschl@marmot.at">Martin Poeschl</a>
 * @version $Id$
 */
public class SqlExpressionTest extends TestCase
{
    private DB db = null;
    

	/**
	 * Constructor for SqlExpressionTest.
	 * @param arg0
	 */
	public SqlExpressionTest(String arg0)
	{
		super(arg0);
	}

    /**
     * set up environment
     */
    public void setUp()
    {
        try
        {
            db = DBFactory.create("mysql");
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

	/**
	 * Test for String buildInnerJoin(String, String)
	 */
	public void testBuildInnerJoinStringString()
	{
        String result = SqlExpression.buildInnerJoin("TA.COLA", "TB.COLB");
        assertEquals(result, "TA.COLA=TB.COLB");
	}

	/**
	 * Test for String buildInnerJoin(String, String, boolean, DB)
	 */
	public void testBuildInnerJoinStringStringbooleanDB()
	{
        String result = SqlExpression.buildInnerJoin("TA.COLA", "TB.COLB", 
                true, db);
        assertEquals(result, "TA.COLA=TB.COLB");
	}

	/**
	 * Test for String buildIn(String, Object, SqlEnum, boolean, DB)
	 */
	public void testBuildInStringObjectSqlEnumbooleanDB()
	{
        String[] values = new String[] { "42", "43", "44" };
        String result = SqlExpression.buildIn("COL", values, SqlEnum.IN, 
                true, db);
        assertEquals(result, "COL IN ('42','43','44')");
	}
    
    public void testLargeBuildInStringObjectSqlEnumbooleanDB()
    {
        int size = 10000;
        String[] values = new String[size];
        for (int i = 0; i < size; i++)
        {
            Array.set(values, i, String.valueOf(i));
        }
        long start = System.currentTimeMillis();
        String result = SqlExpression.buildIn("COL", values, SqlEnum.IN, 
                true, db);
        long end =  System.currentTimeMillis();
        System.out.println("large buildIn: " + (end - start));
    }
    
}
