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

import junit.framework.TestCase;

/**
 * Base functionality to be extended by all Torque test cases.  Test
 * case implementations are used to automate unit testing via JUnit.
 *
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @author <a href="mailto:celkins@scardini.com">Christopher Elkins</a>
 * @version $Id$
 */
public abstract class BaseTestCase extends TestCase
{
    /** The path to the configuration file. */
    private static final String CONFIG_FILE
            = "src/test/TurbineResources.properties";

    /** Whether torque has been initialized. */
    private static boolean hasInitialized = false;

    /**
     * Creates a new instance.
     *
     * @param name the name of the test case to run
     */
    public BaseTestCase(String name)
    {
        super(name);
    }

    /**
     * Initialize Torque on the first setUp().  Subclasses which
     * override setUp() must call super.setUp() as their first action.
     */
    public void setUp()
    {
        synchronized (BaseTestCase.class)
        {
            if (!hasInitialized)
            {
                try
                {
                    Torque.init(CONFIG_FILE);
                    hasInitialized = true;
                }
                catch (Exception e)
                {
                    fail("Couldn't initialize Torque: " + e.getMessage());
                }
            }
        }
    }
}
