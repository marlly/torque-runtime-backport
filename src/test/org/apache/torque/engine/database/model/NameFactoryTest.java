package org.apache.torque.engine.database.model;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache Turbine" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Turbine", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.torque.BaseTestCase;

/**
 * <p>Unit tests for class <code>NameFactory</code> and known
 * <code>NameGenerator</code> implementations.</p>
 *
 * <p>This test assumes that it's being run using the MySQL database
 * adapter, <code>DBMM</code>.  MySQL has a column length limit of 64
 * characters.</p>
 *
 * @author <a href="mailto:dlr@collab.net">Daniel Rall</a>
 * @version $Id$
 */
public class NameFactoryTest extends BaseTestCase
{
    private static final String DATABASE_TYPE = "mysql";

    /**
     * The list of known name generation algorithms, specified as the
     * fully qualified class names to <code>NameGenerator</code>
     * implementations.
     */
    private static final String[] ALGORITHMS =
    {
        NameFactory.CONSTRAINT_GENERATOR
    };

    /**
     * Two dimensional arrays of inputs for each algorithm.
     */
    private static final Object[][][] INPUTS =
    {
        { { makeString(61), "I", new Integer(1) },
          { makeString(61), "I", new Integer(2) },
          { makeString(65), "I", new Integer(3) },
          { makeString(4), "FK", new Integer(1) },
          { makeString(5), "FK", new Integer(2) } }
    };

    /**
     * Given the known inputs, the expected name outputs.
     */
    private static final String[][] OUTPUTS =
    {
        { makeString(60) + "_I_1",
          makeString(60) + "_I_2",
          makeString(60) + "_I_3",
          makeString(4)  + "_FK_1",
          makeString(5)  + "_FK_2" }
    };

    /**
     * Used as an input.
     */
    private Database database;

    /**
     * Creates a new instance.
     */
    public NameFactoryTest(String name)
    {
        super(name);
    }

    /**
     * Creates a string of the specified length consisting entirely of
     * the character <code>A</code>.  Useful for simulating table
     * names, etc.
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

    /**
     * Creates a test suite for this class.
     *
     * @return A test suite for this class.
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTest(new NameFactoryTest("NameFactoryTest"));
        return suite;
    }

    protected void setUp()
    {
        AppData appData =
            new AppData(DATABASE_TYPE, "src/templates/sql/base/");
        database = new Database();
        database.setDatabaseType(DATABASE_TYPE);
        appData.addDatabase(database);
    }

    /**
     * Runs this test case.
     */
    protected void runTest()
        throws Throwable
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
                assertEquals("Algorithm " + algo + " failed to generate an " +
                             "unique name", generated, expected);
            }
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
        return list;
    }
}
