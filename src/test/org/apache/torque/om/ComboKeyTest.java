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

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * TestCase for ComboKey
 *
 * @author <a href="mailto:drfish@cox.net">J. Russell Smyth</a>
 * @version $Revision$
 */
public class ComboKeyTest extends TestCase
{
    private ComboKey c1a = new ComboKey(
        new SimpleKey[]{new StringKey("key1"), new StringKey("key2")});
    private ComboKey c1b = new ComboKey(
        new SimpleKey[]{new StringKey("key1"), new StringKey("key2")});
    private ComboKey c2a = new ComboKey(
        new SimpleKey[]{new StringKey("key3"), new StringKey("key4")});
    // complex keys for test
    private java.util.Date now = new java.util.Date();
    private ComboKey c3a = new ComboKey(
        new SimpleKey[]{new StringKey("key1"), null, new DateKey(now)});
    private ComboKey c3b = new ComboKey(new SimpleKey[]{
        new StringKey("key1"), null, new DateKey(now)});
    private ComboKey c4a = new ComboKey(
        new SimpleKey[]{new StringKey("key1"), null, new NumberKey(123456)});

    /**
     * Simple constructor.
     *
     * @param name the name of the test to execute
     */
    public ComboKeyTest(String name)
    {
        super(name);
    }


    public static void main(java.lang.String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(ComboKeyTest.class);

        return suite;
    }

    public void testReflexive()
    {
        Assert.assertTrue(c1a.equals(c1a));
        // Complex key using null and date
        // This currently has to use looseEquals as ComboKey.equals(Obj)
        // does not accept null key values (WHY!)
        Assert.assertTrue(c3a.looseEquals(c3a));
    }

//    public void testReflexiveWithNullKeyValue()
//    {
//        Assert.assertTrue(c3a.equals(c3a));
//    }

    public void testSymmetric()
    {
        Assert.assertTrue(c1a.equals(c1b));
        Assert.assertTrue(c1b.equals(c1a));
    }

    public void testNull()
    {
        Assert.assertTrue(!c1a.equals(null));
    }

    public void testNotEqual()
    {
        Assert.assertTrue(!c1a.equals(c2a));
    }

    public void testRoundTripWithStringKeys()
    {
        // two strings
        ComboKey oldKey = new ComboKey(
            new SimpleKey[]{new StringKey("key1"), new StringKey("key2")});
        ComboKey newKey = null;
        String stringValue = oldKey.toString();
        System.out.println("OldKey as String=" + stringValue);
        try
        {
            newKey = new ComboKey(stringValue);
        }
        catch(Exception e)
        {
            fail("Exception " + e.getClass().getName()
                     + " thrown on new ComboKey(" + stringValue + "):"
                     + e.getMessage());
        }
        Assert.assertEquals(oldKey,newKey);
    }

    public void testRoundTripWithComplexKey()
    {
        // complex key
        ComboKey oldKey = new ComboKey(
            new SimpleKey[]{new StringKey("key1"), new NumberKey(12345),
            new DateKey(new java.util.Date())});
        ComboKey newKey = null;
        String stringValue = oldKey.toString();
        System.out.println("OldKey as String=" + stringValue);
        try
        {
            newKey = new ComboKey(stringValue);
        }
        catch(Exception e)
        {
            fail("Exception " + e.getClass().getName() + " thrown on new ComboKey("
                 + stringValue + "):" + e.getMessage());
        }
        Assert.assertEquals(oldKey,newKey);
    }

    public void testRoundTripWithNullKey()
    {
        // with null key
        ComboKey oldKey = new ComboKey(
            new SimpleKey[]{new StringKey("key1"), null});
        ComboKey newKey = null;
        String stringValue = oldKey.toString();
        System.out.println("OldKey as String=" + stringValue);
        try
        {
            newKey = new ComboKey(stringValue);
        }
        catch(Exception e)
        {
            fail("Exception " + e.getClass().getName() + " thrown on new ComboKey("
                 + stringValue + "):" + e.getMessage());
        }
        // This currently has to use looseEquals as ComboKey.equals(Obj)
        // does not accept null key values (WHY!)
        Assert.assertTrue(oldKey.looseEquals(newKey));
    }


    /** Test of appendTo method, of class org.apache.torque.om.ComboKey. */
    public void testAppendTo()
    {
        StringBuffer sb = new StringBuffer();
        c1a.appendTo(sb);
        Assert.assertEquals("Skey1:Skey2:", sb.toString());
    }

    /** Test of toString method, of class org.apache.torque.om.ComboKey. */
    public void testToString()
    {
        Assert.assertEquals("Skey1::N123456:", c4a.toString());
    }
}

