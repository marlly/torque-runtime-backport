package org.apache.torque.om;

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

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * TestCase for ComboKey
 *
 * @author <a href="mailto:drfish@cox.net">J. Russell Smyth</a>
 * @version $Id$
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

        /**
         * 
         * @param args
         */
    public static void main(java.lang.String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

        /**
         * 
         * @return Test
         */
    public static Test suite()
    {
        TestSuite suite = new TestSuite(ComboKeyTest.class);

        return suite;
    }

        /**
         * 
         *
         */
    public void testReflexive()
    {
        Assert.assertTrue(c1a.equals(c1a));
        // Complex key using null and date
        // This currently has to use looseEquals as ComboKey.equals(Obj)
        // does not accept null key values (WHY!)
        Assert.assertTrue(c3a.looseEquals(c3a));
    }

        /**
         * 
         *
         */
    public void testSymmetric()
    {
        Assert.assertTrue(c1a.equals(c1b));
        Assert.assertTrue(c1b.equals(c1a));
    }

        /**
         * 
         *
         */
    public void testNull()
    {
        Assert.assertTrue(!c1a.equals(null));
    }

        /**
         * 
         *
         */
    public void testNotEqual()
    {
        Assert.assertTrue(!c1a.equals(c2a));
    }

        /**
         * 
         *
         */
    public void testRoundTripWithStringKeys()
    {
        // two strings
        ComboKey oldKey = new ComboKey(
            new SimpleKey[]{new StringKey("key1"), new StringKey("key2")});
        ComboKey newKey = null;
        String stringValue = oldKey.toString();
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

        /**
         * 
         *
         */
    public void testRoundTripWithComplexKey()
    {
        // complex key
        ComboKey oldKey = new ComboKey(
            new SimpleKey[]{new StringKey("key1"), new NumberKey(12345),
            new DateKey(new java.util.Date())});
        ComboKey newKey = null;
        String stringValue = oldKey.toString();
        try
        {
            newKey = new ComboKey(stringValue);
        }
        catch (Exception e)
        {
            fail("Exception " + e.getClass().getName() 
                    + " thrown on new ComboKey("
                    + stringValue + "):" + e.getMessage());
        }
        Assert.assertEquals(oldKey,newKey);
    }

        /**
         * 
         *
         */
    public void testRoundTripWithNullKey()
    {
        // with null key
        ComboKey oldKey = new ComboKey(
            new SimpleKey[]{new StringKey("key1"), null});
        ComboKey newKey = null;
        String stringValue = oldKey.toString();
        try
        {
            newKey = new ComboKey(stringValue);
        }
        catch (Exception e)
        {
            fail("Exception " + e.getClass().getName() 
                    + " thrown on new ComboKey("
                    + stringValue + "):" + e.getMessage());
        }
        // This currently has to use looseEquals as ComboKey.equals(Obj)
        // does not accept null key values (WHY!)
        Assert.assertTrue(oldKey.looseEquals(newKey));
    }


    /** 
     * Test of appendTo method, of class org.apache.torque.om.ComboKey. 
     */
    public void testAppendTo()
    {
        StringBuffer sb = new StringBuffer();
        c1a.appendTo(sb);
        Assert.assertEquals("Skey1:Skey2:", sb.toString());
    }

    /** 
     * Test of toString method, of class org.apache.torque.om.ComboKey. 
     */
    public void testToString()
    {
        Assert.assertEquals("Skey1::N123456:", c4a.toString());
    }
}
