package org.apache.torque.engine.platform;

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

import org.apache.torque.engine.database.model.Domain;
import org.apache.torque.engine.database.model.SchemaType;

/**
 * @author <a href="mailto:mpoeschl@marmot.at">Martin Poeschl</a>
 * @version $Id$
 */
public class PlatformMysqlImplTest extends TestCase {

    Platform platform;
    
    public void setUp()
    {
        platform = PlatformFactory.getPlatformFor("mysql");
    }
    
    public void testGetMaxColumnNameLength() {
        assertEquals(64, platform.getMaxColumnNameLength());
    }

    public void testGetNativeIdMethod() {
        assertEquals("identity", platform.getNativeIdMethod());
    }

    public void testGetDomainForJdbcType() {
        Domain numeric = platform.getDomainForSchemaType(SchemaType.NUMERIC);
        assertEquals(SchemaType.NUMERIC, numeric.getType());
        assertEquals("DECIMAL", numeric.getSqlType());
    }

}
