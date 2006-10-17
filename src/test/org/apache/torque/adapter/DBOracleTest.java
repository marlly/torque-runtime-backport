package org.apache.torque.adapter;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import junit.framework.TestCase;

import org.apache.torque.util.Query;
import org.apache.torque.util.UniqueList;

public class DBOracleTest extends TestCase
{
    /**
     * Tests whether replacing the select columns in limit/offset
     * treatment works (double column names must be aliased)
     */
    public void testSelectColumnsForLimitOffset()
    {
        Query query = new Query();
        UniqueList input = new UniqueList();
        input.add("c1");
        input.add("c2");
        input.add("c1 a1");
        input.add("c1 a2");
        input.add("t.c1 a4");
        
        // A list with no duplicates must remain unchanged
        query.setSelectClause(new UniqueList(input));
        new DBOracle().generateLimits(query, 0, 1);
        assertEquals(input, query.getSelectClause());

        // double column names must be aliased 
        input.set(1, "t.c1");
        query.setSelectClause(new UniqueList(input));
        new DBOracle().generateLimits(query, 0, 1);        
        UniqueList expected = new UniqueList(input);
        expected.set(1, "t.c1 a0");
        assertEquals(expected, query.getSelectClause());
        
        // a column name which is the same as an alias name must be replaced
        input.set(1, "c2");
        input.set(0, "t.a1");
        query.setSelectClause(new UniqueList(input));
        new DBOracle().generateLimits(query, 0, 1);        
        expected = new UniqueList(input);
        expected.set(0, "t.a1 a0");
        assertEquals(query.getSelectClause(), expected);
        
        // triple column names must be made unique
        input.set(1, "t2.a1");
        query.setSelectClause(new UniqueList(input));
        new DBOracle().generateLimits(query, 0, 1);        
        expected = new UniqueList(input);
        expected.set(0, "t.a1 a0");
        expected.set(1, "t2.a1 a3");
        assertEquals(expected, query.getSelectClause());
    }
        
}
