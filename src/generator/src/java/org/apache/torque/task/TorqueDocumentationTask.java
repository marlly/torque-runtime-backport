package org.apache.torque.task;

/*
 * Copyright 2001,2004 The Apache Software Foundation.
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

import org.apache.velocity.context.Context;

/**
 * An ant task for generating output by using Velocity
 *
 * @author <a href="mailto:mpoeschl@marmot.at">Martin Poeschl</a>
 * @version $Id$
 */
public class TorqueDocumentationTask extends TorqueDataModelTask
{
    /** output format for the generated docs */
    private String outputFormat;

    /**
     * Get the current output format.
     *
     * @return the current output format
     */
    public String getOutputFormat()
    {
        return outputFormat;
    }

    /**
     * Set the current output format.
     *
     * @param v output format
     */
    public void setOutputFormat(String v)
    {
        outputFormat = v;
    }

    /**
     * Place our target package value into the context for use in the templates.
     *
     * @return the context
     * @throws Exception a generic exception
     */
    public Context initControlContext() throws Exception
    {
        super.initControlContext();
        context.put("outputFormat", outputFormat);
        context.put("escape", new org.apache.velocity.anakia.Escape());
        return context;
    }
}
