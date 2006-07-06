package org.apache.torque.adapter;

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
 * Torque Database Adapter for DB2/400 on the IBM AS400 platform.
 *
 * @author <a href="mailto:sweaver@rippe.com">Scott Weaver</a>
 * @author <a href="mailto:vido@ldh.org">Augustin Vidovic</a>
 * @version $Id$
 */
public class DBDB2400 extends DBDB2App
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -6185644296549139007L;

   /**
    * UpperCase/IgnoreCase sql function in DB2/400
    */
   public static final String UCASE = "UCASE";


    /**
     * DBDB2400 constructor.
     */
    protected DBDB2400()
    {
        super();
    }

    /**
     * This method is used to ignore case.
     *
     * @param in The string whose case to ignore.
     * @return The string in a case that can be ignored.
     */
    public String ignoreCase(String in)
    {
        String s = formatCase(in);
        return s;
    }

    /**
     * This method is used to ignore case.
     *
     * @param in The string to transform to upper case.
     * @return The upper case string.
     */
    public String toUpperCase(String in)
    {
        String s = formatCase(in);
        return s;
    }

    /**
     * Convenience method for String-formatting
     * upper/ignore case statements.
     *
     * @param in The string to transform to upper case.
     * @return The upper case string.
     */
    private String formatCase(String in)
    {
        return new StringBuffer(UCASE + "(").append(in).append(")").toString();
    }
}
