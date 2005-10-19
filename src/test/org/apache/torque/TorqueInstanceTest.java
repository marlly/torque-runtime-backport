package org.apache.torque;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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


/**
 * Tests the TorqueInstance Class.
 *
 * @author <a href="mailto:fischer@seitenbau.de">Thomas Fischer</a>
 * @version $Id$
 */
public class TorqueInstanceTest extends BaseTestCase
{  
    /** The name of the "default" dataSourceFactory" as used by Turbine */
    private static final String DEFAULT_NAME = "default";
    
    /**
     * Creates a new instance.
     *
     * @param name the name of the test case to run
     */
    public TorqueInstanceTest(String name)
    {
        super(name);
    }
    
    /**
     * Checks whether a DataSourceFactory with the name 
     * <code>DEFAULT_NAME</code> is defined (TRQS 322)
     * @throws Exception if an error occurs during the Test
     */
    public void testDefaultDataSourceFactory() throws Exception
    {
        assertNotNull(Torque.getInstance().getDataSourceFactory(DEFAULT_NAME));
    }
}
