package org.apache.torque.engine.database.transform;

/*
 * Copyright 2003,2004 The Apache Software Foundation.
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

import org.apache.torque.engine.database.model.Database;
import org.apache.torque.engine.database.model.Table;

/**
 * @author <a href="mailto:mpoeschl@marmot.at">Martin Poeschl</a>
 * @version $Id$
 */
public class SQLToAppDataTest extends TestCase
{
    
    /**
     * Class to test for void SQLToAppData(String, String)
     */
    public void testMySql() throws Exception
    {
        SQLToAppData sqlToAppData = new SQLToAppData(
                "src/test/org/apache/torque/engine/database/transform/mysql.sql",
                "mysql");
        Database db = sqlToAppData.execute();
        assertTrue(db.getTables().size() > 0);
        Table course = db.getTable("course");
        assertTrue(course != null);
    }

}
