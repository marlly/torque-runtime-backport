package org.apache.torque.engine.database.model;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.torque.engine.EngineException;

import junit.framework.TestCase;

/**
 * <p>Unit tests for class <code>NameFactory</code> and known
 * <code>NameGenerator</code> implementations.</p>
 *
 * <p>To add more tests, add entries to the <code>ALGORITHMS</code>,
 * <code>INPUTS</code>, and <code>OUTPUTS</code> arrays, and code to
 * the <code>makeInputs()</code> method.</p>
 *
 * <p>This test assumes that it's being run using the MySQL database
 * adapter, <code>DBMM</code>.  MySQL has a column length limit of 64
 * characters.</p>
 *
 * @author <a href="mailto:dlr@collab.net">Daniel Rall</a>
 * @version $Id$
 */
public class NameFactoryTest extends TestCase
{

    /** The database to mimic in generating the SQL. */
    private static final String DATABASE_TYPE = "mysql";

    /**
     * The list of known name generation algorithms, specified as the
     * fully qualified class names to <code>NameGenerator</code>
     * implementations.
     */
    private static final String[] ALGORITHMS =
        { NameFactory.CONSTRAINT_GENERATOR, NameFactory.JAVA_GENERATOR };

    /**
     * Two dimensional arrays of inputs for each algorithm.
     */
    private static final Object[][][] INPUTS =
        { { { makeString(61), "I", new Integer(1)}, {
                makeString(61), "I", new Integer(2)
                }, {
                makeString(65), "I", new Integer(3)
                }, {
                makeString(4), "FK", new Integer(1)
                }, {
                makeString(5), "FK", new Integer(2)
                }
        }, {
            {
                "MY_USER", NameGenerator.CONV_METHOD_UNDERSCORE }, {
                "MY_USER", NameGenerator.CONV_METHOD_JAVANAME }, {
                "MY_USER", NameGenerator.CONV_METHOD_NOCHANGE }
        }
    };

    /**
     * Given the known inputs, the expected name outputs.
     */
    private static final String[][] OUTPUTS =
        {
            {
                makeString(60) + "_I_1",
                makeString(60) + "_I_2",
                makeString(60) + "_I_3",
                makeString(4) + "_FK_1",
                makeString(5) + "_FK_2" },
            {
            "MyUser", "MYUSER", "MY_USER" }
    };

    /**
     * Used as an input.
     */
    private Database database;

    /**
     * Creates a new instance.
     *
     * @param name the name of the test to run
     */
    public NameFactoryTest(String name)
    {
        super(name);
    }

    /**
     * Creates a string of the specified length consisting entirely of
     * the character <code>A</code>.  Useful for simulating table
     * names, etc.
     *
     * @param len the number of characters to include in the string
     * @return a string of length <code>len</code> with every character an 'A'
     */
    private static final String makeString(int len)
    {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < len; i++)
        {
            buf.append('A');
        }
        return buf.toString();
    }

    /** Sets up the Torque model. */
    public void setUp()
    {
        database = new Database(DATABASE_TYPE);
        database.setDatabaseType(DATABASE_TYPE);
    }

    /**
     * @throws Exception on fail
     */
    public void testNames() throws Exception
    {
        for (int algoIndex = 0; algoIndex < ALGORITHMS.length; algoIndex++)
        {
            String algo = ALGORITHMS[algoIndex];
            Object[][] algoInputs = INPUTS[algoIndex];
            for (int i = 0; i < algoInputs.length; i++)
            {
                List inputs = makeInputs(algo, algoInputs[i]);
                String generated = NameFactory.generateName(algo, inputs);
                String expected = OUTPUTS[algoIndex][i];
                assertEquals(
                    "Algorithm " + algo + " failed to generate an unique name",
                    generated,
                    expected);
            }
        }
    }
    
    /**
     * @throws Exception on fail
     */
    public void testException() throws Exception
    {
        try
        {
            NameFactory.generateName("non.existing.class", new ArrayList());
            assertTrue("Expected an EngineException", false);           
        }
        catch (EngineException ex)
        {
        }
    }

    /**
     * Creates the list of arguments to pass to the specified type of
     * <code>NameGenerator</code> implementation.
     *
     * @param algo The class name of the <code>NameGenerator</code> to
     * create an argument list for.
     * @param inputs The (possibly partial) list inputs from which to
     * generate the final list.
     * @return the list of arguments to pass to the <code>NameGenerator</code>
     */
    private final List makeInputs(String algo, Object[] inputs)
    {
        List list = null;
        if (NameFactory.CONSTRAINT_GENERATOR.equals(algo))
        {
            list = new ArrayList(inputs.length + 1);
            list.add(0, database);
            list.addAll(Arrays.asList(inputs));
        }
        else if (NameFactory.JAVA_GENERATOR.equals(algo))
        {
            list = Arrays.asList(inputs);
        }
        return list;
    }

}
