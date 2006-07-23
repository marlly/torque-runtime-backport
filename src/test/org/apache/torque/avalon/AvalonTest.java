package org.apache.torque.avalon;
/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.apache.avalon.framework.component.ComponentException;
import org.apache.fulcrum.testcontainer.BaseUnitTest;

/**
 * Basic testing of the Torque Avalon Component
 *
 * @author <a href="mailto:tv@apache.org">Thomas Vandahl</a>
 * @version $Id:$
 */
public class AvalonTest extends BaseUnitTest
{
    private Torque torque = null;
    
    /**
     * Constructor for test.
     *
     * @param testName name of the test being executed
     */
    public AvalonTest(String name)
    {
        super( name );
    }

    public void setUp() throws Exception
    {
        super.setUp();
        try
        {
            torque = (Torque) this.resolve( Torque.class.getName() );
        }
        catch (ComponentException e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    
    /**
     * Verifies that the container initialization and lookup works properly.
     */
    public void testAvalon()
    {
        assertTrue(torque.isInit());
        assertTrue("Instances should be identical", torque == org.apache.torque.Torque.getInstance());
    }
}
