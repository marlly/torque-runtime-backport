package org.apache.torque.om;

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

import java.util.Arrays;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Currently just tests the equality of NumberKey.
 *
 * @author <a href="mailto:stephenh@chase3000.com">Stephen Haberman</a>
 * @version $Id$
 */
public class NumberKeyTest extends TestCase
{

    /** Test value. */
    private NumberKey n1a = new NumberKey(1);
    /** Test value. */
    private NumberKey n1b = new NumberKey(1);
    /** Test value. */
    private NumberKey n1c = new NumberKey(1);
    /** Test value. */
    private NumberKey n2a = new NumberKey(2);

    /**
     * Simple constructor.
     *
     * @param name the name of the test to execute
     */
    public NumberKeyTest(String name)
    {
        super(name);
    }

    /**
     * Test a.equals(a)
     */
    public void testReflexive()
    {
        Assert.assertTrue(n1a.equals(n1a));
    }

    /**
     * Test a.equals(b) = b.equals(a)
     */
    public void testSymmetric()
    {
        Assert.assertTrue(n1a.equals(n1b));
        Assert.assertTrue(n1b.equals(n1a));

        Assert.assertTrue(!"1".equals(n1a));
        // As this used to give false, i.e. n1a was considered equal to "1"
        // it can lead to difficult to find bugs if it is immediately
        // changed to the opposite.  So this will throw an exception.
        //Assert.assertTrue(!n1a.equals("1"));
        try
        {
            Assert.assertTrue(!n1a.equals("1"));
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }
        Assert.assertTrue(!n1a.equals(new Integer(1)));
        Assert.assertTrue(!new Integer(1).equals(n1a));
    }

    /**
     * Test a.equals(b) = b.equals(c) = c.equals(a)
     */
    public void testTransitive()
    {
        Assert.assertTrue(n1a.equals(n1b));
        Assert.assertTrue(n1b.equals(n1c));
        Assert.assertTrue(n1c.equals(n1a));
    }

    /**
     * Test !a.equals(null)
     */
    public void testNull()
    {
        Assert.assertTrue(!n1a.equals(null));
    }

    /**
     * Test sorting.
     */
    public void testList()
    {
        Object[] array = new Object[] { n1a, n2a, n1b };
        Arrays.sort(array);

        Assert.assertEquals(n1a, array[0]);
        Assert.assertEquals(n1b, array[1]);
        Assert.assertEquals(n2a, array[2]);
    }

    /**
     * Test long constructor
     */
    public void testLongConstructor()
    {
        NumberKey key = new NumberKey(9900000000000001L);
        assertEquals("9900000000000001", key.toString());
    }
}
