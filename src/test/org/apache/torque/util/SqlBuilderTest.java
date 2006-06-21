package org.apache.torque.util;

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

import org.apache.torque.BaseTestCase;
import org.apache.torque.TorqueException;

/**
 * Tests for SqlExpression
 *
 * @author <a href="mailto:mpoeschl@marmot.at">Martin Poeschl</a>
 * @author <a href="mailto:seade@backstagetech.com.au">Scott Eade</a>
 * @version $Id: SqlExpressionTest.java 239636 2005-08-24 12:38:09Z henning $
 */
public class SqlBuilderTest extends BaseTestCase
{
    /**
     * Creates a new instance.
     *
     * @param name the name of the test case to run
     */
    public SqlBuilderTest(String name)
    {
        super(name);
    }

    public void testExtractTableName() throws TorqueException
    {
        // standard cases with / without schema
        String columnName = "table.column";
        String tableName = SQLBuilder.getTableName(columnName, null);
        assertEquals("table", tableName);

        columnName = "schema.table.column";
        tableName = SQLBuilder.getTableName(columnName, null);
        assertEquals("schema.table", tableName);

        // functions
        columnName = "function(table.column)";
        tableName = SQLBuilder.getTableName(columnName, null);
        assertEquals("table", tableName);

        columnName = "function(1,table.column,2)";
        tableName = SQLBuilder.getTableName(columnName, null);
        assertEquals("table", tableName);

        // comparisons
        columnName = "table.column < 10";
        tableName = SQLBuilder.getTableName(columnName, null);
        assertEquals("table", tableName);

        columnName = "table.column<10";
        tableName = SQLBuilder.getTableName(columnName, null);
        assertEquals("table", tableName);

        columnName = "10 > table.column";
        tableName = SQLBuilder.getTableName(columnName, null);
        assertEquals("table", tableName);

        columnName = "10>table.column";
        tableName = SQLBuilder.getTableName(columnName, null);
        assertEquals("table", tableName);

        columnName = "10>table.column";
        tableName = SQLBuilder.getTableName(columnName, null);
        assertEquals("table", tableName);

        // in clause
        columnName = "table.column in (1,2,3)";
        tableName = SQLBuilder.getTableName(columnName, null);
        assertEquals("table", tableName);

        // wildcard
        columnName = "*";
        tableName = SQLBuilder.getTableName(columnName, null);
        assertEquals(null, tableName);

        // function with wildcard
        columnName = "count(*)";
        tableName = SQLBuilder.getTableName(columnName, null);
        assertEquals(null, tableName);
 
        // empty String and null
        columnName = "";
        try
        {
            tableName = SQLBuilder.getTableName(columnName, null);
            fail("getTableName() should fail for empty column name");
        }
        catch (TorqueException e)
        {
        }

        columnName = null;
        try
        {
            tableName = SQLBuilder.getTableName(columnName, null);
            fail("getTableName() should fail for null as column name");
        }
        catch (TorqueException e)
        {
        }
        
        // failure: no dot or wildcard
        columnName = "column";
        try
        {
            tableName = SQLBuilder.getTableName(columnName, null);
            fail("getTableName() should fail for column name "
                    + "without a dot or wildcard");
        }
        catch (TorqueException e)
        {
        }
        
    }
}
