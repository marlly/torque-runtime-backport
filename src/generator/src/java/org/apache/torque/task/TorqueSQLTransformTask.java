package org.apache.torque.task;

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

import java.io.BufferedWriter;
import java.io.FileWriter;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import org.apache.torque.engine.database.model.Database;
import org.apache.torque.engine.database.transform.SQLToAppData;

/**
 * An ant task for creating an xml schema from an sql schema
 *
 * @author <a href="mailto:leon@opticode.co.za">Leon Messerschmidt</a>
 * @author <a href="jvanzyl@zenplex.com">Jason van Zyl</a>
 * @version $Id$
 */
public class TorqueSQLTransformTask extends Task
{
    /** SQL input file. */
    private String inputFile;

    /** XML descriptor output file. */
    private String outputFile;

    /**
     * Get the current input file
     *
     * @return the input file
     */
    public String getInputFile()
    {
        return inputFile;
    }

    /**
     * Set the sql input file.  This file must exist
     *
     * @param v the input file
     */
    public void setInputFile(String v)
    {
        inputFile = v;
    }

    /**
     * Get the current output file.
     *
     * @return the output file
     */
    public String getOutputFile()
    {
        return outputFile;
    }

    /**
     * Set the current output file.  If the file does not
     * exist it will be created.  If the file exists all
     * it's contents will be replaced.
     *
     * @param v the output file
     */
    public void setOutputFile (String v)
    {
        outputFile = v;
    }

    /**
     * Execute the task.
     *
     * @throws BuildException Any exceptions caught during procssing will be
     *         rethrown wrapped into a BuildException
     */
    public void execute() throws BuildException
    {
        try
        {
            log("Parsing SQL Schema", Project.MSG_INFO);

            SQLToAppData sqlParser = new SQLToAppData(inputFile);
            Database app = sqlParser.execute();

            log("Preparing to write xml schema", Project.MSG_INFO);
            FileWriter fr = new FileWriter(outputFile);
            BufferedWriter br = new BufferedWriter (fr);

            br.write(app.toString());

            log("Writing xml schema", Project.MSG_INFO);

            br.flush();
            br.close();
        }
        catch (Exception e)
        {
            throw new BuildException(e);
        }
    }
}
