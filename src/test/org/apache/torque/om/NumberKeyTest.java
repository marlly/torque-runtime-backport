package org.apache.torque.om;

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

import java.util.Arrays;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Currently just tests the equality of NumberKey.
 * 
 * @author <a href="mailto:stephenh@chase3000.com">Stephen Haberman</a>
 * @version $Revision$
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
