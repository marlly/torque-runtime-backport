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

import junit.framework.TestCase;

/**
 * Test for UniqueList
 *
 * @author <a href="mailto:mpoeschl@marmot.at">Martin Poeschl</a>
 * @version $Id$
 */
public class UniqueListTest extends TestCase
{
    public UniqueListTest(String name)
    {
        super(name);
    }

    /**
     * null values are not allowed
     */
    public void testNull()
    {
        UniqueList uniqueList = new UniqueList();
        Object o = null;
        boolean actualReturn = uniqueList.add(o);
        assertEquals("return value", false, actualReturn);
    }

    /**
     * duplicates values are not allowed
     */
    public void testUnique()
    {
        UniqueList uniqueList = new UniqueList();
        uniqueList.add("Table");
        uniqueList.add("TableA");
        uniqueList.add("Table");
        uniqueList.add("TableB");
        assertEquals(3, uniqueList.size());
    }
}
